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

import com.mongodb.*;
import com.mongodb.util.JSON;
import fr.eolya.utils.nosql.mongodb.*;
import fr.eolya.crawler.queue.*;

/**
 * A MongoDB queue for web site url
 * 
 * poped url are not deleted but marked as "done"
 * we keep trace of all referers for url
 * 
 * Key json fields :
 * 	url
 * 	depth
 * 	referer
 * 	_referers
 * 	_done
 */
public class MongoDBWebSiteUrlFifoQueue implements IFifoQueue {

	private MongoDBDatabase db;
	private String collName;	
	private MongoDBCollection coll = null;
	private final Object collMonitor = new Object();

	private final String hashFieldName = "keyHash";
	private final String uniqueKeyFieldName = "url";
	private final String depthFieldName = "depth";
	private final String refererFieldName = "referer";

	/**
	 * @param db         			The MongoDB database
	 * @param collName     			The MongoDB collection name
	 * @param uniqueKeyFieldName   	unique key field name for documents in the queue
	 * @return
	 */
	public MongoDBWebSiteUrlFifoQueue(MongoDBDatabase db, String collName) {
		this.db = db;
		this.collName = collName;
	}

	public void reset() {
		synchronized (collMonitor) {
			if (db.getDb().collectionExists(collName)) {
				// delete existing collection
				coll = new MongoDBCollection(db,collName);
				coll.drop();
			}
			// create new collection with 
			coll = new MongoDBCollection(db,collName);
			coll.createIndex(hashFieldName, false);
		}		
	}

	public long size() {
		DBCursor cur = null;
		BasicDBObject docsearch = new BasicDBObject();
		docsearch.put("_done", "false");
		cur = coll.getColl().find(docsearch);
		return cur.size();
	}

	public boolean push(String json) throws QueueIncoherenceException, QueueInvalidDataException {
		boolean ret = true;
		BasicDBObject doc = (BasicDBObject) JSON.parse(json);
		String keyValue = doc.getString(uniqueKeyFieldName);
		String depth = doc.getString(depthFieldName);
		if (keyValue==null || depth==null) throw new QueueInvalidDataException("missing fiels in json");

		String referer = doc.getString(refererFieldName);

		// get existing item in queue
		String currentDepth = null;
		String currentReferers = null;
		String currentDone = null;
		BasicDBObject docsearch = new BasicDBObject();
		docsearch.put(hashFieldName, keyValue.hashCode());

		synchronized (collMonitor) {
			BasicDBObject curDoc = null;
			DBCursor cur = coll.getColl().find(docsearch);
			if (cur.count()>0) {
				if (cur.count()>1) throw new QueueIncoherenceException("more than one item");
				curDoc = (BasicDBObject) cur.next();
				if (keyValue.equals(curDoc.getString(uniqueKeyFieldName))) {
					currentDepth = curDoc.getString(depthFieldName);
					currentReferers = curDoc.getString("_referers");
					currentDone = curDoc.getString("_done");
				}
			}

			// build new doc
			doc.put(hashFieldName, keyValue.hashCode());
			if ((curDoc==null) || (Long.parseLong(depth) < Long.parseLong(currentDepth))) {
				doc.put("_done", "false");
			} else {
				ret = false;
				doc.put("_done", currentDone);
			}
			if (referer!=null) {
				if (currentReferers==null) {
					currentReferers = referer;			
				} else {
					currentReferers += "/n" + referer;
				}
			}
			if (currentReferers!=null) {
				doc.put("_referers", currentReferers);
			}
			if (curDoc!=null) {
				coll.remove(curDoc);
			}
			coll.add(doc);
			return ret;
		}
	}

	public boolean contains(String keyValue) {	
		return (containsInternal(keyValue, uniqueKeyFieldName)!=null);			
	}

	public String contains(String keyValue, String returnedField) {	
		return containsInternal(keyValue, returnedField);			
	}

	private String containsInternal(String keyValue, String returnedField) {		
		BasicDBObject docsearch = new BasicDBObject();
		docsearch.put(hashFieldName, keyValue.hashCode());
		docsearch.put("_done", "false");
		synchronized (collMonitor) {
			DBCursor cur = coll.getColl().find(docsearch);
			if (cur.count()==0) return null;
			//if (cur.count()==1) return true;
			while (cur.hasNext()) {
				BasicDBObject doc = (BasicDBObject) cur.next();
				if (keyValue.equals(doc.getString(uniqueKeyFieldName))) {
					return doc.getString(returnedField);
				}
			}
		}
		return null;
	}

	public String pop() {
		return pop(null);
	}

	public String pop(String extraSortField) {
		DBCursor cur = null;
		
		BasicDBObject docsearch = new BasicDBObject();
		docsearch.put("_done", "false");

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
				doc2.put("_done", "true");
				coll.update(doc, doc2);					

				return doc.toString();
			}
		}
		return null;	
	}

}

