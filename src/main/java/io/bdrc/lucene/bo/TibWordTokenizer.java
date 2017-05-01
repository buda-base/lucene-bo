/*
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

package io.bdrc.lucene.bo;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.lucene.analysis.CharacterUtils;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.CharacterUtils.CharacterBuffer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.PagedBytes.Reader;

import io.bdrc.lucene.stemmer.Optimizer;
import io.bdrc.lucene.stemmer.Row;
import io.bdrc.lucene.stemmer.Trie;

public final class TibWordTokenizer extends Tokenizer {
	private Trie scanner;

	// this tokenizer generates three attributes:
	// term offset, positionIncrement and type
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
	private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
	private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);
		
	public TibWordTokenizer() throws FileNotFoundException, IOException {
		init();
	}
	
	
	private void init() throws FileNotFoundException, IOException {
		this.scanner = new Trie(true);
		
//		currently only adds the entries without any diff
		String file = "./resource/output/total_lexicon.txt";
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		        String[] parts = line.split(" ");
		        this.scanner.add(parts[0], "X");
		    }
			Optimizer opt = new Optimizer();
			this.scanner.reduce(opt);
		}
	}
	
	  private int offset = 0, bufferIndex = 0, dataLen = 0, finalOffset = 0;
	  private static final int MAX_WORD_LEN = 255;
	  private static final int IO_BUFFER_SIZE = 4096;
	  
//	  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
//	  private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
	  
	  private final CharacterBuffer ioBuffer = CharacterUtils.newCharacterBuffer(IO_BUFFER_SIZE);
	
	  /**
	   * Returns true iff a codepoint should be included in a token. This tokenizer
	   * generates as tokens adjacent sequences of codepoints which satisfy this
	   * predicate. Codepoints for which this is false are used to define token
	   * boundaries and are not included in tokens.
	   */
	  protected boolean isTokenChar(int c) {
		return false;
	}
	  /**
	   * Called on each token character to normalize it before it is added to the
	   * token. The default implementation does nothing. Subclasses may use this to,
	   * e.g., lowercase tokens.
	   */
	  protected int normalize(int c) {
	    return c;
	  }
	  
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
	      // use CharacterUtils here to support < 3.1 UTF-16 code unit behavior if the char based methods are gone
	      final int c = Character.codePointAt(ioBuffer.getBuffer(), bufferIndex, ioBuffer.getLength());
	      final int charCount = Character.charCount(c);
	      bufferIndex += charCount;

	      if (isTokenChar(c)) {               // if it's a token char
	        if (length == 0) {                // start of token
	          assert start == -1;
	          start = offset + bufferIndex - charCount;
	          end = start;
	        } else if (length >= buffer.length-1) { // check if a supplementary could run out of bounds
	          buffer = termAtt.resizeBuffer(2+length); // make sure a supplementary fits in the buffer
	        }
	        end += charCount;
	        length += Character.toChars(normalize(c), buffer, length); // buffer it, normalized
	        if (length >= MAX_WORD_LEN) { // buffer overflow! make sure to check for >= surrogate pair could break == test
	          break;
	        }
	      } else if (length > 0) {           // at non-Letter w/ chars
	        break;                           // return 'em
	      }
	    }

	    termAtt.setLength(length);
	    assert start != -1;
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
