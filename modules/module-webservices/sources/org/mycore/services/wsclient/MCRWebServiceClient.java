/*
 * $RCSfile$
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

package org.mycore.services.wsclient;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.Properties;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import org.mycore.common.xml.MCRURIResolver;
import org.w3c.dom.*;

import org.apache.log4j.Logger;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.AxisFault;

/**
 * Test client for MyCoRe MCRWebService
 * 
 * @author Harald Richter
 * @version $Revision$ $Date$
 **/
public class MCRWebServiceClient
{
  static Logger logger = Logger.getLogger( MCRWebServiceClient.class );

  private static void usage()
  {
    System.out.println( "usage: parameter of MCRWebServicClient\n" );
    System.out.println( "-endpoint     url of webservice" );
    System.out.println( "-operation    valid opartions are:" );
    System.out.println( "                 retrieve, parameter -mcrid required" );
    System.out.println( "                 query, parameter -file required" );
    System.out.println( "-mcrid        id of MyCoRe-Object" );
    System.out.println( "-file         xml file with query" );
  }
  
  private static void handleParams(String args[], Properties params)
  {
    for (int i=0; i<args.length; i=i+2 )
    {
      String op    = args[i];
      String value = args[i+1];
      if ( "-endpoint".equals(op))
        params.setProperty("endpoint", value);
      else if ( "-operation".equals(op))
        params.setProperty("operation", value);
      else if ( "-mcrid".equals(op))
        params.setProperty("mcrid", value);
      else if ( "-file".equals(op))
        params.setProperty("file", value);
    }
  }
  
  public static void main(String args[])
  {
/* This code can be used to build a client without MCRWebServiceServiceLocator     
    String endpoint = "http://localhost:8080/docportal/services/MCRWebService";
    try 
    {
      Service service = new Service();
      Call    call    = (Call)service.createCall();
      call.setTargetEndpointAddress(new URL(endpoint) );
      call.setOperationName("MCRDoRetrieveObject");
      String mcrid = "DocPortal_document_00410901";
      org.w3c.dom.Document result = (org.w3c.dom.Document)call.invoke( new Object[] { mcrid } );

      org.jdom.input.DOMBuilder d = new org.jdom.input.DOMBuilder();
      org.jdom.Document doc = d.build(result);
      org.jdom.output.XMLOutputter outputter = new org.jdom.output.XMLOutputter();
      logger.info( outputter.outputString( doc ) );
    }
    catch (AxisFault ax)
    {
      System.out.println(ax.toString());
      ax.dump();
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
  }
*/  
    Properties params = new Properties();
    params.setProperty("endpoint", "http://localhost:8080/docportal/services/MCRWebService");
    params.setProperty("mcrid", "DocPortal_document_00410901");
    params.setProperty("operation", "retrieve");
    
    handleParams(args, params);
    
    String endpoint = (String)params.getProperty("endpoint");
    System.out.println("Endpoint: " + endpoint);
    String operation = (String)params.getProperty("operation");
    System.out.println("Operation: " + operation);
    
    
    MCRWebServiceServiceLocator l = new MCRWebServiceServiceLocator();
    try 
    {
      l.setMCRWebServiceEndpointAddress(endpoint);
      MCRWebService stub          = l.getMCRWebService();
      if ("retrieve".equals(operation))
      {
        org.w3c.dom.Document result = stub.MCRDoRetrieveObject((String)params.getProperty("mcrid"));

        org.jdom.input.DOMBuilder d = new org.jdom.input.DOMBuilder();
        org.jdom.Document doc = d.build(result);
        org.jdom.output.XMLOutputter outputter = new org.jdom.output.XMLOutputter();
        logger.info( outputter.outputString(doc ) );
      }
      else if ("query".equals(operation))
      {
        String file = (String)params.getProperty("file");
        if ( null != file )
        {
          System.out.println("file://" + file );
          org.jdom.Element query = MCRURIResolver.instance().resolve( "file://" + file );
          org.jdom.Document root = new org.jdom.Document(query);
          org.jdom.output.DOMOutputter doo = new org.jdom.output.DOMOutputter();

          org.w3c.dom.Document result = stub.MCRDoQuery(doo.output(root));

          org.jdom.input.DOMBuilder d = new org.jdom.input.DOMBuilder();
          org.jdom.Document doc = d.build(result);
          org.jdom.output.XMLOutputter outputter = new org.jdom.output.XMLOutputter();
          logger.info( outputter.outputString(doc ) );
        } else
          System.out.println("xml file with query missing");
      }
      else
      {
        usage();
      }
    }
    catch (AxisFault ax)
    {
      System.out.println(ax.toString());
      ax.dump();
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
  }
}