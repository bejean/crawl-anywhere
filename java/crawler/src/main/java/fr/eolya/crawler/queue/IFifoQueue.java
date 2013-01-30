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

public interface IFifoQueue {	
	public void reset();
	public long size();
	public boolean push(String json) throws QueueIncoherenceException, QueueInvalidDataException;
	public String pop();
	public String pop(String extraSortField);
	public boolean contains(String keyValue);
	public String contains(String keyValue, String returnedField);
}
