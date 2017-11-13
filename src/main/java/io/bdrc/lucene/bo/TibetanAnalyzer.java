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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.core.StopFilter;
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
	String lexiconFileName = null;
	
	/**
	 * Creates a new {@link TibetanAnalyzer}
	 * 
	 * @param  segmentInWords  if the segmentation is on words instead of syllables
	 * @param  lemmatize  if the analyzer should remove affixed particles, and normalize words in words mode
	 * @param  filterChars  if the text should be converted to NFD (necessary for texts containing NFC strings)
	 * @param  fromEwts  if the text should be converted from EWTS
	 * @param  stopFileName  file containing all the stopwords
	 * @param  lexiconFileName  file name of the lexicon file to be used for word segmentation (null for the default one)
	 */
	public TibetanAnalyzer(boolean segmentInWords, boolean lemmatize, boolean filterChars, boolean fromEwts, String stopFilename, String lexiconFileName) throws IOException {
		this.segmentInWords = segmentInWords;
		this.lemmatize = lemmatize;
		this.filterChars = filterChars;
		this.fromEwts = fromEwts;
		if (stopFilename != null ) {
		    InputStream stream = null;
	        stream = TibetanAnalyzer.class.getResourceAsStream("/bo-stopwords.txt");
	        if (stream == null) {      // we're not using the jar, these is no resource, assuming we're running the code
	            this.tibStopSet = StopFilter.makeStopSet(getWordList(new FileInputStream(stopFilename), "#"));
	        } else {
	            this.tibStopSet = StopFilter.makeStopSet(getWordList(stream, "#"));
	        }
		} else {
			this.tibStopSet = null;
		}
		this.lexiconFileName = lexiconFileName;
	}
    
    /**
     * Creates a new {@link TibetanAnalyzer}
     * 
     * @param  segmentInWords  if the segmentation is on words instead of syllables
     * @param  lemmatize  if the analyzer should remove affixed particles, and normalize words in words mode
     * @param  filterChars  if the text should be converted to NFD (necessary for texts containing NFC strings)
     * @param  fromEwts  if the text should be converted from EWTS
     */
    public TibetanAnalyzer(boolean segmentInWords, boolean lemmatize, boolean filterChars, boolean fromEwts) {
        this.segmentInWords = segmentInWords;
        this.lemmatize = lemmatize;
        this.filterChars = filterChars;
        this.fromEwts = fromEwts;
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
	 * @throws IOException  if the file containing stopwords can't be opened
	 */
	public TibetanAnalyzer() throws IOException {
		this(true, true, true, false, "src/main/resources/bo-stopwords.txt", "resources/output/total_lexicon.txt");
	}
  
    /**
     * @param reader Reader containing the list of stopwords
     * @param comment The string representing a comment.
     * @return result the {@link ArrayList} to fill with the reader's words
     */
    public static ArrayList<String> getWordList(InputStream inputStream, String comment) throws IOException {
        ArrayList<String> result = new ArrayList<String>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(inputStream));
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
