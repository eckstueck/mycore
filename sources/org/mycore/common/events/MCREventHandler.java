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

package org.mycore.common.events;

import org.mycore.common.*;

/**
 * Objects that implement this interface can react when some kind of predefined
 * event happens in MyCoRe. Implementing classes are registered using the
 * configuration property MCR.EventHandler.X.class where X is a number starting
 * from 1. Event handlers are called in the same order as they are registered in
 * the properties files.
 * 
 * @author Frank Lützenkirchen
 */
public interface MCREventHandler {
    /**
     * Handles an event. The handler is responsible for filtering the event type
     * it is interested in and wants to react on.
     * 
     * @param evt
     *            the Event object containing information about the event
     */
    public void doHandleEvent(MCREvent evt) throws MCRException;
}