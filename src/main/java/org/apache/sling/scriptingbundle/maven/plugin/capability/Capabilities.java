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
package org.apache.sling.scriptingbundle.maven.plugin.capability;

import java.util.Collections;
import java.util.Set;

import org.apache.sling.scriptingbundle.maven.plugin.capability.ProvidedResourceTypeCapability;
import org.apache.sling.scriptingbundle.maven.plugin.capability.ProvidedScriptCapability;
import org.apache.sling.scriptingbundle.maven.plugin.capability.RequiredResourceTypeCapability;
import org.jetbrains.annotations.NotNull;

public class Capabilities {

    private final Set<ProvidedResourceTypeCapability> providedResourceTypeCapabilities;
    private final Set<ProvidedScriptCapability> providedScriptCapabilities;
    private final Set<RequiredResourceTypeCapability> requiredResourceTypeCapabilities;
    public static final Capabilities EMPTY = new Capabilities(Collections.emptySet(), Collections.emptySet(), Collections.emptySet());

    public Capabilities(
            @NotNull Set<ProvidedResourceTypeCapability> providedResourceTypeCapabilities,
            @NotNull Set<ProvidedScriptCapability> providedScriptCapabilities,
            @NotNull Set<RequiredResourceTypeCapability> requiredResourceTypeCapabilities) {
        this.providedResourceTypeCapabilities = providedResourceTypeCapabilities;
        this.providedScriptCapabilities = providedScriptCapabilities;
        this.requiredResourceTypeCapabilities = requiredResourceTypeCapabilities;
    }

    public @NotNull Set<ProvidedResourceTypeCapability> getProvidedResourceTypeCapabilities() {
        return Collections.unmodifiableSet(providedResourceTypeCapabilities);
    }

    public @NotNull Set<ProvidedScriptCapability> getProvidedScriptCapabilities() {
        return providedScriptCapabilities;
    }

    public @NotNull Set<RequiredResourceTypeCapability> getRequiredResourceTypeCapabilities() {
        return Collections.unmodifiableSet(requiredResourceTypeCapabilities);
    }
}
