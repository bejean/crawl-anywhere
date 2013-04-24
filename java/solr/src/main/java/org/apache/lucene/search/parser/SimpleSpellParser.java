package org.apache.lucene.search.parser;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.Directory;

public class SimpleSpellParser implements SpellParser {

    private String defaultField;
    private Directory spellIndexDirectory;
    private SpellChecker spellChecker = null;

    public SimpleSpellParser(String defaultField, Directory spellIndexDirectory) {
        this.defaultField = defaultField;
        this.spellIndexDirectory = spellIndexDirectory;
    }

    /*
    public Query parse(String queryString) {
        return new TermQuery(new Term(defaultField, queryString));
    }

    public Query suggest(String queryString, int numSug) throws ParseException {
        try {
            SpellChecker spellChecker = new SpellChecker(spellIndexDirectory);
            if (spellChecker.exist(queryString)) {
                return null;
            }
            String[] similarWords = spellChecker.suggestSimilar(queryString, numSug);
            if (similarWords.length == 0) {
                return null;
            }
            return new TermQuery(new Term(defaultField, similarWords[0]));
        } catch (IOException e) {
            throw new ParseException(e.getMessage());
        }
    }
    */

    public boolean initialize() {
        try {
            spellChecker = new SpellChecker(spellIndexDirectory);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            spellChecker = null;
            return false;
        }
    }

    public String[] getSimilarWords(String queryString, int numSug) throws ParseException {
        try {
            if (spellChecker==null) {
                return null;
            }
            //SpellChecker spellChecker = new SpellChecker(spellIndexDirectory);
            //if (spellChecker.exist(queryString)) {
            //    return null;
            //}
            String[] similarWords = spellChecker.suggestSimilar(queryString, numSug);
            if (similarWords.length == 0) {
                return null;
            }
            return similarWords;
        } catch (IOException e) {
            throw new ParseException(e.getMessage());
        }
    }

    /*
    public String[] getSimilarWordsMorePopular(String queryString, int numSug, IndexReader ir) throws ParseException {
        try {
            SpellChecker spellChecker = new SpellChecker(spellIndexDirectory);
            //if (spellChecker.exist(queryString)) {
            //    return null;
            //}
            String[] similarWords = spellChecker.suggestSimilar(queryString, numSug, ir, this.defaultField, true);
            if (similarWords.length == 0) {
                return null;
            }
            return similarWords;
        } catch (IOException e) {
            throw new ParseException(e.getMessage());
        }
    }
    */

}
