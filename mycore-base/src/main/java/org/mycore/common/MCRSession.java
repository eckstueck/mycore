/*
 * 
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.common;

import static org.mycore.common.events.MCRSessionEvent.Type.activated;
import static org.mycore.common.events.MCRSessionEvent.Type.passivated;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.EntityTransaction;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.events.MCRSessionEvent;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.common.events.MCRShutdownHandler.Closeable;
import org.mycore.frontend.servlets.MCRServletJob;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Instances of this class collect information kept during a session like the currently active user, the preferred
 * language etc.
 * 
 * @author Detlev Degenhardt
 * @author Jens Kupferschmidt
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRSession implements Cloneable {

    private static final URI defaultURI = URI.create("");

    /** A map storing arbitrary session data * */
    private Map<Object, Object> map = new Hashtable<Object, Object>();

    @SuppressWarnings("unchecked")
    private Map.Entry<Object, Object>[] emptyEntryArray = new Map.Entry[0];

    private List<Map.Entry<Object, Object>> mapEntries;

    private boolean mapChanged = true;

    AtomicInteger accessCount;

    AtomicInteger concurrentAccess;

    ThreadLocal<AtomicInteger> currentThreadCount = new ThreadLocal<AtomicInteger>() {
        @Override
        public AtomicInteger initialValue() {
            return new AtomicInteger();
        }
    };

    /** the logger */
    static Logger LOGGER = Logger.getLogger(MCRSession.class.getName());

    /** The user ID of the session */
    private MCRUserInformation userInformation;

    /** The language for this session as upper case character */
    private String language = null;

    private Locale locale = null;

    /** The unique ID of this session */
    private String sessionID = null;

    private String ip = null;

    private long loginTime, lastAccessTime, thisAccessTime, createTime;

    private boolean dataBaseAccess;

    private ThreadLocal<EntityTransaction> transaction = new ThreadLocal<>();

    private ThreadLocal<MCRServletJob> servletJob = new ThreadLocal<>();

    private StackTraceElement[] constructingStackTrace;

    private Optional<URI> firstURI = Optional.empty();

    private ThreadLocal<Throwable> lastActivatedStackTrace = new ThreadLocal<Throwable>();

    private ThreadLocal<List<Runnable>> onCommitTasks = new ThreadLocal<List<Runnable>>() {
        @Override
        protected List<Runnable> initialValue() {
            return Collections.synchronizedList(new CopyOnWriteArrayList<>());
        }
    };

    private static ExecutorService COMMIT_SERVICE;

    private static MCRUserInformation guestUserInformation = MCRSystemUserInformation.getGuestInstance();

    static {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("MCRSession-OnCommitService-#%d")
                                                                .build();
        COMMIT_SERVICE = Executors.newFixedThreadPool(4, threadFactory);
        MCRShutdownHandler.getInstance().addCloseable(new Closeable() {

            @Override
            public void prepareClose() {
                COMMIT_SERVICE.shutdown();
            }

            @Override
            public int getPriority() {
                return Integer.MIN_VALUE + 8;
            }

            @Override
            public void close() {
                if (!COMMIT_SERVICE.isTerminated()) {
                    try {
                        COMMIT_SERVICE.awaitTermination(10, TimeUnit.MINUTES);
                    } catch (InterruptedException e) {
                        LOGGER.warn("Error while waiting for shutdown.", e);
                    }
                }
            }

        });
    }

    /**
     * The constructor of a MCRSession. As default the user ID is set to the value of the property variable named
     * 'MCR.Users.Guestuser.UserName'.
     */
    MCRSession() {
        MCRConfiguration config = MCRConfiguration.instance();
        userInformation = guestUserInformation;
        setCurrentLanguage(config.getString("MCR.Metadata.DefaultLang", MCRConstants.DEFAULT_LANG));
        dataBaseAccess = MCRHIBConnection.isEnabled();

        accessCount = new AtomicInteger();
        concurrentAccess = new AtomicInteger();

        ip = "";
        sessionID = buildSessionID();
        MCRSessionMgr.addSession(this);

        LOGGER.debug("MCRSession created " + sessionID);
        setLoginTime();
        createTime = loginTime;
        Throwable t = new Throwable();
        t.fillInStackTrace();
        constructingStackTrace = t.getStackTrace();
    }

    protected final void setLoginTime() {
        loginTime = System.currentTimeMillis();
        lastAccessTime = loginTime;
        thisAccessTime = loginTime;
    }

    /**
     * Constructs a unique session ID for this session, based on current time and IP address of host where the code
     * runs.
     */
    private static String buildSessionID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Returns the unique ID of this session
     */
    public String getID() {
        return sessionID;
    }

    /**
     * Returns a list of all stored object keys within MCRSession. This method is not thread safe. I you need thread
     * safe access to all stored objects use {@link MCRSession#getMapEntries()} instead.
     * 
     * @return Returns a list of all stored object keys within MCRSession as java.util.Ierator
     */
    public Iterator<Object> getObjectsKeyList() {
        return Collections.unmodifiableSet(map.keySet()).iterator();
    }

    /**
     * Returns an unmodifiable list of all entries in this MCRSession This method is thread safe.
     */
    public List<Map.Entry<Object, Object>> getMapEntries() {
        if (mapChanged) {
            mapChanged = false;
            final Set<Entry<Object, Object>> entrySet = Collections.unmodifiableMap(map).entrySet();
            final Map.Entry<Object, Object>[] entryArray = entrySet.toArray(emptyEntryArray);
            mapEntries = Collections.unmodifiableList(Arrays.asList(entryArray));
        }
        return mapEntries;
    }

    /** returns the current language */
    public final String getCurrentLanguage() {
        return language;
    }

    /** sets the current language */
    public final void setCurrentLanguage(String language) {
        Locale newLocale = Locale.forLanguageTag(language);
        this.language = language;
        this.locale = newLocale;
    }

    public Locale getLocale() {
        return locale;
    }

    /** Write data to the logger for debugging purposes */
    public final void debug() {
        LOGGER.debug("SessionID = " + sessionID);
        LOGGER.debug("UserID    = " + getUserInformation().getUserID());
        LOGGER.debug("IP        = " + ip);
        LOGGER.debug("language  = " + language);
    }

    /** Stores an object under the given key within the session * */
    public Object put(Object key, Object value) {
        mapChanged = true;
        return map.put(key, value);
    }

    /** Returns the object that was stored in the session under the given key * */
    public Object get(Object key) {
        return map.get(key);
    }

    public void deleteObject(Object key) {
        mapChanged = true;
        map.remove(key);
    }

    /** Get the current ip value */
    public String getCurrentIP() {
        return ip;
    }

    /** Set the ip to the given IP */
    public final void setCurrentIP(String newip) {
        java.util.StringTokenizer st = new java.util.StringTokenizer(newip, ".");
        if (st.countTokens() == 4) {
            try {
                while (st.hasMoreTokens()) {
                    int i = Integer.parseInt(st.nextToken());
                    if (i < 0 || i > 255) {
                        return;
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("Exception while parsing new ip " + newip + " using old value.", e);
                return;
            }
            ip = newip;
        }

        //IPV6,z.B: 2001:0db8:85a3:0000:0000:8a2e:0370:7334
        st = new java.util.StringTokenizer(newip, ":");
        if (st.countTokens() == 8) {
            try {
                while (st.hasMoreTokens()) {
                    long l = Long.parseLong(st.nextToken(), 16);
                    if (l < 0 || l > 65535) {
                        return;
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("Exception while parsing new ip " + newip + " using old value.", e);
                return;
            }
            ip = newip;
        }
    }

    public final long getLoginTime() {
        return loginTime;
    }

    public void close() {
        // remove from session list
        LOGGER.debug("Remove myself from MCRSession list");
        MCRSessionMgr.removeSession(this);
        // clear bound objects
        LOGGER.debug("Clearing local map.");
        map.clear();
        mapEntries = null;
        sessionID = null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MCRSession[");
        sb.append(getID());
        sb.append(",user:'");
        sb.append(getUserInformation().getUserID());
        sb.append("',ip:");
        sb.append(getCurrentIP());
        sb.append("]");
        return sb.toString();
    }

    public long getLastAccessedTime() {
        return lastAccessTime;
    }

    public void setServletJob(MCRServletJob job) {
        if (!firstURI.isPresent()) {
            firstURI = Optional.of(URI.create(job.getRequest().getRequestURI()));
        }
        servletJob.set(job);
    }

    /**
     * Returns this thread current HttpServletRequest.
     * Does not work in a Thread that does not handle the current request or in CLI.
     */
    public Optional<HttpServletRequest> getServletRequest() {
        return Optional.ofNullable(servletJob.get()).map(MCRServletJob::getRequest);
    }

    /**
     * Returns this thread current HttpServletResponse.
     * Does not work in a Thread that does not handle the current request or in CLI.
     */
    public Optional<HttpServletResponse> getServletResponse() {
        return Optional.ofNullable(servletJob.get()).map(MCRServletJob::getResponse);
    }

    /**
     * Activate this session. For internal use mainly by MCRSessionMgr.
     * 
     * @see MCRSessionMgr#setCurrentSession(MCRSession)
     */
    void activate() {
        lastAccessTime = thisAccessTime;
        thisAccessTime = System.currentTimeMillis();
        accessCount.incrementAndGet();
        if (currentThreadCount.get().getAndIncrement() == 0) {
            lastActivatedStackTrace.set(new RuntimeException("This is for debugging purposes only"));
            fireSessionEvent(activated, concurrentAccess.incrementAndGet());
        } else {
            MCRException e = new MCRException(
                "Cannot activate a Session more than once per thread: " + currentThreadCount.get().get());
            LOGGER.warn("Too many activate() calls stacktrace:", e);
            LOGGER.warn("First activate() call stacktrace:", lastActivatedStackTrace.get());
        }
    }

    /**
     * Passivate this session. For internal use mainly by MCRSessionMgr.
     * 
     * @see MCRSessionMgr#releaseCurrentSession()
     */
    void passivate() {
        if (currentThreadCount.get().getAndDecrement() == 1) {
            lastActivatedStackTrace.set(null);
            fireSessionEvent(passivated, concurrentAccess.decrementAndGet());
        } else {
            LOGGER.debug("deactivate currentThreadCount: " + currentThreadCount.get().get());
        }
        if (!firstURI.isPresent()) {
            firstURI = Optional.of(defaultURI);
        }
        servletJob.remove();
    }

    /**
     * Fire MCRSessionEvents. This is a common method that fires all types of MCRSessionEvent. Mainly for internal use
     * of MCRSession and MCRSessionMgr.
     * 
     * @param type
     *            type of event
     * @param concurrentAccessors
     *            number of concurrentThreads (passivateEvent gets 0 for singleThread)
     */
    void fireSessionEvent(MCRSessionEvent.Type type, int concurrentAccessors) {
        MCRSessionEvent event = new MCRSessionEvent(this, type, concurrentAccessors);
        LOGGER.debug(event);
        MCRSessionMgr.getListeners().stream().forEach(l -> l.sessionEvent(event));
    }

    public long getThisAccessTime() {
        return thisAccessTime;
    }

    public long getCreateTime() {
        return createTime;
    }

    /**
     * starts a new database transaction.
     */
    public void beginTransaction() {
        if (dataBaseAccess) {
            EntityTransaction entityTransaction = MCREntityManagerProvider.getCurrentEntityManager().getTransaction();
            entityTransaction.begin();
            transaction.set(entityTransaction);
        }
    }

    /**
     * commits the database transaction. Commit is only done if {@link #isTransactionActive()} returns true.
     */
    public void commitTransaction() {
        if (isTransactionActive()) {
            transaction.get().commit();
            beginTransaction();
            MCREntityManagerProvider.getCurrentEntityManager().clear();
            transaction.get().commit();
            transaction.remove();
        }
        submitOnCommitTasks();
    }

    /**
     * forces the database transaction to roll back. Roll back is only performed if {@link #isTransactionActive()}
     * returns true.
     */
    public void rollbackTransaction() {
        if (isTransactionActive()) {
            transaction.get().rollback();
            MCREntityManagerProvider.getCurrentEntityManager().close();
            transaction.remove();
        }
    }

    /**
     * Is the transaction still alive?
     * 
     * @return true if the transaction is still alive
     */
    public boolean isTransactionActive() {
        if (!dataBaseAccess) {
            return false;
        }
        if (transaction.get() == null) {
            transaction.set(MCREntityManagerProvider.getCurrentEntityManager().getTransaction());
        }
        return transaction.get() != null && transaction.get().isActive();
    }

    public StackTraceElement[] getConstructingStackTrace() {
        return constructingStackTrace;
    }

    public Optional<URI> getFirstURI() {
        return firstURI;
    }

    /**
     * @return the userInformation
     */
    public MCRUserInformation getUserInformation() {
        return userInformation;
    }

    /**
     * @param userSystemAdapter
     *            the userInformation to set
     */
    public void setUserInformation(MCRUserInformation userSystemAdapter) {
        this.userInformation = userSystemAdapter;
        setLoginTime();
    }

    /**
     * Add a task which will be executed after {@link #commitTransaction()} was called.
     * 
     * @param task thread witch will be executed after an commit
     */
    public void onCommit(Runnable task) {
        this.onCommitTasks.get().add(task);
    }

    protected synchronized void submitOnCommitTasks() {
        this.onCommitTasks.get().forEach(task -> {
            COMMIT_SERVICE.submit(task);
        });
        this.onCommitTasks.get().clear();
    }

}
