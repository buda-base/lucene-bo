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


import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.Tokenizer;

/**
 * An Analyzer that uses {@link TibSyllableTokenizer} and filters with StopFilter
 * 
 * Derived from Lucene 6.4.1 analysis.core.WhitespaceAnalyzer.java
 **/
public final class TibetanAnalyzer extends Analyzer {
	static final List<String> tibStopWords = Arrays.asList(
			"ཏུ", 
			"གི", "ཀྱི", 
			"གིས", "ཀྱིས", "ཡིས", 
			"ཀྱང", 
			"སྟེ", "ཏེ", 
			"མམ", "རམ", "སམ", "ཏམ", 
			"ནོ", "བོ", "ཏོ", 
			"གིན", "ཀྱིན", "གྱིན", "ཡིན", 
			"ཅིང", "ཅིག", 
//			"ཅེ་ན", "ཞེ་ན", // will be useful when on the word-level
//			"ཅེའོ", "ཞེའོ", "ཤེའོ", // not useful after TibEndingFilter 
			"ཅེས", "ཞེས"
			);
	static final CharArraySet tibStopSet = StopFilter.makeStopSet(tibStopWords);

	/**
	 * Creates a new {@link TibetanAnalyzer}
	 */
	public TibetanAnalyzer() {
	}
  
	@Override
	protected TokenStreamComponents createComponents(final String fieldName) {
		Tokenizer source = new TibSyllableTokenizer();
		TokenFilter filter1 = new TibAffixedFilter(source);
		StopFilter filter2 = new StopFilter(filter1, tibStopSet);
		return new TokenStreamComponents(source, filter2);
	}
}
