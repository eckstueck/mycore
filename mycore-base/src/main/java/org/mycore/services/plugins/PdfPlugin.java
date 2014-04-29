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

package org.mycore.services.plugins;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashSet;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.datamodel.ifs.MCRFileContentType;
import org.mycore.datamodel.ifs.MCRFileContentTypeFactory;

/**
 * Provide some info about your class!
 * 
 * @author Thomas Scheffler (yagee)
 */
public class PdfPlugin implements TextFilterPlugin {
    private static final Logger LOGGER = Logger.getLogger(PdfPlugin.class);
    private static HashSet<MCRFileContentType> contentTypes = null;

    private static final String name = "Yagee's amazing PDF Filter";

    private static final int MAJOR = 0;

    private static final int MINOR = 7;

    private static String info = null;

    private static String p2t_info = null;

    private static final String textencoding = "UTF-8";

    /**
     * 
     */
    public PdfPlugin() {
        super();

        if (contentTypes == null) {
            contentTypes = new HashSet<MCRFileContentType>();

            if (MCRFileContentTypeFactory.isTypeAvailable("pdf")) {
                contentTypes.add(MCRFileContentTypeFactory.getType("pdf"));
            }
        }

        if (p2t_info == null && !pdftotext()) {
            throw new FilterPluginInstantiationException("The execution of \"pdftotext\" failed." + "Maybe it's not installed or in your search path!\n" + "To use this Plugin you have to install XPdf" + "http://www.foolabs.com/xpdf/) and ensure " + "the pdftotext binary is in your search path.\n" + "Another reason maybe that you are using a version that" + " is not compatible with this Plugin:\n" + getName() + " v" + MAJOR + '.' + MINOR);
        }

        if (info == null) {
            info = "This filter uses XPDF for transformation." + "\nSource code is available on http://www.foolabs.com/xpdf/" + "\nCurrently using: " + p2t_info;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.services.plugins.TextFilterPlugin#getName()
     */
    public String getName() {
        return name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.services.plugins.TextFilterPlugin#getInfo()
     */
    public String getInfo() {
        return info;
    }

    private boolean pdftotext() {
        int rc;
        final String[] testcommand = { "pdftotext", "-v" };
        String s;
        StringBuilder infofetch = new StringBuilder();

        try {
            Process p = Runtime.getRuntime().exec(testcommand);
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            while ((s = stdError.readLine()) != null) {
                infofetch.append(s).append(", ");
            }

            rc = p.waitFor();
            p2t_info = infofetch.deleteCharAt(infofetch.length() - 2).toString();
            LOGGER.debug("PdfPlugin availability check: rc="+rc+", p2t_info="+p2t_info);
        } catch (IOException e) {
            if (e.getMessage().indexOf("not found") > 0) {
                //NOTE: It is a ugly pain to parse a error message, but at worst we throw the wrong error message
                throw new FilterPluginInstantiationException(testcommand[0] + " is not installed or in search path!\n" + "To use this Plugin you have to install XPdf" + "http://www.foolabs.com/xpdf/) and ensure " + "the pdftotext binary is in your search path.", e);
            }
            throw new FilterPluginInstantiationException("Error while excuting " + testcommand, e);
        } catch (InterruptedException e) {
            throw new FilterPluginInstantiationException("Error while excuting " + testcommand, e);
        }

        return rc == 99 || rc == 0;
    }

    private boolean pdftotext(File pdffile, File txtfile) {
        int rc;
        final String[] testcommand = { "pdftotext", "-enc", textencoding, "-raw", pdffile.getAbsolutePath(), txtfile.getAbsolutePath() };
        String s;

        try {
            StringBuffer sb = new StringBuffer();

            for (String element : testcommand) {
                sb.append(element).append(' ');
            }

            System.err.println(sb);

            Process p = Runtime.getRuntime().exec(testcommand);
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            while ((s = stdError.readLine()) != null) {
                System.err.println(s);
            }

            rc = p.waitFor();
        } catch (IOException e) {
            if (e.getMessage().indexOf("not found") > 0) {
                //NOTE: It is a ugly pain to parse a error message, but at worst we throw the wrong error message
                throw new MCRConfigurationException(testcommand[0] + " is not installed or in search path!", e);
            }
            throw new MCRConfigurationException("Error while excuting " + testcommand, e);
        } catch (InterruptedException e) {
            throw new MCRConfigurationException("Error while excuting " + testcommand, e);
        }

        return rc == 00;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.services.plugins.TextFilterPlugin#getSupportedContentTypes()
     */
    public HashSet<MCRFileContentType> getSupportedContentTypes() {
        return contentTypes;
    }

    public Reader transform(MCRFileContentType ct, InputStream input) throws FilterPluginTransformException {
        if (!getSupportedContentTypes().contains(ct)) {
            throw new FilterPluginTransformException("ContentType " + ct + " is not supported by " + getName() + "!");
        }

        try {
            System.err.println("===== PDF decoding starts ====");

            File pdffile = File.createTempFile("inp", ".pdf");
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(pdffile));
            pdffile.deleteOnExit();
            IOUtils.copy(input, out);
            out.close();

            File txtfile = File.createTempFile("out", ".txt");
            txtfile.deleteOnExit();

            if (!pdftotext(pdffile, txtfile)) {
                throw new FilterPluginTransformException("pdftotext reported an error while exporting text of PDF file!");
            }

            pdffile.delete();

            FileInputStream fin = new FileInputStream(txtfile);

            return new InputStreamReader(fin, textencoding);
        } catch (FileNotFoundException e) {
            throw new FilterPluginTransformException("File was not found!", e);
        } catch (IOException e) {
            throw new FilterPluginTransformException("General I/O Exception occured", e);
        }
    }

    /**
     * @see org.mycore.services.plugins.TextFilterPlugin#getMajorNumber()
     */
    public int getMajorNumber() {
        return MAJOR;
    }

    /**
     * @see org.mycore.services.plugins.TextFilterPlugin#getMinorNumber()
     */
    public int getMinorNumber() {
        return MINOR;
    }
}