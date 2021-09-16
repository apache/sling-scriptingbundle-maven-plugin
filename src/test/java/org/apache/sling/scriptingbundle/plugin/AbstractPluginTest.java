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
package org.apache.sling.scriptingbundle.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.sling.scriptingbundle.plugin.capability.Capabilities;
import org.apache.sling.scriptingbundle.plugin.capability.ProvidedResourceTypeCapability;
import org.apache.sling.scriptingbundle.plugin.capability.ProvidedScriptCapability;
import org.apache.sling.scriptingbundle.plugin.capability.RequiredResourceTypeCapability;
import org.junit.Test;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

/** Common base class for both Bnd plugin and Maven plugin */
public abstract class AbstractPluginTest {

    public abstract PluginExecution executePluginOnProject(String projectName) throws Exception;
    
    public abstract void cleanUp(String projectName) throws Exception;

    @Test
    public void testProject1() throws Exception {
        try {
            PluginExecution execution = executePluginOnProject("project-1");
            Capabilities capabilities = execution.getCapabilities();
            Map<String, String> scriptEngineMappings = execution.getScriptEngineMappings();
            Set<ProvidedResourceTypeCapability> pExpected = new HashSet<>(Arrays.asList(
                    // org/apache/sling/bar/1.0.0
                    ProvidedResourceTypeCapability.builder().withResourceType("org/apache/sling/bar").withScriptEngine("htl")
                            .withScriptExtension("html").withVersion(new Version("1.0.0")).build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("org/apache/sling/bar").withScriptEngine("htl")
                            .withScriptExtension("html").withVersion(new Version("1.0.0")).withSelectors(Arrays.asList("depth1"
                            , "100")).build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("org/apache/sling/bar").withScriptEngine("htl")
                            .withScriptExtension("html").withVersion(new Version("1.0.0")).withSelectors(Arrays.asList("depth1"
                            , "200")).build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("org/apache/sling/bar").withScriptEngine("htl")
                            .withScriptExtension("html").withVersion(new Version("1.0.0")).withSelectors(Arrays.asList("depth1"
                            , "depth2", "100")).build(),
    
                    // org/apache/sling/foo
                    ProvidedResourceTypeCapability.builder().withResourceType("org/apache/sling/foo").withScriptEngine("htl")
                            .withScriptExtension("html").build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("org/apache/sling/foo").withScriptEngine("htl")
                            .withScriptExtension("html").withSelectors(Arrays.asList("depth1"
                            , "100")).build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("org/apache/sling/foo").withScriptEngine("htl")
                            .withScriptExtension("html").withSelectors(Arrays.asList("depth1"
                            , "200")).build(),
                            
                    ProvidedResourceTypeCapability.builder().withResourceType("org/apache/sling/foo").withScriptEngine("htl")
                            .withScriptExtension("html").withSelectors(Arrays.asList("depth1"
                            , "depth1", "depth3")).build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("org/apache/sling/foo").withScriptEngine("htl")
                            .withScriptExtension("html").withSelectors(Arrays.asList("depth1"
                            , "depth2", "100")).build(),
    
                    // org/apache/sling/foo/depth1/depth2/depth3
                    ProvidedResourceTypeCapability.builder().withResourceType("org/apache/sling/foo/depth1/depth2/depth3")
                            .withExtendsResourceType("org" +
                                    "/apache/sling/bar").build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("org/apache/sling/foo/depth1/depth2/depth3")
                            .withScriptEngine("htl").withScriptExtension("html")
                            .withSelectors(Arrays.asList("depth3-selector")).build(),
    
                    // org.apache.sling.foobar/1.0.0
                    ProvidedResourceTypeCapability.builder().withResourceType("org.apache.sling.foobar").withScriptEngine("htl")
                            .withScriptExtension("html").withVersion(new Version("1.0.0")).withExtendsResourceType("org/apache/sling/bar").build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("org.apache.sling.foobar").withScriptEngine("htl")
                            .withScriptExtension("html").withVersion(new Version("1.0.0")).withSelectors(Arrays.asList("depth1"
                            , "100")).build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("org.apache.sling.foobar").withScriptEngine("htl")
                            .withScriptExtension("html").withVersion(new Version("1.0.0")).withSelectors(Arrays.asList("depth1"
                            , "200")).build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("org.apache.sling.foobar").withScriptEngine("htl")
                            .withScriptExtension("html").withVersion(new Version("1.0.0")).withSelectors(Arrays.asList("depth1"
                            , "depth2", "100")).build(),
    
                    // org.apache.sling.foobar
                    ProvidedResourceTypeCapability.builder().withResourceType("org.apache.sling.foobar")
                            .withExtendsResourceType("org/apache/sling/bar").build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("org.apache.sling.foobar").withScriptEngine("htl")
                            .withScriptExtension("html").withSelectors(Arrays.asList("depth1"
                            , "100")).build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("org.apache.sling.foobar").withScriptEngine("htl")
                            .withScriptExtension("html").withSelectors(Arrays.asList("depth1"
                            , "200")).build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("org.apache.sling.foobar").withScriptEngine("htl")
                            .withScriptExtension("html").withSelectors(Arrays.asList("depth1"
                            , "depth2", "100")).build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("org.apache.sling.foobar").withScriptEngine("htl")
                            .withScriptExtension("html").withRequestMethod("GET").build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("org.apache.sling.foobar").withScriptEngine("htl")
                            .withScriptExtension("html").withRequestMethod("GET").withSelectors(Arrays.asList("test"))
                            .build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("org.apache.sling.foobar").withScriptEngine("htl")
                            .withScriptExtension("html").withRequestMethod("GET").withSelectors(Arrays.asList("test"))
                            .withRequestExtension("txt").build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("org.apache.sling.foobar").withScriptEngine("htl")
                            .withScriptExtension("html").withSelectors(Arrays.asList("test"))
                            .withRequestExtension("txt").build(),
    
                    // sling
                    ProvidedResourceTypeCapability.builder().withResourceType("sling").withScriptEngine("htl").withScriptExtension("html")
                            .build(),
    
                    // sling/test
                    ProvidedResourceTypeCapability.builder().withResourceType("/libs/sling/test").withResourceType("sling/test")
                            .withScriptEngine("htl").withScriptExtension("html").build()
            ));
    
            Set<RequiredResourceTypeCapability> rExpected = new HashSet<>(Arrays.asList(
                    RequiredResourceTypeCapability.builder().withResourceType("sling/default")
                            .withVersionRange(VersionRange.valueOf("[1.0.0,2.0.0)")).withIsOptional().build(),
                    RequiredResourceTypeCapability.builder().withResourceType("org/apache/sling/bar").build(),
                    RequiredResourceTypeCapability.builder().withResourceType("org/apache/sling/bar")
                            .withVersionRange(VersionRange.valueOf("[1.0.0,2.0.0)")).withIsOptional().build()
            ));
            Set<ProvidedScriptCapability> sExpected = new HashSet<>(Arrays.asList(
                    ProvidedScriptCapability.builder(scriptEngineMappings)
                            .withPath("/org.apache.sling.wrongbar/wrongbar.has.too.many.selectors.html").build()
            ));
            verifyCapabilities(capabilities, pExpected, rExpected, sExpected);
        } finally {
            cleanUp("project-1");
        }
    }

    @Test
    public void testProject2() throws Exception {
        try {
            PluginExecution execution = executePluginOnProject("project-2");
            Capabilities capabilities = execution.getCapabilities();
            Map<String, String> scriptEngineMappings = execution.getScriptEngineMappings();
            Set<ProvidedResourceTypeCapability> pExpected = new HashSet<>(Arrays.asList(
                    ProvidedResourceTypeCapability.builder().withResourceType("libs/sling/test").withScriptEngine("thymeleaf")
                            .withScriptExtension("html").build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("libs/sling/test").withScriptEngine("rhino")
                            .withScriptExtension("js").withSelectors(Arrays.asList("merged")).build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("libs/sling/test2").withScriptEngine("jsp")
                            .withScriptExtension("jsp").withRequestExtension("html").build()
            ));
            Set<ProvidedScriptCapability> expectedScriptCapabilities = new HashSet<>(Arrays.asList(
                    ProvidedScriptCapability.builder(scriptEngineMappings).withPath("/libs/sling/commons/template.html").build(),
                    ProvidedScriptCapability.builder(scriptEngineMappings).withPath("/libs/sling/utils/initialiseWarpDrive.html").build()
            ));
            Set<RequiredResourceTypeCapability> expectedRequired = new HashSet<>(Arrays.asList(
                    RequiredResourceTypeCapability.builder().withResourceType("sling/scripting/warpDrive")
                            .withVersionRange(VersionRange.valueOf("[1.0.0,2.0.0)")).withIsOptional().build()
            ));
            verifyCapabilities(capabilities, pExpected, expectedRequired, expectedScriptCapabilities);
        } finally {
            cleanUp("project-2");
        }
    }

    @Test
    public void testIncludeExcludes() throws Exception {
        try {
            PluginExecution execution = executePluginOnProject("project-3");
            Capabilities capabilities = execution.getCapabilities();
            Set<ProvidedResourceTypeCapability> pExpected = new HashSet<>(Arrays.asList(
                    ProvidedResourceTypeCapability.builder().withResourceType("sling/scriptingbundle/includeexclude")
                            .withScriptEngine("htl").withScriptExtension("html").build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("sling/scriptingbundle/includeexclude")
                            .withSelectors(Arrays.asList("selector")).withScriptEngine("htl").withScriptExtension("html")
                            .build()
            ));
            verifyCapabilities(capabilities, pExpected, Collections.emptySet(), Collections.emptySet());
        } finally {
            cleanUp("project-3");
        }
    }

    @Test
    public void testFileVault1() throws Exception {
        try {
            PluginExecution execution = executePluginOnProject("filevault-1");
            Capabilities capabilities = execution.getCapabilities();
            Set<ProvidedResourceTypeCapability> pExpected = new HashSet<>(Arrays.asList(
                ProvidedResourceTypeCapability.builder().withResourceTypes("my-scripts/image", "/apps/my-scripts/image")
                        .withScriptEngine("htl").withScriptExtension("html").withExtendsResourceType("generic/image").build(),
                ProvidedResourceTypeCapability.builder().withResourceTypes("my-scripts/teaser", "/apps/my-scripts/teaser")
                        .withScriptEngine("htl").withScriptExtension("html").build()
            ));
            Set<RequiredResourceTypeCapability> rExpected = new HashSet<>(Arrays.asList(
               RequiredResourceTypeCapability.builder().withResourceType("generic/image").withIsOptional().build(),
               RequiredResourceTypeCapability.builder().withResourceType("required/one").withIsOptional().build(),
               RequiredResourceTypeCapability.builder().withResourceType("required/two").withIsOptional().build(),
               RequiredResourceTypeCapability.builder().withResourceType("my-scripts/image").build()
            ));
            verifyCapabilities(capabilities, pExpected, rExpected, Collections.emptySet());
        } finally {
            cleanUp("filevault-1");
        }
    }

    @Test
    public void testProject4() throws Exception {
        try {
            PluginExecution execution = executePluginOnProject("project-4");
            Capabilities capabilities = execution.getCapabilities();
            Set<ProvidedResourceTypeCapability> pExpected = new HashSet<>(Arrays.asList(
                    ProvidedResourceTypeCapability.builder().withResourceTypes("components/test", "/apps/components/test")
                            .withScriptEngine("htl").withScriptExtension("html").build()
            ));
            Set<RequiredResourceTypeCapability> rExpected = new HashSet<>(Arrays.asList(
                    RequiredResourceTypeCapability.builder().withResourceType("components/testhelper").build()
            ));
            verifyCapabilities(capabilities, pExpected, rExpected, Collections.emptySet());
        } finally {
            cleanUp("project-4");
        }
    }

    private void verifyCapabilities(Capabilities capabilities, Set<ProvidedResourceTypeCapability> pExpected, Set<RequiredResourceTypeCapability> rExpected, Set<ProvidedScriptCapability> sExpected) {
        Set<ProvidedResourceTypeCapability> provided = new HashSet<>(capabilities.getProvidedResourceTypeCapabilities());
        StringBuilder missingProvided = new StringBuilder();
        for (ProvidedResourceTypeCapability capability : pExpected) {
            boolean removed = provided.remove(capability);
            if (!removed) {
                missingProvided.append("Missing capability: ").append(capability.toString()).append(System.lineSeparator());
            }
        }
        if (missingProvided.length() > 0) {
            fail(missingProvided.toString());
        }
        StringBuilder extraProvided = new StringBuilder();
        for (ProvidedResourceTypeCapability capability : provided) {
            extraProvided.append("Extra provided capability: ").append(capability.toString()).append(System.lineSeparator());
        }
        if (extraProvided.length() > 0) {
            fail(extraProvided.toString());
        }
    
        Set<RequiredResourceTypeCapability> required = new HashSet<>(capabilities.getRequiredResourceTypeCapabilities());
        assertEquals(rExpected.size(), required.size());
        StringBuilder missingRequired = new StringBuilder();
        for (RequiredResourceTypeCapability capability : rExpected) {
            boolean removed = required.remove(capability);
            if (!removed) {
                missingRequired.append("Missing required capability: ").append(capability.toString()).append(System.lineSeparator());
            }
        }
        if (missingRequired.length() > 0) {
            fail(missingRequired.toString());
        }
        StringBuilder extraRequired = new StringBuilder();
        for (RequiredResourceTypeCapability capability : required) {
            extraRequired.append("Extra required capability: ").append(capability.toString()).append(System.lineSeparator());
        }
        if (extraRequired.length() > 0) {
            fail(extraRequired.toString());
        }
    
        Set<ProvidedScriptCapability> providedScriptCapabilities = new HashSet<>(capabilities.getProvidedScriptCapabilities());
        assertEquals(sExpected.size(), providedScriptCapabilities.size());
        StringBuilder missingProvidedScripts = new StringBuilder();
        for (ProvidedScriptCapability capability : sExpected) {
            boolean removed = providedScriptCapabilities.remove(capability);
            if (!removed) {
                missingProvidedScripts.append("Missing script capability: ").append(capability.toString()).append(System.lineSeparator());
            }
        }
        if (missingProvidedScripts.length() > 0) {
            fail(missingProvidedScripts.toString());
        }
        StringBuilder extraProvidedScripts = new StringBuilder();
        for (ProvidedScriptCapability capability : providedScriptCapabilities) {
            extraProvidedScripts.append("Extra provided script capability: ").append(capability.toString()).append(System.lineSeparator());
        }
        if (extraProvidedScripts.length() > 0) {
            fail(extraProvidedScripts.toString());
        }
    }
}
