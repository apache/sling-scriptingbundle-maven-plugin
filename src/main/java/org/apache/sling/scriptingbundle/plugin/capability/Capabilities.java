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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.scriptingbundle.plugin.processor.Constants;
import org.apache.sling.scriptingbundle.plugin.processor.FileProcessor;
import org.apache.sling.scriptingbundle.plugin.processor.Logger;
import org.apache.sling.scriptingbundle.plugin.processor.PathOnlyScriptAnalyser;
import org.apache.sling.scriptingbundle.plugin.processor.ResourceTypeFolderAnalyser;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.VersionRange;

public class Capabilities {

    private final Set<ProvidedResourceTypeCapability> providedResourceTypeCapabilities;
    private final Set<ProvidedScriptCapability> providedScriptCapabilities;
    private final Set<RequiredResourceTypeCapability> requiredResourceTypeCapabilities;
    private final Set<RequiredResourceTypeCapability> unresolvedRequiredResourceTypeCapabilities;
    public static final Capabilities EMPTY = new Capabilities(Collections.emptySet(), Collections.emptySet(), Collections.emptySet());

    public Capabilities(
            @NotNull Set<ProvidedResourceTypeCapability> providedResourceTypeCapabilities,
            @NotNull Set<ProvidedScriptCapability> providedScriptCapabilities,
            @NotNull Set<RequiredResourceTypeCapability> requiredResourceTypeCapabilities) {
        this.providedResourceTypeCapabilities = providedResourceTypeCapabilities;
        this.providedScriptCapabilities = providedScriptCapabilities;
        this.requiredResourceTypeCapabilities = requiredResourceTypeCapabilities;
        unresolvedRequiredResourceTypeCapabilities = new HashSet<>(requiredResourceTypeCapabilities);
        providedResourceTypeCapabilities.forEach(providedResourceTypeCapability -> unresolvedRequiredResourceTypeCapabilities
                .removeIf(requiredResourceTypeCapability -> requiredResourceTypeCapability.isSatisfied(providedResourceTypeCapability)));
    }

    public @NotNull Set<ProvidedResourceTypeCapability> getProvidedResourceTypeCapabilities() {
        return Collections.unmodifiableSet(providedResourceTypeCapabilities);
    }

    public @NotNull Set<ProvidedScriptCapability> getProvidedScriptCapabilities() {
        return Collections.unmodifiableSet(providedScriptCapabilities);
    }

    public @NotNull Set<RequiredResourceTypeCapability> getRequiredResourceTypeCapabilities() {
        return Collections.unmodifiableSet(requiredResourceTypeCapabilities);
    }

    public @NotNull Set<RequiredResourceTypeCapability> getUnresolvedRequiredResourceTypeCapabilities() {
        return Collections.unmodifiableSet(unresolvedRequiredResourceTypeCapabilities);
    }

    public @NotNull String getProvidedCapabilitiesString() {
        StringBuilder builder = new StringBuilder();
        int pcNum = getProvidedResourceTypeCapabilities().size();
        int psNum = getProvidedScriptCapabilities().size();
        int pcIndex = 0;
        int psIndex = 0;
        for (ProvidedResourceTypeCapability capability : getProvidedResourceTypeCapabilities()) {
            builder.append(Constants.CAPABILITY_NS).append(";");
            processListAttribute(Constants.CAPABILITY_RESOURCE_TYPE_AT, builder,capability.getResourceTypes());
            Optional.ofNullable(capability.getScriptEngine()).ifPresent(scriptEngine ->
                    builder.append(";")
                            .append(Constants.CAPABILITY_SCRIPT_ENGINE_AT).append("=").append("\"").append(scriptEngine).append("\"")
            );
            Optional.ofNullable(capability.getScriptExtension()).ifPresent(scriptExtension ->
                    builder.append(";")
                            .append(Constants.CAPABILITY_SCRIPT_EXTENSION_AT).append("=").append("\"").append(scriptExtension).append("\"")
            );
            Optional.ofNullable(capability.getVersion()).ifPresent(version ->
                    builder.append(";")
                            .append(Constants.CAPABILITY_VERSION_AT).append("=").append("\"").append(version).append("\"")
            );
            Optional.ofNullable(capability.getExtendsResourceType()).ifPresent(extendedResourceType ->
                    builder.append(";")
                            .append(Constants.CAPABILITY_EXTENDS_AT).append("=").append("\"").append(extendedResourceType).append("\"")
            );
            Optional.ofNullable(capability.getRequestMethod()).ifPresent(method ->
                    builder.append(";")
                            .append(Constants.CAPABILITY_METHODS_AT).append("=").append("\"").append(method).append("\"")
            );
            Optional.ofNullable(capability.getRequestExtension()).ifPresent(requestExtension ->
                    builder.append(";")
                            .append(Constants.CAPABILITY_EXTENSIONS_AT).append("=").append("\"").append(requestExtension).append("\"")
            );
            if (!capability.getSelectors().isEmpty()) {
                builder.append(";");
                processListAttribute(Constants.CAPABILITY_SELECTORS_AT, builder, capability.getSelectors());
            }
            if (pcIndex < pcNum - 1) {
                builder.append(",");
            }
            pcIndex++;
        }
        if (builder.length() > 0 && psNum > 0) {
            builder.append(",");
        }
        for (ProvidedScriptCapability scriptCapability : getProvidedScriptCapabilities()) {
            builder.append(Constants.CAPABILITY_NS).append(";").append(Constants.CAPABILITY_PATH_AT).append("=\"").append(scriptCapability.getPath()).append(
                    "\";").append(Constants.CAPABILITY_SCRIPT_ENGINE_AT).append("=").append(scriptCapability.getScriptEngine()).append(";")
                    .append(Constants.CAPABILITY_SCRIPT_EXTENSION_AT).append("=").append(scriptCapability.getScriptExtension());
            if (psIndex < psNum - 1) {
                builder.append(",");
            }
            psIndex++;
        }
        return builder.toString();
    }

    public @NotNull String getRequiredCapabilitiesString() {
        StringBuilder builder = new StringBuilder();
        int pcNum = getRequiredResourceTypeCapabilities().size();
        int pcIndex = 0;
        for (RequiredResourceTypeCapability capability : getRequiredResourceTypeCapabilities()) {
            builder.append(Constants.CAPABILITY_NS).append(";filter:=\"").append("(&(!(" + ServletResolverConstants.SLING_SERVLET_SELECTORS + "=*))");
            VersionRange versionRange = capability.getVersionRange();
            if (versionRange != null) {
                builder.append("(&").append(versionRange.toFilterString("version")).append("(").append(ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES).append(
                        "=").append(capability.getResourceType()).append(")))\"");
            } else {
                builder.append("(").append(ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES).append("=").append(capability.getResourceType()).append("))\"");
            }
            if (pcIndex < pcNum - 1) {
                builder.append(",");
            }
            pcIndex++;
        }
        return builder.toString();
    }

    public static @NotNull Capabilities fromFileSystemTree(@NotNull Path root, @NotNull Stream<Path> files, @NotNull Logger logger,
                                                           @NotNull Set<String> searchPaths, @NotNull Map<String, String> scriptEngineMappings) {
        Set<ProvidedResourceTypeCapability> providedResourceTypeCapabilities = new LinkedHashSet<>();
        Set<ProvidedScriptCapability> providedScriptCapabilities = new LinkedHashSet<>();
        Set<RequiredResourceTypeCapability> requiredResourceTypeCapabilities = new LinkedHashSet<>();
        FileProcessor fileProcessor = new FileProcessor(logger, searchPaths, scriptEngineMappings);
        ResourceTypeFolderAnalyser resourceTypeFolderAnalyser = new ResourceTypeFolderAnalyser(logger, root, fileProcessor);
        PathOnlyScriptAnalyser
                pathOnlyScriptAnalyser = new PathOnlyScriptAnalyser(logger, root, scriptEngineMappings, fileProcessor);
        files.forEach(path -> {
            if (Files.isDirectory(path)) {
                Capabilities resourceTypeCapabilities = resourceTypeFolderAnalyser.getCapabilities(path);
                providedResourceTypeCapabilities.addAll(resourceTypeCapabilities.getProvidedResourceTypeCapabilities());
                requiredResourceTypeCapabilities.addAll(resourceTypeCapabilities.getRequiredResourceTypeCapabilities());
            } else {
                Capabilities pathCapabilities = pathOnlyScriptAnalyser.getProvidedScriptCapability(path);
                providedScriptCapabilities.addAll(pathCapabilities.getProvidedScriptCapabilities());
                requiredResourceTypeCapabilities.addAll(pathCapabilities.getRequiredResourceTypeCapabilities());
            }
        });
        return new Capabilities(providedResourceTypeCapabilities, providedScriptCapabilities, requiredResourceTypeCapabilities);
    }

    private void processListAttribute(@NotNull String capabilityAttribute, @NotNull StringBuilder builder, @NotNull Set<String> values) {
        builder.append(capabilityAttribute).append("=").append("\"");
        int valuesSize = values.size();
        int valueIndex = 0;
        for (String item : values) {
            builder.append(item);
            if (valueIndex < valuesSize - 1) {
                builder.append(",");
            }
            valueIndex++;
        }
        builder.append("\"");
    }
}
