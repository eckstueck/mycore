/*
 * $Id$
 * $Revision: 5697 $ $Date: 30.09.2010 $
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

import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRObjectIDPoolTest extends MCRTestCase {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
        setProperty("MCR.Metadata.Type.test", Boolean.TRUE.toString(), true);
    }

    @Test
    public void getInstance() {
        System.gc();
        System.runFinalization();
        int before = MCRObjectIDPool.getSize();
        String id = "MyCoRe_test_11111111";
        @SuppressWarnings("unused")
        MCRObjectID mcrId = MCRObjectIDPool.getMCRObjectID(id);
        assertEquals("ObjectIDPool size is different", before + 1, MCRObjectIDPool.getSize());
        mcrId = null;
        System.gc();
        System.runFinalization();
        assertEquals("ObjectIDPool size is different", before, MCRObjectIDPool.getSize());
    }
}
