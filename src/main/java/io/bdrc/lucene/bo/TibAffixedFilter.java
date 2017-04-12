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


import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;


/**
 * Removes <tt>འི</tt>, <tt>འོ</tt> and <tt>འིས</tt> characters at end of token for use in the ChunkAnalyzer or WylieAnalyzer.
 * <p>
 * The <tt>འི</tt> in Wylie at the end is a modifer that can be usefully ignored in search and indexing so that "པོ" and "པོའི" will match. This should help
 * searches to be more lenient.
 * <p>
 * Derived from Lucene 4.4.0 analysis.standard.ClassicFilter
 */

public class TibAffixedFilter extends TokenFilter {
	static char AA = 'འ';
	static char GIGU = 'ི';
	static char NARO = 'ོ';
	static char SA = 'ས';
	static char NGA = 'ང';
	static char MA = 'མ';

	public TibAffixedFilter(TokenStream input) {
		super(input);
	}

	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

	/**
	 * Returns the next token in the stream, or null at EOS.
	 * <p>
	 * Removes <tt>འས</tt> from the end of words.
	 * <p>
	 * Removes dots from acronyms.
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
			if (buffer[len - 3] == AA && buffer[len - 2] == GIGU && buffer[len - 1] == SA) {
				termAtt.setLength(len - 3);
				return true;
			}
		}

		// if the token ends with "འི" or "འོ" or "འང" or "འམ" then decrement token length by 2
		if (len > 2) {
			if (buffer[len - 2] == AA && (buffer[len - 1] == GIGU || buffer[len - 1] == NARO
					|| buffer[len - 1] == MA || buffer[len - 1] == NGA)) {
				termAtt.setLength(len - 2);
			}
		}

		return true;
	}
}
