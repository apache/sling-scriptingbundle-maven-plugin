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
import java.util.Iterator;
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
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(scriptsDirectory.resolve(resourceTypeDirectory))) {
                for (Path directoryEntry : directoryStream) {
                    if (Files.isRegularFile(directoryEntry)) {
                        Path file = directoryEntry.getFileName();
                        if (file != null) {
                            if (MetadataMojo.EXTENDS_FILE.equals(file.toString())) {
                                processExtendsFile(resourceType, version, directoryEntry, providedCapabilities, requiredCapabilities);
                            } else if (MetadataMojo.REQUIRES_FILE.equals(file.toString())) {
                                processRequiresFile(directoryEntry, requiredCapabilities);
                            } else {
                                processScriptFile(resourceTypeDirectory, scriptEngineMappings, directoryEntry, resourceType, version,
                                        resourceTypeLabel, providedCapabilities);
                            }
                        }
                    } else if (Files.isDirectory(directoryEntry) && !resourceTypeFolderPredicate.test(directoryEntry)) {
                        try (Stream<Path> subtree = Files.walk(directoryEntry)) {
                            Iterator<Path> subtreeIterator = subtree.iterator();
                            while (subtreeIterator.hasNext()) {
                                Path subtreeEntry = subtreeIterator.next();
                                if (Files.isRegularFile(subtreeEntry)) {
                                    processScriptFile(resourceTypeDirectory, scriptEngineMappings, subtreeEntry, resourceType, version,
                                            resourceTypeLabel, providedCapabilities);
                                }
                            }
                        }
                    }
                }
            } catch (IOException | IllegalArgumentException e) {
                log.warn(String.format("Cannot analyse folder %s.", scriptsDirectory.resolve(resourceTypeDirectory).toString()), e);
            }
        }

        return new Capabilities(providedCapabilities, requiredCapabilities);
    }

    private void processExtendsFile(@NotNull String resourceType, @Nullable String version, @NotNull Path extendsFile,
                                    @NotNull Set<ProvidedCapability> providedCapabilities,
                                    @NotNull Set<RequiredCapability> requiredCapabilities)
            throws IOException {
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
                try {
                    if (extendedResourceTypeVersion != null) {
                        requiredBuilder.withVersionRange(
                                VersionRange.valueOf(extendedResourceTypeVersion.substring(extendedResourceTypeVersion.indexOf('=') + 1)));
                    }
                } catch (IllegalArgumentException ignored) {
                    log.warn(String.format("Invalid version range '%s' defined for extended resourceType '%s'" +
                                    " in file %s.", extendedResourceTypeVersion, extendedResourceType,
                            extendsFile.toString()));
                }
                requiredCapabilities.add(requiredBuilder.build());
            }
        }
    }

    private void processRequiresFile(@NotNull Path requiresFile,
                                     @NotNull Set<RequiredCapability> requiredCapabilities)
            throws IOException {
        List<String> requiredResourceTypes = Files.readAllLines(requiresFile, StandardCharsets.UTF_8);
        for (String requiredResourceType : requiredResourceTypes) {
            if (StringUtils.isNotEmpty(requiredResourceType)) {
                String[] requireParts = requiredResourceType.split(";");
                String resourceType = requireParts[0];
                String version = requireParts.length > 1 ? requireParts[1] : null;
                RequiredCapability.Builder requiredBuilder =
                        RequiredCapability.builder().withResourceType(resourceType);
                try {
                    if (version != null) {
                        requiredBuilder.withVersionRange(VersionRange.valueOf(version.substring(version.indexOf('=') + 1)));
                    }
                } catch (IllegalArgumentException ignored) {
                    log.warn(String.format("Invalid version range '%s' defined for required resourceType '%s'" +
                                    " in file %s.", version, resourceType,
                            requiresFile.toString()));
                }
                requiredCapabilities.add(requiredBuilder.build());
            }
        }
    }

    private void processScriptFile(@NotNull Path resourceTypeDirectory, @NotNull Map<String, String> scriptEngineMappings,
                                   @NotNull Path script, @NotNull String resourceType, @Nullable String version,
                                   @NotNull String resourceTypeLabel, @NotNull Set<ProvidedCapability> providedCapabilities) {
        Path scriptFile = script.getFileName();
        if (scriptFile != null) {
            Path relativeResourceTypeFolder = resourceTypeDirectory.relativize(script);
            int pathSegments = relativeResourceTypeFolder.getNameCount();
            ArrayList<String> selectors = new ArrayList<>();
            if (pathSegments > 1) {
                for (int i = 0; i < pathSegments - 1; i++) {
                    selectors.add(relativeResourceTypeFolder.getName(i).toString());
                }
            }
            String[] parts = scriptFile.toString().split("\\.");
            if (parts.length < 2 || parts.length > 4) {
                log.warn(String.format("Skipping script %s since it either does not target a script engine or it provides too many " +
                        "selector parts in its name.", script.toString()));
                return;
            }
            String name = parts[0];
            String scriptEngineExtension = parts[parts.length - 1];
            String scriptEngine = scriptEngineMappings.get(scriptEngineExtension);
            if (StringUtils.isNotEmpty(scriptEngine)) {
                String requestExtension = null;
                String requestMethod = null;
                if (parts.length == 2 && !resourceTypeLabel.equals(name)) {
                    if (MetadataMojo.METHODS.contains(name)) {
                        requestMethod = name;
                    } else if (MimeTypeChecker.hasMimeType(name)) {
                        requestExtension = name;
                    } else {
                        selectors.add(name);
                    }
                }
                if (parts.length == 3) {
                    String middle = parts[1];
                    if (MetadataMojo.METHODS.contains(middle)) {
                        requestMethod = middle;
                    } else if (MimeTypeChecker.hasMimeType(middle)) {
                        requestExtension = middle;
                    }
                    if (!resourceTypeLabel.equals(name)) {
                        selectors.add(name);
                    }
                }
                if (parts.length == 4){
                    requestExtension = parts[1];
                    requestMethod = parts[2];
                    if (!resourceTypeLabel.equals(name)) {
                        selectors.add(name);
                    }
                }
                providedCapabilities.add(
                        ProvidedCapability.builder()
                                .withResourceType(resourceType)
                                .withVersion(version)
                                .withSelectors(selectors)
                                .withRequestExtension(requestExtension)
                                .withRequestMethod(requestMethod)
                                .withScriptEngine(scriptEngine)
                                .build()
                );
            } else {
                log.warn(String.format("Cannot find a script engine mapping for script %s.", script.toString()));
            }
        } else {
            log.warn(String.format("Skipping path %s since it has 0 elements.", script.toString()));
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
