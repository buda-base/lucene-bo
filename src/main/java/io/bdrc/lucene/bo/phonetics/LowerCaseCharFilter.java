package io.bdrc.lucene.bo.phonetics;

import java.io.Reader;

import org.apache.lucene.analysis.charfilter.MappingCharFilter;
import org.apache.lucene.analysis.charfilter.NormalizeCharMap;

public class LowerCaseCharFilter extends MappingCharFilter {
    
    /*
     * I'm not sure why but there's no Lucene char filter to lower case (only token filters, less good for us)
     * so we create our own. It's very basic and only handles the letters we need (no the whole Unicode range)
     */
    
    private static NormalizeCharMap cache = null;

    public LowerCaseCharFilter(final Reader in) {
        super(getCharMapCached(), in);
    }
    
    public static NormalizeCharMap getCharMapCached() {
        if (cache == null)
            cache = getNormalizeCharMap();
        return cache;
    }
    
    public final static NormalizeCharMap getNormalizeCharMap() {
        NormalizeCharMap.Builder builder = new NormalizeCharMap.Builder();
        builder.add("A", "a");
        builder.add("Ä", "a");
        builder.add("B", "b");
        builder.add("C", "c");
        builder.add("D", "d");
        builder.add("E", "e");
        builder.add("G", "g");
        builder.add("H", "h");
        builder.add("I", "i");
        builder.add("J", "j");
        builder.add("K", "k");
        builder.add("L", "l");
        builder.add("M", "m");
        builder.add("N", "n");
        builder.add("O", "o");
        builder.add("Ö", "o");
        builder.add("P", "p");
        builder.add("Q", "q");
        builder.add("R", "r");
        builder.add("S", "s");
        builder.add("T", "t");
        builder.add("U", "u");
        builder.add("Ü", "u");
        builder.add("V", "v");
        builder.add("W", "w");
        builder.add("Y", "y");
        builder.add("Z", "z");
        return builder.build(); 
    }
}
