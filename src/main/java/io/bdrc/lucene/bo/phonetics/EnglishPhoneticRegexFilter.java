package io.bdrc.lucene.bo.phonetics;

import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.pattern.PatternReplaceCharFilter;

public final class EnglishPhoneticRegexFilter {
    
    public static class PhoneticFilter1 extends PatternReplaceCharFilter {
        public PhoneticFilter1(final Reader in) {
            super(rCatcherMerged1, repl, in);
        }
        // trinlay, trinley -> trinle
        public static final Pattern rCatcherMerged1 = Pattern.compile("[ae]y($|[^aeiou])");
        public static final String repl = "e$1";
        public final static String normalizeR(final String in) {
            final Matcher matcher = rCatcherMerged1.matcher(in);
            return matcher.replaceAll(repl);
        }
    }
    
    public static class PhoneticFilter2 extends PatternReplaceCharFilter {
        public PhoneticFilter2(final Reader in) {
            super(rCatcherMerged2, repl, in);
        }
        // wa not at the beginning -> ba
        public static final Pattern rCatcherMerged2 = Pattern.compile("([a-zA-Z])w([aeo])(?![nN])");
        public static final String repl = "$1 b$2";
        public final static String normalizeR(final String in) {
            final Matcher matcher = rCatcherMerged2.matcher(in);
            return matcher.replaceAll(repl);
        }
    }
    
    public static class PhoneticFilter3 extends PatternReplaceCharFilter {
        public PhoneticFilter3(final Reader in) {
            super(rCatcherMerged3, repl, in);
        }
        // remove s or d at the end of a word
        public static final Pattern rCatcherMerged3 = Pattern.compile("[sd](?![a-zA-Z])");
        public static final String repl = "";
        public final static String normalizeR(final String in) {
            final Matcher matcher = rCatcherMerged3.matcher(in);
            return matcher.replaceAll(repl);
        }
    }
    
    public static class PhoneticFilter4 extends PatternReplaceCharFilter {
        public PhoneticFilter4(final Reader in) {
            super(rCatcherMerged4, repl, in);
        }
        // words ending in ny -> ni
        public static final Pattern rCatcherMerged4 = Pattern.compile("Y(?![a-zA-Z])");
        public static final String repl = "ni";
        public final static String normalizeR(final String in) {
            final Matcher matcher = rCatcherMerged4.matcher(in);
            return matcher.replaceAll(repl);
        }
    }
    
    public static class PhoneticFilter5 extends PatternReplaceCharFilter {
        public PhoneticFilter5(final Reader in) {
            super(rCatcherMerged5, repl, in);
        }
        // words ending in gy -> gi
        public static final Pattern rCatcherMerged5 = Pattern.compile("G(?![a-zA-Z])");
        public static final String repl = "gi";
        public final static String normalizeR(final String in) {
            final Matcher matcher = rCatcherMerged5.matcher(in);
            return matcher.replaceAll(repl);
        }
    }
    
    public static class PhoneticFilter6 extends PatternReplaceCharFilter {
        public PhoneticFilter6(final Reader in) {
            super(rCatcherMerged6, repl, in);
        }
        // words ending in y -> i
        public static final Pattern rCatcherMerged6 = Pattern.compile("y(?![a-zA-Z])");
        public static final String repl = "i";
        public final static String normalizeR(final String in) {
            final Matcher matcher = rCatcherMerged6.matcher(in);
            return matcher.replaceAll(repl);
        }
    }
    
    public static class PhoneticFilter7 extends PatternReplaceCharFilter {
        public PhoneticFilter7(final Reader in) {
            super(rCatcherMerged7, repl, in);
        }
        // Detect two consecutive identical letters that are either at the end of the string
        // or followed by a non-letter character
        public static final Pattern rCatcherMerged7 = Pattern.compile("([bgmn])\\1(?=$|[^a-zA-Z])");
        public static final String repl = "$1";
        public final static String normalizeR(final String in) {
            final Matcher matcher = rCatcherMerged7.matcher(in);
            return matcher.replaceAll(repl);
        }
    }
    
    public static Reader plugFilters(Reader in) {
        in = new PhoneticFilter1(in);
        in = new PhoneticFilter2(in);
        in = new PhoneticFilter3(in);
        in = new PhoneticFilter4(in);
        in = new PhoneticFilter5(in);
        in = new PhoneticFilter6(in);
        in = new PhoneticFilter7(in);
        return in;
    }

}
