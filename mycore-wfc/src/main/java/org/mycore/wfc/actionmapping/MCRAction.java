/*
 * $Id$
 * $Revision: 5697 $ $Date: 15.03.2012 $
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

package org.mycore.wfc.actionmapping;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.mycore.parsers.bool.MCRCondition;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "action")
public class MCRAction {

    @XmlAttribute
    private String action;

    @XmlElement(name = "when")
    private MCRDecision[] decisions;

    public String getURL(MCRWorkflowData workflowData) {
        for (MCRDecision decision : decisions) {
            @SuppressWarnings("unchecked")
            MCRCondition<Object> cond = (MCRCondition<Object>) decision.getCondition();
            if (cond.evaluate(workflowData)) {
                return decision.getUrl();
            }
        }
        return null;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public MCRDecision[] getDecisions() {
        return decisions;
    }

    public void setDecisions(MCRDecision... decisions) {
        this.decisions = decisions;
    }
}
