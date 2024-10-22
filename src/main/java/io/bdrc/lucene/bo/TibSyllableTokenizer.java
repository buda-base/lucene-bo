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
import java.util.ArrayList;
import java.util.List;

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
    private final IsStandardTibetanAttribute istAtt = addAttribute(IsStandardTibetanAttribute.class);

    private final CharacterBuffer ioBuffer = CharacterUtils.newCharacterBuffer(IO_BUFFER_SIZE);
    private char[] bufferForStacks = null;
    
    private final List<Integer> stackBreaks = new ArrayList<>();  // To store stack break positions
    private final List<Integer> stackBreakOffsets = new ArrayList<>();  // To store stack break positions
    private int stackBreakIndex = 0;  // To track the current stack break being processed
    private int stackStart = -1;
    private int stackStartOffset = -1;
    private int stackEnd = -1;
    
    private boolean tokenizeNonStandardTibIntoStacks = true;
    
    /**
     * Construct a new TibSyllableTokenizer.
     */
    public TibSyllableTokenizer() { }
    
    public TibSyllableTokenizer(final boolean tokenizeNonStandardTibIntoStacks) {
        this.tokenizeNonStandardTibIntoStacks = tokenizeNonStandardTibIntoStacks;
    }

    // see http://jrgraphix.net/r/Unicode/0F00-0FFF
    protected boolean isTibLetterOrDigit(int c) {
        return ('\u0F40' <= c && c <= '\u0FBC') || ('\u0F20' <= c && c <= '\u0F33') || (c == '\u0F00');
    }
    
    @Override
    public final boolean incrementToken() throws IOException {
        clearAttributes();

        // If there are remaining stack breaks, return the next token based on stack breaks
        if (stackBreakIndex < stackBreaks.size()) {
            int start = stackStart;
            int startOffset = stackStartOffset;
            int end = stackBreaks.get(stackBreakIndex);
            int endOffset = stackBreakOffsets.get(stackBreakIndex);
            //System.out.println(String.format("start=%d, startOffset=%d, end=%d, endOffset=%d", start, startOffset, end, endOffset));
            termAtt.copyBuffer(bufferForStacks, start, end - start);
            istAtt.setIsStandardTibetan(false);
            offsetAtt.setOffset(startOffset, endOffset);
            stackStart = end;  // Move the start to the next break
            stackStartOffset = endOffset;
            stackBreakIndex++;
            return true;
        } else {
            istAtt.setIsStandardTibetan(true);
        }

        // Normal tokenization process
        int length = 0;
        int start = -1;
        int end = -1;
        char[] buffer = termAtt.buffer();

        while (true) {
            if (bufferIndex >= dataLen) {
                offset += dataLen;
                CharacterUtils.fill(ioBuffer, input); // Fill buffer
                if (ioBuffer.getLength() == 0) {
                    dataLen = 0;
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

            final int c = Character.codePointAt(ioBuffer.getBuffer(), bufferIndex, ioBuffer.getLength());
            final int charCount = Character.charCount(c);
            bufferIndex += charCount;

            if (isTibLetterOrDigit(c)) {  // Token character
                if (length == 0) {
                    start = offset + bufferIndex - charCount;
                    end = start;
                } else if (length >= buffer.length - 1) {
                    buffer = termAtt.resizeBuffer(2 + length);
                }

                end += charCount;
                length += Character.toChars(c, buffer, length);

                if (length >= maxTokenLen) {
                    break;
                }
            } else if (length > 0) {
                break;
            }
        }

        // Check if the token is a valid Tibetan syllable
        if (length > 0) {
            if (tokenizeNonStandardTibIntoStacks && !CommonHelpers.isStandardTibetan(buffer, 0, length)) {
                //System.out.println(String.copyValueOf(buffer, 0, length)+" is not standard Tibetan");
                // It's not a valid Tibetan syllable, so split it into smaller tokens
                stackBreaks.clear();  // Clear any previous stack breaks
                stackBreakOffsets.clear();
                stackBreakIndex = 0;
                stackStart = 0;  // Initialize the start of the stack
                stackStartOffset = correctOffset(start);
                int currentStackBreak = 0;

                while (currentStackBreak < length) {
                    int nextBreak = CommonHelpers.nextStackBreak(buffer, currentStackBreak, length);
                    // System.out.println(String.format("currentStackBreak=%d, length=%d -> nextBreak=%d", currentStackBreak, length, nextBreak));
                    stackBreaks.add(nextBreak);  // Add the break position
                    stackBreakOffsets.add(correctOffset(start+nextBreak));
                    currentStackBreak = nextBreak;
                }
                stackBreakOffsets.add(correctOffset(end));
                // Return the first stack as the token                
                stackEnd = stackBreaks.get(0);
                // System.out.println(String.format("stackend=%d, stackendoffset=%d", stackEnd, stackBreakOffsets.get(0)));
                istAtt.setIsStandardTibetan(false);
                bufferForStacks = new char[length];
                System.arraycopy(buffer, 0, bufferForStacks, 0, length);
                termAtt.copyBuffer(buffer, stackStart, stackEnd - stackStart);
                offsetAtt.setOffset(stackStartOffset, stackBreakOffsets.get(0));
                stackStart = stackEnd;  // Move to the next break
                stackBreakIndex = 1;
                return true;
            } else {
                // Valid syllable, return it as a single token
                termAtt.copyBuffer(buffer, 0, length);
                istAtt.setIsStandardTibetan(true);
                offsetAtt.setOffset(correctOffset(start), correctOffset(end));
                return true;
            }
        }
        return false;
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
      stackBreaks.clear();
      stackBreakOffsets.clear();
      stackBreakIndex = 0;
    }

}
