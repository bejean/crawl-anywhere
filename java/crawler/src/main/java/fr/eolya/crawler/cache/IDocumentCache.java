package fr.eolya.crawler.cache;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;

import fr.eolya.utils.XMLConfig;

public interface IDocumentCache {
    public String put(String itemId, InputStream dataStream, long dataSize, HashMap<String, String> params, HashMap<String, String> metas, XMLConfig extra);
    public void remove(String itemId);
    public DocumentCacheItem get(String itemId);
	public long size();
    public void reset();
    public Iterator<DocumentCacheItem> getIterator();
}
