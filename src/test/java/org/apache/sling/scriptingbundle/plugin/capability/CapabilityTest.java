/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.scriptingbundle.plugin.capability;

import java.util.Collections;
import java.util.LinkedHashSet;
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
        String expectedHeaderValue =
                "sling.servlet;sling.servlet.resourceTypes:List<String>=\"my/type,/libs/my/type\";version:Version=\"2.1.0\";sling.servlet.methods=POST;sling.servlet.extensions=json;sling.servlet.selectors:List<String>=\"selector1,selector\\,2\"";
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
        String expectedHeaderValue =
                "sling.servlet;filter:=\"(&(!(sling.servlet.selectors=*))(&(&(version=*)(!(version<=1.0.0))(!(version>=3.0.0)))(sling.servlet.resourceTypes=my/type)))\";resolution:=optional"
                        + ",sling.servlet;filter:=\"(&(!(sling.servlet.selectors=*))(sling.servlet.resourceTypes=/other/type))\"";
        Assert.assertEquals(expectedHeaderValue, caps.getRequiredCapabilitiesString());
    }
}
