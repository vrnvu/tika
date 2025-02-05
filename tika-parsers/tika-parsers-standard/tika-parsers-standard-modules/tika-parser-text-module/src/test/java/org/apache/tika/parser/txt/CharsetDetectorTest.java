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
package org.apache.tika.parser.txt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import org.apache.tika.TikaTest;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;

public class CharsetDetectorTest extends TikaTest {

    @Test
    public void testTagDropper() throws IOException {
        try (InputStream in = getResourceAsStream("/test-documents/resume.html")) {
            CharsetDetector detector = new CharsetDetector();
            detector.enableInputFilter(true);
            detector.setText(in);
            CharsetMatch[] matches = detector.detectAll();
            CharsetMatch mm = null;
            for (CharsetMatch m : matches) {
                if (mm == null || mm.getConfidence() < m.getConfidence()) {
                    mm = m;
                }
            }
            assertTrue(mm != null);
            assertEquals("UTF-8", mm.getName());
        }
    }

    /* https://issues.apache.org/jira/browse/TIKA-1248
     * Verify empty or null declaredEncoding doesn't cause an exception
     *
     */

    @Test
    public void testEmptyOrNullDeclaredCharset() throws IOException {
        try (InputStream in = getResourceAsStream("/test-documents/resume.html")) {
            CharsetDetector detector = new CharsetDetector();
            Reader reader = detector.getReader(in, null);
            assertTrue(reader.ready());

            reader = detector.getReader(in, "");
            assertTrue(reader.ready());
        }
    }

    @Test
    public void testWin125XHeuristics() throws Exception {
        //TIKA-2219
        CharsetDetector detector = new CharsetDetector();
        try (InputStream is = getResourceAsStream("/test-documents/testTXT_win-1252.txt")) {
            detector.setText(is);
        }
        CharsetMatch charset = detector.detect();
        assertEquals("windows-1252", charset.getName());
    }

    @Test
    public void testSetTextConsistency() throws Exception {
        //TIKA-2475
        File file = getResourceAsFile("/test-documents/multi-language.txt");
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        InputStream fileStream = new ByteArrayInputStream(fileBytes);

        CharsetDetector fromBytesDetector = new CharsetDetector();
        fromBytesDetector.setText(fileBytes);

        CharsetDetector fromStreamDetector = new CharsetDetector();
        fromStreamDetector.setText(fileStream);

        assertEquals("ISO-8859-1", fromBytesDetector.detect().getName());
        assertEquals("ISO-8859-1", fromStreamDetector.detect().getName());
    }

    @Test
    public void testZeroLength() throws Exception {
        CharsetDetector detector = new CharsetDetector();
        detector.setText(new byte[0]);
        //charset detector returns "UTF-8" when there's no data
        assertEquals("UTF-8", detector.detect().getName());
    }

    @Test
    public void testLengthResetCorrectly() throws IOException {
        //test that the underlying array.length is reset correctly
        //first fill the buffer with windows-1256

        String computer = "\u0627\u0644\u062D\u0627\u0633\u0648\u0628";
        StringBuilder sb = new StringBuilder();
        CharsetDetector detector = new CharsetDetector();
        for (int i = 0; i < 5000; i++) {
            sb.append(computer);
        }
        detector.setText(sb.toString().getBytes("windows-1256"));
        assertEquals("windows-1256", detector.detect().getName());

        sb.setLength(0);
        for (int i = 0; i < 5; i++) {
            sb.append(computer);
        }
        //then fill a small part of the buffer with UTF-8
        detector.setText(sb.toString().getBytes("UTF-8"));
        assertEquals("UTF-8", detector.detect().getName());
    }

    @Test
    public void testIgnoreCharset() throws Exception {
        //TIKA-3516, TIKA-3525, TIKA-1236
        TikaConfig tikaConfig = new TikaConfig(
                getResourceAsStream("/test-configs/tika-config-ignore-charset.xml"));

        Metadata m = new Metadata();

        m.set(TikaCoreProperties.RESOURCE_NAME_KEY, "texty-text.txt");
        assertContains("ACTIVE AGE", getXML("testIgnoreCharset.txt",
                new AutoDetectParser(tikaConfig), m).xml);
    }
}
