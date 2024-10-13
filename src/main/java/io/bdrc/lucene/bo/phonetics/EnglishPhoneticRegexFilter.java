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
        public static final Pattern rCatcherMerged2 = Pattern.compile("([a-z])w([aeo])(?![nN])");
        public static final String repl = "$1 b$2";
        public final static String normalizeR(final String in) {
            final Matcher matcher = rCatcherMerged2.matcher(in);
            return matcher.replaceAll(repl);
        }
    }
    
    public static Reader plugFilters(Reader in) {
        in = new PhoneticFilter1(in);
        in = new PhoneticFilter2(in);
        return in;
    }

}
