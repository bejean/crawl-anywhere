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
package fr.eolya.utils.nosql.mongodb;

import java.util.ArrayList;

import org.bson.types.ObjectId;
import com.mongodb.*;

/**
 * A MongoDB helper class representing.
 */
public class MongoDBCollection {

	private DBCollection coll;
	private String collName = null;

	/**
	 * @param db         The MongoDB database
	 * @param collName   The MongoDB collection name
	 * @return
	 */
	public MongoDBCollection(MongoDBDatabase db, String collName) {
		coll = db.getDb().getCollection(collName);
		this.collName = collName;
	}

	public MongoDBCollection(DBCollection coll) {
		this.coll = coll;
	}

	public DBCollection getColl() {
		return coll;
	}

	public String getCollName() {
		return collName;
	}

	public void createIndex(String fieldsName, boolean unique) {
		String[] aTemp=fieldsName.split(",");
		BasicDBObject fields = new BasicDBObject();	
		for (int i=0; i<aTemp.length; i++) {
			fields.put(aTemp[i], 1);
		}
		BasicDBObject options = new BasicDBObject();	
		options.put("unique", unique);
		coll.ensureIndex(fields, options);
	}

	public String add(BasicDBObject doc) {
		try {
			coll.insert( doc );
			ObjectId id = (ObjectId)doc.get( "_id" );
			return id.toString();
		} catch (Exception e) {
			//e.printStackTrace();
			return null;
		}
	}

	public int update(BasicDBObject docsearch, BasicDBObject doc) {
		try {
			WriteResult wr = coll.update( docsearch, doc );
			return wr.getN();
		} catch (Exception e) {
			//e.printStackTrace();
			return -1;
		}
	}

	public boolean contains(BasicDBObject docsearch) {
		if (coll.count(docsearch)==1) return true;
		return false;
	}

	public int remove(BasicDBObject doc) {
		WriteResult wr = coll.remove(doc);
		return wr.getN();
	}

	public int removeAll() {
		WriteResult wr = coll.remove(new BasicDBObject());
		return wr.getN();
	}

	public void drop() {
		coll.drop();
	}

	public long size() {
		return coll.count(new BasicDBObject());
	}
	
	public long count(String query) {
		BasicDBObject docsearch = null;
		if (query==null || "".equals(query)) {
			docsearch = new BasicDBObject();
		} else {
			docsearch = MongoDBHelper.JSON2BasicDBObject(query);
		}
		return coll.count(docsearch);
	}

	public ArrayList<String> getValues(BasicDBObject docsearch, String field) {
		DBCursor cur = null;
		if (docsearch!=null) 
			cur = coll.find(docsearch);
		else
			cur = coll.find();
		if (cur.count()==0) return null;
		ArrayList<String> values = new ArrayList<String>();
		while (cur.hasNext()) {
			BasicDBObject doc = (BasicDBObject) cur.next();
			values.add((String)doc.getString(field));
		}
		return values;
	}
	
	public String getValue(BasicDBObject docsearch, String field) {
		DBCursor cur = coll.find(docsearch);
		if (cur.count()!=1) return null;
		BasicDBObject doc = (BasicDBObject) cur.next();
		return (String)doc.getString(field);
	}
	
//	public HashMap<String,String> getItemMap(BasicDBObject docsearch) {
//		DBCursor cur = coll.find(docsearch);
//		if (cur.count()!=1) return null;
//		BasicDBObject doc = (BasicDBObject) cur.next();		
//		return MongoDBHelper.BasicDBObject2Map(doc);
//	}

	public String getJson(BasicDBObject docsearch) {
		BasicDBObject doc = get(docsearch);
		if (doc==null) return null;
		return (String)doc.toString();
	}
	
	public BasicDBObject get(BasicDBObject docsearch) {
		DBCursor cur = coll.find(docsearch);
		if (cur.count()!=1) return null;
		BasicDBObject doc = (BasicDBObject) cur.next();
		return doc;
	}
	
	public DBCursor getCursor(BasicDBObject docsearch) {
		return coll.find(docsearch);
	}
}

