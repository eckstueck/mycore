/*
 * 
 * $Revision: 13085 $ $Date: 19.03.2010 23:42:07 $
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
package org.mycore.datamodel.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;

import org.jdom.Document;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mycore.common.MCRException;
import org.mycore.common.MCRTestCase;
import org.mycore.common.xml.MCRXMLHelper;
import org.xml.sax.SAXParseException;

/**
 * @author Thomas Scheffler
 *
 */
public class MCRObjectMetadataTest extends MCRTestCase {
    private static final String TEST_OBJECT_RESOURCE_NAME = "/mcr_test_01.xml";
    private Document testObjectDocument;
    private MCRObjectMetadata testMetadata;

    /* (non-Javadoc)
     * @see org.mycore.common.MCRTestCase#setUp()
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        setProperty("MCR.Metadata.DefaultLang", "de", true);
        testObjectDocument = loadResourceDocument(TEST_OBJECT_RESOURCE_NAME);
        testMetadata = new MCRObjectMetadata();
        testMetadata.setFromDOM(testObjectDocument.getRootElement().getChild("metadata"));
    }

    /**
     * Test method for {@link org.mycore.datamodel.metadata.MCRObjectMetadata#size()}.
     */
    @Test
    public void size() {
        assertEquals("Expected just one metadata entry", 1, testMetadata.size());
    }

    /**
     * Test method for {@link org.mycore.datamodel.metadata.MCRObjectMetadata#getMetadataTagName(int)}.
     */
    @Test
    public void getMetadataTagName() {
        assertEquals("Metadata tag is not 'def.textfield'", "def.textfield", testMetadata.getMetadataTagName(0));
    }

    /**
     * Test method for {@link org.mycore.datamodel.metadata.MCRObjectMetadata#getHeritableMetadata()}.
     */
    @Test
    public void getHeritableMetadata() {
        assertEquals("Did not find any heritable metadata", 1, testMetadata.getHeritableMetadata().size());
    }

    /**
     * Test method for {@link org.mycore.datamodel.metadata.MCRObjectMetadata#removeInheritedMetadata()}.
     */
    @Test
    public void removeInheritedMetadata() {
        testMetadata.removeInheritedMetadata();
        assertEquals("Did not expect removal of any metadata", 1, testMetadata.size());
    }

    /**
     * Test method for {@link org.mycore.datamodel.metadata.MCRObjectMetadata#appendMetadata(org.mycore.datamodel.metadata.MCRObjectMetadata)}.
     */
    @Test
    public void appendMetadata() {
        MCRObjectMetadata meta2 = getDateObjectMetadata();
        testMetadata.appendMetadata(meta2);
        assertEquals("Expected 2 metadates", 2, testMetadata.size());
    }

    private MCRObjectMetadata getDateObjectMetadata() {
        MCRObjectMetadata meta2 = new MCRObjectMetadata();
        MCRMetaISO8601Date date = new MCRMetaISO8601Date("def.datefield", "datefield", "test", 0);
        date.setDate(new Date());
        MCRMetaElement el2 = new MCRMetaElement();
        el2.addMetaObject(date);
        el2.setClassName(MCRMetaISO8601Date.class.getSimpleName());
        el2.setHeritable(true);
        el2.setTag(date.datapart);
        meta2.setMetadataElement(el2, date.datapart);
        return meta2;
    }

    /**
     * Test method for {@link org.mycore.datamodel.metadata.MCRObjectMetadata#getMetadataElement(java.lang.String)}.
     */
    @Test
    public void getMetadataElementString() {
        assertEquals("did not get correct MCRMetaElement instance", testMetadata.getMetadataElement(0), testMetadata.getMetadataElement("def.textfield"));
    }

    /**
     * Test method for {@link org.mycore.datamodel.metadata.MCRObjectMetadata#getMetadataElement(int)}.
     */
    @Test
    public void getMetadataElementInt() {
        assertEquals("did not get correct MCRMetaElement instance", testMetadata.getMetadataElement("def.textfield"), testMetadata.getMetadataElement(0));
    }

    /**
     * Test method for {@link org.mycore.datamodel.metadata.MCRObjectMetadata#setMetadataElement(org.mycore.datamodel.metadata.MCRMetaElement, java.lang.String)}.
     */
    @Test
    @Ignore("not implemented")
    public void setMetadataElement() {
        fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link org.mycore.datamodel.metadata.MCRObjectMetadata#removeMetadataElement(java.lang.String)}.
     */
    @Test
    @Ignore("not implemented")
    public void removeMetadataElementString() {
        fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link org.mycore.datamodel.metadata.MCRObjectMetadata#removeMetadataElement(int)}.
     */
    @Test
    @Ignore("not implemented")
    public void removeMetadataElementInt() {
        fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link org.mycore.datamodel.metadata.MCRObjectMetadata#createXML()}.
     */
    @Test
    @Ignore("not implemented")
    public void createXML() {
        fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link org.mycore.datamodel.metadata.MCRObjectMetadata#isValid()}.
     */
    @Test
    @Ignore("not implemented")
    public void isValid() {
        fail("Not yet implemented"); // TODO
    }

    private static Document loadResourceDocument(String resource) throws URISyntaxException, MCRException, SAXParseException {
        URL mcrTestUrl = MCRObjectMetadataTest.class.getResource(resource);
        Document xml = MCRXMLHelper.parseURI(mcrTestUrl.toURI(), true);
        return xml;
    }
}
