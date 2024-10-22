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
     * - ts = T
     * - sh = S
     * - tr = D
     */
    
    private static NormalizeCharMap cache = null;
    private static boolean ignoreRetroflex = true;

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
        builder.add("0", "༠");
        builder.add("1", "༡");
        builder.add("2", "༢");
        builder.add("3", "༣");
        builder.add("4", "༤");
        builder.add("5", "༥");
        builder.add("6", "༦");
        builder.add("7", "༧");
        builder.add("8", "༨");
        builder.add("9", "༩");
        builder.add("ae", "e");
        builder.add("ai", "e"); // a'i is sometimes pronounced aï (as in Dalai Lama) or e (more common)
        builder.add("aï", "e");
        builder.add("ee", "i"); // shree
        builder.add("oe", "o"); // Damchoe
        builder.add("ue", "u"); // Tsondue
        // remove different diacritics
        builder.add("ü", "u"); // \\uFC
        builder.add("u\u0308", "u"); // decomposed version
        builder.add("ö", "o"); // \\uF6
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
        builder.add("ahth", "a d"); // kaHtog
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
        builder.add("dj", "c"); // Dudjom
        builder.add("zh", "S");
        builder.add("sh", "S");
        builder.add("shy", "S"); // Nyingtik Yabshyi
        builder.add("dz", "T"); // to avoid confusion between dz and z
        // builder.add("dzh", "T"); // not sure it's really cogent
        // remove aspiration
        builder.add("hl", "l"); // hl is a bit closer to pronounciation
        builder.add("lh", "l");
        builder.add("chh", "c");
        builder.add("ch", "c");
        builder.add("j", "c");
        builder.add("jh", "c");
        builder.add("tsh", "T");
        builder.add("ts", "T");
        builder.add("ph", "b");
        builder.add("p", "b");
        builder.add("thr", ignoreRetroflex ? "d" : "D");
        builder.add("th", "d");
        builder.add("dh", "d"); // Dhondup
        builder.add("tr", ignoreRetroflex ? "d" : "D");
        builder.add("dr", ignoreRetroflex ? "d" : "D");
        builder.add("dhr", ignoreRetroflex ? "d" : "D");
        builder.add("t", "d");
        builder.add("kh", "g");
        builder.add("k", "g");
        builder.add("ck", "g"); // dicki
        builder.add("ngh", " N"); // Sengha / Singha
        builder.add("khy", "G");
        builder.add("ky", "G");
        builder.add("nkhy", "n G"); // Kunkhyab
        builder.add("nky", "n G");
        // complicated, see tokenizer
        builder.add("ngy", "nG");
        builder.add("gy", "G");
        builder.add("ng", "N");
        builder.add("ny", "Y");
        builder.add("v", " b");
        // exceptions
        builder.add("patrul", ignoreRetroflex ? "bal dul" : "bal Dul");
        builder.add("patrül", ignoreRetroflex ? "bal dul" : "bal Dul");
        builder.add("mingyur", "mi Gur");
        builder.add("pakshi", "ba gSi");
        builder.add("rakshi", "ra gSi");
        builder.add("dharma", "da rma");
        builder.add("amchi ", "em ci ");
        builder.add("wose", "o se");
        builder.add("wöse", "o se");
        builder.add("kanjur", "ga Gur");
        builder.add("kangyur", "ga Gur");
        builder.add("tanjur", "den Gur");
        builder.add("tenjur", "den Gur");
        builder.add("umdze", "u Te");
        builder.add("umze", "u Te");
        builder.add("umdzé", "u Te");
        builder.add("umzé", "u Te");
        builder.add("sangye", "saN Ge");
        builder.add("sangyé", "saN Ge");
        builder.add("senge", "seN ge");
        builder.add("sengé", "seN ge");
        builder.add("sengge", "seN ge");
        builder.add("senggé", "seN ge");
        builder.add("ringdzin", "rig Tin");
        builder.add("ringzin", "rig Tin");
        builder.add("rindzin", "rig Tin");
        builder.add("rinzin", "rig Tin");
        builder.add("diki", "de gi");
        builder.add("dicki", "de gi");
        builder.add("dickie", "de gi");
        builder.add("karmay", "gar meu"); // མཁར་རྨེའུ -> Karmay
        builder.add("amnye", "a Ye");
        builder.add("amnyé", "a Ye");
        builder.add("derge", "de ge");
        builder.add("dergé", "de ge");
        builder.add("chamdo", "cab do");
        builder.add("gompa", "gon ba"); // this one is a bit difficult
        // chos rgya pronounced chögyam as a short for chos kyi rgya mtsho
        builder.add("chogyam", "cho Ga");
        builder.add("chögyam", "cho Ga");
        builder.add("gyamtso", "Ga To");
        builder.add("khandro", ignoreRetroflex ? "ga do" : "ga Do");
        builder.add("amdo", "a do");
        builder.add("chorten", "cho den");
        builder.add("chörten", "cho den");
        builder.add("dorje", "do ce");
        builder.add("dorjee", "do ce");
        builder.add("dorji", "do ce");
        builder.add("kumbum", "gu bum");
        builder.add("orgyen", "o Gen");
        builder.add("urgyen", "u Gen");
        builder.add("lharje", "lha ce");
        builder.add("lharjé", "lha ce");
        builder.add("gyantse", "Gal Te");
        builder.add("gyantsé", "Gal Te");
        builder.add("lobzang", "lo saN");
        builder.add("lopzang", "lo saN");
        builder.add("lobsang", "lo saN");
        builder.add("lopsang", "lo saN");
        builder.add("chod ", "cho");
        builder.add("labrang", "la daN");
        builder.add("agsar", "a sar");
        builder.add("chugsum", "chu sum");
        builder.add("padm", "be m");
        builder.add("gendun", "ge dun");
        builder.add("bonjong", "bo joN");
        builder.add("ganden", "ga den");
        builder.add("kundun", "gu dun");
        builder.add("kundün", "gu dun");
        builder.add("ngondzin", "No Tin");
        builder.add("yabshi", "ya Si");
        builder.add("sabche", "sa ce");
        builder.add("chonjug", "co cug");
        builder.add("kenjug", "ge cug");
        builder.add("panchen", "ben cen");
        builder.add("paṇchen", "ben cen");
        builder.add("pandita", "ba ndida");
        builder.add("paṇḍita", "ba ndida");
        builder.add("vajra", "ba Tra"); // ts -> c (dz -> j) is a bit difficult...
        if (!ignoreRetroflex) {
            builder.add("tashi", "da Si");
            builder.add("tulku", "dul gu");
            builder.add("tülku", "dul gu");
        }
        return builder.build(); 
    }
}
