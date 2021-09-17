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
package org.apache.sling.scriptingbundle.plugin.bnd;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.scriptingbundle.plugin.capability.Capabilities;
import org.apache.sling.scriptingbundle.plugin.processor.Constants;
import org.apache.sling.scriptingbundle.plugin.processor.Logger;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.service.AnalyzerPlugin;
import aQute.bnd.service.Plugin;
import aQute.service.reporter.Reporter;

public class BundledScriptsScannerPlugin implements AnalyzerPlugin, Plugin {

    public static final String GLOB = "glob:";
    static final String PROJECT_BUILD_FOLDER = "project.output";
    static final String PROJECT_ROOT_FOLDER = "project.dir";

    private Map<String, String> pluginProperties;
    private Reporter reporter;
    private Logger logger;

    private Capabilities capabilities;
    private Map<String, String> scriptEngineMappings;
    private boolean inContentPackage;

    @Override
    public boolean analyzeJar(Analyzer analyzer) throws Exception {
        logger = new BndLogger(reporter);
        inContentPackage = "content-package".equals(analyzer.get("project.packaging"));
        Path workDirectory = Paths.get(analyzer.get(PROJECT_BUILD_FOLDER), "scriptingbundle-maven-plugin");
        Files.createDirectories(workDirectory);
        Set<PathMatcher> includes = getConfiguredIncludes();
        Set<PathMatcher> excludes = getConfiguredExcludes();
        getConfiguredSourceDirectories().stream().map(sourceDirectory -> {
            Path sourceDirectoryPath = Paths.get(sourceDirectory);
            if (!Files.exists(sourceDirectoryPath)) {
                sourceDirectoryPath = Paths.get(analyzer.get(PROJECT_ROOT_FOLDER), sourceDirectory);
            }
            return sourceDirectoryPath;
        }).filter(sourceDirectoryPath -> Files.exists(sourceDirectoryPath) && Files.isDirectory(sourceDirectoryPath)).forEach(sourceDirectoryPath -> {
            try (Stream<Path> includedFiles = walkPath(sourceDirectoryPath, includes, excludes)) {
                includedFiles.forEach(
                    file -> {
                        try {
                            if (!Files.isDirectory(file)) {
                                Path workingCopy = Paths.get(workDirectory.toString(), sourceDirectoryPath.relativize(file).toString());
                                Files.createDirectories(workingCopy.getParent());
                                Files.copy(file, workingCopy, StandardCopyOption.REPLACE_EXISTING);
                            }
                        } catch (IOException e) {
                            logger.error("Cannot copy file into working directory.", e);
                        }
                    }
                );
            } catch (IOException e) {
                logger.error("Cannot analyse source folders.", e);
            }
        });
        scriptEngineMappings = getConfiguredScriptEngineMappings();
        capabilities = Capabilities
                .fromFileSystemTree(workDirectory, walkPath(workDirectory, includes, excludes), logger,
                getConfiguredSearchPaths(), scriptEngineMappings, getMissingRequirementsOptional(), inContentPackage);
        String providedCapabilitiesDefinition = capabilities.getProvidedCapabilitiesString();
        String requiredCapabilitiesDefinition = capabilities.getRequiredCapabilitiesString();

        String providedCapabilities = analyzer.get(aQute.bnd.osgi.Constants.PROVIDE_CAPABILITY);
        if (StringUtils.isNotEmpty(providedCapabilities)) {
            providedCapabilities += ", " + providedCapabilitiesDefinition;
        } else {
            providedCapabilities = providedCapabilitiesDefinition;
        }
        analyzer.set(aQute.bnd.osgi.Constants.PROVIDE_CAPABILITY, providedCapabilities);

        String requiredCapabilities = analyzer.get(aQute.bnd.osgi.Constants.REQUIRE_CAPABILITY);
        if (StringUtils.isNotEmpty(requiredCapabilities)) {
            requiredCapabilities += ", " + requiredCapabilitiesDefinition;
        } else {
            requiredCapabilities = requiredCapabilitiesDefinition;
        }
        analyzer.set(aQute.bnd.osgi.Constants.REQUIRE_CAPABILITY, requiredCapabilities);
        return false;
    }

    @Override
    public void setProperties(Map<String, String> pluginProperties) {
        this.pluginProperties = pluginProperties;
    }

    @Override
    public void setReporter(Reporter reporter) {
        this.reporter = reporter;
    }

    public Capabilities getCapabilities() {
        return capabilities;
    }

    public Map<String, String> getScriptEngineMappings() {
        return scriptEngineMappings;
    }

    private Set<String> getConfiguredSourceDirectories() {
        String sourceDirectoriesCSV = pluginProperties.get(Constants.BND_SOURCE_DIRECTORIES);
        if (StringUtils.isNotEmpty(sourceDirectoriesCSV)) {
            return Collections.unmodifiableSet(Arrays.stream(sourceDirectoriesCSV.split(",")).map(String::trim).collect(Collectors.toSet()));
        }
        return Constants.DEFAULT_SOURCE_DIRECTORIES;
    }

    private Set<PathMatcher> getConfiguredExcludes() {
        String excludesCSV = pluginProperties.get(Constants.BND_EXCLUDES);
        if (StringUtils.isNotEmpty(excludesCSV)) {
            return Collections.unmodifiableSet(Arrays.stream(excludesCSV.split(",")).map(String::trim)
                    .map(pattern -> FileSystems.getDefault().getPathMatcher(GLOB + pattern)).collect(
                            Collectors.toSet()));
        }
        return Collections.unmodifiableSet(Constants.DEFAULT_EXCLUDES.stream().map(pattern -> FileSystems.getDefault().getPathMatcher(GLOB + pattern))
                .collect(Collectors.toSet()));
    }

    private Set<PathMatcher> getConfiguredIncludes() {
        String includesCSV = pluginProperties.get(Constants.BND_INCLUDES);
        if (StringUtils.isNotEmpty(includesCSV)) {
            return Collections.unmodifiableSet(Arrays.stream(includesCSV.split(",")).map(String::trim)
                    .map(pattern -> FileSystems.getDefault().getPathMatcher(GLOB + pattern)).collect(
                            Collectors.toSet()));
        }
        return Collections.emptySet();
    }

    private Map<String, String> getConfiguredScriptEngineMappings() {
        HashMap<String, String> mappings = new HashMap<>(Constants.DEFAULT_EXTENSION_TO_SCRIPT_ENGINE_MAPPING);
        String scriptEngineMappingsCSV = pluginProperties.get(Constants.BND_SCRIPT_ENGINE_MAPPINGS);
        if (StringUtils.isNotEmpty(scriptEngineMappingsCSV)) {
            List<String> extensionEngineMappings =
                    Arrays.stream(scriptEngineMappingsCSV.split(",")).map(String::trim).collect(Collectors.toList());
            extensionEngineMappings.forEach(mapping -> {
                String[] mappingArray = mapping.split(":");
                if (mappingArray.length != 2) {
                    logger.error(String.format("Invalid script engine mapping: %s.", mapping));
                } else {
                    mappings.put(mappingArray[0].trim(), mappingArray[1].trim());
                }
            });
        }
        return Collections.unmodifiableMap(mappings);
    }

    private Set<String> getConfiguredSearchPaths() {
        String searchPathsString = pluginProperties.get(Constants.BND_SEARCH_PATHS);
        if (StringUtils.isNotEmpty(searchPathsString)) {
            return Collections.unmodifiableSet(Arrays.stream(searchPathsString.split(",")).map(String::trim).collect(Collectors.toSet()));
        }
        return Constants.DEFAULT_SEARCH_PATHS;
    }

    private boolean getMissingRequirementsOptional() {
        String missingRequirementsOptionalString = pluginProperties.get(Constants.BND_MISSING_REQUIREMENTS_OPTIONAL);
        if (missingRequirementsOptionalString != null) {
            missingRequirementsOptionalString = missingRequirementsOptionalString.trim().toLowerCase();
            return !"false".equals(missingRequirementsOptionalString);
        }
        return true;
    }

    private Stream<Path> walkPath(Path path, Set<PathMatcher> includes, Set<PathMatcher> excludes) throws IOException {
        return Files.walk(path).filter(file -> {
            boolean include = false;
            if (includes.isEmpty()) {
                include = true;
            }
            Optional<PathMatcher> includeOptions = includes.stream().filter(pathMatcher -> pathMatcher.matches(file)).findFirst();
            if (includeOptions.isPresent()) {
                include = true;
            }
            Optional<PathMatcher> excludeOptions = excludes.stream().filter(pathMatcher -> pathMatcher.matches(file)).findFirst();
            if (excludeOptions.isPresent()) {
                include = false;
            }
            return include;
        }).flatMap(file -> {
            if (!Files.isDirectory(file)) {
                return Stream.of(file, file.getParent());
            }
            return Stream.of(file);
        });
    }

}
