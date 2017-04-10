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


import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WordlistLoader;

/**
 * Filters {@link TibetanTokenizer} with {@link ClassicFilter}, {@link
 * LowerCaseFilter} and {@link StopFilter}, using a list of
 * English stop words.
 * 
 * ClassicAnalyzer was named StandardAnalyzer in Lucene versions prior to 3.1. 
 * As of 3.1, {@link StandardAnalyzer} implements Unicode text segmentation,
 * as specified by UAX#29.
 */
public final class TibetanAnalyzer extends StopwordAnalyzerBase {

  /** Default maximum allowed token length */
  public static final int DEFAULT_MAX_TOKEN_LENGTH = 255;

  private int maxTokenLength = DEFAULT_MAX_TOKEN_LENGTH;

  /** An unmodifiable set containing some common English words that are usually not
  useful for searching. */
  
  /** default code
   * 		public static final CharArraySet STOP_WORDS_SET = StopAnalyzer.ENGLISH_STOP_WORDS_SET;
   * replaced by the following lines from StandardAnalyzer (StopAnalyzer imports from there)
   */
  public static final CharArraySet STOP_WORDS_SET;
  
  static {
    final List<String> stopWords = Arrays.asList(
	  "gi", "kyi", "gyi", "yi", "gis", "kyis", "gyis", "yis",
	  "su", "ru", "ra", "du", "na", "la", "tu",
	  "go", "ngo", "do", "no", "po", "mo", "ro", "lo", "so", "to",
	  "dang"
    );
    final CharArraySet stopSet = new CharArraySet(stopWords, false);
    STOP_WORDS_SET = CharArraySet.unmodifiableSet(stopSet); 
  }

  /** Builds an analyzer with the given stop words.
   * @param stopWords stop words */
  public TibetanAnalyzer(CharArraySet stopWords) {
    super(stopWords);
  }

  /** Builds an analyzer with the default stop words ({@link
   * #STOP_WORDS_SET}).
   */
  public TibetanAnalyzer() {
    this(STOP_WORDS_SET);
  }

  /** Builds an analyzer with the stop words from the given reader.
   * @see WordlistLoader#getWordSet(Reader)
   * @param stopwords Reader to read stop words from */
  public TibetanAnalyzer(Reader stopwords) throws IOException {
    this(loadStopwordSet(stopwords));
  }

  /**
   * Set maximum allowed token length.  If a token is seen
   * that exceeds this length then it is discarded.  This
   * setting only takes effect the next time tokenStream or
   * tokenStream is called.
   */
  public void setMaxTokenLength(int length) {
    maxTokenLength = length;
  }
    
  /**
   * @see #setMaxTokenLength
   */
  public int getMaxTokenLength() {
    return maxTokenLength;
  }

  @Override
  protected TokenStreamComponents createComponents(final String fieldName) {
    final TibetanTokenizer src = new TibetanTokenizer();
    src.setMaxTokenLength(maxTokenLength);
    TokenStream tok = new TibEndingFilter(src);
    tok = new StopFilter(tok, stopwords);
    return new TokenStreamComponents(src, tok) {
      @Override
      protected void setReader(final Reader reader) {
        src.setMaxTokenLength(TibetanAnalyzer.this.maxTokenLength);
        super.setReader(reader);
      }
    };
  }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    return new LowerCaseFilter(in);
  }
}
