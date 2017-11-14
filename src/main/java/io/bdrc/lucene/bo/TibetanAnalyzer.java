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
import org.apache.lucene.analysis.TokenStream;
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
	boolean segmentInWords = false; 
	boolean lemmatize = false;
	boolean filterChars = false;
	boolean fromEwts = false;
	String lexiconFileName = null;
    
    /**
     * Creates a new {@link TibetanAnalyzer} with default lexicon
     * 
     * @param  segmentInWords  if the segmentation is on words instead of syllables
     * @param  lemmatize  if the analyzer should remove affixed particles, and normalize words in words mode
     * @param  filterChars  if the text should be converted to NFD (necessary for texts containing NFC strings)
     * @param  fromEwts  if the text should be converted from EWTS
     */
    public TibetanAnalyzer(boolean segmentInWords, boolean lemmatize, boolean filterChars, boolean fromEwts) {
        this(segmentInWords, lemmatize, filterChars, fromEwts, null);
    }
    
    /**
     * Creates a new {@link TibetanAnalyzer}
     * 
     * @param  segmentInWords  if the segmentation is on words instead of syllables
     * @param  lemmatize  if the analyzer should remove affixed particles, and normalize words in words mode
     * @param  filterChars  if the text should be converted to NFD (necessary for texts containing NFC strings)
     * @param  fromEwts  if the text should be converted from EWTS
     * @param  lexiconFileName  file name of the lexicon file to be used for word segmentation (null for the default one)
     */
    public TibetanAnalyzer(boolean segmentInWords, boolean lemmatize, boolean filterChars, boolean fromEwts, String lexiconFileName) {
        this.segmentInWords = segmentInWords;
        this.lemmatize = lemmatize;
        this.filterChars = filterChars;
        this.fromEwts = fromEwts;
        this.lexiconFileName = lexiconFileName;
    }
	
	/**
	 * Creates a new {@link TibetanAnalyzer} with the default values
	 */
	public TibetanAnalyzer() {
		this(true, true, true, false, null);
	}
  
	@Override
	protected Reader initReader(String fieldName, Reader reader) {
		if (this.fromEwts) {
			reader = new TibEwtsFilter(reader);
		} else if (filterChars) { // filterChars is never needed after ewts translation
			reader = new TibCharFilter(reader);
		}
		return super.initReader(fieldName, reader);
	}
	
	@Override
	protected TokenStreamComponents createComponents(final String fieldName) {
		Tokenizer source = null;
		TokenStream filter = null;
		
		if (segmentInWords) {
			try {
				if (this.lexiconFileName != null)
					source = new TibWordTokenizer(this.lexiconFileName);
				else
					source = new TibWordTokenizer();
				if (lemmatize) {
					((TibWordTokenizer) source).setLemmatize(lemmatize);
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			filter = new StopFilter(source, tibStopSet);
		
		} else {
			source = new TibSyllableTokenizer();
			if (lemmatize) {
				filter = (TibAffixedFilter) new TibAffixedFilter(source);
			}
			filter = new StopFilter(filter, tibStopSet);
		}		
		
		return new TokenStreamComponents(source, filter);
	}
}
