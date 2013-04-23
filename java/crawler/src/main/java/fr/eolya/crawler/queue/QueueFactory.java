package fr.eolya.crawler.queue;

import java.net.UnknownHostException;

import fr.eolya.crawler.queue.mongodb.MongoDBSourceItemsQueue;
import fr.eolya.crawler.queue.mongodb.MongoDBSourceQueue;
import fr.eolya.utils.nosql.IDBConnection;
import fr.eolya.utils.nosql.mongodb.MongoDBConnection;

public class QueueFactory {

	public static ISourceQueue getSourceQueueInstance(String type, IDBConnection con, String dbName, String dbCollName, boolean test, boolean interactiveOnly, boolean suspiciousOnly, String accountId, String sourceId, String engineId) {
		if (!type.equals(con.getType())) return null;
		
		if ("mongodb".equals(type)) {
			try {
				return  new MongoDBSourceQueue((MongoDBConnection) con, dbName, dbCollName, test, interactiveOnly, suspiciousOnly, accountId, sourceId, engineId);
			} catch (UnknownHostException e) {
				e.printStackTrace();
				return null;
			}
		}
		return null;
	}

	public static ISourceItemsQueue getSourceItemsQueueInstance(String type, int sourceId, IDBConnection con, String dbName, String dbCollName) {
		if (!type.equals(con.getType())) return null;

		if ("mongodb".equals(type)) {
			try {
				return new MongoDBSourceItemsQueue(sourceId, (MongoDBConnection) con, dbName, dbCollName);
			} catch (UnknownHostException e) {
				e.printStackTrace();
				return null;
			}
		}
		return null;
	}

}
