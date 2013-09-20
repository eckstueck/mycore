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

import org.jaxen.JaxenException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Parent;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRBinding {

    private final static Logger LOGGER = Logger.getLogger(MCRBinding.class);

    protected String name;

    protected List<Object> boundNodes = new ArrayList<Object>();

    protected MCRBinding parent;

    protected List<MCRBinding> children = new ArrayList<MCRBinding>();

    public MCRBinding(Document document) throws JDOMException {
        this.boundNodes.add(document);
    }

    private MCRBinding(MCRBinding parent) {
        this.parent = parent;
        parent.children.add(this);
    }

    public MCRBinding(String xPath, MCRBinding parent) throws JDOMException, JaxenException {
        this(parent);
        bind(xPath, null);
    }

    public MCRBinding(String xPath, String defaultValue, String name, MCRBinding parent) throws JDOMException, JaxenException {
        this(parent);
        this.name = (name != null) && !name.isEmpty() ? name : null;
        bind(xPath, defaultValue);
    }

    private void bind(String xPath, String defaultValue) throws JaxenException {
        Map<String, Object> variables = buildXPathVariables();

        XPathExpression<Object> xPathExpr = XPathFactory.instance().compile(xPath, Filters.fpassthrough(), variables,
                MCRUsedNamespaces.getNamespaces());

        boundNodes.addAll(xPathExpr.evaluate(parent.getBoundNodes()));

        LOGGER.debug("Bind to " + xPath + " selected " + boundNodes.size() + " node(s)");

        if (boundNodes.isEmpty()) {
            Object built = new MCRNodeBuilder(variables).buildNode(xPath, defaultValue, (Parent) (parent.getBoundNode()));
            LOGGER.debug("Bind to " + xPath + " generated node " + MCRXPathBuilder.buildXPath(built));
            boundNodes.add(built);
        }
    }

    public MCRBinding(int pos, MCRBinding parent) {
        this(parent);
        boundNodes.add(parent.getBoundNodes().get(pos - 1));
        LOGGER.debug("Repeater bind to child [" + pos + "]");
    }

    public List<Object> getBoundNodes() {
        return boundNodes;
    }

    public Object getBoundNode() {
        return boundNodes.get(0);
    }

    public Element cloneBoundElement(int index) {
        Element template = (Element) (boundNodes.get(index));
        Element newElement = template.clone();
        Element parent = template.getParentElement();
        int indexInParent = parent.indexOf(template) + 1;
        parent.addContent(indexInParent, newElement);
        boundNodes.add(index + 1, newElement);
        return newElement;
    }

    public void detachBoundNodes() {
        while (!boundNodes.isEmpty())
            detachBoundNode();
    }

    public void detachBoundNode() {
        Object node = getBoundNode();
        if (node instanceof Attribute)
            ((Attribute) node).detach();
        else
            ((Element) node).detach();
        boundNodes.remove(node);
    }

    public MCRBinding getParent() {
        return parent;
    }

    public List<MCRBinding> getAncestorsAndSelf() {
        List<MCRBinding> ancestors = new ArrayList<MCRBinding>();
        MCRBinding current = this;
        do {
            ancestors.add(0, current);
            current = current.getParent();
        } while (current != null);
        return ancestors;
    }

    private String getValue(Object node) {
        if (node instanceof Element)
            return ((Element) node).getTextTrim();
        else
            return ((Attribute) node).getValue();
    }

    public String getValue() {
        return getValue(getBoundNode());
    }

    public boolean hasValue(String value) {
        for (Object node : boundNodes)
            if (value.equals(getValue(node)))
                return true;

        return false;
    }

    public String getName() {
        return name;
    }

    public List<MCRBinding> getChildren() {
        return children;
    }

    public Map<String, Object> buildXPathVariables() {
        Map<String, Object> variables = new HashMap<String, Object>();
        for (MCRBinding ancestor : getAncestorsAndSelf()) {
            for (MCRBinding child : ancestor.getChildren()) {
                String childName = child.getName();
                if (childName != null)
                    variables.put(childName, child.getBoundNodes());
            }
        }
        return variables;
    }

    public String getAbsoluteXPath() {
        return MCRXPathBuilder.buildXPath(getBoundNode());
    }
}
