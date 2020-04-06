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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.sling.scripting.bundle.tracker.ResourceType;
import org.apache.sling.scriptingbundle.maven.plugin.capability.ProvidedResourceTypeCapability;
import org.apache.sling.scriptingbundle.maven.plugin.capability.RequiredResourceTypeCapability;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.VersionRange;

public class FileProcessor {

    private final Log log;
    private final Set<String> searchPaths;
    private final Map<String, String> scriptEngineMappings;

    FileProcessor(Log log, Set<String> searchPaths, Map<String, String> scriptEngineMappings) {
        this.log = log;
        this.searchPaths = searchPaths;
        this.scriptEngineMappings = scriptEngineMappings;
    }

    void processExtendsFile(@NotNull ResourceType resourceType, @NotNull Path extendsFile,
                            @NotNull Set<ProvidedResourceTypeCapability> providedCapabilities,
                            @NotNull Set<RequiredResourceTypeCapability> requiredCapabilities) {
        try {
            List<String> extendResources = Files.readAllLines(extendsFile, StandardCharsets.UTF_8);
            if (extendResources.size() == 1) {
                String extend = extendResources.get(0);
                if (StringUtils.isNotEmpty(extend)) {
                    String[] extendParts = extend.split(";");
                    String extendedResourceType = extendParts[0];
                    String extendedResourceTypeVersion = extendParts.length > 1 ? extendParts[1] : null;
                    Set<String> searchPathResourceTypes = processSearchPathResourceTypes(resourceType);
                    Optional<ProvidedResourceTypeCapability> rootCapability = providedCapabilities.stream().filter(capability ->
                        capability.getResourceTypes().equals(searchPathResourceTypes) && capability.getSelectors().isEmpty() &&
                                StringUtils.isEmpty(capability.getRequestMethod()) && StringUtils.isEmpty(capability.getRequestExtension())
                    ).findFirst();
                    rootCapability.ifPresent(capability -> {
                        providedCapabilities.remove(capability);
                        ProvidedResourceTypeCapability replacement =
                                ProvidedResourceTypeCapability.builder().fromCapability(capability)
                                        .withExtendsResourceType(extendedResourceType).build();
                        providedCapabilities.add(replacement);

                    });
                    if (!rootCapability.isPresent()) {
                        providedCapabilities.add(
                                ProvidedResourceTypeCapability.builder()
                                        .withResourceTypes(processSearchPathResourceTypes(resourceType))
                                        .withVersion(resourceType.getVersion())
                                        .withExtendsResourceType(extendedResourceType)
                                        .build());
                    }
                    RequiredResourceTypeCapability.Builder requiredBuilder =
                            RequiredResourceTypeCapability.builder().withResourceType(extendedResourceType);
                    extractVersionRange(extendsFile, requiredBuilder, extendedResourceTypeVersion);
                    requiredCapabilities.add(requiredBuilder.build());
                }
            }
        } catch (IOException e) {
            log.error(String.format("Unable to read file %s.", extendsFile.toString()), e);
        }
    }

    void processRequiresFile(@NotNull Path requiresFile,
                                    @NotNull Set<RequiredResourceTypeCapability> requiredCapabilities) {
        try {
            List<String> requiredResourceTypes = Files.readAllLines(requiresFile, StandardCharsets.UTF_8);
            for (String requiredResourceType : requiredResourceTypes) {
                if (StringUtils.isNotEmpty(requiredResourceType)) {
                    String[] requireParts = requiredResourceType.split(";");
                    String resourceType = requireParts[0];
                    String version = requireParts.length > 1 ? requireParts[1] : null;
                    RequiredResourceTypeCapability.Builder requiredBuilder =
                            RequiredResourceTypeCapability.builder().withResourceType(resourceType);
                    extractVersionRange(requiresFile, requiredBuilder, version);
                    requiredCapabilities.add(requiredBuilder.build());
                }
            }
        } catch (IOException e) {
            log.error(String.format("Unable to read file %s.", requiresFile.toString()), e);
        }
    }

    void processScriptFile(@NotNull Path resourceTypeDirectory, @NotNull Path scriptPath,
                                   @NotNull ResourceType resourceType, @NotNull Set<ProvidedResourceTypeCapability> providedCapabilities) {
        Path scriptFile = scriptPath.getFileName();
        if (scriptFile != null) {
            Path relativeResourceTypeFolder = resourceTypeDirectory.relativize(scriptPath);
            int pathSegments = relativeResourceTypeFolder.getNameCount();
            LinkedHashSet<String> selectors = new LinkedHashSet<>();
            if (pathSegments > 1) {
                for (int i = 0; i < pathSegments - 1; i++) {
                    selectors.add(relativeResourceTypeFolder.getName(i).toString());
                }
            }
            String scriptFileName = scriptFile.toString();
            Script script = Script.parseScript(scriptFileName);
            if (script != null) {
                String scriptEngine = scriptEngineMappings.get(script.getScriptExtension());
                if (scriptEngine != null) {
                    String scriptName = script.getName();
                    Set<String> searchPathProcessesResourceTypes = processSearchPathResourceTypes(resourceType);
                    if (scriptName != null && !resourceType.getResourceLabel().equals(scriptName)) {
                        selectors.add(script.getName());
                    }
                    Optional<ProvidedResourceTypeCapability> extendsCapability = Optional.empty();
                    if (selectors.isEmpty() && StringUtils.isEmpty(script.getRequestExtension()) && StringUtils.isEmpty(script.getRequestMethod())) {
                         extendsCapability =
                                 providedCapabilities.stream().filter(capability -> StringUtils.isNotEmpty(capability.getExtendsResourceType()) &&
                                 capability.getResourceTypes().equals(searchPathProcessesResourceTypes) &&
                                 capability.getSelectors().isEmpty() && StringUtils.isEmpty(capability.getRequestExtension()) &&
                                 StringUtils.isEmpty(capability.getRequestMethod())).findAny();
                    }
                    ProvidedResourceTypeCapability.Builder builder = ProvidedResourceTypeCapability.builder()
                            .withResourceTypes(searchPathProcessesResourceTypes)
                            .withVersion(resourceType.getVersion())
                            .withSelectors(selectors)
                            .withRequestExtension(script.getRequestExtension())
                            .withRequestMethod(script.getRequestMethod())
                            .withScriptEngine(scriptEngine)
                            .withScriptExtension(script.getScriptExtension());
                    extendsCapability.ifPresent(capability -> {
                                builder.withExtendsResourceType(capability.getExtendsResourceType());
                                providedCapabilities.remove(capability);
                            }
                    );
                    providedCapabilities.add(builder.build());
                } else {
                    log.warn(String.format("Cannot find a script engine mapping for script %s.", scriptPath.toString()));
                }
            } else {
                log.warn(String.format("File %s does not denote a script.", scriptPath.toString()));
            }
        } else {
            log.warn(String.format("Skipping path %s since it has 0 elements.", scriptPath.toString()));
        }
    }

    private Set<String> processSearchPathResourceTypes(@NotNull ResourceType resourceType) {
        Set<String> resourceTypes = new HashSet<>();
        for (String searchPath : searchPaths) {
            if (!searchPath.endsWith("/")) {
                searchPath = searchPath + "/";
            }
            String absoluteType = "/" + resourceType.getType();
            if (absoluteType.startsWith(searchPath)) {
                resourceTypes.add(absoluteType);
                resourceTypes.add(absoluteType.substring(searchPath.length()));
            }
        }
        if (resourceTypes.isEmpty()) {
            resourceTypes.add(resourceType.getType());
        }
        return resourceTypes;
    }

    private void extractVersionRange(@NotNull Path requiresFile, @NotNull RequiredResourceTypeCapability.Builder requiredBuilder,
                                            String version) {
        try {
            if (version != null) {
                requiredBuilder.withVersionRange(VersionRange.valueOf(version.substring(version.indexOf('=') + 1)));
            }
        } catch (IllegalArgumentException ignored) {
            log.warn(String.format("Invalid version range format %s in file %s.", version, requiresFile.toString()));
        }
    }
}
