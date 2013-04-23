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

import fr.eolya.utils.nosql.IDBConnection;

/**
 * A MongoDB class representing a DB connection.
 */
public class MongoDBConnection implements IDBConnection {

	private MongoClient m = null;
	private String hostName = null;
	private int hostPort = 0;

	/**
	 * @param hostName         The MongoDB server host name
	 * @param hostPort         The MongoDB server host port
	 * @return
	 * @throws UnknownHostException 
	 */
	public MongoDBConnection(String hostName, int hostPort, String userName, String userPassword) throws UnknownHostException {
		
		MongoClientOptions.Builder builder = new MongoClientOptions.Builder();
		builder.autoConnectRetry(true);
		builder.socketKeepAlive(true);
		builder.writeConcern(WriteConcern.SAFE);

		if ("".equals(hostName)) hostName = "localhost";
		if (hostPort>0) {
			ServerAddress addr = new ServerAddress(hostName, hostPort);
			m = new MongoClient(addr, builder.build());
		} else {
			m = new MongoClient(hostName, builder.build());
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
	
	public void close() {
		if (m==null) return;
		m.close();
		m = null;
	}
	
	public String getType() {
		return "mongodb";
	}

}

