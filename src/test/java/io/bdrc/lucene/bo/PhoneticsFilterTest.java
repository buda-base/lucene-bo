package io.bdrc.lucene.bo;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.charfilter.MappingCharFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.junit.Assert;
import org.junit.Test;

import io.bdrc.lucene.bo.phonetics.EnglishPhoneticCharMapFilter;
import io.bdrc.lucene.bo.phonetics.EnglishPhoneticFilter;
import io.bdrc.lucene.bo.phonetics.EnglishPhoneticRegexFilter;
import io.bdrc.lucene.bo.phonetics.EnglishPhoneticTokenizer;
import io.bdrc.lucene.bo.phonetics.LowerCaseCharFilter;
import io.bdrc.lucene.bo.phonetics.StandardTibetanPhoneticFilter;

import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.pattern.PatternReplaceCharFilter;

public class PhoneticsFilterTest {

    private static String applyCharMapFilter(String input) throws IOException {
        MappingCharFilter filter = new EnglishPhoneticCharMapFilter(new StringReader(input));
        StringBuilder result = new StringBuilder();
        char[] buffer = new char[1024];
        int length;
        
        // Read and process the input through the filter
        while ((length = filter.read(buffer)) != -1) {
            result.append(buffer, 0, length);
        }
        filter.close();
        return result.toString();
    }
    
    public static void testPhoneticMapping(final String input, final String expected) throws IOException {
            String actualOutput = applyCharMapFilter(input);
            assertEquals("fail on "+input, expected, actualOutput);
    }
    
    @Test
    public void testCharMapFilter() throws IOException {
        testPhoneticMapping("dzongsar", "ToNsar");
        testPhoneticMapping("tenzin", "denTin");
        testPhoneticMapping("lobzang", "lo saN");
        testPhoneticMapping("lopzang", "lo saN");
    }
    
    static void assertTokenStream(final TokenStream tokenStream, final List<String> expected) {
        try {
            List<String> termList = new ArrayList<String>();
            CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            while (tokenStream.incrementToken()) {
                termList.add(charTermAttribute.toString());
            }
            System.out.println(String.join(" ", termList));
            assertThat(termList, is(expected));
        } catch (IOException e) {
            assertTrue(false);
        }
    }
    
    static TokenStream stringToTokenStream(final String s) throws IOException {
        final WhitespaceTokenizer tokenizer = new WhitespaceTokenizer();
        tokenizer.setReader(new StringReader(s));
        
        // Create the filter and pass the tokenizer
        final TokenStream tokenStream = new StandardTibetanPhoneticFilter(tokenizer);
        tokenStream.reset();
        return tokenStream;
    }
    
    @Test
    public void testStandardTibetanSimple() throws IOException {
        assertTokenStream(stringToTokenStream("གཤན བཤན རྟེན བསྟན ཐེན"), Arrays.asList("Sen", "Sen", "ten", "ten", "ten"));
    }
    
    static TokenStream tokenize(Reader reader, Tokenizer tokenizer) throws IOException {
        tokenizer.close();
        tokenizer.end();
        tokenizer.setReader(reader);
        tokenizer.reset();
        return tokenizer;
    }
    
    public List<List<String>> getQueryTokens(final String input) throws IOException {
        final List<List<String>> tokens = new ArrayList<>();
        Reader reader = new StringReader(input);
        reader = new LowerCaseCharFilter(reader);
        reader = new EnglishPhoneticCharMapFilter(reader);
        reader = EnglishPhoneticRegexFilter.plugFilters(reader);
        TokenStream tokenStream = tokenize(reader, new EnglishPhoneticTokenizer());
        CharTermAttribute charTermAttr = tokenStream.addAttribute(CharTermAttribute.class);
        PositionIncrementAttribute posIncrAttr = tokenStream.addAttribute(PositionIncrementAttribute.class);
        //tokenStream.reset();
        List<String> lastPosition = new ArrayList<>();
        while (tokenStream.incrementToken()) {
            if (posIncrAttr.getPositionIncrement() > 0) {
                lastPosition = new ArrayList<>();
                tokens.add(lastPosition);
            }
            lastPosition.add(charTermAttr.toString());
        }
        tokenStream.end();
        reader.close();
        return tokens;
    }
    
    public List<List<String>> getIndexTokens(final String input) throws IOException {
        final List<List<String>> tokens = new ArrayList<>();
        Reader reader = new StringReader(input);
        reader = new TibEwtsFilter(reader);
        reader = new TibCharFilter(reader, true, true);
        reader = TibPattFilter.plugFilters(reader);
        TokenStream tokenStream = tokenize(reader, new TibSyllableTokenizer());
        tokenStream = new EnglishPhoneticFilter(tokenStream);
        CharTermAttribute charTermAttr = tokenStream.addAttribute(CharTermAttribute.class);
        PositionIncrementAttribute posIncrAttr = tokenStream.addAttribute(PositionIncrementAttribute.class);
        //tokenStream.reset();
        List<String> lastPosition = new ArrayList<>();
        while (tokenStream.incrementToken()) {
            if (posIncrAttr.getPositionIncrement() > 0) {
                lastPosition = new ArrayList<>();
                tokens.add(lastPosition);
            }
            lastPosition.add(charTermAttr.toString());
        }
        tokenStream.end();
        reader.close();
        return tokens;
    }
    
    // Function to check if the token streams from both analyzers match
    public void checkMatch(final String queryInput, final String indexInput) throws IOException {
        final List<List<String>> queryTokens = getQueryTokens(queryInput);
        final List<List<String>> indexTokens = getIndexTokens(indexInput);
     // Ensure both lists have the same number of positions
        Assert.assertEquals(String.format("The number of positions in query and index tokens are not the same for %s -> %s, %s %s", queryInput, indexInput, queryTokens, indexTokens), 
                            queryTokens.size(), indexTokens.size());

        // Iterate through each position
        for (int position = 0; position < queryTokens.size(); position++) {
            List<String> queryPositionTokens = queryTokens.get(position);
            List<String> indexPositionTokens = indexTokens.get(position);

            // Find any common token at the current position
            boolean foundMatch = queryPositionTokens.stream()
                    .anyMatch(indexPositionTokens::contains);

            // If no common token is found, assert failure with detailed output
            Assert.assertTrue(String.format(
                "No matching token found at position %d for %s -> %s. Query tokens: %s, Index tokens: %s",
                position, queryInput, indexInput, queryPositionTokens, indexPositionTokens), foundMatch);
        }
    }
    
    @Test
    public void integratedPhoneticTest() throws IOException {
        checkMatch("Dalailama", "tA la'i bla ma");
        checkMatch("Dalaï Lama", "tA la'i bla ma");
        checkMatch("Kangyur", "bka' 'gyur");
        checkMatch("Kanjur", "bka' 'gyur");
        checkMatch("Ösel", "'od gsal");
        checkMatch("Wösel", "'od gsal");
        checkMatch("Selwè", "gsal ba'i");
        checkMatch("Padma Jungné", "pad+ma 'byung gnas");
        checkMatch("Péma Jungné", "pad+ma 'byung gnas");
        checkMatch("Tendzin Gyatso", "bstan 'dzin rgya mtsho");
        checkMatch("Tenzin Gyamtso", "bstan 'dzin rgya mtsho");
        checkMatch("Pelzang", "dpal bzang");
        checkMatch("Pelsang", "dpal bzang");
        checkMatch("Dulzin", "'dul 'dzin");
        checkMatch("Kunzang", "kun bzang");
        checkMatch("Kelzang", "bkal bzang");
        checkMatch("Kelsang", "bkal bzang");
        checkMatch("Panchen Lama", "paN chen bla ma");
        checkMatch("Paṇchen Lama", "paN chen bla ma");
        checkMatch("Phurpa Netik", "phur pa gnad tig");
        checkMatch("Jamyang Khyentse Wangpo", "'jam dbyangs mkhyen brtse'i dbang po");
        checkMatch("Marpa Lotsawa", "mar pa lo tsA ba");
        checkMatch("Marpa Lotsawa", "mar pa lotsA ba");
        checkMatch("Tsokar Gyaltsen", "mtsho skar rgyal mtshan");
        checkMatch("Tsokar Gyeltsen", "mtsho skar rgyal mtshan");
        checkMatch("Samding Dorje Phagmo", "bsam sding rdo rje phag mo");
        checkMatch("Orgyen", "o rgyan");
        checkMatch("Khandro Nyingtik", "mkha' 'gro snying thig");
        checkMatch("vajra", "ba dz+ra");
        checkMatch("Sakya Pandita", "sa skya paN+Di ta");
        checkMatch("Gyalwang Drukpa", "rgyal dbang 'brug pa");
        checkMatch("Gyalwa Gyamtso", "rgyal ba rgya mtsho");
        checkMatch("Ladakh", "la dwags");
        checkMatch("Trinley", "'phrin les");
        checkMatch("Wanggyal", "dbang rgyal");
        checkMatch("Wangyal", "dbang rgyal");
        checkMatch("Rangjung Kunkhyab", "rang byung kun khyab");
        checkMatch("Rinchen Terdzö", "rin chen gter mdzod");
        checkMatch("Lhatsün Jangchub Ö", "lha btsun byang chub 'od");
        checkMatch("Lotsawa", "lotsa ba");
        checkMatch("Katog", "kaHthog");
        checkMatch("kunga", "kun dga'");
        checkMatch("Drupgyü Nyima", "sgrub brgyud nyi ma");
        checkMatch("Karma", "kar+ma");
        checkMatch("Denkarma", "ldan dkar ma");
        checkMatch("Trisong Detsen", "khri srong lde btsan");
        checkMatch("Trisong Detsen", "khri srong lde'u btsan");
        checkMatch("Wangdud", "dbang bdud"); // d suffixes not supported in the phonetic
        checkMatch("Choegyal", "chos rgyal");
        checkMatch("Mip'am", "mi pham");
        checkMatch("Mipham", "mi pham");
        checkMatch("Mipam", "mi pham");
        checkMatch("Mingyur", "mi 'gyur");
        checkMatch("Karma Pakshi", "kar+ma pak+shi");
        checkMatch("Ratna Lingpa", "rat+na gling pa");
        checkMatch("Bande Kawa Paltsek", "ban+de ska ba dpal brtsegs");
        checkMatch("Paltsek Rakshita", "dpal brtsegs rak+Shi ta");
        checkMatch("Shri Singha", "shrI sing+ha");
        checkMatch("Dzikar", "'dzi sgar");
        checkMatch("Chönyi Gyamtso", "chos nyid rgya mtsho");
        checkMatch("Chönyid Gyamtso", "chos nyid rgya mtsho");
        checkMatch("Chöni Gyamtso", "chos nyid rgya mtsho");
        checkMatch("Chöny Gyamtso", "chos nyid rgya mtsho");
        checkMatch("Acarya", "A tsar+yA");
        checkMatch("Lopön", "slob dpon");
        checkMatch("Zopa", "bzo pa");
        checkMatch("Guru", "gu ru");
        checkMatch("Zhalu", "zhwa lu");
        // checkMatch("Sangdo Palri", "zangs mdog dpal ri"); // not indicating g suffix not supported (yet?)
        checkMatch("Chagya Chenpo", "phyag rgya chen po");
        checkMatch("Chakchen Gauma", "phyag chen ga'u ma");
        checkMatch("Samye Gompa", "bsam yas dgon pa");
        checkMatch("Lochen Dharmashri", "lo chen d+harma shrI");
        checkMatch("Mengagde", "man ngag sde");
        checkMatch("Senyig", "gsan yig");
        checkMatch("Minyak", "mi nyag");
        checkMatch("Gomnyam Drugpa", "sgom nyams drug pa");
        checkMatch("Longchen Nyingtik", "klong chen snying thig");
        checkMatch("Jikmé Lingpa", "'jigs med gling pa");
        checkMatch("Jigmé Lingpa", "'jigs med gling pa");
        checkMatch("Tulku Thondup", "sprul sku don grub");
        checkMatch("Golok", "mgo log");
        checkMatch("Derge", "sde dge");
        checkMatch("Jamyang Khyentse", "'jam dbyangs mkhyen brtse");
        checkMatch("Milarepa", "mi la ras pa");
        checkMatch("Tsongkhapa", "tsong kha pa");
        checkMatch("Lobsang", "blo bzang");
        checkMatch("Lobsang Chokyi Gyaltsen", "blo bzang chos kyi rgyal mtshan");
        checkMatch("Pema Norbu", "pad ma nor bu");
        checkMatch("Thubten Chodron", "thub bstan chos sgron");
        checkMatch("Lobsang Gyatso", "blo bzang rgya mtsho");
        checkMatch("Gendun Chophel", "dge 'dun chos 'phel");
        checkMatch("Lobsang Sonam", "blo bzang bsod nams");
        checkMatch("Lobsang Dondrub", "blo bzang don grub");
        checkMatch("Gyalwa Khedrup", "rgyal ba mkhas grub");
        checkMatch("Lodro Gyatso", "blo gros rgya mtsho");
        checkMatch("Lobsang Yeshe", "blo bzang ye she");
        checkMatch("Pema Thinley", "pad ma 'phrin las");
        checkMatch("Rigzin Gyatso", "rig 'dzin rgya mtsho");
        checkMatch("Thubten Gyaltsen", "thub bstan rgyal mtshan");
        checkMatch("Lobsang Palden", "blo bzang dpal ldan");
        checkMatch("Lodro Gyaltsen", "blo gros rgyal mtshan");
        checkMatch("Rigzin Chokdrub", "rig 'dzin mchog 'grub");
        checkMatch("Lodro Pelbar", "blo gros dpal 'bar");
        checkMatch("Lobsang Rinchen", "blo bzang rin chen");
        checkMatch("Lobsang Jampa", "blo bzang 'jam pa");
        checkMatch("Pema Yeshe", "pad ma ye shes");
        checkMatch("Lobsang Lungtog", "blo bzang lung rtogs");
        checkMatch("Lobsang Chogyal", "blo bzang chos rgyal");
        checkMatch("Lobsang Thinley", "blo bzang 'phrin las");
        checkMatch("Rigzin Chogyal", "rig 'dzin chos rgyal");
        checkMatch("Pema Osel", "pad ma 'od gsal");
        checkMatch("Thubten Jampal", "thub bstan 'jam dpal");
        checkMatch("Rigzin Pema", "rig 'dzin pad ma");
        checkMatch("Lodro Palden", "blo gros dpal ldan");
        checkMatch("Lodro Sangpo", "blo gros bzang po");
        checkMatch("Thubten Thinley", "thub bstan 'phrin las");
        checkMatch("Lodro Gyatso", "blo gros rgya mtsho");
        checkMatch("Lobsang Yeshe", "blo bzang ye shes");
        checkMatch("Lodro Dargye", "blo gros dar rgyas");
        checkMatch("Pema Dorje", "pad ma rdo rje");
        checkMatch("Thubten Chopal", "thub bstan chos dpal");
        checkMatch("Lobsang Chokyi Gyaltsen", "blo bzang chos kyi rgyal mtshan");
        checkMatch("Rigzin Dorje", "rig 'dzin rdo rje");
        checkMatch("Lobsang Pema", "blo bzang pad ma");
        checkMatch("Lobsang Tobgyal", "blo bzang stobs rgyal");
        checkMatch("Rigzin Jampal", "rig 'dzin 'jam dpal");
        checkMatch("Lodro Thubten", "blo gros thub bstan");
        checkMatch("Lobsang Sherab", "blo bzang shes rab");
        checkMatch("Pema Chophel", "pad ma chos 'phel");
        checkMatch("Lodro Ozer", "blo gros 'od zer");
        checkMatch("Rigzin Tenzin", "rig 'dzin bstan 'dzin");
        checkMatch("Lobsang Drakpa", "blo bzang grags pa");
        checkMatch("Lodro Palden", "blo gros dpal ldan");
        checkMatch("Pema Tobgyal", "pad ma stobs rgyal");
        checkMatch("Lodro Chokdrub", "blo gros mchog grub");
        checkMatch("Rigzin Lodro", "rig 'dzin blo gros");
        checkMatch("Lobsang Nyima", "blo bzang nyi ma");
        checkMatch("Lobsang Pema Ozer", "blo bzang pad ma 'od zer");
        checkMatch("Lodro Rinchen", "blo gros rin chen");
        checkMatch("Pema Dechen", "pad ma bde chen");
        checkMatch("Lobsang Pema Chok", "blo bzang pad ma mchog");
        checkMatch("Rigzin Pelbar", "rig 'dzin dpal 'bar");
        checkMatch("Lobsang Sonam", "blo bzang bsod nams");
        checkMatch("Lodro Jampal", "blo gros 'jam dpal");
        checkMatch("Lobsang Osel", "blo bzang 'od gsal");
        checkMatch("Lodro Pema", "blo gros pad ma");
        checkMatch("Rigzin Norbu", "rig 'dzin nor bu");
        checkMatch("Lobsang Dargye", "blo bzang dar rgyas");
        checkMatch("Rigzin Chophel", "rig 'dzin chos 'phel");
        checkMatch("Lobsang Tenpa", "blo bzang bstan pa");
        checkMatch("Lobsang Rigzin", "blo bzang rig 'dzin");
        checkMatch("Pema Thinley", "pad ma 'phrin las");
        checkMatch("Lobsang Nyima", "blo bzang nyi ma");
        checkMatch("Rigzin Pema", "rig 'dzin pad ma");
        checkMatch("Lobsang Tobgyal", "blo bzang stobs rgyal");
        checkMatch("Rigzin Jampal", "rig 'dzin 'jam dpal");
        checkMatch("Lodro Thubten", "blo gros thub bstan");
        checkMatch("Lobsang Sherab", "blo bzang shes rab");
        checkMatch("Pema Chophel", "pad ma chos 'phel");
        checkMatch("Lodro Ozer", "blo gros 'od zer");
        checkMatch("Lobsang Drakpa", "blo bzang grags pa");
        checkMatch("Thubten Gyatso", "thub bstan rgya mtsho");
        checkMatch("Dorje Chang", "rdo rje 'chang");
        checkMatch("Lhamo Gyalmo", "lha mo rgyal mo");
        checkMatch("Gyakar", "rgya gar");
        checkMatch("Tsangpo", "gtsang po");
        checkMatch("Lhasa", "lha sa");
        checkMatch("Gyangbum", "gyang 'bum");
        checkMatch("Kardze", "dkar mdzes");
        checkMatch("Yuthok Yonten", "g.yu thog yon tan");
        checkMatch("Drigung", "'bri gung");
        checkMatch("Trisong Detsen", "khri srong lde btsan");
        checkMatch("Lhatse", "lha rtse");
        checkMatch("Lhatsun", "lha btsun");
        checkMatch("Tenzin Chogyal", "bstan 'dzin chos rgyal");
        checkMatch("Domé", "mdo smad");
        checkMatch("Nima", "nyi ma");
        checkMatch("Nyima", "nyi ma");
        checkMatch("Me Druk", "smad 'brug");
        checkMatch("Shigatse", "gzhis ka rtse");
        checkMatch("Yeru", "g.yas ru");
        checkMatch("Yoru", "g.yo ru");
        checkMatch("Choying", "chos dbyings");
        checkMatch("Pema Tsuklak", "padma gtsug lag");
        checkMatch("Chimpu", "mchims phu");
        checkMatch("Drepung", "'bras spungs");
        checkMatch("Choklha", "mchog lha");
        checkMatch("Gyaltse", "rgyal rtse");
        checkMatch("Samye", "bsam yas");
        checkMatch("Ngaba", "nga ba");
        checkMatch("Potrang", "pho brang");
        checkMatch("Phagri", "phag ri");
        checkMatch("Peme Namgyal", "pad ma'i rnam rgyal");
        checkMatch("Rongdrak", "rong brag");
        checkMatch("Drikha", "'bri kha");
        checkMatch("Sakya", "sa skya");
        checkMatch("Purang", "spu rangs");
        checkMatch("Zangdog Palri", "zangs mdog dpal ri");
        checkMatch("Yuthok", "g.yu thog");
        checkMatch("Lhamo", "lha mo");
        checkMatch("Menri", "sman ri");
        checkMatch("Sangye Tenzin", "sangs rgyas bstan 'dzin");
        checkMatch("Kunkyen", "kun mkhyen");
        checkMatch("Bodjong", "bod ljongs");
        checkMatch("Drokpa", "'brog pa");
        checkMatch("Tso Ngon", "mtsho sngon");
        checkMatch("Gonlung", "dgon lung");
        checkMatch("Nyingje", "snying rje");
        checkMatch("Gyalwa", "rgyal ba");
        checkMatch("Shika", "gzhis ka");
        checkMatch("Pawo", "dpa' bo");
        checkMatch("Yuldo", "yul mdo");
        checkMatch("Barkham", "'bar khams");
        checkMatch("Serzhong", "ser gzhong");
        checkMatch("Yuru", "g.yu ru");
        checkMatch("Ragong", "rwa sgong");
        checkMatch("Zhalu", "zhwa lu");
        checkMatch("Pari", "dpa' ris");
        checkMatch("Sogpo", "sog po");
        checkMatch("Gyalsé", "rgyal sras");
        checkMatch("Labrang", "bla brang");
        checkMatch("Mangra", "mang ra");
        checkMatch("Bayan", "ba yan");
        checkMatch("Tsering", "tshe ring");
        checkMatch("Dargye", "dar rgyas");
        checkMatch("Sangye Gyatso", "sangs rgyas rgya mtsho");
        checkMatch("Gyaltshab", "rgyal tshab");
        checkMatch("Lhodrak", "lho brag");
        checkMatch("Dechen", "bde chen");
        checkMatch("Pakpa", "'phags pa");
        checkMatch("Shangpang", "shangs spang");
        checkMatch("Sharpa", "shar pa");
        checkMatch("Ngangtso", "ngang tsho");
        checkMatch("Phuru", "phu ru");
        checkMatch("Tselha", "tshe lha");
        checkMatch("Onpo", "'on po");
        checkMatch("Chimpa", "mchims pa");
        checkMatch("Lhamo Dechen", "lha mo bde chen");
        checkMatch("Kyungpo", "khyung po");
        checkMatch("Dulwa Gyaltsen", "'dul ba rgyal mtshan");
        checkMatch("Lhogyu", "lho rgyud");
        checkMatch("Shangsa", "shangs sa");
        checkMatch("Chokling", "mchog gling");
        checkMatch("Gyalsid", "rgyal srid"); // ?
        checkMatch("Dega", "de ga");
        checkMatch("Gangga", "gang+ga");
        checkMatch("Ganga", "gang+ga");
        checkMatch("Mangalam", "ma ng+ga laM");
        checkMatch("Mangalam", "ma ng+ga lam");
        checkMatch("Utpala Karpo", "ut+pala dkar po");
        checkMatch("Vaidurya Karpo", "vaidur+ya dkar po");
        checkMatch("Bedurya Karpo", "vaidur+ya dkar po");
        checkMatch("Mandala", "maN+Dala");
        checkMatch("tika", "tik+ka");
        checkMatch("Kirti", "kir+ti");
        checkMatch("Indra", "in+dra");
        checkMatch("Yangti", "g.yang Ti");
        checkMatch("Shambhala", "sham+b+hala");
        checkMatch("Shambhala", "sham b+ha la");
        checkMatch("Shambala", "sham b+ha la");
        checkMatch("Densatil", "gdan sa mthil"); // gdan sa thel?
        checkMatch("Hemis", "he mis");
        //checkMatch("Key", "ki"); // ?
        checkMatch("Kye Gompa", "dkil dgon");
        checkMatch("Kyegön", "dkil dgon");
        checkMatch("Mindroling", "smin grol gling");
        checkMatch("Tsuglakhang", "gtsug lag khang");
        checkMatch("Tsuglagkhang", "gtsug lag khang");
        checkMatch("Pabonka", "pha bong kha"); // ?
        checkMatch("Gyantse", "rgyal rtse");
        checkMatch("Gyangtse", "rgyal rtse"); // ?
        checkMatch("Palyul", "dpal yul");
        checkMatch("Reting", "rwa sgreng");
        checkMatch("Riwoche", "ri bo che");
        checkMatch("Ngawang", "ngag dbang");
        checkMatch("Ngakwang", "ngag dbang");
        checkMatch("Jamyang Ngakwang Legdrub", "'jam dbyangs ngag dbang legs grub");
        checkMatch("Jampeyang Ngakwang Legdrub", "'jam pa'i dbyangs ngag dbang legs grub");
        checkMatch("Bari Lotsāwa", "ba ri lo tsA ba");
        checkMatch("Benza Ratna", "badz+ra rat+na");
        checkMatch("Chökyi Tsultrim", "chos kyi tshul khrims");
        checkMatch("Chönyi", "chos nyid");
        checkMatch("Chöwang", "chos dbang");
        checkMatch("Dagpo", "dwags po");
        checkMatch("Dhagpo", "dwags po");
        checkMatch("Dhagpo", "dwags po");
        checkMatch("Dhagpo Targyen", "dwags po thar rgyen");
        checkMatch("Ngulchu Thogmé Zangpo", "dngul chu thogs med bzang po");
        checkMatch("Damchö Lingpa", "dam chos gling pa");
        checkMatch("Drigung Lingpa", "'bri gung gling pa");
        checkMatch("Gomtsul", "sgom tshul");
        checkMatch("Jamgön Kunga Lodrö", "'jam mgon kun dga' blo gros");
        checkMatch("Jamyang Khyentse Wangchuk", "'jam dbyangs mkhyen rtse dbang phyug");
        checkMatch("Jamyang Ngakwang Legdrub", "'jam dbyangs ngag dbang legs grub");
        checkMatch("Jangchub Zangpo", "byang chub bzang po");
        checkMatch("Jetsun Trinlepa", "rje btsun phrin las pa");
        checkMatch("Kunga Rinchen", "kun dga rin chen");
        checkMatch("Kunlo", "kun blo");
        checkMatch("Labsum", "bslab gsum");
        checkMatch("Le Nagpa", "sle nag pa");
        checkMatch("Lhalung", "lha lung");
        checkMatch("Losal Tenzin", "blo gsal bstan 'dzin");
        checkMatch("Mangala", "mang+ga la");
        checkMatch("Ngor Evaṃ", "ngor e waM");
        checkMatch("Evam", "e waM");
        checkMatch("Nyen Lotsāwa Darma Drak", "gnyan lo tsA ba dar ma grags");
        checkMatch("Palden Gyaltsen", "dpal ldan rgyal mtshan");
        checkMatch("Rinchen Jampal", "rin chen 'jam dpal");
        checkMatch("Sachen Kunga Nyingpo", "sa chen kun dga' snying po");
        checkMatch("Śākyaśrībhadra", "shAkya shrI bha dra");
        checkMatch("Shenyen Drewa", "bshes gnyen sgre ba");
        checkMatch("Sherab Jangchub", "shes rab byang chub");
        checkMatch("Sönam Chogden", "bsod nams mchog ldan");
        checkMatch("Sönam Chogdrub", "bsod nams mchog grub");
        checkMatch("Sönam Lhawang", "bsod nams lha dbang");
        checkMatch("Tognepa", "thog gnas pa");
        checkMatch("Tsarchen", "tshar chen");
        checkMatch("Tsechok Samten Ling", "tshe mchog bsam gtan gling");
        checkMatch("Wang Rab", "dbang rab");
        checkMatch("Kunzang lame shelung" , "kun bzang bla ma'i zhal lung");
    }   
}
