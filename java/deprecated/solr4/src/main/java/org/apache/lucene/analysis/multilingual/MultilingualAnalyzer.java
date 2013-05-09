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

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.ar.ArabicNormalizationFilter;
import org.apache.lucene.analysis.ar.ArabicStemFilter;
import org.apache.lucene.analysis.bg.BulgarianStemFilter;
import org.apache.lucene.analysis.charfilter.MappingCharFilter;
import org.apache.lucene.analysis.charfilter.NormalizeCharMap;
import org.apache.lucene.analysis.cjk.CJKBigramFilter;
import org.apache.lucene.analysis.cjk.CJKWidthFilter;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.cz.CzechStemFilter;
import org.apache.lucene.analysis.de.GermanLightStemFilter;
import org.apache.lucene.analysis.de.GermanMinimalStemFilter;
import org.apache.lucene.analysis.de.GermanNormalizationFilter;
import org.apache.lucene.analysis.el.GreekLowerCaseFilter;
import org.apache.lucene.analysis.el.GreekStemFilter;
import org.apache.lucene.analysis.en.EnglishMinimalStemFilter;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.es.SpanishLightStemFilter;
import org.apache.lucene.analysis.fa.PersianCharFilter;
import org.apache.lucene.analysis.fa.PersianNormalizationFilter;
import org.apache.lucene.analysis.fi.FinnishLightStemFilter;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.fr.FrenchLightStemFilter;
import org.apache.lucene.analysis.fr.FrenchMinimalStemFilter;
import org.apache.lucene.analysis.ga.IrishLowerCaseFilter;
import org.apache.lucene.analysis.gl.GalicianMinimalStemFilter;
import org.apache.lucene.analysis.gl.GalicianStemFilter;
import org.apache.lucene.analysis.hi.HindiNormalizationFilter;
import org.apache.lucene.analysis.hi.HindiStemFilter;
import org.apache.lucene.analysis.hu.HungarianLightStemFilter;
import org.apache.lucene.analysis.id.IndonesianStemFilter;
import org.apache.lucene.analysis.in.IndicNormalizationFilter;
import org.apache.lucene.analysis.it.ItalianLightStemFilter;
import org.apache.lucene.analysis.ja.JapaneseBaseFormFilter;
import org.apache.lucene.analysis.ja.JapaneseKatakanaStemFilter;
import org.apache.lucene.analysis.ja.JapanesePartOfSpeechStopFilter;
import org.apache.lucene.analysis.ja.JapaneseTokenizer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer.Mode;
import org.apache.lucene.analysis.ja.dict.UserDictionary;
import org.apache.lucene.analysis.lv.LatvianStemFilter;
import org.apache.lucene.analysis.miscellaneous.KeywordMarkerFilter;
import org.apache.lucene.analysis.miscellaneous.StemmerOverrideFilter;
import org.apache.lucene.analysis.no.NorwegianLightStemFilter;
import org.apache.lucene.analysis.no.NorwegianMinimalStemFilter;
import org.apache.lucene.analysis.pt.PortugueseMinimalStemFilter;
import org.apache.lucene.analysis.pt.PortugueseStemFilter;
import org.apache.lucene.analysis.pt.PortugueseLightStemFilter;
import org.apache.lucene.analysis.ru.RussianLightStemFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.*;
import org.apache.lucene.analysis.sv.SwedishLightStemFilter;
import org.apache.lucene.analysis.synonym.SynonymFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.analysis.th.ThaiWordFilter;
import org.apache.lucene.analysis.util.CharArrayMap;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.ElisionFilter;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.Version;
import org.apache.solr.common.SolrException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;


public class MultilingualAnalyzer extends Analyzer {

	/** Default maximum allowed token length */
	public static final int DEFAULT_MAX_TOKEN_LENGTH = 255;
	private static final String defaultLang ="en";

	private final Version matchVersion;
	private MultilingualConfig multilingualConfig;

	private int maxTokenLength = DEFAULT_MAX_TOKEN_LENGTH;

	private boolean doStem = false;
	private boolean trace = false;
	private String configDir = null;
	
	public MultilingualAnalyzer(Version matchVersion, String[] configXml, boolean doStem, String configDir) {
		super(new NoReuseStrategy());
		this.matchVersion= matchVersion;
		init(configXml, doStem, configDir, false);
	}

	public MultilingualAnalyzer(Version matchVersion, String[] configXml, boolean doStem, String configDir, boolean trace) {
		super(new NoReuseStrategy());
		this.matchVersion= matchVersion;
		init(configXml, doStem, configDir, trace);
	}

	public MultilingualAnalyzer(Version matchVersion, String configFile, boolean doStem, String configDir) {
		super(new NoReuseStrategy());
		this.matchVersion= matchVersion;
		init(configFile, doStem, configDir, false);
	}

	public MultilingualAnalyzer(Version matchVersion, String configFile, boolean doStem, String configDir, boolean trace) {
		super(new NoReuseStrategy());
		this.matchVersion= matchVersion;
		init(configFile, doStem, configDir, trace);
	}

	public MultilingualAnalyzer(Version matchVersion) {
		super(new NoReuseStrategy());
		this.matchVersion= matchVersion;
		System.out.println("MultilingualAnalizer - default constructor");
	}

	public void init(String[] configXml, boolean doStem, String configDir, boolean trace) {
		initInternal(null, configXml, doStem, configDir, trace);		
	}

	public void init(String configFile, boolean doStem, String configDir, boolean trace) {
		initInternal(configFile, null, doStem, configDir, trace);
	}

	private void initInternal(String configFile, String[] configXml, boolean doStem, String configDir, boolean trace) {
		this.doStem = doStem;
		this.trace = trace;

		if (configFile!=null && !"".equals(configFile)) {	
			multilingualConfig = loadConfig(configFile);
		}
		if (configXml!=null) {	
			multilingualConfig = loadConfig(configXml);
		}

		String home = multilingualConfig.getConfigHome();
		if (home!=null && !"".equals(home)) 
			this.configDir= home;
		else {
			if (configDir!=null && !"".equals(configDir)) 
				this.configDir = configDir;
			else {
				if (configFile!=null && !"".equals(configFile)) {
					File f = new File(configFile);
					if (f.exists() && f.isAbsolute())
						this.configDir = f.getParent();
				}
			}
		}
		TRACE("MultilingualAnalizer : configDir       = " + this.configDir);
	}

	public void setTrace(boolean value) {
		this.trace = value;
	}

	private MultilingualConfig loadConfig(String configFile) {
		if (configFile!=null && !"".equals(configFile)) {
			MultilingualConfig config = new MultilingualConfig();
			try {
				config.loadFile(configFile);
				return config;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private MultilingualConfig loadConfig(String[] configXml) {
		if (configXml!=null && configXml.length>0) {
			MultilingualConfig config = new MultilingualConfig();
			try {
				config.loadString(configXml);
				return config;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * Set maximum allowed token length.  If a token is seen
	 * that exceeds this length then it is discarded.  This
	 * setting only takes effect the next time tokenStream or
	 * tokenStream is called.
	 */
	public void setMaxTokenLength(int length) {
		maxTokenLength = length;
	}

	/**
	 * @see #setMaxTokenLength
	 */
	public int getMaxTokenLength() {
		return maxTokenLength;
	}

	@Override
	protected TokenStreamComponents createComponents(final String fieldName, final Reader reader) {

		String langCode = defaultLang;
		Properties prop = null;
		String mode = "index"; 
		final Tokenizer src;

		MultilingualReaderFactory mr = new MultilingualReaderFactory();
		Reader multiligualReader = mr.analyze(reader);
		if (!"".equals(mr.langCode)) langCode = mr.langCode;
		if (!"".equals(mr.mode)) mode = mr.mode;

		TRACE("");

		TRACE("MultilingualAnalizer : field name    = " + fieldName);
		TRACE("MultilingualAnalizer : value header  = " + mr.valueHeader);
		TRACE("MultilingualAnalizer : mode          = " + mr.mode);
		TRACE("MultilingualAnalizer : text language = " + mr.langCode);
		
		if (configDir!=null) 
			TRACE("MultilingualAnalizer : configDir     = " + configDir);

		TRACE("Xml = " + multilingualConfig.getXml(langCode, mode));

		// charFilter
		prop = multilingualConfig.getCharFilterProperties(langCode, mode);
		if (prop!=null && prop.getProperty("class")!=null && !"".equals(prop.getProperty("class"))) {
			Reader tmpReader = charFilterFactory(prop.getProperty("class"), prop, configDir, matchVersion, multiligualReader); 
			if (tmpReader!=null) multiligualReader = tmpReader;
		}

		// tokenizer
		prop = multilingualConfig.getTokenizerProperties(langCode, mode);
		if (prop!=null && prop.getProperty("class")!=null && !"".equals(prop.getProperty("class"))) {
			src = tokenizerFactory(prop.getProperty("class"), prop, configDir, matchVersion, multiligualReader);
		}
		else return null;

		// filters
		int filterCount = multilingualConfig.getFilterCount(langCode, mode);
		TokenStream tok = null;
		if (filterCount>0) {
			for (int i=0; i<filterCount; i++) {
				prop = multilingualConfig.getFilterProperties(i, langCode, mode);
				if (prop!=null && prop.getProperty("class")!=null && !"".equals(prop.getProperty("class"))) {
					if (i==0) {
						TokenStream tmptok = filterFactory(prop.getProperty("class"), prop, configDir, matchVersion, src);
						if (tmptok!=null) 
							tok = tmptok;
						else
							tok = src;
					}
					else {
						TokenStream tmptok = filterFactory(prop.getProperty("class"), prop, configDir, matchVersion, tok);	
						if (tmptok!=null) tok = tmptok;
					}
				}
			}
		}

		return new TokenStreamComponents(src, tok);
		//		return new TokenStreamComponents(src, tok) {
		//			@Override
		//			protected void reset(final Reader reader) throws IOException {
		//				//src.setMaxTokenLength(MultilingualAnalyzer.this.maxTokenLength);
		//				super.reset(reader);
		//			}
		//		};
	}


	private CharFilter charFilterFactory(String className, Properties prop, String configHome, Version matchVersion, Reader reader) {

		if ("".equals(className) || "".equals(configHome)) return null;

		CharFilter charFilter = null;

		if ("org.apache.lucene.analysis.charfilter.MappingCharFilter".equals(className)) {
			String mapping = prop.getProperty("mapping");
			NormalizeCharMap charMap = MultilingualFactoryHelper.getNormalizeCharMap( mapping, configHome, matchVersion);
			charFilter = new MappingCharFilter(charMap, reader);
		}

		if ("org.apache.lucene.analysis.fa.PersianCharFilter".equals(className)) {
			//charFilter = new PersianCharFilter(CharReader.get(reader));
			charFilter = new PersianCharFilter(reader);
		}
		return charFilter;
	}

	private Tokenizer tokenizerFactory(String className, Properties prop, String configHome, Version matchVersion, Reader reader) {

		if ("".equals(className) || "".equals(configHome)) return null;

		Tokenizer tokenizer = null;

		if ("org.apache.lucene.analysis.standard.StandardTokenizer".equals(className)) {
			tokenizer = new StandardTokenizer(matchVersion, reader);
		}

		if ("org.apache.lucene.analysis.core.WhitespaceTokenizer".equals(className)) {
			tokenizer = new WhitespaceTokenizer(matchVersion, reader);
		}

		if ("org.apache.lucene.analysis.ja.JapaneseTokenizer".equals(className)) {
			Mode mode;
			UserDictionary userDictionary;
			String modeName = prop.getProperty("mode");
			if (modeName != null) {
				mode = Mode.valueOf(modeName.toUpperCase(Locale.ENGLISH));
			} else {
				mode = JapaneseTokenizer.DEFAULT_MODE;
			}

			final String userDictionaryPath = prop.getProperty("userDictionary");

			try {
				if (userDictionaryPath != null) {
					InputStream stream = new FileInputStream(new File(MultilingualFactoryHelper.getRessourceFilePath(userDictionaryPath, configHome)));
					String encoding = prop.getProperty("userDictionaryEncoding");
					if (encoding == null) {
						encoding = IOUtils.UTF_8;
					}
					CharsetDecoder decoder = Charset.forName(encoding).newDecoder()
							.onMalformedInput(CodingErrorAction.REPORT)
							.onUnmappableCharacter(CodingErrorAction.REPORT);
					Reader reader2 = new InputStreamReader(stream, decoder);
					userDictionary = new UserDictionary(reader2);
				} else {
					userDictionary = null;
				}
			} catch (Exception e) {
				throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, e);
			}
			tokenizer = new JapaneseTokenizer(reader, userDictionary, true, mode);
		}

		return tokenizer;
	}




	private TokenStream filterFactory(String className, Properties prop, String configHome, Version matchVersion, TokenStream in) {

		if ("".equals(className) || "".equals(configHome)) return null;

		TokenFilter filter = null;

		// ja
		if ("org.apache.lucene.analysis.ja.JapaneseBaseFormFilter".equals(className)) {
			filter = new JapaneseBaseFormFilter(in);	    
		}			       
		if ("org.apache.lucene.analysis.ja.JapaneseKatakanaStemFilter".equals(className)) {
			String minimumLengthStr = prop.getProperty("minimumLength");
			int minimumLength = JapaneseKatakanaStemFilter.DEFAULT_MINIMUM_LENGTH;
			if (!"".equals(minimumLengthStr)) minimumLength = Integer.parseInt(minimumLengthStr);					
			filter = new JapaneseKatakanaStemFilter(in, minimumLength);	    
		}			       
		if ("org.apache.lucene.analysis.ja.JapanesePartOfSpeechStopFilter".equals(className)) {
			final boolean enablePositionIncrements = MultilingualFactoryHelper.isTrue(prop.getProperty("enablePositionIncrements", "false"));
			final String stopTagFiles = prop.getProperty("tags");
			final Set<String> stopTags;
			try {
				CharArraySet cas = MultilingualFactoryHelper.getWordSet(stopTagFiles, true, configHome, matchVersion);
				stopTags = new HashSet<String>();
				for (Object element : cas) {
					char chars[] = (char[]) element;
					stopTags.add(new String(chars));
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			filter = new JapanesePartOfSpeechStopFilter(enablePositionIncrements, in, stopTags);
		}			       

		// ar & fa
		if ("org.apache.lucene.analysis.ar.ArabicNormalizationFilter".equals(className)) {
			filter = new ArabicNormalizationFilter(in);	    
		}
		if ("org.apache.lucene.analysis.fa.PersianNormalizationFilter".equals(className)) {
			filter = new PersianNormalizationFilter(in);	    
		}

		if ("org.apache.lucene.analysis.ar.ArabicStemFilter".equals(className) && doStem) {
			filter = new ArabicStemFilter(in);
		}

		// bg
		if ("org.apache.lucene.analysis.bg.BulgarianStemFilter".equals(className) && doStem) {
			filter = new BulgarianStemFilter(in);
		}

		// cz
		if ("org.apache.lucene.analysis.cz.CzechStemFilter".equals(className) && doStem) {
			filter = new CzechStemFilter(in);
		}		

		// de
		if ("org.apache.lucene.analysis.de.GermanNormalizationFilter".equals(className)) {
			filter = new GermanNormalizationFilter(in);
		}		
		if ("org.apache.lucene.analysis.de.GermanMinimalStemFilter".equals(className) && doStem) {
			filter = new GermanMinimalStemFilter(in);
		}		
		if ("org.apache.lucene.analysis.de.GermanLightStemFilter".equals(className) && doStem) {
			filter = new GermanLightStemFilter(in);
		}		

		// el
		if ("org.apache.lucene.analysis.el.GreekLowerCaseFilter".equals(className)) {
			filter = new GreekLowerCaseFilter(matchVersion, in);
		}		
		if ("org.apache.lucene.analysis.el.GreekStemFilter".equals(className) && doStem) {
			filter = new GreekStemFilter(in);
		}		

		// es
		if ("org.apache.lucene.analysis.es.SpanishLightStemFilter".equals(className) && doStem) {
			filter = new SpanishLightStemFilter(in);
		}			

		// fi
		if ("org.apache.lucene.analysis.fi.FinnishLightStemFilter".equals(className) && doStem) {
			filter = new FinnishLightStemFilter(in);
		}				

		// en
		if ("org.apache.lucene.analysis.en.EnglishPossessiveFilter".equals(className)) {
			filter = new EnglishPossessiveFilter(matchVersion, in);	    
		}

		if ("org.apache.lucene.analysis.en.PorterStemFilter".equals(className) && doStem) {
			filter = new PorterStemFilter(in);
		}

		if ("org.apache.lucene.analysis.en.EnglishMinimalStemFilter".equals(className) && doStem) {
			filter = new EnglishMinimalStemFilter(in);
		}

		// fr
		if ("org.apache.lucene.analysis.fr.ElisionFilter".equals(className)) {
			final String articles = prop.getProperty("articles");
			final boolean ignoreCase = MultilingualFactoryHelper.isTrue (prop.getProperty("ignoreCase"));
			CharArraySet wordSet=null;
			try {
				wordSet = MultilingualFactoryHelper.getWordSet( articles, ignoreCase, configHome, matchVersion);
			} catch (IOException e) {
				e.printStackTrace();
			}
			filter = (wordSet == null ? new ElisionFilter(in, FrenchAnalyzer.DEFAULT_ARTICLES) : new ElisionFilter(in,wordSet));	    
		}

		if ("org.apache.lucene.analysis.fr.FrenchLightStemFilter".equals(className) && doStem) {
			filter = new FrenchLightStemFilter(in);
		}


		if ("org.apache.lucene.analysis.fr.FrenchMinimalStemFilter".equals(className) && doStem) {
			filter = new FrenchMinimalStemFilter(in);
		}

		// ga
		if ("org.apache.lucene.analysis.ga.IrishLowerCaseFilter".equals(className)) {
			filter = new IrishLowerCaseFilter(in);
		}


		// gl
		if ("org.apache.lucene.analysis.gl.GalicianStemFilter".equals(className) && doStem) {
			filter = new GalicianStemFilter(in);
		}
		if ("org.apache.lucene.analysis.gl.GalicianMinimalStemFilter".equals(className) && doStem) {
			filter = new GalicianMinimalStemFilter(in);
		}

		// cjk
		if ("org.apache.lucene.analysis.cjk.CJKWidthFilter".equals(className)) {
			filter = new CJKWidthFilter(in);
		}
		if ("org.apache.lucene.analysis.cjk.CJKBigramFilter".equals(className)) {
			filter = new CJKBigramFilter(in);
		}

		// hi
		if ("org.apache.lucene.analysis.in.IndicNormalizationFilter".equals(className)) {
			filter = new IndicNormalizationFilter(in);
		}
		if ("org.apache.lucene.analysis.in.HindiNormalizationFilter".equals(className)) {
			filter = new HindiNormalizationFilter(in);
		}
		if ("org.apache.lucene.analysis.in.HindiStemFilter".equals(className) && doStem) {
			filter = new HindiStemFilter(in);
		}

		// hu
		if ("org.apache.lucene.analysis.hu.HungarianLightStemFilter".equals(className) && doStem) {
			filter = new HungarianLightStemFilter(in);
		}

		// id
		if ("org.apache.lucene.analysis.id.IndonesianStemFilter".equals(className) && doStem) {
			final boolean stemDerivational = MultilingualFactoryHelper.isTrue (prop.getProperty("stemDerivational"));
			filter = new IndonesianStemFilter(in, stemDerivational);
		}

		// it
		if ("org.apache.lucene.analysis.it.ItalianLightStemFilter".equals(className) && doStem) {
			filter = new ItalianLightStemFilter(in);
		}

		// lv
		if ("org.apache.lucene.analysis.lv.LatvianStemFilter".equals(className) && doStem) {
			filter = new LatvianStemFilter(in);
		}

		// nl
		if ("org.apache.lucene.analysis.miscellaneous.StemmerOverrideFilter".equals(className) && doStem) {
			final boolean ignoreCase = MultilingualFactoryHelper.isTrue (prop.getProperty("ignoreCase"));
			final String dictionary = prop.getProperty("dictionary");
			CharArrayMap<String> charMap = MultilingualFactoryHelper.getCharArrayMap( dictionary, ignoreCase, configHome, matchVersion);
			//filter = (charMap == null ? (TokenFilter) in : new StemmerOverrideFilter(matchVersion, in, charMap));
			filter = (charMap == null ? (TokenFilter) in : new StemmerOverrideFilter(in, charMap)); // since 4.2.1			
		}

		// pt
		if ("org.apache.lucene.analysis.pt.PortugueseLightStemFilter".equals(className) && doStem) {
			filter = new PortugueseLightStemFilter(in);
		}
		if ("org.apache.lucene.analysis.pt.PortugueseMinimalStemFilter".equals(className) && doStem) {
			filter = new PortugueseMinimalStemFilter(in);
		}
		if ("org.apache.lucene.analysis.pt.PortugueseStemFilter".equals(className) && doStem) {
			filter = new PortugueseStemFilter(in);
		}

		// no
		if ("org.apache.lucene.analysis.no.NorwegianLightStemFilter".equals(className) && doStem) {
			filter = new NorwegianLightStemFilter(in);
		}

		if ("org.apache.lucene.analysis.no.NorwegianMinimalStemFilter".equals(className) && doStem) {
			filter = new NorwegianMinimalStemFilter(in);
		}

		// ru
		if ("org.apache.lucene.analysis.ru.RussianLightStemFilter".equals(className) && doStem) {
			filter = new RussianLightStemFilter(in);
		}

		// sv
		if ("org.apache.lucene.analysis.sv.SwedishLightStemFilter".equals(className) && doStem) {
			filter = new SwedishLightStemFilter(in);
		}

		// th
		if ("org.apache.lucene.analysis.th.ThaiWordFilter".equals(className) && doStem) {
			filter = new ThaiWordFilter(matchVersion, in);
		}


		// generic
		if ("org.apache.lucene.analysis.LowerCaseFilter".equals(className)) {
			filter = new LowerCaseFilter(matchVersion, in);
		}

		if ("org.apache.lucene.analysis.standard.StandardFilter".equals(className)) {
			filter = new StandardFilter(matchVersion, in);
		}

		if ("org.apache.lucene.analysis.StopFilter".equals(className)) {
			final String words = prop.getProperty("words");
			final String format = prop.getProperty("format");
			final boolean ignoreCase = MultilingualFactoryHelper.isTrue (prop.getProperty("ignoreCase"));
			final boolean enablePositionIncrements = MultilingualFactoryHelper.isTrue (prop.getProperty("enablePositionIncrements"));
			CharArraySet stopWords=null;
			try {
				if ("snowball".equalsIgnoreCase(format)) {
					stopWords = MultilingualFactoryHelper.getSnowballWordSet( words, ignoreCase, configHome, matchVersion);

				} else {
					stopWords = MultilingualFactoryHelper.getWordSet( words, ignoreCase, configHome, matchVersion);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			StopFilter stopFilter = new StopFilter(matchVersion,in,stopWords);
			stopFilter.setEnablePositionIncrements(enablePositionIncrements);
			filter = stopFilter;
		}

		if ("org.apache.lucene.analysis.synonym.SynonymFilter".equals(className)) {
			final String synonyms = prop.getProperty("synonyms");
			final boolean expand = MultilingualFactoryHelper.isTrueDefault (prop.getProperty("expand"), "true");
			final boolean dedup = MultilingualFactoryHelper.isTrueDefault (prop.getProperty("dedup"), "true");
			final boolean ignoreCase = MultilingualFactoryHelper.isTrueDefault (prop.getProperty("ignoreCase"), "true");

			Analyzer analyzer = new Analyzer() {
				@Override
				protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
					Tokenizer tokenizer = new WhitespaceTokenizer(Version.LUCENE_31, reader);
					TokenStream stream = ignoreCase ? new LowerCaseFilter(Version.LUCENE_31, tokenizer) : tokenizer;
					return new TokenStreamComponents(tokenizer, stream);
				}
			};

			SynonymMap map;
			String format = prop.getProperty("format");
			try {
				if (format == null || format.equals("solr")) {
					map = MultilingualFactoryHelper.loadSolrSynonyms(synonyms, dedup, expand, analyzer, configHome);
				} else if (format.equals("wordnet")) {
					map = MultilingualFactoryHelper.loadWordnetSynonyms(synonyms, dedup, expand, analyzer, configHome);
				} else {
					throw new RuntimeException("Unrecognized synonyms format: " + format);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			filter = (TokenFilter) (map.fst == null ? in : new SynonymFilter(in, map, ignoreCase));
		}


		if ("org.apache.lucene.analysis.KeywordMarkerFilter".equals(className)) {
			final String protectedWordsFileName = prop.getProperty("protected");
			final boolean ignoreCase = MultilingualFactoryHelper.isTrue (prop.getProperty("ignoreCase"));
			CharArraySet protectedWords=null;
			try {
				protectedWords = MultilingualFactoryHelper.getSnowballWordSet( protectedWordsFileName, ignoreCase, configHome, matchVersion);
			} catch (IOException e) {
				e.printStackTrace();
			}
			filter = new KeywordMarkerFilter(in, protectedWords);
		}


		if ("org.apache.lucene.analysis.snowball.SnowballFilter".equals(className) && doStem) {
			String language = prop.getProperty("language");
			filter = new SnowballFilter(in, language);
		}

		if (filter==null) {
			// log error
			filter = (TokenFilter) in;
		}

		return filter;
	}

	/**
	 * Implementation of {@link ReuseStrategy} in order to disable reuse.
	 * The stream component may change at each query and index time according 
	 * to text language
	 */
	public static class NoReuseStrategy extends ReuseStrategy {

		/**
		 * {@inheritDoc}
		 */
		@SuppressWarnings("unchecked")
		public TokenStreamComponents getReusableComponents(String fieldName) {
			System.out.println("MultilingualAnalizer - getReusableComponents");
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@SuppressWarnings("unchecked")
		public void setReusableComponents(String fieldName, TokenStreamComponents components) {
			System.out.println("MultilingualAnalizer - setReusableComponents");
			return;
		}
	}

	private void TRACE(String msg) {
		if (trace) System.out.println(msg);
	}



}
