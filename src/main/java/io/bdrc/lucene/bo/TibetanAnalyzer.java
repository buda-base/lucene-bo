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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
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
	
	/**
	 * Creates a new {@link TibetanAnalyzer}
	 */
	public TibetanAnalyzer() {
	}
  
	@Override
	protected Reader initReader(String fieldName, Reader reader) {
		TibCharFilter charFilter = new TibCharFilter(reader);
		return super.initReader(fieldName, charFilter);
	}
	
	@Override
	protected TokenStreamComponents createComponents(final String fieldName) {
		Tokenizer source = null;
		try {
			source = new TibWordTokenizer();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		TokenFilter filter1 = new TibAffixedFilter(source);
		StopFilter filter2 = new StopFilter(filter1, tibStopSet);
		return new TokenStreamComponents(source, filter2);
	}
}
