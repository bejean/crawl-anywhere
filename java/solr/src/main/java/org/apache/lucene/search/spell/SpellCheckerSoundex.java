package org.apache.lucene.search.spell;

import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.Directory;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Document;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: administrator
 * Date: 20 mars 2008
 * Time: 20:48:35
 * To change this template use File | Settings | File Templates.
 */
public class SpellCheckerSoundex extends SpellChecker {

    public SpellCheckerSoundex() throws IOException {
        super(null);
    }

    public SpellCheckerSoundex(Directory spellIndex) throws IOException {
      super(spellIndex);
    }

    private static Document createDocument(String text, int ng1, int ng2) {
      Document doc = new Document();
      //doc.add(new Field(F_WORD, text, Field.Store.YES, Field.Index.UN_TOKENIZED)); // orig term
      doc.add(new Field(F_WORD, text, Field.Store.YES, Field.Index.NOT_ANALYZED)); // orig term
      addGram(text, doc, ng1, ng2);
      addSoundex(text, doc);
      return doc;
    }

    private static void addGram(String text, Document doc, int ng1, int ng2) {
      int len = text.length();
      for (int ng = ng1; ng <= ng2; ng++) {
        String key = "gram" + ng;
        String end = null;
        for (int i = 0; i < len - ng + 1; i++) {
          String gram = text.substring(i, i + ng);
          //doc.add(new Field(key, gram, Field.Store.NO, Field.Index.UN_TOKENIZED));
          doc.add(new Field(key, gram, Field.Store.NO, Field.Index.NOT_ANALYZED));
          if (i == 0) {
            //doc.add(new Field("start" + ng, gram, Field.Store.NO, Field.Index.UN_TOKENIZED));
            doc.add(new Field("start" + ng, gram, Field.Store.NO, Field.Index.NOT_ANALYZED));
          }
          end = gram;
        }
        if (end != null) { // may not be present if len==ng1
          //doc.add(new Field("end" + ng, end, Field.Store.NO, Field.Index.UN_TOKENIZED));
          doc.add(new Field("end" + ng, end, Field.Store.NO, Field.Index.NOT_ANALYZED));
        }
      }
    }

    private static void addSoundex(String text, Document doc) {
        //doc.add(new Field("soundex", Soundex.soundex(text), Field.Store.YES, Field.Index.UN_TOKENIZED));
        doc.add(new Field("soundex", Soundex.soundex(text), Field.Store.YES, Field.Index.NOT_ANALYZED));
    }


}
