/**
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
 **/

package org.mycore.access;

import java.util.Date;
import org.mycore.user.MCRUser;
import org.mycore.user.MCRGroup;

/**
 * Implementation of a (date &lt; x) clause
 * 
 * @author Matthias Kramm
 */

class MCRDateBeforeClause implements MCRAccessCtrlDefinition {
    private Date date;

    MCRDateBeforeClause(Date date) {
        this.date = date;
    }

    public boolean hasAccess(MCRUser user, Date date, MCRIPAddress ip) {
        return date.before(this.date);
    }

    public String toString() {
        return "date < " + date + "\n";
    }
};

