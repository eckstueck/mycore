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

package org.mycore.common.events;

/**
 * Represents an event that occured in the MyCoRe system. Events are of a
 * predefined event type like create, update, delete and an object type like
 * object or file. They can be handled by MCREventHandler implementations.
 * Events are automatically created by some MyCoRe components and are forwarded
 * to the handlers by MCREventManager.
 * 
 * @author Frank Lützenkirchen
 */
public class MCREvent extends java.util.Hashtable<String, Object> {
    /**
     * Default version ID
     */
    private static final long serialVersionUID = 1L;

    /** Pre-defined event types * */
    final static public String CREATE_EVENT = "create";

    final static public String UPDATE_EVENT = "update";

    final static public String DELETE_EVENT = "delete";

    final static public String REPAIR_EVENT = "repair";

    final static public String INDEX_EVENT = "index";

    final static public String OBJECT_TYPE = "MCRObject";

    final static public String DERIVATE_TYPE = "MCRDerivate";

    final static public String CLASS_TYPE = "MCRClassification";

    final static public String PATH_TYPE = "MCRPath";

    final public static String MOVE_EVENT = "move";

    final static public String PATH_KEY = PATH_TYPE;

    final static public String FILEATTR_KEY = PATH_TYPE + ":attr";

    final static public String OBJECT_KEY = "object";

    final static public String OBJECT_OLD_KEY = "object.old";

    final static public String DERIVATE_KEY = "derivate";

    final static public String DERIVATE_OLD_KEY = "derivate.old";

    /** The object type like object or file * */
    private String objType;

    /** The event type like create, update or delete * */
    private String evtType;

    /**
     * Creates a new event object of the given object type (object, file) and
     * event type (create, update, delete)
     */
    public MCREvent(String objType, String evtType) {
        this.objType = objType;
        this.evtType = evtType;
    }

    /**
     * Returns the object type of this event
     * 
     * @return the object type of this event
     */
    public String getObjectType() {
        return objType;
    }

    /**
     * Returns the event type of this event
     * 
     * @return the event type of this event
     */
    public String getEventType() {
        return evtType;
    }
}
