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

package org.mycore.services.fieldquery;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.mycore.parsers.bool.MCRAndCondition;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCRNotCondition;
import org.mycore.parsers.bool.MCROrCondition;

/**
 * Executes queries on all configured searchers and returns query results.
 * 
 * @author Frank Lützenkirchen
 */
public class MCRQueryManager {

    /**
     * Executes a query defined as XML document and returns the query results.
     * 
     * @param query
     *            the query
     * @return the query results
     */
    public static MCRResults search(Document query) {
        Element root = query.getRootElement();
        int maxResults = Integer.parseInt(root.getAttributeValue("maxResults"));

        List sortBy = null;
        Element sortByElem = query.getRootElement().getChild("sortBy");

        if (sortByElem != null) {
            List children = sortByElem.getChildren();
            sortBy = new ArrayList(children.size());

            for (int i = 0; i < children.size(); i++) {
                Element sortByChild = (org.jdom.Element) (children.get(i));
                String name = sortByChild.getAttributeValue("field");
                String ad = sortByChild.getAttributeValue("order");

                MCRFieldDef fd = MCRFieldDef.getDef(name);
                boolean direction = ("ascending".equals(ad) ? MCRSortBy.ASCENDING : MCRSortBy.DESCENDING);
                sortBy.add(new MCRSortBy(fd, direction));
            }
        }

        Element condElem = (Element) (root.getChild("conditions").getChildren().get(0));
        MCRCondition cond = new MCRQueryParser().parse(condElem);

        return search(cond, sortBy, maxResults);
    }

    /**
     * Executes a query defined as MCRCondition object and returns the query
     * results.
     * 
     * @param cond
     *            the query condition
     * @param sortBy
     *            a List of MCRSortBy objects that defines the sort order of the
     *            results, may be null
     * @param maxResults
     *            the maximum number of results to return, a value &lt; 1 means
     *            to return all results.
     * @return the query results
     */
    public static MCRResults search(MCRCondition cond, List sortBy, int maxResults) {
        String index = getIndex(cond);
        if (index == null) {
            throw new IllegalArgumentException("Querying multiple indexes not yet supported!");
        }

        MCRSearcher searcher = MCRSearcherFactory.getSearcherForIndex(index);
        MCRResults results = searcher.search(cond, sortBy, maxResults);

        // Sort results if not already sorted
        if (!results.isSorted())
            results.sortBy(sortBy);

        return results;
    }

    private static String getIndex(MCRCondition cond) {
        if (cond instanceof MCRQueryCondition)
            return ((MCRQueryCondition) cond).getField().getIndex();
        else if (cond instanceof MCRNotCondition)
            return getIndex(((MCRNotCondition) cond).getChild());

        List children = null;
        if (cond instanceof MCRAndCondition)
            children = ((MCRAndCondition) cond).getChildren();
        else
            children = ((MCROrCondition) cond).getChildren();

        String index = getIndex((MCRCondition) (children.get(0)));
        for (int i = 1; i < children.size(); i++) {
            String other = getIndex((MCRCondition) (children.get(i)));
            if (!index.equals(other))
                return null; // mixed indexes here!
        }
        return index;
    }
}
