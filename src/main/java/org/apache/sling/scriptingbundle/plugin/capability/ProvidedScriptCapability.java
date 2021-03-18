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

import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class ProvidedScriptCapability {

    private final String path;
    private final String scriptExtension;
    private final String scriptEngine;

    private ProvidedScriptCapability(@NotNull String path, @NotNull String scriptExtension, @NotNull String scriptEngine) {
        this.path = path;
        this.scriptExtension = scriptExtension;
        this.scriptEngine = scriptEngine;
    }

    public String getPath() {
        return path;
    }

    public String getScriptExtension() {
        return scriptExtension;
    }

    public String getScriptEngine() {
        return scriptEngine;
    }

    public static Builder builder(Map<String, String> scriptEngineMappings) {
        return new Builder(scriptEngineMappings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, scriptExtension, scriptEngine);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ProvidedScriptCapability) {
            ProvidedScriptCapability other = (ProvidedScriptCapability) obj;
            return Objects.equals(path, other.path) && Objects.equals(scriptExtension, other.scriptExtension) &&
                    Objects.equals(scriptEngine, other.scriptEngine);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s { path=%s; scriptExtension=%s; scriptEngine=%s }", ProvidedScriptCapability.class.getSimpleName(), path,
                scriptExtension, scriptEngine);
    }

    public static class Builder {
        private final Map<String, String> scriptEngineMappings;
        private String path;

        public Builder(Map<String, String> scriptEngineMappings) {
            this.scriptEngineMappings = scriptEngineMappings;
        }

        public ProvidedScriptCapability build() {
            int lastDotIndex = path.lastIndexOf('.');
            if (lastDotIndex == -1 || lastDotIndex == path.length() - 1) {
                throw new IllegalStateException(String.format("Path %s does not seem to have an extension.", path));
            }
            String extension = path.substring(lastDotIndex + 1);
            String scriptEngine = scriptEngineMappings.get(extension);
            if (StringUtils.isEmpty(scriptEngine)) {
                throw new IllegalStateException(String.format("Path %s does not seem to have an extension mapped to a script engine.",
                        path));
            }
            return new ProvidedScriptCapability(path, extension, scriptEngine);
        }

        public ProvidedScriptCapability.Builder withPath(String path) {
            if (StringUtils.isEmpty(path)) {
                throw new NullPointerException("The path cannot be null or empty.");
            }
            this.path = path;
            return this;
        }
    }

}
