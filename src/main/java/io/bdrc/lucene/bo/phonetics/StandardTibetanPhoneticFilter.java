package io.bdrc.lucene.bo.phonetics;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

public final class StandardTibetanPhoneticFilter extends TokenFilter {

    public StandardTibetanPhoneticFilter(TokenStream input) {
        super(input);
    }

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    
    @Override
    public final boolean incrementToken() throws IOException {
        if (!input.incrementToken())
            return false;
        
        PhoneticSystemStandardTibetan.INSTANCE.getPhonetics(termAtt);
        return true;
    }

}
