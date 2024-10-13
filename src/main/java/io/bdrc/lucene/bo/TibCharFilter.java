package io.bdrc.lucene.bo;

import java.io.Reader;

import org.apache.lucene.analysis.charfilter.MappingCharFilter;
import org.apache.lucene.analysis.charfilter.NormalizeCharMap;

public class TibCharFilter extends MappingCharFilter {
    public TibCharFilter(final Reader in) {
        super(getTibNormalizeCharMapCached(true, true), in);
    }
    
    public TibCharFilter(final Reader in, final boolean lenient, final boolean oldtib) {
        super(getTibNormalizeCharMapCached(lenient, oldtib), in);
    }
    
    private static final NormalizeCharMap[] cache = new NormalizeCharMap[] {null, null, null, null};
    private static NormalizeCharMap getTibNormalizeCharMapCached(final boolean lenient, final boolean oldtib) {
        final int idx = lenient ? (oldtib ? 0 : 1) : (oldtib ? 2 : 3);
        if (cache[idx] == null)
            cache[idx] = getTibNormalizeCharMap(lenient, oldtib);
        return cache[idx];
    }

    public final static NormalizeCharMap getTibNormalizeCharMap(final boolean lenient, final boolean oldtib) {
        NormalizeCharMap.Builder builder = new NormalizeCharMap.Builder();
        // The non-breaking tsheg and double tsheg are replaced with the normal one
        builder.add("\u0f0C", "\u0F0B");
        builder.add("\u0fD2", "\u0F0B");
        // we map latin digits to Tibetan instead of the opposite because
        // TibSyllableTokenizer remove non-Tibetan (including latin digits)
        builder.add("༠", "0");
        builder.add("༡", "1");
        builder.add("༢", "2");
        builder.add("༣", "3");
        builder.add("༤", "4");
        builder.add("༥", "5");
        builder.add("༦", "6");
        builder.add("༧", "7");
        builder.add("༨", "8");
        builder.add("༩", "9");
        // Characters to delete: the markers found under selected syllables
        builder.add("\u0F35", ""); // ༵
        builder.add("\u0F37", ""); // ༷
        builder.add("\u0F39", ""); // ༹
        // Characters to decompose
        builder.add("\u0F00", "\u0F68\u0F7C\u0F7E"); // ༀ
        builder.add("\u0F02", "\u0F60\u0F74\u0F82\u0F7F"); // ༂
        builder.add("\u0F03", "\u0F60\u0F74\u0F82\u0F14"); // ༃
        builder.add("\u0F43", "\u0F42\u0FB7"); // གྷ
        builder.add("\u0F48", "\u0F47\u0FB7"); // ཈
        builder.add("\u0F4D", lenient ? "\u0F47\u0FB7" : "\u0F4C\u0FB7"); // ཌྷ
        builder.add("\u0F52", "\u0F51\u0FB7"); // དྷ
        builder.add("\u0F57", "\u0F56\u0FB7"); // བྷ
        builder.add("\u0F5C", "\u0F5B\u0FB7"); // ཛྷ
        builder.add("\u0F69", lenient ? "\u0F40\u0FB4" : "\u0F40\u0FB5"); // ཀྵ
        builder.add("\u0F73", lenient ? "\u0F72" : "\u0F71\u0F72"); // ཱི
        builder.add("\u0F75", lenient ? "\u0F74" : "\u0F71\u0F74"); // ཱུ
        builder.add("\u0F76", lenient ? "\u0FB2\u0F72" : "\u0FB2\u0F80"); // ྲྀ
        builder.add("\u0F77", lenient ? "\u0FB2\u0F72" : "\u0FB2\u0F71\u0F80"); // ཷ
        builder.add("\u0F78", lenient ? "\u0FB3\u0F72" : "\u0FB3\u0F80"); // ླྀ
        builder.add("\u0F79", lenient ? "\u0FB3\u0F72" : "\u0FB3\u0F71\u0F80"); // ཹ
        builder.add("\u0F81", lenient ? "\u0F72" : "\u0F71\u0F80"); // ཱྀ
        builder.add("\u0F93", "\u0F92\u0FB7"); // ྒྷ
        builder.add("\u0F9D", lenient ? "\u0FA1\u0FB7" : "\u0F9C\u0FB7"); // ྜྷ
        builder.add("\u0FA2", "\u0FA1\u0FB7"); // ྡྷ
        builder.add("\u0FA7", "\u0FA6\u0FB7"); // ྦྷ
        builder.add("\u0FAC", "\u0FAB\u0FB7"); // ྫྷ
        builder.add("\u0FB9", lenient ? "\u0F90\u0FB4" : "\u0F90\u0FB5"); // ྐྵ
        if (lenient) {
            // not entirely sure about the following one:
            builder.add("ཾ", "ྃ");
            // double vowels
            builder.add("ེེ", "ཻ");
            builder.add("ོོ", "ཽ");
            // mapping retroflex to "normal", so that the search is less case sensitive
            builder.add("ཊ", "ཏ");
            builder.add("ཋ", "ཐ");
            builder.add("ཌ", "ད");
            builder.add("ཎ", "ན");
            builder.add("ཱ", "");
            builder.add("ྂ", "ྃ");
            builder.add("ྚ", "ྟ");
            builder.add("ྛ", "ྠ");
            builder.add("ྜ", "ྡ");
            builder.add("ྞ", "ྣ");
            builder.add("ྺ", "ྭ");
            builder.add("ྻ", "ྱ");
            builder.add("ྰ", "");
            builder.add("ྼ", "ྲ");
            builder.add("ཪ", "ར");
            builder.add("ཥ", "ཤ");
            builder.add("ྵ" , "ྴ");
            // a few Sanskrit stacks:
            builder.add("ནྱ", "ཉ");
            builder.add("ྣྱ", "ྙ");
            builder.add("རྨྨ", "རྨ");
            builder.add("རྦྦ", "རྦ");
            builder.add("རྒྒ", "རྒ");
            // padma = pad+ma, pandi = pan+di, ratna = rat+na
            // https://github.com/buda-base/lucene-bo/issues/33
            builder.add("པདམ", "པད་མ");
            builder.add("པདྨ", "པད་མ");
            builder.add("སེངྒེ", "སེང་གེ");
            builder.add("སེངགེ", "སེང་གེ");
            builder.add("ལིངྒ", "ལིང་ག");
            builder.add("ལོཙ", "ལོ་ཙ");
            builder.add("ལོཚ", "ལོ་ཙ");
            builder.add("ལོ་ཙྭ", "ལོ་ཙ");
            builder.add("ཙྪ", "ཙ");
            builder.add("ཀུཎྜ", "ཀུ་ནྡ");
            builder.add("ཀུནྡ", "ཀུ་ནྡ");
            builder.add("བནྡྷ", "བན་དྷ");
            builder.add("མནྟ", "མན་ཏ");
            builder.add("ཀྲོདྷ", "ཀྲོ་དྷ");
            builder.add("ཀྲོདྡྷ", "ཀྲོ་དྷ");
            builder.add("པནདི", "པནྡི");
            builder.add("རཏན", "རཏྣ");
            // dwags = dags, a bit risqué but should work
            builder.add("དྭགས", "དགས");
            builder.add("\u0FC6", "");
            builder.add("༸", "༧"); // often conflated
        }
        if (lenient || oldtib)
            builder.add("ྀ", "ི");
        if (oldtib) {
            builder.add("ོེ", "ོའི");
            builder.add("བགྱིསྣ", "བགྱིས་ན");
            builder.add("རབལ", "རབ་ལ");
            builder.add("མཆིསྣ", "མཆིས་ན");
            // builder.add("མོལ", "མོ་ལ"); indicated in the doc, but would conflict with other things
            builder.add("ཐོགསླ", "ཐོག་སླ");
            builder.add("ལྕེབསའོ", "ལྕེབས་སོ");
            builder.add("གཤེགསའོ", "གཤེགས་སོ");
            builder.add("བཏགསའོ", "བཏགས་སོ");
            builder.add("ལསྩོགསྟེ", "ལ་སྩོགས་སྟེ");
            // builder.add("མའང", "མ་འང"); indicated but more or less useless
            builder.add("མྱི", "མི");
            builder.add("མྱེ", "མེ");
            builder.add("གསྩན", "གསན");
            builder.add("གསྩང", "གསང");
            builder.add("སྩོགས", "སོགས");
            builder.add("སྩུབ", "སུབ");
            builder.add("སྩང", "སང");
            builder.add("སྩངས", "སངས");
            builder.add("གསྩུག", "གསུག");
            builder.add("བསྩག", "བསག");
            builder.add("མཀ", "མཁ");
            builder.add("མཅ", "མཆ");
            builder.add("མཏ", "མཐ");
            builder.add("མཙ", "མཚ");
            builder.add("འཀ", "འཁ");
            builder.add("འཅ", "འཆ");
            builder.add("འཏ", "འཐ");
            builder.add("འཔ", "འཕ");
            builder.add("འཙ", "འཚ");
            builder.add("དཁ", "དཀ");
            builder.add("དཕ", "དཔ");
            builder.add("གཆ", "གཅ");
            builder.add("གཐ", "གཏ");
            builder.add("གཚ", "གཙ");
            builder.add("བཁ", "བཀ");
            builder.add("བཆ", "བཅ");
            builder.add("བཐ", "བཏ");
            builder.add("བཚ", "བཙ");
            builder.add("སྑ", "སྐ");
            builder.add("སྠ", "སྟ");
            builder.add("སྥ", "སྤ");
            builder.add("སྪ", "སྩ");
            builder.add("རྑ", "རྐ");
            builder.add("རྪ", "རྩ");
            builder.add("རྠ", "རྟ");
            builder.add("ལྑ", "ལྐ");
            builder.add("ལྖ", "ལྕ");
            builder.add("ལྠ", "ལྟ");
            builder.add("ལྥ", "ལྤ");
            builder.add("པྱག", "ཕྱག");
            builder.add("པྱི", "ཕྱི");
            builder.add("པོ་ཉ", "ཕོ་ཉ");
            builder.add("དམག་ཕོན", "དམག་དཔོན");
            builder.add("པོག་པ", "ཕོག་པ");
            builder.add("ཕོ་བྲང", "པོ་བྲང");
            builder.add("བལ་ཕོ", "བལ་པོ");
            builder.add("ཕལ་ཕོ", "ཕལ་པོ");
            builder.add("རྩང་ཅེན", "རྩང་ཆེན");
            builder.add("ལོ་ཕར", "ལོ་པར");
            builder.add("བློན་ཅེ", "བློན་ཆེ");
            builder.add("ཞལ་ཅེ", "ཞལ་ཆེ");
            builder.add("མེར་ཁེ", "མེར་ཀེ");
            builder.add("ལོ་ཆིག", "ལོ་གཅིག");
            builder.add("ཆེད་པོ", "ཆེན་པོ");
            builder.add("ཅེད་པོ", "ཆེན་པོ");
            builder.add("ཅེན་པོ", "ཆེན་པོ");
        }
        return builder.build();
    }
}
