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

import org.apache.maven.plugin.logging.Log;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@Ignore
public class MetadataMojoCapabilityFindingTest {

    private MetadataMojo plugin;

    @Rule
    public ExpectedException thrown= ExpectedException.none();

    @Before
    public void setUp() {
        plugin = spy(new MetadataMojo());
        when(plugin.getLog()).thenReturn(mock(Log.class));
    }

    @After
    public void tearDown() {
        plugin = null;
    }

    @Test
    public void testScriptNameFullCalculation() {
        String scriptPath = "org.apache.foo/1.0.0/POST.hi.xml.jsp";

        ProvidedCapability expected = ProvidedCapability.builder()
                .withResourceType("org.apache.foo")
                .withVersion("1.0.0")
//                .withName("hi")
//                .withMethod("POST")
//                .withExtension("xml")
//                .withScriptExtension("jsp")
                .build();
        assertEquals(expected, plugin.getScript(scriptPath));
    }

    @Test
    public void testScriptNameFullCalculationFolderHierarchy() {
        String scriptPath = "org/apache/foo/1.0.0/POST.hi.xml.jsp";

        ProvidedCapability expected = ProvidedCapability.builder()
                .withResourceType("org/apache/foo")
                .withVersion("1.0.0")
//                .withName("hi")
//                .withMethod("POST")
//                .withExtension("xml")
//                .withScriptExtension("jsp")
                .build();
        assertEquals(expected, plugin.getScript(scriptPath));
    }

    @Test
    public void testScriptNameMinCalculation() {
        String scriptPath = "org.apache.foo/foo.jsp";

        ProvidedCapability expected = ProvidedCapability.builder()
                .withResourceType("org.apache.foo")
//                .withName("foo")
//                .withScriptExtension("jsp")
                .build();

        assertEquals(expected, plugin.getScript(scriptPath));
    }

    @Test
    public void testScriptNameMinCalculationFolderHierarchy() {
        String scriptPath = "org/apache/foo/foo.jsp";

        ProvidedCapability expected = ProvidedCapability.builder()
                .withResourceType("org/apache/foo")
//                .withName("foo")
//                .withScriptExtension("jsp")
                .build();

        assertEquals(expected, plugin.getScript(scriptPath));
    }

    @Test
    public void testScriptNameVersionAndMethodCalculation() {
        String scriptPath = "org.apache.foo/1.2.0/Post.jsp";

        ProvidedCapability expected = ProvidedCapability.builder()
                .withResourceType("org.apache.foo")
                .withVersion("1.2.0")
//                .withMethod("POST")
//                .withScriptExtension("jsp")
                .build();
        assertEquals(expected, plugin.getScript(scriptPath));
    }

    @Test()
    public void testScriptNameVersionAndMethodMinCalculation() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("The script's scriptExtension cannot be null");

        String scriptPath = "org.apache.foo/1.2.0/Post";
        plugin.getScript(scriptPath);
    }

    @Test()
    public void testScriptNameMinCalculationInvalidPath() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("The script's scriptExtension cannot be null");

        String scriptPath = "org.apache.foo/1.2.0/foo.";
        plugin.getScript(scriptPath);
    }

    @Test()
    public void testScriptNameMinCalculationNoResourceType() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("The script's resourceType cannot be null");

        String scriptPath = "foo.html";
        plugin.getScript(scriptPath);
    }
}
