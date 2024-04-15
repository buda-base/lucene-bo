package io.bdrc.lucene.bo;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.synonym.SolrSynonymParser;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.Test;

public class SynonymFormatTest {

    static TokenStream tokenize(Reader reader, Tokenizer tokenizer) throws IOException {
        tokenizer.close();
        tokenizer.end();
        tokenizer.setReader(reader);
        tokenizer.reset();
        return tokenizer;
    }
    
    static void assertTokenStream(TokenStream tokenStream, List<String> expected) {
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
    
    @Test
    public void formatTest() throws IOException, ParseException {
        boolean lenient = true;
        String inputMethod = "unicode";
        Analyzer a = new TibetanAnalyzer(false, lenient ? "" : "paba-affix", lenient ? "l-ot" : "min", inputMethod, null, null);
        //Reader r = new java.io.StringReader("ཀོུབ ༠");
        //r = new TibCharFilter(r, lenient, lenient);
        //r = TibPattFilter.plugFilters(r);
        //TokenStream ts = tokenize(r, new TibSyllableTokenizer());
        //ts = new TibAffixedFilter(ts, lenient);
        //ts = new PaBaFilter(ts);
        //assertTokenStream(ts, Arrays.asList("ཀོུབ", "༠"));
        SolrSynonymParser ssp = new SolrSynonymParser(true, true, a);
        ssp.parse(new InputStreamReader(CommonHelpers.getResourceOrFile("synonyms.txt")));
    }
    
}
