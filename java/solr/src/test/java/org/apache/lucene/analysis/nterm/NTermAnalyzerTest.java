package org.apache.lucene.analysis.nterm;

import java.io.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

public class NTermAnalyzerTest {
	private static final String[] strings = {
		"iphone ipad",
		"400 500",
		"XY&Z Corporation - mots-clés xyz@example.com et avec des accentués, de ponctuations et l'apostrophe l'équipe. 23a12 ab2cd sport: Sport Sports",
		"The quick brown fox jumped over the lazy dogs",
		"mot1 mot2. mot3 mot4 mot5: mot6 mot-mot7 mot8- mot9 mot8 -mot9 mot8 - mot9 mot11 & mot12",
		"Magnolia Conference « Orange11 Blog / Orange11: Enterprise Java, Open Source, software solutions, Amsterdam "
	};

	public static void main(String[] args) throws IOException {
		for (int i = 0; i < strings.length; i++) {
			testAnalyzer(strings[i], "");
			testAnalyzer(strings[i], "/Data/Projects/CrawlAnywhere/dev/java/solr4/config/nterm_stopfilter_rules.txt");
		}
	}

	private static void testAnalyzer(String text, String rules) throws IOException {

		System.out.println("Analzying \"" + text + "\"");
		Analyzer analyzer =  new NTermAnalyzer(Version.LUCENE_40, rules);
		System.out.println("\t" + analyzer.getClass().getName() + ":");
		System.out.print("\t\t");
		TokenStream stream = analyzer.tokenStream("contents", new StringReader(text));
		//!!!
		CharTermAttribute termAtt = (CharTermAttribute) stream.getAttribute(CharTermAttribute.class);
		while (stream.incrementToken()) {
			//!!!
			String text2 = termAtt.toString();
			System.out.print("[" + text2 + "] ");
		}
		System.out.println("\n");
	}
}