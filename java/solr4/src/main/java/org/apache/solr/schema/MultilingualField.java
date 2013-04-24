/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.solr.schema;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.lucene.analysis.multilingual.MultilingualAnalyzer;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.util.Version;
import org.apache.solr.common.*;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.core.SolrResourceLoader;

/** <code>TextField</code> is the basic type for configurable text analysis.
 * Analyzers for field types using this implementation should be defined in the schema.
 *
 */
public class MultilingualField extends TextField implements ResourceLoaderAware {  //, SolrCoreAware

	private IndexSchema schema;
	private String analyzerConfigPath;
	private boolean doStem = false;
	private boolean trace = false;
	private String configDir = null;

	@Override
	protected void init(final IndexSchema schema, final Map<String,String> args) {
		this.schema = schema;

		String traceValue = args.get("trace");
		trace = ("1".equals(traceValue) || "true".equals(traceValue) || "yes".equals(traceValue));

		TRACE("MultilingualField - init");

		analyzerConfigPath = args.get("analyzerConfig");
		args.remove("analyzerConfig");
		TRACE("MultilingualField - init - analyzerConfigPath : " + analyzerConfigPath);

		String doStemValue = args.get("doStem");
		doStem = ("1".equals(doStemValue) || "true".equals(doStemValue) || "yes".equals(doStemValue));
		TRACE("MultilingualField - init - doStem : " + doStemValue);

		args.remove("analyzerConfig");
		args.remove("doStem");
		args.remove("trace");

		if (analyzerConfigPath == null) {
			final SolrException e = this.createException("Missing analyzerConfig parameter");
			throw e;
		}
		super.init(schema, args);
	}

	//	public void inform(SolrCore solrCore) {
	//		// TODO Auto-generated method stub
	//		String s = solrCore.getIndexDir();
	//		System.out.println(s);
	//	}

	public void inform(org.apache.lucene.analysis.util.ResourceLoader loader) {
		TRACE("MultilingualField - inform");

		String [] configXml = loadAnalyzerConfig(loader);
		configDir = getConfigDir(loader);

		TRACE("MultilingualField - inform - configDir : " + configDir);

		//		Properties coreProperties = ((SolrResourceLoader) loader).getCoreProperties();
		//		for(String key : coreProperties.stringPropertyNames()) {
		//			String value = coreProperties.getProperty(key);
		//			System.out.println(key + " => " + value);
		//		}

		TRACE("MultilingualField - inform - analyzer : " + this.analyzer.getClass().getSimpleName());

		// update the analyzer references
		this.analyzer =  new MultilingualAnalyzer(Version.LUCENE_40, configXml, doStem, configDir, trace);		
		this.queryAnalyzer = new MultilingualAnalyzer(Version.LUCENE_40, configXml, doStem, configDir, trace);

		//		((MultilingualAnalyzer)this.analyzer).init(configXml, doStem, configDir, trace);
		//		TRACE("MultilingualField - inform 1");
		//		((MultilingualAnalyzer)this.queryAnalyzer).init(configXml, doStem, configDir, trace);
		//		TRACE("MultilingualField - inform 2");

		// tell the {@link IndexSchema} to refresh its analyzers
		schema.refreshAnalyzers();

	}

	private String getConfigDir(org.apache.lucene.analysis.util.ResourceLoader loader) {
	      File f = new File(analyzerConfigPath);
	      if (!f.isAbsolute()) {
	        // try $CWD/$configDir/$resource
	        f = new File(((SolrResourceLoader)loader).getConfigDir() + analyzerConfigPath);
	      }
	      if (f.isFile() && f.canRead()) {
	    	  String path = f.getAbsolutePath();
	    	  if (path.lastIndexOf(f.separatorChar)!=-1) {
	    		  return path.substring(0, path.lastIndexOf(f.separatorChar));
	    	  }
	    	  return null;
	      }
	      return null;
	}

		
	private String [] loadAnalyzerConfig(org.apache.lucene.analysis.util.ResourceLoader loader) {
		//log.info("Loading top-level analyzer configuration file at " + analyzerConfigPath);

		try {
			InputStream is = (FileInputStream) loader.openResource(analyzerConfigPath);
			java.io.DataInputStream din = new java.io.DataInputStream(is);
			StringBuffer sb = new StringBuffer();
			try{
				String line = null;
				while((line=din.readLine()) != null){
					sb.append(line+"\n");
				}
			}catch(Exception ex){
				ex.getMessage();
			}finally{
				try{
					is.close();
				}catch(Exception ex){}
			}
			return sb.toString().split("\n");
		} catch (final IOException e) {
			//log.error("Error loading top-level analyzer configuration file at " + analyzerConfigPath, e);
			throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, e);
		}
	}

	private SolrException createException(final String message) {
		final SolrException e = new SolrException
				(ErrorCode.SERVER_ERROR, "FieldType: " + this.getClass().getSimpleName() +
						" (" + typeName + ") " + message);
		return e;
	}

	private void TRACE(String msg) {
		if (trace) System.out.println(msg);
	}






}
