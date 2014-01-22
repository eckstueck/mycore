/*
 * $Revision$ 
 * $Date$
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

package org.mycore.frontend.xeditor;

import static org.junit.Assert.*;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.junit.Test;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRXPathBuilderTest {

    @Test
    public void testXPath() {
        Element root = new Element("root");
        Element title1 = new Element("title");
        Element title2 = new Element("title");
        Element author = new Element("contributor");
        Attribute role = new Attribute("role", "author");
        Attribute lang = new Attribute("lang", "fodeo", Namespace.XML_NAMESPACE);
        author.setAttribute(role);
        author.setAttribute(lang);
        root.addContent(title1);
        root.addContent(author);
        root.addContent(title2);
        new Document(root);

        assertEquals("/root", MCRXPathBuilder.buildXPath(root));
        assertEquals("/root/contributor", MCRXPathBuilder.buildXPath(author));
        assertEquals("/root/title", MCRXPathBuilder.buildXPath(title1));
        assertEquals("/root/title[2]", MCRXPathBuilder.buildXPath(title2));
        assertEquals("/root/contributor/@role", MCRXPathBuilder.buildXPath(role));
        assertEquals("/root/contributor/@xml:lang", MCRXPathBuilder.buildXPath(lang));
        
        root.detach();
        assertEquals("root", MCRXPathBuilder.buildXPath(root));
        assertEquals("root/contributor", MCRXPathBuilder.buildXPath(author));
    }
}
