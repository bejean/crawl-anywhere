package org.apache.lucene.index;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.search.spell.Dictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.search.spell.PlainTextDictionary;
//import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import java.io.IOException;
import java.io.File;

public class SpellIndex {
    public void createSpellIndex(String field,
                                Directory originalIndexDirectory,
                                Directory spellIndexDirectory) throws IOException {

        IndexReader indexReader = null;
        try {
            indexReader = IndexReader.open(originalIndexDirectory);
            Dictionary dictionary = new LuceneDictionary(indexReader, field);
            SpellChecker spellChecker = new SpellChecker(spellIndexDirectory);
            spellChecker.indexDictionary(dictionary);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally {
            if (indexReader != null) {
                indexReader.close();
            }
        }
    }

    public void createSpellIndex(File f,
                                Directory spellIndexDirectory) throws IOException {

        try {
            Dictionary dictionary = new PlainTextDictionary(f);
            SpellChecker spellChecker = new SpellChecker(spellIndexDirectory);
            spellChecker.indexDictionary(dictionary);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}

