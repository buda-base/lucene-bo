package io.bdrc.lucene.bo.phonetics;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EnglishPhoneticTokenizer extends Tokenizer {
    
    // made by ChatGPT, quite unoptimized, but this direction is less critical than the other one for performance

    private final CharTermAttribute charTermAttr = addAttribute(CharTermAttribute.class);
    private final PositionIncrementAttribute posIncrAttr = addAttribute(PositionIncrementAttribute.class);
    
    private static final class TokenWithIncrement {
        public final String token;
        public final int increment;
        
        TokenWithIncrement(final String token, final int increment) {
            this.token = token;
            this.increment = increment;
        }
    }

    private final List<TokenWithIncrement> tokens = new ArrayList<>();
    private int tokenIndex = 0;

    @Override
    public final boolean incrementToken() throws IOException {
        if (tokens.isEmpty()) {
            final StringBuilder inputStringBuilder = new StringBuilder();
            int ch;
            while ((ch = input.read()) != -1) {
                inputStringBuilder.append((char) ch);
            }
            if (inputStringBuilder.isEmpty())
                return false;
            final String inputText = inputStringBuilder.toString().trim();
            tokenizeText(inputText);
            //for (final TokenWithIncrement twi : tokens) {
            //    System.out.println("found token "+twi.token);
            //}
        }

        if (tokenIndex < tokens.size()) {
            clearAttributes();
            final TokenWithIncrement twi = tokens.get(tokenIndex);
            posIncrAttr.setPositionIncrement(twi.increment);
            char[] buffer = charTermAttr.buffer();
            final int toklen = twi.token.length(); 
            if (toklen > charTermAttr.length())
                buffer = charTermAttr.resizeBuffer(toklen);
            // copy the string in the buffer
            twi.token.getChars(0, toklen, buffer, 0);
            charTermAttr.setLength(toklen);
            tokenIndex++;
            return true;
        }
        return false;
    }

    private void tokenizeText(String inputText) {
        // Split the text by spaces to get words
        final String[] words = inputText.split("\\s+");
        for (final String word : words) {
            if (word.isEmpty()) continue;
            final List<List<String>> syllables = processWord(word.toCharArray(), 0, word.length());
            for (final List<String> possibleSyllables : syllables) {
                // increment is 1 for the first in the possible syllables, then 0
                boolean first = true;
                for (final String possibleSyllable : possibleSyllables) {
                    tokens.add(new TokenWithIncrement(possibleSyllable, first ? 1 : 0));
                    first = false;
                }
            }
        }
    }

    private List<String[]> getPossibleCuts(char[] b, int start, int end) {
        // This is where you implement your logic for determining possible cuts between vowels.
        // For now, just return one possible cut for demonstration.
        // Here we are returning a single cut as an example.
        final String ch = String.copyValueOf(b, start, end-start);
        if ("cfhjstdTDSvw".indexOf(b[start]) != -1) {
            // if starts with a letter that cannot be a suffix, then cut before
            return Collections.singletonList(new String[] { "", ch });
        }
        if (ch.equals("N")) {
            // this one is pretty nasty...
            final String rest = String.copyValueOf(b, start+1, end-start-1);
            return List.of(new String[] {"", "N"+rest}, new String[] {"N", ""+rest}, new String[] {"n", "g"+rest}, new String[] {"N", "N"+rest}, new String[] {"N", "g"+rest}, new String[] {"n", "N"+rest});
        }
        if (end == start+1) {
            // not 100% sure it's always true, but "ala" -> "a la" and "ama" -> "a ma"
            if (b[start] == 'l' || b[start] == 'm'  || b[start] == 'g')
                return Collections.singletonList(new String[] { "", ch });
            // if just 1 character that is ambiguously a start or end of a syllable:
            return List.of(new String[] {"", ch}, new String[] {ch, ""}, new String[] {ch, ch});
        }
        // if a character is repeated, we cut between the repetition
        char previouschar = '.';
        int pos = start;
        while (pos < end) {
            if (b[pos] == previouschar)
                return Collections.singletonList(new String[] { String.copyValueOf(b, start, pos-start), String.copyValueOf(b, pos, end-pos) });
            previouschar = b[pos];
            pos += 1;
        }
        if (ch.startsWith("nG")) {
            final String rest = String.copyValueOf(b, start+2, end-start-2);
            return List.of(new String[] {"N", "y"+rest}, new String[] {"n", "G"+rest}, new String[] {"N", "G"+rest});
        }
        if (ch.startsWith("N")) {
            // like "wangpo"
            final String rest = String.copyValueOf(b, start+1, end-start-1);
            return Collections.singletonList(new String[] {"N", rest});
        }
        if (ch.startsWith("Y")) {
            final String rest = String.copyValueOf(b, start+1, end-start-1);
            return List.of(new String[] {"n", "y"+rest}, new String[] {"", "Y"+rest});
        }
        // for the rest we cut after the first (ex: palden)
        return Collections.singletonList(new String[] { String.copyValueOf(b, start, 1), String.copyValueOf(b, start+1, end-start-1)});
        
    }

    private List<List<String>> processWord(char[] b, int start, int end) {
        List<int[]> vowelPositions = new ArrayList<>();
        
        // Find all the positions of vowels in the word
        int vowelgroupstart = -1;
        boolean previousisvowel = false;
        for (int i = start; i < end; i++) {
            if ("aeiou".indexOf(b[i]) != -1) {
                if (!previousisvowel)
                    vowelgroupstart = i;
                previousisvowel = true;
            } else {
                if (previousisvowel) {
                    final int[] vowelPosition = new int[2];
                    vowelPosition[0] = vowelgroupstart;
                    vowelPosition[1] = i;
                    vowelPositions.add(vowelPosition);
                    previousisvowel = false;
                }
            }
        }
        if (previousisvowel) {
            final int[] vowelPosition = new int[2];
            vowelPosition[0] = vowelgroupstart;
            vowelPosition[1] = end;
            vowelPositions.add(vowelPosition);
        }

        int vowelCount = vowelPositions.size();
        
        if (vowelCount < 2) {
            // If there is only one vowel, return the whole word as a token
            return List.of(List.of(String.copyValueOf(b, start, end-start)));
        } else {
            // we initialize the list of possible onsets to what's before the first vowel group + the first vowel group
            List<String> possibleOnsets = List.of(String.copyValueOf(b, start, vowelPositions.get(0)[1]-start));
            List<List<String>> syllableGroups = new ArrayList<>(); // the final result
            
            for (int i = 0; i < vowelCount - 1; i++) {
                int intervowel_start = vowelPositions.get(i)[1];
                int intervowel_end = vowelPositions.get(i + 1)[0];
                
                if (intervowel_start <= intervowel_end) {
                    // Get the substring between vowels
                    List<String[]> possibleCuts = getPossibleCuts(b, intervowel_start, intervowel_end);
                    List<String> syllableGroup = new ArrayList<>();
                    
                    for (String[] cut : possibleCuts) {
                        // for all possible cuts, first look at all possible onsets and finish the previous syllables
                        for (String possibleOnset : possibleOnsets) {
                            syllableGroup.add(possibleOnset+cut[0]);
                        }
                    }
                    syllableGroups.add(syllableGroup);
                    
                    // if we're at the end, just finish:
                    if (i == vowelCount -2) {
                        syllableGroup = new ArrayList<>();
                        final String ending = String.copyValueOf(b, vowelPositions.get(i + 1)[0], end-vowelPositions.get(i + 1)[0]);
                        for (String[] cut : possibleCuts) {
                            // for all possible cuts, create the new set of possible onsets
                            syllableGroup.add(cut[1]+ending);
                        }
                        syllableGroups.add(syllableGroup);
                    }
                    
                    // otherwise create the new list of possible onsets:
                    possibleOnsets = new ArrayList<>();
                    final String afternextvowel = String.copyValueOf(b, vowelPositions.get(i + 1)[0], vowelPositions.get(i + 1)[1]-vowelPositions.get(i + 1)[0]);
                    for (String[] cut : possibleCuts) {
                        // for all possible cuts, create the new set of possible onsets
                        possibleOnsets.add(cut[1]+afternextvowel);
                    }
                    
                }
            }
            return syllableGroups;
        }
    }
    
    // TODO: test with Gomnyam

    @Override
    public void reset() throws IOException {
        super.reset();
        tokens.clear();
        tokenIndex = 0;
    }
}
