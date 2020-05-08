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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.maven.plugin.logging.Log;
import org.apache.sling.scriptingbundle.maven.plugin.capability.Capabilities;
import org.apache.sling.scriptingbundle.maven.plugin.capability.ProvidedResourceTypeCapability;
import org.apache.sling.scriptingbundle.maven.plugin.capability.RequiredResourceTypeCapability;
import org.apache.sling.servlets.resolver.bundle.tracker.ResourceType;
import org.jetbrains.annotations.NotNull;


class ResourceTypeFolderAnalyser {

    private final Log log;
    private final Path scriptsDirectory;
    private final ResourceTypeFolderPredicate resourceTypeFolderPredicate;
    private FileProcessor fileProcessor;

    ResourceTypeFolderAnalyser(@NotNull Log log, @NotNull Path scriptsDirectory, @NotNull FileProcessor fileProcessor) {
        this.log = log;
        this.scriptsDirectory = scriptsDirectory;
        this.resourceTypeFolderPredicate = new ResourceTypeFolderPredicate(log);
        this.fileProcessor = fileProcessor;
    }

    Capabilities getCapabilities(@NotNull Path resourceTypeDirectory) {
        Set<ProvidedResourceTypeCapability> providedCapabilities = new LinkedHashSet<>();
        Set<RequiredResourceTypeCapability> requiredCapabilities = new LinkedHashSet<>();
        if (resourceTypeDirectory.startsWith(scriptsDirectory) && resourceTypeFolderPredicate.test(resourceTypeDirectory)) {
            try (DirectoryStream<Path> resourceTypeDirectoryStream = Files
                    .newDirectoryStream(scriptsDirectory.resolve(resourceTypeDirectory))) {
                Path relativeResourceTypeDirectory = scriptsDirectory.relativize(resourceTypeDirectory);
                final ResourceType resourceType = ResourceType.parseResourceType(relativeResourceTypeDirectory.toString());
                resourceTypeDirectoryStream.forEach(entry -> {
                    if (Files.isRegularFile(entry)) {
                        Path file = entry.getFileName();
                        if (file != null) {
                            if (MetadataMojo.EXTENDS_FILE.equals(file.toString())) {
                                fileProcessor.processExtendsFile(resourceType, entry, providedCapabilities, requiredCapabilities);
                            } else if (MetadataMojo.REQUIRES_FILE.equals(file.toString())) {
                                fileProcessor.processRequiresFile(entry, requiredCapabilities);
                            } else {
                                fileProcessor.processScriptFile(resourceTypeDirectory, entry, resourceType, providedCapabilities);
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
                                    file -> fileProcessor.processScriptFile(resourceTypeDirectory, file, resourceType, providedCapabilities)
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

        return new Capabilities(providedCapabilities, Collections.emptySet(), requiredCapabilities);
    }
}
