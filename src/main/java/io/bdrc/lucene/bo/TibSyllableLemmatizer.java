package io.bdrc.lucene.bo;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.lucene.stemmer.Row;
import io.bdrc.lucene.stemmer.Trie;

public class TibSyllableLemmatizer extends TokenFilter {

    private static Trie defaultTrie = null;
    
    private Trie scanner = null;
    static final Logger logger = LoggerFactory.getLogger(TibWordTokenizer.class);
    
    protected TibSyllableLemmatizer(final TokenStream input) {
        super(input);
        if (defaultTrie != null) {
            this.scanner = defaultTrie;
            return;
        }
        InputStream stream = null;
        stream = CommonHelpers.getResourceOrFile("verbs-compiled-trie.dump");
        if (stream == null) {
            final String msg = "The syllables compiled Trie is not found. Either rebuild the Jar or run BuildCompiledTrie.main()"
                    + "\n\tAborting...";
            logger.error(msg);
        } else {
            try {
                this.scanner = new Trie(new DataInputStream(stream));
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            defaultTrie = scanner;
        }
        
    }
    
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    
    public String getReplacement(final char[] buffer, final int len) {
        int curidx = 0;
        int foundMatchCmdIndex = -1;
        Row curRow = this.scanner.getRow(this.scanner.getRoot());
        while (curidx < len && curRow != null) {
            final char c = buffer[curidx];
            foundMatchCmdIndex = curRow.getCmd(c);
            int ref = curRow.getRef(c);
            curRow = (ref >= 0) ? this.scanner.getRow(ref) : null;
            curidx += 1;
        }
        if (curidx != len || foundMatchCmdIndex == -1) return null;
        return this.scanner.getCommandVal(foundMatchCmdIndex);
    }
    
    @Override
    public final boolean incrementToken() throws IOException {
        if (!input.incrementToken()) {
            return false;
        }
        final char[] buffer = termAtt.buffer();
        final int len = termAtt.length();
        
        String repl = getReplacement(buffer, len);
        if (repl == null) return true;
        
        int newlen = repl.length();
        if (newlen != len)
            termAtt.setLength(newlen);
        
        // almost magical
        repl.getChars(0, newlen, buffer, 0);
        return true;
    }

}
