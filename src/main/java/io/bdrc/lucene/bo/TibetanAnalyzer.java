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
import java.io.Reader;
import java.util.ArrayList;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.util.IOUtils;

/**
 * An Analyzer that uses {@link TibSyllableTokenizer} and filters with StopFilter
 * 
 * Derived from Lucene 6.4.1 analysis.core.WhitespaceAnalyzer.java
 * 
 * @author Chris Tomlinson
 * @author Hélios Hildt
 **/
public final class TibetanAnalyzer extends Analyzer {
	
	CharArraySet tibStopSet;
	boolean segmentInWords = false; 
	boolean lemmatize = false;
	boolean filterChars = false;
	boolean fromEwts = false;
	
	/**
	 * Creates a new {@link TibetanAnalyzer}
	 * 
	 * @param  segmentInWords  if the segmentation is on words instead of syllables
	 * @param  lemmatize  if the analyzer should remove affixed particles, and normalize words in words mode
	 * @param  filterChars  if the text should be converted to NFD (necessary for texts containing NFC strings)
	 * @param  fromEwts  if the text should be converted from EWTS to Unicode
	 * @throws IOException  if the file containing stopwords can't be opened 
	 */
	public TibetanAnalyzer(boolean segmentInWords, boolean lemmatize, boolean filterChars, boolean fromEwts, String stopFilename) throws IOException {
		this.segmentInWords = segmentInWords;
		this.lemmatize = lemmatize;
		this.filterChars = filterChars;
		this.fromEwts = fromEwts;
		if (stopFilename != null) {
			this.tibStopSet = StopFilter.makeStopSet(getWordList(stopFilename, "#"));
		} else {
			this.tibStopSet = null;
		}
	}
	
	/**
	 * Creates a new {@link TibetanAnalyzer} with the default values
	 * @throws IOException  if the file containing stopwords can't be opened
	 */
	public TibetanAnalyzer() throws IOException {
		this(true, true, true, false, "src/main/resources/tib-stopwords.txt");
	}
  
	/**
	 * @param reader Reader containing the list of stopwords
	 * @param comment The string representing a comment.
	 * @return result the {@link ArrayList} to fill with the reader's words
	 */
	public static ArrayList<String> getWordList(String filename, String comment) throws IOException {
		ArrayList<String> result = new ArrayList<String>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(filename));
			String word = null;
			while ((word = br.readLine()) != null) {
				word = word.replace("\t", "");
				if (word.contains(comment)) {
					if (!word.startsWith(comment)) {
						word = word.substring(0, word.indexOf(comment));
						word = word.trim();
						if (!word.isEmpty()) result.add(word);
					}
				} else {
					word = word.trim();
					if (!word.isEmpty()) result.add(word);
				}
			}
		}
		finally {
			IOUtils.close(br);
		}
		return result;
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
