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
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An Analyzer that uses {@link TibSyllableTokenizer} and filters with
 * StopFilter
 * 
 * Derived from Lucene 6.4.1 analysis.core.WhitespaceAnalyzer.java
 * 
 * @author Chris Tomlinson
 * @author Hélios Hildt
 **/
public final class TibetanAnalyzer extends Analyzer {

    static public final String INPUT_METHOD_UNICODE = "unicode";
    static public final String INPUT_METHOD_DTS = "dts";
    static public final String INPUT_METHOD_EWTS = "ewts";
    static public final String INPUT_METHOD_ALALC = "alalc";
    static public final String INPUT_METHOD_DEFAULT = INPUT_METHOD_UNICODE;

    static final Logger logger = LoggerFactory.getLogger(TibetanAnalyzer.class);

    CharArraySet tibStopSet;
    boolean segmentInWords = false;
    String lemmatize = null;
    boolean convertOldTib = false;
    boolean normalizeMin = false;
    boolean lemmatizeAffixes = false;
    boolean lemmatizePaba = false;
    boolean lemmatizeVerbs = false;
    boolean lemmatizeLemma = false;
    boolean lenient = false;
    String normalize = null;
    String lexiconFileName = null;
    String inputMethod = INPUT_METHOD_DEFAULT;

    // compatibility layer for < 1.5.0
    public TibetanAnalyzer(boolean segmentInWords, boolean lemmatize, boolean normalize, String inputMethod,
            String stopFilename, String lexiconFileName) throws IOException {
            this(segmentInWords, segmentInWords ? "lemmas" : "affix-paba", "min", inputMethod, stopFilename, lexiconFileName);
    }
    
    /**
     * Creates a new {@link TibetanAnalyzer}
     * 
     * @param segmentInWords
     *            if the segmentation is on words instead of syllables
     * @param lemmatize
     *            if the analyzer should remove affixed particles, and normalize
     *            words in words mode
     * @param normalize
     *            if the text should be converted to NFD (necessary for texts
     *            containing NFC strings)
     * @param inputMethod
     *            if the text should be converted from EWTS to Unicode
     * @param stopFilename
     *            a file name with a stop word list
     * @param lexiconFileName
     *            lexicon used to populate the Trie
     * @throws IOException
     *             if the file containing stopwords can't be opened
     */
    public TibetanAnalyzer(boolean segmentInWords, String lemmatize, String normalize, String inputMethod,
            String stopFilename, String lexiconFileName) throws IOException {
        this.segmentInWords = segmentInWords;
        this.lemmatize = lemmatize;
        this.normalize = normalize;
        this.inputMethod = inputMethod;
        if (this.normalize.contains("ot")) {
            this.convertOldTib = true;
            this.normalizeMin = true;
        }
        if (this.normalize.contains("l")) {
            this.lenient = true;
            this.normalizeMin = true;
        }
        this.lemmatizeLemma = this.lemmatize.contains("lemmas");
        this.lemmatizeVerbs = this.lemmatize.contains("verbs");
        this.lemmatizePaba = this.lemmatize.contains("paba");
        this.lemmatizeAffixes = this.lemmatize.contains("affix");
        if (stopFilename != null) {
            if (stopFilename.isEmpty()) {
                InputStream stream = null;
                stream = CommonHelpers.getResourceOrFile("bo-stopwords.txt");
                if (stream == null) {
                    final String msg = "The default compiled Trie is not found. Either rebuild the Jar or run BuildCompiledTrie.main()"
                            + "\n\tAborting...";
                    logger.error(msg);
                    this.tibStopSet = null;
                } else {
                    this.tibStopSet = StopFilter.makeStopSet(getWordList(stream, "#"));
                }
            } else {
                this.tibStopSet = StopFilter.makeStopSet(getWordList(new FileInputStream(stopFilename), "#"));
            }
        } else {
            this.tibStopSet = null;
        }
        this.lexiconFileName = lexiconFileName;
    }

    /**
     * Creates a new {@link TibetanAnalyzer}s
     * 
     * @param segmentInWords
     *            if the segmentation is on words instead of syllables
     * @param lemmatize
     *            if the analyzer should remove affixed particles, and normalize
     *            words in words mode
     * @param filterChars
     *            if the text should be converted to NFD (necessary for texts
     *            containing NFC strings)
     * @param inputMethod
     *            if the text should be converted from EWTS to Unicode
     * @param stopFilename
     *            a file name with a stop word list
     * @throws IOException
     *             if the file containing stopwords can't be opened
     */
    public TibetanAnalyzer(boolean segmentInWords, boolean lemmatize, boolean filterChars, String inputMethod,
            String stopFilename) throws IOException {
        this(segmentInWords, lemmatize, filterChars, inputMethod, stopFilename, null);
    }

    /**
     * Creates a new {@link TibetanAnalyzer} with the default values
     * 
     * @throws IOException
     *             if the file containing stopwords can't be opened
     */
    public TibetanAnalyzer() throws IOException {
        this(true, true, true, INPUT_METHOD_DEFAULT, "src/main/resources/bo-stopwords.txt",
                "resources/output/total_lexicon.txt");
    }

    /**
     * @param inputStream
     *            stream to the list of stopwords
     * @param comment
     *            The string representing a comment
     * @throws IOException
     *             if the file containing stopwords can't be opened
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
                        if (!word.isEmpty())
                            result.add(word);
                    }
                } else {
                    word = word.trim();
                    if (!word.isEmpty())
                        result.add(word);
                }
            }
        } finally {
            IOUtils.close(br);
        }
        return result;
    }

    @Override
    protected Reader initReader(String fieldName, Reader reader) {
        switch (this.inputMethod) {
        case INPUT_METHOD_EWTS:
        case INPUT_METHOD_DTS:
        case INPUT_METHOD_ALALC:
            reader = new TibEwtsFilter(reader, this.inputMethod);
            break;
        case INPUT_METHOD_UNICODE:
        default:
            break;
        }
        if (this.normalizeMin)
            reader = new TibCharFilter(reader, this.lenient, this.convertOldTib);
        if (this.convertOldTib)
            reader = TibPattFilter.plugFilters(reader);
        return super.initReader(fieldName, reader);
    }

    @Override
    protected TokenStreamComponents createComponents(final String fieldName) {
        Tokenizer source = null;
        TokenFilter filter = null;

        if (segmentInWords) {
            try {
                if (lexiconFileName != null) {
                    source = new TibWordTokenizer(lexiconFileName);
                } else {
                    source = new TibWordTokenizer();
                }
                ((TibWordTokenizer) source).setLemmatize(this.lemmatizeLemma);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            source = new TibSyllableTokenizer();
            if (this.lemmatizeAffixes)
                filter = new TibAffixedFilter(source, this.convertOldTib);
            if (this.lemmatizeVerbs)
                filter = new TibSyllableLemmatizer(filter == null ? source : filter);
            if (this.lemmatizePaba)
                filter = new PaBaFilter(filter == null ? source : filter);
        }
        if (tibStopSet != null) {
            if (filter != null) {
                filter = new StopFilter(filter, tibStopSet);
            } else {
                filter = new StopFilter(source, tibStopSet);
            }
        }
        if (filter != null) {
            return new TokenStreamComponents(source, filter);
        } else {
            return new TokenStreamComponents(source);
        }
    }
}