package io.bdrc.lucene.bo.phonetics;

import java.util.Map;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import io.bdrc.lucene.bo.phonetics.BasicTrie.TrieMatch;;

public class PhoneticSystem {

    protected BasicTrie onsetTrie;
    protected Map<String,char[]> vowelCodaRoot;
    protected Map<Character,String> sktPhonetic;
    private static final char[] wa = "w".toCharArray();
    private char[] implicitA;
    
    PhoneticSystem(final String implicitA) {
        this.implicitA = implicitA.toCharArray();
    }

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
    public boolean getPhonetics(final CharTermAttribute termAtt) {
        char[] b = termAtt.buffer();
        final int len = termAtt.length();
        if (len == 0)
            return false;
        // Find the longest onset match at the beginning of the string
        final TrieMatch onset = onsetTrie.findLongestMatch(b, len);
        if (onset.nbchar == 0)
            return false;
        // could be optimized by not using a string here... but it should be fine
        final String secondPart = String.copyValueOf(b, onset.nbchar, len-onset.nbchar);
        final char[] vowelCodaPhonetic = vowelCodaRoot.getOrDefault(secondPart, null);
        if (vowelCodaPhonetic == null)
            return false;
        char[] phonetic = onset.phonetic;
        // hack: dba = wa (but dbu != wu), this is the only case that doesn't fit in this algorithm
        if (onset.nbchar == 2 && b[0] == 'ད' && b[1] == 'བ' && len > 2 && b[2] != '\u0f72' && b[2] != '\u0f74' && b[2] != '\u0f7a' && b[2] != '\u0f7c')
            phonetic = wa;
        final int newLength = phonetic.length + vowelCodaPhonetic.length;
        if (b.length < newLength)
            b = termAtt.resizeBuffer(newLength);
        // just copy the two arrays:
        System.arraycopy(phonetic, 0, b, 0, phonetic.length);  // Copy starting at index 0
        System.arraycopy(vowelCodaPhonetic, 0, b, phonetic.length, vowelCodaPhonetic.length);
        termAtt.setLength(newLength);
        return true;
    }
    
    public boolean getSktPhonetics(final CharTermAttribute termAtt) {
        char[] b = termAtt.buffer();
        final int len = termAtt.length();
        if (len == 0)
            return false;
        // substitute character by character
        final StringBuilder phonetic = new StringBuilder();
        for (int i = 0 ; i < len ; i++) {
            char c = b[i];
            if (sktPhonetic.containsKey(c)) {
                phonetic.append(sktPhonetic.get(c));
            }  else {
                phonetic.append(c);
            }
        }
        if (phonetic.length() == 0 || "ieou".indexOf(phonetic.charAt(phonetic.length() - 1)) == -1)
            phonetic.append(this.implicitA);
        final int newLength = phonetic.length();
        if (b.length < newLength)
            b = termAtt.resizeBuffer(newLength);
        phonetic.getChars(0, newLength, b, 0);
        termAtt.setLength(newLength);
        return true;
    }
}
