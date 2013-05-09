package org.apache.lucene.analysis.multilingual;

import java.io.Reader;

import org.apache.lucene.analysis.TokenStream;

public class MultilingualAnalyzerQuery extends MultilingualAnalyzer {

	public TokenStream tokenStream(String fieldName, Reader reader) {
		return tokenStreamDo(fieldName, reader, "query");
	}

}
