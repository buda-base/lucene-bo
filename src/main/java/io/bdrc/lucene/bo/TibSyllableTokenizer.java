/*******************************************************************************
 * Copyright (c) 2017 Tibetan Buddhist Resource Center (TBRC)
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


import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.util.CharTokenizer;
import org.apache.lucene.util.AttributeFactory;

/**
 * A tokenizer that divides Tibetan text into syllables
 */
public final class TibSyllableTokenizer extends CharTokenizer {
  
	/**
	 * Construct a new TibSyllableTokenizer.
	 */
	public TibSyllableTokenizer() {
	}
	
	// see http://jrgraphix.net/r/Unicode/0F00-0FFF
	protected boolean isTibLetterOrDigit(int c) {
		return ('\u0F40' <= c && c <= '\u0FBC') || ('\u0F20' <= c && c <= '\u0F33') || (c == '\u0F00');
	}
  
	/**
	 * Construct a new WhitespaceTokenizer using a given
	 * {@link org.apache.lucene.util.AttributeFactory}.
	 *
	 * @param factory
	 *          the attribute factory to use for this {@link Tokenizer}
	 */
	public TibSyllableTokenizer(AttributeFactory factory) {
		super(factory);
	}
  
	/** Collects only characters which do not satisfy
	 * {@link Character#isWhitespace(int)}.*/
	@Override
	protected boolean isTokenChar(int c) {
		return isTibLetterOrDigit(c);
	}
}
