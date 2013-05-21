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

package org.apache.solr.analysis.nterm;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.nterm.NTermStopFilter;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class NTermStopFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {

	public NTermStopFilterFactory(Map<String, String> args) {
		super(args);
		ntermStopFilterRulesFile = args.get("ntermStopFilterRules");
	}

	private String ntermStopFilterRulesFile;
	private String configDir;

//	@Override
//	public void init(Map<String,String> args) {
//		//super.init(args);
//		assureMatchVersion();
//	}
	
	public void inform(ResourceLoader loader) throws IOException {
		configDir = ((org.apache.solr.core.SolrResourceLoader) loader).getConfigDir();
		//ntermStopFilterRulesFile = args.get("ntermStopFilterRules");
		//System.out.println("NTermStopFilterFactory - inform - " + configDir + " - " + ntermStopFilterRulesFile);
		if (ntermStopFilterRulesFile == null) throw new RuntimeException(); 
	}

	public TokenStream create(TokenStream input) {
		String rules = new File(configDir, ntermStopFilterRulesFile).toString();
		//System.out.println("NTermStopFilterFactory - create - " + rules);
		NTermStopFilter stopFilter = new NTermStopFilter(luceneMatchVersion,input,rules);
		return stopFilter;
	}


}
