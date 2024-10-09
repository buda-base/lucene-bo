package io.bdrc.lucene.bo.phonetics;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

public class StandardTibetanPhoneticFilter  extends TokenFilter{

    protected StandardTibetanPhoneticFilter(TokenStream input) {
        super(input);
    }

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    
    @Override
    public boolean incrementToken() throws IOException {
        if (!input.incrementToken()) {
            return false;
        }
        
        final char[] buffer = termAtt.buffer();
        final int len = termAtt.length();
        
        
        
        return false;
    }

}
