package org.apache.lucene.search.parser;

import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Query;

public interface SpellParser {
    //public Query parse(String queryString) throws ParseException;
    //public Query suggest(String queryString, int numSug) throws ParseException;
    public String[] getSimilarWords(String queryString, int numSug) throws ParseException;
}

