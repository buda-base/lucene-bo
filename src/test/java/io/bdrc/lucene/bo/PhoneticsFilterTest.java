package io.bdrc.lucene.bo;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.Test;

import io.bdrc.lucene.bo.phonetics.StandardTibetanPhoneticFilter;

import org.apache.lucene.analysis.core.WhitespaceTokenizer;

public class PhoneticsFilterTest {

    static void assertTokenStream(final TokenStream tokenStream, final List<String> expected) {
        try {
            List<String> termList = new ArrayList<String>();
            CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            while (tokenStream.incrementToken()) {
                termList.add(charTermAttribute.toString());
            }
            System.out.println(String.join(" ", termList));
            assertThat(termList, is(expected));
        } catch (IOException e) {
            assertTrue(false);
        }
    }
    
    static TokenStream stringToTokenStream(final String s) throws IOException {
        final WhitespaceTokenizer tokenizer = new WhitespaceTokenizer();
        tokenizer.setReader(new StringReader(s));
        
        // Create the filter and pass the tokenizer
        final TokenStream tokenStream = new StandardTibetanPhoneticFilter(tokenizer);
        tokenStream.reset();
        return tokenStream;
    }
    
    @Test
    public void testStandardTibetanSimple() throws IOException {
        assertTokenStream(stringToTokenStream("གཤན བཤན རྟེན བསྟན ཐེན"), Arrays.asList("Sen", "Sen", "ten", "ten", "ten"));
    }
    
}
