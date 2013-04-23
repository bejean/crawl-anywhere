package fr.eolya.crawler.cache.mongodb;

import java.util.Iterator;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;

import fr.eolya.crawler.cache.DocumentCacheItem;

public class MongoDBDocumentCacheIterator implements Iterator<DocumentCacheItem> {

	private DBCursor cur;
	
	public MongoDBDocumentCacheIterator(DBCursor cur) {
		this.cur = cur;
	}

	@Override
	public boolean hasNext() {
		return cur.hasNext();
	}

	@Override
	public DocumentCacheItem next() {		
		BasicDBObject doc = (BasicDBObject) cur.next();
		return MongoDBDocumentCache.doc2DocumentCacheItem(doc);
	}

	@Override
	public void remove() {
		 cur.remove();
	}
}
