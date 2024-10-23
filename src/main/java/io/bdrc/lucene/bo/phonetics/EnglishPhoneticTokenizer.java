package io.bdrc.lucene.bo.phonetics;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

import java.io.IOException;
import java.util.ArrayList;
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
                for (String possibleSyllable : possibleSyllables) {
                    /*
                     * Important hack: we transform al and an into el and en. We can't do that earlier
                     * because we don't want to change "pala" ("pa" "la") into "pela" ("pe" "la"). We need
                     * to do it at a stage where we need if the l is suffix or not. That stage is here. 
                     */
                    if (possibleSyllable.endsWith("al"))
                        possibleSyllable = possibleSyllable.substring(0,possibleSyllable.length()-2)+"el";
                    else if (possibleSyllable.endsWith("an"))
                        possibleSyllable = possibleSyllable.substring(0,possibleSyllable.length()-2)+"en";
                    if (possibleSyllable.startsWith("Gi"))
                        possibleSyllable = "gi" + possibleSyllable.substring(2);
                    else if (possibleSyllable.startsWith("Yi"))
                        possibleSyllable = "ni" + possibleSyllable.substring(2);
                    if (possibleSyllable.contains("z")) {
                        // z can be either T (ts) or s
                        tokens.add(new TokenWithIncrement(possibleSyllable.replace('z', 'T'), first ? 1 : 0));
                        tokens.add(new TokenWithIncrement(possibleSyllable.replace('z', 's'), 0));
                        first = false;
                    }
                    tokens.add(new TokenWithIncrement(possibleSyllable, first ? 1 : 0));
                    first = false;
                }
            }
        }
    }

    private List<String[]> getPossibleCuts(char[] b, int start, int end) {
        // For an intervocalic group of consonnants, give a list of two arrays:
        // first the list of possible codas of the last syllable
        // second the list of possible onsets of the next syllable
        final String ch = String.copyValueOf(b, start, end-start);
        if ("cfhjstdTDSvzw".indexOf(b[start]) != -1) {
            // if starts with a letter that cannot be a suffix, then cut before
            return List.of(new String[] { "" }, new String[] { ch });
        }
        if (ch.startsWith("G")) {
            final String rest = String.copyValueOf(b, start+1, end-start-1);
            return List.of(new String[] {"g", ""}, new String[] {"G"+rest});
        }
        if (ch.equals("N")) {
            // this one is pretty nasty...
            return List.of(new String[] {"N", "", "n"}, new String[] {"g", "N", "ng", "Ng"});
        }
        if (ch.startsWith("N")) {
            // like "wangpo"
            final String rest = String.copyValueOf(b, start+1, end-start-1);
            return List.of(new String[] {"N", ""}, new String[] {rest, "N"+rest});
        }
        if (ch.startsWith("Y")) {
            final String rest = String.copyValueOf(b, start+1, end-start-1);
            return List.of(new String[] {"", "n" }, new String[] {"y"+rest, "Y"+rest});
        }
        if (end == start+1) {
            // if just 1 character that is ambiguously a start or end of a syllable:
            return List.of(new String[] {"", ch}, new String[] {"", ch});
        }
        // if a character is repeated, we cut between the repetition
        char previouschar = '.';
        int pos = start;
        while (pos < end) {
            if (b[pos] == previouschar)
                return List.of(new String[] { String.copyValueOf(b, start, pos-start) }, new String[] { String.copyValueOf(b, pos, end-pos) });
            previouschar = b[pos];
            pos += 1;
        }
        if (ch.startsWith("nG")) {
            final String rest = String.copyValueOf(b, start+2, end-start-2);
            //return Collections.singletonList(new String[] { "N", rest }); // Wangyal -> Wang gyal. Can't find an example with ngy = n gy
            return List.of(new String[] {"N", "n"}, new String[] {"G"+rest});
        }
        // for the rest we cut after the first (ex: palden)
        return List.of(new String[] { String.copyValueOf(b, start, 1) }, new String[] { String.copyValueOf(b, start+1, end-start-1)});
        
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
            final List<List<String>> syllableGroups = new ArrayList<>(); // the final result
            
            for (int i = 0; i < vowelCount - 1; i++) {
                final int intervowel_start = vowelPositions.get(i)[1];
                final int intervowel_end = vowelPositions.get(i + 1)[0];
                
                if (intervowel_start <= intervowel_end) {
                    // Get the substring between vowels
                    final List<String[]> possibleCuts = getPossibleCuts(b, intervowel_start, intervowel_end);
                    List<String> syllableGroup = new ArrayList<>();

                    for (final String possibleCoda : possibleCuts.get(0)) {
                        // for all possible cuts, first look at all possible onsets and finish the previous syllables
                        for (final String possibleOnset : possibleOnsets) {
                            syllableGroup.add(possibleOnset+possibleCoda);
                        }
                    }
                    syllableGroups.add(syllableGroup);
                    
                    // if we're at the end, just finish:
                    if (i == vowelCount -2) {
                        syllableGroup = new ArrayList<>();
                        final String ending = String.copyValueOf(b, vowelPositions.get(i + 1)[0], end-vowelPositions.get(i + 1)[0]);
                        for (String possibleOnset : possibleCuts.get(1)) {
                            syllableGroup.add(possibleOnset+ending);
                        }
                        syllableGroups.add(syllableGroup);
                        break;
                    }
                    
                    // otherwise create the new list of possible onsets:
                    possibleOnsets = new ArrayList<>();
                    final String afternextvowel = String.copyValueOf(b, vowelPositions.get(i + 1)[0], vowelPositions.get(i + 1)[1]-vowelPositions.get(i + 1)[0]);
                    for (String possibleOnset : possibleCuts.get(1)) {
                        // for all possible cuts, create the new set of possible onsets
                        possibleOnsets.add(possibleOnset+afternextvowel);
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
