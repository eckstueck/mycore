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

package org.mycore.common.xml;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.transform.TransformerException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.jdom.Attribute;
import org.jdom.Comment;
import org.jdom.Content;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.ProcessingInstruction;
import org.jdom.Text;
import org.jdom.Verifier;
import org.jdom.transform.JDOMSource;
import org.mycore.common.MCRException;
import org.mycore.datamodel.ifs2.MCRContent;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * This class provides some static utility methods to deal with XML/DOM
 * elements, nodes etc.
 * 
 * @author Detlev Degenhardt
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 */
public class MCRXMLHelper {

    /**
     * Parses an XML file from a URI and returns it as DOM. Use the validation
     * value from mycore.properties.
     * 
     * @param uri
     *            the URI of the XML file
     * @throws MCRException
     *             if XML could not be parsed
     * @return the XML file as a DOM object
     * @throws SAXParseException 
     * @deprecated use MCRXMLParserFactory.getParser().parseXML(MCRContent xml)
     */
    public static Document parseURI(URI uri) throws MCRException, SAXParseException {
        try {
            return MCRXMLParserFactory.getParser().parseXML(MCRContent.readFrom(uri.toURL()));
        } catch (MalformedURLException e) {
            throw new MCRException(e);
        } catch (IOException e) {
            throw new MCRException(e);
        }
    }

    /**
     * Parses an XML file from a URI and returns it as DOM. Use the given
     * validation flag.
     * 
     * @param uri
     *            the URI of the XML file
     * @param valid
     *            the validation flag
     * @throws MCRException
     *             if XML could not be parsed
     * @return the XML file as a DOM object
     * @throws SAXParseException 
     * @deprecated use MCRXMLParserFactory.getParser(boolean validate).parseXML(MCRContent xml)
     */
    public static Document parseURI(URI uri, boolean valid) throws MCRException, SAXParseException {
        try {
            return MCRXMLParserFactory.getParser(valid).parseXML(MCRContent.readFrom(uri));
        } catch (MalformedURLException e) {
            throw new MCRException(e);
        } catch (IOException e) {
            throw new MCRException(e);
        }
    }

    /**
     * Parses an XML String and returns it as DOM. Use the validation value from
     * mycore.properties.
     * 
     * @param xml
     *            the XML String to be parsed
     * @throws MCRException
     *             if XML could not be parsed
     * @return the XML file as a DOM object
     * @throws SAXParseException 
     * @deprecated use MCRXMLParserFactory.getParser().parseXML(MCRContent xml)
     */
    public static Document parseXML(String xml) throws MCRException, SAXParseException {
        try {
            return MCRXMLParserFactory.getParser().parseXML(MCRContent.readFrom(xml));
        } catch (UnsupportedEncodingException e) {
            throw new MCRException(e);
        } catch (IOException e) {
            throw new MCRException(e);
        }
    }

    /**
     * Parses an XML String and returns it as DOM. Use the given validation
     * flag.
     * 
     * @param xml
     *            the XML String to be parsed
     * @param valid
     *            the validation flag
     * @throws MCRException
     *             if XML could not be parsed
     * @return the XML file as a DOM object
     * @throws SAXParseException 
     * @deprecated use MCRXMLParserFactory.getParser(boolean validate).parseXML(MCRContent xml)
     */
    public static Document parseXML(String xml, boolean valid) throws MCRException, SAXParseException {
        try {
            return MCRXMLParserFactory.getParser(valid).parseXML(MCRContent.readFrom(xml));
        } catch (UnsupportedEncodingException e) {
            throw new MCRException(e);
        } catch (IOException e) {
            throw new MCRException(e);
        }
    }

    /**
     * Parses an Byte Array and returns it as DOM. Use the validation value from
     * mycore.properties.
     * 
     * @param xml
     *            the XML Byte Array to be parsed
     * @throws MCRException
     *             if XML could not be parsed
     * @return the XML file as a DOM object
     * @throws SAXParseException 
     * @deprecated use MCRXMLParserFactory.getParser().parseXML(MCRContent xml)
     */
    public static Document parseXML(byte[] xml) throws MCRException, SAXParseException {
        try {
            return MCRXMLParserFactory.getParser().parseXML(MCRContent.readFrom(xml, null));
        } catch (IOException e) {
            throw new MCRException(e);
        }
    }

    /**
     * Parses an Byte Array and returns it as DOM. Use the given validation
     * flag.
     * 
     * @param xml
     *            the XML Byte Array to be parsed
     * @param valid
     *            the validation flag
     * @throws MCRException
     *             if XML could not be parsed
     * @return the XML file as a DOM object
     * @throws SAXParseException 
     * @deprecated use MCRXMLParserFactory.getParser(boolean validate).parseXML(MCRContent xml)
     */
    public static Document parseXML(byte[] xml, boolean valid) throws MCRException, SAXParseException {
        try {
            return MCRXMLParserFactory.getParser(valid).parseXML(MCRContent.readFrom(xml, null));
        } catch (IOException e) {
            throw new MCRException(e);
        }
    }

    /**
     * Removes characters that are illegal in XML text nodes or attribute
     * values.
     * 
     * @param text
     *            the String that should be used in XML elements or attributes
     * @return the String with all illegal characters removed
     */
    public static String removeIllegalChars(String text) {
        if (text == null || text.trim().length() == 0) {
            return text;
        }
        if (org.jdom.Verifier.checkCharacterData(text) == null) {
            return text;
        }

        // It seems we have to filter out invalid XML characters...
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < text.length(); i++) {
            if (Verifier.isXMLCharacter(text.charAt(i))) {
                sb.append(text.charAt(i));
            }
        }
        return sb.toString();
    }

    /**
     * validates <code>doc</code> using XML Schema defined <code>schemaURI</code>
     * @param doc document to be validated
     * @param schemaURI URI of XML Schema document
     * @throws SAXException if validation fails
     * @throws IOException if resolving resources fails
     */
    public static void validate(Document doc, String schemaURI) throws SAXException, IOException {
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema;
        try {
            schema = sf.newSchema(MCRURIResolver.instance().resolve(schemaURI, null));
        } catch (TransformerException e) {
            Throwable cause = e.getCause();
            if (cause == null) {
                throw new IOException(e);
            }
            if (cause instanceof SAXException) {
                throw (SAXException) cause;
            }
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw new IOException(e);
        }
        Validator validator = schema.newValidator();
        validator.validate(new JDOMSource(doc));
    }

    /**
     * checks whether two documents are equal.
     * 
     * This test performs a deep check across all child components of a
     * Document.
     * 
     * @param d1
     *            first Document to compare
     * @param d2
     *            second Document to compare
     * @return true, if d1 and d2 are deep equal
     * @see Document#equals(java.lang.Object)
     */
    public static boolean deepEqual(Document d1, Document d2) {
        return JDOMEquivalent.equivalent(d1, d2);
    }

    /**
     * checks whether two elements are equal.
     * 
     * This test performs a deep check across all child components of a
     * element.
     * 
     * @param e1
     *            first Document to compare
     * @param e2
     *            second Document to compare
     * @return true, if e1 and e2 are deep equal
     * @see Document#equals(java.lang.Object)
     */
    public static boolean deepEqual(Element e1, Element e2) {
        return JDOMEquivalent.equivalent(e1, e2);
    }

    private static class JDOMEquivalent {

        private JDOMEquivalent() {
        }

        public static boolean equivalent(Document d1, Document d2) {
            return equivalentContent(d1, d2);
        }

        @SuppressWarnings("unchecked")
        public static boolean equivalent(Element e1, Element e2) {
            return equivalentName(e1, e2) && equivalentAttributes(e1, e2) && equivalentContent(e1.getContent(), e2.getContent());
        }

        public static boolean equivalent(Text t1, Text t2) {
            String v1 = t1.getValue();
            String v2 = t2.getValue();
            return v1.equals(v2);
        }

        public static boolean equivalent(DocType d1, DocType d2) {
            return (d1.getPublicID() == d2.getPublicID() || d1.getPublicID().equals(d2.getPublicID()))
                && (d1.getSystemID() == d2.getSystemID() || d1.getSystemID().equals(d2.getSystemID()));
        }

        public static boolean equivalent(Comment c1, Comment c2) {
            String v1 = c1.getValue();
            String v2 = c2.getValue();
            return v1.equals(v2);
        }

        public static boolean equivalent(ProcessingInstruction p1, ProcessingInstruction p2) {
            String t1 = p1.getTarget();
            String t2 = p2.getTarget();
            String d1 = p1.getData();
            String d2 = p2.getData();
            return t1.equals(t2) && d1.equals(d2);
        }

        public static boolean equivalentAttributes(Element e1, Element e2) {
            @SuppressWarnings("unchecked")
            List<Attribute> aList1 = e1.getAttributes();
            @SuppressWarnings("unchecked")
            List<Attribute> aList2 = e2.getAttributes();
            if (aList1.size() != aList2.size()) {
                return false;
            }
            HashSet<String> orig = new HashSet<String>(aList1.size());
            for (Attribute attr : aList1) {
                orig.add(attr.toString());
            }
            for (Attribute attr : aList1) {
                orig.remove(attr.toString());
            }
            return orig.size() == 0;
        }

        @SuppressWarnings("unchecked")
        public static boolean equivalentContent(Document d1, Document d2) {
            return equivalentContent(d1.getContent(), d2.getContent());
        }

        public static boolean equivalentContent(List<Content> l1, List<Content> l2) {
            if (l1.size() != l2.size()) {
                return false;
            }
            boolean result = true;
            Iterator<Content> i1 = l1.iterator();
            Iterator<Content> i2 = l2.iterator();
            while (result && i1.hasNext() && i2.hasNext()) {
                Object o1 = i1.next();
                Object o2 = i2.next();
                if (o1 instanceof Element && o2 instanceof Element) {
                    result = equivalent((Element) o1, (Element) o2);
                } else if (o1 instanceof Text && o2 instanceof Text) {
                    result = equivalent((Text) o1, (Text) o2);
                } else if (o1 instanceof Comment && o2 instanceof Comment) {
                    result = equivalent((Comment) o1, (Comment) o2);
                } else if (o1 instanceof ProcessingInstruction && o2 instanceof ProcessingInstruction) {
                    result = equivalent((ProcessingInstruction) o1, (ProcessingInstruction) o2);
                } else if (o1 instanceof DocType && o2 instanceof DocType) {
                    result = equivalent((DocType) o1, (DocType) o2);
                } else {
                    result = false;
                }
            }
            return result;
        }

        public static boolean equivalentName(Element e1, Element e2) {
            Namespace ns1 = e1.getNamespace();
            String localName1 = e1.getName();

            Namespace ns2 = e2.getNamespace();
            String localName2 = e2.getName();

            return ns1.equals(ns2) && localName1.equals(localName2);
        }
    }
}
