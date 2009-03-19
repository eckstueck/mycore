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

package org.mycore.datamodel.ifs2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.VFS;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRUsageException;
import org.mycore.common.MCRUtils;

/**
 * Used to read/write content from any source to any target. Sources and targets
 * can be strings, local files, Apache VFS file objects, XML documents, byte[]
 * arrays and streams. MCRContent can only be consumed once, otherwise the
 * getters throw MCRUsageException. Use makeCopies() to avoid this. The
 * underlying input stream is closed after consumption, excepted you use
 * getInputStream(), of course.
 * 
 * @author Frank Lützenkirchen
 */
public class MCRContent {

    /**
     * The content itself
     */
    protected InputStream in;

    /**
     * If true, this content already was used and cannot be used again
     */
    protected boolean consumed = false;

    /**
     * Creates content from a String, using UTF-8 encoding
     * 
     * @param text
     *            the content
     */
    public MCRContent(String text) throws IOException, UnsupportedEncodingException {
        this(text, "UTF-8");
    }

    /**
     * Creates content from a String, using the given encoding
     * 
     * @param text
     *            the content
     * @param encoding
     *            the encoding to be used to write bytes
     */
    public MCRContent(String text, String encoding) throws IOException, UnsupportedEncodingException {
        this(text.getBytes(encoding));
    }

    /**
     * Creates content from a local file
     * 
     * @param file
     *            the local file to read
     */
    public MCRContent(File file) throws IOException {
        this(new FileInputStream(file));
    }

    /**
     * Creates content from Apache VFS file object
     * 
     * @param fo
     *            the file object to read content from
     */
    public MCRContent(FileObject fo) throws FileSystemException {
        this(fo.getContent().getInputStream());
    }

    /**
     * Creates content from byte[] arrray
     * 
     * @param bytes
     *            the content's bytes
     */
    public MCRContent(byte[] bytes) throws IOException {
        this(new ByteArrayInputStream(bytes));
    }

    /**
     * Creates content from XML document. Content will be written
     * pretty-formatted, using UTF-8 encoding and line indentation.
     * 
     * @param xml
     *            the XML document to read in as content
     */
    public MCRContent(Document xml) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XMLOutputter xout = new XMLOutputter();
        xout.setFormat(Format.getPrettyFormat().setEncoding("UTF-8").setIndent("  "));
        xout.output(xml, out);
        out.close();
        in = new ByteArrayInputStream(out.toByteArray());
    }

    /**
     * Creates new content from input stream
     * 
     * @param in
     *            the input stream to read content from
     */
    public MCRContent(InputStream in) {
        this.in = in;
    }

    /**
     * Creates new content reading from the given URL.
     * 
     * @param url
     *            the url to read content from
     */
    public MCRContent(URL url) throws FileSystemException {
        this(VFS.getManager().resolveFile(url.toExternalForm()));
    }

    /**
     * Returns content as input stream. Be sure to close this stream properly!
     * 
     * @return input stream to read content from
     */
    public InputStream getInputStream() {
        checkConsumed();
        return in;
    }

    /**
     * Returns content as content input stream, which provides MD5
     * functionality. Be sure to close this stream properly!
     * 
     * @return the content input stream
     */
    public MCRContentInputStream getContentInputStream() {
        if (!(in instanceof MCRContentInputStream))
            in = new MCRContentInputStream(in);
        return (MCRContentInputStream) in;
    }

    /**
     * Sends content to the given OutputStream
     * 
     * @param out
     *            the OutputStream to write the content to
     */
    public void sendTo(OutputStream out) throws IOException {
        checkConsumed();
        MCRUtils.copyStream(in, out);
        in.close();
    }

    /**
     * Sends content to the given local file
     * 
     * @param target
     *            the file to write the content to
     */
    public void sendTo(File target) throws IOException {
        OutputStream out = new FileOutputStream(target);
        sendTo(out);
        out.close();
    }

    /**
     * Sends the content to the given file object
     * 
     * @param target
     *            the file to write the content to
     */
    public void sendTo(FileObject target) throws IOException, FileSystemException {
        OutputStream out = target.getContent().getOutputStream();
        sendTo(out);
        out.close();
    }

    /**
     * Parses content, assuming it is XML, and returns the parsed document.
     * 
     * @return the XML document parsed from content
     */
    public Document getXML() throws JDOMException, IOException {
        checkConsumed();
        Document xml = new SAXBuilder().build(in);
        in.close();
        return xml;
    }

    /**
     * Returns the raw content
     * 
     * @return the content
     */
    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        sendTo(baos);
        baos.close();
        return baos.toByteArray();
    }

    /**
     * Returns the content as String, assuming the provided encoding
     * 
     * @param encoding
     *            the encoding to use to build the characters
     * @return content as String
     */
    public String getString(String encoding) throws IOException, UnsupportedEncodingException {
        return new String(getBytes(), encoding);
    }

    /**
     * Returns content as String, assuming UTF-8 encoding
     * 
     * @return content as String
     */
    public String getString() throws Exception {
        return getString("UTF-8");
    }

    /**
     * Ensures that this content is not already consumed, because it can only be
     * used once.
     */
    protected void checkConsumed() {
        if (consumed)
            throw new MCRUsageException("MCRContent is already consumed, can only be used once");
        else
            consumed = true;
    }

    /**
     * Makes copies of the content, consuming this content
     * 
     * @param numCopies
     *            the number of copies to make
     * @return copies of the content
     */
    public MCRContent[] makeCopies(int numCopies) throws IOException {
        MCRContent[] copies = new MCRContent[numCopies];
        byte[] content = getBytes();
        for (int i = 0; i < numCopies; i++)
            copies[i] = new MCRContent(content);
        return copies;
    }
}
