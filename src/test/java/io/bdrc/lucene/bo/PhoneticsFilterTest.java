package io.bdrc.lucene.bo;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.charfilter.MappingCharFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.Test;

import io.bdrc.lucene.bo.phonetics.EnglishPhoneticCharMapFilter;
import io.bdrc.lucene.bo.phonetics.EnglishPhoneticFilter;
import io.bdrc.lucene.bo.phonetics.EnglishPhoneticRegexFilter;
import io.bdrc.lucene.bo.phonetics.EnglishPhoneticTokenizer;
import io.bdrc.lucene.bo.phonetics.LowerCaseCharFilter;
import io.bdrc.lucene.bo.phonetics.StandardTibetanPhoneticFilter;

import org.apache.lucene.analysis.core.WhitespaceTokenizer;

public class PhoneticsFilterTest {

    private static String applyCharMapFilter(String input) throws IOException {
        MappingCharFilter filter = new EnglishPhoneticCharMapFilter(new StringReader(input));
        StringBuilder result = new StringBuilder();
        char[] buffer = new char[1024];
        int length;
        
        // Read and process the input through the filter
        while ((length = filter.read(buffer)) != -1) {
            result.append(buffer, 0, length);
        }
        filter.close();
        return result.toString();
    }
    
    public static void testPhoneticMapping(final String input, final String expected) throws IOException {
            String actualOutput = applyCharMapFilter(input);
            assertEquals("fail on "+input, expected, actualOutput);
    }
    
    @Test
    public void testCharMapFilter() throws IOException {
        testPhoneticMapping("dzongsar", "tsongsar");
        testPhoneticMapping("tenzin", "dentsin");
        testPhoneticMapping("lobzang", "lo saN");
        testPhoneticMapping("lopzang", "lo saN");
    }
    
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
    
    static TokenStream tokenize(Reader reader, Tokenizer tokenizer) throws IOException {
        tokenizer.close();
        tokenizer.end();
        tokenizer.setReader(reader);
        tokenizer.reset();
        return tokenizer;
    }
    
    public List<String> getQueryTokens(final String input) throws IOException {
        final List<String> tokens = new ArrayList<>();
        Reader reader = new StringReader(input);
        reader = new LowerCaseCharFilter(reader);
        reader = new EnglishPhoneticCharMapFilter(reader);
        reader = EnglishPhoneticRegexFilter.plugFilters(reader);
        TokenStream tokenStream = tokenize(reader, new EnglishPhoneticTokenizer());
        CharTermAttribute charTermAttr = tokenStream.addAttribute(CharTermAttribute.class);
        //tokenStream.reset();
        while (tokenStream.incrementToken()) {
            tokens.add(charTermAttr.toString());
        }
        tokenStream.end();
        reader.close();
        return tokens;
    }
    
    public List<String> getIndexTokens(final String input) throws IOException {
        final List<String> tokens = new ArrayList<>();
        Reader reader = new StringReader(input);
        reader = new TibEwtsFilter(reader);
        reader = new TibCharFilter(reader, true, true);
        reader = TibPattFilter.plugFilters(reader);
        TokenStream tokenStream = tokenize(reader, new TibSyllableTokenizer());
        tokenStream = new EnglishPhoneticFilter(tokenStream);
        CharTermAttribute charTermAttr = tokenStream.addAttribute(CharTermAttribute.class);
        //tokenStream.reset();
        while (tokenStream.incrementToken()) {
            tokens.add(charTermAttr.toString());
        }
        tokenStream.end();
        reader.close();
        return tokens;
    }
    
    // Function to check if the token streams from both analyzers match
    public void checkMatch(String queryInput, String indexInput) throws IOException {
        List<String> queryTokens = getQueryTokens(queryInput);
        List<String> indexTokens = getIndexTokens(indexInput);
        assertEquals("Token streams do not match! Query input: " + queryInput + ", Index input: " + indexInput, queryTokens, indexTokens);
    }
    
    @Test
    public void integratedPhoneticTest() throws IOException {
        checkMatch("Dalailama", "tA la'i bla ma");
        checkMatch("Dalaï Lama", "tA la'i bla ma");
        checkMatch("Kangyur", "bka' 'gyur");
        checkMatch("Kanjur", "bka' 'gyur");
        checkMatch("Ösel", "'od gsal");
        checkMatch("Wösel", "'od gsal");
        checkMatch("Selwè", "gsal ba'i");
        checkMatch("Padma Jungné", "pad+ma 'byung gnas");
        checkMatch("Péma Jungné", "pad+ma 'byung gnas");
        
    }
    
}
