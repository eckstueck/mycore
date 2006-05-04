/*
 * $RCSfile$
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

package org.mycore.backend.hibernate;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

import org.mycore.backend.hibernate.tables.MCRLINKHREF;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.metadata.MCRLinkTableInterface;

/**
 * This class implements the MCRLinkTableInterface.
 * 
 * @author Heiko Helmbrecht
 * @author Jens Kupferschmidt
 */
public class MCRHIBLinkTableStore implements MCRLinkTableInterface {
    // logger
    static Logger logger = Logger.getLogger(MCRHIBLinkTableStore.class.getName());

    private String classname = "org.mycore.backend.hibernate.tables.MCRLINKHREF";

    private Session getSession() {
        return MCRHIBConnection.instance().getSession();
    }

    /**
     * The constructor for the class MCRHIBLinkTableStore.
     */
    public MCRHIBLinkTableStore() {
    }

    /**
     * The method create a new item in the datastore.
     * 
     * @param from
     *            a string with the link ID MCRFROM
     * @param to
     *            a string with the link ID MCRTO
     * @param type
     *            a string with the link ID MCRTYPE
     * @param attr
     *            a string with the link ID MCRATTR
     */
    public final void create(String from, String to, String type, String attr) {
        if ((from == null) || ((from = from.trim()).length() == 0)) {
            throw new MCRPersistenceException("The from value is null or empty.");
        }

        if ((to == null) || ((to = to.trim()).length() == 0)) {
            throw new MCRPersistenceException("The to value is null or empty.");
        }

        if ((type == null) || ((type = type.trim()).length() == 0)) {
            throw new MCRPersistenceException("The type value is null or empty.");
        }

        if ((attr == null) || ((attr = attr.trim()).length() == 0)) {
            attr = "";
        }

        Session session = getSession();
        Transaction tx = session.beginTransaction();

        try {
            MCRLINKHREF l = new MCRLINKHREF(from, to, type, attr);
            session.saveOrUpdate(l);
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            logger.error(e);
        } finally {
            session.close();
        }
    }

    /**
     * The method removes a item for the from ID from the datastore.
     * 
     * @param from
     *            a string with the link ID MCRFROM
     * @param to
     *            a string with the link ID MCRTO
     * @param type
     *            a string with the link ID MCRTYPE
     */
    public final void delete(String from, String to, String type) {
        if ((from == null) || ((from = from.trim()).length() == 0)) {
            throw new MCRPersistenceException("The from value is null or empty.");
        }
        StringBuffer sb = new StringBuffer();
        sb.append("from ").append(classname).append(" where MCRFROM = '").append(from).append("'");

        if ((to != null) && ((to = to.trim()).length() > 0)) {
            sb.append(" and MCRTO = '").append(to).append("'");
        }

        if ((type != null) && ((type = type.trim()).length() > 0)) {
            sb.append(" and MCRTYPE = '").append(type).append("'");
        }

        Session session = getSession();
        Transaction tx = session.beginTransaction();

        try {
            List l = session.createQuery(sb.toString()).list();
            if (l.size() == 0)
                return;
            for (int t = 0; t < l.size(); t++) {
                session.delete(l.get(t));
            }
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            logger.error(e);
        } finally {
            session.close();
        }
    }

    /**
     * The method count the number of references with '%from%' and 'to' and
     * optional 'type' and optional 'restriction%' values of the table.
     * 
     * @param fromtype
     *            a substing in the from ID as String, it can be null
     * @param to
     *            the object ID as String, which is referenced
     * @param type
     *            the refernce type, it can be null
     * @param restriction
     *            a first part of the to ID as String, it can be null
     * @return the number of references
     */
    public final int countTo(String fromtype, String to, String type, String restriction) {
        Session session = getSession();
        Integer returns;
        StringBuffer qBf = new StringBuffer(1024);
        qBf.append("select count(key.mcrfrom) from ").append(classname).append(" where MCRTO = :to");
        boolean tset = false, rset = false, fset = false;

        if ((type != null) && (type.length() != 0)) {
            tset = true;
            qBf.append(" and MCRTYPE = :type");
        }
        if ((restriction != null) && (restriction.length() != 0)) {
            rset = true;
            qBf.append(" and MCRTO like :restriction");
        }
        if ((fromtype != null) && (fromtype.length() != 0)) {
            fset = true;
            qBf.append(" and MCRFROM like %_:fromtype_%");
        }

        try {
            Query q = session.createQuery(qBf.toString());
            q.setString("to", to);
            if (tset) {
                q.setString("type", type);
            }
            if (rset) {
                q.setString("restriction", restriction);
            }
            if (fset) {
                q.setString("fromtype", fromtype);
            }
            returns=(Integer)q.uniqueResult();
        } catch (Exception e) {
            logger.error(e);
            throw new MCRException("Error during countTo(" + fromtype + "," + to + "," + type + "," + restriction + ")", e);
        } finally {
            session.close();
        }

        return returns.intValue();
    }

    /**
     * The method returns a Map of all counted distinct references
     * 
     * @param mcrtoPrefix
     * @return
     * 
     * the result-map of (key,value)-pairs can be visualized as<br />
     * select count(mcrfrom) as value, mcrto as key from
     * mcrlinkclass|mcrlinkhref where mcrto like mcrtoPrefix + '%' group by
     * mcrto;
     * 
     */
    public Map getCountedMapOfMCRTO(String mcrtoPrefix) {
        Map map = new HashMap();
        Session session = getSession();
        String query = "select count(key.mcrfrom), key.mcrto from " + classname + " where MCRTO like '" + mcrtoPrefix + "%' group by key.mcrto";
        logger.debug("HQL-Statement: " + query);
        try {
            Iterator results = session.createQuery(query).list().iterator();
            while (results.hasNext()) {
                Object[] row = (Object[]) results.next();
                map.put(row[1], row[0]);
            }
        } catch (Exception e) {
            logger.error("catched error@getCountedMapOfMCRTO:", e);
            throw new MCRException("Error during getCountedMapOfMCRTO", e);
        } finally {
            session.close();
        }
        return map;
    }

    /**
     * Returns a List of all link sources of <code>to</code> and a special
     * <code>type</code>
     * 
     * @param to
     *            Destination-ID
     * @param type
     *            link reference type, this can be null
     * @return List of Strings (Source-IDs)
     */
    public List getSourcesOf(String to, String type) {
        Session session = getSession();
        StringBuffer querySB = new StringBuffer("select key.mcrfrom from ").append(classname).append(" where MCRTO='").append(to).append("'");
        if ((type != null) && (type.trim().length() > 0)) {
            querySB.append(" and MCRTYPE = '").append(type).append("'");
        }
        String query = querySB.toString();
        logger.debug("HQL-Statement: " + query);
        List returns;
        try {
            returns = session.createQuery(query).list();
        } catch (Exception e) {
            logger.error("catched error@getSourceOf:", e);
            throw new MCRException("Error during getSourceOf", e);
        } finally {
            session.close();
        }
        return returns;
    }

    /**
     * Returns a List of all link destinations of <code>destination</code>
     * 
     * @param source
     *            source-ID
     * @param type
     *            reference type of the link
     * @return List of Strings (Destination-IDs)
     */
    public List getDestinationsOf(String source, String type) {
        Session session = getSession();
        StringBuffer querySB = new StringBuffer("select key.mcrto from ").append(classname).append(" where MCRFROM='").append(source).append("'");
        if ((type != null) && (type.trim().length() != 0)) {
            querySB.append(" and MCRTYPE = '").append(type).append("'");
        }
        String query = querySB.toString();
        logger.debug("HQL-Statement: " + query);
        List returns;
        try {
            returns = session.createQuery(query).list();
        } catch (Exception e) {
            logger.error("catched error@getDestinationOf:", e);
            throw new MCRException("Error during getDestinationOf", e);
        } finally {
            session.close();
        }
        return returns;
    }

}