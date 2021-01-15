/*******************************************************************************
 * Copyright (c) 2017 Buddhist Digital Resource Center (BDRC)
 * 
 * If this file is a derivation of another work the license header will appear 
 * below; otherwise, this work is licensed under the Apache License, Version 2.0 
 * (the "License"); you may not use this file except in compliance with the 
 * License.
 * 
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package io.bdrc.lucene.bo;

import java.io.IOException;

import org.apache.lucene.analysis.CharacterUtils;
import org.apache.lucene.analysis.CharacterUtils.CharacterBuffer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

/**
 * A TibSyllableTokenizer divides text between sequences of Tibetan Letter
 * and/or Digit characters and sequences of all other characters - typically
 * some sort of white space but other punctuation and characters from other
 * language code-pages are not considered as constituents of tokens for the
 * purpose of search and indexing.
 * <p>
 * Adjacent sequences of Tibetan Letter and/or Digit characters form tokens.
 * </p>
 * <p>
 * Derived from Lucene 8.0.0 CharTokenizer
 * </p>
 * 
 */
public final class TibSyllableTokenizer extends Tokenizer {
    
    
    
    private int offset = 0, bufferIndex = 0, dataLen = 0, finalOffset = 0;
    public static final int DEFAULT_MAX_WORD_LEN = 255;
    private static final int IO_BUFFER_SIZE = 4096;
    private final int maxTokenLen = 255;

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

    private final CharacterBuffer ioBuffer = CharacterUtils.newCharacterBuffer(IO_BUFFER_SIZE);
    
    /**
     * Construct a new TibSyllableTokenizer.
     */
    public TibSyllableTokenizer() {
    }

    // see http://jrgraphix.net/r/Unicode/0F00-0FFF
    protected boolean isTibLetterOrDigit(int c) {
        return ('\u0F40' <= c && c <= '\u0FBC') || ('\u0F20' <= c && c <= '\u0F33') || (c == '\u0F00');
    }

    public final static int ST_INIT = 0;
    public final static int ST_TIB = 0;
    public final static int ST_NTIB = 0;
    
    @Override
    public final boolean incrementToken() throws IOException {
      clearAttributes();
      int length = 0;
      int start = -1; // this variable is always initialized
      int end = -1;
      char[] buffer = termAtt.buffer();
      while (true) {
        if (bufferIndex >= dataLen) {
          offset += dataLen;
          CharacterUtils.fill(ioBuffer, input); // read supplementary char aware with CharacterUtils
          if (ioBuffer.getLength() == 0) {
            dataLen = 0; // so next offset += dataLen won't decrement offset
            if (length > 0) {
              break;
            } else {
              finalOffset = correctOffset(offset);
              return false;
            }
          }
          dataLen = ioBuffer.getLength();
          bufferIndex = 0;
        }
        // use CharacterUtils here to support < 3.1 UTF-16 code unit behavior if the char based
        // methods are gone
        final int c = Character.codePointAt(ioBuffer.getBuffer(), bufferIndex, ioBuffer.getLength());
        final int charCount = Character.charCount(c);
        bufferIndex += charCount;
        if (isTibLetterOrDigit(c)) { // if it's a token char
          if (length == 0) { // start of token
            start = offset + bufferIndex - charCount;
            end = start;
          } else if (length >= buffer.length - 1) { // supplementary could run out of bounds?
            // make sure a supplementary fits in the buffer
            buffer = termAtt.resizeBuffer(2 + length);
          }
          end += charCount;
          length += Character.toChars(c, buffer, length); // buffer it, normalized
          // buffer overflow! make sure to check for >= surrogate pair could break == test
          // XXX: manual change here to cut after 0F7F
          if (c == '\u0f7f' || length >= maxTokenLen) {
            break;
          }
        } else if (length > 0) { // at non-Letter w/ chars
          break; // return 'em
        }
      }

      termAtt.setLength(length);
      offsetAtt.setOffset(correctOffset(start), finalOffset = correctOffset(end));
      return true;
    }

    @Override
    public final void end() throws IOException {
      super.end();
      // set final offset
      offsetAtt.setOffset(finalOffset, finalOffset);
    }

    @Override
    public void reset() throws IOException {
      super.reset();
      bufferIndex = 0;
      offset = 0;
      dataLen = 0;
      finalOffset = 0;
      ioBuffer.reset(); // make sure to reset the IO buffer!!
    }

}
