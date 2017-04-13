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


import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;


/**
 * Removes <tt>འི</tt>, <tt>འོ</tt> and <tt>འིས</tt> characters at end of token.
 * <p>
 * The <tt>འི</tt> is an affixed particle that can be usefully ignored in search and indexing so that "པོ" and "པོའི" will match. This should help
 * searches to be more lenient.
 * <p>
 * Derived from Lucene 6.4.1 analysis.standard.ClassicFilter
 */

public class TibAffixedFilter extends TokenFilter {

	public TibAffixedFilter(TokenStream input) {
		super(input);
	}

	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

	/**
	 * Returns the next token in the stream, or null at EOS.
	 * <p>
	 * Removes <tt>འི</tt>, <tt>འོ</tt>, <tt>འམ</tt>, <tt>འང</tt> and <tt>འིས</tt> from the end of words.
	 * <p>
	 */
	@Override
	public final boolean incrementToken() throws java.io.IOException {
		if (!input.incrementToken()) {
			return false;
		}

		final char[] buffer = termAtt.buffer();
		final int len = termAtt.length();

		// if the token ends with "འིས" then decrement token length by 3
		if (len > 3) {
			if (buffer[len - 3] == '\u0F60' && buffer[len - 2] == '\u0F72' && buffer[len - 1] == '\u0F66') {
				termAtt.setLength(len - 3);
				return true;
			}
		}

		// if the token ends with "འི" or "འོ" or "འམ" or "འང" then decrement token length by 2
		if (len > 2) {
			if (buffer[len - 2] == '\u0F60' && (buffer[len - 1] == '\u0F72' || buffer[len - 1] == '\u0F7C'  
					|| buffer[len - 1] == '\u0F58' || buffer[len - 1] == '\u0F44')) {
				termAtt.setLength(len - 2);
			}
		}

		return true;
	}
}
