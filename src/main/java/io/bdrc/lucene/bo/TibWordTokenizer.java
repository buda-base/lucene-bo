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
		
	public TibWordTokenizer(String filename) throws FileNotFoundException, IOException {
		init(filename);
	}
	
	public TibWordTokenizer() throws FileNotFoundException, IOException {
		init("resource/output/total_lexicon.txt");
	}
	
	private void init(String filename) throws FileNotFoundException, IOException {
		this.scanner = new Trie(true);
		
//		currently only adds the entries without any diff
		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		    	int spaceIndex = line.indexOf(' ');
		    	if (spaceIndex == -1) {
		    		// error!
		    	} else {
		    		System.out.println("adding "+line.substring(0, spaceIndex));
		    		//this.scanner.add(line.substring(0, spaceIndex), "X");
		    	}
		    }
			//Optimizer opt = new Optimizer();
			//this.scanner.reduce(opt);
		}
	}
	
	  private int offset = 0, bufferIndex = 0, dataLen = 0, finalOffset = 0;
	  private static final int MAX_WORD_LEN = 255;
	  private static final int IO_BUFFER_SIZE = 4096;
	  
//	  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
//	  private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
	  
	  private final CharacterBuffer ioBuffer = CharacterUtils.newCharacterBuffer(IO_BUFFER_SIZE);

	  /**
	   * Called on each token character to normalize it before it is added to the
	   * token. The default implementation does nothing. Subclasses may use this to,
	   * e.g., lowercase tokens.
	   */
	  protected int normalize(int c) {
	    return c;
	  }
	  
	public boolean isTibLetter(int c) {
		return ('\u0F40' <= c && c <= '\u0FBC');
	}
	
	@Override
	public final boolean incrementToken() throws IOException {
		clearAttributes();
		int length = 0;
	    int start = -1; // this variable is always initialized
	    int end = -1;
	    int confirmedEnd = -1;
	    int confirmedEndIndex = -1;
	    int w = -1;
	    boolean potentialEnd = false;
	    boolean passedFirstSyllable = false;
	    Row now = null;
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

	      if (isTibLetter(c) || (c == '\u0F0B' && length > 0)) {  // if it's a token char
	    	System.out.println("loop on "+ (char) c); 
	        if (length == 0) {                // start of token
	          assert(start == -1);
	          now = scanner.getRow(scanner.getRoot());
	          start = offset + bufferIndex - charCount;
	          end = start + charCount;
	        } else {
	        	if (length >= buffer.length-1) { // check if a supplementary could run out of bounds
	        		buffer = termAtt.resizeBuffer(2+length); // make sure a supplementary fits in the buffer
	        	}
	        	if (now == null) {
	        		System.out.println("now is null");
	        		if (!passedFirstSyllable) {
	        			// we're in a broken state (in the first syllable and no match)
		        		// we just want to go to the end of the syllable
		        		if (c == '\u0F0B') {
		        			confirmedEnd = end;
		        			confirmedEndIndex = bufferIndex;
		        			break;
		        		}
	        		} else {
	        			break;
	        		}
	        	} else {
	        		if (c == '\u0F0B') {
	        			passedFirstSyllable = true;
	        			if (potentialEnd) {
	        				confirmedEnd = end;
	        				confirmedEndIndex = bufferIndex;
	        				System.out.println("confirming end at previous character");
	        			}
	        		}
		        	end += charCount;
		        	w = now.getCmd((char) c); // TODO: problems ahead if c is > 16 bit
		        	potentialEnd = (w >= 0); // we may have caught the end, but we must check if next character is a tsheg
		        	w = now.getRef((char) c);
		        	now = (w >= 0) ? scanner.getRow(w) : null;
	        	}
	        }
	        
	        length += Character.toChars(normalize(c), buffer, length); // buffer it, normalized
	        if (length >= MAX_WORD_LEN) { // buffer overflow! make sure to check for >= surrogate pair could break == test
	          break;
	        }
	      } else if (length > 0) {           // at non-Letter w/ chars
	        break;                           // return 'em
	      }
	    }
    	if (potentialEnd) {
    		confirmedEnd = end;
    		confirmedEndIndex = bufferIndex;
    	}
	    System.out.println("confirmed end: "+confirmedEnd);
	    System.out.println("start: "+start);
	    if (confirmedEnd > 0) {
	    	bufferIndex = confirmedEndIndex;
	    	end = confirmedEnd;
	    }
	    termAtt.setLength(end - start);
	    assert(start != -1);
    	finalOffset = correctOffset(end);
	    offsetAtt.setOffset(correctOffset(start), finalOffset);
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
