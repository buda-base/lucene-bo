package io.bdrc.lucene.sixtofour;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.Reader;
import java.lang.reflect.Field;

/**
 * Dummy.READER is required to migrate these 3 classes from Lucene 6 to 4:
 * SkrtWordTokenizer, SkrtSyllableTokenizer, SanskritAnalyzer;
 * More precisely, Lucene 6 has Analyzer and Tokenizer constructors taking no arguments,
 * that set the `input' member to the (private) Tokenizer.ILLEGAL_STATE_READER;
 * OTOH Lucene 4 requires at least the provision of a Reader object,
 * (and using Tokenizer.ILLEGAL_STATE_READER is not an option as it is private)
 * Hence -- the need for a Dummy.READER, which will throw upon invocation
 * of its read() or close() method.
 */
public abstract class Dummy {

    private static Reader  obtain_ILLEGAL_STATE_READER() {
        try {
            Field f = org.apache.lucene.analysis.Tokenizer.class.getDeclaredField("ILLEGAL_STATE_READER"); //NoSuchFieldException
            f.setAccessible(true);
            Reader isr = (Reader) f.get(org.apache.lucene.analysis.Tokenizer.class); //IllegalAccessException
            return isr;
        }
        catch (NoSuchFieldException exn) {
            throw new RuntimeException(exn);
        }
        catch (IllegalAccessException exn) {
            throw new RuntimeException(exn);
        }
    }
    public static final Reader  READER = obtain_ILLEGAL_STATE_READER();
}
