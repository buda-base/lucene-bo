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

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * Removes <em>འི</em>, <em>འོ</em>, <em>འིའོ</em>, <em>འམ</em>, <em>འང</em> and
 * <em>འིས</em> characters at end of token.
 * <p>
 * The <em>འི</em> is an affixed particle that can be usefully ignored in search
 * and indexing so that "པོ" and "པོའི" will match. This should help searches to
 * be more lenient.
 * </p>
 * <p>
 * Derived from Lucene 6.4.1 analysis.standard.ClassicFilter
 * </p>
 * 
 * @author Chris Tomlinson
 * @author Hélios Hildt
 */
public final class TibAffixedFilter extends TokenFilter {

    boolean convertOldTib = false;
    public TibAffixedFilter(TokenStream input, boolean convertOldTib) {
        super(input);
        this.convertOldTib = convertOldTib;
    }

    /**
     * Decides whether the syllable had a final འ before being affixed.
     * 
     * @param p
     *            the prefix
     * @param m
     *            the main stack
     * @return true if this syllable configuration requires an final འ to be legal.
     */
    final private static boolean needsAASuffix(char p, char m) {
        switch (p) {
        case 'ག':
            switch (m) {
            case 'ཅ':
            case 'ཉ':
            case 'ཏ':
            case 'ད':
            case 'ན':
            case 'ཙ':
            case 'ཞ':
            case 'ཟ':
            case 'ཡ':
            case 'ཤ':
            case 'ས':
                return true;
            default:
                return false;
            }
        case 'ད':
            switch (m) {
            case 'ཀ':
            case 'ག':
            case 'ང':
            case 'པ':
            case 'བ':
            case 'མ':
                return true;
            default:
                return false;
            }
        case 'བ':
            switch (m) {
            case 'ཀ':
            case 'ག':
            case 'ཅ':
            case 'ཏ':
            case 'ད':
            case 'ཙ':
            case 'ཞ':
            case 'ཟ':
            case 'ཤ':
            case 'ས':
                return true;
            default:
                return false;
            }
        case 'མ':
            switch (m) {
            case 'ཁ':
            case 'ག':
            case 'ང':
            case 'ཆ':
            case 'ཇ':
            case 'ཉ':
            case 'ཐ':
            case 'ད':
            case 'ན':
            case 'ཚ':
            case 'ཛ':
                return true;
            default:
                return false;
            }
        case 'འ':
            switch (m) {
            case 'ཁ':
            case 'ག':
            case 'ཆ':
            case 'ཇ':
            case 'ཐ':
            case 'ད':
            case 'ཕ':
            case 'བ':
            case 'ཚ':
            case 'ཛ':
                return true;
            default:
                return false;
            }
        default:
            return false;
        }
    }

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    /**
     * Returns the next token in the stream, or null at EOS.
     * <p>
     * Removes <em>འི</em>, <em>འོ</em>, <em>འིའོ</em>, <em>འམ</em>, <em>འང</em> and
     * <em>འིས</em> from the end of words.
     * </p>
     */
    @Override
    public final boolean incrementToken() throws java.io.IOException {
        if (!input.incrementToken()) {
            return false;
        }

        final char[] buffer = termAtt.buffer();
        final int len = termAtt.length();

        // if the token ends with "འིའོ", "འིའམ", "འིའང", "འོའམ", "འོའང" then decrement token length by 4
        if (len > 4) {
            if (buffer[len - 4] == '\u0F60'
                    && ((buffer[len - 3] == '\u0F72' && buffer[len - 2] == '\u0F60' && (buffer[len - 1] == '\u0F7C' || buffer[len - 1] == '\u0F44' || buffer[len - 1] == '\u0F58')) ||
                            (buffer[len - 3] == '\u0F7C' && buffer[len - 2] == '\u0F60' && (buffer[len - 1] == '\u0F44' || buffer[len - 1] == '\u0F58')))) {
                // if the host syllable had a འ before the particle was affixed, do not remove
                // it.
                if (len == 6 && needsAASuffix(buffer[len - 6], buffer[len - 5])) {
                    termAtt.setLength(len - 3);
                } else {
                    termAtt.setLength(len - 4);
                }
                return true;
            }
        }

        // if the token ends with "འིས" then decrement token length by 3
        if (len > 3) {
            if (buffer[len - 3] == '\u0F60' && buffer[len - 2] == '\u0F72' && buffer[len - 1] == '\u0F66') {
                // if the host syllable had a འ before the particle was affixed, do not remove
                // it.
                if (len == 5 && needsAASuffix(buffer[len - 5], buffer[len - 4])) {
                    termAtt.setLength(len - 2);
                } else {
                    termAtt.setLength(len - 3);
                }
                return true;
            }
        }

        // if the token ends with "འི" or "འོ" or "འམ" or "འང" then decrement token
        // length by 2
        if (len > 2) {
            if (buffer[len - 2] == '\u0F60' && (buffer[len - 1] == '\u0F72' || buffer[len - 1] == '\u0F7C'
                    || buffer[len - 1] == '\u0F58' || buffer[len - 1] == '\u0F44')) {
                // if the host syllable had a འ before the particle was affixed, do not remove
                // it.
                if (len == 4 && needsAASuffix(buffer[len - 4], buffer[len - 3])) {
                    termAtt.setLength(len - 1);
                } else {
                    termAtt.setLength(len - 2);
                }
            }
        }

        // if the token ends with "འུར" or "འུས" then decrement token length by 1
        if (len > 4) { // the trick here is that we don't want the token to start with འུ so we test for a length of 4 instead of 3 
            if (buffer[len - 3] == '\u0F60' && buffer[len - 2] == '\u0F74' && (buffer[len - 1] == '\u0F62' || buffer[len - 1] == '\u0F66')) {
                termAtt.setLength(len - 1);
            }
        }

        // if the token ends with "འ" and it doesn't need to, we shorten it by 1
        if ((len > 3 || len == 2) && buffer[len - 1] == '\u0F60') {
            termAtt.setLength(len - 1);
        } else if (len == 3 && buffer[len - 1] == '\u0F60' && !needsAASuffix(buffer[len - 3], buffer[len - 2])) {
            termAtt.setLength(len - 1);
        }
        
        // next rules are Old Tibetan only:
        if (!this.convertOldTib)
            return true;
        
        // Dadrag. Handled here instead of a regex as it's less costly and (almost) makes sense
        // [^གམ][ནལར])ད
        if (len >= 3 && buffer[len - 1] == '\u0F51' && (buffer[len - 2] == '\u0F53' || buffer[len - 2] == '\u0F62' || buffer[len - 2] == '\u0F63') && buffer[len - 3] != '\u0F42' && buffer[len - 3] != '\u0F58') {
            termAtt.setLength(len - 1);
        }
        
        // Medial འ. From Tibetan-nlp: In Old Tibetan we can find the presence of a medial འ (lost in Classical Tibetan) within syllables as xxའས་ / xxའད་ / xxའར་.
        if (len >= 3 && buffer[len - 2] == '\u0F60' && (buffer[len - 1] == '\u0F51' || buffer[len - 1] == '\u0F62' || buffer[len - 1] == '\u0F66')) {
            buffer[len-2] = buffer[len-1];
            termAtt.setLength(len - 1);
        }
        
        return true;
    }
}
