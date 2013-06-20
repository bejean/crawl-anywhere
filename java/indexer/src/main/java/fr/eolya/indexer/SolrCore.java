package fr.eolya.indexer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.SolrInputDocument;

import fr.eolya.utils.Logger;

public class SolrCore extends EngineAbstract implements IEngine {
    
    private String url = null;
    private CommonsHttpSolrServer server = null;
    private String requestHandler = null;

    //	private int optimizeDocsCount = 0;
    //	private int commitDocsCount = 0;
    //	private int optimizeEach = 0;
    //	private int commitWithin = 0;
    //	private int commitEach = 0;
    //	private long lastCommitTimestamp = 0;
    
    //SimpleHttpConnectionManager connectionManager = null;
    ///HttpClient httpClient = null;
    
    public String getUrl() {
        return url;
    }
    
    public SolrCore(String url, int commitWithin, int commitEach, int optimizeEach, Logger logger, String requestHandler) {
        this.url = url;
        if ((optimizeEach>0) && (optimizeEach<commitEach)) optimizeEach=commitEach;
        if (commitWithin>0) commitEach=0;
        
        this.commitWithin = commitWithin;
        this.commitEach = commitEach;
        this.optimizeEach = optimizeEach;
        this.logger = logger;
        this.requestHandler = requestHandler;
    }
    
    
    
    //	public boolean connect(boolean useHttpClient) {
    //		if (useHttpClient) {
    //			connectionManager = new SimpleHttpConnectionManager();
    //			if (connectionManager == null) return false;
    //
    //			HttpConnectionManagerParams params = connectionManager.getParams();
    //			params.setConnectionTimeout(5000);
    //			params.setSoTimeout(20000);
    //
    //			httpClient = new HttpClient(connectionManager);
    //			if (httpClient == null) return false;
    //
    //			httpClient.getParams().setSoTimeout(20000);			
    //		}
    //		else {
    //			connectionManager = null;
    //			httpClient = null;
    //		}
    //		return connect();
    //
    //	}
    
    public boolean connect() {
        try {
            //			if (httpClient!=null)
            //				server = new CommonsHttpSolrServer(url, httpClient);
            //			else
            server = new CommonsHttpSolrServer(url);
            server.setParser(new XMLResponseParser());
            return true;
        } catch (Exception e) {
            log("error connecting to solr (" + url + ") " + e.getMessage());
            if (outputStackTrace) e.printStackTrace();
        }
        return false;		
    }
    
    public void close() {}
    
    public boolean ping() {
        lastExceptionCauseName = "";
        if (server==null) {
            log("Connection not initiated");
            return false;
        }
        try {
            @SuppressWarnings("unused")
            SolrPingResponse ping = server.ping();
        } catch (SolrServerException sse) {
            lastExceptionCauseName = sse.getMessage();
            //.getCause().getClass().getName();
            log("Ping failed on solr (" + url + ") " + sse.getMessage());
            if (outputStackTrace) {
                System.out.println(sse.getMessage());
                System.out.println(lastExceptionCauseName);
                sse.printStackTrace();
            }
            return false;			
        } catch (Exception e) {
            lastExceptionCauseName = e.getMessage();
            //.getCause().getClass().getName();
            log("Ping failed on solr (" + url + ") " + e.getMessage());
            if (outputStackTrace) {
                System.out.println(e.getMessage());
                System.out.println(lastExceptionCauseName);
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }	
    
    public int addDocuments(Collection<InputDocument> docs) {
        //public int add(Collection<SolrInputDocument> docs) {
        
        Collection<SolrInputDocument> solrDocs = new ArrayList<SolrInputDocument>();
        for (Iterator<InputDocument> iter = docs.iterator(); iter.hasNext();) {
            InputDocument doc = (InputDocument) iter.next();
            SolrInputDocument solrDoc = new SolrInputDocument();
            //addDocument(doc);
            
            Collection<InputField> fields = doc.getFields();
            for (Iterator<InputField> iterFields = fields.iterator(); iterFields.hasNext();) {
                InputField field = (InputField) iterFields.next();
                if (field.boost!=null)
                    solrDoc.addField(field.name, field.value, Float.parseFloat(field.boost));
                else
                    solrDoc.addField(field.name, field.value);
            }
            solrDocs.add(solrDoc);
        }
        
        try {
            //if (commitWithin==0) {
            //    server.add(solrDocs);
            //}
            //else {
                UpdateRequest req = new UpdateRequest();
                if ((this.requestHandler != null) && (!this.requestHandler.isEmpty()))
                    req.setPath(this.requestHandler);
                req.add(solrDocs);
                if (commitWithin>0)
                	req.setCommitWithin(commitWithin);
                req.process(server);
            //}
        } catch (Exception e) {
            log("     error : " + e.getMessage());
            if (outputStackTrace) e.printStackTrace();
            return -1;
        }
        commitDocsCount += docs.size();
        optimizeDocsCount += docs.size();
        commit(false);
        return docs.size();
    }
    
    protected void internalOptimize() {
        try {
            server.optimize();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    protected void internalCommit() {
        try {
            server.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    protected void internalDeleteByQuery(String query) {
        try {
            if ("".equals(query)) query = "*:*";
            server.deleteByQuery(query);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
//    public boolean commit(boolean force) {
//        if (server==null) return false;
//        if (commitDocsCount==0) return true;
//        try {		
//            Date now = new Date();
//            if ( (optimizeEach>0) && (optimizeDocsCount >= optimizeEach)) {
//                log("Commiting index");
//                server.commit();
//                log("Optimizing index");
//                server.optimize();
//                optimizeDocsCount = 0;
//                commitDocsCount = 0;
//                lastCommitTimestamp = now.getTime();
//            } else {
//                long lastCommitDelay = (now.getTime() - lastCommitTimestamp) / 1000;
//                if ((commitDocsCount >= commitEach && commitEach > 0) || force || (lastCommitDelay > 300  && commitEach > 0)) {
//                    log("Commiting index");
//                    server.commit();
//                    commitDocsCount = 0;
//                    lastCommitTimestamp = now.getTime();
//                }
//            }
//            return true;
//        }
//        catch (Exception e) {
//            if (outputStackTrace) e.printStackTrace();
//            return false;
//        }
//    }
    
//    public boolean optimize(boolean force) {
//        if (server==null) return false;
//        if (optimizeDocsCount==0) return true;
//        try {		
//            if ((optimizeEach>0) && (optimizeDocsCount >= optimizeEach || force)) {
//                log("Optimizing index");
//                server.commit();
//                server.optimize();
//                optimizeDocsCount = 0;
//                commitDocsCount = 0;
//            }
//            return true;
//        }
//        catch (Exception e) {
//            if (outputStackTrace) e.printStackTrace();
//            return false;
//        }
//    }
    
//    public boolean deleteByQuery(String query) {
//        if (server==null) return false;
//        try {
//            server.deleteByQuery(query);
//            commitDocsCount++;
//            optimizeDocsCount++;
//        } catch (Exception e) {
//            if (outputStackTrace) e.printStackTrace();
//            return false;
//        }
//        commit(true);
//        return true;
//    }
    
//    public boolean resetIndex() {
//        if (server==null) return false;
//        try {
//            server.deleteByQuery("*:*");
//            server.commit();
//            server.optimize();
//            return true;
//        } catch (Exception e) {
//            log("failed to reset index");
//            if (outputStackTrace) e.printStackTrace();
//            return false;
//        }
//    }
    
    
    
    //	public static void main(String[] args) {
    //		String url = "http://localhost:8180/solr_3e193e3f-45d6-4fe5-9ec9-995dcf762180/core";
    //		SolrCore core = new SolrCore(url, 1000, 0, 0, null);
    //		core.setOutputStackTrace(true);
    //		if (core.connect(true)) {
    //			if (!core.ping()) {
    //				System.out.println(core.getLastExceptionCauseName());		
    //			}
    //		}
    //	}
    
}
