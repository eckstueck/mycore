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

package org.mycore.datamodel.ifs;

import org.mycore.common.*;

/**
 * Decides which MCRContentIndexer implementation should be used to build the
 * index of a given file content type. The system configuration sets the 
 * MCRContentIndexerSelector implementation that is used to make this decision
 * by the property "MCR.IFS.ContentIndexerSelector.Class". MyCoRe provides
 * a simple implementation of this interface in the class 
 * MCRSimpleContentIndexerDetector that decides based on the file content type.
 *
 * @author Harald Richter
 * @version $Revision$ $Date$
 */
public interface MCRContentIndexerDetector
{
  /**
   * Returns the ID of the file content indexer to be used to build
   * the index of the given file. The index selector can make the
   * decision based on the properties of the given file content type
   * or based on system configuration.
   **/
  public String getIndexer( String fct ) throws MCRException;
}
