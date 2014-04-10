package fr.eolya.crawler.database.mongodb;

import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;

import fr.eolya.crawler.connectors.ISource;
import fr.eolya.crawler.database.ICrawlerDB;
import fr.eolya.utils.XMLConfig;
import fr.eolya.utils.json.JSONHelper;
import fr.eolya.utils.nosql.mongodb.MongoDBCollection;
import fr.eolya.utils.nosql.mongodb.MongoDBConnection;
import fr.eolya.utils.nosql.mongodb.MongoDBDatabase;
import fr.eolya.utils.nosql.mongodb.MongoDBHelper;

public class MongoDBCrawlerDB implements ICrawlerDB {

	private MongoDBConnection con;
	private MongoDBDatabase db;
	private MongoDBDatabase dbQueues;

	private final Object sourcesCollMonitor = new Object();

	public MongoDBCrawlerDB(MongoDBConnection con, String dbName, String dbNameQueues) throws UnknownHostException {
		this.con = con;
		this.db = new MongoDBDatabase(this.con, dbName);
		if (StringUtils.isNotBlank(dbNameQueues)) 
			this.dbQueues = new MongoDBDatabase(this.con, dbNameQueues);
		else 
			this.dbQueues = this.db;
	}

	public String getVersion() {
		MongoDBCollection coll = new MongoDBCollection(db,"infos");
		String query = "{\"name\": \"version\"}";	
		return coll.getValue(MongoDBHelper.JSON2BasicDBObject (query), "value");		
	}

	public String getSourceClass(String sourceType) {
		MongoDBCollection coll = new MongoDBCollection(db,"plugins");
		String query = String.format("{\"id\": %1$s}", sourceType);	
		return coll.getValue(MongoDBHelper.JSON2BasicDBObject (query), "class_java");		
	}

	public HashMap<String,String> getTarget(String targetId) {
		MongoDBCollection coll = new MongoDBCollection(db,"targets");
		String query = String.format("{\"id\": %1$s}", targetId);	
		try {
			// TODO : replace by getItemMap
			return JSONHelper.getJSONMapString(coll.getJson(MongoDBHelper.JSON2BasicDBObject (query)));
		} catch (Exception e) {
			//e.printStackTrace();
			return null;
		} 
	}

	public void close() {
		if (this.con==null) return;
		this.con.close();
	}

	public void fixStartupSourcesStatus () {
		// TODO : should work on a specific account or all account of an engine if either engine_id or account_id are specified ???
		MongoDBCollection coll = new MongoDBCollection(db,"sources");
		String query = String.format("{\"$or\": [{\"crawl_process_status\": \"1\"}, {\"crawl_process_status\": \"5\"}, {\"_poped\": true}]}");	

		BasicDBObject docsearch = MongoDBHelper.JSON2BasicDBObject(query);
		synchronized (sourcesCollMonitor) {
			DBCursor cur = coll.getColl().find(docsearch);
			if (cur.count()==0) return;
			while (cur.hasNext()) {
				BasicDBObject doc = (BasicDBObject) cur.next();
				BasicDBObject doc2 = (BasicDBObject) doc.copy();
				doc2.put("crawl_nexttime", new Date());
				doc2.put("crawl_process_status", ISource.CRAWL_PROCESS_MODE_NONE);
				doc2.put("crawl_priority", "2");
				doc2.put("running_crawl_lastupdate", null);
				doc2.put("_poped", false);
				coll.update(doc, doc2);					
			}
		}
	}

	public boolean updateSourceStatusStartup(int id, long queueSize, long doneQueueSize) {
		MongoDBCollection coll = new MongoDBCollection(db,"sources");
		String query = String.format("{\"id\": %1$s}", id);	

		BasicDBObject docsearch = MongoDBHelper.JSON2BasicDBObject(query);

		synchronized (sourcesCollMonitor) {
			DBCursor cur = coll.getColl().find(docsearch);

			if (cur.count()!=1) return false;

			BasicDBObject doc = (BasicDBObject) cur.next();
			BasicDBObject doc2 = (BasicDBObject) doc.copy();

			if (doneQueueSize>0 || queueSize>0) {
				doc2.put("running_crawl_item_processed", doneQueueSize);
				doc2.put("running_crawl_item_to_process", queueSize);

			} else {
				doc2.put("running_crawl_item_processed", 0);
				doc2.put("running_crawl_item_to_process", 0);
			}
			doc2.put("crawl_lasttime_start", new Date());	        
			doc2.put("running_crawl_lastupdate", new Date());	 
			doc2.put("crawl_process_status", ISource.CRAWL_PROCESS_STATUS_CRAWLING);
			doc2.put("crawl_mode", ISource.CRAWL_PROCESS_MODE_NONE);
			doc2.put("_poped", false);
			coll.update(doc, doc2);				
		}
		return true;
	}

	public boolean updateSourceProcessingInfo(int id, long queueSize, long doneQueueSize, String processingInfo) {
		MongoDBCollection coll = new MongoDBCollection(db,"sources");
		String query = String.format("{\"id\": %1$s}", id);	

		BasicDBObject docsearch = MongoDBHelper.JSON2BasicDBObject(query);
		synchronized (sourcesCollMonitor) {
			DBCursor cur = coll.getColl().find(docsearch);
			if (cur.count()!=1) return false;
			BasicDBObject doc = (BasicDBObject) cur.next();
			BasicDBObject doc2 = (BasicDBObject) doc.copy();
			doc2.put("running_crawl_item_processed", doneQueueSize);
			doc2.put("running_crawl_item_to_process", queueSize);
			doc2.put("running_crawl_lastupdate", new Date());	 
			doc2.put("processing_info", processingInfo);	 
			coll.update(doc, doc2);				
		}
		return true;
	}

	private long getSourcePageCount(int id) {
		MongoDBCollection coll = new MongoDBCollection(dbQueues,"pages_" + String.valueOf(id));
		return coll.count("");
	}

	private long getSourcePageCountSuccess(int id) {
		MongoDBCollection coll = new MongoDBCollection(dbQueues,"pages_" + String.valueOf(id));
		String query = "{\"crawl_status\": 200}";	
		return coll.count(query);
	}

	private long getSourcePageCountPushed(int id) {
		MongoDBCollection coll = new MongoDBCollection(dbQueues,"pages_" + String.valueOf(id));
		String query = "{\"crawl_status\": 200, \"crawl_mode\": \"a\"}";	
		return coll.count(query);
	}

	public boolean updateSourceStatusStop(ISource src, long queueSize, long doneQueueSize, boolean crawlerStopRequested, boolean pause, boolean pauseBySchedule, XMLConfig config) {

		MongoDBCollection coll = new MongoDBCollection(db,"sources");
		String query = String.format("{\"id\": %1$s}", src.getId());	

		BasicDBObject docsearch = MongoDBHelper.JSON2BasicDBObject(query);

		synchronized (sourcesCollMonitor) {
			DBCursor cur = coll.getColl().find(docsearch);

			if (cur.count()!=1) return false;

			BasicDBObject doc = (BasicDBObject) cur.next();
			BasicDBObject doc2 = (BasicDBObject) doc.copy();


			doc2.put("crawl_lastpagecount", doneQueueSize);	 
			doc2.put("crawl_pagecount", getSourcePageCount(src.getId()));	 
			doc2.put("crawl_pagecount_success", getSourcePageCountSuccess(src.getId()));	 
			doc2.put("crawl_pagecount_pushed", getSourcePageCountPushed(src.getId()));	 

			// TODO: v4
			/*
            if (item.getDepth()==0) {
                 rootUrlStatusCode = statusCode;
                 rootUrlStatusMessage = statusMessage.trim();
             }
			 */
			//doc2.put("crawl_status", rootUrlStatusCode);	 
			//doc2.put("crawl_status", rootUrlStatusMessage);	 

			/*
            s += ",crawl_lastpagecount = " + String.valueOf(done.size());
            s += ",crawl_pagecount = '" + pageCount + "'";
            s += ",crawl_pagecount_success = '" + pageCountSuccess + "'";
            s += ",crawl_pagecount_pushed = '" + pageCountPushed + "'";
            s += ",crawl_status = '" + rootUrlStatusCode + "'";
            s += ",crawl_status_message = '" + rootUrlStatusMessage.replace("'", "\\'") + "'";
			 */

			if (queueSize==0)
				doc2.put("crawl_firstcompleted", "1");	 

			if (pause) {
				doc2.put("running_crawl_item_processed", doneQueueSize);
				doc2.put("running_crawl_item_to_process", queueSize);
				if (pauseBySchedule) {
					doc2.put("crawl_process_status", ISource.CRAWL_PROCESS_STATUS_PAUSE_BY_SCHEDULE);
				}
				doc2.put("crawl_priority", 4);
			} else {
				doc2.put("running_crawl_item_processed", 0);
				doc2.put("running_crawl_item_to_process", 0);
				doc2.put("crawl_process_status", ISource.CRAWL_PROCESS_MODE_NONE);            	
				//if (crawlerStopRequested) {
				//	doc2.put("crawl_priority", 2);
				//}
				//else {
				doc2.put("crawl_priority", 0);

				int crawlPeriod;
				if (src.isOptimized())            
					crawlPeriod = Integer.parseInt(config.getProperty("/crawler/param[@name='period_optimized_source']", "24"));
				else
					crawlPeriod = Integer.parseInt(config.getProperty("/crawler/param[@name='period']", "168"));

				Date read_nexttime = getNextCrawlStartDate(src.getCurrentCrawlNextTime(), crawlPeriod, Integer.parseInt(src.getCrawlPeriod())); 
				doc2.put("crawl_nexttime", read_nexttime);
				//}
			}

			doc2.put("running_crawl_lastupdate", new Date());	 
			doc2.put("crawl_lasttime_end", new Date());	 
			doc2.put("crawl_mode", ISource.CRAWL_PROCESS_MODE_NONE);

			if (src.getExtra()!=null && src.getExtra().asXml()!=null) 
				doc2.put("extra", src.getExtra().asXml().replaceAll("'", "''"));
			else
				doc2.put("extra", "");

			coll.update(doc, doc2);		
			
			updateSourceLog(src, 0);
		}
		return true;
	}
	
	public boolean updateSourceLog(ISource src, int retention) {
		MongoDBCollection coll = new MongoDBCollection(db,"sources_log");
		coll.createIndex("id_source", false);
		coll.createIndex("createtime", false);

		BasicDBObject doc = new BasicDBObject();
		doc.put("id_source", src.getId());
		doc.put("createtime", new Date().getTime());
		doc.put("log",src.memLogGet());

		coll.add(doc);
		
		return true;
	}

	private static Date getNextCrawlStartDate(Date currentCrawlNextTime, int period, int sourcePeriod) {

		Date now = new java.util.Date();
		Date nextCrawlStartDate = null;

		if (sourcePeriod!=0) period = sourcePeriod;
		if (sourcePeriod==999999) period = 867834; // on demand = 99 years

		while (nextCrawlStartDate==null || nextCrawlStartDate.before(now)) {
			Date tempDate = null; 
			if (currentCrawlNextTime==null) {
				tempDate = new Date();
			}
			else {
				if (nextCrawlStartDate==null)
					tempDate = currentCrawlNextTime;
				else
					tempDate = nextCrawlStartDate;

			}
			long t = tempDate.getTime() + (period*3600*1000);
			nextCrawlStartDate = new Date(t);
		}
		return nextCrawlStartDate;        
	}

	public String getSourceCrawlProcessStatus(int id) {
		MongoDBCollection coll = new MongoDBCollection(db,"sources");
		String query = String.format("{\"id\": %1$s}", id);	
		BasicDBObject docsearch = MongoDBHelper.JSON2BasicDBObject(query);
		DBCursor cur = coll.getColl().find(docsearch);
		if (cur.count()!=1) return null;
		BasicDBObject doc = (BasicDBObject) cur.next();
		return doc.getString("crawl_process_status");
	}

}
