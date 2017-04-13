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


import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.Tokenizer;

/**
 * An Analyzer that uses {@link WhitespaceTokenizer}.
 **/
public final class TibetanAnalyzer extends Analyzer {
	static final List<String> tibStopWords = Arrays.asList(
			"གི", "ཀྱི", "གྱི", "ཡི",
			"གིས", "ཀྱིས", "གྱིས", "ཡིས", "ན",
			"སུ", "ར", "རུ", "དུ", "ལ", "ཏུ",
			"གོ", "ངོ", "དོ", "ནོ", "པོ",
			"མོ", "རོ", "ལོ", "སོ", "ཏོ",
			"དང"
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
