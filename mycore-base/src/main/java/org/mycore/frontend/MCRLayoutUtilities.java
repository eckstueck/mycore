package org.mycore.frontend;

import static org.mycore.access.MCRAccessManager.PERMISSION_READ;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.access.mcrimpl.MCRAccessStore;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.xml.MCRURIResolver;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

/**
 * 
 * Xalan extention for navigation.xsl
 *
 */
public class MCRLayoutUtilities {
    private static final int STANDARD_CACHE_SECONDS = 10;

    private static final XPathFactory XPATH_FACTORY = XPathFactory.instance();

    final static String OBJIDPREFIX_WEBPAGE = "webpage:";

    // strategies for access verification
    public final static int ALLTRUE = 1;

    public final static int ONETRUE_ALLTRUE = 2;

    public final static int ALL2BLOCKER_TRUE = 3;

    private final static Logger LOGGER = Logger.getLogger(MCRLayoutUtilities.class);

    private static HashMap<String, Element> itemStore = new HashMap<String, Element>();

    private static final String NAV_RESOURCE = MCRConfiguration.instance().getString("MCR.NavigationFile",
        "/config/navigation.xml");

    private static final ServletContext SERVLET_CONTEXT = MCRURIResolver.getServletContext();

    private static LoadingCache<String, URL> NAV_URL_CACHE = CacheBuilder.newBuilder().maximumSize(1)
        .expireAfterWrite(STANDARD_CACHE_SECONDS, TimeUnit.SECONDS).build(new CacheLoader<String, URL>() {

            @Override
            public URL load(String key) throws Exception {
                return SERVLET_CONTEXT.getResource(NAV_RESOURCE);
            }
        });

    private static final LoadingCache<URL, Long> NAV_MODIFIED_CACHE = CacheBuilder.newBuilder().maximumSize(1)
        .expireAfterWrite(STANDARD_CACHE_SECONDS, TimeUnit.SECONDS).build(new CacheLoader<URL, Long>() {

            @Override
            public Long load(URL key) throws Exception {
                URLConnection urlConnection = key.openConnection();
                return urlConnection.getLastModified();
            }
        });

    private static final LoadingCache<String, Document> NAV_DOCUMENT_CACHE = CacheBuilder.newBuilder()
        .expireAfterWrite(STANDARD_CACHE_SECONDS, TimeUnit.SECONDS)
        .removalListener(new RemovalListener<String, Document>() {
            @Override
            public void onRemoval(RemovalNotification<String, Document> notification) {
                if (notification.getCause() == RemovalCause.EXPIRED) {
                    URL url = NAV_URL_CACHE.getUnchecked(notification.getKey());
                    long lastModified = NAV_MODIFIED_CACHE.getUnchecked(url);
                    long diff = System.currentTimeMillis() - lastModified;
                    if (TimeUnit.SECONDS.convert(diff, TimeUnit.MILLISECONDS) > STANDARD_CACHE_SECONDS) {
                        LOGGER.info("Keeping " + url + " in cache");
                        notification.setValue(notification.getValue());
                    } else {
                        LOGGER.info("Removing modified " + notification.getKey());
                    }
                }
            }
        }).build(new CacheLoader<String, Document>() {
            @Override
            public Document load(String key) throws Exception {
                URL url = NAV_URL_CACHE.get(key);
                try {
                    return new SAXBuilder(XMLReaders.NONVALIDATING).build(url);
                } finally {
                    itemStore.clear();
                }
            }
        });

    private static final boolean ACCESS_CONTROLL_ON = MCRConfiguration.instance().getBoolean(
        "MCR.Website.ReadAccessVerification", true);

    /**
     * Verifies a given $webpage-ID (//item/@href) from navigation.xml on read
     * permission, based on ACL-System. To be used by XSL with
     * Xalan-Java-Extension-Call. $blockerWebpageID can be used as already
     * verified item with read access. So, only items of the ancestor axis till
     * and exclusive $blockerWebpageID are verified. Use this, if you want to
     * speed up the check
     * 
     * @param webpageID
     *            any item/@href from navigation.xml
     * @param blockerWebpageID
     *            any ancestor item of webpageID from navigation.xml
     * @return true if access granted, false if not
     */
    public static boolean readAccess(String webpageID, String blockerWebpageID) {
        if (ACCESS_CONTROLL_ON) {
            long startTime = System.currentTimeMillis();
            boolean access = getAccess(webpageID, PERMISSION_READ, ALL2BLOCKER_TRUE, blockerWebpageID);
            LOGGER.debug("checked read access for webpageID= " + webpageID + " (with blockerWebpageID ="
                + blockerWebpageID + ") => " + access + ": took " + getDuration(startTime) + " msec.");
            return access;
        } else {
            return true;
        }
    }

    /**
     * Verifies a given $webpage-ID (//item/@href) from navigation.xml on read
     * permission, based on ACL-System. To be used by XSL with
     * Xalan-Java-Extension-Call.
     * 
     * @param webpageID
     *            any item/@href from navigation.xml
     * @return true if access granted, false if not
     */
    public static boolean readAccess(String webpageID) {
        if (ACCESS_CONTROLL_ON) {
            long startTime = System.currentTimeMillis();
            boolean access = getAccess(webpageID, PERMISSION_READ, ALLTRUE);
            LOGGER.debug("checked read access for webpageID= " + webpageID + " => " + access + ": took "
                + getDuration(startTime) + " msec.");
            return access;
        } else {
            return true;
        }
    }

    /**
     * Returns all labels of the ancestor axis for the given item within
     * navigation.xml
     * 
     * @param item
     * @return Label as String, like "labelRoot > labelChild >
     *         labelChildOfChild"
     * @throws JDOMException
     * @throws IOException
     */
    public static String getAncestorLabels(Element item) {
        String label = "";
        String lang = MCRSessionMgr.getCurrentSession().getCurrentLanguage().trim();
        XPathExpression<Element> xpath;
        Element ic = null;
        xpath = XPATH_FACTORY.compile("//.[@href='" + getWebpageID(item) + "']", Filters.element());
        ic = xpath.evaluateFirst(getNavi());
        while (ic.getName().equals("item")) {
            ic = ic.getParentElement();
            String webpageID = getWebpageID(ic);
            Element labelEl = null;
            xpath = XPATH_FACTORY.compile("//.[@href='" + webpageID + "']/label[@xml:lang='" + lang + "']",
                Filters.element());
            labelEl = xpath.evaluateFirst(getNavi());
            if (labelEl != null) {
                if (label.equals("")) {
                    label = labelEl.getTextTrim();
                } else {
                    label = labelEl.getTextTrim() + " > " + label;
                }
            }
        }
        return label;
    }

    /**
     * Verifies, if an item of navigation.xml has a given $permission.
     * 
     * @param webpageID
     *            item/@href
     * @param permission
     *            permission to look for
     * @param strategy
     *            ALLTRUE => all ancestor items of webpageID must have the
     *            permission, ONETRUE_ALLTRUE => only 1 ancestor item must have
     *            the permission
     * @return true, if access, false if no access
     */
    public static boolean getAccess(String webpageID, String permission, int strategy) {
        Element item = getItem(webpageID);
        // check permission according to $strategy
        boolean access = strategy == ALLTRUE;
        if (strategy == ALLTRUE) {
            while (item != null && access) {
                access = itemAccess(permission, item, access);
                item = item.getParentElement();
            }
        } else if (strategy == ONETRUE_ALLTRUE) {
            while (item != null && !access) {
                access = itemAccess(permission, item, access);
                item = item.getParentElement();
            }
        }
        return access;
    }

    /**
     * Verifies, if an item of navigation.xml has a given $permission with a
     * stop item ($blockerWebpageID)
     * 
     * @param webpageID
     *            item/@href
     * @param permission
     *            permission to look for
     * @param strategy
     *            ALL2BLOCKER_TRUE => all ancestor items of webpageID till and
     *            exlusiv $blockerWebpageID must have the permission
     * @param blockerWebpageID
     *            any ancestor item of webpageID from navigation.xml
     * @return true, if access, false if no access
     */
    public static boolean getAccess(String webpageID, String permission, int strategy, String blockerWebpageID) {
        Element item = getItem(webpageID);
        // check permission according to $strategy
        boolean access = false;
        if (strategy == ALL2BLOCKER_TRUE) {
            access = true;
            do {
                access = itemAccess(permission, item, access);
                item = item.getParentElement();
            } while (item != null && access && !getWebpageID(item).equals(blockerWebpageID));
        }
        return access;
    }

    /**
     * Returns a Element presentation of an item[@href=$webpageID]
     * 
     * @param webpageID
     * @return Element
     */
    private static Element getItem(String webpageID) {
        Element item = itemStore.get(webpageID);
        if (item == null) {
            XPathExpression<Element> xpath = XPATH_FACTORY.compile("//.[@href='" + webpageID + "']", Filters.element());
            item = xpath.evaluateFirst(getNavi());
            itemStore.put(webpageID, item);
        }
        return item;
    }

    /**
     * Verifies a single item on access according to $permission
     * 
     * @param permission
     * @param item
     * @param access
     *            initial value
     * @return
     */
    public static boolean itemAccess(String permission, Element item, boolean access) {
        String objID = getWebpageACLID(item);
        if (MCRAccessManager.hasRule(objID, permission)) {
            access = MCRAccessManager.checkPermission(objID, permission);
        }
        return access;
    }

    /**
     * Verifies a single item on access according to $permission and for a given
     * user
     * 
     * @param permission
     * @param item
     * @param access
     *            initial value
     * @param userID
     */
    public static boolean itemAccess(String permission, Element item, boolean access, String userID) {
        MCRAccessInterface am = MCRAccessManager.getAccessImpl();
        String objID = getWebpageACLID(item);
        if (am.hasRule(objID, permission)) {
            access = am.checkPermission(objID, permission, userID);
        }
        return access;
    }

    private static String getWebpageACLID(Element item) {
        return OBJIDPREFIX_WEBPAGE + getWebpageID(item);
    }

    public static String getWebpageACLID(String webpageID) {
        return OBJIDPREFIX_WEBPAGE + webpageID;
    }

    private static String getWebpageID(Element item) {
        return item.getAttributeValue("href");
    }

    /**
     * Returns the navigation.xml as org.jdom2.document, using a cache the
     * enhance loading time.
     * 
     * @return navigation.xml as org.jdom2.document
     */
    public static Document getNavi() {
        return NAV_DOCUMENT_CACHE.getUnchecked(NAV_RESOURCE);
    }

    public static long getDuration(long startTime) {
        return System.currentTimeMillis() - startTime;
    }

    public static String getOBJIDPREFIX_WEBPAGE() {
        return OBJIDPREFIX_WEBPAGE;
    }

    public static boolean hasRule(String permission, String webpageID) {
        MCRAccessInterface am = MCRAccessManager.getAccessImpl();
        return am.hasRule(getWebpageACLID(webpageID), permission);
    }

    public static String getRuleID(String permission, String webpageID) {
        MCRAccessStore as = MCRAccessStore.getInstance();
        String ruleID = as.getRuleID(getWebpageACLID(webpageID), permission);
        if (ruleID != null) {
            return ruleID;
        } else {
            return "";
        }
    }

    public static String getRuleDescr(String permission, String webpageID) {
        MCRAccessInterface am = MCRAccessManager.getAccessImpl();
        String ruleDes = am.getRuleDescription(getWebpageACLID(webpageID), permission);
        if (ruleDes != null) {
            return ruleDes;
        } else {
            return "";
        }
    }

    public static String getPermission2ReadWebpage() {
        return PERMISSION_READ;
    }

}
