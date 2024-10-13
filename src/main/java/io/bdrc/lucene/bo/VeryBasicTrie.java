package io.bdrc.lucene.bo;

import java.util.HashMap;
import java.util.Map;

public class VeryBasicTrie {

    public static final class TrieNode {
        final Map<Character, TrieNode> children = new HashMap<>();
        boolean canMatch = false;
    }

    private TrieNode root;

    public VeryBasicTrie() {
        this.root = new TrieNode();
    }

    public void add(final String key) {
        TrieNode node = root;
        for (char ch : key.toCharArray()) {
            node.children.putIfAbsent(ch, new TrieNode());
            node = node.children.get(ch);
        }
        node.canMatch = true;
    }

    // Finds the longest matching string in the Trie between start and end and returns the number of characters that matched
    public int findLongestMatchPos(final char[] b, final int start, final int end) {
        TrieNode node = root;
        int longestMatchPos = -1;
        for (int i = start; i < end; i++) {
            char ch = b[i];
            if (!node.children.containsKey(ch)) {
                break;
            }
            node = node.children.get(ch);
            if (node.canMatch) {
                longestMatchPos = start+i+1;
            }
        }
        return longestMatchPos;
    }
    
}
