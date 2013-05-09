package org.apache.lucene.analysis.multilingual;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.text.ParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.charfilter.NormalizeCharMap;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.synonym.SolrSynonymParser;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.analysis.synonym.WordnetSynonymParser;
import org.apache.lucene.analysis.util.CharArrayMap;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.TextConfig;
import org.apache.lucene.analysis.util.WordlistLoader;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.Version;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.StrUtils;

public class MultilingualFactoryHelper {

	public static boolean isTrueDefault (String value, String defaultValue) {
		if (value==null || "".equals(value)) 
			return isTrue (value);
		else
			return isTrue (defaultValue);
	}

	public static boolean isTrue (String value) {
		if (value==null || "".equals(value)) return false;
		value=value.toLowerCase();
		return ("1".equals(value) || "true".equals(value) || "yes".equals(value));
	}


	/**
	 * Load synonyms from the solr format, "format=solr".
	 */
	public static SynonymMap loadSolrSynonyms(String synonyms, boolean dedup, boolean expand, Analyzer analyzer, String configHome) throws IOException, ParseException {
		if (synonyms == null)
			throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Missing required argument 'synonyms'.");

		CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder()
				.onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT);

		SolrSynonymParser parser = new SolrSynonymParser(dedup, expand, analyzer);
		File synonymFile = new File(synonyms);
		if (synonymFile.exists()) {
			decoder.reset();
			parser.add(new InputStreamReader(new FileInputStream(getRessourceFilePath(synonyms, configHome)), decoder));
		} else {
			List<String> files = StrUtils.splitFileNames(synonyms);
			for (String file : files) {
				decoder.reset();
				parser.add(new InputStreamReader(new FileInputStream(getRessourceFilePath(file, configHome)), decoder));
			}
		}
		return parser.build();
	}

	/**
	 * Load synonyms from the wordnet format, "format=wordnet".
	 */
	public static SynonymMap loadWordnetSynonyms(String synonyms, boolean dedup, boolean expand, Analyzer analyzer, String configHome) throws IOException, ParseException {
		if (synonyms == null)
			throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Missing required argument 'synonyms'.");

		CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder()
				.onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT);

		WordnetSynonymParser parser = new WordnetSynonymParser(dedup, expand, analyzer);
		File synonymFile = new File(synonyms);
		if (synonymFile.exists()) {
			decoder.reset();
			parser.add(new InputStreamReader(new FileInputStream(getRessourceFilePath(synonyms, configHome)), decoder));
		} else {
			List<String> files = StrUtils.splitFileNames(synonyms);
			for (String file : files) {
				decoder.reset();
				parser.add(new InputStreamReader(new FileInputStream(getRessourceFilePath(file, configHome)), decoder));
			}
		}
		return parser.build();
	}

	public static CharArraySet getWordSet( String wordFile, boolean ignoreCase, String configHome, Version matchVersion) throws IOException {
		CharArraySet words = null;

		if( wordFile != null ) {
			List<String> wlist = null;
			try{
				words = new CharArraySet(matchVersion, 10, ignoreCase);
				File mappingFile = new File( getRessourceFilePath(wordFile, configHome) );
				if( mappingFile.exists() ){
					wlist = TextConfig.getFileLines( getRessourceFilePath(wordFile, configHome) );
					words.addAll(StopFilter.makeStopSet(matchVersion, wlist, ignoreCase));
				}
			}
			catch( IOException e ){
				throw new RuntimeException( e );
			}
			return words;
		}
		return null;
	}

	protected static CharArraySet getSnowballWordSet( String wordFile, boolean ignoreCase, String configHome, Version matchVersion) throws IOException {

		CharArraySet words = null;

		if( wordFile != null ) {
			List<String> wlist = null;
			try{
				words = new CharArraySet(matchVersion, 10, ignoreCase);
				File mappingFile = new File( getRessourceFilePath(wordFile, configHome) );
				if( mappingFile.exists() ){
					Reader reader = null;
					InputStream stream = null;
					try {
						stream = new FileInputStream(mappingFile);
						CharsetDecoder decoder = IOUtils.CHARSET_UTF_8.newDecoder()
								.onMalformedInput(CodingErrorAction.REPORT)
								.onUnmappableCharacter(CodingErrorAction.REPORT);
						reader = new InputStreamReader(stream, decoder);
						WordlistLoader.getSnowballWordSet(reader, words);

					} finally {
						IOUtils.closeWhileHandlingException(reader, stream);
					}
				}
			}
			catch( IOException e ){
				throw new RuntimeException( e );
			}
			return words;
		}
		return null;
	}

	public static NormalizeCharMap getNormalizeCharMap(String mapping, String configHome, Version matchVersion) {
		NormalizeCharMap normMap = null;

		if( mapping != null ) {
			List<String> wlist = null;
			try{
				File mappingFile = new File( getRessourceFilePath(mapping, configHome) );
				if( mappingFile.exists() ){
					wlist = TextConfig.getFileLines( getRessourceFilePath(mapping, configHome) );
				}
			}
			catch( IOException e ){
				throw new RuntimeException( e );
			}
			//normMap = new NormalizeCharMap();
			//parseRules( wlist, normMap , mapping);

			final NormalizeCharMap.Builder builder = new NormalizeCharMap.Builder();
			parseRules(wlist, builder, mapping);
		    normMap = builder.build();
		      
			return normMap;
		}
		return null;
	}

	public static CharArrayMap<String> getCharArrayMap(String mapping, Boolean ignoreCase, String configHome, Version matchVersion) {
		CharArrayMap<String> map = null;
		if( mapping != null ) {
			List<String> wlist = null;
			try{
				File mappingFile = new File( getRessourceFilePath(mapping, configHome) );
				if( mappingFile.exists() ){
					wlist = TextConfig.getFileLines( getRessourceFilePath(mapping, configHome) );
					map = new CharArrayMap<String>(matchVersion, 10, ignoreCase);
					for (String line : wlist) {
						String[] aMapping = line.split("\t", 2);
						map.put(aMapping[0], aMapping[1]);
					}
					return map;
				}
			}
			catch( IOException e ){
				throw new RuntimeException( e );
			}
		}
		return null;
	}

/*	protected static void parseRules( List<String> rules, NormalizeCharMap normMap, String mapping ){
		// "source" => "target"
		final Pattern p = Pattern.compile( "\"(.*)\"\\s*=>\\s*\"(.*)\"\\s*$" );

		for( String rule : rules ){
			Matcher m = p.matcher( rule );
			if( !m.find() )
				throw new RuntimeException( "Invalid Mapping Rule : [" + rule + "], file = " + mapping );
			normMap.add( parseString( m.group( 1 ) ), parseString( m.group( 2 ) ) );
		}
	}
*/

	  protected static void parseRules( List<String> rules, NormalizeCharMap.Builder builder, String mapping ){
		// "source" => "target"
		final Pattern p = Pattern.compile( "\"(.*)\"\\s*=>\\s*\"(.*)\"\\s*$" );
	    for( String rule : rules ){
	      Matcher m = p.matcher( rule );
	      if( !m.find() )
	        throw new IllegalArgumentException("Invalid Mapping Rule : [" + rule + "], file = " + mapping);
	      builder.add( parseString( m.group( 1 ) ), parseString( m.group( 2 ) ) );
	    }
	  }

	protected static String parseString( String s ){

		char[] out = new char[256];

		int readPos = 0;
		int len = s.length();
		int writePos = 0;
		while( readPos < len ){
			char c = s.charAt( readPos++ );
			if( c == '\\' ){
				if( readPos >= len )
					throw new RuntimeException( "Invalid escaped char in [" + s + "]" );
				c = s.charAt( readPos++ );
				switch( c ) {
				case '\\' : c = '\\'; break;
				case '"' : c = '"'; break;
				case 'n' : c = '\n'; break;
				case 't' : c = '\t'; break;
				case 'r' : c = '\r'; break;
				case 'b' : c = '\b'; break;
				case 'f' : c = '\f'; break;
				case 'u' :
					if( readPos + 3 >= len )
						throw new RuntimeException( "Invalid escaped char in [" + s + "]" );
					c = (char)Integer.parseInt( s.substring( readPos, readPos + 4 ), 16 );
					readPos += 4;
					break;
				}
			}
			out[writePos++] = c;
		}
		return new String( out, 0, writePos );
	}

	public static String getRessourceFilePath(String RessourceFileName, String configHome) {
		try{
			File ressourceFile = new File( RessourceFileName );
			if (ressourceFile.exists()) return RessourceFileName;
			ressourceFile = new File(configHome, RessourceFileName);
			if (ressourceFile.exists()) return ressourceFile.getAbsolutePath();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}


}
