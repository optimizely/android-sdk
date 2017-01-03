/*
 *    Copyright 2017, Optimizely
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.optimizely.ab.bucketing.internal;

import org.junit.Test;

import java.nio.charset.Charset;
import java.util.Random;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static org.junit.Assert.assertEquals;

/**
 * @author yonik
 *
 * See http://github.com/yonik/java_util for future updates to this file.
 * <p>
 * <b>NOTICE:</b> changes made to the original
 * <ul>
 *   <li>package changed</li>
 *   <li>removed {@code testCorrectValues}</li>
 *   <li>findbugs fixes & removing {@code System.out.println}</li>
 * </ul>
 *
 * optimizely
 * somerandomvalue
 *
 */
public class MurmurHash3Test {

    private final Charset utf8Charset = Charset.forName("UTF-8");

    private void doString(String s) {
        doString(s, 0, 0);
    }

    private void doString(String s, int pre, int post) {
        byte[] utf8 = s.getBytes(utf8Charset);
        int hash1 = MurmurHash3.murmurhash3_x86_32(utf8, pre, utf8.length-pre-post, 123456789);
        int hash2 = MurmurHash3.murmurhash3_x86_32(s, pre, s.length()-pre-post, 123456789);
        if (hash1 != hash2) {
            // second time for debugging...
            hash2 = MurmurHash3.murmurhash3_x86_32(s, pre, s.length()-pre-post, 123456789);
        }
        assertEquals(hash1, hash2);
    }

    @Test
    @SuppressFBWarnings(
        value={"SF_SWITCH_FALLTHROUGH","SF_SWITCH_NO_DEFAULT"},
        justification="deliberate")
    public void testStringHash() {
        doString("hello!");
        doString("ABCD");
        doString("\u0123");
        doString("\u2345");
        doString("\u2345\u1234");

        Random r = new Random();
        StringBuilder sb = new StringBuilder(40);
        for (int i=0; i<100000; i++) {
            sb.setLength(0);
            int pre = r.nextInt(3);
            int post = r.nextInt(3);
            int len = r.nextInt(16);

            for (int j=0; j<pre; j++) {
                int codePoint = r.nextInt(0x80);
                sb.appendCodePoint(codePoint);
            }

            for (int j=0; j<len; j++) {
                int codePoint;
                do {
                    int max = 0;
                    switch (r.nextInt() & 0x3) {
                        case 0: max=0x80; break;   // 1 UTF8 bytes
                        case 1: max=0x800; break;  // up to 2 bytes
                        case 2: max=0xffff+1; break; // up to 3 bytes
                        case 3: max=Character.MAX_CODE_POINT+1; // up to 4 bytes
                    }

                    codePoint = r.nextInt(max);
                }  while (codePoint < 0xffff && (Character.isHighSurrogate((char)codePoint) || Character.isLowSurrogate((char)codePoint)));

                sb.appendCodePoint(codePoint);
            }

            for (int j=0; j<post; j++) {
                int codePoint = r.nextInt(0x80);
                sb.appendCodePoint(codePoint);
            }

            String s = sb.toString();
            String middle = s.substring(pre, s.length()-post);

            doString(s);
            doString(middle);
            doString(s, pre, post);
        }

    }
}


// Here is my C++ code to produce the list of answers to check against.
/***************************************************
 #include <iostream>
 #include "MurmurHash3.h"
 using namespace std;

 int main(int argc, char** argv) {
 char* val = strdup("Now is the time for all good men to come to the aid of their country");
 int max = strlen(val);
 int hash=0;
 for (int i=0; i<max; i++) {
 hash = hash*31 + (val[i] & 0xff);
 // we want to make sure that some high bits are set on the bytes
 // to catch errors like signed vs unsigned shifting, etc.
 val[i] = (char)hash;
 }
 uint32_t seed = 1;
 for (int len=0; len<max; len++) {
 seed = seed * 0x9e3779b1;
 int result;
 MurmurHash3_x86_32(val, len, seed, &result);
 cout << "0x" << hex << result << ",";
 }
 cout << endl;
 cout << endl;
 // now 128 bit
 seed = 1;
 for (int len=0; len<max; len++) {
 seed = seed * 0x9e3779b1;
 long result[2];
 MurmurHash3_x64_128(val, len, seed, &result);
 cout << "0x" << hex << result[0] << "L,0x" << result[1] << "L," ;
 }

 cout << endl;
 }
 ***************************************************/