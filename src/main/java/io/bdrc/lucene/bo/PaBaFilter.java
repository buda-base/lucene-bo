package io.bdrc.lucene.bo;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * Transforms <em>བ</em> and <em>བོ</em> into <em>པ</em> and <em>པོ</em>.
 * 
 * The filter is designed to be plugged after the TibAffixedFilter and will not
 * try to normalize affixed versions.
 * 
 * @author Elie Roux
 */
public class PaBaFilter extends TokenFilter {

    public static final char[] paArray = new char[1];
    public static final char[] poArray = new char[2];
    static {
        paArray[0] = 'པ';
        poArray[0] = 'པ';
        poArray[1] = '\u0F7C';
    }

    public PaBaFilter(final TokenStream input) {
        super(input);
    }

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    @Override
    public final boolean incrementToken() throws java.io.IOException {
        if (!input.incrementToken()) {
            return false;
        }

        final char[] buffer = termAtt.buffer();
        final int len = termAtt.length();
        if (len == 1 && buffer[0] == 'བ') {
            termAtt.copyBuffer(paArray, 0, 1);
        } else if (len == 2 && buffer[0] == 'བ' && buffer[1] == '\u0F7C') {
            termAtt.copyBuffer(poArray, 0, 2);
        }
        return true;
    }
}
