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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.VersionRange;

public class RequiredCapability {

    private final String resourceType;
    private final VersionRange versionRange;

    private RequiredCapability(@NotNull String resourceType, @Nullable VersionRange versionRange) {
        this.resourceType = resourceType;
        this.versionRange = versionRange;
    }

    static Builder builder() {
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

    @Override
    public String toString() {
        return String.format("%s{resourceType=%s, versionRange=%s}", this.getClass().getSimpleName(),
                resourceType, versionRange);
    }

    static class Builder {
        private String resourceType;
        private VersionRange versionRange;

        RequiredCapability build() {
            return new RequiredCapability(resourceType, versionRange);
        }

        Builder withResourceType(String resourceType) {
            if (StringUtils.isEmpty(resourceType)) {
                throw new NullPointerException("The required resourceType cannot be null or empty.");
            }
            this.resourceType = resourceType;
            return this;
        }

        Builder withVersionRange(VersionRange versionRange) {
            this.versionRange = versionRange;
            return this;
        }
    }

}
