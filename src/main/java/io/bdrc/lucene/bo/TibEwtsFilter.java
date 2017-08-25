package io.bdrc.lucene.bo;

import java.io.IOException;
import java.io.Reader;
import org.apache.lucene.analysis.charfilter.BaseCharFilter;
import org.apache.lucene.analysis.util.RollingCharBuffer;

import io.bdrc.ewtsconverter.EwtsConverter;

/**
 * A filter that converts EWTS input into Tibetan Unicode
 * 
 * Partially inpired from Lucene 6 org.apache.lucene.analysis.charfilterMappingCharFilter
 * 
 * @author Elie Roux
 **/
public class TibEwtsFilter extends BaseCharFilter {
	
	public static final EwtsConverter converter = new EwtsConverter(false, false, false, true);
	
	private final RollingCharBuffer buffer = new RollingCharBuffer();
	private final int MAX_EWTS_LEN = 32;
	private String replacement = null;
	private int replacementIdx = -1;
	private int replacementLen = -1;
	private int inputOff;
	StringBuilder tmpEwts;
	
	public TibEwtsFilter(Reader in) {
		super(in);
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
	
	@Override
	public int read() throws IOException {
		if (replacement != null && replacementIdx < replacementLen) {
			return replacement.charAt(replacementIdx++);
		}
		replacement = null;
		replacementIdx = 0;
		tmpEwts = new StringBuilder();
		int initialInputOff = inputOff;
		while (true) {
			int c = buffer.get(inputOff);
			if (c == -1) {
				replacement = tmpEwts.length() > 0 ? converter.toUnicode(tmpEwts.toString()) : null;
				break;
			}
			inputOff = inputOff +1;
			tmpEwts.append((char) c);
			if (c == ' ' || c == '*' || c == '_' || c > 127 || inputOff - initialInputOff > MAX_EWTS_LEN) {
				replacement = converter.toUnicode(tmpEwts.toString());
				break;
			}
		}
		buffer.freeBefore(inputOff);
		if (replacement == null || replacement.isEmpty()) {
			return -1;
		}
		int diff = (inputOff - initialInputOff) - replacement.length();
		// verbatim from charfilterMappingCharFilter
        if (diff != 0) {
            final int prevCumulativeDiff = getLastCumulativeDiff();
            if (diff > 0) {
              addOffCorrectMap(inputOff - diff - prevCumulativeDiff, prevCumulativeDiff + diff);
            } else {
              final int outputStart = inputOff - prevCumulativeDiff;
              for(int extraIDX=0;extraIDX<-diff;extraIDX++) {
                addOffCorrectMap(outputStart + extraIDX, prevCumulativeDiff - extraIDX - 1);
              }
            }
        }
		replacementIdx = 1;
		replacementLen = replacement.length();
		return replacement.charAt(0);
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		System.out.println("calling read with off="+off+", len="+len);
	    int numRead = 0;
	    for(int i = off; i < off + len; i++) {
	    	int c = read();
	    	if (c != -1) {
	    		System.out.println("reading "+(char)c);
	    	}
	      	if (c == -1) {
	      		System.out.println("reached end of input");
	      		break;
	      	}
	      	cbuf[i] = (char) c;
	      	numRead++;
	    }

	    return numRead == 0 ? -1 : numRead;
	}
}
