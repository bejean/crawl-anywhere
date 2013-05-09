package org.apache.lucene.analysis.generic;

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

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;

/**
 * Normalizes token text to lower case.
 *
 * @version $Id: GenericFilter.java 472959 2006-11-09 16:21:50Z yonik $
 */
public final class GenericFilter extends TokenFilter {

	private TermAttribute termAtt;

	public GenericFilter(TokenStream in) {
		super(in);
		termAtt = (TermAttribute) addAttribute(TermAttribute.class);
	}

	/* pre 3.0.0
	public final Token next() throws IOException {
		Token t = input.next();

		if (t == null)
			return null;

		// convertir les diacrité é à ç ... en leurs équivalents sans accents
		t.setTermText(GenericNormalizer.normalize(t.termText()).toLowerCase());

		return t;
	}
	 */

	public boolean incrementToken() throws IOException
	{
		if (input.incrementToken()) {
			final char[] buffer = termAtt.termBuffer();
			final int length = termAtt.termLength();
			for(int i=0;i<length;i++)
				buffer[i] = GenericNormalizer.normalize(buffer[i]);
			return true;
		} else
			return false;
	}

}
