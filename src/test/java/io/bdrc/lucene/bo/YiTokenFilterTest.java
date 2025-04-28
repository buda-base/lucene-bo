package io.bdrc.lucene.bo;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.junit.Test;

public class YiTokenFilterTest {

    // Custom analyzer that uses our filter
    private static class TibetanAnalyzer extends Analyzer {
        @Override
        protected TokenStreamComponents createComponents(String fieldName) {
            Tokenizer tokenizer = new WhitespaceTokenizer();
            TokenStream filter = new YiTokenFilter(tokenizer);
            return new TokenStreamComponents(tokenizer, filter);
            //return new TokenStreamComponents(tokenizer);
        }
    }

    // Helper class to capture token information
    private static class TokenInfo {
        String term;
        int posIncrement;

        TokenInfo(String term, int posIncrement) {
            this.term = term;
            this.posIncrement = posIncrement;
        }

        @Override
        public String toString() {
            return String.format("term=%s, posIncrement=%d", term, posIncrement);
        }
    }

    // Helper method to analyze text and return token information
    private List<TokenInfo> analyzeText(Analyzer analyzer, String text) throws IOException {
        List<TokenInfo> result = new ArrayList<>();
        TokenStream stream = analyzer.tokenStream("dummy", new StringReader(text));
        
        CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);
        PositionIncrementAttribute posIncrAtt = stream.addAttribute(PositionIncrementAttribute.class);
        
        stream.reset();
        while (stream.incrementToken()) {
            result.add(new TokenInfo(termAtt.toString(), posIncrAtt.getPositionIncrement()));
            System.out.println(String.format("%s (%d)", termAtt.toString(), posIncrAtt.getPositionIncrement()));
        }
        stream.close();
        
        return result;
    }

    @Test
    public void testBasicFiltering() throws IOException {
        try (Analyzer analyzer = new TibetanAnalyzer()) {
            // Test basic filtering with no special characters
            List<TokenInfo> tokens = analyzeText(analyzer, "token1 token2 token3");
            assertEquals(3, tokens.size());
            assertEquals("token1", tokens.get(0).term);
            assertEquals(1, tokens.get(0).posIncrement);
            assertEquals("token2", tokens.get(1).term);
            assertEquals(1, tokens.get(1).posIncrement);
            assertEquals("token3", tokens.get(2).term);
            assertEquals(1, tokens.get(2).posIncrement);
        }
    }

    @Test
    public void testTargetCharWithException() throws IOException {
        try (Analyzer analyzer = new TibetanAnalyzer()) {
            // Test target character followed by exception character
            List<TokenInfo> tokens = analyzeText(analyzer, "word1 ཡི ག word2");
            assertEquals(4, tokens.size());
            assertEquals("word1", tokens.get(0).term);
            assertEquals(1, tokens.get(0).posIncrement);
            assertEquals("ཡི", tokens.get(1).term);
            assertEquals(1, tokens.get(1).posIncrement); // Should have normal posIncrement when followed by exception
            assertEquals("ག", tokens.get(2).term);
            assertEquals(1, tokens.get(2).posIncrement);
            assertEquals("word2", tokens.get(3).term);
            assertEquals(1, tokens.get(3).posIncrement);
        }
    }

    @Test
    public void testTargetCharWithoutException() throws IOException {
        try (Analyzer analyzer = new TibetanAnalyzer()) {
            // Test target character not followed by exception character
            List<TokenInfo> tokens = analyzeText(analyzer, "word1 ཡི normal word2");
            assertEquals(4, tokens.size());
            assertEquals("word1", tokens.get(0).term);
            assertEquals(1, tokens.get(0).posIncrement);
            assertEquals("ཡི", tokens.get(1).term);
            assertEquals(0, tokens.get(1).posIncrement); // Should have posIncrement=0 when not followed by exception
            assertEquals("normal", tokens.get(2).term);
            assertEquals(1, tokens.get(2).posIncrement);
            assertEquals("word2", tokens.get(3).term);
            assertEquals(1, tokens.get(3).posIncrement);
        }
    }

    @Test
    public void testTargetCharAsFirstToken() throws IOException {
        try (Analyzer analyzer = new TibetanAnalyzer()) {
            // Test target character as the first token
            List<TokenInfo> tokens = analyzeText(analyzer, "ཡི word1 word2");
            assertEquals(3, tokens.size());
            assertEquals("ཡི", tokens.get(0).term);
            assertEquals(1, tokens.get(0).posIncrement); // Should always have normal posIncrement when first token
            assertEquals("word1", tokens.get(1).term);
            assertEquals(1, tokens.get(1).posIncrement);
            assertEquals("word2", tokens.get(2).term);
            assertEquals(1, tokens.get(2).posIncrement);
        }
    }

    @Test
    public void testMultipleTargetChars() throws IOException {
        try (Analyzer analyzer = new TibetanAnalyzer()) {
            // Test multiple target characters in sequence
            List<TokenInfo> tokens = analyzeText(analyzer, "word1 ཡི ཡི word2");
            assertEquals(4, tokens.size());
            assertEquals("word1", tokens.get(0).term);
            assertEquals(1, tokens.get(0).posIncrement);
            assertEquals("ཡི", tokens.get(1).term);
            assertEquals(0, tokens.get(1).posIncrement); // First ཡི should be ignored
            assertEquals("ཡི", tokens.get(2).term);
            assertEquals(1, tokens.get(2).posIncrement); // Second ཡི doesn't make a lot of sense... don't skip it
            assertEquals("word2", tokens.get(3).term);
            assertEquals(1, tokens.get(3).posIncrement);
        }
    }

    @Test
    public void testOtherExceptionChar() throws IOException {
        try (Analyzer analyzer = new TibetanAnalyzer()) {
            // Test with the other exception character "དམ"
            List<TokenInfo> tokens = analyzeText(analyzer, "word1 ཡི དམ word2");
            assertEquals(4, tokens.size());
            assertEquals("word1", tokens.get(0).term);
            assertEquals(1, tokens.get(0).posIncrement);
            assertEquals("ཡི", tokens.get(1).term);
            assertEquals(1, tokens.get(1).posIncrement); // Should have normal posIncrement
            assertEquals("དམ", tokens.get(2).term);
            assertEquals(1, tokens.get(2).posIncrement);
            assertEquals("word2", tokens.get(3).term);
            assertEquals(1, tokens.get(3).posIncrement);
        }
    }

    @Test
    public void testTargetCharWithExceptionInSameToken() throws IOException {
        try (Analyzer analyzer = new TibetanAnalyzer()) {
            // Test when target and exception are in the same token
            List<TokenInfo> tokens = analyzeText(analyzer, "word1 ཡིག word2");
            assertEquals(3, tokens.size());
            assertEquals("word1", tokens.get(0).term);
            assertEquals(1, tokens.get(0).posIncrement);
            assertEquals("ཡིག", tokens.get(1).term);
            assertEquals(1, tokens.get(1).posIncrement); // Should have normal posIncrement as it contains both
            assertEquals("word2", tokens.get(2).term);
            assertEquals(1, tokens.get(2).posIncrement);
        }
    }

    @Test
    public void testEmptyStream() throws IOException {
        try (Analyzer analyzer = new TibetanAnalyzer()) {
            // Test with empty input
            List<TokenInfo> tokens = analyzeText(analyzer, "");
            assertEquals(0, tokens.size());
        }
    }

    @Test
    public void testComplexSequence() throws IOException {
        try (Analyzer analyzer = new TibetanAnalyzer()) {
            // Test with a more complex sequence of tokens
            List<TokenInfo> tokens = analyzeText(analyzer, "ཡི normal ཡི ག ཡི normal ཡི དམ ཡི");
            assertEquals(9, tokens.size());
            
            assertEquals("ཡི", tokens.get(0).term);
            assertEquals(1, tokens.get(0).posIncrement); // First token
            
            assertEquals("normal", tokens.get(1).term);
            assertEquals(1, tokens.get(1).posIncrement);
            
            assertEquals("ཡི", tokens.get(2).term);
            assertEquals(1, tokens.get(2).posIncrement); // Followed by exception
            
            assertEquals("ག", tokens.get(3).term);
            assertEquals(1, tokens.get(3).posIncrement);
            
            assertEquals("ཡི", tokens.get(4).term);
            assertEquals(0, tokens.get(4).posIncrement); // Not followed by exception
            
            assertEquals("normal", tokens.get(5).term);
            assertEquals(1, tokens.get(5).posIncrement);
            
            assertEquals("ཡི", tokens.get(6).term);
            assertEquals(1, tokens.get(6).posIncrement); // Followed by exception
            
            assertEquals("དམ", tokens.get(7).term);
            assertEquals(1, tokens.get(7).posIncrement);
            
            assertEquals("ཡི", tokens.get(8).term);
            assertEquals(1, tokens.get(8).posIncrement); // Last token, not followed by exception
        }
    }
}
