package io.bdrc.lucene.bo;

import java.io.BufferedReader;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import io.bdrc.lucene.stemmer.Reduce;
import io.bdrc.lucene.stemmer.Trie;

public class BuildCompiledTrie {
    /**
     * Builds a Trie from all the entries in a list of files Dumps it in a binary
     * file
     * 
     * !!! Ensure to have enough Stack memory
     * 
     */

    public static void main(String[] args) {
        try {
            compileTrie("src/main/resources/bo-compiled-trie.dump", Arrays.asList("resources/output/total_lexicon.txt"));
            compileTrie("src/main/resources/verbs-compiled-trie.dump", Arrays.asList("resources/output/verbs_lemmas.csv"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    public static Trie compileTrie(String outFile, List<String> inputFiles) throws FileNotFoundException, IOException {
        Trie trie = buildTrie(inputFiles);
        storeTrie(trie, outFile);
        return trie;
    }

    /**
     * 
     * @param inputFiles
     *            the list of files to feed the Trie with
     * @throws FileNotFoundException
     *             input file not found
     * @throws IOException
     *             could not write to the output file
     * @return the optimized Trie
     */
    public static Trie buildTrie(List<String> inputFiles) throws FileNotFoundException, IOException {
        /* Fill the Trie with the content of all inputFiles */
        Trie trie = new Trie(true);
        for (String filename : inputFiles) {
            // currently only adds the entries without any diff
            try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
                String line;
                while ((line = br.readLine()) != null) {
                    final int spaceIndex = line.indexOf(' ');
                    if (spaceIndex == -1) {
                        throw new IllegalArgumentException(
                                "The dictionary file is corrupted in the following line.\n" + line);
                    } else {
                        trie.add(line.substring(0, spaceIndex), line.substring(spaceIndex + 1));
                    }
                }
            }
        }
        trie = new Reduce().optimize(trie);
        return trie;
    }

    /**
     * 
     * @param trie
     *            the trie to store in binary format
     * @param outFilename
     *            the path+filename of the output file
     * @throws FileNotFoundException
     *             output file not found
     * @throws IOException
     *             could not write to output file
     */
    public static void storeTrie(Trie trie, String outFilename) throws FileNotFoundException, IOException {
        OutputStream output = new DataOutputStream(new FileOutputStream(outFilename));
        trie.store((DataOutput) output);
    }
}