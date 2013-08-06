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
import java.util.Date;
import java.util.Map;

import com.mongodb.*;

import org.bson.types.ObjectId;

import fr.eolya.utils.nosql.mongodb.*;
import fr.eolya.crawler.queue.*;

/**
 * A MongoDB queue for source items quue
 * 
 * poped items are not deleted but marked as "done"
 * we keep trace of all referers for url
 * 
 * Key json fields :
 * 	url
 * 	depth
 * 	referer
 * 	_referers
 * 	_done
 */
public class MongoDBSourceItemsQueue implements ISourceItemsQueue {

	private MongoDBConnection con = null;
	private MongoDBDatabase db = null;
	private String collName;	
	private MongoDBCollection coll = null;
	private final Object collMonitor = new Object();
	private int sourceId;

	private final String timestampFieldName = "_timestamp";
	private final String hashFieldName = "_keyhash";
	private final String referersFieldName = "_referers";
	private final String createdFieldName = "_created";

	private final String sourceIdFieldName = "id_source";
	private final String uniqueKeyFieldName = "url";
	private final String depthFieldName = "depth";
	private final String refererFieldName = "referers";

	private long startTime; 
	private long size; 
	private long doneCount; 
	private final String stateId = "000000000000000000000000";
	
	private boolean rescan;
	private int startDepth;
	private boolean checkDeletionMode;

	/** 
	 * Connect to the DB and create an empty queue collection if it doesn't exist 
	 * 
	 * @param dbHost         		The MongoDB database host
	 * @param dbPort         		The MongoDB database port
	 * @param dbName         		The MongoDB database name
	 * @param collName     			The MongoDB collection name
	 * @param userName         		The MongoDB user name
	 * @param userPassword         	The MongoDB user password
	 * @return
	 * @throws UnknownHostException 
	 */
	public MongoDBSourceItemsQueue(int sourceId, MongoDBConnection con, String dbName, String collName) throws UnknownHostException {

		this.con = con;
		this.db = new MongoDBDatabase(this.con, dbName);

		this.sourceId = sourceId;

		this.collName = collName + "_" + String.valueOf(sourceId);
		//this.coll = new MongoDBCollection(db, this.collName);

		if (db.getDb().collectionExists(this.collName)) {
			this.coll = new MongoDBCollection(db,this.collName);
		} else {
			this.coll = createCollection();
		}
		rescan = false;
		startDepth = 0;
		checkDeletionMode = false;
		readState();
/*
		startTime = 0; 
		size = 0; 
		doneCount = 0;
		*/
	}

	public void close() {
		if (this.con==null) return;
		this.con.close();
	}

	/** 
	 * Drop the queue collection and create an new empty queue collection
	 * 
	 * @return
	 */
	public void reset() {
		synchronized (collMonitor) {
			if (db.getDb().collectionExists(collName)) {
				// delete existing collection
				coll = new MongoDBCollection(db,collName);
				coll.drop();
			}
			// create new collection with 
			coll = createCollection();
		}		
		startTime = 0; 
		size = 0; 
		doneCount = 0;
	}

	/** 
	 * Create an new empty queue collection
	 * 
	 * @return
	 */
	private MongoDBCollection createCollection() {
		// create new collection with 
		MongoDBCollection coll = new MongoDBCollection(db,collName);
		coll.createIndex(hashFieldName, false);
		coll.createIndex(timestampFieldName, false);

		BasicDBObject doc = new BasicDBObject();
		doc.put("_id", new ObjectId(stateId));
		doc.put("starttime", 0);
		coll.add(doc);

		return coll;
	}

	/*
	private long getDBQueueSize() {
		String query = "{\"" + timestampFieldName + "\": {\"$gt\": " + String.valueOf(startTime) + "}}";
		BasicDBObject docsearch = MongoDBHelper.JSON2BasicDBObject(query);
		DBCursor cur = coll.getColl().find(docsearch);
		return cur.count();
	}
	*/
	
	/** 
	 * Read queue state
	 * 
	 * starttime -> timestamp (log value) of the current crawl start
	 * 
	 * for an item of the collection :
	 * 		timestamp < starttime	=> not in queue
	 * 		timestamp > starttime	=> in queue
	 * 		timestamp = starttime	=> done
	 * 
	 * @return last start time
	 */
	private Long readState() {

		// read start time
		BasicDBObject docsearch = new BasicDBObject();
		docsearch.put("_id", new ObjectId(stateId));
		DBCursor cur = coll.getColl().find(docsearch);
		if (cur.count()>1) return null;
		if (cur.count()==0) return null;

		BasicDBObject doc = (BasicDBObject) cur.next();
		startTime = doc.getLong("starttime");

		// read sizes
		if (startTime==0 || rescan || startDepth>0) {
// TODO v4 : in fact startTime never = 0 !!!
			if (!rescan) {
				// previous crawl terminated fine
				
				if (startDepth>0) {
					//String queryTimeStamp =	"{\"" + timestampFieldName + "\": {\"$ne\": " + String.valueOf(startTime) + "}}";		
					//String queryMode = "{\"crawl_mode\":a}";			
					//String query = "{\"$and\": [" + queryTimeStamp + ", " + queryMode + "]}";
					String query = "{\"depth\":" + String.valueOf(startDepth) + "}";
					size = count(query);				
				} else {
					size = 0; 
				}
			} else {
				// get queue size : timestamp != starttime => in queue
				//String query = "{\"" + timestampFieldName + "\": {\"$ne\": " + String.valueOf(startTime) + "}}";		

				String queryTimeStamp =	"{\"" + timestampFieldName + "\": {\"$ne\": " + String.valueOf(startTime) + "}}";		
				String queryMode = "{\"crawl_mode\":\"a\"}";			
				String query200 = "{\"crawl_status\":200}";			
				String query = "{\"$and\": [" + queryTimeStamp + ", " + queryMode + ", " + query200 + "]}";

				size = count(query);				
			}
			doneCount = 0;
		}
		else {
			// previous crawl was not terminated

			// get queue size : timestamp > starttime => in queue
			String query = "{\"" + timestampFieldName + "\": {\"$gt\": " + String.valueOf(startTime) + "}}";			
			//docsearch = MongoDBHelper.JSON2BasicDBObject(query);
			//cur = coll.getColl().find(docsearch);
			//size = cur.size();
			size = count(query);
			
			// get done count : timestamp = starttime => done
			query = "{\"" + timestampFieldName + "\": "+ String.valueOf(startTime) + "}";
			//docsearch = MongoDBHelper.JSON2BasicDBObject(query);
			//cur = coll.getColl().find(docsearch);
			//doneCount = cur.size();
			doneCount = count(query);
		}
		return startTime;
	}	
	
	private long count(String query) {
		BasicDBObject docsearch = new BasicDBObject();
		docsearch = MongoDBHelper.JSON2BasicDBObject(query);
		DBCursor cur = coll.getColl().find(docsearch);
		return cur.size();
	}


	/** 
	 * Write new queue state
	 * 
	 * @return new start time
	 */
	private Long writeState(long time) {
		try {
			Thread.sleep(10); // ms
		} catch (InterruptedException e) {}
		BasicDBObject docsearch = new BasicDBObject();
		docsearch.put("_id", new ObjectId(stateId));
		BasicDBObject doc = new BasicDBObject();
		doc.put("_id", new ObjectId(stateId));
		doc.put("starttime", time);
		coll.update(docsearch, doc);	
		startTime = time;
		return startTime;
	}

	/** 
	 * Start queue
	 * 
	 * @return new start time
	 */
	public Long start() {
		long time = readState();
		if (time==0)
			return writeState(new Date().getTime());
		else
			return time;
	}

	/** 
	 * Restart queue
	 * 
	 * @return new start time
	 */
	public Long reStart() {
		return reStart(0);
	}
	public Long reStart(int startDepth) {
		this.startDepth = startDepth;
		long time2 = writeState(new Date().getTime());
		//if (startDepth>0) {
		//	long time = readState();
		//	time = time;
		//}
		return time2;
	}
	public int getStartDepth() {
		return this.startDepth;
	}
	public int getCurrentMaxDepth() {
		//String queryTimeStamp =	"{\"" + timestampFieldName + "\": {\"$ne\": " + String.valueOf(startTime) + "}}";		
		//String queryMode = "{\"crawl_mode\":a}";			
		//String query = "{\"$and\": [" + queryTimeStamp + ", " + queryMode + "]}";
		
		String query1 = "{\"crawl_mode\":\"a\"}";
		String query2 = "{\"crawl_status\":200}";
		String query = "{\"$and\": [" + query1 + ", " + query2 + "]}";

		BasicDBObject docsearch = new BasicDBObject();
		docsearch = MongoDBHelper.JSON2BasicDBObject(query);
		
		DBCursor cur = coll.getColl().find(docsearch).sort(new BasicDBObject("depth", -1));		

		if (cur.count()==0) return 0;
		BasicDBObject doc = (BasicDBObject) cur.next();
		return doc.getInt("depth");
	}
	public boolean setCheckDeletionMode() {
		if (checkDeletionMode) return true;
		if (rescan || startDepth > 0) return false;

		synchronized (collMonitor) {
			String query1 =	"{\"" + timestampFieldName + "\": {\"$gt\": " + String.valueOf(startTime) + "}}";				
			String query2_1 = "{\"" + timestampFieldName + "\": {\"$lt\": " + String.valueOf(startTime) + "}}";		
			String query2_2 = "{\"crawl_status\":200}";
			String query_2 = "{\"$and\": [" + query2_1 + ", " + query2_2 + "]}";
			String query = "{\"$or\": [" + query1 + ", " + query_2 + "]}";
			size = count(query);				
			checkDeletionMode = true;
		}
		return true;
	}
	public boolean isCheckDeletionMode() {
		return checkDeletionMode;
	}
	
	
	/** 
	 * Restart queue
	 * 
	 * @return new start time
	 */
	public Long reScan() {
		rescan = true;
		writeState(new Date().getTime());
		return readState();
	}

	/** 
	 * Stop queue
	 * 
	 * @return new start time
	 */
	public Long stop() {
		return writeState(0);
	}

	/** 
	 * Push a new item
	 * 
	 * @return success or not
	 */
	public boolean push(Map<String,Object> item) throws QueueIncoherenceException, QueueInvalidDataException {

		boolean ret = true;

		BasicDBObject doc = new BasicDBObject(item);
		String keyValue = doc.getString(uniqueKeyFieldName);
		String depth = doc.getString(depthFieldName);
		String sourceId = doc.getString(sourceIdFieldName);

		if (sourceId==null || keyValue==null || depth==null) throw new QueueInvalidDataException("Missing fields in json");
		if (Integer.parseInt(sourceId)!=this.sourceId) throw new QueueInvalidDataException("Invalid source id in json");

		String referer = doc.getString(refererFieldName);

		// Get existing item in queue
		String currentDepth = null;
		String currentReferers = null;
		long currentTimestamp = 0;
		BasicDBObject docsearch = new BasicDBObject();
		docsearch.put(sourceIdFieldName, Integer.parseInt(sourceId));
		docsearch.put(hashFieldName, keyValue.hashCode());

		synchronized (collMonitor) {
			BasicDBObject curDoc = null;
			DBCursor cur = coll.getColl().find(docsearch);
			if (cur.count()>0) {
				while (cur.hasNext() && curDoc==null) {
					curDoc = (BasicDBObject) cur.next();
					if (!keyValue.equals(doc.getString(uniqueKeyFieldName))) {
						curDoc = null;
					}
				}
				if (curDoc!=null) {
					currentDepth = curDoc.getString(depthFieldName);
					currentReferers = curDoc.getString(referersFieldName);
					currentTimestamp = curDoc.getLong(timestampFieldName);
	
					/*
					 * Remember : for an item of the collection :
					 * 		timestamp < starttime	=> not in queue
					 * 		timestamp > starttime	=> in queue
					 * 		timestamp = starttime	=> done
					 */
					if ((Long.parseLong(depth) >= Long.parseLong(currentDepth)) && (currentTimestamp>=startTime)) return false;
				}
			}
			
			// build new doc
			doc.put(hashFieldName, keyValue.hashCode());
			doc.put(timestampFieldName, new Date().getTime());

			if (referer!=null) {
				if (currentReferers==null) {
					currentReferers = referer;			
				} else {
					currentReferers += "/n" + referer;
				}
			}
			if (currentReferers!=null) {
				doc.put(referersFieldName, currentReferers);
			}
			if (curDoc!=null) {
				coll.update(curDoc, doc);	
				// TODO : decrease done size in some case ???
			} else {
				doc.put(createdFieldName, new Date().getTime());
				coll.add(doc);
			}
			size++;
			return ret;
		}
	}

	/** 
	 * Check if item is in queue
	 * 
	 * @return true or false
	 */
	public boolean contains(String keyValue) {	
		return (getInternal(keyValue, false)!=null);			
	}

	/** 
	 * Check if item is in queue
	 * 
	 * @return requested field value or null
	 */
	//private String contains(String keyValue, String returnedField) {	
	//	BasicDBObject doc = getInternal(keyValue, false);
	//	if (doc==null) return null;
	//	return doc.getString(returnedField);			
	//}

	private BasicDBObject getInternal(String keyValue, boolean done) {		

		String queryTimeStamp;
		if (done) {
			if (startDepth==0)
				queryTimeStamp = "{\"" + timestampFieldName + "\":" + String.valueOf(startTime) + "}";	
			else {
				queryTimeStamp = "{\"" + timestampFieldName + "\": {\"$lte\": " + String.valueOf(startTime) + "}}";			
			}
		}
		else
			queryTimeStamp = "{\"" + timestampFieldName + "\": {\"$gt\": " + String.valueOf(startTime) + "}}";			

		String queryHash = "{\"" + hashFieldName + "\":" + keyValue.hashCode() + "}";			
		String query = "{\"$and\": [" + queryTimeStamp + ", " + queryHash + "]}";

		BasicDBObject docsearch = MongoDBHelper.JSON2BasicDBObject(query);

		//synchronized (collMonitor) {
		DBCursor cur = coll.getColl().find(docsearch);
		if (cur.count()==0) return null;
		while (cur.hasNext()) {
			BasicDBObject doc = (BasicDBObject) cur.next();
			if (keyValue.equals(doc.getString(uniqueKeyFieldName))) {
				return doc;
			}
		}
		//}
		return null;
	}

	/** 
	 * Get the older item (FIFO)
	 * 
	 * @return the item or null
	 */
	public Map<String,Object> pop() {
		return pop(null);
	}

	/** 
	 * Get the older item (FIFO)
	 * 
	 * @return the item or null
	 */
	public Map<String,Object> pop(String extraSortField) {

		String query;
		if (!rescan) {			
			if (startDepth==0) {
				if (checkDeletionMode) {
					String query1 =	"{\"" + timestampFieldName + "\": {\"$gt\": " + String.valueOf(startTime) + "}}";				
					String query2_1 = "{\"" + timestampFieldName + "\": {\"$lt\": " + String.valueOf(startTime) + "}}";		
					String query2_2 = "{\"crawl_status\":200}";
					String query_2 = "{\"$and\": [" + query2_1 + ", " + query2_2 + "]}";
					query = "{\"$or\": [" + query1 + ", " + query_2 + "]}";
				} else {
					query =	"{\"" + timestampFieldName + "\": {\"$gt\": " + String.valueOf(startTime) + "}}";		
				}
			} else {
				String query1 =	"{\"" + timestampFieldName + "\": {\"$gt\": " + String.valueOf(startTime) + "}}";				
				String query2_1 = "{\"" + timestampFieldName + "\": {\"$lt\": " + String.valueOf(startTime) + "}}";		
				String query2_2 = "{\"depth\":" + String.valueOf(startDepth) + "}";
				String query2_3 = "{\"crawl_status\":200}";
				String query_2 = "{\"$and\": [" + query2_1 + ", " + query2_2 + ", " + query2_3 + "]}";
				query = "{\"$or\": [" + query1 + ", " + query_2 + "]}";
			}
		} else {
			String queryTimeStamp =	"{\"" + timestampFieldName + "\": {\"$ne\": " + String.valueOf(startTime) + "}}";		
			String queryMode = "{\"crawl_mode\":\"a\"}";			
			String query200 = "{\"crawl_status\":200}";			
			query = "{\"$and\": [" + queryTimeStamp + ", " + queryMode + ", " + query200 + "]}";
		}
		BasicDBObject docsearch = MongoDBHelper.JSON2BasicDBObject(query);

		DBCursor cur = null;
		synchronized (collMonitor) {
			if (extraSortField!=null) {
				cur = coll.getColl().find(docsearch).sort(new BasicDBObject(extraSortField, 1));
			}
			else{
				cur = coll.getColl().find(docsearch);
			}
			if (cur.hasNext()) {
				BasicDBObject doc = (BasicDBObject) cur.next();
				BasicDBObject doc2 = (BasicDBObject) doc.copy();
				doc2.put(timestampFieldName, startTime);
				coll.update(doc, doc2);		
				size--;
				doneCount++;
				//return doc.toMap();
				return MongoDBHelper.BasicDBObject2Map(doc2);
			}
		}
		return null;	
	}
	
	/** 
	 * Get queue size
	 * 
	 * @return size
	 */
	public long size() {
		return size;
	}

	/** 
	 * Get queue size
	 * 
	 * @return size
	 */
	public long getQueueSize() {
		return size();
	}

	/** 
	 * Get done item count
	 * 
	 * @return count
	 */
	public long getDoneQueueSize() {
		return doneCount;
	}

	/** 
	 * Get done item
	 * 
	 * @return item
	 */
	public Map<String,Object> getDone(String keyValue) {
		BasicDBObject doc = getInternal(keyValue, true);
		if (doc==null) return null;
		//return doc.toMap();
		return MongoDBHelper.BasicDBObject2Map(doc);
	}

	/** 
	 * Check if item was done
	 * 
	 * @return true or false
	 */
	public boolean isDone(String keyValue) {
		// TODO: optimize by just counting but not get back the document object
		BasicDBObject doc = getInternal(keyValue, true);
		if (doc==null) return false;
		return true;
	}

	public boolean updateDone(Map<String,Object> item) {
		// TODO: optimize 1 search + 1 update !!! may be the MongoDB _id is in the json
		BasicDBObject doc = new BasicDBObject(item);
		String keyValue = doc.getString(uniqueKeyFieldName);

		synchronized (collMonitor) {
			BasicDBObject curDoc = getInternal(keyValue, true);
			if (curDoc==null) return false;
			coll.update(curDoc, doc);	
		}		
		return true;
	}
	
	public String getCreated(Map<String,Object> item) {
		if (item.get(createdFieldName) == null) return ""; // TODO v4 : warning String.valueOf(null) return "null"
		return String.valueOf(item.get(createdFieldName));
	}
}

