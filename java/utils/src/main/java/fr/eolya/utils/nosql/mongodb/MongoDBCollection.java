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

import org.bson.types.ObjectId;
import com.mongodb.*;

/**
 * A MongoDB helper class representing.
 */
public class MongoDBCollection {

	//private MongoDBDatabase db;
	private DBCollection coll;
	private String collName = null;

	/**
	 * @param db         The MongoDB database
	 * @param collName   The MongoDB collection name
	 * @return
	 */
	public MongoDBCollection(MongoDBDatabase db, String collName) {
		//this.db = db;
		coll = db.getDb().getCollection(collName);
		this.collName = collName;
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
			fields.put(aTemp[i], "1");
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
			e.printStackTrace();
			return null;
		}
	}

	public String update(BasicDBObject docsearch, BasicDBObject doc) {
		try {
			coll.update( docsearch, doc );
			ObjectId id = (ObjectId)doc.get( "_id" );
			return id.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public boolean contains(BasicDBObject docsearch) {
		if (coll.count(docsearch)==1) return true;
		return false;
	}

	public void remove(BasicDBObject doc) {
		coll.remove(doc);
	}

	public void removeAll() {
		coll.remove(new BasicDBObject());
	}

	public void drop() {
		coll.drop();
	}

	public long size() {
		return coll.count(new BasicDBObject());
	}
}

