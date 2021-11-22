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
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.util.PlatformNameFormat;
import org.apache.sling.api.resource.type.ResourceType;
import org.apache.sling.scriptingbundle.plugin.capability.ProvidedResourceTypeCapability;
import org.apache.sling.scriptingbundle.plugin.capability.RequiredResourceTypeCapability;
import org.apache.sling.scriptingbundle.plugin.processor.filevault.VaultContentXmlReader;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.VersionRange;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;

public class FileProcessor {

    private final Logger log;
    private final Set<String> searchPaths;
    private final Map<String, String> scriptEngineMappings;

    private static final Collection<String> EXTENDS_ALLOWED_ATTRIBUTE_NAMES = Arrays.asList(aQute.bnd.osgi.Constants.RESOLUTION_DIRECTIVE, aQute.bnd.osgi.Constants.VERSION_ATTRIBUTE);
    private static final Collection<String> REQUIRES_ALLOWED_ATTRIBUTE_NAMES = Arrays.asList(aQute.bnd.osgi.Constants.RESOLUTION_DIRECTIVE, aQute.bnd.osgi.Constants.VERSION_ATTRIBUTE);

    
    public FileProcessor(Logger log, Set<String> searchPaths, Map<String, String> scriptEngineMappings) {
        this.log = log;
        this.searchPaths = searchPaths;
        this.scriptEngineMappings = scriptEngineMappings;
    }

    public void processExtendsFile(@NotNull ResourceType resourceType, @NotNull Path file,
                            @NotNull Set<ProvidedResourceTypeCapability> providedCapabilities,
                            @NotNull Set<RequiredResourceTypeCapability> requiredCapabilities) throws IllegalArgumentException {
        try {
            List<String> extendedResources = Files.readAllLines(file, StandardCharsets.UTF_8);
            if (extendedResources.size() == 1) {
                processExtendedResourceType(resourceType, file, providedCapabilities, requiredCapabilities, extendedResources.get(0));
            } else {
                throw new IllegalArgumentException(String.format("The file '%s' must contain one line only (not multiple ones)", file));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(String.format("Unable to read file %s.", file.toString()), e);
        }
    }

    private void processExtendedResourceType(@NotNull ResourceType resourceType, @NotNull Path extendsFile,
                           @NotNull Set<ProvidedResourceTypeCapability> providedCapabilities,
                           @NotNull Set<RequiredResourceTypeCapability> requiredCapabilities, @NotNull String extendedResource) {
        Parameters parameters = OSGiHeader.parseHeader(extendedResource);
        if (parameters.size() != 1) {
            throw new IllegalArgumentException(String.format("The file '%s' must contain one clause only (not multiple ones separated by ',')",
                    extendsFile));
        }
        Entry<String, Attrs> extendsParameter = parameters.entrySet().iterator().next();
        for (String attributeName : extendsParameter.getValue().keySet()) {
            if (!EXTENDS_ALLOWED_ATTRIBUTE_NAMES.contains(attributeName)) {
                throw new IllegalArgumentException(String.format("Found unsupported attribute/directive '%s' in file '%s'. Only the following attributes or directives may be used in the extends file: %s", attributeName,
                        extendsFile, String.join(",", EXTENDS_ALLOWED_ATTRIBUTE_NAMES)));
            }
        }
        String extendedResourceType = FilenameUtils.normalize(extendsParameter.getKey(), true);
        boolean isOptional = aQute.bnd.osgi.Constants.OPTIONAL.equals(extendsParameter.getValue().get(aQute.bnd.osgi.Constants.RESOLUTION_DIRECTIVE));
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
        if (isOptional) {
            requiredBuilder.withIsOptional();
        }
        extractVersionRange(extendsFile, requiredBuilder, extendsParameter.getValue().getVersion());
        requiredCapabilities.add(requiredBuilder.build());
    }

    void processRequiresFile(@NotNull Path requiresFile,
                                    @NotNull Set<RequiredResourceTypeCapability> requiredCapabilities) {
        try {
            List<String> requiredResourceTypes = Files.readAllLines(requiresFile, StandardCharsets.UTF_8);
            processRequiredResourceTypes(requiresFile, requiredCapabilities, requiredResourceTypes);
        } catch (IOException e) {
            throw new UncheckedIOException(String.format("Unable to read file %s.", requiresFile), e);
        }
    }

    private void processRequiredResourceTypes(@NotNull Path requiresFile, @NotNull Set<RequiredResourceTypeCapability> requiredCapabilities,
                           List<String> requiredResourceTypes) {
        for (String requiredResourceType : requiredResourceTypes) {
            Parameters parameters = OSGiHeader.parseHeader(requiredResourceType);
            if (parameters.size() != 1) {
                throw new IllegalArgumentException(String.format("Each line in file '%s' must contain one clause only (not multiple ones separated by ',')",
                        requiresFile));
            }
            Entry<String, Attrs> requiresParameter = parameters.entrySet().iterator().next();
            for (String attributeName : requiresParameter.getValue().keySet()) {
                if (!REQUIRES_ALLOWED_ATTRIBUTE_NAMES.contains(attributeName)) {
                    throw new IllegalArgumentException(String.format("Found unsupported attribute/directive '%s' in file '%s'. Only the following attributes or directives may be used in the requires file: %s", attributeName,
                            requiresFile, String.join(",", REQUIRES_ALLOWED_ATTRIBUTE_NAMES)));
                }
            }
            String resourceType = FilenameUtils.normalize(requiresParameter.getKey(), true);
            boolean isOptional = aQute.bnd.osgi.Constants.OPTIONAL.equals(requiresParameter.getValue().get(aQute.bnd.osgi.Constants.RESOLUTION_DIRECTIVE));
            RequiredResourceTypeCapability.Builder requiredBuilder =
                    RequiredResourceTypeCapability.builder().withResourceType(resourceType);
            if (isOptional) {
                requiredBuilder.withIsOptional();
            }
            extractVersionRange(requiresFile, requiredBuilder, requiresParameter.getValue().getVersion());
            requiredCapabilities.add(requiredBuilder.build());
        }
    }

    public void processScriptFile(@NotNull Path resourceTypeDirectory, @NotNull Path scriptPath,
                                   @NotNull ResourceType resourceType, @NotNull Set<ProvidedResourceTypeCapability> providedCapabilities,
                                  boolean inContentPackage) {
        String filePath = scriptPath.toString();
        String extension = FilenameUtils.getExtension(filePath);
        if (StringUtils.isNotEmpty(extension)) {
            Path scriptFile = scriptPath.getFileName();
            if (scriptFile != null) {
                Path relativeResourceTypeFolder = resourceTypeDirectory.relativize(scriptPath);
                int pathSegments = relativeResourceTypeFolder.getNameCount();
                List<String> selectors = new ArrayList<>();
                if (pathSegments > 1) {
                    for (int i = 0; i < pathSegments - 1; i++) {
                        selectors.add(inContentPackage ?
                                PlatformNameFormat.getRepositoryName(relativeResourceTypeFolder.getName(i).toString()) :
                                relativeResourceTypeFolder.getName(i).toString()
                        );
                    }
                }
                String scriptFileName = scriptFile.toString();
                Script script = Script.parseScript(inContentPackage ? PlatformNameFormat.getRepositoryPath(scriptFileName) : scriptFileName);
                if (script != null) {
                    String scriptEngine = scriptEngineMappings.get(script.getScriptExtension());
                    if (scriptEngine != null) {
                        String scriptName = script.getName();
                        Set<String> searchPathProcessesResourceTypes = processSearchPathResourceTypes(resourceType);
                        if (scriptName != null) {
                            if (!resourceType.getResourceLabel().equals(scriptName)
                            || script.getRequestMethod() != null) {
                                selectors.add(script.getName());
                            }
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
                        log.debug(String.format("Cannot find a script engine mapping for script %s.", scriptPath));
                    }
                } else {
                    log.debug(String.format("Skipping file %s not denoting a script as it does not follow the filename patterns outlined " +
                            "at https://sling.apache.org/documentation/the-sling-engine/url-to-script-resolution.html#script-naming-conventions", scriptPath));
                }
            } else {
                throw new IllegalArgumentException(String.format("Invalid path given: '%s'.", scriptPath));
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

    public void processVaultFile(@NotNull Path entry, @NotNull ResourceType resourceType,
                                 @NotNull Set<ProvidedResourceTypeCapability> providedCapabilities,
                                 @NotNull Set<RequiredResourceTypeCapability> requiredCapabilities) {
        try {
            VaultContentXmlReader reader = new VaultContentXmlReader(entry);
            Optional<String> slingResourceSuperType = reader.getSlingResourceSuperType();
            slingResourceSuperType.ifPresent(
                    resourceSuperType -> processExtendedResourceType(resourceType, entry, providedCapabilities, requiredCapabilities,
                            resourceSuperType));
            if (!reader.getSlingRequiredResourceTypes().isEmpty()) {
                processRequiredResourceTypes(entry, requiredCapabilities, new ArrayList<>(reader.getSlingRequiredResourceTypes()));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(String.format("Unable to read file %s.", entry), e);
        }
    }
}
