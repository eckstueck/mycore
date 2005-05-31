/*
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
 */

package org.mycore.backend.hibernate.tables;

public class MCRCLASSLABEL{

    private MCRCLASSLABELPK key;
    private String text;
    private String mcrdesc;
    
    private String id;
    private String lang;

    /**
    * @hibernate.property
    * column="Primary Key"
    * not-null="true"
    * update="true"
    */
    public MCRCLASSLABELPK getKey() {
        return key;
    }
    public void setKey(MCRCLASSLABELPK key) {
        this.key = key;
    }

    /**
     * @hibernate.property
     * column="ID"
     * not-null="true"
     * update="true"
     */
    public String getId() {
        return key.getId();
    }
    public void setId(String id) {
        key.setId(id);
    }
    
    /**
     * @hibernate.property
     * column="LANG"
     * not-null="true"
     * update="true"
     */
    public String getLang() {
        return key.getLang();
    }
    public void setLang(String lang) {
        key.setLang(lang);
    }
    
    /**
    * @hibernate.property
    * column="TEXT"
    * not-null="true"
    * update="true"
    */
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }

    /**
    * @hibernate.property
    * column="MCRDESC"
    * not-null="true"
    * update="true"
    */
    public String getMcrdesc() {
        return mcrdesc;
    }
    public void setMcrdesc(String mcrdesc) {
        this.mcrdesc = mcrdesc;
    }
}
