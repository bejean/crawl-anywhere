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

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ar.ArabicLetterTokenizer;
import org.apache.lucene.analysis.ar.ArabicNormalizationFilter;
import org.apache.lucene.analysis.cjk.CJKTokenizer;
import org.apache.lucene.analysis.fa.PersianNormalizationFilter;
import org.apache.lucene.analysis.ru.RussianLetterTokenizer;
import org.apache.lucene.analysis.ru.RussianLowerCaseFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.th.ThaiWordFilter;
import org.apache.lucene.util.Version;

import org.apache.lucene.analysis.generic.GenericTokenizer;

public abstract class MultilingualAbstractAnalyzer extends Analyzer {

	static final String defaultLang ="en";

	public TokenStream tokenStreamDo(String fieldName, Reader reader, String mode) {
		
		String langCode;
		
		MultilingualUtils.trace ("----- " + fieldName);			

		MultilingualReader mr = new MultilingualReader();
		reader = mr.analyze(reader);
		
		if (!"".equals(mr.langCode))
			langCode = mr.langCode;
		else
			langCode = defaultLang;

		TokenStream result = null;

		// Arabic
		// To test : http://www.nongnu.org/aramorph/french/index.html
		if ("ar".equals(langCode))
		{
			result = new ArabicLetterTokenizer(reader);
			//result = new StopFilter( tokenizer, stoptable );
			result = new LowerCaseFilter(result);
			result = new ArabicNormalizationFilter( result );
			result = processStemmingFilter(result, langCode);
			return result;			
		}

		// Arabic - Persian (Farsi)
		if ("fa".equals(langCode))
		{
			result = new ArabicLetterTokenizer(reader);
			//result = new LowerCaseFilter(result);
			result = new ArabicNormalizationFilter( result );
			/* additional persian-specific normalization */
			result = new PersianNormalizationFilter(result);
			/*
			 * the order here is important: the stopword list is normalized with the above!
			 */			
			//result = new StopFilter( tokenizer, stoptable );
			// Why no stemming ?
			//result = new ArabicStemFilter( result );
			return result;			
		}

		// Chinese
		// To be read : http://alias-i.com/lingpipe/demos/tutorial/chineseTokens/read-me.html
		// To be tested : http://code.google.com/p/imdict-chinese-analyzer/
		// It seems ChineseTokenizer vs CJKTokenizer, the winner is CJKTokenizer
		if ("zh".equals(langCode))
		{
			//result = new ChineseTokenizer(reader);
			result = new CJKTokenizer(reader);
			return result;			
		}

		// CJK - Japonese / Korean
		if ("zh".equals(langCode) || "ja".equals(langCode) || "ko".equals(langCode))
		{
			result = new CJKTokenizer(reader);
			return result;			
		}

		// Thai
		if ("th".equals(langCode))
		{
			result = new StandardTokenizer(Version.LUCENE_29,reader);
			result = new StandardFilter(result);
			result = new ThaiWordFilter(result);
			return result;
		}

		// Russian
		if ("ru".equals(langCode))
		{
			//result = new RussianLetterTokenizer(reader, charset);
			result = new RussianLetterTokenizer(reader);
			//result = new RussianLowerCaseFilter(result, charset);
			result = new RussianLowerCaseFilter(result);
			result = processStemmingFilter(result, langCode);
			//result = new StopFilter(result, stopSet);
			return result;
		}

		// Brasilian
		if ("br".equals(langCode))
		{
			result = new StandardTokenizer(Version.LUCENE_29,reader);
			result = new LowerCaseFilter( result );
			result = new StandardFilter( result );
			result = processStopFilter(result, "pt");
			result = processStemmingFilter(result, langCode);
			return result;
		}

		// Default : en / fr / pt 
		//result = new WhitespaceTokenizer(reader);
		result = new GenericTokenizer(reader);
		result = new LowerCaseFilter(result);
		result = processStopFilter(result, langCode);
		result = processStemmingFilter(result, langCode);
		result = processLogFilter(result);
		return result;
	}

	/*
	private TokenStream processLowerCaseFilter(TokenStream input, String langCode, boolean defaultProcessing)
	{
		// Default processing ?
		if (defaultProcessing)
			return new LowerCaseFilter(input);

		// Nothing to do
		return input;
	}
	 */

	protected TokenStream processStopFilter(TokenStream input, String langCode)
	{
		Set stop = readStopWords(langCode);
		if (stop!=null)
			return new StopFilter(true, input, stop);

		return input;
	}	

	protected abstract TokenStream processStemmingFilter(TokenStream input, String langCode);
	protected abstract TokenStream processLogFilter(TokenStream input);

	protected static final Set ENGLISH_STOP_WORDS_SET;
	static {
		final String[] stopWords = new String[]{
				"a", "an", "and", "are", "as", "at", "be", "but", "by",
				"for", "if", "in", "into", "is", "it",
				"no", "not", "of", "on", "or", "such",
				"that", "the", "their", "then", "there", "these",
				"they", "this", "to", "was", "will", "with"
		};
		final CharArraySet stopSet = new CharArraySet(stopWords.length, false);
		stopSet.addAll(Arrays.asList(stopWords));  
		ENGLISH_STOP_WORDS_SET = CharArraySet.unmodifiableSet(stopSet); 
	}	

	protected static final Set FRENCH_STOP_WORDS_SET;
	static {
		final String[] stopWords = new String[]{
				"a", "afin", "ai", "au", "aux", 
				"c", "car", "ce", "ceci", "cela", "celle", "celles", "celui", "ces",
				"cet", "cette", "ceux", "ci", "d", "dans", "de", "des", "donc", "du", "elle", "elles",
				"en", "et", "il", "ils", "j", "je", "l", "la", "le", "les", "n", "ne", "ni", 
				"nous", "on", "par", "pas", "qu", "que", "quel", "quelle", "quelles", "quels",
				"qui", "quoi", "s", "sa", "se", "ses", "si", "ta", "te", "tes", "tien", "tienne", "tiennes",
				"tiens", "toi", "ton", "un", "une", "y", "à"
		};

		final CharArraySet stopSet = new CharArraySet(stopWords.length, false);
		stopSet.addAll(Arrays.asList(stopWords));  
		FRENCH_STOP_WORDS_SET = CharArraySet.unmodifiableSet(stopSet); 
	}	


	protected static final Set PORTUGESE_STOP_WORDS_SET;
	static {
		final String[] stopWords = new String[]{
				"a","ainda","alem","ambas","ambos","antes",
				"ao","aonde","aos","apos","aquele","aqueles",
				"as","assim","com","como","contra","contudo",
				"cuja","cujas","cujo","cujos","da","das","de",
				"dela","dele","deles","demais","depois","desde",
				"desta","deste","dispoe","dispoem","diversa",
				"diversas","diversos","do","dos","durante","e",
				"ela","elas","ele","eles","em","entao","entre",
				"essa","essas","esse","esses","esta","estas",
				"este","estes","ha","isso","isto","logo","mais",
				"mas","mediante","menos","mesma","mesmas","mesmo",
				"mesmos","na","nas","nao","nas","nem","nesse","neste",
				"nos","o","os","ou","outra","outras","outro","outros",
				"pelas","pelas","pelo","pelos","perante","pois","por",
				"porque","portanto","proprio","propios","quais","qual",
				"qualquer","quando","quanto","que","quem","quer","se",
				"seja","sem","sendo","seu","seus","sob","sobre","sua",
				"suas","tal","tambem","teu","teus","toda","todas","todo",
				"todos","tua","tuas","tudo","um","uma","umas","uns"
		};
		final CharArraySet stopSet = new CharArraySet(stopWords.length, false);
		stopSet.addAll(Arrays.asList(stopWords));  
		PORTUGESE_STOP_WORDS_SET = CharArraySet.unmodifiableSet(stopSet); 
	}	

	// Lire un fichier texte se trouvant dans le fichier jar de l'application
	protected Set readStopWords(String langCode) {
		try {
			ArrayList<String> l = new ArrayList<String>();

			InputStream in = ClassLoader.getSystemResourceAsStream("stopwords_" + langCode + ".txt");
			if (in==null) 
			{		
				if ("fr".equals(langCode))
					return FRENCH_STOP_WORDS_SET;
				if ("en".equals(langCode))
					return ENGLISH_STOP_WORDS_SET;
				if ("pt".equals(langCode))
					return PORTUGESE_STOP_WORDS_SET;

				return null;
			}

			BufferedReader d = new BufferedReader(new InputStreamReader(in));
			String line;
			while ((line = d.readLine()) != null)
			{
				l.add(line);
			}
			return StopFilter.makeStopSet(l, true);

			/*
			DataInputStream dis = new DataInputStream(ClassLoader.getSystemResourceAsStream(filename));
			byte[] data = new byte[dis.available()];
			while (dis.)
			//dis.readFully(data);
			dis.close();
			return (new String(data));
			 */
		}
		catch (IOException ioe) {
			System.out.println(ioe);
			return null;
		}
	}    
}
