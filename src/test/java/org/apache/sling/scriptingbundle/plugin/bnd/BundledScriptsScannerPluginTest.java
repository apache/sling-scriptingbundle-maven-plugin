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
package org.apache.sling.scriptingbundle.plugin.bnd;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;

import org.apache.commons.io.FileUtils;
import org.apache.sling.scriptingbundle.plugin.capability.Capabilities;
import org.apache.sling.scriptingbundle.plugin.capability.ProvidedResourceTypeCapability;
import org.apache.sling.scriptingbundle.plugin.capability.ProvidedScriptCapability;
import org.apache.sling.scriptingbundle.plugin.capability.RequiredResourceTypeCapability;
import org.apache.sling.scriptingbundle.plugin.bnd.BundledScriptsScannerPlugin;
import org.junit.Test;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class BundledScriptsScannerPluginTest {

    @Test
    public void testProject1() throws Exception {
        try (Builder builder = prepareBuilder("project-1")) {
            BundledScriptsScannerPlugin plugin = builder.getPlugin(BundledScriptsScannerPlugin.class);
            assertNotNull(plugin);
            builder.build();
            Capabilities capabilities = plugin.getCapabilities();
            Map<String, String> scriptEngineMappings = plugin.getScriptEngineMappings();
            Set<ProvidedResourceTypeCapability> pExpected = new HashSet<>(Arrays.asList(
                    // org/apache/sling/bar/1.0.0
                    ProvidedResourceTypeCapability.builder().withResourceType("org/apache/sling/bar").withScriptEngine("htl")
                            .withScriptExtension("html").withVersion(new Version("1.0.0")).build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("org/apache/sling/bar").withScriptEngine("htl")
                            .withScriptExtension("html").withVersion(new Version("1.0.0")).withSelectors(new LinkedHashSet<>(Arrays.asList("depth1"
                            , "100"))).build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("org/apache/sling/bar").withScriptEngine("htl")
                            .withScriptExtension("html").withVersion(new Version("1.0.0")).withSelectors(new LinkedHashSet<>(Arrays.asList("depth1"
                            , "200"))).build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("org/apache/sling/bar").withScriptEngine("htl")
                            .withScriptExtension("html").withVersion(new Version("1.0.0")).withSelectors(new LinkedHashSet<>(Arrays.asList("depth1"
                            , "depth2", "100"))).build(),

                    // org/apache/sling/foo
                    ProvidedResourceTypeCapability.builder().withResourceType("org/apache/sling/foo").withScriptEngine("htl")
                            .withScriptExtension("html").build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("org/apache/sling/foo").withScriptEngine("htl")
                            .withScriptExtension("html").withSelectors(new LinkedHashSet<>(Arrays.asList("depth1"
                            , "100"))).build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("org/apache/sling/foo").withScriptEngine("htl")
                            .withScriptExtension("html").withSelectors(new LinkedHashSet<>(Arrays.asList("depth1"
                            , "200"))).build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("org/apache/sling/foo").withScriptEngine("htl")
                            .withScriptExtension("html").withSelectors(new LinkedHashSet<>(Arrays.asList("depth1"
                            , "depth2", "100"))).build(),

                    // org/apache/sling/foo/depth1/depth2/depth3
                    ProvidedResourceTypeCapability.builder().withResourceType("org/apache/sling/foo/depth1/depth2/depth3")
                            .withExtendsResourceType("org" +
                                    "/apache/sling/bar").build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("org/apache/sling/foo/depth1/depth2/depth3")
                            .withScriptEngine("htl").withScriptExtension("html")
                            .withSelectors(new LinkedHashSet<>(Arrays.asList("depth3-selector"))).build(),

                    // org.apache.sling.foobar/1.0.0
                    ProvidedResourceTypeCapability.builder().withResourceType("org.apache.sling.foobar").withScriptEngine("htl")
                            .withScriptExtension("html").withVersion(new Version("1.0.0")).withExtendsResourceType("org/apache/sling/bar").build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("org.apache.sling.foobar").withScriptEngine("htl")
                            .withScriptExtension("html").withVersion(new Version("1.0.0")).withSelectors(new LinkedHashSet<>(Arrays.asList("depth1"
                            , "100"))).build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("org.apache.sling.foobar").withScriptEngine("htl")
                            .withScriptExtension("html").withVersion(new Version("1.0.0")).withSelectors(new LinkedHashSet<>(Arrays.asList("depth1"
                            , "200"))).build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("org.apache.sling.foobar").withScriptEngine("htl")
                            .withScriptExtension("html").withVersion(new Version("1.0.0")).withSelectors(new LinkedHashSet<>(Arrays.asList("depth1"
                            , "depth2", "100"))).build(),

                    // org.apache.sling.foobar
                    ProvidedResourceTypeCapability.builder().withResourceType("org.apache.sling.foobar")
                            .withExtendsResourceType("org/apache/sling/bar").build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("org.apache.sling.foobar").withScriptEngine("htl")
                            .withScriptExtension("html").withSelectors(new LinkedHashSet<>(Arrays.asList("depth1"
                            , "100"))).build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("org.apache.sling.foobar").withScriptEngine("htl")
                            .withScriptExtension("html").withSelectors(new LinkedHashSet<>(Arrays.asList("depth1"
                            , "200"))).build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("org.apache.sling.foobar").withScriptEngine("htl")
                            .withScriptExtension("html").withSelectors(new LinkedHashSet<>(Arrays.asList("depth1"
                            , "depth2", "100"))).build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("org.apache.sling.foobar").withScriptEngine("htl")
                            .withScriptExtension("html").withRequestMethod("GET").build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("org.apache.sling.foobar").withScriptEngine("htl")
                            .withScriptExtension("html").withRequestMethod("GET").withSelectors(new LinkedHashSet<>(Arrays.asList("test")))
                            .build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("org.apache.sling.foobar").withScriptEngine("htl")
                            .withScriptExtension("html").withRequestMethod("GET").withSelectors(new LinkedHashSet<>(Arrays.asList("test")))
                            .withRequestExtension("txt").build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("org.apache.sling.foobar").withScriptEngine("htl")
                            .withScriptExtension("html").withSelectors(new LinkedHashSet<>(Arrays.asList("test")))
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
                            .withVersionRange(VersionRange.valueOf("[1.0.0,2.0.0)")).build(),
                    RequiredResourceTypeCapability.builder().withResourceType("org/apache/sling/bar").build(),
                    RequiredResourceTypeCapability.builder().withResourceType("org/apache/sling/bar")
                            .withVersionRange(VersionRange.valueOf("[1.0.0,2.0.0)")).withIsOptional().build()
            ));
            Set<ProvidedScriptCapability> sExpected = new HashSet<>(Arrays.asList(
                    ProvidedScriptCapability.builder(scriptEngineMappings)
                            .withPath("/org.apache.sling.wrongbar/wrongbar.has.too.many.selectors.html").build()
            ));
            Set<RequiredResourceTypeCapability> urExpected = new HashSet<>(Arrays.asList(
                    RequiredResourceTypeCapability.builder().withResourceType("sling/default")
                            .withVersionRange(VersionRange.valueOf("[1.0.0,2.0.0)")).build()
            ));
            verifyCapabilities(capabilities, pExpected, rExpected, sExpected, urExpected);
        } finally {
            cleanUp("project-1");
        }
    }

    @Test
    public void testProject2() throws Exception {
        try (Builder builder = prepareBuilder("project-2")) {
            BundledScriptsScannerPlugin plugin = builder.getPlugin(BundledScriptsScannerPlugin.class);
            assertNotNull(plugin);
            builder.build();
            Capabilities capabilities = plugin.getCapabilities();
            Map<String, String> scriptEngineMappings = plugin.getScriptEngineMappings();
            Set<ProvidedResourceTypeCapability> pExpected = new HashSet<>(Arrays.asList(
                    ProvidedResourceTypeCapability.builder().withResourceType("libs/sling/test").withScriptEngine("thymeleaf")
                            .withScriptExtension("html").build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("libs/sling/test").withScriptEngine("rhino")
                            .withScriptExtension("js").withSelectors(new HashSet<>(Arrays.asList("merged"))).build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("libs/sling/test2").withScriptEngine("jsp")
                            .withScriptExtension("jsp").withRequestExtension("html").build()
            ));
            Set<ProvidedScriptCapability> expectedScriptCapabilities = new HashSet<>(Arrays.asList(
                    ProvidedScriptCapability.builder(scriptEngineMappings).withPath("/libs/sling/commons/template.html").build(),
                    ProvidedScriptCapability.builder(scriptEngineMappings).withPath("/libs/sling/utils/initialiseWarpDrive.html").build()
            ));
            Set<RequiredResourceTypeCapability> expectedRequired = new HashSet<>(Arrays.asList(
                    RequiredResourceTypeCapability.builder().withResourceType("sling/scripting/warpDrive")
                            .withVersionRange(VersionRange.valueOf("[1.0.0,2.0.0)")).build()
            ));
            Set<RequiredResourceTypeCapability> expectedUnresolvedRequired = new HashSet<>(Arrays.asList(
                    RequiredResourceTypeCapability.builder().withResourceType("sling/scripting/warpDrive")
                            .withVersionRange(VersionRange.valueOf("[1.0.0,2.0.0)")).build()
            ));
            verifyCapabilities(capabilities, pExpected, expectedRequired, expectedScriptCapabilities, expectedUnresolvedRequired);
        } finally {
            cleanUp("project-2");
        }
    }

    @Test
    public void testIncludeExcludes() throws Exception {
        try (Builder builder = prepareBuilder("project-3")) {
            BundledScriptsScannerPlugin plugin = builder.getPlugin(BundledScriptsScannerPlugin.class);
            assertNotNull(plugin);
            builder.build();
            Capabilities capabilities = plugin.getCapabilities();
            Set<ProvidedResourceTypeCapability> pExpected = new HashSet<>(Arrays.asList(
                    ProvidedResourceTypeCapability.builder().withResourceType("sling/scriptingbundle/includeexclude")
                            .withScriptEngine("htl").withScriptExtension("html").build(),
                    ProvidedResourceTypeCapability.builder().withResourceType("sling/scriptingbundle/includeexclude")
                            .withSelectors(new HashSet<>(Arrays.asList("selector"))).withScriptEngine("htl").withScriptExtension("html")
                            .build()
            ));
            verifyCapabilities(capabilities, pExpected, Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
        } finally {
            cleanUp("project-3");
        }
    }

    private Builder prepareBuilder(String projectName) throws IOException {
        Path projectRootFolder = Paths.get("src", "test", "resources", "bnd", projectName);
        Path projectTargetFolder = projectRootFolder.resolve("target");
        Path projectClassesFolder = projectTargetFolder.resolve("classes");
        Files.createDirectories(projectClassesFolder);
        Builder builder = new Builder();
        Jar jar = new Jar("test.jar", projectClassesFolder.toFile());
        jar.setManifest(new Manifest());
        builder.setJar(jar);
        File bndFile = projectRootFolder.resolve("bnd.bnd").toFile();
        builder.setProperties(bndFile.getParentFile(), builder.loadProperties(bndFile));
        builder.set(BundledScriptsScannerPlugin.PROJECT_ROOT_FOLDER, projectRootFolder.toString());
        builder.set(BundledScriptsScannerPlugin.PROJECT_BUILD_FOLDER, projectTargetFolder.toString());
        return builder;
    }

    private void cleanUp(String projectName) throws IOException {
        Path projectTargetFolder = Paths.get("src", "test", "resources", "bnd", projectName, "target");
        FileUtils.forceDeleteOnExit(projectTargetFolder.toFile());
    }

    private void verifyCapabilities(Capabilities capabilities, Set<ProvidedResourceTypeCapability> pExpected,
                                    Set<RequiredResourceTypeCapability> rExpected, Set<ProvidedScriptCapability> sExpected,
                                    Set<RequiredResourceTypeCapability> urExpected) {
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

        Set<RequiredResourceTypeCapability> unresolvedRequired =
                new HashSet<>(capabilities.getUnresolvedRequiredResourceTypeCapabilities());
        assertEquals(urExpected.size(), unresolvedRequired.size());
        StringBuilder missingUnresolvedRequired = new StringBuilder();
        for (RequiredResourceTypeCapability capability : urExpected) {
            boolean removed = unresolvedRequired.remove(capability);
            if (!removed) {
                missingUnresolvedRequired.append("Missing unresolved required capability: ").append(capability.toString())
                        .append(System.lineSeparator());
            }
        }
        if (missingUnresolvedRequired.length() > 0) {
            fail(missingUnresolvedRequired.toString());
        }
        StringBuilder extraUnresolvedRequired = new StringBuilder();
        for (RequiredResourceTypeCapability capability : unresolvedRequired) {
            extraUnresolvedRequired.append("Extra unresolved required capability: ").append(capability.toString())
                    .append(System.lineSeparator());
        }
        if (extraUnresolvedRequired.length() > 0) {
            fail(extraUnresolvedRequired.toString());
        }
    }
}
