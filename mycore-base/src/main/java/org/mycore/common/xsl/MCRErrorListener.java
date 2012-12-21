/*
 * $Id$
 * $Revision: 5697 $ $Date: Nov 16, 2012 $
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

package org.mycore.common.xsl;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRErrorListener implements ErrorListener {
    private static Logger LOGGER = Logger.getLogger(MCRErrorListener.class);

    private static MCRErrorListener instance = new MCRErrorListener();

    public static MCRErrorListener getInstance() {
        return instance;
    }

    /* (non-Javadoc)
     * @see javax.xml.transform.ErrorListener#warning(javax.xml.transform.TransformerException)
     */
    @Override
    public void warning(TransformerException exception) throws TransformerException {
        exception = unwrapException(exception);
        LOGGER.warn("Exception while XSL transformation.", exception);
    }

    /* (non-Javadoc)
     * @see javax.xml.transform.ErrorListener#error(javax.xml.transform.TransformerException)
     */
    @Override
    public void error(TransformerException exception) throws TransformerException {
        exception = unwrapException(exception);
        LOGGER.error("Exception while XSL transformation.", exception);
        throw exception;
    }

    /* (non-Javadoc)
     * @see javax.xml.transform.ErrorListener#fatalError(javax.xml.transform.TransformerException)
     */
    @Override
    public void fatalError(TransformerException exception) throws TransformerException {
        exception = unwrapException(exception);
        LOGGER.fatal("Exception while XSL transformation.", exception);
        throw exception;
    }

    private TransformerException unwrapException(TransformerException exception) {
        TransformerException e = exception;
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof TransformerException) {
                return unwrapException((TransformerException) cause);
            }
            cause = cause.getCause();
        }
        return e;
    }

}