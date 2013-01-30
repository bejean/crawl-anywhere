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
public class MongoDBConnection {

	private Mongo m;
	private String hostName = null;
	private int hostPort = 0;

	/**
	 * @param hostName         The MongoDB server host name
	 * @param hostPort         The MongoDB server host port
	 * @return
	 * @throws UnknownHostException 
	 */
	public MongoDBConnection(String hostName, int hostPort) throws UnknownHostException {

		// http://api.mongodb.org/java/2.9.1/com/mongodb/MongoOptions.html#autoConnectRetry
		MongoOptions options = new MongoOptions();
		options.autoConnectRetry = true;
		options.safe = true;
		options.socketKeepAlive = true;

		if ("".equals(hostName)) hostName = "localhost";
		if (hostPort>0) {
			ServerAddress addr = new ServerAddress(hostName, hostPort);
			m = new Mongo(addr, options);
		} else {
			m = new Mongo(hostName, options);
		}
		this.hostName = hostName;
		this.hostPort = hostPort;
	}

	public Mongo getMongo() {
		return m;
	}

	public String getHostName() {
		return hostName;
	}

	public int getHostPort() {
		return hostPort;
	}
}

