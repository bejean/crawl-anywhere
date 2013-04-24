package org.apache.lucene.analysis.multilingual;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;

public class MultilingualAnalyzer extends MultilingualAbstractAnalyzer {
		
	public TokenStream tokenStream(String fieldName, Reader reader) {
		return tokenStreamDo(fieldName, reader, "index");
	}
	
	protected TokenStream processStemmingFilter(TokenStream input, String langCode)
	{
		return input;
	}	
	protected TokenStream processLogFilter(TokenStream input)
	{
		return input;
	}	

/*
	public static void main(String[] args) throws IOException {
		// text to tokenize
		final String text = "This is a demo of the new TokenStream API";

		MultilingualAnalyzer analyzer = new MultilingualAnalyzer();
		TokenStream stream = analyzer.tokenStream("field", new StringReader("[en]" + text));

		// get the TermAttribute from the TokenStream
		TermAttribute termAtt = (TermAttribute) stream.addAttribute(TermAttribute.class);

		// get the TermLanguageAttribute from the TokenStream
		TermLanguageAttribute langAtt = (TermLanguageAttribute) stream.addAttribute(TermLanguageAttribute.class);

		stream.reset();

		// print all tokens until stream is exhausted
		while (stream.incrementToken()) {
			System.out.println(termAtt.term() + ": " + langAtt.getLanguage());
		}

		stream.end();
		stream.close();

		stream = analyzer.tokenStream("field", new StringReader(text));

		// get the TermAttribute from the TokenStream
		termAtt = (TermAttribute) stream.addAttribute(TermAttribute.class);

		// get the TermLanguageAttribute from the TokenStream
		langAtt = (TermLanguageAttribute) stream.addAttribute(TermLanguageAttribute.class);

		stream.reset();

		// print all tokens until stream is exhausted
		while (stream.incrementToken()) {
			System.out.println(termAtt.term() + ": " + langAtt.getLanguage());
		}

		stream.end();
		stream.close();

	
	}
*/

}
