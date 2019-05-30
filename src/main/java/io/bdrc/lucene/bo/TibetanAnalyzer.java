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
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.lucene.sixtofour.CharArraySet;
import io.bdrc.lucene.sixtofour.StopFilter;

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
    boolean lemmatize = false;
    boolean filterChars = false;
    String lexiconFileName = null;
    String inputMethod = INPUT_METHOD_DEFAULT;

    /**
     * Creates a new {@link TibetanAnalyzer}
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
     * @param lexiconFileName
     *            lexicon used to populate the Trie
     * @throws IOException
     *             if the file containing stopwords can't be opened
     */
    public TibetanAnalyzer(boolean segmentInWords, boolean lemmatize, boolean filterChars, String inputMethod,
            String stopFilename, String lexiconFileName) throws IOException {
        this.segmentInWords = segmentInWords;
        this.lemmatize = lemmatize;
        this.filterChars = filterChars;
        this.inputMethod = inputMethod;
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
     * Creates a new {@link TibetanAnalyzer}s
     * This constructor is required by eXist (3.4.x and 4.x, may be higher versions as well)
     *
     * @param version  Apache Lucene version that eXist employs
     *
     * @param segmentInWords
     *            if the segmentation is on words instead of syllables
     * @param lemmatize
     *            if the analyzer should remove affixed particles, and normalize
     *            words in words mode
     * @param filterChars
     *            if the text should be converted to NFD (necessary for texts
     *            containing NFC strings)
     * @throws IOException
     *             if the file containing stopwords can't be opened
     */
    public TibetanAnalyzer(org.apache.lucene.util.Version version,
                           Boolean segmentInWords,
                           Boolean lemmatize,
                           Boolean filterChars) throws IOException {
        this(segmentInWords, lemmatize, filterChars, INPUT_METHOD_DEFAULT,null);
        CommonHelpers.logger.info("eXist -> new TibetanAnalyzer"
            + "( version: " + version
            + ", segmentInWords: " + segmentInWords
            + ", lemmatize: " + lemmatize
            + ", filterChars: " + filterChars
            + " )");
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
            reader = new TibCharFilter(reader);
            break;
        }
        return super.initReader(fieldName, reader);
    }

    @Override
    protected TokenStreamComponents createComponents(final String fieldName, Reader reader) {
        Tokenizer source = null;
        TokenFilter filter = null;

        if (segmentInWords) {
            try {
                if (lexiconFileName != null) {
                    source = new TibWordTokenizer(lexiconFileName);
                } else {
                    source = new TibWordTokenizer();
                }
                ((TibWordTokenizer) source).setLemmatize(lemmatize);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            source = new TibSyllableTokenizer();
            if (lemmatize) {
                filter = new TibAffixedFilter(source);
            }
        }
        if (tibStopSet != null) {
            if (filter != null) {
                filter = new StopFilter(filter, tibStopSet);
            } else {
                filter = new StopFilter(source, tibStopSet);
            }
        }

        // This is required for Lucene versions prior to 5.0.0
        // as Lucene 5 (and later) Analyzer.tokenStream() *always* invokes components.setReader(r)
        // no matter if it is a reused or a new components object,
        // whereas Lucene up to 4.10.4 invokes it only on reused components object.
        if (org.apache.lucene.util.Version.LATEST.major < 5) {
            try {
                source.setReader(reader);
            } catch (IOException e) {
                throw new RuntimeException("Unexpected: ", e);
            }
        }

        if (filter != null) {
            return new TokenStreamComponents(source, filter);
        } else {
            return new TokenStreamComponents(source);
        }
    }
}
