/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Licensed to the Apache Software Foundation (ASF) under one
 ~ or more contributor license agreements.  See the NOTICE file
 ~ distributed with this work for additional information
 ~ regarding copyright ownership.  The ASF licenses this file
 ~ to you under the Apache License, Version 2.0 (the
 ~ "License"); you may not use this file except in compliance
 ~ with the License.  You may obtain a copy of the License at
 ~
 ~   http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package org.apache.sling.scriptingbundle.maven.plugin;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ScriptTest {

    @Test
    public void parseTwoPartScriptName() {
        Script script = Script.parseScript("test.html");
        assertNotNull(script);
        assertEquals("test", script.getName());
        assertNull(script.getRequestExtension());
        assertNull(script.getRequestMethod());
        assertEquals("html", script.getScriptExtension());
    }

    @Test
    public void parseTwoPartScriptRequestMethod() {
        Script script = Script.parseScript("GET.html");
        assertNotNull(script);
        assertNull(script.getName());
        assertNull(script.getRequestExtension());
        assertEquals("GET", script.getRequestMethod());
        assertEquals("html", script.getScriptExtension());
    }

    @Test
    public void parseTwoPartScriptRequestExtension() {
        Script script = Script.parseScript("html.html");
        assertNotNull(script);
        assertNull(script.getName());
        assertEquals("html", script.getRequestExtension());
        assertNull(script.getRequestMethod());
        assertEquals("html", script.getScriptExtension());
    }

    @Test
    public void testThreePartScriptNameRequestExtension() {
        Script script = Script.parseScript("test.txt.html");
        assertNotNull(script);
        assertEquals("test", script.getName());
        assertEquals("txt", script.getRequestExtension());
        assertNull(script.getRequestMethod());
        assertEquals("html", script.getScriptExtension());
    }

    @Test
    public void testThreePartScriptNameRequestMethod() {
        Script script = Script.parseScript("test.POST.html");
        assertNotNull(script);
        assertEquals("test", script.getName());
        assertNull(script.getRequestExtension());
        assertEquals("POST", script.getRequestMethod());
        assertEquals("html", script.getScriptExtension());
    }

    @Test
    public void testFourPartScript() {
        Script script = Script.parseScript("test.txt.PUT.html");
        assertNotNull(script);
        assertEquals("test", script.getName());
        assertEquals("txt", script.getRequestExtension());
        assertEquals("PUT", script.getRequestMethod());
        assertEquals("html", script.getScriptExtension());
    }

    @Test
    public void testScriptWithNotEnoughOrTooManyParts() {
        assertNull(Script.parseScript("extends"));
        assertNull(Script.parseScript("test.1.txt.PUT.html"));
    }
}
