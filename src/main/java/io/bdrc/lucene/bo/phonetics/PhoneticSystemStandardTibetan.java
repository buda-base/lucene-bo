package io.bdrc.lucene.bo.phonetics;

import java.util.HashMap;

public class PhoneticSystemStandardTibetan extends PhoneticSystem {
    
    /*
     * What we do here is use the phonological notation given by Tournadre in the
     * Manual of Standard Tibetan. We do the following adjustments:
     * - we use "+" and "-" to indicate tone instead of diacritics
     * - we use "S" instead of "sh" for convenience (avoiding the confusion with tsh, etc.)
     * - we use "~" to indicate an optional pre-nasalization
     * 
     * See https://github.com/Esukhia/bophono
     */
    
    // pre-nasalization is probably too subtle for our use case
    public boolean ignoreInitialNasalization = true;
    // in a word, syllables after the first have no tone and thus are easy to confuse
    // so we ignore tones, especially since we don't segment into words most of the time
    public boolean ignoreTone = true;
    // in a word, syllables after the first often loose their aspiration. Also low tones
    // often lose their aspiration
    public boolean ignoreAspiration = true;
    // contour tone is too subtle for what we're aiming at
    public boolean ignoreContourTone = true;
    // sa and da have a little influence on the pronounciation (beyond the vowel change)
    // but our intuition is that that too is also too subtle
    public boolean ignoreSDSuffix = true;
    // also ignore long / short vowels
    public boolean ignoreLengthener = true;
    // fold ä into e
    public boolean foldAE = true; 
    
    public void addOnset(final String onset, String phonetic, final boolean canbefinal) {
        if (this.ignoreInitialNasalization && phonetic.startsWith("#"))
            phonetic = phonetic.substring(1);
        if (this.ignoreTone)
            phonetic = phonetic.substring(0, phonetic.length()-1);
        if (this.ignoreAspiration && phonetic.contains("h"))
            phonetic = phonetic.replace("h", "");
        this.onsetTrie.add(onset,  phonetic, canbefinal);
    }
    
    public void addOnset(final String onset, final String phonetic) {
        this.addOnset(onset, phonetic, true);
    }
    
    public void addVowelCoda(final String vowelCoda, String phonetic) {
        if (this.ignoreContourTone && phonetic.endsWith("~"))
            phonetic = phonetic.substring(0, phonetic.length()-1);
        if (this.ignoreSDSuffix && phonetic.endsWith("'"))
            phonetic = phonetic.substring(0, phonetic.length()-1);
        if (this.ignoreLengthener && phonetic.contains(":"))
            phonetic = phonetic.replace(":", "");
        if (this.foldAE && phonetic.contains("ä"))
            phonetic = phonetic.replace("ä", "e");
        this.vowelCodaRoot.put(vowelCoda, phonetic.toCharArray());
    }
    
    public void addSkt(final char c, String sktPhonetic) {
        this.sktPhonetic.put(c, sktPhonetic);
    }
    
    public static final PhoneticSystemStandardTibetan INSTANCE = new PhoneticSystemStandardTibetan();
    
    public PhoneticSystemStandardTibetan() {
        super("");
        this.onsetTrie = new BasicTrie();
        this.addOnset("ཀ", "k+");
        this.addOnset("ཀྱ", "ky+");
        this.addOnset("ཀྲ", "tr+");
        this.addOnset("ཀླ", "l+");
        this.addOnset("དཀ", "k+");
        this.addOnset("དཀྱ", "ky+");
        this.addOnset("དཀྲ", "tr+");
        this.addOnset("བཀ", "k+");
        this.addOnset("བཀྱ", "ky+");
        this.addOnset("བཀྲ", "tr+");
        this.addOnset("བཀླ", "l+");
        this.addOnset("རྐ", "k+");
        this.addOnset("རྐྱ", "ky+");
        this.addOnset("ལྐ", "k+");
        this.addOnset("སྐ", "k+");
        this.addOnset("སྐྱ", "ky+");
        this.addOnset("སྐྲ", "tr+");
        this.addOnset("བརྐ", "k+");
        this.addOnset("བརྐྱ", "ky+");
        this.addOnset("བསྐ", "k+");
        this.addOnset("བསྐྱ", "ky+");
        this.addOnset("བསྐྲ", "tr+");
        this.addOnset("ཁ", "kh+");
        this.addOnset("ཁྱ", "khy+");
        this.addOnset("ཁྲ", "thr+");
        this.addOnset("མཁ", "~kh+");
        this.addOnset("མཁྱ", "~khy+");
        this.addOnset("མཁྲ", "~thr+");
        this.addOnset("འཁ", "~kh+");
        this.addOnset("འཁྱ", "~khy+");
        this.addOnset("འཁྲ", "~thr+");
        this.addOnset("ག", "kh-");
        this.addOnset("གྱ", "khy-");
        this.addOnset("གྲ", "thr-");
        this.addOnset("གླ", "l+");
        this.addOnset("དག", "k-", false);
        this.addOnset("དགྱ", "ky-");
        this.addOnset("དགྲ", "tr-");
        this.addOnset("བག", "k-", false);
        this.addOnset("བགྱ", "ky-");
        this.addOnset("བགྲ", "tr-");
        this.addOnset("མག", "~k-", false);
        this.addOnset("མགྱ", "~ky-");
        this.addOnset("མགྲ", "~tr-");
        this.addOnset("འག", "~k-", false);
        this.addOnset("འགྱ", "~ky-");
        this.addOnset("འགྲ", "~tr-");
        this.addOnset("རྒ", "k-");
        this.addOnset("རྒྱ", "ky-");
        this.addOnset("ལྒ", "k-");
        this.addOnset("སྒ", "k-");
        this.addOnset("སྒྱ", "ky-");
        this.addOnset("སྒྲ", "tr-");
        this.addOnset("བརྒ", "k-");
        this.addOnset("བརྒྱ", "ky-");
        this.addOnset("བསྒ", "k-");
        this.addOnset("བསྒྱ", "ky-");
        this.addOnset("བསྒྲ", "tr-");
        this.addOnset("ང", "ng-");
        this.addOnset("དང", "ng+", false);
        this.addOnset("མང", "~ng+", false);
        this.addOnset("རྔ", "ng+");
        this.addOnset("ལྔ", "ng+");
        this.addOnset("སྔ", "ng+");
        this.addOnset("བརྔ", "ng+");
        this.addOnset("བསྔ", "ng+");
        this.addOnset("ཅ", "c+");
        this.addOnset("གཅ", "c+");
        this.addOnset("བཅ", "c+");
        this.addOnset("ལྕ", "c+");
        this.addOnset("ཆ", "ch+");
        this.addOnset("མཆ", "~ch+");
        this.addOnset("འཆ", "~ch+");
        this.addOnset("ཇ", "ch-");
        this.addOnset("མཇ", "~c-");
        this.addOnset("འཇ", "~c-");
        this.addOnset("རྗ", "c-");
        this.addOnset("ལྗ", "~c-");
        this.addOnset("བརྗ", "c-");
        this.addOnset("ཉ", "ny-");
        this.addOnset("གཉ", "ny+");
        this.addOnset("མཉ", "~ny+");
        this.addOnset("རྙ", "ny+");
        this.addOnset("སྙ", "ny+");
        this.addOnset("བརྙ", "ny+");
        this.addOnset("བསྙ", "ny+");
        this.addOnset("ཏ", "t+");
        this.addOnset("གཏ", "t+");
        this.addOnset("བཏ", "t+");
        this.addOnset("རྟ", "t+");
        this.addOnset("ལྟ", "~t+");
        this.addOnset("སྟ", "t+");
        this.addOnset("བརྟ", "t+");
        this.addOnset("བལྟ", "t+");
        this.addOnset("བསྟ", "t+");
        this.addOnset("ཐ", "th+");
        this.addOnset("མཐ", "~th+");
        this.addOnset("འཐ", "~th+");
        this.addOnset("ད", "th-");
        this.addOnset("དྲ", "thr-");
        this.addOnset("གད", "t-", false);
        this.addOnset("བད", "t-", false);
        this.addOnset("མད", "~t-", false);
        this.addOnset("འད", "~t-", false);
        this.addOnset("འདྲ", "~tr-");
        this.addOnset("རྡ", "t-");
        this.addOnset("ལྡ", "~t-");
        this.addOnset("སྡ", "t-");
        this.addOnset("བརྡ", "t-");
        this.addOnset("བལྡ", "t-");
        this.addOnset("བསྡ", "t-");
        this.addOnset("ན", "n-");
        this.addOnset("གན", "n+", false);
        this.addOnset("མན", "~n+", false);
        this.addOnset("རྣ", "n+");
        this.addOnset("སྣ", "n+");
        this.addOnset("བརྣ", "n+");
        this.addOnset("བསྣ", "n+");
        this.addOnset("པ", "p+");
        this.addOnset("པྱ", "c+");
        this.addOnset("པྲ", "tr+");
        this.addOnset("དཔ", "p+");
        this.addOnset("དཔྱ", "c+");
        this.addOnset("དཔྲ", "tr+");
        this.addOnset("ལྤ", "p+");
        this.addOnset("སྤ", "p+");
        this.addOnset("སྤྱ", "c+");
        this.addOnset("སྤྲ", "tr+");
        this.addOnset("ཕ", "ph+");
        this.addOnset("ཕྱ", "ch+");
        this.addOnset("ཕྲ", "thr+");
        this.addOnset("འཕ", "~ph+");
        this.addOnset("འཕྱ", "~ch+");
        this.addOnset("འཕྲ", "~thr+");
        this.addOnset("བ", "ph-");
        this.addOnset("བྱ", "ch-");
        this.addOnset("བྲ", "thr-");
        this.addOnset("བླ", "l+");
        this.addOnset("དབ", "+", false);
        this.addOnset("དབྱ", "y+");
        this.addOnset("དབྲ", "r+");
        this.addOnset("འབ", "~p-", false);
        this.addOnset("འབྱ", "~c-");
        this.addOnset("འབྲ", "~tr-");
        this.addOnset("རྦ", "p-");
        this.addOnset("ལྦ", "p-");
        this.addOnset("སྦ", "p-");
        this.addOnset("སྦྱ", "c-");
        this.addOnset("སྦྲ", "tr-");
        this.addOnset("མ", "m-");
        this.addOnset("མྱ", "ny-");
        this.addOnset("དམ", "m+", false);
        this.addOnset("དམྱ", "ny+");
        this.addOnset("རྨ", "m+");
        this.addOnset("རྨྱ", "ny+");
        this.addOnset("སྨ", "m+");
        this.addOnset("སྨྱ", "ny+");
        this.addOnset("ཙ", "ts+");
        this.addOnset("གཙ", "ts+");
        this.addOnset("བཙ", "ts+");
        this.addOnset("རྩ", "ts+");
        this.addOnset("སྩ", "ts+");
        this.addOnset("བརྩ", "ts+");
        this.addOnset("བསྩ", "ts+");
        this.addOnset("ཚ", "tsh+");
        this.addOnset("མཚ", "~tsh+");
        this.addOnset("འཚ", "~tsh+");
        this.addOnset("ཛ", "tsh-");
        this.addOnset("མཛ", "~ts-");
        this.addOnset("འཛ", "~ts-");
        this.addOnset("རྫ", "ts-");
        this.addOnset("བརྫ", "ts-");
        this.addOnset("ཝ", "w-");
        this.addOnset("ཞ", "S-");
        this.addOnset("གཞ", "S-");
        this.addOnset("བཞ", "S-");
        this.addOnset("ཟ", "s-");
        this.addOnset("ཟླ", "~t-");
        this.addOnset("གཟ", "s-");
        this.addOnset("བཟ", "s-");
        this.addOnset("བཟླ", "t-");
        this.addOnset("འ", "-");
        this.addOnset("ཡ", "y-");
        this.addOnset("གཡ", "y+");
        this.addOnset("ར", "r-");
        this.addOnset("རླ", "l+");
        this.addOnset("བརླ", "l+");
        this.addOnset("ལ", "l-");
        this.addOnset("ཤ", "S+");
        this.addOnset("གཤ", "S+");
        this.addOnset("བཤ", "S+");
        this.addOnset("ས", "s+");
        this.addOnset("སྲ", "s+");
        this.addOnset("སླ", "l+");
        this.addOnset("གས", "s+", false);
        this.addOnset("བས", "s+", false);
        this.addOnset("བསྲ", "s+");
        this.addOnset("བསླ", "l+");
        this.addOnset("ཧ", "h+");
        this.addOnset("ཧྲ", "rh+");
        this.addOnset("ལྷ", "lh+");
        this.addOnset("ཨ", "+");
        this.addOnset("བགླ", "l+");
        this.addOnset("མྲ", "m+");
        this.addOnset("སྨྲ", "m+");
        this.addOnset("ཏྲ", "tr+");
        this.addOnset("བརྟ", "t+");
        this.addOnset("ཐྲ", "thr+");
        this.addOnset("སྣྲ", "n+");
        this.addOnset("ཀྭ", "k+");
        this.addOnset("བཀྭ", "k+");
        this.addOnset("ཁྭ", "kh+");
        this.addOnset("གྭ", "kh-");
        this.addOnset("གྲྭ", "thr-");
        this.addOnset("བཅྭ", "c+");
        this.addOnset("ཉྭ", "ny-");
        this.addOnset("ཏྭ", "t+");
        this.addOnset("ཐྭ", "th+");
        this.addOnset("དྭ", "th-");
        this.addOnset("དྲྭ", "thr-");
        this.addOnset("ཕྱྭ", "ch+");
        this.addOnset("མྭ", "m-");
        this.addOnset("ཙྭ", "ts+");
        this.addOnset("རྩྭ", "ts+");
        this.addOnset("ཚྭ", "tsh+");
        this.addOnset("ཛྭ", "tsh-");
        this.addOnset("ཞྭ", "S-");
        this.addOnset("ཟྭ", "s-");
        this.addOnset("རྭ", "r-");
        this.addOnset("ལྭ", "l-");
        this.addOnset("ལྷྭ", "lh+");
        this.addOnset("ཤྭ", "S+");
        this.addOnset("སྟྭ", "t+");
        this.addOnset("སྭ", "s+");
        this.addOnset("བསྭ", "s+");
        this.addOnset("ཧྭ", "h+");
        
        this.vowelCodaRoot = new HashMap<>();
        this.addVowelCoda("", "a");
        this.addVowelCoda("འ", "a:");
        this.addVowelCoda("ག", "ak");
        this.addVowelCoda("གས", "ak");
        this.addVowelCoda("ང", "ang");
        this.addVowelCoda("ངས", "ang~");
        this.addVowelCoda("ད", "ä'");
        this.addVowelCoda("ན", "än");
        this.addVowelCoda("བ", "ap");
        this.addVowelCoda("བས", "ap");
        this.addVowelCoda("མ", "am");
        this.addVowelCoda("མས", "am~");
        this.addVowelCoda("ལ", "äl");
        this.addVowelCoda("འི", "ä");
        this.addVowelCoda("འིའོ", "aio");
        this.addVowelCoda("འོ", "ao");
        this.addVowelCoda("འང", "aang");
        this.addVowelCoda("འམ", "aam");
        this.addVowelCoda("ར", "ar");
        this.addVowelCoda("ས", "ä'");
        this.addVowelCoda("ི", "i");
        this.addVowelCoda("ིག", "ik");
        this.addVowelCoda("ིགས", "ik");
        this.addVowelCoda("ིང", "ing");
        this.addVowelCoda("ིངས", "ing~");
        this.addVowelCoda("ིད", "i'");
        this.addVowelCoda("ིན", "in");
        this.addVowelCoda("ིབ", "ip");
        this.addVowelCoda("ིབས", "ip");
        this.addVowelCoda("ིམ", "im");
        this.addVowelCoda("ིམས", "im~");
        this.addVowelCoda("ིལ", "il");
        this.addVowelCoda("ིའི", "i:");
        this.addVowelCoda("ིའིའོ", "i:o");
        this.addVowelCoda("ིའོ", "io");
        this.addVowelCoda("ིའང", "iang");
        this.addVowelCoda("ིའམ", "iam");
        this.addVowelCoda("ིར", "ir");
        this.addVowelCoda("ིས", "i'");
        this.addVowelCoda("ུ", "u");
        this.addVowelCoda("ུག", "uk");
        this.addVowelCoda("ུགས", "uk");
        this.addVowelCoda("ུང", "ung");
        this.addVowelCoda("ུངས", "ung~");
        this.addVowelCoda("ུད", "ü'");
        this.addVowelCoda("ུན", "ün");
        this.addVowelCoda("ུབ", "up");
        this.addVowelCoda("ུབས", "up");
        this.addVowelCoda("ུམ", "um");
        this.addVowelCoda("ུམས", "um~");
        this.addVowelCoda("ུལ", "ül");
        this.addVowelCoda("ུའི", "ü");
        this.addVowelCoda("ུའིའོ", "uio");
        this.addVowelCoda("ུའོ", "uo");
        this.addVowelCoda("ུའང", "uang");
        this.addVowelCoda("ུའམ", "uam");
        this.addVowelCoda("ུར", "ur");
        this.addVowelCoda("ུས", "ü'");
        this.addVowelCoda("ེ", "e");
        this.addVowelCoda("ེག", "ek");
        this.addVowelCoda("ེགས", "ek");
        this.addVowelCoda("ེང", "eng");
        this.addVowelCoda("ེངས", "eng~");
        this.addVowelCoda("ེད", "e'");
        this.addVowelCoda("ེན", "en");
        this.addVowelCoda("ེབ", "ep");
        this.addVowelCoda("ེབས", "ep");
        this.addVowelCoda("ེམ", "em");
        this.addVowelCoda("ེམས", "em~");
        this.addVowelCoda("ེལ", "el");
        this.addVowelCoda("ེའི", "e");
        this.addVowelCoda("ེའིའོ", "eio");
        this.addVowelCoda("ེའོ", "eo");
        this.addVowelCoda("ེའང", "eang");
        this.addVowelCoda("ེའམ", "eam");
        this.addVowelCoda("ེར", "er");
        this.addVowelCoda("ེས", "e'");
        this.addVowelCoda("ོ", "o");
        this.addVowelCoda("ོག", "ok");
        this.addVowelCoda("ོགས", "ok");
        this.addVowelCoda("ོང", "ong");
        this.addVowelCoda("ོངས", "ong~");
        this.addVowelCoda("ོད", "ö'");
        this.addVowelCoda("ོན", "ön");
        this.addVowelCoda("ོབ", "op");
        this.addVowelCoda("ོབས", "op");
        this.addVowelCoda("ོམ", "om");
        this.addVowelCoda("ོམས", "om~");
        this.addVowelCoda("ོལ", "öl");
        this.addVowelCoda("ོའི", "ö");
        this.addVowelCoda("ོའིའོ", "oio");
        this.addVowelCoda("ོའོ", "oo");
        this.addVowelCoda("ོའང", "oang");
        this.addVowelCoda("ོའམ", "oam");
        this.addVowelCoda("ོར", "or");
        this.addVowelCoda("ོས", "ö'");
        this.addVowelCoda("འུ", "au");
        this.addVowelCoda("འུའི", "au");
        this.addVowelCoda("འུའིའོ", "auio");
        this.addVowelCoda("འུའོ", "auo");
        this.addVowelCoda("འུའང", "auang");
        this.addVowelCoda("འུའམ", "auam");
        this.addVowelCoda("འུར", "aur");
        this.addVowelCoda("འུས", "aü'");
        this.addVowelCoda("ིའུ", "iu");
        this.addVowelCoda("ིའུའི", "iu");
        this.addVowelCoda("ིའུའིའོ", "iuio");
        this.addVowelCoda("ིའུའོ", "iuo");
        this.addVowelCoda("ིའུའང", "iuang");
        this.addVowelCoda("ིའུའམ", "iuam");
        this.addVowelCoda("ིའུར", "iur");
        this.addVowelCoda("ིའུས", "iü'");
        this.addVowelCoda("ུའུ", "u");
        this.addVowelCoda("ུའུའི", "ui");
        this.addVowelCoda("ུའུའིའོ", "uio");
        this.addVowelCoda("ུའུའོ", "uo");
        this.addVowelCoda("ུའུའང", "uang");
        this.addVowelCoda("ུའུའམ", "uam");
        this.addVowelCoda("ུའུར", "uur");
        this.addVowelCoda("ུའུས", "uü'");
        this.addVowelCoda("ེའུ", "eu");
        this.addVowelCoda("ེའུའི", "eui");
        this.addVowelCoda("ེའུའིའོ", "euio");
        this.addVowelCoda("ེའུའོ", "euo");
        this.addVowelCoda("ེའུའང", "euang");
        this.addVowelCoda("ེའུའམ", "euam");
        this.addVowelCoda("ེའུར", "eur");
        this.addVowelCoda("ེའུས", "eü'");
        this.addVowelCoda("ོའུ", "ou");
        this.addVowelCoda("ོའུའི", "oui");
        this.addVowelCoda("ོའུའིའོ", "ouio");
        this.addVowelCoda("ོའུའོ", "ouo");
        this.addVowelCoda("ོའུའང", "ouang");
        this.addVowelCoda("ོའུའམ", "ouam");
        this.addVowelCoda("ོའུར", "our");
        this.addVowelCoda("ོའུས", "oü'");
        
        this.sktPhonetic = new HashMap<>();
        this.addSkt('ཀ', "g");
        this.addSkt('ཁ', "g");
        this.addSkt('ག', "g");
        this.addSkt('ང', "N");
        this.addSkt('ཅ', "c");
        this.addSkt('ཆ', "c");
        this.addSkt('ཇ', "c");
        this.addSkt('ཉ', "Y");
        this.addSkt('ཐ', "d");
        this.addSkt('ཏ', "d");
        this.addSkt('ད', "d");
        this.addSkt('ན', "n");
        this.addSkt('པ', "b");
        this.addSkt('ཕ', "b");
        this.addSkt('བ', "b");
        this.addSkt('མ', "m");
        this.addSkt('ཙ', "c"); // ts -> c
        this.addSkt('ཚ', "c");
        this.addSkt('ཛ', "c");
        this.addSkt('ཝ', "b");
        this.addSkt('ཞ', "S");
        this.addSkt('ཟ', "s");
        this.addSkt('འ', "");
        this.addSkt('ཡ', "y");
        this.addSkt('ར', "r");
        this.addSkt('ལ', "l");
        this.addSkt('ཤ', "S");
        this.addSkt('ཥ', "S");
        this.addSkt('ས', "s");
        this.addSkt('ཧ', "");
        this.addSkt('ཨ', "");
        this.addSkt('ཪ', "r");
        this.addSkt('ཱ', "");
        this.addSkt('ི', "i");
        this.addSkt('ུ', "u");
        this.addSkt('ེ', "e");
        this.addSkt('ཻ', "e");
        this.addSkt('ོ', "o");
        this.addSkt('ཽ', "o");
        this.addSkt('ཾ', "n");
        this.addSkt('ཿ', "");
        this.addSkt('ྀ', "i");
        this.addSkt('ྂ', "n");
        this.addSkt('ྃ', "n");
        this.addSkt('྄', "");
        this.addSkt('ྐ', "g");
        this.addSkt('ྑ', "g");
        this.addSkt('ྒ', "g");
        this.addSkt('ྔ', "N");
        this.addSkt('ྕ', "c");
        this.addSkt('ྖ', "c");
        this.addSkt('ྗ', "c");
        this.addSkt('ྙ', "Y");
        this.addSkt('ྟ', "d");
        this.addSkt('ྠ', "d");
        this.addSkt('ྡ', "d");
        this.addSkt('ྣ', "n");
        this.addSkt('ྤ', "b");
        this.addSkt('ྥ', "b");
        this.addSkt('ྦ', "b");
        this.addSkt('ྨ', "m");
        this.addSkt('ྩ', "c");
        this.addSkt('ྪ', "c");
        this.addSkt('ྫ', "c");
        this.addSkt('ྭ', "");
        this.addSkt('ྮ', "S");
        this.addSkt('ྯ', "s");
        this.addSkt('ྰ', "");
        this.addSkt('ྱ', "y");
        this.addSkt('ྲ', "r");
        this.addSkt('ླ', "l");
        this.addSkt('ྴ', "S");
        this.addSkt('ྵ', "S");
        this.addSkt('ྶ', "s");
        this.addSkt('ྷ', "");
        this.addSkt('ྸ', "");
        this.addSkt('ྺ', "b");
        this.addSkt('ྻ', "y");
        this.addSkt('ྼ', "r");
    }

}
