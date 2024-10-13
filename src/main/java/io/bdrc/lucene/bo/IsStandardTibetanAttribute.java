package io.bdrc.lucene.bo;

import org.apache.lucene.util.Attribute;

public interface IsStandardTibetanAttribute extends Attribute {
    
    /*
     * Just a boolean attribute set to true if the syllable is following
     * spelling rules of Standard Tibetan. Set by the tokenizer.
     */

}
