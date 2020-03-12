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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;


class ResourceTypeFolderAnalyser {

    private final Log log;
    private final Path scriptsDirectory;
    private final ResourceTypeFolderPredicate resourceTypeFolderPredicate;

    ResourceTypeFolderAnalyser(@NotNull Log log, @NotNull Path scriptsDirectory) {
        this.log = log;
        this.scriptsDirectory = scriptsDirectory;
        this.resourceTypeFolderPredicate = new ResourceTypeFolderPredicate(log);
    }

    Capabilities getCapabilities(@NotNull Path resourceTypeDirectory, @NotNull Map<String, String> scriptEngineMappings) {
        Set<ProvidedCapability> providedCapabilities = new LinkedHashSet<>();
        Set<RequiredCapability> requiredCapabilities = new LinkedHashSet<>();
        String version = null;
        String resourceType = null;
        try {
            Path fileName = resourceTypeDirectory.getFileName();
            if (fileName != null) {
                version = Version.parseVersion(fileName.toString()).toString();
            }
        } catch (IllegalArgumentException ignored) {
            log.debug("No resourceType version detected in " + scriptsDirectory.resolve(resourceTypeDirectory).toString());
        }
        if (StringUtils.isNotEmpty(version)) {
            Path parent = resourceTypeDirectory.getParent();
            if (parent != null) {
                resourceType = getResourceType(parent);
            }
        } else {
            resourceType = getResourceType(resourceTypeDirectory);
        }
        if (resourceType != null) {
            String resourceTypeLabel = getResourceTypeLabel(resourceType);
            String finalResourceType = resourceType;
            String finalVersion = version;
            try (DirectoryStream<Path> resourceTypeDirectoryStream = Files.newDirectoryStream(scriptsDirectory.resolve(resourceTypeDirectory))) {
                resourceTypeDirectoryStream.forEach(entry -> {
                    if (Files.isRegularFile(entry)) {
                        Path file = entry.getFileName();
                        if (file != null) {
                            if (MetadataMojo.EXTENDS_FILE.equals(file.toString())) {
                                processExtendsFile(finalResourceType, finalVersion, entry, providedCapabilities, requiredCapabilities);
                            } else if (MetadataMojo.REQUIRES_FILE.equals(file.toString())) {
                                processRequiresFile(entry, requiredCapabilities);
                            } else {
                                processScriptFile(resourceTypeDirectory, scriptEngineMappings, entry, finalResourceType, finalVersion,
                                        resourceTypeLabel, providedCapabilities);
                            }
                        }
                    } else if (Files.isDirectory(entry) && !resourceTypeFolderPredicate.test(entry)) {
                        try (Stream<Path> selectorFilesStream = Files.walk(entry).filter(Files::isRegularFile).filter(file -> {
                            Path fileParent = file.getParent();
                            while (!resourceTypeDirectory.equals(fileParent)) {
                                if (resourceTypeFolderPredicate.test(fileParent)) {
                                    return false;
                                }
                                fileParent = fileParent.getParent();
                            }
                            return true;
                        })) {
                            selectorFilesStream.forEach(
                                    file -> processScriptFile(resourceTypeDirectory, scriptEngineMappings, file, finalResourceType,
                                            finalVersion, resourceTypeLabel, providedCapabilities)
                            );
                        } catch (IOException e) {
                            log.error(String.format("Unable to scan folder %s.", entry.toString()), e);
                        }
                    }
                });
            } catch (IOException | IllegalArgumentException e) {
                log.warn(String.format("Cannot analyse folder %s.", scriptsDirectory.resolve(resourceTypeDirectory).toString()), e);
            }
        }

        return new Capabilities(providedCapabilities, requiredCapabilities);
    }

    private void processExtendsFile(@NotNull String resourceType, @Nullable String version, @NotNull Path extendsFile,
                                    @NotNull Set<ProvidedCapability> providedCapabilities,
                                    @NotNull Set<RequiredCapability> requiredCapabilities) {
        try {
            List<String> extendResources = Files.readAllLines(extendsFile, StandardCharsets.UTF_8);
            if (extendResources.size() == 1) {
                String extend = extendResources.get(0);
                if (StringUtils.isNotEmpty(extend)) {
                    String[] extendParts = extend.split(";");
                    String extendedResourceType = extendParts[0];
                    String extendedResourceTypeVersion = extendParts.length > 1 ? extendParts[1] : null;
                    providedCapabilities.add(
                            ProvidedCapability.builder()
                                    .withResourceType(resourceType)
                                    .withVersion(version)
                                    .withExtendsResourceType(extendedResourceType)
                                    .build());
                    RequiredCapability.Builder requiredBuilder =
                            RequiredCapability.builder().withResourceType(extendedResourceType);
                    extractVersionRange(extendsFile, requiredBuilder, extendedResourceTypeVersion);
                    requiredCapabilities.add(requiredBuilder.build());
                }
            }
        } catch (IOException e) {
            log.error(String.format("Unable to read file %s.", extendsFile.toString()), e);
        }
    }

    private void processRequiresFile(@NotNull Path requiresFile,
                                     @NotNull Set<RequiredCapability> requiredCapabilities) {
        try {
            List<String> requiredResourceTypes = Files.readAllLines(requiresFile, StandardCharsets.UTF_8);
            for (String requiredResourceType : requiredResourceTypes) {
                if (StringUtils.isNotEmpty(requiredResourceType)) {
                    String[] requireParts = requiredResourceType.split(";");
                    String resourceType = requireParts[0];
                    String version = requireParts.length > 1 ? requireParts[1] : null;
                    RequiredCapability.Builder requiredBuilder =
                            RequiredCapability.builder().withResourceType(resourceType);
                    extractVersionRange(requiresFile, requiredBuilder, version);
                    requiredCapabilities.add(requiredBuilder.build());
                }
            }
        } catch (IOException e) {
            log.error(String.format("Unable to read file %s.", requiresFile.toString()), e);
        }
    }

    private void extractVersionRange(@NotNull Path requiresFile, @NotNull RequiredCapability.Builder requiredBuilder, String version) {
        try {
            if (version != null) {
                requiredBuilder.withVersionRange(VersionRange.valueOf(version.substring(version.indexOf('=') + 1)));
            }
        } catch (IllegalArgumentException ignored) {
            log.warn(String.format("Invalid version range format %s in file %s.", version, requiresFile.toString()));
        }
    }

    private void processScriptFile(@NotNull Path resourceTypeDirectory, @NotNull Map<String, String> scriptEngineMappings,
                                   @NotNull Path scriptPath, @NotNull String resourceType, @Nullable String version,
                                   @NotNull String resourceTypeLabel, @NotNull Set<ProvidedCapability> providedCapabilities) {
        Path scriptFile = scriptPath.getFileName();
        if (scriptFile != null) {
            Path relativeResourceTypeFolder = resourceTypeDirectory.relativize(scriptPath);
            int pathSegments = relativeResourceTypeFolder.getNameCount();
            ArrayList<String> selectors = new ArrayList<>();
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
                    if (!resourceTypeLabel.equals(scriptName)) {
                        if (scriptFileName.split("\\.").length == 2 && scriptName != null &&
                                scriptName.equals(script.getRequestExtension())) {
                            /*
                            it's unclear if the script name represents a selector or an extension, but it cannot be both;
                            so we have:
                                1. capability for the name as a selector
                                2. capability for the name as a request extension
                             */
                            ArrayList<String> capSelectors = new ArrayList<>(selectors);
                            capSelectors.add(scriptName);
                            providedCapabilities.add(
                                    ProvidedCapability.builder()
                                            .withResourceType(resourceType)
                                            .withVersion(version)
                                            .withSelectors(capSelectors)
                                            .withRequestMethod(script.getRequestMethod())
                                            .withScriptEngine(scriptEngine)
                                            .build()
                            );
                            providedCapabilities.add(
                                    ProvidedCapability.builder()
                                            .withResourceType(resourceType)
                                            .withVersion(version)
                                            .withSelectors(selectors)
                                            .withRequestExtension(script.getRequestExtension())
                                            .withRequestMethod(script.getRequestMethod())
                                            .withScriptEngine(scriptEngine)
                                            .build()
                            );
                        } else {
                            if (scriptName != null && !MetadataMojo.METHODS.contains(scriptName)) {
                                selectors.add(script.getName());
                            }
                        }
                    }
                    providedCapabilities.add(
                            ProvidedCapability.builder()
                                    .withResourceType(resourceType)
                                    .withVersion(version)
                                    .withSelectors(selectors)
                                    .withRequestExtension(script.getRequestExtension())
                                    .withRequestMethod(script.getRequestMethod())
                                    .withScriptEngine(scriptEngine)
                                    .build()
                    );
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

    private String getResourceType(@NotNull Path resourceTypeFolder) {
        StringBuilder stringBuilder = new StringBuilder();
        Path relativeResourceTypePath = scriptsDirectory.relativize(resourceTypeFolder);
        if (StringUtils.isNotEmpty(relativeResourceTypePath.toString())) {
            int parts = relativeResourceTypePath.getNameCount();
            for (int i = 0; i < parts; i++) {
                stringBuilder.append(relativeResourceTypePath.getName(i));
                if (i < parts - 1) {
                    stringBuilder.append('/');
                }
            }
        }
        return stringBuilder.toString();
    }

    private @NotNull String getResourceTypeLabel(@NotNull String resourceType) {
        String resourceTypeLabel = null;
        if (resourceType.contains("/")) {
            int lastIndex = resourceType.lastIndexOf('/');
            if (lastIndex < resourceType.length() - 2) {
                resourceTypeLabel = resourceType.substring(++lastIndex);
            }
        } else if (resourceType.contains(".")) {
            int lastIndex = resourceType.lastIndexOf('.');
            if (lastIndex < resourceType.length() - 2) {
                resourceTypeLabel = resourceType.substring(++lastIndex);
            }
        } else {
            resourceTypeLabel = resourceType;
        }
        if (StringUtils.isEmpty(resourceTypeLabel)) {
            throw new IllegalArgumentException(String.format("Resource type '%s' does not provide a resourceTypeLabel.", resourceType));
        }
        return resourceTypeLabel;
    }
}
