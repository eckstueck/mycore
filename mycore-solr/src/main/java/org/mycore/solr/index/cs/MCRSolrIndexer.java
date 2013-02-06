/**
 * 
 */
package org.mycore.solr.index.cs;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.jdom2.Document;
import org.jdom2.Element;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRUtils;
import org.mycore.common.content.MCRBaseContent;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRFileContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventManager;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSearcher;
import org.mycore.services.fieldquery.MCRSortBy;
import org.mycore.solr.MCRSolrServerFactory;
import org.mycore.solr.legacy.MCRLuceneSolrAdapter;
import org.mycore.solr.logging.MCRSolrLogLevels;

/**
 * @author shermann
 *
 */
public class MCRSolrIndexer extends MCRSearcher {
    private static final Logger LOGGER = Logger.getLogger(MCRSolrIndexer.class);

    /** The Server used for indexing. */
    final static HttpSolrServer solrServer = MCRSolrServerFactory.getSolrServer();

    /** The executer service used for submitting the index requests. */
    final static ExecutorService executorService = Executors.newFixedThreadPool(10);

    /** Specify how many documents will be submitted to solr at a time when rebuilding the metadata index. Default is 100. */
    final static int BULK_SIZE = MCRConfiguration.instance().getInt("MCR.Module-solr.bulk.size", 100);

    /** default is 10 MB */
    public final static long OVER_THE_WIRE_THRESHOLD = MCRConfiguration.instance().getLong("MCR.Module-solr.OverTheWireThresholdInBytes",
            1024 * 1024 * 10);

    @Override
    public boolean isIndexer() {
        return true;
    }

    @Override
    synchronized protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
        this.handleMCRBaseCreated(evt, obj);
    }

    @Override
    synchronized protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        this.handleMCRBaseCreated(evt, obj);

    }

    @Override
    protected void handleObjectRepaired(MCREvent evt, MCRObject obj) {
        this.handleMCRBaseCreated(evt, obj);
    }

    @Override
    synchronized protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        this.deleteByIdFromSolr(obj.getId().toString());
    }

    @Override
    protected void handleDerivateCreated(MCREvent evt, MCRDerivate derivate) {
        this.handleMCRBaseCreated(evt, derivate);
    }

    @Override
    protected void handleDerivateUpdated(MCREvent evt, MCRDerivate derivate) {
        this.handleMCRBaseCreated(evt, derivate);
    }

    @Override
    protected void handleDerivateRepaired(MCREvent evt, MCRDerivate derivate) {
        this.handleMCRBaseCreated(evt, derivate);
    }

    @Override
    protected void handleDerivateDeleted(MCREvent evt, MCRDerivate derivate) {
        this.deleteByIdFromSolr(derivate.getId().toString());
    }

    synchronized protected void handleMCRBaseCreated(MCREvent evt, MCRBase objectOrDerivate) {
        long tStart = System.currentTimeMillis();
        try {
            LOGGER.trace("Solr: submitting data of\"" + objectOrDerivate.getId().toString() + "\" for indexing");
            MCRContent content = (MCRContent) evt.get("content");
            if (content == null) {
                content = new MCRBaseContent(objectOrDerivate);
            }
            MCRBaseContentStream contentStream = new MCRBaseContentStream(objectOrDerivate.getId().toString(), content);
            executorService.submit(contentStream);
            LOGGER.trace("Solr: submitting data of\"" + objectOrDerivate.getId().toString() + "\" for indexing done in "
                    + (System.currentTimeMillis() - tStart) + "ms ");
        } catch (Exception ex) {
            LOGGER.log(MCRSolrLogLevels.SOLR_ERROR, "Error creating transfer thread", ex);
        }
    }

    @Override
    protected void handleFileCreated(MCREvent evt, MCRFile file) {

        try {
            LOGGER.trace("Solr: submitting file \"" + file.getAbsolutePath() + " (" + file.getID() + ")\" for indexing");
            if (file.getSize() > MCRSolrIndexer.OVER_THE_WIRE_THRESHOLD) {
                MCRBaseContentStream contentStream = new MCRBaseContentStream(file.getID(), new MCRJDOMContent(file.createXML()));
                executorService.submit(contentStream);
            } else {
                /* extract metadata with tika */
                MCRAbstractSolrContentStream<MCRFile> contentStream = new MCRFileContentStream(file);
                executorService.submit(contentStream);
            }

        } catch (Exception ex) {
            LOGGER.log(MCRSolrLogLevels.SOLR_ERROR, "Error creating transfer thread", ex);
        }
    }

    @Override
    protected void handleFileUpdated(MCREvent evt, MCRFile file) {
        this.handleFileCreated(evt, file);
    }

    @Override
    protected void handleFileDeleted(MCREvent evt, MCRFile file) {
        this.deleteByIdFromSolr(file.getID());
    }

    /**
     * @param solrID
     * @return
     */
    protected UpdateResponse deleteByIdFromSolr(String solrID) {
        UpdateResponse updateResponse = null;
        try {
            LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "Deleting \"" + solrID + "\" from solr");
            updateResponse = solrServer.deleteById(solrID);
            solrServer.commit();
        } catch (Exception e) {
            LOGGER.log(MCRSolrLogLevels.SOLR_ERROR, "Error deleting document from solr", e);
        }
        return updateResponse;
    }

    /**
     * Rebuilds solr's metadata index.
     */
    public static void rebuildMetadataIndex() {
        rebuildMetadataIndex(MCRXMLMetadataManager.instance().listIDs());
    }

    /**
     * Rebuilds solr's metadata index only for objects of the given type.
     * 
     * @param type of the objects to index
     */
    public static void rebuildMetadataIndex(String type) {
        List<String> identfiersOfType = MCRXMLMetadataManager.instance().listIDsOfType(type);
        rebuildMetadataIndex(identfiersOfType);
    }

    /**
     * Rebuilds solr's metadata index.
     * @param list list of identifiers of the objects to index
     */
    public static void rebuildMetadataIndex(List<String> list) {
        LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "=======================");
        LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "Building Metadata Index");
        LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "=======================");

        if (list.size() == 0) {
            LOGGER.info("Sorry, no documents to index");
            return;
        }

        StopWatch swatch = new StopWatch();
        swatch.start();
        LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "Sending " + list.size() + " objects to solr for reindexing");

        Element objCollector = new Element("mcrObjs");
        MCRXMLMetadataManager metadataMgr = MCRXMLMetadataManager.instance();

        for (String id : list) {
            try {
                LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "Submitting data of\"" + id + "\" for indexing");
                Document mcrObjXML = metadataMgr.retrieveXML(MCRObjectID.getInstance(id));
                objCollector.addContent(mcrObjXML.getRootElement().detach());

                if (objCollector.getChildren().size() % BULK_SIZE == 0) {
                    executorService.submit(new MCRXMLSolrIndexer(new MCRXMLContentCollectorStream(objCollector)));
                    objCollector = new Element("mcrObjs");
                }
            } catch (Exception ex) {
                LOGGER.log(MCRSolrLogLevels.SOLR_ERROR, "Error creating transfer thread", ex);
            }
        }
        /* index remaining docs*/
        int remaining = objCollector.getChildren().size();
        LOGGER.trace("Indexing almost done. Only " + remaining + " object(s) remaining");
        if (remaining > 0) {
            executorService.submit(new MCRXMLSolrIndexer(new MCRXMLContentCollectorStream(objCollector)));
        }

        long durationInMilliSeconds = swatch.getTime();
        LOGGER.log(MCRSolrLogLevels.SOLR_INFO,
                "Submitted data of " + list.size() + " objects for indexing done in " + Math.ceil(durationInMilliSeconds / 1000)
                        + " seconds (" + durationInMilliSeconds / list.size() + " ms/object)");
        try {
            // we wait until all index threads are finished 
            MCRSolrServerFactory.getConcurrentSolrServer().blockUntilFinished();
            // one last commit before we are done
            MCRSolrServerFactory.getConcurrentSolrServer().commit();
        } catch (SolrServerException e) {
            LOGGER.log(MCRSolrLogLevels.SOLR_ERROR, "Could not commit changes to index", e);
        } catch (IOException e) {
            LOGGER.log(MCRSolrLogLevels.SOLR_ERROR, "An " + e.getClass() + " occured", e);
        }
    }

    /**
     * Rebuilds solr's content index.
     */
    public static void rebuildContentIndex() {
        LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "======================");
        LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "Building Content Index");
        LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "======================");

        List<String> list = MCRXMLMetadataManager.instance().listIDsOfType("derivate");
        if (list.size() == 0) {
            LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "No derivates to index");
            return;
        }

        long tStart = System.currentTimeMillis();

        LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "Sending content of files of " + list.size() + " derivates to solr for reindexing");
        for (String derivate : list) {
            List<MCRFile> files = MCRUtils.getFiles(derivate);
            LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "Sending files (" + files.size() + ") for derivate \"" + derivate + "\"");

            for (MCRFile file : files) {
                try {
                    LOGGER.trace("Solr: submitting file \"" + file.getAbsolutePath() + " (" + file.getID() + ")\" for indexing");

                    MCREvent evt = new MCREvent(MCREvent.FILE_TYPE, MCREvent.UPDATE_EVENT);
                    evt.put("file", file);

                    MCREventManager.instance().handleEvent(evt);
                } catch (Exception ex) {
                    LOGGER.log(MCRSolrLogLevels.SOLR_ERROR, "Error creating transfer thread", ex);
                }
            }
        }

        long tStop = System.currentTimeMillis();
        LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "Submitted data of " + list.size() + " derivates for indexing done in " + (tStop - tStart)
                + "ms (" + ((float) (tStop - tStart) / list.size()) + " ms/derivate)");
    }

    /**
     * Rebuilds and optimizes solr's metadata and content index. 
     */
    public static void rebuildMetadataAndContentIndex() throws Exception {
        MCRSolrIndexer.rebuildMetadataIndex();
        MCRSolrIndexer.rebuildContentIndex();
        MCRSolrIndexer.optimize();
    }

    /**
     * Drops the current solr index.
     */
    public static void dropIndex() throws Exception {
        LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "Dropping solr index...");
        MCRSolrServerFactory.getSolrServer().deleteByQuery("*:*");
        LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "Dropping solr index...done");
    }

    /**
     * Sends a signal to the remote solr server to optimize its index. 
     */
    public static void optimize() {
        try {
            LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "Sending optimize request to solr");
            MCRSolrServerFactory.getSolrServer().optimize();
        } catch (Exception ex) {
            LOGGER.log(MCRSolrLogLevels.SOLR_ERROR, "Could not optimize solr index", ex);
        }
    }

    /**
     * Handles legacy lucene searches.
     * */
    @SuppressWarnings("rawtypes")
    public MCRResults search(MCRCondition condition, int maxResults, List<MCRSortBy> sortBy, boolean addSortData) {
        LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "Processing legacy query \"" + condition.toString() + "\"");
        MCRResults result = MCRLuceneSolrAdapter.search(condition, maxResults, sortBy, addSortData);
        return result;
    }
}
