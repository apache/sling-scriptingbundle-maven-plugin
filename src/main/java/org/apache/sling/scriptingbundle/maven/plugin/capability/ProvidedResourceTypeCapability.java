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
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProvidedResourceTypeCapability {
    private final Set<String> resourceTypes;
    private final String scriptEngine;
    private final String scriptExtension;
    private final String extendsResourceType;
    private final String version;
    private final String requestExtension;
    private final String requestMethod;
    private final Set<String> selectors;

    private ProvidedResourceTypeCapability(@NotNull Set<String> resourceTypes, @Nullable String scriptEngine,
                                           @Nullable String scriptExtension, @Nullable String extendsResourceType,
                                           @Nullable String version, @Nullable String requestExtension, @Nullable String requestMethod,
                                           @NotNull Set<String> selectors) {
        this.resourceTypes = resourceTypes;
        this.scriptEngine = scriptEngine;
        this.scriptExtension = scriptExtension;
        this.extendsResourceType = extendsResourceType;
        this.version = version;
        this.requestExtension = requestExtension;
        this.requestMethod = requestMethod;
        this.selectors = selectors;
    }

    public static Builder builder() {
        return new Builder();
    }

    @NotNull
    public Set<String> getResourceTypes() {
        return Collections.unmodifiableSet(resourceTypes);
    }

    @Nullable
    public String getScriptEngine() {
        return scriptEngine;
    }

    @Nullable
    public String getScriptExtension() {
        return scriptExtension;
    }

    @Nullable
    public String getVersion() {
        return version;
    }

    @Nullable
    public String getRequestExtension() {
        return requestExtension;
    }

    @Nullable
    public String getExtendsResourceType() {
        return extendsResourceType;
    }

    @Nullable
    public String getRequestMethod() {
        return requestMethod;
    }

    @NotNull
    public Set<String> getSelectors() {
        return Collections.unmodifiableSet(selectors);
    }

    @Override
    public int hashCode() {
        return Objects
                .hash(resourceTypes, scriptEngine, scriptExtension, version, requestExtension, extendsResourceType, requestMethod,
                        selectors);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ProvidedResourceTypeCapability) {
            ProvidedResourceTypeCapability other = (ProvidedResourceTypeCapability) obj;
            return Objects.equals(resourceTypes, other.resourceTypes) && Objects.equals(scriptEngine, other.scriptEngine) &&
                    Objects.equals(scriptExtension, other.scriptExtension) &&
                    Objects.equals(version, other.version) && Objects.equals(requestExtension, other.requestExtension) &&
                    Objects.equals(extendsResourceType, other.extendsResourceType) && Objects.equals(requestMethod, other.requestMethod) &&
                    Objects.equals(selectors, other.selectors);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format(
            "%s{resourceTypes=%s, scriptEngine=%s, scriptEngineExtension=%s, version=%s, selectors=%s, requestExtension=%s, " +
                    "requestMethod=%s, extendsResourceType=%s}",
            this.getClass().getSimpleName(), resourceTypes, scriptEngine, scriptExtension, version, selectors, requestExtension,
                requestMethod,
                extendsResourceType
        );
    }

    public static class Builder {
        private Set<String> resourceTypes = new HashSet<>();
        private String scriptEngine;
        private String scriptExtension;
        private String extendsResourceType;
        private String version;
        private String requestExtension;
        private String requestMethod;
        private Set<String> selectors = Collections.emptySet();

        public Builder withResourceTypes(@NotNull Set<String> resourceTypes) {
            if (resourceTypes.isEmpty()) {
                throw new IllegalArgumentException("The script's resourceTypes cannot be null or empty.");
            }
            this.resourceTypes = resourceTypes;
            return this;
        }

        public Builder withResourceType(@NotNull String resourceType) {
            if (StringUtils.isEmpty(resourceType)) {
                throw new IllegalArgumentException("The script's resourceType cannot be null or empty.");
            }
            resourceTypes.add(resourceType);
            return this;
        }

        public Builder withScriptEngine(String scriptEngine) {
            this.scriptEngine = scriptEngine;
            return this;
        }

        public Builder withScriptExtension(String scriptExtension) {
            this.scriptExtension = scriptExtension;
            return this;
        }

        public Builder withExtendsResourceType(String extendsResourceType) {
            this.extendsResourceType = extendsResourceType;
            return this;
        }

        public Builder withVersion(String version) {
            this.version = version;
            return this;
        }

        public Builder withRequestExtension(String requestExtension) {
            this.requestExtension = requestExtension;
            return this;
        }

        public Builder withRequestMethod(String requestMethod) {
            this.requestMethod = requestMethod;
            return this;
        }

        public Builder withSelectors(@NotNull Set<String> selectors) {
            this.selectors = selectors;
            return this;
        }

        public Builder fromCapability(@NotNull ProvidedResourceTypeCapability capability) {
            if (capability.getResourceTypes().isEmpty()) {
                throw new IllegalArgumentException("The script's resourceTypes cannot be null or empty.");
            }
            this.resourceTypes = capability.getResourceTypes();
            this.scriptEngine = capability.getScriptEngine();
            this.scriptExtension = capability.getScriptExtension();
            this.extendsResourceType = capability.getExtendsResourceType();
            this.version = capability.getVersion();
            this.requestExtension = capability.getRequestExtension();
            this.requestMethod = capability.getRequestMethod();
            this.selectors = capability.getSelectors();
            return this;
        }


        public ProvidedResourceTypeCapability build() {
            return new ProvidedResourceTypeCapability(resourceTypes, scriptEngine, scriptExtension, extendsResourceType, version,
                    requestExtension, requestMethod, selectors);
        }
    }
}
