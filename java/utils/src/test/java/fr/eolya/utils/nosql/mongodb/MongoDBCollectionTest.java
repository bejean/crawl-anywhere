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
import java.util.Date;

import junit.framework.TestCase;
import org.junit.Test;

import com.mongodb.BasicDBObject;

import fr.eolya.utils.nosql.DBConnectionFactory;

public class MongoDBCollectionTest extends TestCase { 

	@Test
	public void testMongoDBCollection() {
		try {
			MongoDBConnection con = null;

			try {
				con = (MongoDBConnection) DBConnectionFactory.getDBConnectionInstance("mongodb", "localhost", 27017, "", "");
			} 
			catch (Exception e) {}

			if (con!=null) {
				MongoDBDatabase db = new MongoDBDatabase(con, "testFifoQueue");

				if (db.collectionExists("MongoDBCollectionTest")) db.collectionDrop("MongoDBCollectionTest");

				MongoDBCollection coll = new MongoDBCollection(db, "MongoDBCollectionTest");
				coll.createIndex("key", true);

				assertNotNull(coll.add(MongoDBHelper.JSON2BasicDBObject("{'key':'1', 'name':'1'}")));
				assertNull(coll.add(MongoDBHelper.JSON2BasicDBObject("{'key':'1', 'name':'1'}")));
				assertEquals(1, coll.remove(MongoDBHelper.JSON2BasicDBObject("{'key':'1', 'name':'1'}")));			
				assertNotNull(coll.add(MongoDBHelper.JSON2BasicDBObject("{'key':'1', 'name':'1'}")));
				assertEquals(1, coll.size());

				assertTrue(coll.contains(MongoDBHelper.JSON2BasicDBObject("{'key':'1', 'name':'1'}")));
				assertEquals(1 ,coll.update(MongoDBHelper.JSON2BasicDBObject("{'key':'1', 'name':'1'}"), MongoDBHelper.JSON2BasicDBObject("{'key':'1', 'name':'2'}")));
				assertFalse(coll.contains(MongoDBHelper.JSON2BasicDBObject("{'key':'1', 'name':'1'}")));
				assertTrue(coll.contains(MongoDBHelper.JSON2BasicDBObject("{'key':'1', 'name':'2'}")));
				assertEquals(1, coll.size());
				assertNull(coll.add(MongoDBHelper.JSON2BasicDBObject("{'key':'1', 'name':'2'}")));
				assertEquals(1, coll.size());
				
				coll.removeAll();

				assertNotNull(coll.add(MongoDBHelper.JSON2BasicDBObject("{'key':'1', 'name':'1'}")));
				assertNotNull(coll.add(MongoDBHelper.JSON2BasicDBObject("{'key':'2', 'name':'2'}")));
				assertNotNull(coll.add(MongoDBHelper.JSON2BasicDBObject("{'key':'3', 'name':'3'}")));

				ArrayList<String> l = coll.getValues(null, "name");
				
				assertEquals(3, l.size());
				
				coll.removeAll();

				BasicDBObject doc = new BasicDBObject();
				doc.append("key", "1");
				doc.append("name", "1");
				doc.append("created", new Date());
				
				assertNotNull(coll.add(doc));
				
//				HashMap<String,String> m = coll.getItemMap(MongoDBHelper.JSON2BasicDBObject("{'key':'1'}"));
//
//				assertNotNull(m);

				con.close();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	@Test
	public void testMongoDBCollection2() {
		try {
			MongoDBConnection con = null;

			try {
				con = (MongoDBConnection) DBConnectionFactory.getDBConnectionInstance("mongodb", "localhost", 27017, "", "");
			} 
			catch (Exception e) {}

			if (con!=null) {
				MongoDBDatabase db = new MongoDBDatabase(con, "testFifoQueue");

				if (db.collectionExists("MongoDBCollectionTest")) db.collectionDrop("MongoDBCollectionTest");

				MongoDBCollection coll = new MongoDBCollection(db, "MongoDBCollectionTest");
				coll.createIndex("key", true);

				BasicDBObject doc = new BasicDBObject();
				doc.append("key", 1);
				doc.append("name", "name");
				doc.append("date", new Date());
				
				//Map<String,Object> m = doc.toMap();
				//Date d = (Date) m.get("date");
				
				assertNotNull(coll.add(doc));
				assertEquals(1, coll.size());

				con.close();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
