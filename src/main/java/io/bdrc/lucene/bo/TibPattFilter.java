package io.bdrc.lucene.bo;

import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.pattern.PatternReplaceCharFilter;

public class TibPattFilter {
    
    // in this class we have one Filter per pattern
    // All the patterns are related to Old Tibetan, and are important to apply
    // before any tokenization happens as they add tsheks
    
    public static class MergedSylFilter1 extends PatternReplaceCharFilter {
        public MergedSylFilter1(Reader in) {
            super(rCatcherMerged1, repl, in);
        }
        // from Tibetan-nlp: Traditionally in Classical Tibetan, syllables are separated by a tsheg. 
        // In Old Tibetan texts, syllable margins are not so clear and often a syllable (verb, noun and so on)
        // is merged together with the following case marker or converb (For example: སྟགི > སྟག་གི,  དུསུ > དུས་སུ,  བཀུམོ > བཀུམ་མོ). 
        // Rule: Split merged syllables for cases as དྲངསྟེ > དྲངས་ཏེ
        // ([ཀ-ྼ])སྟེ   -> $1ས་ཏེ
        public static final Pattern rCatcherMerged1 = Pattern.compile("([ཀ-ྼ])སྟེ");
        public static final String repl = "$1ས་ཏེ";
        public final static String normalizeR(final String in) {
            final Matcher matcher = rCatcherMerged1.matcher(in);
            return matcher.replaceAll(repl);
        }
    }
    
    public static class MergedSylFilter2 extends PatternReplaceCharFilter {
        public MergedSylFilter2(Reader in) {
            super(rCatcherMerged2, repl, in);
        }
        // from Tibetan-nlp:
        // Rule: Split merged syllables for cases as གཅལྟོ > གཅལད་ཏོ
        // ([ཀ-ྼ][ནལར])ྟ([ེོ])", "$1་ཏ$2
        public static final Pattern rCatcherMerged2 = Pattern.compile("([ཀ-ྼ][ནལར])ྟ([ེོ])");
        public static final String repl = "$1་ཏ$2";
        public final static String normalizeR(final String in) {
            final Matcher matcher = rCatcherMerged2.matcher(in);
            return matcher.replaceAll(repl);
        }
    }
    
    public static class MergedSylFilter3 extends PatternReplaceCharFilter {
        public MergedSylFilter3(Reader in) {
            super(rCatcherMerged3, repl, in);
        }
        // from Tibetan-nlp:
        // Rule: Split merged syllables for cases with genitive as གགྀ་ > གག་གྀ་, པགི་ > པག་གི་
        // (I need to include this rule otherwise these cases are not taken into account by the
        // generic rules where the condition {2-6}C will skip them.
        // On the other hand, in the generic rule, using a condition as {1-6}C
        // will introduce errors since the rule will split words as "bshi"
        // ([ཀ-ྼ])ག([ིྀ][^ཀ-ྼ])", "$1ག་ག$2
        // the first character shouldn't be a valid prefix of ག  (which are ད, བ, མ and འ), see
        // https://github.com/tibetan-nlp/tibcg3/issues/4
        public static final Pattern rCatcherMerged3 = Pattern.compile("([ཀ-ཐདྷ-ཕབྷཙ-ཟཡ-ྼ])ག([ིྀ][^ཀ-ྼ])");
        public static final String repl = "$1ག་ག$2";
        public final static String normalizeR(final String in) {
            final Matcher matcher = rCatcherMerged3.matcher(in);
            return matcher.replaceAll(repl);
        }
    }
    
    public static class MergedSylFilter4 extends PatternReplaceCharFilter {
        public MergedSylFilter4(Reader in) {
            super(rCatcherMerged4, repl, in);
        }
        // from Tibetan-nlp:
        // Rule: Split merged syllables
        // see also https://github.com/tibetan-nlp/tibcg3/issues/6
        public static final Pattern rCatcherMerged4 = Pattern.compile("([ཀ-ྼ][ཀ-ྼ]+)([ཀ-ཟཡ-ཬ])([ོེིྀུ])");
        public static final String repl = "$1$2་$2$3";
        public final static String normalizeR(final String in) {
            final Matcher matcher = rCatcherMerged4.matcher(in);
            return matcher.replaceAll(repl);
        }
    }
    
    public static Reader plugFilters(Reader in) {
        in = new MergedSylFilter1(in);
        in = new MergedSylFilter2(in);
        in = new MergedSylFilter3(in);
        in = new MergedSylFilter4(in);
        return in;
    }

}
