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

import junit.framework.TestCase;
import org.junit.Test;

import fr.eolya.utils.json.JSONHelper;
import fr.eolya.utils.nosql.mongodb.*;

public class MongoDBWebSiteUrlFifoQueueTest extends TestCase { 

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
				MongoDBWebSiteUrlFifoQueue queue = new MongoDBWebSiteUrlFifoQueue(db, "TestMongoDBWebSiteUrlFifoQueue"); 

				queue.reset();
				assertEquals(0, queue.size());

				assertTrue(queue.push("{'url':'http://a.a.a/', 'depth':0}"));
				assertTrue(queue.push("{'url':'http://a.a.a/2.html', 'depth':1}"));
				assertTrue(queue.push("{'url':'http://a.a.a/3.html', 'depth':2}"));

				assertTrue(queue.contains("http://a.a.a/"));
				assertEquals(3, queue.size());

				assertEquals("http://a.a.a/", JSONHelper.getJSONFieldText(queue.pop(), "url"));
				assertEquals("http://a.a.a/2.html", JSONHelper.getJSONFieldText(queue.pop(), "url"));
				assertEquals("http://a.a.a/3.html", JSONHelper.getJSONFieldText(queue.pop(), "url"));

				assertFalse(queue.contains("http://a.a.a/"));
				assertEquals(0, queue.size());

				assertFalse(queue.push("{'url':'http://a.a.a/', 'depth':0}")); 		
				assertTrue(queue.push("{'url':'http://a.a.a/4.html', 'depth':2}"));	
				assertTrue(queue.push("{'url':'http://a.a.a/3.html', 'depth':1}")); 
				assertFalse(queue.push("{'url':'http://a.a.a/2.html', 'depth':2}")); 

				assertEquals(2, queue.size());

				assertEquals("http://a.a.a/3.html", JSONHelper.getJSONFieldText(queue.pop("depth"), "url"));
				assertEquals("http://a.a.a/4.html", JSONHelper.getJSONFieldText(queue.pop("depth"), "url"));

				assertEquals(0, queue.size());		
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
