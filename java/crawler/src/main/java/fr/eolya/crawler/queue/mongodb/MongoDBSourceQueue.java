/*
 * Licensed to Eolya and Dominique Bejean under one
 * or more contributor license agreements. 
 * Eolya licenses this file to you under the 
 * Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package fr.eolya.crawler.queue.mongodb;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import com.mongodb.*;

import fr.eolya.utils.nosql.mongodb.*;
import fr.eolya.crawler.connectors.ISource;
import fr.eolya.crawler.queue.*;

/**
 * A MongoDB queue for web site definition
 */
public class MongoDBSourceQueue implements ISourceQueue {

	private MongoDBConnection con = null;
	private MongoDBDatabase db = null;

	private String collName;	
	private MongoDBCollection coll = null;
	private final Object collMonitor = new Object();
	
	private boolean test;
	private boolean interactiveOnly;
	private boolean suspiciousOnly;
	private String accountId;
	private String sourceId;
	private String engineId;
	



	/**
	 * @param db         			The MongoDB database
	 * @param collName     			The MongoDB collection name
	 * @param uniqueKeyFieldName   	unique key field name for documents in the queue
	 * @return
	 * @throws UnknownHostException 
	 */
	public MongoDBSourceQueue(MongoDBConnection con, String dbName, String collName, boolean test, boolean interactiveOnly, boolean suspiciousOnly, String accountId, String sourceId, String engineId) throws UnknownHostException {

		this.con = con;
		this.db = new MongoDBDatabase(this.con, dbName);
				
		this.collName = collName;
		this.coll = new MongoDBCollection(db, this.collName);
		
		this.test = test;
		this.interactiveOnly = interactiveOnly;
		this.suspiciousOnly= suspiciousOnly;
		this.accountId = accountId;
		this.sourceId = sourceId;
		this.engineId = engineId;
	}

	public void close() {
		if (this.con==null) return;
		this.con.close();
	}
	
	public long size() {
		DBCursor cur = null;	
		String query = getQuery(test, interactiveOnly, suspiciousOnly, accountId, sourceId, engineId);
		BasicDBObject docsearch = MongoDBHelper.JSON2BasicDBObject(query);
		cur = coll.getColl().find(docsearch);
		return cur.size();
	}

	public Map<String,Object> pop() {
		DBCursor cur = null;	
		String query = getQuery(test, interactiveOnly, suspiciousOnly, accountId, sourceId, engineId);
		query = String.format("{\"$and\": [{\"$or\": [{\"_poped\": { \"$exists\": false }},{\"_poped\": false}]}, %1$s]}",
				query);				    

		BasicDBObject docsearch = MongoDBHelper.JSON2BasicDBObject(query);

		synchronized (collMonitor) {
			// TODO : sort by priority and next crawl date
			cur = coll.getColl().find(docsearch).sort(new BasicDBObject("crawl_priority", -1).append("crawl_nexttime",1));

			if (cur.hasNext()) {
				BasicDBObject doc = (BasicDBObject) cur.next();
				BasicDBObject doc2 = (BasicDBObject) doc.copy();
				doc2.put("_poped", true);
				coll.update(doc, doc2);	
				//return doc.toMap();
				return MongoDBHelper.BasicDBObject2Map(doc);
				//return doc.toString();
			}
		}
		return null;	
	}
	
	public void unpop(int id) {
		DBCursor cur = null;	
		String query = String.format("{\"id\": %1$s}", id);	
		BasicDBObject docsearch = MongoDBHelper.JSON2BasicDBObject(query);

		synchronized (collMonitor) {
			cur = coll.getColl().find(docsearch);
			if (cur.hasNext()) {
				BasicDBObject doc = (BasicDBObject) cur.next();
				BasicDBObject doc2 = (BasicDBObject) doc.copy();
				doc2.put("_poped", false);
				coll.update(doc, doc2);	
			}
		}
	}

	private String getQuery(boolean test, boolean interactiveOnly, boolean suspiciousOnly, String accountId, String sourceId, String engineId) {
		// http://docs.mongodb.org/manual/reference/sql-comparison/
		// http://rickosborne.org/download/SQL-to-MongoDB.pdf
		
		String qQeleted = "{\"deleted\": \"0\"}";
		
		String qStatus = String.format("{ \"$and\": [{\"crawl_process_status\": { \"$ne\": \"%1$s\"}}, {\"crawl_process_status\": { \"$ne\": \"%2$s\"}}, {\"crawl_process_status\": { \"$ne\": \"%3$s\"}}]}",
				ISource.CRAWL_PROCESS_STATUS_CRAWLING, ISource.CRAWL_PROCESS_STATUS_PAUSE_REQUESTED, ISource.CRAWL_PROCESS_STATUS_STOP_REQUESTED);

		//String qMode = String.format("{ \"$or\": [{\"crawl_mode\": \"%1$s\"}, {\"crawl_mode\": \"%2$s\"}, {\"crawl_mode\": \"%3$s\"}]}",
		//		ISource.CRAWL_PROCESS_MODE_RESET, ISource.CRAWL_PROCESS_MODE_CLEAR, ISource.CRAWL_PROCESS_MODE_CLEAN);
		String qMode = String.format("{ \"$or\": [{\"crawl_mode\": \"%1$s\"}, {\"crawl_mode\": \"%2$s\"}]}",
				ISource.CRAWL_PROCESS_MODE_RESET, ISource.CRAWL_PROCESS_MODE_CLEAR);
		
		String qStatusMode = String.format("{ \"$or\": [%1$s, %2$s]}",
				qStatus, qMode);
		
		String query;
		if (test) {
			String qEnabled = "{\"enabled\": \"2\"}";
			query = String.format("{ \"$and\": [%1$s, %2$s]}",
					qQeleted, qEnabled);		
			//sqlStatement = "enabled = 2 and deleted = 0 and ";

		} else {
			String qEnabled = "{\"enabled\": \"1\"}";
			query = String.format("{ \"$and\": [%1$s, %2$s, %3$s]}",
					qQeleted, qEnabled, qStatusMode);			
	        //sqlStatement = "(enabled = 1 and deleted = 0 and ((not crawl_process_status = 1 and not crawl_process_status = 2) or (crawl_mode = 2 or crawl_mode = 5 or crawl_mode = 6))) and ";
		}

		if (!"".equals(accountId)) {
			String qAccountId = String.format("{\"id_account\": %1$s}",
					accountId);	
			query = String.format("{\"$and\": [%1$s, %2$s]}",
					qAccountId, query);				    
			//sqlStatement += " id_account = " + accountId + " and " ;				
		} else {
			String qAccountIds;
			if ("".equals(engineId)) {
				qAccountIds = "{\"deleted\": \"0\"}";
				// sqlStatement += " (id_account in (select id from accounts where deleted = 0)) and " ;
			} else {
				qAccountIds = String.format("{\"$and\": [{\"deleted\": \"0\"}, {\"id_engine\": %1$s}]}",
						engineId);
				// sqlStatement += " (id_account in (select id from accounts where id_engine = " + engineId + " and deleted = 0)) and " ;	
			}
			MongoDBCollection accountsColl = new MongoDBCollection(db, "accounts");
			ArrayList<String> ids = accountsColl.getValues(MongoDBHelper.JSON2BasicDBObject (qAccountIds), "id");
			if (ids!=null) {
				String qAccounts = "{ \"id_account\": { \"$in\": [";
				for (int i = 0; i < ids.size(); i++) {
					if (i>0) qAccounts += ", ";
					qAccounts += ids.get(i);
				}
				qAccounts += "]}}";
				query = String.format("{\"$and\": [%1$s, %2$s]}",
						qAccounts, query);				    
			}
		}

	    if (!"".equals(sourceId)) {
			String qSourceId = String.format("{\"id\": %1$s}",
					sourceId);	
			query = String.format("{\"$and\": [%1$s, %2$s]}",
					qSourceId, query);				    
	        //sqlStatement += " id = " + sourceId + " and ";	
	    }
	    
		String qTime1 = "{\"$and\": [{\"crawl_nexttime\": { \"$exists\": false }}, {\"crawl_minimal_period\": { \"$ne\": \"999999\"}}]}";

	    Date startDate = new Date();
	    startDate.setTime(0);
	    Date endDate = new Date();
	    DBObject qTime2 = new BasicDBObject();
	    qTime2.put("crawl_nexttime", new BasicDBObject("$gt", startDate).append("$lte", endDate));

//	    DBObject qqq = QueryBuilder.start().put("crawl_nexttime").greaterThan(startDate).lessThanEquals(endDate).get();
//	    String sss4 = qqq.toString();
	    
	    String qTime = String.format("{\"$or\": [%1$s, %2$s]}",
				qTime1, qTime2.toString());	
		query = String.format("{\"$and\": [%1$s, %2$s]}",
				qTime, query);				    
		//sqlStatement += " ((crawl_nexttime is null and crawl_minimal_period != '999999') or crawl_nexttime <= now())";

	    if (interactiveOnly){
			String qInteractive = String.format("{\"crawl_mode\": { \"$ne\": %1$s}}",
					ISource.CRAWL_PROCESS_MODE_NONE);
			query = String.format("{\"$and\": [%1$s, %2$s]}",
					qInteractive, query);				    
	        //sqlStatement += " and (not crawl_mode = 0)";
	    }
	    
	    if (suspiciousOnly) {
			String qSuspicious1 = "{\"$and\": [{\"crawl_lasttime_end\" : { \"$exists\": false }, {\"crawl_lastpagecount\": { \"$lt\": 3}}]}";
			String qSuspicious = String.format("{\"$or\": [%1$s, {\"crawl_status\": { \"$ne\": \"0\"}}]}",
					qSuspicious1);		
			query = String.format("{\"$and\": [%1$s, %2$s]}",
					qSuspicious, query);				    
	       // sqlStatement += " and ((not crawl_lasttime_end is null and crawl_lastpagecount < 3) or not crawl_status = '0')";
	    }
		return query;
	}
}


