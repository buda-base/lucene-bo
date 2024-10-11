package io.bdrc.lucene.bo.phonetics;

import java.io.Reader;

import org.apache.lucene.analysis.charfilter.MappingCharFilter;
import org.apache.lucene.analysis.charfilter.NormalizeCharMap;

public final class EnglishPhoneticCharMapFilter extends MappingCharFilter {
    
    /*
     * Converts into the internal phonetic, with
     * - gy = G
     * - ng = N (when unambiguous)
     * - ny = Y (when unambiguous)
     * - dz = Z
     * - sh = S
     * - tr = D
     */
    
    private static NormalizeCharMap cache = null;

    public EnglishPhoneticCharMapFilter(final Reader in) {
        super(getCharMapCached(), in);
    }
    
    public static NormalizeCharMap getCharMapCached() {
        if (cache == null)
            cache = getNormalizeCharMap();
        return cache;
    }
    
    public final static NormalizeCharMap getNormalizeCharMap() {
        NormalizeCharMap.Builder builder = new NormalizeCharMap.Builder();
        builder.add("ae", "e");
        builder.add("oe", "o"); // Damchoe
        builder.add("ue", "u"); // Tsondue
        // remove different diacritics
        builder.add("ü", "u"); // \\uFC
        builder.add("u\u0308", "u"); // decomposed version
        builder.add("ö", "u"); // \\uF6
        builder.add("o\u0308", "o"); // decomposed version
        builder.add("ä", "e"); // \\uE4
        builder.add("a\u0308", "e"); // decomposed version
        builder.add("ï", "i");
        builder.add("i\u0308", "i"); // decomposed version
        builder.add("é", "e");
        // remove all acute accent diacritics
        builder.add("\u0301", "");
        builder.add("è", "e");
        builder.add("\u0300", "");
        builder.add("ṇ", "n"); // Paṇchen
        builder.add("n\u0323", "n");
        builder.add("aht", "a d"); // kaHtog
        builder.add("ḥ", " "); // kaḥtog
        builder.add("h\u0323", " ");
        builder.add("'", ""); // ka'tog, Mip'am
        builder.add("-", " "); // 
        // various diactitics
        builder.add("ś", "S");
        builder.add("s\u0301", "S");
        builder.add("ṣ", "S");
        builder.add("s\u0323", "S");
        builder.add("ñ", "Y");
        builder.add("n\u0303", "Y");
        builder.add("ṅ", "N");
        builder.add("n\u0307", "N");
        builder.add("ā", "a");
        builder.add("a\u0304", "a");
        builder.add("ī", "i");
        builder.add("i\u0304", "i");
        builder.add("ū", "u");
        builder.add("u\u0304", "u");
        builder.add("ṃ", "n");
        builder.add("m\u0323", "n");
        builder.add("ṁ", "n");
        builder.add("m\u0307", "n");
        builder.add("ṭ", "t");
        builder.add("t\u0323", "t");
        builder.add("ḍ", "d");
        builder.add("d\u0323", "d");
        builder.add("dj", "j"); // dordje -> dorje
        builder.add("zh", "S");
        builder.add("sh", "S");
        builder.add("shy", "S"); // Nyingtik Yabshyi
        builder.add("dz", "ts"); // to avoid confusion between dz and z
        builder.add("dzh", "ts");
        // z to dz in some cases
        builder.add("nz", "nts"); // tenzin
        builder.add("ngz", "Nts"); // 
        // but in most cases s (lobzang -> lobsang)
        builder.add("z", "s");
        // remove aspiration
        builder.add("chh", "c"); //
        builder.add("ch", "c");
        builder.add("j", "c");
        builder.add("jh", "c");
        builder.add("tsh", "ts");
        builder.add("ph", "b");
        builder.add("p", "b");
        builder.add("thr", "D");
        builder.add("th", "d");
        builder.add("dh", "d"); // Dhondup
        builder.add("tr", "D");
        builder.add("dr", "D");
        builder.add("dhr", "D");
        builder.add("t", "d");
        builder.add("kh", "g");
        builder.add("k", "g");
        builder.add("ck", "g"); // dicki
        builder.add("khy", "G");
        builder.add("ky", "G");
        // these are usually pronounced the same, example: Chöki -> chos kyi
        builder.add("kyi", "gi");
        builder.add("ngyi", "N yi"); // exception sangyik sang yig
        builder.add("gyi", "gi");
        builder.add("khyi", "gi");
        // exceptions
        builder.add("patrul", "pal Dul");
        builder.add("patrül", "pal Dul");
        builder.add("wose", "o se");
        builder.add("kanjur", "ka Gur");
        builder.add("kangyur", "ka Gur");
        builder.add("tanjur", "ten Gur");
        builder.add("tenjur", "ten Gur");
        builder.add("sangye", "saN Ge");
        builder.add("sangyé", "saN Ge");
        builder.add("senge", "seN ge");
        builder.add("sengé", "seN ge");
        builder.add("sengge", "seN ge");
        builder.add("senggé", "seN ge");
        builder.add("tashi", "Da Si");
        builder.add("tulku", "Dul ku");
        builder.add("tülku", "Dul ku");
        builder.add("ringdzin", "rig tsin");
        builder.add("ringzin", "rig tsin");
        builder.add("rindzin", "rig tsin");
        builder.add("rinzin", "rig tsin");
        builder.add("diki", "de gi");
        builder.add("dicki", "de gi");
        builder.add("karmay", "gar meu"); // མཁར་རྨེའུ -> Karmay
        builder.add("amnye", "a Ye");
        builder.add("amnyé", "a Ye");
        builder.add("derge", "de ge");
        builder.add("dergé", "de ge");
        builder.add("chamdo", "cab do");
        builder.add("gompa", "gon pa"); // this one is a bit difficult
        // chos rgya pronounced chögyam as a short for chos kyi rgya mtsho
        builder.add("chogyam", "cho Ga");
        builder.add("chögyam", "cho Ga");
        builder.add("gyamtso", "Ga tso");
        builder.add("khandro", "ga Do");
        builder.add("amdo", "a do");
        builder.add("chorten", "cho den");
        builder.add("chörten", "cho den");
        builder.add("dorje", "do je");
        builder.add("dorjee", "do je");
        builder.add("dorji", "do je");
        builder.add("kumbum", "gu bum");
        builder.add("orgyen", "o Gen");
        builder.add("urgyen", "u Gen");
        builder.add("lharje", "lha je");
        builder.add("lharjé", "lha je");
        builder.add("gyantse", "Gal tse");
        builder.add("gyantsé", "Gal tse");
        builder.add("lobzang", "lo saN");
        builder.add("lopzang", "lo saN");
        builder.add("lobsang", "lo saN");
        builder.add("lopsang", "lo saN");
        builder.add("chod ", "cho");
        builder.add("labrang", "la daN");
        builder.add("agsar", "a sar");
        builder.add("chugsum", "chu sum");
        builder.add("padm", "pe m");
        builder.add("gendun", "ge dun");
        builder.add("bonjong", "bo joN");
        builder.add("ganden", "ga den");
        builder.add("kundun", "gu dun");
        builder.add("kundün", "gu dun");
        builder.add("ngondzin", "No Zin");
        builder.add("yabshi", "ya Si");
        builder.add("sabche", "sa ce");
        builder.add("chonjug", "co jug");
        builder.add("kenjug", "ge jug");
        return builder.build(); 
    }
}
