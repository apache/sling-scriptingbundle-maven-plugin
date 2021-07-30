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
package org.apache.sling.scriptingbundle.plugin.processor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.type.ResourceType;
import org.apache.sling.scriptingbundle.plugin.capability.ProvidedResourceTypeCapability;
import org.apache.sling.scriptingbundle.plugin.capability.RequiredResourceTypeCapability;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.VersionRange;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;

public class FileProcessor {

    private final Logger log;
    private final Set<String> searchPaths;
    private final Map<String, String> scriptEngineMappings;

    public FileProcessor(Logger log, Set<String> searchPaths, Map<String, String> scriptEngineMappings) {
        this.log = log;
        this.searchPaths = searchPaths;
        this.scriptEngineMappings = scriptEngineMappings;
    }

    public void processExtendsFile(@NotNull ResourceType resourceType, @NotNull Path extendsFile,
                            @NotNull Set<ProvidedResourceTypeCapability> providedCapabilities,
                            @NotNull Set<RequiredResourceTypeCapability> requiredCapabilities) {
        try {
            List<String> extendResources = Files.readAllLines(extendsFile, StandardCharsets.UTF_8);
            if (extendResources.size() == 1) {
                String extend = extendResources.get(0);
                Parameters parameters = OSGiHeader.parseHeader(extend);
                if (parameters.size() < 1 || parameters.size() > 1) {
                    log.error(String.format("The file '%s' must contain one clause only (not multiple ones separated by ','", extendsFile));
                }
                Entry<String, Attrs> extendsParameter = parameters.entrySet().iterator().next();
                String extendedResourceType = FilenameUtils.normalize(extendsParameter.getKey(), true);
                String disableRequireString = extendsParameter.getValue().get("disableRequiresCapability");
                boolean generateRequireCapability = !Boolean.parseBoolean(disableRequireString);
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
                if (generateRequireCapability) {
                    RequiredResourceTypeCapability.Builder requiredBuilder =
                            RequiredResourceTypeCapability.builder().withResourceType(extendedResourceType);
                    extractVersionRange(extendsFile, requiredBuilder, extendsParameter.getValue().getVersion());
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
                Parameters parameters = OSGiHeader.parseHeader(requiredResourceType);
                if (parameters.size() < 1 || parameters.size() > 1) {
                    log.error(String.format("Each line in file '%s' must contain one clause only (not multiple ones separated by ',', skipping line", requiresFile));
                    continue;
                }
                Entry<String, Attrs> requiresParameter = parameters.entrySet().iterator().next();
                String resourceType = FilenameUtils.normalize(requiresParameter.getKey(), true);
                RequiredResourceTypeCapability.Builder requiredBuilder =
                        RequiredResourceTypeCapability.builder().withResourceType(resourceType);
                extractVersionRange(requiresFile, requiredBuilder, requiresParameter.getValue().getVersion());
                requiredCapabilities.add(requiredBuilder.build());
            }
        } catch (IOException e) {
            log.error(String.format("Unable to read file %s.", requiresFile.toString()), e);
        }
    }

    public void processScriptFile(@NotNull Path resourceTypeDirectory, @NotNull Path scriptPath,
                                   @NotNull ResourceType resourceType, @NotNull Set<ProvidedResourceTypeCapability> providedCapabilities) {
        String filePath = scriptPath.toString();
        String extension = FilenameUtils.getExtension(filePath);
        if (StringUtils.isNotEmpty(extension) && scriptEngineMappings.containsKey(extension)) {
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
                        if (selectors.isEmpty() && StringUtils.isEmpty(script.getRequestExtension()) &&
                                StringUtils.isEmpty(script.getRequestMethod())) {
                            extendsCapability =
                                    providedCapabilities.stream()
                                            .filter(capability -> StringUtils.isNotEmpty(capability.getExtendsResourceType()) &&
                                                    capability.getResourceTypes().equals(searchPathProcessesResourceTypes) &&
                                                    capability.getSelectors().isEmpty() &&
                                                    StringUtils.isEmpty(capability.getRequestExtension()) &&
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
                        log.warn(String.format("Cannot find a script engine mapping for script %s.", scriptPath));
                    }
                } else {
                    log.warn(String.format("File %s does not denote a script.", scriptPath));
                }
            } else {
                log.warn(String.format("Skipping path %s since it has 0 elements.", scriptPath));
            }
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
            log.warn(String.format("Invalid version range format %s in file %s.", version, requiresFile));
        }
    }
}
