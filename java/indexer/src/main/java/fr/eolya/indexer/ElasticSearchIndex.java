package fr.eolya.indexer;

// https://github.com/dadoonet/spring-elasticsearch/blob/master/src/main/java/fr/pilato/spring/elasticsearch/ElasticsearchAbstractClientFactoryBean.java#L616

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.lucene.search.TermQuery;
import org.elasticsearch.action.admin.indices.create.*;
import org.elasticsearch.action.admin.indices.delete.*;
import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.admin.indices.optimize.OptimizeRequest;
import org.elasticsearch.action.admin.indices.settings.*;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.*;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import static org.elasticsearch.common.xcontent.XContentFactory.*;
import org.elasticsearch.action.admin.cluster.health.*;
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequest;

import fr.eolya.utils.Logger;
import fr.eolya.utils.Utils;

public class ElasticSearchIndex extends EngineAbstract implements IEngine {
    
    private String host;
    private int port;
    private String indexName;
    private Client client = null;
    
    public ElasticSearchIndex(String host, int port, String indexName, Logger logger) {
        this.host = host;
        this.port = port;
        this.indexName= indexName;
        this.logger = logger;
        outputStackTrace = true;
    }   
    
    
    /**
     * Check if an index already exists
     * @param index Index name
     * @return true if index already exists
     */
    private boolean isIndexExist() throws Exception {
        return client.admin().indices().prepareExists(indexName).execute().actionGet().isExists();
    }
    
    /**
     * Create a new index in Elasticsearch
     * @param index Index name
     * @param source Index settings
     * @return true if index was successfully created
     */
    private boolean createIndex(String source) {
        try {
            CreateIndexRequestBuilder cirb = client.admin().indices().prepareCreate(indexName);
            if (source != null) {
                cirb.setSettings(source);
            }
            CreateIndexResponse createIndexResponse = cirb.execute().actionGet();
            return createIndexResponse.acknowledged();
        } catch(Exception e) {
            if (outputStackTrace) e.printStackTrace();   
        }
        return false;
    }
    
        private boolean deleteIndex() throws Exception {
            try {
                DeleteIndexRequestBuilder dirb = client.admin().indices().prepareDelete(indexName);
                DeleteIndexResponse deleteIndexResponse = dirb.execute().actionGet();
                return deleteIndexResponse.acknowledged();
            } catch(Exception e) {
                if (outputStackTrace) e.printStackTrace();   
            }
            return false;
        }
    
    /**
     * Check if a mapping already exists in an index
     * @param index Index name
     * @param type Mapping name
     * @return true if mapping exists
     */
    private boolean isMappingExist(String type) {
        ClusterState cs = client.admin().cluster().prepareState().setFilterIndices(indexName).execute().actionGet().getState();
        IndexMetaData imd = cs.getMetaData().index(indexName);
        if (imd == null) return false;
        MappingMetaData mdd = imd.mapping(type);
        if (mdd != null) return true;
        return false;
    }
    
    /**
     * Define a type for a given index and if exists with its mapping definition
     * @param index Index name
     * @param type Type name
     * @param source Mapping JSON source
     * @param force Force rebuild the type : <b>Caution</b> : if true, all your datas for
     * this type will be erased. Use only for developpement or continuous integration
     * @param merge Merge existing mappings
     * @return true if mapping was successfully created
     */
    private boolean createMapping(String type, String source, boolean force, boolean merge) {
        try {
            // If type already exists and if we are in force mode, we delete the type and its mapping
            if (force && isMappingExist(type)) {
                // Remove mapping and type in ElasticSearch !
                client.admin().indices()
                .prepareDeleteMapping()
                .setType(type)
                .execute().actionGet();
            }
            // If type does not exist, we create it
            boolean mappingExist = isMappingExist(type);
            if (merge || !mappingExist) {
                // Read the mapping json file if exists and use it
                // Create type and mapping
                PutMappingResponse response = client.admin().indices()
                                .preparePutMapping()
                                .setType(type)
                                .setSource(source)
                                .execute().actionGet();         
                return response.acknowledged();
            } 
            return false;
        } catch(Exception e) {
            if (outputStackTrace) e.printStackTrace();   
        }
        return false;
    }
    
    /**
     * update index settings in ElasticSearch
     * @param index Index name
     * @param source Settings JSON source
     * @throws Exception
     */
    private boolean updateIndexSettings(String source) {
        try {
            UpdateSettingsRequestBuilder usrb = client.admin().indices().prepareUpdateSettings(indexName);
            
            // If there are settings for this index, we use it. If not, using Elasticsearch defaults.
            if (source != null) usrb.setSettings(source);
            UpdateSettingsResponse response = usrb.execute().actionGet();
            //return response.acknowledged();
            return true;
        } catch(Exception e) {
            if (outputStackTrace) e.printStackTrace();   
        }
        return false;
    }
        
    public boolean connect() {
        try {
            client = new TransportClient().addTransportAddress(new InetSocketTransportAddress(this.host, this.port));
            if (client==null) return false;
            return true;
        } catch (Exception e) {
            log("error connecting to elasticsearch (" + this.host + ":" + String.valueOf(this.port) + ") " + e.getMessage());
            if (outputStackTrace) e.printStackTrace();
        }
        return false;       
    }
    
    public boolean setMapping(String name, String source) {
        try {
            createMapping(name, source, false, false);
            return true;
        } catch (Exception e) {
            log("error create mapping : " + e.getMessage());
            if (outputStackTrace) e.printStackTrace();
        }
        return false;       
    }
    
    
    public boolean open(String settingsPath, String languages, boolean reset) {
        try {
            if (isIndexExist() && reset) {
                deleteIndex();
            }
            if (!isIndexExist()) {
                createIndex(Utils.loadFileInString(settingsPath + "/es_analysis.json"));
                
                // mappings
                String[] l = languages.split(",");
                for (int i=0; i<l.length; i++) {
                    setMapping("mapping_" + l[i].trim(), Utils.loadFileInString(settingsPath + "/es_mapping_" + l[i].trim() + ".json"));
                }
            }
            return true;
        } catch (Exception e) {
            log("error connecting to elasticsearch (" + this.host + ":" + String.valueOf(this.port) + ") " + e.getMessage());
            if (outputStackTrace) e.printStackTrace();
        }
        return false;       
    }
    
    public void close() {
        client.close();
    }
    
    public String getUrl() {
        return "http://" + host + ":" + String.valueOf(port) + "/" + indexName;
    }
    
    private void addDocument(InputDocument doc) {
        if (client==null) return;
        try {
            XContentBuilder xcb;
            xcb = jsonBuilder().startObject();
            Collection<InputField> fields = doc.getFields();
            for (Iterator<InputField> iter = fields.iterator(); iter.hasNext();) {
                InputField field = (InputField) iter.next();
                xcb = xcb.field(field.name, field.value);
            }
            xcb = xcb.endObject();
            String s = xcb.string();
            client.prepareIndex(indexName, doc.getDocumentType()).setSource(xcb).execute().actionGet();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public int addDocuments(Collection<InputDocument> docs) {
        for (Iterator<InputDocument> iter = docs.iterator(); iter.hasNext();) {
            InputDocument doc = (InputDocument) iter.next();
            addDocument(doc);
        }
        return docs.size();
    }
    
    protected void internalOptimize() {
        client.admin().indices().optimize(new OptimizeRequest(indexName)).actionGet();
    }
    
    protected void internalCommit() {
        client.admin().indices().flush(new FlushRequest(indexName)).actionGet();
    }
    
    @Override
    public boolean ping() {
        try {    
            ClusterHealthResponse healthResponse = client.admin().cluster().prepareHealth(indexName).setWaitForYellowStatus().setTimeout("1s").execute().actionGet(); 
            if (healthResponse.timedOut()) return false;
            if (healthResponse.status().equals(ClusterHealthStatus.RED)) return false;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } 
        return false;
    }
    
    @Override
    protected void internalDeleteByQuery(String query) {
        if (client==null) return;
        
        String [] queryItems = query.split(":");
        
        QueryBuilder qb1 = QueryBuilders.termQuery(queryItems[0], queryItems[1]);
        
        client.prepareDeleteByQuery(indexName).
        setQuery(qb1).
        execute().actionGet();
        
        
        //client.deleteByQuery(new DeleteByQueryRequest());
        //.prepareIndex(indexName, doc.getDocumentType()).setSource(xcb).execute().actionGet();

    }
    
}
