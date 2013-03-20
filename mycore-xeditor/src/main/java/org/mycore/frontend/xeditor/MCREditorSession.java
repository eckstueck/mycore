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

import java.text.ParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCREditorSession {

    private final static Logger LOGGER = Logger.getLogger(MCREditorSession.class);

    private String id;

    private Document editedXML;

    private Set<String> xPathsOfDisplayedFields = new HashSet<String>();

    public void setID(String id) {
        this.id = id;
    }

    public String getID() {
        return id;
    }

    public void setEditedXML(Document xml) throws JDOMException {
        editedXML = xml;
    }

    public Document getEditedXML() {
        return editedXML;
    }

    public void markAsTransformedToInputField(Object node) {
        String xPath = MCRXPathBuilder.buildXPath(node);
        LOGGER.debug(id + " uses " + xPath);
        xPathsOfDisplayedFields.add(xPath);
    }

    public void markAsResubmittedFromInputField(Object node) {
        String xPath = MCRXPathBuilder.buildXPath(node);
        LOGGER.debug(id + " set value of " + xPath);
        xPathsOfDisplayedFields.remove(xPath);
    }

    public void setSubmittedValues(String xPath, String[] values ) throws JDOMException, ParseException {
        MCRBinding rootBinding = new MCRBinding(editedXML);
        MCRBinding binding = new MCRBinding(xPath, rootBinding);
        List<Object> boundNodes = binding.getBoundNodes();

        while (boundNodes.size() < values.length) {
            Element newElement = binding.cloneLastBoundElement();
            markAsTransformedToInputField(newElement);
        }

        for (int i = 0; i < values.length; i++) {
            if ((values[i] != null) && (!values[i].trim().isEmpty())) {
                binding.setValue(i, values[i].trim());
                markAsResubmittedFromInputField(boundNodes.get(i));
            }
        }
    }
    
    public void removeDeletedNodes() throws JDOMException, ParseException {
        MCRBinding root = new MCRBinding(editedXML);
        for (String xPath : xPathsOfDisplayedFields)
            new MCRBinding(xPath, root).detachBoundNodes();
        xPathsOfDisplayedFields.clear();
    }
}
