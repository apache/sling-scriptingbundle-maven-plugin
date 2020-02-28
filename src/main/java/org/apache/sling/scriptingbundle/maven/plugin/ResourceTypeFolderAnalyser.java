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
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;


class ResourceTypeFolderAnalyser {

    private final Log log;
    private final Path scriptsDirectory;

    ResourceTypeFolderAnalyser(@NotNull Log log, @NotNull Path scriptsDirectory) {
        this.log = log;
        this.scriptsDirectory = scriptsDirectory;
    }

    Capabilities getCapabilities(@NotNull Path resourceTypeDirectory) {
        var providedCapabilities = new ArrayList<ProvidedCapability>();
        var requiredCapabilities = new ArrayList<RequiredCapability>();
        String version = null;
        String resourceType = null;
        try {
            version = Version.parseVersion(resourceTypeDirectory.getFileName().toString()).toString();
        } catch (IllegalArgumentException ignored) {
            // no version
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
                for (Path child : directoryStream) {
                    if (Files.isRegularFile(child)) {
                        Path file = child.getFileName();
                        if (file != null) {
                            String[] scriptNameParts = file.toString().split("\\.");
                            if (scriptNameParts.length == 1) {
                                if (MetadataMojo.EXTENDS_FILE.equals(file.toString())) {
                                    processExtendsFile(resourceType, version, child, providedCapabilities, requiredCapabilities);
                                } else if (MetadataMojo.REQUIRES_FILE.equals(file.toString())) {
                                    processRequiresFile(child, requiredCapabilities);
                                }
                            } else {

                            }
                        }
                    } else if (Files.isDirectory(child)) {

                    }
                }
            } catch (IOException | IllegalArgumentException e) {
                log.warn(String.format("Cannot analyser folder %s.", scriptsDirectory.resolve(resourceTypeDirectory).toString()));
                if (log.isDebugEnabled()) {
                    log.debug(e);
                }
            }
        }

        return new Capabilities(providedCapabilities, requiredCapabilities);
    }

    private void processExtendsFile(@NotNull String resourceType, @Nullable String version, @NotNull Path extendsFile,
                                    @NotNull ArrayList<ProvidedCapability> providedCapabilities,
                                    @NotNull ArrayList<RequiredCapability> requiredCapabilities)
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
                        requiredBuilder.withVersionRange(VersionRange.valueOf(extendedResourceTypeVersion));
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
                                    @NotNull ArrayList<RequiredCapability> requiredCapabilities)
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
                        requiredBuilder.withVersionRange(VersionRange.valueOf(version));
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
        }
        if (StringUtils.isEmpty(resourceTypeLabel)) {
            throw new IllegalArgumentException(String.format("Resource type '%s' does not provide a resourceTypeLabel.", resourceType));
        }
        return resourceTypeLabel;
    }

}
