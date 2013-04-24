package org.apache.lucene.analysis.multilingual;

import org.apache.lucene.util.AttributeImpl;

public class TermLanguageAttributeImpl extends AttributeImpl implements TermLanguageAttribute {

	public String lang = "";

	@Override
	public void clear() {
		lang="";
	}

	@Override
	public void copyTo(AttributeImpl target) {
		((TermLanguageAttributeImpl) target).lang = lang;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) return true;
		if (other instanceof TermLanguageAttributeImpl) {
			return lang == ((TermLanguageAttributeImpl) other).lang;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return 0;
	}

	public String getLanguage() {
		return lang;
	}

	public void setLanguage(String language) {
		lang = language;
	}
}
