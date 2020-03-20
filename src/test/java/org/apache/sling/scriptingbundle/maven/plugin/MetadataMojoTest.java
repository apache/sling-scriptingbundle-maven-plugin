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

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.project.MavenProject;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.VersionRange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class MetadataMojoTest {

    @Rule
    public MojoRule mojoRule = new MojoRule();

    @After
    public void after() {
        System.clearProperty("basedir");
    }

    @Test
    public void testProject1() throws Exception {
        MojoProject mojoProject = getMojoProject(getProjectLocation("project-1"));
        mojoProject.mojo.execute();
        Capabilities capabilities = mojoProject.mojo.getCapabilities();
        Set<ProvidedCapability> pExpected = new HashSet<>(Arrays.asList(
                // org/apache/sling/bar/1.0.0
                ProvidedCapability.builder().withResourceType("org/apache/sling/bar").withScriptEngine("htl").withVersion("1.0.0").build(),
                ProvidedCapability.builder().withResourceType("org/apache/sling/bar").withScriptEngine("htl").withVersion("1.0.0").withSelectors(new LinkedHashSet<>(Arrays.asList("depth1"
                        , "100"))).build(),
                ProvidedCapability.builder().withResourceType("org/apache/sling/bar").withScriptEngine("htl").withVersion("1.0.0").withSelectors(new LinkedHashSet<>(Arrays.asList("depth1"
                        , "200"))).build(),
                ProvidedCapability.builder().withResourceType("org/apache/sling/bar").withScriptEngine("htl").withVersion("1.0.0").withSelectors(new LinkedHashSet<>(Arrays.asList("depth1"
                        , "depth2", "100"))).build(),

                // org/apache/sling/foo
                ProvidedCapability.builder().withResourceType("org/apache/sling/foo").withScriptEngine("htl").build(),
                ProvidedCapability.builder().withResourceType("org/apache/sling/foo").withScriptEngine("htl").withSelectors(new LinkedHashSet<>(Arrays.asList("depth1"
                        , "100"))).build(),
                ProvidedCapability.builder().withResourceType("org/apache/sling/foo").withScriptEngine("htl").withSelectors(new LinkedHashSet<>(Arrays.asList("depth1"
                        , "200"))).build(),
                ProvidedCapability.builder().withResourceType("org/apache/sling/foo").withScriptEngine("htl").withSelectors(new LinkedHashSet<>(Arrays.asList("depth1"
                        , "depth2", "100"))).build(),

                // org/apache/sling/foo/depth1/depth2/depth3
                ProvidedCapability.builder().withResourceType("org/apache/sling/foo/depth1/depth2/depth3").withExtendsResourceType("org" +
                        "/apache/sling/bar").build(),
                ProvidedCapability.builder().withResourceType("org/apache/sling/foo/depth1/depth2/depth3").withScriptEngine("htl").withSelectors(new LinkedHashSet<>(Arrays.asList("depth3-selector"))).build(),

                // org.apache.sling.foobar/1.0.0
                ProvidedCapability.builder().withResourceType("org.apache.sling.foobar").withScriptEngine("htl").withVersion("1.0.0").build(),
                ProvidedCapability.builder().withResourceType("org.apache.sling.foobar").withVersion("1.0.0").withExtendsResourceType("org/apache/sling/bar").build(),
                ProvidedCapability.builder().withResourceType("org.apache.sling.foobar").withScriptEngine("htl").withVersion("1.0.0").withSelectors(new LinkedHashSet<>(Arrays.asList("depth1"
                        , "100"))).build(),
                ProvidedCapability.builder().withResourceType("org.apache.sling.foobar").withScriptEngine("htl").withVersion("1.0.0").withSelectors(new LinkedHashSet<>(Arrays.asList("depth1"
                        , "200"))).build(),
                ProvidedCapability.builder().withResourceType("org.apache.sling.foobar").withScriptEngine("htl").withVersion("1.0.0").withSelectors(new LinkedHashSet<>(Arrays.asList("depth1"
                        , "depth2", "100"))).build(),

                // org.apache.sling.foobar
                ProvidedCapability.builder().withResourceType("org.apache.sling.foobar").withExtendsResourceType("org/apache/sling/bar").build(),
                ProvidedCapability.builder().withResourceType("org.apache.sling.foobar").withScriptEngine("htl").withSelectors(new LinkedHashSet<>(Arrays.asList("depth1"
                        , "100"))).build(),
                ProvidedCapability.builder().withResourceType("org.apache.sling.foobar").withScriptEngine("htl").withSelectors(new LinkedHashSet<>(Arrays.asList("depth1"
                        , "200"))).build(),
                ProvidedCapability.builder().withResourceType("org.apache.sling.foobar").withScriptEngine("htl").withSelectors(new LinkedHashSet<>(Arrays.asList("depth1"
                        , "depth2", "100"))).build(),
                ProvidedCapability.builder().withResourceType("org.apache.sling.foobar").withScriptEngine("htl").withRequestMethod("GET").build(),
                ProvidedCapability.builder().withResourceType("org.apache.sling.foobar").withScriptEngine("htl").withRequestMethod("GET").withSelectors(new LinkedHashSet<>(Arrays.asList("test"))).build(),
                ProvidedCapability.builder().withResourceType("org.apache.sling.foobar").withScriptEngine("htl").withRequestMethod("GET").withSelectors(new LinkedHashSet<>(Arrays.asList("test"))).withRequestExtension("txt").build(),
                ProvidedCapability.builder().withResourceType("org.apache.sling.foobar").withScriptEngine("htl").withSelectors(new LinkedHashSet<>(Arrays.asList("test"))).withRequestExtension("txt").build(),

                // sling
                ProvidedCapability.builder().withResourceType("sling").withScriptEngine("htl").build(),

                // sling/test
                ProvidedCapability.builder().withResourceType("/libs/sling/test").withResourceType("sling/test").withScriptEngine("htl").build()
        ));

        Set<RequiredCapability> rExpected = new HashSet<>(Arrays.asList(
                RequiredCapability.builder().withResourceType("sling/default").withVersionRange(VersionRange.valueOf("[1.0.0,2.0.0)")).build(),
                RequiredCapability.builder().withResourceType("org/apache/sling/bar").build(),
                RequiredCapability.builder().withResourceType("org/apache/sling/bar").withVersionRange(VersionRange.valueOf("[1.0.0,2.0.0)")).build()
        ));
        verifyCapabilities(capabilities, pExpected, rExpected);
    }

    @Test
    public void testProject2() throws Exception {
        MojoProject mojoProject = getMojoProject(getProjectLocation("project-2"));
        mojoProject.mojo.execute();
        Capabilities capabilities = mojoProject.mojo.getCapabilities();
        Set<ProvidedCapability> pExpected = new HashSet<>(Arrays.asList(
                ProvidedCapability.builder().withResourceType("libs/sling/test").withScriptEngine("thymeleaf").build()
        ));
        verifyCapabilities(capabilities, pExpected, Collections.emptySet());
    }

    private void verifyCapabilities(Capabilities capabilities, Set<ProvidedCapability> pExpected, Set<RequiredCapability> rExpected) {
        Set<ProvidedCapability> provided = new HashSet<>(capabilities.getProvidedCapabilities());
        StringBuilder missingProvided = new StringBuilder();
        for (ProvidedCapability capability : pExpected) {
            boolean removed = provided.remove(capability);
            if (!removed) {
                missingProvided.append("Missing capability: ").append(capability.toString()).append(System.lineSeparator());
            }
        }
        if (missingProvided.length() > 0) {
            fail(missingProvided.toString());
        }
        StringBuilder extraProvided = new StringBuilder();
        for (ProvidedCapability capability : provided) {
            extraProvided.append("Extra provided capability: ").append(capability.toString()).append(System.lineSeparator());
        }
        if (extraProvided.length() > 0) {
            fail(extraProvided.toString());
        }

        Set<RequiredCapability> required = new HashSet<>(capabilities.getRequiredCapabilities());
        assertEquals(rExpected.size(), required.size());
        StringBuilder missingRequired = new StringBuilder();
        for (RequiredCapability capability : rExpected) {
            boolean removed = required.remove(capability);
            if (!removed) {
                missingRequired.append("Missing required capability: ").append(capability.toString()).append(System.lineSeparator());
            }
        }
        if (missingRequired.length() > 0) {
            fail(missingRequired.toString());
        }
        StringBuilder extraRequired = new StringBuilder();
        for (RequiredCapability capability : required) {
            extraRequired.append("Extra required capability: ").append(capability.toString()).append(System.lineSeparator());
        }
        if (extraRequired.length() > 0) {
            fail(extraRequired.toString());
        }
    }

    private MojoProject getMojoProject(File projectDirectory) throws Exception {
        MavenProject project = mojoRule.readMavenProject(projectDirectory);
        MavenSession session = mojoRule.newMavenSession(project);
        MojoExecution execution = mojoRule.newMojoExecution("metadata");
        MetadataMojo validateMojo = (MetadataMojo) mojoRule.lookupConfiguredMojo(session, execution);
        MojoProject mojoProject = new MojoProject();
        mojoProject.mojo = validateMojo;
        mojoProject.project = project;
        return mojoProject;
    }

    private static class MojoProject {
        MetadataMojo mojo;
        MavenProject project;
    }

    private File getProjectLocation(String projectName) {
        return Paths.get("src", "test", "resources", projectName).toFile();
    }
}
