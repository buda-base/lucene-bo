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
import org.apache.lucene.analysis.charfilter.NormalizeCharMap;

/**
 * An Analyzer that uses {@link TibSyllableTokenizer} and filters with StopFilter
 * 
 * Derived from Lucene 6.4.1 analysis.core.WhitespaceAnalyzer.java
 * 
 * @author Chris Tomlinson
 * @author Hélios Hildt
 **/
public final class TibetanAnalyzer extends Analyzer {
	
	// non-ambiguous particles
	static final List<String> tibStopWords = Arrays.asList(
			"ཏུ", 
			"གི", "ཀྱི", 
			"གིས", "ཀྱིས", "ཡིས", 
			"ཀྱང", 
			"སྟེ", "ཏེ", 
			"མམ", "རམ", "སམ", "ཏམ", 
			"ནོ", "བོ", "ཏོ", 
			"གིན", "ཀྱིན", "གྱིན", 
			"ཅིང", "ཅིག", 
			"ཅེས", "ཞེས"
			);
	static final CharArraySet tibStopSet = StopFilter.makeStopSet(tibStopWords);

	public final static NormalizeCharMap getTibNormalizeCharMap() {
		NormalizeCharMap.Builder builder  = new NormalizeCharMap.Builder();	
		// The non-breaking tsheg is replaced by the normal one
		builder.add("\u0f0C", "\u0F0B");
		// Characters to delete: the markers found under selected syllables
		builder.add("\u0F35", ""); //  ༵
		builder.add("\u0F37", ""); //  ༷
		// Characters to decompose
		builder.add("\u0F00", "\u0F68\u0F7C\u0F7E"); //  ༀ 
		builder.add("\u0F02", "\u0F60\u0F70\u0F82"); // ༂
		builder.add("\u0F03", "\u0F60\u0F70\u0F14"); //  ༃
		builder.add("\u0F43", "\u0F42\u0FB7"); //  གྷ
		builder.add("\u0F48", "\u0F47\u0FB7"); //  ཈
		builder.add("\u0F4D", "\u0F4C\u0FB7"); //  ཌྷ
		builder.add("\u0F52", "\u0F51\u0FB7"); //  དྷ
		builder.add("\u0F57", "\u0F56\u0FB7"); //  བྷ
		builder.add("\u0F5C", "\u0F5B\u0FB7"); //  ཛྷ
		builder.add("\u0F69", "\u0F40\u0FB5"); //  ཀྵ
		builder.add("\u0F73", "\u0F71\u0F72"); //    ཱི
		builder.add("\u0F75", "\u0F71\u0F74"); //   ཱུ
		builder.add("\u0F76", "\u0FB2\u0F80"); //   ྲྀ
		builder.add("\u0F77", "\u0FB2\u0F71\u0F80"); //   ཷ
		builder.add("\u0F78", "\u0FB3\u0F80"); //   ླྀ
		builder.add("\u0F79", "\u0FB3\u0F71\u0F80"); //   ཹ
		builder.add("\u0F81", "\u0F71\u0F80"); //     ཱྀ
		builder.add("\u0F93", "\u0F92\u0FB7"); //  ྒྷ
		builder.add("\u0F9D", "\u0F9C\u0FB7"); //  ྜྷ
		builder.add("\u0FA2", "\u0FA1\u0FB7"); //  ྡྷ
		builder.add("\u0FA7", "\u0FA6\u0FB7"); //  ྦྷ
		builder.add("\u0FAC", "\u0FAB\u0FB7"); //  ྫྷ
		builder.add("\u0FB9", "\u0F90\u0FB5"); //  ྐྵ
		return builder.build();
	}
	
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
