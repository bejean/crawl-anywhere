package fr.eolya.crawler.cache;

import java.io.InputStream;
import java.util.HashMap;

import fr.eolya.utils.XMLConfig;

public class DocumentCacheItem {

	public String sourceId;
	public String itemId;
	public String accountId;
	public InputStream streamData;
	public HashMap<String, String> params;
	public HashMap<String, String> metas;
	public XMLConfig extra;
	
    public DocumentCacheItem(String itemId, InputStream streamData, HashMap<String, String> params, HashMap<String, String> metas, XMLConfig extra) {
    	this.itemId = itemId;
    	this.streamData = streamData;
    	this.params = params;
    	this.metas = metas;
    	this.extra = extra;
    }
}
