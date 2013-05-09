package org.apache.lucene.analysis.multilingual;

import java.io.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

public class MultilingualAnalyzerTest {
	private static final String[] strings = {
		"The quick brown fox jumped over the lazy dogs",
		"[fr]XY&Z Corporation - mots-clés xyz@example.com et avec des accentués et l'apostrophe l'équipe.",
		"[fr]moteur de rcherche",
		"[ar]" + "الذين مَلكت أيمانكم",
		"[fa]" + "می‌خورد",
		"[bg]компютри",
		"[es]sociedades",
		"[ca]sociedades",
		"[cn]我是中国人",
		"[cz]The quick brown fox jumped over the lazy dogs",
		"[da]The quick brown fox jumped over the lazy dogs",
		"[de]The quick brown fox jumped over the lazy dogs",
		"[el]The quick brown fox jumped over the lazy dogs",
		"[eu]The quick brown fox jumped over the lazy dogs",
		"[fi]The quick brown fox jumped over the lazy dogs",
		"[gl]The quick brown fox jumped over the lazy dogs",
		"[cn]The quick brown fox jumped over the lazy dogs",
		"[ko]The quick brown fox jumped over the lazy dogs",
		"[ja]The quick brown fox jumped over the lazy dogs",
		"[hi]The quick brown fox jumped over the lazy dogs",
		"[hu]The quick brown fox jumped over the lazy dogs",
		"[hy]The quick brown fox jumped over the lazy dogs",
		"[id]The quick brown fox jumped over the lazy dogs",
		"[it]The quick brown fox jumped over the lazy dogs",
		"[lv]The quick brown fox jumped over the lazy dogs",
		"[nl]The quick brown fox jumped over the lazy dogs",
		"[no]The quick brown fox jumped over the lazy dogs",
		"[pt]The quick brown fox jumped over the lazy dogs",
		"[ra]The quick brown fox jumped over the lazy dogs",
		"[ru]The quick brown fox jumped over the lazy dogs",
		"[sv]The quick brown fox jumped over the lazy dogs",
		"[th]The quick brown fox jumped over the lazy dogs",
		"[tr]The quick brown fox jumped over the lazy dogs"
	};

	public static void main(String[] args) throws IOException {
		Analyzer analyzer = new MultilingualAnalyzer(Version.LUCENE_40, "/Data/Projects/CrawlAnywhere/dev/java/solr4/config/multilingual.xml", false, "/opt/solr40/crawler/conf", true);
		Analyzer analyzerStem =  new MultilingualAnalyzer(Version.LUCENE_40, "/Data/Projects/CrawlAnywhere/dev/java/solr4/config/multilingual.xml", true, "/opt/solr40/crawler/conf", true);

		for (int i = 0; i < strings.length; i++) {
			testAnalyzer(analyzer, strings[i]);
			testAnalyzerStem(analyzerStem, strings[i]);
		}
	}
	
	private static void testAnalyzer(Analyzer analyzer, String text) throws IOException {
		processAnalyzer(analyzer, text);
	}
	
	private static void testAnalyzerStem(Analyzer analyzerStem, String text) throws IOException {
		processAnalyzer(analyzerStem, text);
	}
	
	private static void processAnalyzer(Analyzer analyzer, String text) throws IOException {

		System.out.println("Analzying \"" + text + "\"");
		System.out.println("\t" + analyzer.getClass().getName() + ":");
		System.out.print("\t\t");
		TokenStream stream = analyzer.tokenStream("contents", new StringReader(text));
		CharTermAttribute termAtt = (CharTermAttribute) stream.getAttribute(CharTermAttribute.class);
		while (stream.incrementToken()) {
			String text2 = termAtt.toString();
			System.out.print("[" + text2 + "] ");
		}
		System.out.println("\n");
	}
}