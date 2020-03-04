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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class ProvidedCapability {
    private final String resourceType;
    private final String extendsResourceType;
    private final String version;
    private final String requestExtension;
    private final String requestMethod;
    private final List<String> selectors;

    private ProvidedCapability(@NotNull String resourceType, @Nullable String extendsResourceType,
                               @Nullable String version, @Nullable String requestExtension, @Nullable String requestMethod,
                               @NotNull List<String> selectors) {
        this.resourceType = resourceType;
        this.extendsResourceType = extendsResourceType;
        this.version = version;
        this.requestExtension = requestExtension;
        this.requestMethod = requestMethod;
        this.selectors = selectors;
    }

    static Builder builder() {
        return new Builder();
    }

    @NotNull
    public String getResourceType() {
        return resourceType;
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
    public List<String> getSelectors() {
        return selectors;
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceType, version, requestExtension, requestMethod, selectors);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ProvidedCapability) {
            ProvidedCapability other = (ProvidedCapability) obj;
            return Objects.equals(resourceType, other.resourceType) && Objects.equals(version, other.version) &&
                    Objects.equals(requestExtension, other.requestExtension) && Objects.equals(requestMethod, other.requestMethod) &&
                    Objects.equals(selectors, other.selectors);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s{resourceType=%s, version=%s, selectors=%s, requestExtension=%s, requestMethod=%s}",
                this.getClass().getSimpleName(), resourceType, version, selectors, requestExtension, requestMethod);
    }

    static class Builder {
        private String resourceType;
        private String extendsResourceType;
        private String version;
        private String requestExtension;
        private String requestMethod;
        private List<String> selectors = Collections.emptyList();

        Builder withResourceType(String resourceType) {
            if (StringUtils.isEmpty(resourceType)) {
                throw new NullPointerException("The script's resourceType cannot be null or empty.");
            }
            this.resourceType = resourceType;
            return this;
        }

        Builder withExtendsResourceType(String extendsResourceType) {
            this.extendsResourceType = extendsResourceType;
            return this;
        }

        Builder withVersion(String version) {
            this.version = version;
            return this;
        }

        Builder withRequestExtension(String requestExtension) {
            this.requestExtension = requestExtension;
            return this;
        }

        Builder withRequestMethod(String requestMethod) {
            this.requestMethod = requestMethod;
            return this;
        }

        Builder withSelectors(List<String> selectors) {
            if (selectors == null) {
                throw new NullPointerException("The resourceType selectors list cannot be null.");
            }
            this.selectors = selectors;
            return this;
        }

        ProvidedCapability build() {
            return new ProvidedCapability(resourceType, extendsResourceType, version, requestExtension, requestMethod, selectors);
        }
    }
}
