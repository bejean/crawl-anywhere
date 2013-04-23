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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;

public class MongoDBHelper {
	//	public static DBObject JSON2DBObject (String json) {
	//		return (DBObject) JSON.parse(json);
	//	}

	public static BasicDBObject JSON2BasicDBObject (String json) {
		return (BasicDBObject) JSON.parse(json);
	}	

	public static Map<String,Object> BasicDBObject2Map(BasicDBObject doc) {
		Map<String,Object> ret = new HashMap<String,Object>();
		Iterator<Entry<String, Object>> it = doc.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Object> entry = (Map.Entry<String, Object>) it.next();	
			Object o = doc.get(entry.getKey());
			ret.put(entry.getKey(), o);
			if (o!=null) {
				if (o instanceof Double) {
					try {
						Double d = (Double)o;
						ret.put(entry.getKey(), new Integer(d.intValue()));
					} catch (Exception e) {}
				}
			}
		}		
		return ret;
	}
}
