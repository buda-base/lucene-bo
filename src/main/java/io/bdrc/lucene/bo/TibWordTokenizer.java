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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.lucene.analysis.CharacterUtils;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.CharacterUtils.CharacterBuffer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import io.bdrc.lucene.stemmer.Optimizer;
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
 * Derived from Lucene 6.4.1 analysis.
 * 
 * @author Élie Roux
 * @author Drupchen
 *
 */
public final class TibWordTokenizer extends Tokenizer {
	private Trie scanner;

	// this tokenizer generates three attributes:
	// term offset, positionIncrement and type
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
	private boolean lemmatize = true;
	private boolean debug = false;
	/**
	 * Constructs a TibWordTokenizer using the file designed by filename
	 * @param filename the path to the lexicon file
	 * @throws FileNotFoundException the file containing the lexicon cannot be found
	 * @throws IOException the file containing the lexicon cannot be read
	 */
	public TibWordTokenizer(String filename) throws FileNotFoundException, IOException {
		init(filename);
	}

	/**
	 * Constructs a TibWordTokenizer using the file designed by filename
	 * @param filename the path to the lexicon file
	 * @throws FileNotFoundException the file containing the lexicon cannot be found
	 * @throws IOException the file containing the lexicon cannot be read
	 */
	public TibWordTokenizer(boolean debug, String filename) throws FileNotFoundException, IOException {
		this.debug = debug;
		init(filename);
	}
	
	/**
	 * Constructs a TibWordTokenizer using a default lexicon file (here "resource/output/total_lexicon.txt") 
	 * @throws FileNotFoundException the file containing the lexicon cannot be found
	 * @throws IOException the file containing the lexicon cannot be read
	 */
	public TibWordTokenizer() throws FileNotFoundException, IOException {
		init("resource/output/total_lexicon.txt");
	}

	/**
	 * Initializes and populates {@see #scanner} 
	 * 
	 * The format of each line in filename must be as follows: inflected-form + space + lemma
	 * @param filename the file containing the entries to be added
	 * @throws FileNotFoundException 
	 * @throws IOException
	 */
	private void init(String filename) throws FileNotFoundException, IOException {
		this.scanner = new Trie(true);

		//		currently only adds the entries without any diff
		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
			String line;
			while ((line = br.readLine()) != null) {
				int spaceIndex = line.indexOf(' ');
				if (spaceIndex == -1) {
					throw new IllegalArgumentException("The dictionary file is corrupted in the following line.\n" + line);
				} else {
					this.scanner.add(line.substring(0, spaceIndex), line.substring(spaceIndex+1));
				}
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

	private int tokenLength;

	private int cmdIndex;

	private boolean foundMatch;

	private int foundMatchCmdIndex;

	private boolean foundNonMaxMatch;

	private Row rootRow;

	private Row currentRow;

	private int tokenStart;

	private int tokenEnd;

	private int charCount;

	private boolean passedFirstSyllable;

	/**
	 * Called on each token character to normalize it before it is added to the
	 * token. The default implementation does nothing. Subclasses may use this to,
	 * e.g., lowercase tokens.
	 * 
	 * @param c the character to normalize
	 * @return the normalized character
	 */
	protected int normalize(int c) {
		return c;
	}

	@Override
	public final boolean incrementToken() throws IOException {
		clearAttributes();
		tokenLength = 0;
		tokenStart = -1; // this variable is always initialized
		tokenEnd = -1;
		rootRow = scanner.getRow(scanner.getRoot());
		int confirmedEnd = -1;
		int confirmedEndIndex = -1;
		cmdIndex = -1;
		foundMatchCmdIndex = -1;
		foundNonMaxMatch = false;
		foundMatch = false;
		passedFirstSyllable = false;
		currentRow = null;
		char[] tokenBuffer = termAtt.buffer();
		
		if (debug) {System.out.println("----------------------");}
		
		/* A. FINDING TOKENS */
		while (true) {
			/*>>> Deals with the beginning and end of the input string >>>>>>>>>*/
			if (bufferIndex >= dataLen) {
				offset += dataLen;
				CharacterUtils.fill(ioBuffer, input);		// read supplementary char aware with CharacterUtils
				if (ioBuffer.getLength() == 0) {
					dataLen = 0;							// so next offset += dataLen won't decrement offset
					if (tokenLength > 0) {
						break;
					} else {
						finalOffset = correctOffset(offset);
						return false;
					}
				}
				dataLen = ioBuffer.getLength();
				bufferIndex = 0;
			}
			/*<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<*/
			
			/* A.1. FILLING c WITH CHARS FROM ioBuffer */
			
			/* (use CharacterUtils here to support < 3.1 UTF-16 code unit behavior if the char based methods are gone) */
			final int c = Character.codePointAt(ioBuffer.getBuffer(), bufferIndex, ioBuffer.getLength());	// take next char in ioBuffer
			charCount = Character.charCount(c);
			bufferIndex += charCount;			 			// increment bufferIndex for next value of c
			
			if (debug) {System.out.println(bufferIndex-1 + "\t" + (char) c);}
			
			/* A.2. PROCESSING c */
			
			/* A.2.1) if it's a token char */
			if (isTibetanTokenChar(c)) {
				
				checkIfFirstSylPassed(c);
				if (isStartOfToken(c)) {                // start of token
					assert(tokenStart == -1); // TODO : necessary ???
					tryToFindMatchIn(rootRow, c);
					tryToContinueDownTheTrie(rootRow, c);
					incrementTokenIndices();
//					ifIsNeededAttributeStartingIndexOfNonword();

				} else {
					
					/*>>> corner case for ioBuffer >>>>>>>*/
					if (tokenLength >= tokenBuffer.length-1) { // check if a supplementary could run out of bounds
						tokenBuffer = termAtt.resizeBuffer(2+tokenLength); // make sure a supplementary fits in the buffer
					}
					/*<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<*/
					
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
							break;
						}
					} else {
						if (reachedSylEnd(c) && foundMatch && !foundNonMaxMatch) {
							foundNonMaxMatch = true;
							confirmedEnd = tokenEnd;
							confirmedEndIndex = bufferIndex;
						}
						tokenEnd += charCount;
						tryToFindMatchIn(currentRow, c);
						tryToContinueDownTheTrie(currentRow, c);
					}
				}
				IncrementTokenLengthAndAddCurrentCharTo(tokenBuffer, c);
				/*>>>>>> ioBuffer corner case: buffer overflow! >>>*/
				if (tokenLength >= MAX_WORD_LEN) {		// make sure to check for >= surrogate pair could break == test
					break;
				}
				/*<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<*/
			
			/* A.2.2) if it is not a token char */
			} else if (tokenLength > 0) {           // at non-Letter w/ chars
				break;                           // return 'em
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

	private void checkIfFirstSylPassed(int c) {
		if (c == '\u0F0B' && !passedFirstSyllable) {
			passedFirstSyllable = true;
		}
	}

	private void finalizeSettingTermAttribute() {
		finalOffset = correctOffset(tokenEnd);
		offsetAtt.setOffset(correctOffset(tokenStart), finalOffset);
		termAtt.setLength(tokenEnd - tokenStart);
	}

	private boolean reachedSylEnd(int c) {
		return c == '\u0F0B';	// isTibetanTokenChar() filters all punctuation and space, so filtering tsek is enough
	}

	private boolean wentToMaxDownTheTrie() {
		return currentRow == null;
	}

	private void lemmatizeIfRequired() {
		if (lemmatize) {
			String cmd = scanner.getCommandVal(foundMatchCmdIndex);
			if (cmd != null ) {
				applyCmdToTermAtt(cmd);	
			}
		}
	}

	private void IncrementTokenLengthAndAddCurrentCharTo(char[] tokenBuffer, int c) {
		tokenLength += Character.toChars(normalize(c), tokenBuffer, tokenLength);	// add normalized c to tokenBuffer
	}

	private void incrementTokenIndices() {
		tokenStart = offset + bufferIndex - charCount;
		tokenEnd = tokenStart + charCount;		// tokenEnd is one char ahead of tokenStart (ending index is exclusive)
	}

	private void tryToContinueDownTheTrie(Row row, int c) {
		int ref = row.getRef((char) c);
		currentRow = (ref >= 0) ? scanner.getRow(ref) : null;
	}

	private void tryToFindMatchIn(Row row, int c) {
		cmdIndex = row.getCmd((char) c);
		foundMatch = (cmdIndex >= 0);	// we may have caught the end, but we must check if next character is a tsheg
		if (foundMatch) {
			foundMatchCmdIndex = cmdIndex;
//			foundNonMaxMatch = storeNonMaxMatchState(); TODO 
		}
	}

	final private boolean isStartOfToken(int c) {
		return tokenLength == 0;
	}

	final private boolean isTibetanTokenChar(int c) {
		return isTibLetter(c) || (c == '\u0F0B' && tokenLength > 0);
	}

	/**
	 * Finds whether the given character is a Tibetan letter or not.
	 * @param c a unicode code-point
	 * @return true if {@code c} in the specified range; false otherwise
	 */
	public boolean isTibLetter(int c) {
		return ('\u0F40' <= c && c <= '\u0FBC');	// between "Tibetan Letter Ka" and "Tibetan Subjoined Letter Fixed-Form Ra"
	}
	
	private void applyCmdToTermAtt(String cmd) {
		if (cmd.charAt(0) == '>') {
			// resize buffer
			char operation = cmd.charAt(1);
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
				char[] buffer = termAtt.buffer();
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
		offset = 0;
		dataLen = 0;
		finalOffset = 0;
		ioBuffer.reset(); // make sure to reset the IO buffer!!
	}



	public void setLemmatize(boolean lemmatize) {
		this.lemmatize = lemmatize;
	}
}
