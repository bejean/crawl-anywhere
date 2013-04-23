package fr.eolya.crawler.cache;

import fr.eolya.crawler.cache.mongodb.MongoDBDocumentCache;
import fr.eolya.utils.nosql.IDBConnection;
import fr.eolya.utils.nosql.mongodb.MongoDBConnection;

public class DocumentCacheFactory {

	public static IDocumentCache getDocumentCacheInstance(String type, IDBConnection con, String dbName, String dbCollName, String sourceId) {
		if (!type.equals(con.getType())) return null;
		
		if ("mongodb".equals(type)) {
				return new MongoDBDocumentCache(sourceId, (MongoDBConnection) con, dbName, dbCollName);
		}
		return null;
	}

}
