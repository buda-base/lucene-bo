package io.bdrc.lucene.bo;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.charfilter.MappingCharFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.junit.Assert;
import org.junit.Test;

import io.bdrc.lucene.bo.phonetics.EnglishPhoneticCharMapFilter;
import io.bdrc.lucene.bo.phonetics.EnglishPhoneticFilter;
import io.bdrc.lucene.bo.phonetics.EnglishPhoneticRegexFilter;
import io.bdrc.lucene.bo.phonetics.EnglishPhoneticTokenizer;
import io.bdrc.lucene.bo.phonetics.LowerCaseCharFilter;
import io.bdrc.lucene.bo.phonetics.StandardTibetanPhoneticFilter;

import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.pattern.PatternReplaceCharFilter;

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
        testPhoneticMapping("dzongsar", "ToNsar");
        testPhoneticMapping("tenzin", "denTin");
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
    
    public List<List<String>> getQueryTokens(final String input) throws IOException {
        final List<List<String>> tokens = new ArrayList<>();
        Reader reader = new StringReader(input);
        reader = new LowerCaseCharFilter(reader);
        reader = new EnglishPhoneticCharMapFilter(reader);
        reader = EnglishPhoneticRegexFilter.plugFilters(reader);
        TokenStream tokenStream = tokenize(reader, new EnglishPhoneticTokenizer());
        CharTermAttribute charTermAttr = tokenStream.addAttribute(CharTermAttribute.class);
        PositionIncrementAttribute posIncrAttr = tokenStream.addAttribute(PositionIncrementAttribute.class);
        //tokenStream.reset();
        List<String> lastPosition = new ArrayList<>();
        while (tokenStream.incrementToken()) {
            if (posIncrAttr.getPositionIncrement() > 0) {
                lastPosition = new ArrayList<>();
                tokens.add(lastPosition);
            }
            lastPosition.add(charTermAttr.toString());
        }
        tokenStream.end();
        reader.close();
        return tokens;
    }
    
    public List<List<String>> getIndexTokens(final String input) throws IOException {
        final List<List<String>> tokens = new ArrayList<>();
        Reader reader = new StringReader(input);
        reader = new TibEwtsFilter(reader);
        reader = new TibCharFilter(reader, true, true);
        reader = TibPattFilter.plugFilters(reader);
        TokenStream tokenStream = tokenize(reader, new TibSyllableTokenizer());
        tokenStream = new EnglishPhoneticFilter(tokenStream);
        CharTermAttribute charTermAttr = tokenStream.addAttribute(CharTermAttribute.class);
        PositionIncrementAttribute posIncrAttr = tokenStream.addAttribute(PositionIncrementAttribute.class);
        //tokenStream.reset();
        List<String> lastPosition = new ArrayList<>();
        while (tokenStream.incrementToken()) {
            if (posIncrAttr.getPositionIncrement() > 0) {
                lastPosition = new ArrayList<>();
                tokens.add(lastPosition);
            }
            lastPosition.add(charTermAttr.toString());
        }
        tokenStream.end();
        reader.close();
        return tokens;
    }
    
    // Function to check if the token streams from both analyzers match
    public void checkMatch(final String queryInput, final String indexInput) throws IOException {
        final List<List<String>> queryTokens = getQueryTokens(queryInput);
        final List<List<String>> indexTokens = getIndexTokens(indexInput);
     // Ensure both lists have the same number of positions
        Assert.assertEquals(String.format("The number of positions in query and index tokens are not the same for %s -> %s, %s %s", queryInput, indexInput, queryTokens, indexTokens), 
                            queryTokens.size(), indexTokens.size());

        // Iterate through each position
        for (int position = 0; position < queryTokens.size(); position++) {
            List<String> queryPositionTokens = queryTokens.get(position);
            List<String> indexPositionTokens = indexTokens.get(position);

            // Find any common token at the current position
            boolean foundMatch = queryPositionTokens.stream()
                    .anyMatch(indexPositionTokens::contains);

            // If no common token is found, assert failure with detailed output
            Assert.assertTrue(String.format(
                "No matching token found at position %d for %s -> %s. Query tokens: %s, Index tokens: %s",
                position, queryInput, indexInput, queryPositionTokens, indexPositionTokens), foundMatch);
        }
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
        checkMatch("Tenzin Gyatso", "bstan 'dzin rgya mtsho");
        checkMatch("Tenzin Gyamtso", "bstan 'dzin rgya mtsho");
        checkMatch("Panchen Lama", "paN chen bla ma");
        checkMatch("Paṇchen Lama", "paN chen bla ma");
        checkMatch("Phurpa Netik", "phur pa gnad tig");
        checkMatch("Jamyang Khyentse Wangpo", "'jam dbyangs mkhyen brtse'i dbang po");
        checkMatch("Marpa Lotsawa", "mar pa lo tsA ba");
        checkMatch("Marpa Lotsawa", "mar pa lotsA ba");
        checkMatch("Tsokar Gyaltsen", "mtsho skar rgyal mtshan");
        checkMatch("Tsokar Gyeltsen", "mtsho skar rgyal mtshan");
        checkMatch("Samding Dorje Phagmo", "bsam sding rdo rje phag mo");
        checkMatch("Orgyen", "o rgyan");
        checkMatch("Khandro Nyingtik", "mkha' 'gro snying thig");
        checkMatch("vajra", "ba dz+ra");
        checkMatch("Sakya Pandita", "sa skya paN+Di ta");
        checkMatch("Gyalwang Drukpa", "rgyal dbang 'brug pa");
        checkMatch("Gyalwa Gyamtso", "rgyal ba rgya mtsho");
        checkMatch("Ladakh", "la dwags");
        checkMatch("Trinley", "'phrin les");
        checkMatch("Wanggyal", "dbang rgyal");
        checkMatch("Wangyal", "dbang rgyal");
        checkMatch("Rangjung Kunkhyab", "rang byung kun khyab");
        checkMatch("Rinchen Terdzö", "rin chen gter mdzod");
        checkMatch("Lhatsün Jangchub Ö", "lha btsun byang chub 'od");
        checkMatch("Lotsawa", "lotsa ba");
        checkMatch("Katog", "kaHthog");
        checkMatch("kunga", "kun dga'");
        checkMatch("Drupgyü Nyima", "sgrub brgyud nyi ma");
        checkMatch("Karma", "kar+ma");
        checkMatch("Denkarma", "ldan dkar ma");
        checkMatch("Trisong Detsen", "khri srong lde btsan");
        checkMatch("Trisong Detsen", "khri srong lde'u btsan");
        // checkMatch("Wangdud", "dbang bdud"); // d suffixes not supported in the phonetic
        checkMatch("Choegyal", "chos rgyal");
        checkMatch("Mip'am", "mi pham");
        checkMatch("Mipham", "mi pham");
        checkMatch("Mipam", "mi pham");
        checkMatch("Mingyur", "mi 'gyur");
        checkMatch("Karma Pakshi", "kar+ma pak+shi");
        checkMatch("Ratna Lingpa", "rat+na gling pa");
        checkMatch("Bande Kawa Paltsek", "ban+de ska ba dpal brtsegs");
        checkMatch("Paltsek Rakshita", "dpal brtsegs rak+Shi ta");
        checkMatch("Shri Singha", "shrI sing+ha");
        //checkMatch("Acarya", "A tsar+yA");
        checkMatch("Lopön", "slob dpon");
        checkMatch("Zopa", "bzo pa");
        checkMatch("Guru", "gu ru");
        checkMatch("Zhalu", "zhwa lu");
        // checkMatch("Sangdo Palri", "zangs mdog dpal ri"); // not indicating g suffix not supported (yet?)
        checkMatch("Chagya Chenpo", "phyag rgya chen po");
        checkMatch("Chakchen Gauma", "phyag chen ga'u ma");
        checkMatch("Samye Gompa", "bsam yas dgon pa");
        checkMatch("Lochen Dharmashri", "lo chen d+harma shrI");
        checkMatch("Mengagde", "man ngag sde");
        checkMatch("Senyig", "gsan yig");
        checkMatch("Minyak", "mi nyag");
        checkMatch("Gomnyam Drugpa", "sgom nyams drug pa");
    }
    
}
