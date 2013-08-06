package fr.eolya.indexer;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;

import fr.eolya.utils.Logger;

public class SolrCore extends EngineAbstract implements IEngine {
    
    private String url = null;
    private String baseurl = null;
    private String home = null;
    private String coreName = null;
    private CoreContainer coreContainer = null;
    private SolrServer server = null;
    private String requestHandler = null;
    private boolean solrUseJavaBin = false;
    
    public String getUrl() {
        return url;
    }
    
    public SolrCore(String baseurl, String coreName, int commitWithin, int commitEach, int optimizeEach, Logger logger, String requestHandler, boolean solrUseJavaBin) {
    	URL u = null;
    	try {
			u = new URL(baseurl);
		} catch (MalformedURLException e) {}
    	
    	if (u!=null) {
    		this.baseurl = baseurl;
    	}
    	else {
    		File f = new File(baseurl);
    		if (f.exists() && f.isDirectory()) {
    			home = baseurl;
    		}
    	}
    	this.url = baseurl + "/" + coreName;
    	this.coreName = coreName;
        if ((optimizeEach>0) && (optimizeEach<commitEach)) optimizeEach=commitEach;
        if (commitWithin>0) commitEach=0;
        
        this.commitWithin = commitWithin;
        this.commitEach = commitEach;
        this.optimizeEach = optimizeEach;
        this.logger = logger;
        this.requestHandler = requestHandler;
        this.solrUseJavaBin = solrUseJavaBin;
    }
        
    public boolean connect() {
        try {
        	server = null;
        	coreContainer = null;
        	if (baseurl!=null) {
        		server = new HttpSolrServer(url);
	            if (!solrUseJavaBin)
	            	((HttpSolrServer)server).setParser(new XMLResponseParser());
        	}

           	if (home!=null) {
           		/*
           		coreContainer = new CoreContainer(home);
           		CoreDescriptor discriptor = new CoreDescriptor(coreContainer, this.coreName, new File(home, this.coreName).getAbsolutePath());
           		org.apache.solr.core.SolrCore solrCore = coreContainer.create(discriptor);
     		    coreContainer.register(solrCore, false);
     		    server = new EmbeddedSolrServer(coreContainer, this.coreName);
     		    */
           		
           		File h = new File(home); 
           	    File f = new File( h, "solr.xml" ); 
           	    CoreContainer container = new CoreContainer(home); 
           	    container.load( home, f ); 
           	    server = new EmbeddedSolrServer( container, this.coreName ); 
           	}
           	
           	if (server==null) return false;

            return true;
        } catch (Exception e) {
            log("error connecting to solr (" + url + ") " + e.getMessage());
            if (outputStackTrace) e.printStackTrace();
        }
        return false;		
    }
    
    public void close() {
    	if (coreContainer!=null) coreContainer.shutdown();
    }
    
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
            log("Ping failed on solr (" + url + ") " + sse.getMessage());
            if (outputStackTrace) {
                System.out.println(sse.getMessage());
                System.out.println(lastExceptionCauseName);
                sse.printStackTrace();
            }
            return false;			
        } catch (Exception e) {
            lastExceptionCauseName = e.getMessage();
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
        
        Collection<SolrInputDocument> solrDocs = new ArrayList<SolrInputDocument>();
        for (Iterator<InputDocument> iter = docs.iterator(); iter.hasNext();) {
            InputDocument doc = (InputDocument) iter.next();
            SolrInputDocument solrDoc = new SolrInputDocument();
            
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
            UpdateRequest req = new UpdateRequest();
            if ((this.requestHandler != null) && (!this.requestHandler.isEmpty()))
                req.setPath(this.requestHandler);
            req.add(solrDocs);
            if (commitWithin>0)
            	req.setCommitWithin(commitWithin);
            req.process(server);
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
}
