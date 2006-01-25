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

package org.mycore.access;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.user.MCRUser;
import org.mycore.user.MCRUserMgr;

import sun.misc.Compare;

/**
 * MyCoRe-Standard Implementation of the
 * MCRAccessInterface
 * 
 * Maps object ids to rules
 * 
 * @author Matthias Kramm
 * @author Heiko Helmbrecht
 */
public class MCRAccessManager extends MCRAccessManagerBase{
	
	public static final String systemRulePrefix = "SYSTEMRULE" ;
	
    MCRCache cache;

    MCRAccessStore accessStore;

    MCRRuleStore ruleStore;

    MCRAccessRule dummyRule;
    
    boolean disabled = false;

    public MCRAccessManager() {
        MCRConfiguration config = MCRConfiguration.instance();
        int size = config.getInt("MCR.AccessPool.CacheSize", 2048);
        String pools = config.getString("MCR.AccessPools", "");

        if (pools.trim().length() == 0) {
            disabled = true;
        }

        cache = new MCRCache(size);
        accessStore = MCRAccessStore.getInstance();
        ruleStore = MCRRuleStore.getInstance();
        accessComp = new MCRAccessConditionsComparator();
        
        nextFreeRuleID = new HashMap();

        dummyRule = new MCRAccessRule(null, null, null, null, "dummy rule, always true");
    }

    private static MCRAccessManager singleton;
    private static Comparator accessComp ;
    
    private static HashMap nextFreeRuleID;

    // extended methods
    public static synchronized MCRAccessManagerBase instance() {
        if (singleton == null) {
            singleton = new MCRAccessManager();
        }
        return singleton;
    }
    
	public void addRule(String id, String pool, Element rule) throws MCRException {
		MCRRuleMapping ruleMapping = getAutoGeneratedRuleMapping(rule, "System", pool, id);
		String oldRuleID = accessStore.getRuleID(id, pool);
		if(oldRuleID == null || oldRuleID.equals("")) {
			accessStore.createAccessDefinition(ruleMapping);
		}else{
			accessStore.updateAccessDefinition(ruleMapping);
		}
		return;
	}
	
	public void removeRule(String id, String pool) throws MCRException {
		String ruleID = accessStore.getRuleID(id, pool);
		if(ruleID != null && !ruleID.equals("")) {
			MCRRuleMapping ruleMapping = accessStore.getAccessDefinition(ruleID, pool, id);
			accessStore.deleteAccessDefinition(ruleMapping);
		}
	}

	public void removeAllRules(String id) throws MCRException {
		for(Iterator it = accessStore.getPoolsForObject(id).iterator(); it.hasNext();) {
			String pool = (String) it.next();
			removeRule(id, pool);
		}
	}	

    public void updateRule(String id, String pool, org.jdom.Element rule) throws MCRException {
		MCRRuleMapping ruleMapping = getAutoGeneratedRuleMapping(rule, "System", pool, id);
		String oldRuleID = accessStore.getRuleID(id, pool);
		if(oldRuleID == null || oldRuleID.equals("")) {
			logger.debug("updateRule called for id <" + id + "> and pool <" + pool + ">, but no rule is existing, so new rule was created");
			accessStore.createAccessDefinition(ruleMapping);
		}else{
			accessStore.updateAccessDefinition(ruleMapping);
		}
		return;    	
	}	
	
    public boolean checkAccess(String id, String pool, MCRSession session) {
        MCRUser user = MCRUserMgr.instance().retrieveUser(session.getCurrentUserID());
        MCRIPAddress ip;

        try {
            ip = new MCRIPAddress(session.getCurrentIP());
        } catch (UnknownHostException e) {
            /* this should never happen */
            throw new MCRException("unknown host", e);
        }
        return checkAccess(id, pool, user, ip);
    }	
    
    public boolean checkAccessCondition(String id, String pool, org.jdom.Element rule, MCRSession session) {
    	// pool not needed?
    	Date date = new Date();
        MCRUser user = MCRUserMgr.instance().retrieveUser(session.getCurrentUserID());
        MCRIPAddress ip;

        try {
            ip = new MCRIPAddress(session.getCurrentIP());
        } catch (UnknownHostException e) {
            /* this should never happen */
            throw new MCRException("unknown host", e);
        }
        
        MCRAccessRule accessRule = new MCRAccessRule(null, null, null, getNormalizedRuleString(rule), null );
        
        return accessRule.checkAccess(user, date, ip);
    }

    public Element getAccessRule(String objID, String pool) {
    	MCRAccessRule accessRule = getAccess(objID, pool);
    	MCRRuleParser parser = new MCRRuleParser();
		Element rule = parser.parse(accessRule.rule).toXML();
		Element condition = new Element("condition");
		condition.setAttribute("format","xml");
		condition.addContent(rule);
    	return condition;
    }    
    
    // not extended methods

    public boolean isDisabled() {
        return disabled;
    }

    public MCRAccessRule getAccess(String objID, String pool) {
        if (disabled) {
            return dummyRule;
        }

        MCRAccessRule a = (MCRAccessRule) cache.get(pool + "#" + objID);

        if (a == null) {
            String ruleID = accessStore.getRuleID(objID, pool);

            if (ruleID != null) {
                a = ruleStore.getRule(ruleID);
            } else {
                a = null;
            }

            if (a == null) {
                a = dummyRule;
            }

            cache.put(pool + "#" + objID, a);
        }

        return a;
    }

    /**
     * Validator methods to validate access definition for given object and pool
     * 
     * @param pool
     *            poolname as string
     * @param objID
     *            MCRObjectID as string
     * @param user
     *            MCRUser
     * @param ip
     *            ip-Address
     * @param session
     *            MCRSession           
     * @return
     */
    public boolean checkAccess(String objID, String pool, MCRUser user, MCRIPAddress ip) {
        Date date = new Date();
        MCRAccessRule rule = getAccess(objID, pool);

        if (rule == null) {
            return true; // no rule: everybody can access this
        }

        return rule.checkAccess(user, date, ip);
    }


    /**
     * method removes an access entry from the cache
     * @param objID
     * @param pool
     * @return dummy-true
     */
    public boolean removeFromCache(String objID, String pool) {
    	String cacheKey = pool + "#" + objID;
    	cache.remove(cacheKey);
    	return true;
    }

    /**
     * method that delivers the next free ruleID for a given Prefix
     *  and sets the counter to counter + 1
     * @param prefix
     *          String
     * @return
     *         String 
     */
    public synchronized String getNextFreeRuleID(String prefix) {
    	int nextFreeID;
    	if(nextFreeRuleID.containsKey(prefix)) {
    		nextFreeID = ((Integer)nextFreeRuleID.get(prefix)).intValue();
    	}else {
    		nextFreeID = ruleStore.getNextFreeRuleID(prefix);
    	}
    	nextFreeRuleID.put(prefix, new Integer(nextFreeID + 1));
    	return prefix + String.valueOf(nextFreeID);
	}
    
    /**
     * delivers the rule as string, after normalizing it via sorting with MCRAccessConditionsComparator
     * @param rule
     *          Jdom-Element
     * @return
     * 		    String
     */
    public String getNormalizedRuleString(Element rule) {
    	Element normalizedRule = normalize((Element)rule.getChildren().get(0));
		MCRRuleParser parser = new MCRRuleParser();
		return parser.parse(normalizedRule).toString();
    }
    
    /**
     * returns a auto-generated MCRRuleMapping, needed to create Access Definitions
     * @param rule
     *          JDOM-Representation of a MCRAccess Rule
     * @param creator
     *          String
     * @param pool
     *          String
     * @param id
     *          String
     * @return
     *         MCRRuleMapping
     */
    public MCRRuleMapping getAutoGeneratedRuleMapping(Element rule, String creator, String pool, String id) {
		String ruleString = getNormalizedRuleString(rule);
		ArrayList existingIDs = ruleStore.retrieveRuleIDs(ruleString);
		String ruleID = null;
		if(existingIDs != null && existingIDs.size() > 0) {
			// rule yet exists
			ruleID = (String)existingIDs.get(0);
		}else{
			ruleID = getNextFreeRuleID(systemRulePrefix) ;
			MCRAccessRule accessRule = new MCRAccessRule(ruleID, creator, new Date(), ruleString, "");
			ruleStore.createRule(accessRule);
		}
		MCRRuleMapping ruleMapping = new MCRRuleMapping();
		ruleMapping.setCreator(creator);
		ruleMapping.setCreationdate(new Date());
		ruleMapping.setPool(pool);
		ruleMapping.setRuleId(ruleID);
		ruleMapping.setObjId(id);  
		return ruleMapping;
    }
	
	/**
	 * method, that normalizes the jdom-representation of
	 * a mycore access condition
	 * 
	 * @param rule condition-JDOM of an access-rule
	 * @return the normalized JDOM-Rule
	 */
	public Element normalize(Element rule) {
		Element newRule = new Element(rule.getName());
		for (Iterator it = rule.getAttributes().iterator(); it.hasNext();) {
			Attribute att = (Attribute) it.next();
			newRule.setAttribute((Attribute)att.clone());
		}
		List children = rule.getChildren();
		if (children == null || children.size() == 0) 
			return newRule;
		List newList = new ArrayList();
		for (Iterator it = children.iterator(); it.hasNext();) {
			Element el = (Element) it.next();
			newList.add(el.clone());
		}
		Collections.sort(newList, accessComp);
		for (Iterator it = newList.iterator(); it.hasNext();) {
			Element el = (Element) it.next();
			newRule.addContent(normalize(el));
		}
		return newRule;
	}

	/**
	 * A Comparator for the Condition Elements for
	 * 	 normalizing the access conditions
	 * 
	 */
	private class MCRAccessConditionsComparator implements Comparator {

		public int compare(Object arg0, Object arg1) {
			Element el0 = (Element) arg0;
			Element el1 = (Element) arg1;
			String nameEl0 = el0.getName().toLowerCase();
			String nameEl1 = el1.getName().toLowerCase();
			int nameCompare = nameEl0.compareTo(nameEl1);
			// order "boolean" before "condition"
			if (nameCompare != 0)
				return nameCompare;
			if (nameEl0.equals("boolean")) {
				String opEl0 = el0.getAttributeValue("operator").toLowerCase();
				String opEl1 = el0.getAttributeValue("operator").toLowerCase();
				return opEl0.compareTo(opEl1);
			} else if (nameEl0.equals("condition")) {
				String fieldEl0 = el0.getAttributeValue("field").toLowerCase();
				String fieldEl1 = el1.getAttributeValue("field").toLowerCase();
				int fieldCompare = fieldEl0.compareTo(fieldEl1);
				if (fieldCompare != 0)
					return fieldCompare;
				String valueEl0 = el0.getAttributeValue("value");
				String valueEl1 = el1.getAttributeValue("value");
				return valueEl0.compareTo(valueEl1);
			}
			return 0;
		}
	};    	

};

