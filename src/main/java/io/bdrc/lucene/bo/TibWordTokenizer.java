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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.util.RollingCharBuffer;

import io.bdrc.lucene.stemmer.Row;
import io.bdrc.lucene.stemmer.Trie;

/**
 * A maximal-matching word tokenizer for Tibetan that uses a {@link Trie}.
 * 
 * <p>
 * Takes a syllable at a time and returns the longest sequence of syllable that form a word within the Trie.<br>
 * {@link #isTibLetter(int)} is used to distinguish clusters of letters forming syllables and {@code u\0F0B}(tsheg) to distinguish syllables within a word.
 * <br> 
 *  - Unknown syllables are tokenized as separate words.
 * <br>
 *  - All the punctuation is discarded from the produced tokens, including the tsheg that usually follows "ང". 
 * <p>
 * Due to its design, this tokenizer doesn't deal with contextual ambiguities.<br>
 * For example, if both དོན and དོན་གྲུབ exist in the Trie, དོན་གྲུབ will be returned every time the sequence དོན + གྲུབ is found.<br>
 * The sentence སེམས་ཅན་གྱི་དོན་གྲུབ་པར་ཤོག will be tokenized into "སེམས་ཅན + གྱི + དོན་གྲུབ + པར + ཤོག" (སེམས་ཅན + གྱི + དོན + གྲུབ་པར + ཤོག expected).   
 * 
 * Derived from Lucene 6.4.1 analysis.util.CharTokenizer
 * 
 * @author Élie Roux
 * @author Drupchen
 *
 */
public final class TibWordTokenizer extends Tokenizer {
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
	
	private Trie scanner;
	private String compiledTrieName = "src/main/resources/bo-compiled-trie.dump";
	
	private boolean debug = false;
	private boolean lemmatize = true;
		
	/**
	 * Constructs a TibWordTokenizer using a default lexicon file (here "resource/output/total_lexicon.txt") 
	 * @throws FileNotFoundException the file containing the lexicon cannot be found
	 * @throws IOException the file containing the lexicon cannot be read
	 */
	public TibWordTokenizer() throws FileNotFoundException, IOException {
		InputStream stream = null;
		stream = TibWordTokenizer.class.getResourceAsStream("/total_lexicon.txt");
		if (stream == null) {
			// we're not using the jar, there is no resource, assuming we're running the code
			init(new FileReader("resources/output/total_lexicon.txt"));
		} else {
			init(new InputStreamReader(stream));
		}
	}

	/**
	 * Constructs a TibWordTokenizer using a given trie
	 * @param trie  built with BuildCompiledTrie.java
	 */
	public TibWordTokenizer(Trie trie) {
        this.scanner = trie;
        ioBuffer = new RollingCharBuffer();
        ioBuffer.reset(input);
	}
	
	public TibWordTokenizer(String trieFile) throws FileNotFoundException, IOException {
	    System.out.println("\n\tcompiled Trie not found, building it.");
        long start = System.currentTimeMillis();
        this.scanner = BuildCompiledTrie.buildTrie(Arrays.asList(trieFile));
        long end = System.currentTimeMillis();
        System.out.println("\tTime: " + (end - start) / 1000 + "s.");
        ioBuffer = new RollingCharBuffer();
        ioBuffer.reset(input);
	}
	
	/**
     * 
     * @throws FileNotFoundException  the file of the compiled Trie is not found
     * @throws IOException  the file of the compiled Trie can't be opened
     */
    private void init(Reader reader) throws FileNotFoundException, IOException {
        InputStream stream = null;
        stream = TibWordTokenizer.class.getResourceAsStream("/bo-compiled-trie.dump");
        if (stream == null) {  // we're not using the jar, there is no resource, assuming we're running the code
            if (!new File(compiledTrieName).exists()) {
                System.out.println("\n\tcompiled Trie not found, building it.");
                long start = System.currentTimeMillis();
                BuildCompiledTrie.compileTrie();
                long end = System.currentTimeMillis();
                System.out.println("\tTime: " + (end - start) / 1000 + "s.");
            }
            init(new FileInputStream(compiledTrieName));    
        } else {
            init(stream);
        }
    }
    
    /**
     * Opens an existing compiled Trie
     * 
     * @param inputStream the compiled Trie opened as a Stream 
     */
    private void init(InputStream inputStream) throws FileNotFoundException, IOException {
        System.out.println("\n\tLoading the trie");
        long start = System.currentTimeMillis();
        this.scanner = new Trie(new DataInputStream(inputStream));
        long end = System.currentTimeMillis();
        System.out.println("\tTime: " + (end - start) / 1000 + "s.");
        ioBuffer = new RollingCharBuffer();
        ioBuffer.reset(input);
    }
	
	private int bufferIndex = 0, finalOffset = 0;
	private static final int MAX_WORD_LEN = 255;

	private RollingCharBuffer ioBuffer;
	private int tokenLength;
	private int cmdIndex;
	private boolean foundMatch;
	private int foundMatchCmdIndex;
	private Row rootRow;
	private Row currentRow;
	private int tokenStart;
	private int tokenEnd;
	private final int charCount = 1; // the number of chars in a codepoint, always 1 for Tibetan

	private boolean passedFirstSyllable;

	/**
	 * Called on each token character to normalize it before it is added to the
	 * token. The default implementation does nothing. Subclasses may use this to,
	 * e.g., lowercase tokens.
	 * 
	 * @param c the character to normalize
	 * @return the normalized character
	 */
	protected int normalize(final int c) {
		return c;
	}

	@Override
	public final boolean incrementToken() throws IOException {
		clearAttributes();
		ioBuffer.freeBefore(bufferIndex);
		tokenLength = 0;
		tokenStart = -1; // this variable is always initialized
		tokenEnd = -1;
		rootRow = scanner.getRow(scanner.getRoot());
		int confirmedEnd = -1;
		int confirmedEndIndex = -1;
		cmdIndex = -1;
		foundMatchCmdIndex = -1;
		foundMatch = false;
		passedFirstSyllable = false;
		currentRow = null;
		char[] tokenBuffer = termAtt.buffer();
		
		if (debug) {System.out.println("----------------------");}
		
		/* A. FINDING TOKENS */
		while (true) {
			/* A.1. FILLING c WITH CHARS FROM ioBuffer */
			final int c = ioBuffer.get(bufferIndex);	// take next char in ioBuffer
			bufferIndex += charCount;			 		// increment bufferIndex for next value of c
			/* when ioBuffer is empty (end of input, ...) */
			if (c == -1) {
				bufferIndex -= charCount;
				if (tokenLength == 0) {
					finalOffset = correctOffset(bufferIndex);
					return false;
				}
				break;
			}
 
			if (debug) {System.out.println("\t" + (char) c);}
			
			/* A.2. PROCESSING c */
			
			/* A.2.1) if it's a token char */
			if (isTibetanTokenChar(c)) {
				
				checkIfFirstSylPassed(c);
				if (isStartOfToken(c)) {                // start of token
					tryToFindMatchIn(rootRow, c);
					tryToContinueDownTheTrie(rootRow, c);
					incrementTokenIndices();

				} else {
					ifNeededResize(tokenBuffer);
					if (wentToMaxDownTheTrie()) {
						if (!passedFirstSyllable) {
							// we're in a broken state (in the first syllable and no match)
							// we just want to go to the end of the syllable
							if (reachedSylEnd(c)) {
								confirmedEnd = tokenEnd;
								confirmedEndIndex = bufferIndex;
								break;
							}
							tokenEnd += charCount; // else we're just passing
						} else {
							stepBackIfStartedNextSylButCantGoFurther(c);	// the current chars begin an entry in the Trie
							break;
						}
					} else {					// normal case: we are in the middle of a potential token
						if (foundMatch) {
							confirmedEnd = tokenEnd;
							confirmedEndIndex = bufferIndex;
						}
						tokenEnd += charCount;
						tryToFindMatchIn(currentRow, c);
						tryToContinueDownTheTrie(currentRow, c);
					}
				}
				IncrementTokenLengthAndAddCurrentCharTo(tokenBuffer, c);
				if (tokenLength >= MAX_WORD_LEN) {	// tokenBuffer corner case: buffer overflow! 
					break;
				}
			
			/* A.2.2) if it is not a token char */
			} else if (tokenLength > 0) {
				break;
			}
		}
		
		/* B. HANDING THEM TO LUCENE */
		
		/* B.1. IF THERE IS A NON-MAX MATCH */
		if (foundMatch) {
			confirmedEnd = tokenEnd;
			confirmedEndIndex = bufferIndex;
		}
		if (confirmedEnd > 0) {
			bufferIndex = confirmedEndIndex;
			tokenEnd = confirmedEnd;
		}
		
		/* B.2. EXITING incrementToken() WITH THE TOKEN */
		assert(tokenStart != -1);
		finalizeSettingTermAttribute();
		lemmatizeIfRequired();
		return true;
	}

	private void ifNeededResize(char[] tokenBuffer) {
		if (tokenLength >= tokenBuffer.length-1) {				// check if a supplementary could run out of bounds
			tokenBuffer = termAtt.resizeBuffer(2+tokenLength);	// make sure a supplementary fits in the buffer
		}
	}

	private final void stepBackIfStartedNextSylButCantGoFurther(final int c) {
		if (cmdIndex == -1 && currentRow == null && passedFirstSyllable && !reachedSylEnd(c)) {
			bufferIndex -= charCount;
			tokenEnd -= charCount;
		}
	}

	private final void checkIfFirstSylPassed(final int c) {
		if (c == '\u0F0B' && !passedFirstSyllable) {
			passedFirstSyllable = true;
		}
	}

	private final void finalizeSettingTermAttribute() {
		finalOffset = correctOffset(tokenEnd);
		offsetAtt.setOffset(correctOffset(tokenStart), finalOffset);
		termAtt.setLength(tokenEnd - tokenStart);
	}

	private final boolean reachedSylEnd(final int c) {
		return c == '\u0F0B';	// isTibetanTokenChar() filters all punctuation and space, so filtering tsek is enough
	}

	private final boolean wentToMaxDownTheTrie() {
		return currentRow == null;
	}

	private final void lemmatizeIfRequired() {
		if (lemmatize) {
			final String cmd = scanner.getCommandVal(foundMatchCmdIndex);
			if (cmd != null ) {
				applyCmdToTermAtt(cmd);	
			}
		}
	}

	private final void IncrementTokenLengthAndAddCurrentCharTo(final char[] tokenBuffer, final int c) {
		tokenLength += Character.toChars(normalize(c), tokenBuffer, tokenLength);	// add normalized c to tokenBuffer
	}

	private final void incrementTokenIndices() {
		tokenStart = bufferIndex - charCount;
		tokenEnd = tokenStart + charCount;		// tokenEnd is one char ahead of tokenStart (ending index is exclusive)
	}

	private final void tryToContinueDownTheTrie(final Row row, final int c) {
		final int ref = row.getRef((char) c);
		currentRow = (ref >= 0) ? scanner.getRow(ref) : null;
	}

	private final void tryToFindMatchIn(final Row row, final int c) {
		cmdIndex = row.getCmd((char) c);
		foundMatch = (cmdIndex >= 0);	// we may have caught the end, but we must check if next character is a tsheg
		if (foundMatch) {
			foundMatchCmdIndex = cmdIndex; 
		}
	}

	private final boolean isStartOfToken(final int c) {
		return tokenLength == 0;
	}

	private final boolean isTibetanTokenChar(final int c) {
		return isTibLetter(c) || (c == '\u0F0B' && tokenLength > 0);
	}

	/**
	 * Finds whether the given character is a Tibetan letter or not.
	 * @param c a unicode code-point
	 * @return true if {@code c} in the specified range; false otherwise
	 */
	public final boolean isTibLetter(final int c) {
		return ('\u0F40' <= c && c <= '\u0FBC');	// between "Tibetan Letter Ka" and "Tibetan Subjoined Letter Fixed-Form Ra"
	}
	
	private final void applyCmdToTermAtt(final String cmd) {
		if (cmd.charAt(0) == '>') {
			// resize buffer
			final char operation = cmd.charAt(1);
			switch(operation) {
			case 'A':
				termAtt.setLength(termAtt.length() - 1);
				break;
			case 'B':
				termAtt.setLength(termAtt.length() - 2);
				break;
			case 'C':
				termAtt.setLength(termAtt.length() - 3);
				break;
			case 'D':
				// replaces the last character by a འ
				final char[] buffer = termAtt.buffer();
				buffer[termAtt.length()-1] = 'འ';
				break;
			default:
				throw new IllegalArgumentException("the operation should be A, B, C or D.");
			}
			
		} else if (cmd.charAt(0) == '/') {
			// replace content
			termAtt.setEmpty().append(cmd.substring(1, cmd.length()));
		} else {
			
		}

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
		finalOffset = 0;
		ioBuffer.reset(input); // make sure to reset the IO buffer!!
	}

	public final void setLemmatize(final boolean lemmatize) {
		this.lemmatize = lemmatize;
	}
	
	public final void setDebug(final boolean debug) {
		this.debug = debug;
	}
}
