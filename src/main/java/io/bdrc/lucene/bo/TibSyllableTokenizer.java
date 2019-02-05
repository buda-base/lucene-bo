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

import org.apache.lucene.analysis.util.CharTokenizer;

import io.bdrc.lucene.sixtofour.Dummy;

/**
 * A TibSyllableTokenizer divides text between sequences of Tibetan Letter
 * and/or Digit characters and sequences of all other characters - typically
 * some sort of white space but other punctuation and characters from other
 * language code-pages are not considered as constituents of tokens for the
 * purpose of search and indexing.
 * <p>
 * Adjacent sequences of Tibetan Letter and/or Digit characters form tokens.
 * </p>
 * <p>
 * Derived from Lucene 6.4.1 analysis.core.WhitespaceTokenizer.java
 * </p>
 * 
 * @author Chris Tomlinson
 * @author Hélios Hildt
 * 
 */
public final class TibSyllableTokenizer extends CharTokenizer {

    /**
     * Construct a new TibSyllableTokenizer.
     */
    public TibSyllableTokenizer() {
        super(Dummy.READER);
    }

    // see http://jrgraphix.net/r/Unicode/0F00-0FFF
    protected boolean isTibLetterOrDigit(int c) {
        return ('\u0F40' <= c && c <= '\u0FBC') || ('\u0F20' <= c && c <= '\u0F33') || (c == '\u0F00');
    }

    /**
     * Collects only characters which satisfy isTibetanLetterOrDigit()
     */
    @Override
    protected boolean isTokenChar(int c) {
        return isTibLetterOrDigit(c);
    }
}
