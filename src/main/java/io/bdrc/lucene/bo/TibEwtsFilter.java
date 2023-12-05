package io.bdrc.lucene.bo;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.charfilter.BaseCharFilter;
import org.apache.lucene.analysis.util.RollingCharBuffer;

import io.bdrc.ewtsconverter.EwtsConverter;

/**
 * A filter that converts EWTS input into Tibetan Unicode
 * 
 * Partially inspired from Lucene 6
 * org.apache.lucene.analysis.charfilterMappingCharFilter
 * 
 * @author Elie Roux
 **/
public class TibEwtsFilter extends BaseCharFilter {

    public static final EwtsConverter converter_ewts = new EwtsConverter(false, false, false, false, EwtsConverter.Mode.EWTS);
    public static final EwtsConverter converter_dts = new EwtsConverter(false, false, false, false, EwtsConverter.Mode.EWTS);
    public static final EwtsConverter converter_alalc = new EwtsConverter(false, false, false, false, EwtsConverter.Mode.EWTS);
    
    private final RollingCharBuffer buffer = new RollingCharBuffer();
    private final int MAX_EWTS_LEN = 32;
    private String replacement = null;
    private int replacementIdx = -1;
    private int replacementLen = -1;
    private int inputOff;
    private final boolean lenient;
    StringBuilder tmpEwts;
    public EwtsConverter converter;

    public TibEwtsFilter(Reader in) {
        this(in, TibetanAnalyzer.INPUT_METHOD_EWTS, true);
    }

    public TibEwtsFilter(final Reader in, final String inputMethod, final boolean lenient) {
        super(in);
        this.lenient = lenient;
        this.converter = converter_ewts;
        switch (inputMethod) {
        case TibetanAnalyzer.INPUT_METHOD_DTS:
            this.converter = converter_dts;
            break;
        case TibetanAnalyzer.INPUT_METHOD_ALALC:
            this.converter = converter_alalc;
            break;
        default:
            break;
        }
        buffer.reset(in);
        inputOff = 0;
    }

    @Override
    public void reset() throws IOException {
        input.reset();
        buffer.reset(input);
        replacement = null;
        inputOff = 0;
    }

    public static boolean isEwtsLetters(final int c) {
        if (c == ' ' || c == '*' || c == '_' || c == '(' || c == '/' || c == ')' || c == ':')
            return false;
        return true;
    }

    @Override
    public int read() throws IOException {
        if (replacement != null && replacementIdx < replacementLen) {
            return replacement.charAt(replacementIdx++);
        }
        replacement = null;
        replacementIdx = 0;
        tmpEwts = new StringBuilder();
        final int initialInputOff = inputOff;
        boolean stoppedOnPunctuation = false;
        while (true) {
            final int c = buffer.get(inputOff);
            if (c == -1) {
                replacement = tmpEwts.length() > 0 ? this.converter.toUnicode(tmpEwts.toString(), null, false, this.lenient) : null;
                break;
            }
            inputOff = inputOff + 1;
            tmpEwts.append((char) c);
            if (!isEwtsLetters(c)) {
                replacement = this.converter.toUnicode(tmpEwts.toString(), null, false, this.lenient);
                stoppedOnPunctuation = true;
                break;
            }
            if (inputOff - initialInputOff > MAX_EWTS_LEN) {
                replacement = this.converter.toUnicode(tmpEwts.toString(), null, false, this.lenient);
                break;
            }
        }
        // System.out.println("ewts: \""+tmpEwts.toString()+"\",
        // replacement="+replacement);
        buffer.freeBefore(inputOff);
        if (replacement == null) {
            replacementLen = 0;
        } else {
            replacementLen = replacement.length();
        }
        final int diff = (inputOff - initialInputOff) - replacementLen;
        // System.out.println("diff="+diff+", replacementLen="+replacementLen+",
        // initialInputOff="+initialInputOff+", inputOff="+inputOff);
        // verbatim from charfilterMappingCharFilter
        if (diff != 0) {
            final int prevCumulativeDiff = getLastCumulativeDiff();
            if (diff > 0) {
                final int adjustedInputOff = stoppedOnPunctuation ? inputOff - 1 : inputOff;
                addOffCorrectMap(adjustedInputOff - diff - prevCumulativeDiff, prevCumulativeDiff + diff);
            } else {
                final int outputStart = inputOff - prevCumulativeDiff;
                for (int extraIDX = 0; extraIDX < -diff; extraIDX++) {
                    addOffCorrectMap(outputStart + extraIDX, prevCumulativeDiff - extraIDX - 1);
                }
            }
        }
        if (replacementLen == 0) {
            return -1;
        }
        replacementIdx = 1;
        return replacement.charAt(0);
    }

    @Override
    public int read(final char[] cbuf, final int off, final int len) throws IOException {
        int numRead = 0;
        for (int i = off; i < off + len; i++) {
            int c = read();
            if (c == -1) {
                break;
            }
            cbuf[i] = (char) c;
            numRead++;
        }
        return numRead == 0 ? -1 : numRead;
    }
}
