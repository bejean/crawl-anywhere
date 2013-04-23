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
package fr.eolya.crawler.queue;

import java.util.Map;

public interface ISourceItemsQueue {	
	public void reset();
	public long size();
	public boolean push(Map<String,Object> item) throws QueueIncoherenceException, QueueInvalidDataException;
	public Map<String,Object> pop();
	public Map<String,Object> pop(String extraSortField);
	public boolean contains(String keyValue);
	
	public long getQueueSize();
	
	public long getDoneQueueSize();
	public Map<String,Object> getDone(String url);
	public boolean isDone(String url);
	public boolean updateDone(Map<String,Object> item);
	
	public Long start();
	public Long reStart();
	public Long stop();
	public void close();
	
	public String getCreated(Map<String,Object> item);
}
