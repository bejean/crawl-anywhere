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

import junit.framework.TestCase;
import org.junit.Test;

public class MongoDBCollectionTest extends TestCase { 

	@Test
	public void testMongoDBWebSiteUrlFifoQueue() {
		try {
			MongoDBConnection con = null;

			try {
				con = new MongoDBConnection("localhost", 27017);
			} 
			catch (Exception e) {}

			if (con!=null) {
				MongoDBDatabase db = new MongoDBDatabase(con, "testFifoQueue");

				if (db.collectionExists("MongoDBCollectionTest")) db.collectionDrop("MongoDBCollectionTest");

				MongoDBCollection coll = new MongoDBCollection(db, "MongoDBCollectionTest");
				coll.createIndex("key", true);

				assertNotNull(coll.add(MongoDBHelper.JSON2BasicDBObject("{'key':'1', 'name':'1'}")));
				assertNull(coll.add(MongoDBHelper.JSON2BasicDBObject("{'key':'1', 'name':'1'}")));
				coll.remove(MongoDBHelper.JSON2BasicDBObject("{'key':'1', 'name':'1'}"));			
				assertNotNull(coll.add(MongoDBHelper.JSON2BasicDBObject("{'key':'1', 'name':'1'}")));
				assertEquals(1, coll.size());

				assertTrue(coll.contains(MongoDBHelper.JSON2BasicDBObject("{'key':'1', 'name':'1'}")));
				coll.update(MongoDBHelper.JSON2BasicDBObject("{'key':'1', 'name':'1'}"), MongoDBHelper.JSON2BasicDBObject("{'key':'1', 'name':'2'}"));
				assertFalse(coll.contains(MongoDBHelper.JSON2BasicDBObject("{'key':'1', 'name':'1'}")));
				assertTrue(coll.contains(MongoDBHelper.JSON2BasicDBObject("{'key':'1', 'name':'2'}")));
				assertEquals(1, coll.size());
				assertNull(coll.add(MongoDBHelper.JSON2BasicDBObject("{'key':'1', 'name':'2'}")));
				assertEquals(1, coll.size());
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
