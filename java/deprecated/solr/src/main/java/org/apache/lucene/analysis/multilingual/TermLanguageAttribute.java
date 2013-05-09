package org.apache.lucene.analysis.multilingual;

import org.apache.lucene.util.Attribute;

public interface TermLanguageAttribute extends Attribute {
	public void setLanguage(String language);
	public String getLanguage();
}
