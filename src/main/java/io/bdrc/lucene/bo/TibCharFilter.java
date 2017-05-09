package io.bdrc.lucene.bo;

import java.io.Reader;

import org.apache.lucene.analysis.charfilter.MappingCharFilter;
import org.apache.lucene.analysis.charfilter.NormalizeCharMap;

public class TibCharFilter extends MappingCharFilter {
	public TibCharFilter(Reader in) {
		super(getTibNormalizeCharMap(), in);
	}

	public final static NormalizeCharMap getTibNormalizeCharMap() {
		NormalizeCharMap.Builder builder  = new NormalizeCharMap.Builder();	
		// The non-breaking tsheg is replaced by the normal one
		builder.add("\u0f0C", "\u0F0B");
		// Characters to delete: the markers found under selected syllables
		builder.add("\u0F35", ""); //  ༵
		builder.add("\u0F37", ""); //  ༷
		// Characters to decompose
		builder.add("\u0F00", "\u0F68\u0F7C\u0F7E"); //  ༀ 
		builder.add("\u0F02", "\u0F60\u0F70\u0F82"); // ༂
		builder.add("\u0F03", "\u0F60\u0F70\u0F14"); //  ༃
		builder.add("\u0F43", "\u0F42\u0FB7"); //  གྷ
		builder.add("\u0F48", "\u0F47\u0FB7"); //  ཈
		builder.add("\u0F4D", "\u0F4C\u0FB7"); //  ཌྷ
		builder.add("\u0F52", "\u0F51\u0FB7"); //  དྷ
		builder.add("\u0F57", "\u0F56\u0FB7"); //  བྷ
		builder.add("\u0F5C", "\u0F5B\u0FB7"); //  ཛྷ
		builder.add("\u0F69", "\u0F40\u0FB5"); //  ཀྵ
		builder.add("\u0F73", "\u0F71\u0F72"); //    ཱི
		builder.add("\u0F75", "\u0F71\u0F74"); //   ཱུ
		builder.add("\u0F76", "\u0FB2\u0F80"); //   ྲྀ
		builder.add("\u0F77", "\u0FB2\u0F71\u0F80"); //   ཷ
		builder.add("\u0F78", "\u0FB3\u0F80"); //   ླྀ
		builder.add("\u0F79", "\u0FB3\u0F71\u0F80"); //   ཹ
		builder.add("\u0F81", "\u0F71\u0F80"); //     ཱྀ
		builder.add("\u0F93", "\u0F92\u0FB7"); //  ྒྷ
		builder.add("\u0F9D", "\u0F9C\u0FB7"); //  ྜྷ
		builder.add("\u0FA2", "\u0FA1\u0FB7"); //  ྡྷ
		builder.add("\u0FA7", "\u0FA6\u0FB7"); //  ྦྷ
		builder.add("\u0FAC", "\u0FAB\u0FB7"); //  ྫྷ
		builder.add("\u0FB9", "\u0F90\u0FB5"); //  ྐྵ
		return builder.build();
	}
}
