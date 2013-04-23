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

import fr.eolya.crawler.connectors.web.SourceItemWeb;
import fr.eolya.crawler.queue.ISourceItemsQueue;
import fr.eolya.crawler.queue.QueueFactory;
import fr.eolya.utils.nosql.DBConnectionFactory;
import fr.eolya.utils.nosql.IDBConnection;

public class SourceItemsQueueTest extends TestCase { 

	private static SourceItemWeb getItemWeb(int sourceId, String url, int depth) {
		return new	SourceItemWeb(sourceId, url, depth, url, url, "", "", null, null, "", "", "", "", "", "", "", "s", false);
	}
	
	
	@Test
	public void testMongoDBWebSiteUrlFifoQueue() {
		try {
			IDBConnection dbConnection = DBConnectionFactory.getDBConnectionInstance("mongodb", "localhost", 27017,  "", "");
			ISourceItemsQueue queue = QueueFactory.getSourceItemsQueueInstance("mongodb", 1, dbConnection, "testFifoQueue", "TestMongoDBWebSiteUrlFifoQueue");
			
			if (queue!=null) {
				queue.reset();
				queue.start();
				assertEquals(0, queue.size());
				
				assertTrue(queue.push(getItemWeb(1, "http://a.a.a/", 0).getMap()));
				assertTrue(queue.push(getItemWeb(1, "http://a.a.a/2.html", 1).getMap()));
				assertTrue(queue.push(getItemWeb(1, "http://a.a.a/3.html", 2).getMap()));

				assertTrue(queue.contains("http://a.a.a/"));
				assertEquals(3, queue.size());

				assertEquals(0, queue.getDoneQueueSize());
				assertFalse(queue.isDone("http://a.a.a/"));
				
				assertEquals("http://a.a.a/", queue.pop().get("url"));
				assertEquals("http://a.a.a/2.html", queue.pop().get("url"));
				assertEquals("http://a.a.a/3.html", queue.pop().get("url"));

				assertEquals(3, queue.getDoneQueueSize());
				assertTrue(queue.isDone("http://a.a.a/"));

				assertFalse(queue.contains("http://a.a.a/"));
				assertEquals(0, queue.size());

				assertFalse(queue.push(getItemWeb(1, "http://a.a.a/", 0).getMap()));
				assertTrue(queue.push(getItemWeb(1, "http://a.a.a/4.html", 2).getMap()));
				assertTrue(queue.push(getItemWeb(1, "http://a.a.a/3.html", 1).getMap()));
				assertFalse(queue.push(getItemWeb(1, "http://a.a.a/2.html", 2).getMap()));
				
				assertEquals(2, queue.size());

				assertEquals("http://a.a.a/3.html", queue.pop("depth").get("url"));
				assertEquals("http://a.a.a/4.html", queue.pop("depth").get("url"));

				assertEquals(0, queue.size());		
			}
			
			queue = QueueFactory.getSourceItemsQueueInstance("mongodb", 1, dbConnection, "testFifoQueue", "TestMongoDBWebSiteUrlFifoQueue");	
			if (queue!=null) {
				queue.start();
				assertEquals(0, queue.size());
				assertFalse(queue.push(getItemWeb(1, "http://a.a.a/", 0).getMap()));
				queue.stop();
			}

			queue = QueueFactory.getSourceItemsQueueInstance("mongodb", 1, dbConnection, "testFifoQueue", "TestMongoDBWebSiteUrlFifoQueue");	
			if (queue!=null) {
				queue.start();
				assertEquals(0, queue.size());
				assertTrue(queue.push(getItemWeb(1, "http://a.a.a/", 0).getMap()));
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void testMongoDBWebSiteUrlFifoQueue2() {
		try {
			IDBConnection dbConnection = DBConnectionFactory.getDBConnectionInstance("mongodb", "localhost", 27017,  "", "");
			ISourceItemsQueue queue = QueueFactory.getSourceItemsQueueInstance("mongodb", 1, dbConnection, "testFifoQueue", "TestMongoDBWebSiteUrlFifoQueue");
			
			if (queue!=null) {
				queue.reset();
				queue.start();
				assertEquals(0, queue.size());
				assertTrue(queue.push(getItemWeb(1, "http://a.a.a/", 0).getMap()));
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}

	}

}
