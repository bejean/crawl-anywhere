package org.apache.lucene.analysis.generic;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import java.io.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;


/**
 * An abstract base class for simple, character-oriented tokenizers.
 */
public class GenericTokenizer extends Tokenizer {
    public GenericTokenizer(Reader input) {
        super(input);
        offsetAtt = (OffsetAttribute) addAttribute(OffsetAttribute.class);
        termAtt = (TermAttribute) addAttribute(TermAttribute.class);
    }

    private int offset = 0, bufferIndex = 0, dataLen = 0;
    private static final int MAX_WORD_LEN = 255;
    private static final int IO_BUFFER_SIZE = 1024;
    //private final char[] buffer = new char[MAX_WORD_LEN];
    private final char[] ioBuffer = new char[IO_BUFFER_SIZE];

    private TermAttribute termAtt;
    private OffsetAttribute offsetAtt;

    /** Returns true iff a character should be included in a token.  This
     * tokenizer generates as tokens adjacent sequences of characters which
     * satisfy this predicate.  Characters for which this is false are used to
     * define token boundaries and are not included in tokens. */
    /**
     * Collects only characters which satisfy
     * {@link Character#isLetter(char)}.
     */
    protected boolean isTokenChar(char c) {
        return Character.isLetter(c);
    }

    /** Called on each token character to normalize it before it is added to the
     * token.  The default implementation does nothing.  Subclasses may use this
     * to, e.g., lowercase tokens. */
    /**
     * Collects only characters which satisfy
     * {@link Character#isLetter(char)}.
     */
    protected char normalize(char c) {
        return Character.toLowerCase(c);
    }


    /**
     * Returns the next token in the stream, or null at EOS.
     */
    /*
    public final Token next() throws IOException {
        int length = 0;
        int start = offset;
        while (true) {

            String str = null;

            offset++;
            if (bufferIndex >= dataLen) {
                dataLen = input.read(ioBuffer);
                bufferIndex = 0;
            }

            if (dataLen == -1) {
                if (length > 0)
                    break;
                else
                    return null;
            } else
                str = GenericNormalizer.normalize(ioBuffer[bufferIndex++]);

            boolean stop = false;
            for (int i = 0; i < str.length(); i++) {
                final char c;
                c = str.charAt(i);
                if (isTokenChar(c)) {                   // if it's a token char

                    if (length == 0)                    // start of token
                        start = offset - 1;

                    buffer[length++] = normalize(c);    // buffer it, normalized

                    if (length == MAX_WORD_LEN)         // buffer overflow!
                        break;

                } else if (length > 0)                  // at non-Letter w/ chars
                    stop = true;
                //break;                                // return 'em
            }
            if (stop)
                break;
        }

        return new Token(new String(buffer, 0, length), start, start + length);
    }
    */
    
    
    public final boolean incrementToken() throws IOException {
        clearAttributes();
        int length = 0;
        int start = bufferIndex;
        char[] buffer = termAtt.termBuffer();
        while (true) {

          if (bufferIndex >= dataLen) {
            offset += dataLen;
            dataLen = input.read(ioBuffer);
            if (dataLen == -1) {
              dataLen = 0;                            // so next offset += dataLen won't decrement offset
              if (length > 0)
                break;
              else
                return false;
            }
            bufferIndex = 0;
          }

          final char c = ioBuffer[bufferIndex++];

          if (isTokenChar(c)) {               // if it's a token char

            if (length == 0)                 // start of token
              start = offset + bufferIndex - 1;
            else if (length == buffer.length)
              buffer = termAtt.resizeTermBuffer(1+length);

            buffer[length++] = GenericNormalizer.normalize(c);

            if (length == MAX_WORD_LEN)      // buffer overflow!
              break;

          } else if (length > 0)             // at non-Letter w/ chars
            break;                           // return 'em
        }

        termAtt.setTermLength(length);
        offsetAtt.setOffset(correctOffset(start), correctOffset(start+length));
        return true;
      }
      
      @Override
      public final void end() {
        // set final offset
        int finalOffset = correctOffset(offset);
        offsetAtt.setOffset(finalOffset, finalOffset);
      }

      @Override
      public void reset(Reader input) throws IOException {
        super.reset(input);
        bufferIndex = 0;
        offset = 0;
        dataLen = 0;
      }
}
