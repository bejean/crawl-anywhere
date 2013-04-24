package org.apache.lucene.analysis.multilingual;

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
import org.apache.lucene.analysis.ar.ArabicStemFilter;
import org.apache.lucene.analysis.ru.RussianStemFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;

public class MultilingualStemmedAnalyzer extends MultilingualAbstractAnalyzer {

	public TokenStream tokenStream(String fieldName, Reader reader) {
		return tokenStreamDo(fieldName, reader, "index");
	}

	protected TokenStream processStemmingFilter(TokenStream input, String langCode)
	{
		//if ("br".equals(langCode))
		//	return BrazilianStemFilter(input);
		if ("ar".equals(langCode))
			return new ArabicStemFilter(input);

		if ("ru".equals(langCode))
			return new RussianStemFilter(input);
			//return new SnowballFilter(input, "Russian");

		if ("da".equals(langCode))
			return new SnowballFilter(input, "Danish");

		if ("nl".equals(langCode))
			return new SnowballFilter(input, "Dutch");

		if ("fi".equals(langCode))
			return new SnowballFilter(input, "Finnish");

		if ("de".equals(langCode))
			return new SnowballFilter(input, "German");

		if ("hu".equals(langCode))
			return new SnowballFilter(input, "Hungarian");

		if ("en".equals(langCode))
			return new SnowballFilter(input, "English");
		
		if ("fr".equals(langCode))
			return new SnowballFilter(input, "French");

		if ("it".equals(langCode))
			return new SnowballFilter(input, "Italian");

		if ("no".equals(langCode))
			return new SnowballFilter(input, "Norwegian");

		if ("ro".equals(langCode))
			return new SnowballFilter(input, "Romanian");

		if ("es".equals(langCode))
			return new SnowballFilter(input, "Spanish");

		if ("pt".equals(langCode))
			return new SnowballFilter(input, "Portuguese");

		if ("sv".equals(langCode))
			return new SnowballFilter(input, "Swedish");

		if ("tr".equals(langCode))
			return new SnowballFilter(input, "Turkish");

		return input;
	}	
	
	protected TokenStream processLogFilter(TokenStream input)
	{
		return input;
	}		
}
