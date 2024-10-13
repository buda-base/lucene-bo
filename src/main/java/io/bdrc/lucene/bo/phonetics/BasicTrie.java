package io.bdrc.lucene.bo.phonetics;

import java.util.HashMap;
import java.util.Map;

public class BasicTrie {
    
    public static final class TrieNode {
        final Map<Character, TrieNode> children = new HashMap<>();
        char[] phonetic = null; // Stores the phonetic representation of the syllable part
        boolean canbefinal = false;
    }
    
    public static final class TrieMatch {
        public char[] phonetic = null;
        public int nbchar = 0;
    }

    private final TrieNode root;

    public BasicTrie() {
        this.root = new TrieNode();
    }

    // Adds a string (onset or vowel + coda) to the Trie with its phonetic representation
    public void add(final String key, final String phonetic, final boolean canbefinal) {
        TrieNode node = root;
        for (char ch : key.toCharArray()) {
            node.children.putIfAbsent(ch, new TrieNode());
            node = node.children.get(ch);
        }
        node.phonetic = phonetic.toCharArray(); // Store the phonetic representation at the leaf
        node.canbefinal = canbefinal;
    }

    // same with canbefinal defaulting to true
    public void add(final String key, final String phonetic) {
        add(key, phonetic, true);
    }

    // Finds the longest matching string in the Trie and returns its phonetic representation
    public TrieMatch findLongestMatch(final char[] b, final int len) {
        TrieNode node = root;
        TrieMatch longestMatch = new TrieMatch();
        for (int i = 0; i < len; i++) {
            char ch = b[i];
            if (!node.children.containsKey(ch)) {
                break;
            }
            node = node.children.get(ch);
            if (node.phonetic != null && (node.canbefinal || i < len -1)) {
                longestMatch.phonetic = node.phonetic; // Update longest match found
                longestMatch.nbchar = i+1;
            }
        }
        return longestMatch;
    }
}