package io.bdrc.lucene.bo.phonetics;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import io.bdrc.lucene.bo.IsStandardTibetanAttribute;

public final class StandardTibetanPhoneticFilter extends TokenFilter {

    public StandardTibetanPhoneticFilter(final TokenStream input) {
        super(input);
    }

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final IsStandardTibetanAttribute istAtt = addAttribute(IsStandardTibetanAttribute.class);
    
    @Override
    public final boolean incrementToken() throws IOException {
        if (!input.incrementToken())
            return false;
        
        if (istAtt.getIsStandardTibetan())
            PhoneticSystemStandardTibetan.INSTANCE.getPhonetics(termAtt);
        else
            PhoneticSystemStandardTibetan.INSTANCE.getSktPhonetics(termAtt);
        return true;
    }

}
