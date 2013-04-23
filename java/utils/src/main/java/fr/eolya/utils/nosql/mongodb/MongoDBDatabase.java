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

import java.net.UnknownHostException;
import com.mongodb.*;

/**
 * A MongoDB class representing a DB connection.
 */
public class MongoDBDatabase {

	private DB db;
	private String dbName = null;

	/**
	 * @param con              The MongoDB connection
	 * @param dbName           The MongoDB db name
	 * @return
	 * @throws UnknownHostException 
	 */
	public MongoDBDatabase(MongoDBConnection con, String dbName) {
		db = con.getMongo().getDB(dbName);
		db.setWriteConcern(WriteConcern.SAFE);
		this.dbName = dbName;
	}

	public DB getDb() {
		return db;
	}

	public String getDbName() {
		return dbName;
	}

	public void setWriteConcern(WriteConcern value) {
		db.setWriteConcern(value);
	}
	
	public WriteConcern getWriteConcern() {
		return db.getWriteConcern();
	}
	
	public boolean collectionExists(String collName) {
		return db.collectionExists(collName);
	}

	public void collectionDrop(String collName) {
		DBCollection coll = db.getCollection(collName);
		coll.drop();
	}

}

