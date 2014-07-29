package org.mycore.iview2.frontend.resources;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBException;
import javax.xml.transform.TransformerException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.util.MCRServletContentHelper;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.iview2.frontend.MCRIviewACLProvider;
import org.mycore.iview2.frontend.MCRIviewDefaultACLProvider;
import org.mycore.iview2.frontend.configuration.MCRIViewClientConfiguration;
import org.mycore.iview2.frontend.configuration.MCRIViewClientConfigurationBuilder;
import org.mycore.services.i18n.MCRTranslation;
import org.xml.sax.SAXException;

@Path("/iview/client")
public class MCRIViewClientResource {

    private static final MCRIviewACLProvider IVIEW_ACL_PROVDER = MCRConfiguration.instance()
        .<MCRIviewACLProvider> getInstanceOf("MCR.Module-iview2.MCRIviewACLProvider",
            MCRIviewDefaultACLProvider.class.getName());

    private static final String JSON_CONFIG_ELEMENT_NAME = "json";

    @GET
    @Produces(MediaType.APPLICATION_XML)
    @Path("{derivate}/{path}")
    public void show(@Context HttpServletRequest request, @Context HttpServletResponse response,
        @Context ServletContext context, @Context ServletConfig config) throws Exception {
        MCRContent content = getContent(request, response);
        boolean serveContent = MCRServletContentHelper.isServeContent(request);
        MCRServletContentHelper.serveContent(content, request, response, context,
            MCRServletContentHelper.buildConfig(config), serveContent);
    }

    private static Document buildResponseDocument(MCRIViewClientConfiguration config) throws JDOMException,
        IOException, SAXException, JAXBException {
        String configJson = config.toJSON();
        Element startIviewClientElement = new Element("IViewConfig");
        Element configElement = new Element(JSON_CONFIG_ELEMENT_NAME);
        startIviewClientElement.addContent(configElement);
        startIviewClientElement.addContent(config.toXML().asXML().getRootElement().detach());
        configElement.addContent(configJson);
        Document startIviewClientDocument = new org.jdom2.Document(startIviewClientElement);
        return startIviewClientDocument;
    }

    protected MCRContent getContent(final HttpServletRequest req, final HttpServletResponse resp) throws JDOMException,
        IOException, SAXException, JAXBException, TransformerException {

        String derivate = MCRIViewClientConfiguration.getDerivate(req);
        final MCRObjectID derivateID = MCRObjectID.getInstance(derivate);
        if (!MCRMetadataManager.exists(derivateID)) {
            String errorMessage = MCRTranslation.translate("component.iview2.MCRIViewClientServlet.object.not.found",
                derivateID);
            throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(errorMessage).build());
        }
        if (IVIEW_ACL_PROVDER != null && !IVIEW_ACL_PROVDER.checkAccess(req.getSession(), derivateID)) {
            String errorMessage = MCRTranslation.translate("component.iview2.MCRIViewClientServlet.noRights",
                derivateID);
            throw new WebApplicationException(Response.status(Status.FORBIDDEN).entity(errorMessage).build());
        }
        MCRIViewClientConfiguration config = MCRConfiguration.instance().getInstanceOf(
            "MCR.Module-iview2.configuration", null);
        if (config != null) {
            config.setup(req);
        } else {
            config = MCRIViewClientConfigurationBuilder.metsAndPlugins(req).get();
        }
        MCRJDOMContent source = new MCRJDOMContent(buildResponseDocument(config));
        return MCRLayoutService.instance().getTransformedContent(req, resp, source);
    }
}
