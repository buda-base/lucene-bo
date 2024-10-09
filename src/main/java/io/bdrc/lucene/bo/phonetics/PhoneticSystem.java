package io.bdrc.lucene.bo.phonetics;

import java.util.Map;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import io.bdrc.lucene.bo.phonetics.BasicTrie.TrieMatch;;

public class PhoneticSystem {

    protected BasicTrie onsetTrie;
    protected Map<String,char[]> vowelCodaRoot;

    // Convenience function, not used in production
    public String getPhonetics(final String s) {
        final char[] b = s.toCharArray();
        final TrieMatch onset = onsetTrie.findLongestMatch(b, b.length);
        if (onset.nbchar == 0)
            return null;
        // could be optimized by not using a string here... but it should be fine
        final String secondPart = String.copyValueOf(b, onset.nbchar, b.length);
        final char[] vowelCodaPhonetic = vowelCodaRoot.getOrDefault(secondPart, null);
        if (vowelCodaPhonetic == null)
            return null;
        return String.copyValueOf(onset.phonetic) + String.copyValueOf(vowelCodaPhonetic);
    }
    
    // Function to analyze a syllable into onset and vowel + coda
    public void getPhonetics(final CharTermAttribute termAtt) {
        char[] b = termAtt.buffer();
        final int len = termAtt.length();
        // Find the longest onset match at the beginning of the string
        final TrieMatch onset = onsetTrie.findLongestMatch(b, len);
        if (onset.nbchar == 0)
            return;
        // could be optimized by not using a string here... but it should be fine
        final String secondPart = String.copyValueOf(b, onset.nbchar, len);
        final char[] vowelCodaPhonetic = vowelCodaRoot.getOrDefault(secondPart, null);
        if (vowelCodaPhonetic == null)
            return;
        final int newLength = onset.phonetic.length + vowelCodaPhonetic.length;
        if (b.length < newLength)
            b = termAtt.resizeBuffer(newLength);
        // just copy the two arrays:
        System.arraycopy(onset.phonetic, 0, b, 0, onset.phonetic.length);  // Copy starting at index 0
        System.arraycopy(vowelCodaPhonetic, 0, b, onset.phonetic.length, vowelCodaPhonetic.length);
        termAtt.setLength(onset.phonetic.length + vowelCodaPhonetic.length);
    }
}
