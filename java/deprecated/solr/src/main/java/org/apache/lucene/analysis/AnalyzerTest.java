package org.apache.lucene.analysis;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.util.Version;

import org.apache.lucene.analysis.generic.GenericAnalyzerEx;
import org.apache.lucene.analysis.multilingual.MultilingualAnalyzer;

import java.io.*;

public class AnalyzerTest {
    private static final String[] strings = {
        "[en]The quick brown fox jumped over the lazy dogs",
        "[fr]XY&Z Corporation - mots-clés xyz@example.com et avec des accentués et l'apostrophe l'équipe. 23a12 ab2cd sport Sport Sports",
        "[en]400 500"
    };

    private static final Analyzer[] analyzers = new Analyzer[]{
        new GenericAnalyzerEx(),
        new GenericAnalyzerEx("English"),
        new GenericAnalyzerEx("French"),
        new WhitespaceAnalyzer(),
        new SimpleAnalyzer(),
        //new StopAnalyzer(),
        new StandardAnalyzer(Version.LUCENE_CURRENT),
        new SnowballAnalyzer(Version.LUCENE_29, "English")
    };
    private static final Analyzer[] multilingualAnalyzers = new Analyzer[]{
    	new MultilingualAnalyzer()
    };

    public static void main(String[] args) throws IOException {
        for (int i = 0; i < strings.length; i++) {
            analyze(strings[i]);
        }
    }

    private static void analyze(String text) throws IOException {
    	
    	String text0 = text.substring(4);
    	
        System.out.println("Analzying \"" + text0 + "\"");
        for (int i = 0; i < analyzers.length; i++) {
            Analyzer analyzer = analyzers[i];
            System.out.println("\t" + analyzer.getClass().getName() + ":");
            System.out.print("\t\t");
            TokenStream stream = analyzer.tokenStream("contents", new StringReader(text0));
            TermAttribute termAtt = (TermAttribute) stream.getAttribute(TermAttribute.class);
            //PositionIncrementAttribute posIncrAtt = stream.addAttribute(PositionIncrementAttribute.class);
            while (stream.incrementToken()) {
                String text2 = termAtt.term();
                System.out.print("[" + text2 + "] ");
            }
            /*
            while (true) {
                Token token = stream.next();
                if (token == null) break;

                System.out.print("[" + token.termText() + "] ");
            }
            */
            System.out.println("\n");
        }
        for (int i = 0; i < multilingualAnalyzers.length; i++) {
            Analyzer analyzer = multilingualAnalyzers[i];
            System.out.println("\t" + analyzer.getClass().getName() + ":");
            System.out.print("\t\t");
            TokenStream stream = analyzer.tokenStream("contents", new StringReader(text));
            TermAttribute termAtt = (TermAttribute) stream.getAttribute(TermAttribute.class);
            //PositionIncrementAttribute posIncrAtt = stream.addAttribute(PositionIncrementAttribute.class);
            while (stream.incrementToken()) {
                String text2 = termAtt.term();
                System.out.print("[" + text2 + "] ");
            }
            /*
            while (true) {
                Token token = stream.next();
                if (token == null) break;

                System.out.print("[" + token.termText() + "] ");
            }
            */
            System.out.println("\n");
        }
    }

}