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
package org.apache.sling.scriptingbundle.plugin.capability;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

public class CapabilityTest {

    @Test
    public void testGetProvidedCapabilitiesString() {
        Set<ProvidedResourceTypeCapability> resourceTypeCaps = new LinkedHashSet<>();
        resourceTypeCaps.add(ProvidedResourceTypeCapability.builder()
                .withResourceTypes("my/type", "/libs/my/type")
                .withVersion(new Version("2.1.0"))
                .withRequestExtension("json")
                .withRequestMethod("POST")
                .withSelectors("selector1", "selector,2")
                .build());
        // TODO: add script capabilities
        Capabilities caps = new Capabilities(resourceTypeCaps, Collections.emptySet(), Collections.emptySet());
        String expectedHeaderValue = "sling.servlet;sling.servlet.resourceTypes:List<String>=\"my/type,/libs/my/type\";version:Version=\"2.1.0\";sling.servlet.methods=POST;sling.servlet.extensions=json;sling.servlet.selectors:List<String>=\"selector1,selector\\,2\"";
        Assert.assertEquals(expectedHeaderValue, caps.getProvidedCapabilitiesString());
    }

    @Test
    public void testGetRequiredCapabilitiesString() {
        Set<RequiredResourceTypeCapability> resourceTypeCaps = new LinkedHashSet<>();
        resourceTypeCaps.add(RequiredResourceTypeCapability.builder()
                .withResourceType("my/type")
                .withVersionRange(new VersionRange("(1.0,3.0)"))
                .withIsOptional()
                .build());
        resourceTypeCaps.add(RequiredResourceTypeCapability.builder()
                .withResourceType("/other/type")
                .build());
        Capabilities caps = new Capabilities(Collections.emptySet(), Collections.emptySet(), resourceTypeCaps);
        String expectedHeaderValue = "sling.servlet;filter:=\"(&(!(sling.servlet.selectors=*))(&(&(version=*)(!(version<=1.0.0))(!(version>=3.0.0)))(sling.servlet.resourceTypes=my/type)))\";resolution:=optional" + 
        ",sling.servlet;filter:=\"(&(!(sling.servlet.selectors=*))(sling.servlet.resourceTypes=/other/type))\"";
        Assert.assertEquals(expectedHeaderValue, caps.getRequiredCapabilitiesString());
    }

    /**
     * Test that parentheses in script paths are properly escaped in provided capabilities.
     * This is the main test for the footer(v2) issue.
     */
    @Test
    public void testProvidedScriptCapabilityWithParenthesesInPath() {
        Map<String, String> scriptEngineMappings = new HashMap<>();
        scriptEngineMappings.put("html", "htl");
        
        Set<ProvidedScriptCapability> scriptCaps = new LinkedHashSet<>();
        scriptCaps.add(ProvidedScriptCapability.builder(scriptEngineMappings)
                .withPath("/apps/corp/globals/components/content/footer(v2)/footer.html")
                .build());
        scriptCaps.add(ProvidedScriptCapability.builder(scriptEngineMappings)
                .withPath("/apps/test/component(with)parentheses/script.html")
                .build());
        
        Capabilities caps = new Capabilities(Collections.emptySet(), scriptCaps, Collections.emptySet());
        String result = caps.getProvidedCapabilitiesString();
        
        // Verify parentheses are escaped with single backslashes
        Assert.assertTrue("Path should contain escaped parentheses: " + result, result.contains("footer\\(v2\\)"));
        Assert.assertTrue("Path should contain escaped parentheses: " + result, result.contains("\\(with\\)"));
        
        // Verify the complete capability strings
        Assert.assertTrue(result.contains("sling.servlet.paths=\"/apps/corp/globals/components/content/footer\\(v2\\)/footer.html\""));
        Assert.assertTrue(result.contains("sling.servlet.paths=\"/apps/test/component\\(with\\)parentheses/script.html\""));
    }

    /**
     * Test that parentheses in resource types are properly escaped in provided capabilities.
     */
    @Test
    public void testProvidedResourceTypeCapabilityWithParenthesesInResourceType() {
        Set<ProvidedResourceTypeCapability> resourceTypeCaps = new LinkedHashSet<>();
        resourceTypeCaps.add(ProvidedResourceTypeCapability.builder()
                .withResourceTypes("my/type(v2)", "/libs/component(test)/type")
                .withVersion(new Version("2.1.0"))
                .withRequestExtension("json")
                .build());
        
        Capabilities caps = new Capabilities(resourceTypeCaps, Collections.emptySet(), Collections.emptySet());
        String result = caps.getProvidedCapabilitiesString();
        
        // Verify parentheses are escaped (List<String> output shows double backslashes)
        Assert.assertTrue("Resource type " + result + " should contain escaped parentheses", result.contains("my/type\\\\(v2\\\\)"));
        Assert.assertTrue("Resource type " + result + "  should contain escaped parentheses", result.contains("/libs/component\\\\(test\\\\)/type"));
    }

    /**
     * Test that parentheses in extended resource types are properly escaped.
     */
    @Test
    public void testProvidedCapabilityWithParenthesesInExtendsResourceType() {
        Set<ProvidedResourceTypeCapability> resourceTypeCaps = new LinkedHashSet<>();
        resourceTypeCaps.add(ProvidedResourceTypeCapability.builder()
                .withResourceType("my/type")
                .withExtendsResourceType("parent/type(v1)")
                .build());
        
        Capabilities caps = new Capabilities(resourceTypeCaps, Collections.emptySet(), Collections.emptySet());
        String result = caps.getProvidedCapabilitiesString();
        
        // Verify parentheses in extends attribute are escaped with single backslashes
        Assert.assertTrue("Extended resource type should contain escaped parentheses: " + result, 
                result.contains("parent/type\\(v1\\)"));
    }

    /**
     * Test that parentheses in required resource types are properly escaped in filter strings.
     */
    @Test
    public void testRequiredCapabilityWithParenthesesInResourceType() {
        Set<RequiredResourceTypeCapability> resourceTypeCaps = new LinkedHashSet<>();
        resourceTypeCaps.add(RequiredResourceTypeCapability.builder()
                .withResourceType("my/type(v2)")
                .build());
        
        Capabilities caps = new Capabilities(Collections.emptySet(), Collections.emptySet(), resourceTypeCaps);
        String result = caps.getRequiredCapabilitiesString();
        
        // Verify parentheses are escaped in the filter string with single backslashes
        Assert.assertTrue("Filter should contain escaped resource type: " + result, 
                result.contains("sling.servlet.resourceTypes=my/type\\(v2\\)"));
        
        // Verify the complete filter is valid
        Assert.assertTrue("Complete filter should be valid: " + result,
                result.contains("filter:=\"(&(!(sling.servlet.selectors=*))(sling.servlet.resourceTypes=my/type\\(v2\\)))\""));
    }

    /**
     * Test that other special filter characters (backslash, asterisk) are also properly escaped.
     */
    @Test
    public void testSpecialCharactersEscaping() {
        Map<String, String> scriptEngineMappings = new HashMap<>();
        scriptEngineMappings.put("html", "htl");
        
        Set<ProvidedScriptCapability> scriptCaps = new LinkedHashSet<>();
        // Test backslash escaping (backslash should become double-backslash)
        scriptCaps.add(ProvidedScriptCapability.builder(scriptEngineMappings)
                .withPath("/apps/test\\path/script.html")
                .build());
        // Test asterisk escaping
        scriptCaps.add(ProvidedScriptCapability.builder(scriptEngineMappings)
                .withPath("/apps/test*wildcard/script.html")
                .build());
        
        Capabilities caps = new Capabilities(Collections.emptySet(), scriptCaps, Collections.emptySet());
        String result = caps.getProvidedCapabilitiesString();
        
        // Verify special characters are escaped 
        // Backslash gets escaped to \\ (4 backslashes in Java = 2 in string)
        // Asterisk gets escaped to \* (2 backslashes in Java = 1 in string)
        Assert.assertTrue("Backslash should be escaped: " + result, result.contains("test\\\\path"));
        Assert.assertTrue("Asterisk should be escaped: " + result, result.contains("test\\*wildcard"));
    }

    /**
     * Test that paths without special characters remain unchanged.
     */
    @Test
    public void testNormalPathsUnchanged() {
        Map<String, String> scriptEngineMappings = new HashMap<>();
        scriptEngineMappings.put("html", "htl");
        
        Set<ProvidedScriptCapability> scriptCaps = new LinkedHashSet<>();
        scriptCaps.add(ProvidedScriptCapability.builder(scriptEngineMappings)
                .withPath("/apps/myapp/components/footer/footer.html")
                .build());
        
        Capabilities caps = new Capabilities(Collections.emptySet(), scriptCaps, Collections.emptySet());
        String result = caps.getProvidedCapabilitiesString();
        
        // Verify normal paths work correctly without unnecessary escaping
        Assert.assertTrue(result.contains("sling.servlet.paths=\"/apps/myapp/components/footer/footer.html\""));
        // Verify no backslashes were added where they shouldn't be
        Assert.assertFalse("Should not contain escaped forward slashes", result.contains("\\/"));
    }
}
