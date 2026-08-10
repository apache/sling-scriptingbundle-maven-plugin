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

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

public class RequiredResourceTypeCapability {

    private final String resourceType;
    private final VersionRange versionRange;
    private final boolean isOptional;

    private RequiredResourceTypeCapability(
            @NotNull String resourceType, @Nullable VersionRange versionRange, boolean isOptional) {
        this.resourceType = resourceType;
        this.versionRange = versionRange;
        this.isOptional = isOptional;
    }

    public static Builder builder() {
        return new Builder();
    }

    @NotNull
    public String getResourceType() {
        return resourceType;
    }

    @Nullable
    public VersionRange getVersionRange() {
        return versionRange;
    }

    public boolean isOptional() {
        return isOptional;
    }

    public boolean isSatisfied(@NotNull ProvidedResourceTypeCapability providedResourceTypeCapability) {
        List<String> providedSelectors = providedResourceTypeCapability.getSelectors();
        if (providedSelectors.isEmpty()) {
            for (String providedResourceType : providedResourceTypeCapability.getResourceTypes()) {
                if (resourceType.equals(providedResourceType)) {
                    if (versionRange == null) {
                        return true;
                    } else {
                        Version providedVersion = providedResourceTypeCapability.getVersion();
                        if (providedVersion != null) {
                            return versionRange.includes(providedVersion);
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format(
                "%s{resourceType=%s, versionRange=%s, isOptional=%s}",
                this.getClass().getSimpleName(), resourceType, versionRange, isOptional);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceType, versionRange, isOptional);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof RequiredResourceTypeCapability) {
            RequiredResourceTypeCapability other = (RequiredResourceTypeCapability) obj;
            return Objects.equals(resourceType, other.resourceType)
                    && Objects.equals(versionRange, other.versionRange)
                    && Objects.equals(isOptional, other.isOptional);
        }
        return false;
    }

    public static class Builder {
        private String resourceType;
        private VersionRange versionRange;
        private boolean isOptional;

        public RequiredResourceTypeCapability build() {
            return new RequiredResourceTypeCapability(resourceType, versionRange, isOptional);
        }

        public Builder withResourceType(String resourceType) {
            if (StringUtils.isEmpty(resourceType)) {
                throw new NullPointerException("The required resourceType cannot be null or empty.");
            }
            this.resourceType = resourceType;
            return this;
        }

        public Builder withVersionRange(VersionRange versionRange) {
            this.versionRange = versionRange;
            return this;
        }

        public Builder withIsOptional() {
            this.isOptional = true;
            return this;
        }
    }
}
