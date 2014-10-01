package org.mycore.oai;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.mycore.datamodel.common.MCRISO8601Date;
import org.mycore.oai.pmh.Set;
import org.mycore.solr.MCRSolrServerFactory;

public class MCROAISolrSearcher extends MCROAISearcher {

    protected final static Logger LOGGER = Logger.getLogger(MCROAISolrSearcher.class);

    private Set set;

    private Date from;

    private Date until;

    private List<String> deletedRecords;

    public MCROAISolrSearcher() {
        super();
        this.deletedRecords = new ArrayList<>();
    }

    @Override
    public MCROAIResult query(int cursor) {
        return solrQuery(cursor);
    }

    @Override
    public MCROAIResult query(Set set, Date from, Date until) {
        this.set = set;
        this.from = from;
        this.until = until;
        this.deletedRecords = searchDeleted(from, until);
        return solrQuery(0);
    }

    protected MCROAIResult solrQuery(int start) {
        ModifiableSolrParams params = new ModifiableSolrParams();
        // query
        String restriction = getConfig().getString(getConfigPrefix() + "Search.Restriction", null);
        if (restriction != null) {
            params.add("q", restriction);
        }
        
        String sortBy = getConfig().getString(getConfigPrefix() + "Search.SortBy", null);
        if(sortBy!=null){
        	sortBy.replace("ascending", "asc").replace("descending", "desc");
        	params.add("sort", sortBy);
        }
        // filter query
        StringBuilder fq = new StringBuilder();
        if (this.set != null) {
            // TODO: handle set
        }
        // from & until
        if (this.from != null || this.until != null) {
            fq.append(buildFromUntilCondition(this.from, this.until));
        }
        if (fq.length() > 0) {
            params.add("fq", fq.toString());
        }
        // start & rows
        params.add("start", String.valueOf(start));
        params.add("rows", String.valueOf(getPartitionSize()));
        SolrServer solrServer = MCRSolrServerFactory.getSolrServer();
        try {
            QueryResponse response = solrServer.query(params);
            return new MCROAISolrResult(response, this.deletedRecords);
        } catch (Exception exc) {
            LOGGER.error("Unable to handle solr request", exc);
        }
        return null;
    }

    private String buildFromUntilCondition(Date from, Date until) {
        String fieldFromUntil = getConfig().getString(getConfigPrefix() + "Search.FromUntil", "modified");
        StringBuilder query = new StringBuilder(" +").append(fieldFromUntil).append(":[");
        if (from == null) {
            query.append("* TO ");
        } else {
            MCRISO8601Date mcrDate = new MCRISO8601Date();
            mcrDate.setDate(from);
            query.append(mcrDate.getISOString()).append(" TO ");
        }
        if (until == null) {
            query.append("*]");
        } else {
            MCRISO8601Date mcrDate = new MCRISO8601Date();
            mcrDate.setDate(until);
            query.append(mcrDate.getISOString()).append("]");
        }
        return query.toString();
    }

    @Override
    public Date getEarliestTimestamp() {
        String sortBy = getConfig().getString(getConfigPrefix() + "EarliestDatestamp.SortBy", "modified asc");
        String fieldName = getConfig().getString(getConfigPrefix() + "EarliestDatestamp.fieldName", "modified");
        String restriction = getConfig().getString(getConfigPrefix() + "Search.Restriction", null);
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("sort", sortBy);
        params.add("q", restriction);
        params.add("fl", fieldName);
        params.add("rows", "1");
        SolrServer solrServer = MCRSolrServerFactory.getSolrServer();
        try {
            QueryResponse response = solrServer.query(params);
            SolrDocumentList list = response.getResults();
            if(list.size() >= 1) {
                return (Date) list.get(0).getFieldValue(fieldName);
            }
        } catch (Exception exc) {
            LOGGER.error("Unable to handle solr request", exc);
        }
        return null;
    }

}
