package fr.eolya.crawler.database;

import java.net.UnknownHostException;

import fr.eolya.crawler.database.mongodb.MongoDBCrawlerDB;
import fr.eolya.utils.Logger;
import fr.eolya.utils.nosql.IDBConnection;
import fr.eolya.utils.nosql.mongodb.MongoDBConnection;

public class CrawlerDBFactory {

	public static ICrawlerDB getCrawlerDBInstance(String type, IDBConnection con, String dbName, String dbNameQueues, Logger logger) {
		if (!type.equals(con.getType())) return null;

		if ("mongodb".equals(type)) {
			try {
				return new MongoDBCrawlerDB((MongoDBConnection) con, dbName, dbNameQueues, logger);
			} catch (UnknownHostException e) {
				e.printStackTrace();
				return null;
			}
		}
		return null;
	}

}
