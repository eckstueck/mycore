/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.datamodel.classifications2.impl;

import java.net.URISyntaxException;
import java.net.URL;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.hibernate.criterion.Projections;
import org.jdom.Document;

import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRException;
import org.mycore.common.MCRHibTestCase;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.datamodel.classifications2.utils.MCRStringTransformer;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;

public class MCRCategoryDAOImplTest extends MCRHibTestCase {

    private static final String WORLD_CLASS_RESOURCE_NAME = "/org/mycore/datamodel/classifications2/impl/resources/worldclass.xml";

    static final String CATEGORY_MAPPING_RESOURCE_NAME = "/org/mycore/datamodel/classifications2/impl/MCRCategoryImpl.hbm.xml";

    private static final MCRCategoryDAOImpl DAO = new MCRCategoryDAOImpl();

    private MCRCategory category;

    @Override
    protected void setUp() throws Exception {
        // TODO Auto-generated method stub
        super.setUp();
        loadWorldClassification();
    }

    public void testCalculateLeftRightAndLevel() {
        MCRCategoryImpl co1 = new MCRCategoryImpl();
        co1.setId(MCRCategoryID.rootID("co1"));
        assertEquals(2, MCRCategoryDAOImpl.calculateLeftRightAndLevel(co1, 1, 0));
        assertEquals(0, co1.getLevel());
        MCRCategoryImpl co2 = new MCRCategoryImpl();
        co2.setId(new MCRCategoryID(co1.getId().getRootID(), "co2"));
        co1.getChildren().add(co2);
        assertEquals(4, MCRCategoryDAOImpl.calculateLeftRightAndLevel(co1, 1, 0));
        assertEquals(1, co2.getLevel());
        MCRCategoryImpl co3 = new MCRCategoryImpl();
        co3.setId(new MCRCategoryID(co1.getId().getRootID(), "co3"));
        co1.getChildren().add(co3);
        assertEquals(6, MCRCategoryDAOImpl.calculateLeftRightAndLevel(co1, 1, 0));
        assertEquals(1, co3.getLevel());
        MCRCategoryImpl co4 = new MCRCategoryImpl();
        co4.setId(new MCRCategoryID(co1.getId().getRootID(), "co4"));
        co3.getChildren().add(co4);
        assertEquals(8, MCRCategoryDAOImpl.calculateLeftRightAndLevel(co1, 1, 0));
        assertEquals(2, co4.getLevel());
    }

    public void testAddCategory() throws MCRException {
        DAO.addCategory(null, category);
        endTransaction();
        beginTransaction();
        // clear from cache
        sessionFactory.getCurrentSession().clear();
        assertTrue("Exist check failed for Category " + category.getId(), DAO.exist(category.getId()));
        MCRCategoryImpl india = new MCRCategoryImpl();
        india.setId(new MCRCategoryID(category.getId().getRootID(), "India"));
        india.setLabels(new HashSet<MCRLabel>());
        india.getLabels().add(new MCRLabel("de", "Indien", null));
        india.getLabels().add(new MCRLabel("en", "India", null));
        MCRHIBConnection.instance().flushSession();
        DAO.addCategory(new MCRCategoryID(category.getId().getRootID(), "Asia"), india);
        endTransaction();
        beginTransaction();
        // clear from cache
        sessionFactory.getCurrentSession().clear();
        assertTrue("Exist check failed for Category " + india.getId(), DAO.exist(india.getId()));
        MCRCategoryImpl rootCategory = getRootCategoryFromSession();
        assertEquals("Child category count does not match.", category.getChildren().size(), rootCategory.getChildren().size());
        int allNodes = (Integer) sessionFactory.getCurrentSession().createCriteria(MCRCategoryImpl.class).setProjection(Projections.rowCount()).uniqueResult();
        // category + india
        assertEquals("Complete category count does not match.", countNodes(category) + 1, allNodes);
    }

    private MCRCategoryImpl getRootCategoryFromSession() {
        return (MCRCategoryImpl) sessionFactory.getCurrentSession().get(MCRCategoryImpl.class, ((MCRCategoryImpl) category).getInternalID());
    }

    public void testDeleteCategory() {
        DAO.addCategory(null, category);
        endTransaction();
        beginTransaction();
        // clear from cache
        sessionFactory.getCurrentSession().clear();
        DAO.deleteCategory(category.getId());
        endTransaction();
        beginTransaction();
        // clear from cache
        sessionFactory.getCurrentSession().clear();
        // check if classification is present
        assertFalse("Category is not deleted: " + category.getId(), DAO.exist(category.getId()));
        // check if any subcategory is present
        assertFalse("Category is not deleted: " + category.getChildren().get(0).getId(), DAO.exist(category.getChildren().get(0).getId()));
    }

    public void testGetCategoriesByLabel() {
        DAO.addCategory(null, category);
        endTransaction();
        beginTransaction();
        // clear from cache
        sessionFactory.getCurrentSession().clear();
        MCRCategory find = category.getChildren().get(0).getChildren().get(0);
        MCRCategory dontFind = category.getChildren().get(1);
        MCRLabel label = find.getLabels().iterator().next();
        List<MCRCategory> results = DAO.getCategoriesByLabel(category.getId(), label.getLang(), label.getText());
        assertFalse("No search results found", results.isEmpty());
        assertTrue("Could not find Category: " + find.getId(), results.get(0).getLabels().contains(label));
        assertTrue("No search result expected.", DAO.getCategoriesByLabel(dontFind.getId(), label.getLang(), label.getText()).isEmpty());
    }

    public void testGetCategory() {
        DAO.addCategory(null, category);
        endTransaction();
        beginTransaction();
        // clear from cache
        sessionFactory.getCurrentSession().clear();
        MCRCategory rootCategory = DAO.getCategory(category.getId(), 0);
        assertTrue("Children present with child Level 0.", rootCategory.getChildren().isEmpty());
        rootCategory = DAO.getCategory(category.getId(), 1);
        assertTrue("Children present with child Level 1.", rootCategory.getChildren().get(0).getChildren().isEmpty());
        assertEquals("Category count does not match with child Level 1.\n" + MCRStringTransformer.getString(rootCategory), category.getChildren().size(),
                rootCategory.getChildren().size());
        rootCategory = DAO.getCategory(category.getId(), -1);
        assertEquals("Did not get all categories." + MCRStringTransformer.getString(rootCategory), countNodes(category), countNodes(rootCategory));
    }

    public void testGetChildren() {
        DAO.addCategory(null, category);
        endTransaction();
        beginTransaction();
        // clear from cache
        sessionFactory.getCurrentSession().clear();
        List<MCRCategory> children = DAO.getChildren(category.getId());
        assertEquals("Did not get all children of :" + category.getId(), category.getChildren().size(), children.size());
        for (int i = 0; i < children.size(); i++) {
            assertEquals("Category IDs of children do not match.", category.getChildren().get(i).getId(), children.get(i).getId());
        }
    }

    public void testGetParents() {
        DAO.addCategory(null, category);
        endTransaction();
        beginTransaction();
        // clear from cache
        sessionFactory.getCurrentSession().clear();
        MCRCategory find = category.getChildren().get(0).getChildren().get(0);
        List<MCRCategory> parents = DAO.getParents(find.getId());
        MCRCategory findParent = find;
        for (MCRCategory parent : parents) {
            findParent = findParent.getParent();
            assertNotNull("Returned too much parents.", findParent);
            assertEquals("Parents did not match.", findParent.getId(), parent.getId());
        }
    }

    public void testGetRootCategory() {
        DAO.addCategory(null, category);
        endTransaction();
        beginTransaction();
        // clear from cache
        sessionFactory.getCurrentSession().clear();
        // Europe
        MCRCategory find = category.getChildren().get(0);
        MCRCategory rootCategory = DAO.getRootCategory(find.getId(), 0);
        assertEquals("Category count does not match.", 2, countNodes(rootCategory));
        assertEquals("Did not get root Category.", find.getRoot().getId(), rootCategory.getId());
        rootCategory = DAO.getRootCategory(find.getId(), -1);
        assertEquals("Category count does not match.", 1 + countNodes(find), countNodes(rootCategory));
    }

    public void testChildren() {
        DAO.addCategory(null, category);
        endTransaction();
        beginTransaction();
        // clear from cache
        sessionFactory.getCurrentSession().clear();
        assertTrue("Category '" + category.getId() + "' should have children.", DAO.hasChildren(category.getId()));
        assertFalse("Category '" + category.getChildren().get(1).getId() + "' shouldn't have children.", DAO.hasChildren(category.getChildren().get(1).getId()));
    }

    public void testMoveCategoryWithoutIndex() throws SQLException {
        DAO.addCategory(null, category);
        endTransaction();
        beginTransaction();
        // clear from cache
        sessionFactory.getCurrentSession().clear();
        checkLeftRightLevelValue(getRootCategoryFromSession(), 0, 0);
        endTransaction();
        beginTransaction();
        // clear from cache
        sessionFactory.getCurrentSession().clear();
        MCRCategory moveNode = category.getChildren().get(1);
        // Europe conquer Asia
        DAO.moveCategory(moveNode.getId(), category.getChildren().get(0).getId());
        endTransaction();
        beginTransaction();
        // clear from cache
        sessionFactory.getCurrentSession().clear();
        Connection connection = sessionFactory.getCurrentSession().connection();
        CallableStatement st=connection.prepareCall("SELECT internalId, ClassID, CategID, parentID, positionInParent, level, leftValue, rightValue FROM MCRCategory");
        ResultSet rs=st.executeQuery();
        while (rs.next()){
            System.out.printf("%d %s %s %d %d %d %d %d\n", rs.getInt(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getInt(5), rs.getInt(6), rs.getInt(7), rs.getInt(8));
        }
        MCRCategoryImpl rootNode = getRootCategoryFromSession();
        checkLeftRightLevelValue(rootNode, 0, 0);
    }

    /**
     * @throws URISyntaxException
     */
    private void loadWorldClassification() throws URISyntaxException {
        URL worlClassUrl = this.getClass().getResource(WORLD_CLASS_RESOURCE_NAME);
        Document xml = MCRXMLHelper.parseURI(worlClassUrl.toURI().toString());
        category = MCRXMLTransformer.getCategory(xml);
    }

    private static int countNodes(MCRCategory category) {
        int i = 1;
        for (MCRCategory child : category.getChildren()) {
            i += countNodes(child);
        }
        return i;
    }

    private int checkLeftRightLevelValue(MCRCategoryImpl node, int leftStart, int levelStart) {
        int curValue = leftStart;
        final int nextLevel = levelStart + 1;
        System.out.printf("Checking %s\n", node.getId());
        assertEquals("Left value did not match on ID: " + node.getId(), leftStart, node.getLeft());
        assertEquals("Level value did not match on ID: " + node.getId(), levelStart, node.getLevel());
        for (MCRCategory child : node.children) {
            curValue = checkLeftRightLevelValue((MCRCategoryImpl) child, ++curValue, nextLevel);
        }
        assertEquals("Right value did not match on ID: " + node.getId(), ++curValue, node.getRight());
        return curValue;
    }

    protected boolean isDebugEnabled() {
        return true;
    }

}
