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
package org.apache.sling.scriptingbundle.plugin.processor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.sling.api.resource.type.ResourceType;
import org.apache.sling.scriptingbundle.plugin.capability.ProvidedResourceTypeCapability;
import org.apache.sling.scriptingbundle.plugin.capability.RequiredResourceTypeCapability;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.VersionRange;

public class FileProcessorTest {

    private FileProcessor processor;
    Set<ProvidedResourceTypeCapability> providedCapabilities;
    Set<RequiredResourceTypeCapability> requiredCapabilities;
    private static final ResourceType MY_RESOURCE_TYPE = ResourceType.parseResourceType("apps/my/resource");

    @Before
    public void setUp() {
        processor = new FileProcessor(new Slf4jLogger(), Constants.DEFAULT_SEARCH_PATHS, Constants.DEFAULT_EXTENSION_TO_SCRIPT_ENGINE_MAPPING);
        providedCapabilities = new HashSet<>();
        requiredCapabilities = new HashSet<>();
    }

    @Test
    public void testExtendsValid() {
        Path extendsFile = Paths.get("src", "test", "resources", "extends", "valid");
        processor.processExtendsFile(MY_RESOURCE_TYPE, extendsFile, providedCapabilities, requiredCapabilities);
        Assert.assertEquals(1, requiredCapabilities.size());
        RequiredResourceTypeCapability expectedRequiredCapability = RequiredResourceTypeCapability.builder()
                .withResourceType("org/apache/sling/bar")
                .withVersionRange(new VersionRange("[1.0.0,2.0.0)"))
                .withIsOptional().build();
        Assert.assertEquals(expectedRequiredCapability, requiredCapabilities.iterator().next());
        Assert.assertEquals(1, providedCapabilities.size());
        ProvidedResourceTypeCapability expectedProvidedCapability = ProvidedResourceTypeCapability.builder()
                .withResourceTypes(new HashSet<>(Arrays.asList("my/resource", "/apps/my/resource")))
                .withVersion(MY_RESOURCE_TYPE.getVersion())
                .withExtendsResourceType("org/apache/sling/bar")
                .build();
        Assert.assertEquals(expectedProvidedCapability, providedCapabilities.iterator().next());
    }

    @Test
    public void testExtendsMultipleClauses() {
        Path extendsFile = Paths.get("src", "test", "resources", "extends", "multiple-clauses");
        Assert.assertThrows(IllegalArgumentException.class, () -> { processor.processExtendsFile(MY_RESOURCE_TYPE, extendsFile, providedCapabilities, requiredCapabilities); });
    }

    @Test
    public void testExtendsInvalidAttributes() {
        Path extendsFile = Paths.get("src", "test", "resources", "extends", "invalid-attributes");
        Assert.assertThrows(IllegalArgumentException.class, () -> { processor.processExtendsFile(MY_RESOURCE_TYPE, extendsFile, providedCapabilities, requiredCapabilities); });
    }

    @Test
    public void testExtendsMultipleLines() {
        Path extendsFile = Paths.get("src", "test", "resources", "extends", "multiple-lines");
        Assert.assertThrows(IllegalArgumentException.class, () -> { processor.processExtendsFile(MY_RESOURCE_TYPE, extendsFile, providedCapabilities, requiredCapabilities); });
    }

    @Test
    public void testRequiresValid() {
        Path requiresFile = Paths.get("src", "test", "resources", "requires", "valid");
        processor.processRequiresFile(requiresFile, requiredCapabilities);
        Assert.assertEquals(1, requiredCapabilities.size());
        RequiredResourceTypeCapability expectedCapability = RequiredResourceTypeCapability.builder()
                .withResourceType("org/apache/sling/bar")
                .withVersionRange(new VersionRange("[1.0.0,2.0.0)"))
                .withIsOptional().build();
        Assert.assertEquals(expectedCapability, requiredCapabilities.iterator().next());
    }

    @Test
    public void testRequiresMultipleClauses() {
        Path requiresFile = Paths.get("src", "test", "resources", "requires", "multiple-clauses");
        Assert.assertThrows(IllegalArgumentException.class, () -> { processor.processRequiresFile(requiresFile, requiredCapabilities); });
    }

    @Test
    public void testRequiresInvalidAttributes() {
        Path requiresFile = Paths.get("src", "test", "resources", "extends", "invalid-attributes");
        Assert.assertThrows(IllegalArgumentException.class, () -> { processor.processRequiresFile(requiresFile, requiredCapabilities); });
    }

    @Test
    public void testScriptValid() {
        Path resourceTypeFolder = Paths.get("apps", "my", "resource", "2.0");
        Path script = Paths.get("apps", "my", "resource", "2.0", "selectorb", "selectora.POST.html");
        processor.processScriptFile(resourceTypeFolder, script, MY_RESOURCE_TYPE, providedCapabilities, false);
        Assert.assertEquals(1, providedCapabilities.size());
        ProvidedResourceTypeCapability expectedProvidedCapability = ProvidedResourceTypeCapability.builder()
                .withResourceTypes(new HashSet<>(Arrays.asList("my/resource", "/apps/my/resource")))
                .withVersion(MY_RESOURCE_TYPE.getVersion())
                .withRequestMethod("POST")
                .withSelectors(Arrays.asList("selectorb", "selectora"))
                .withScriptEngine("htl")
                .withScriptExtension("html")
                .build();
        Assert.assertEquals(expectedProvidedCapability, providedCapabilities.iterator().next());
    }

    @Test
    public void testMainScriptSelector() {
        Path resourceTypeFolder = Paths.get("apps", "my", "resource", "test");
        Path script = Paths.get("apps", "my", "resource", "test", "test.POST.html");
        processor.processScriptFile(resourceTypeFolder, script, MY_RESOURCE_TYPE, providedCapabilities, false);

        Assert.assertEquals(1, providedCapabilities.size());

        ProvidedResourceTypeCapability expectedProvidedCapability1 = ProvidedResourceTypeCapability.builder()
                .withResourceTypes(new HashSet<>(Arrays.asList("my/resource", "/apps/my/resource")))
                .withRequestMethod("POST")
                .withSelectors(Arrays.asList("test"))
                .withScriptEngine("htl")
                .withScriptExtension("html")
                .build();

        Assert.assertEquals(new HashSet<>(Arrays.asList(expectedProvidedCapability1)), providedCapabilities);

        Path script2 = Paths.get("apps", "my", "resource", "test", "POST.html");
        processor.processScriptFile(resourceTypeFolder, script2, MY_RESOURCE_TYPE, providedCapabilities, false);

        Assert.assertEquals(2, providedCapabilities.size());

        ProvidedResourceTypeCapability expectedProvidedCapability2 = ProvidedResourceTypeCapability.builder()
                .withResourceTypes(new HashSet<>(Arrays.asList("my/resource", "/apps/my/resource")))
                .withRequestMethod("POST")
                .withScriptEngine("htl")
                .withScriptExtension("html")
                .build();

        Assert.assertEquals(new HashSet<>(Arrays.asList(expectedProvidedCapability1, expectedProvidedCapability2)), providedCapabilities);
    }

    @Test
    public void testScriptUnknownExtension() {
        Path resourceTypeFolder = Paths.get("scripts",  "apps", "my", "resource", "2.0");
        Path script = Paths.get("scripts", "apps", "my", "resource", "2.0", "selectorb", "selectora.POST.abc");
        processor.processScriptFile(resourceTypeFolder, script, MY_RESOURCE_TYPE, providedCapabilities, false);
        // this must not throw an exception but a WARN should be emitted in the log to make users aware of potential misconfigurations
        Assert.assertEquals(0, providedCapabilities.size());
    }
}
