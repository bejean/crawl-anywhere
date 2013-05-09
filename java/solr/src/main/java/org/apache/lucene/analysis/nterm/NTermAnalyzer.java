package org.apache.lucene.analysis.nterm;

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

import java.io.Reader;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.charfilter.MappingCharFilter;
import org.apache.lucene.analysis.charfilter.NormalizeCharMap;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.LowerCaseTokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
import org.apache.lucene.util.Version;

public final class NTermAnalyzer extends StopwordAnalyzerBase {
  
	private final String ntermStopFilterRules;

  /** Builds an analyzer with the stop words from the given set.
   * @param matchVersion See <a href="#version">above</a>
   * @param stopWords Set of stop words */
  public NTermAnalyzer(Version matchVersion, String ntermStopFilterRules) {
    super(matchVersion);
    this.ntermStopFilterRules = ntermStopFilterRules;
  }

  /**
   * Creates
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
   * used to tokenize all the text in the provided {@link Reader}.
   * 
   * @return {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
   *         built from a {@link LowerCaseTokenizer} filtered with
   *         {@link StopFilter}
   */
  @Override
//  protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
//    final Tokenizer source = new StandardTokenizer(matchVersion, reader);
//	//final Tokenizer source = new WhitespaceTokenizer(matchVersion, reader);
//    TokenStream tokenStream = new LowerCaseFilter(matchVersion, source);
//    tokenStream = new ShingleFilter(tokenStream, 2, 3);
//    tokenStream = new NTermStopFilter(matchVersion, tokenStream, ntermStopFilterRules);    
//    return new TokenStreamComponents(source, tokenStream);
//  }
  
  protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
	  
	  	NormalizeCharMap.Builder builder = new NormalizeCharMap.Builder();
	  	builder.add("- ", " | ");
	  	builder.add(" -", " | ");
	  	builder.add(".", " | ");
	  	builder.add(",", " | ");
	  	builder.add(":", " | ");
	  	builder.add(";", " | ");
	  	builder.add("...", " | ");
	  	builder.add("/", " | ");
	  	builder.add("@", " | ");
	  	builder.add("(", " | ");
	  	builder.add(")", " | ");
	  	builder.add("[", " | ");
	  	builder.add("]", " | ");
	  	builder.add("\u00AB", " | ");
	  	builder.add("\u00BB", " | ");

	  	NormalizeCharMap normMap = builder.build();

	  	Reader reader2 = (normMap == null ? reader : new MappingCharFilter(normMap,reader));
	  		  
		final Tokenizer source = new WhitespaceTokenizer(matchVersion, reader2);
	    TokenStream tokenStream = new LowerCaseFilter(matchVersion, source);
	    tokenStream = new ShingleFilter(tokenStream, 2, 3);
	    tokenStream = new NTermStopFilter(matchVersion, tokenStream, ntermStopFilterRules);    
	    return new TokenStreamComponents(source, tokenStream);
	  }
  
}

