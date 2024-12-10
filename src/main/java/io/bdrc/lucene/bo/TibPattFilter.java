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
    
    public static class ReorderFilter extends PatternReplaceCharFilter {
        public ReorderFilter(Reader in) {
            super(rReorder, repl, in);
        }
        // https://github.com/buda-base/lucene-bo/issues/17
        // reorder vowel + subscript into subscript + vowel
        public static final Pattern rReorder = Pattern.compile("([ཱ-྇]+)([ྍ-ྼ]+)");
        public static final String repl = "$2$1";
        public final static String normalizeR(final String in) {
            final Matcher matcher = rReorder.matcher(in);
            return matcher.replaceAll(repl);
        }
    }
    
    public static class SktFilter1 extends PatternReplaceCharFilter {
        public SktFilter1(Reader in) {
            super(init, repl, in);
        }
        public static final Pattern init = Pattern.compile("[\u0F59\u0F5A]([\u0F71\u0F90-\u0FAC\u0FB3\u0FB7])");
        public static final String repl = "\u0F45$1";
    }
    
    public static class SktFilter2 extends PatternReplaceCharFilter {
        public SktFilter2(Reader in) {
            super(init, repl, in);
        }
        public static final Pattern init = Pattern.compile("[\u0FA9\u0FAA]([\u0F71\u0F90-\u0FAC\u0FB3\u0FB7])");
        public static final String repl = "\u0F95$1";
    }
    
    public static class SktFilter3 extends PatternReplaceCharFilter {
        public SktFilter3(Reader in) {
            super(init, repl, in);
        }
        public static final Pattern init = Pattern.compile("\u0F5B([\u0F71\u0F90-\u0FAC\u0FB3\u0FB7])");
        public static final String repl = "\u0F47$1";
    }
    
    public static class SktFilter4 extends PatternReplaceCharFilter {
        public SktFilter4(Reader in) {
            super(init, repl, in);
        }
        public static final Pattern init = Pattern.compile("\u0FAB([\u0F71\u0F90-\u0FAC\u0FB3\u0FB7])");
        public static final String repl = "\u0F97$1";
    }
    
    /*
     * This is to be used in the case where we want to keep some shads in the tokens. In that scenario, there are shads that
     * we don't want: those after the yigo. This filter removes the yigo and subsequent shads
     */
    public static class PunctFilter1 extends PatternReplaceCharFilter {
        public PunctFilter1(Reader in) {
            super(init, repl, in);
        }
        public static final Pattern init = Pattern.compile("[\u0f01-\u0f07\u0fd3\u0fd4]+(\\s|[\u0f08-\u0f14])*");
        public static final String repl = "";
    }
    
    /*
     * This filter folds all type of shad and repetitions into just one shad, to be used in the case where we want to keep some shads in the tokens
     */
    public static class PunctFilter2 extends PatternReplaceCharFilter {
        public PunctFilter2(Reader in) {
            super(init, repl, in);
        }
        public static final Pattern init = Pattern.compile("([\u0f0d-\u0f14]+\\s*)+");
        public static final String repl = " \u0f0d "; // surrounded by space so that shad are treated as isolated tokens by the tokenizer
    }
    
    public static Reader plugFilters(Reader in) {
        in = new ReorderFilter(in);
        in = new MergedSylFilter1(in);
        in = new MergedSylFilter2(in);
        in = new MergedSylFilter3(in);
//        in = new MergedSylFilter4(in);
        in = new SktFilter1(in);
        in = new SktFilter2(in);
        in = new SktFilter3(in);
        in = new SktFilter4(in);
        return in;
    }

}
