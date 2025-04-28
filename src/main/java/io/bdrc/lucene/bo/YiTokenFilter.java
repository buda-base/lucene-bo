package io.bdrc.lucene.bo;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

/**
 * A TokenFilter that conditionally handles "ཡི". These tokens will have their positionIncrement 
 * set to 0 (effectively making them "invisible" for phrase queries) unless they are followed
 * by tokens containing certain characters like "ག", "དམ", etc., or if they are the first token in the stream.
 */
public final class YiTokenFilter extends TokenFilter {
    
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
    
    // The character we want to conditionally ignore
    private static final String TARGET_CHAR = "ཡི";
    
    // Set of characters that, when following TARGET_CHAR, prevent it from being ignored
    private static final Map<String,Boolean> EXCEPTION_CHARS = new HashMap<>();
    static {
    	EXCEPTION_CHARS.put("ག", true);
    	EXCEPTION_CHARS.put("དམ", true);
    	EXCEPTION_CHARS.put("གར", true);
    	EXCEPTION_CHARS.put("གེ", true);
    	EXCEPTION_CHARS.put("དྭགས", true);
    	EXCEPTION_CHARS.put("འཕྱིག", true);
    	EXCEPTION_CHARS.put("མུག", true);
    	EXCEPTION_CHARS.put("རྨོ", true);
    	EXCEPTION_CHARS.put("ཙི", true);
    	EXCEPTION_CHARS.put("ཆད", true);
    	EXCEPTION_CHARS.put("་གཅོད", true);
    	EXCEPTION_CHARS.put("ཐང", true);
    }
    
    // State tracking
    private boolean isFirstToken = true;
    private State savedState = null;
    
    public YiTokenFilter(TokenStream input) {
        super(input);
    }
    
    @Override
    public final boolean incrementToken() throws IOException {
    	System.out.println("\n\nincrement token");
    	System.out.println(String.format("  termatt %s", termAtt.toString()));
    	//System.out.println(String.format("  saved state is null: %b", savedState == null));
    	//System.out.println(String.format("  setCurrentTokenPosIncToZero: %b", setCurrentTokenPosIncToZero));
    	//System.out.println(String.format("  isFirstToken: %b", isFirstToken));
    	//System.out.println(String.format("  checkNextToken: %b", checkNextToken));
        // Handle any pending token position adjustments from previous operations
        if (savedState != null) {
        	System.out.println("saved state non null, restoring");
            restoreState(savedState);
            System.out.println(String.format("  termatt %s", termAtt.toString()));
            savedState = null;
            return true;
        }

        if (!input.incrementToken()) {
            return false;
        }
        
        // If this is the first token, never ignore it regardless of content
        if (isFirstToken) {
            isFirstToken = false;
            return true;
        }

        System.out.println("   incremented input token");
        System.out.println(String.format("  termatt %s", termAtt.toString()));

        final boolean isYi = termAtt.length() == 2 && termAtt.charAt(0) == 'ཡ' && termAtt.charAt(1) == '\u0F72';
        
        // If this token contains the target character, save its state and look ahead
        if (isYi) {
        	System.out.println("found yi");
            savedState = captureState();
            
            // Look ahead to the next token
            if (!input.incrementToken()) {
                // No more tokens, restore the saved state and return
            	// in that case we keep positionIncrement to 1, it doesn't
            	// make a lot of sense to ignore the last token if it's yi
                restoreState(savedState);
                savedState = null;
                return true;
            }
            
            // If the next token doesn't contain an exception character, set the current token's posIncrement to 0
            final String nextTerm = termAtt.toString();
            final boolean nextTermIsException = EXCEPTION_CHARS.containsKey(nextTerm); 
        	System.out.println("    next term: "+nextTerm);        

            State nextState = captureState();
            restoreState(savedState);
            savedState = nextState;
            if (!nextTermIsException)
            	posIncrAtt.setPositionIncrement(0);
            return true;
        }
        
        return true;
    }
    
    @Override
    public void reset() throws IOException {
        super.reset();
        isFirstToken = true;
        savedState = null;
    }
}
