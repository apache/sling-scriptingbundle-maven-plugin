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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.io.DirectoryScanner;
import org.osgi.framework.Version;

/**
 * The {@code metadata} goal will generate two Maven project properties, namely {@code org.apache.sling.scriptingbundle.maven.plugin
 * .Require-Capability} and {@code org.apache.sling.scriptingbundle.maven.plugin.Provide-Capability} which can be used to generate the
 * corresponding OSGi bundle headers for bundles providing scripts executable by a {@link javax.script.ScriptEngine}.
 */
@Mojo(name = "metadata",
      defaultPhase = LifecyclePhase.PACKAGE)
public class MetadataMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}",
               readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}",
               readonly = true)
    private MavenSession session;

    /**
     * Defines where this goal will look for scripts in the project.
     *
     * @since 0.1.0
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/resources/javax.script")
    private String scriptsDirectory;

    static final Set<String> METHODS = new HashSet<>(Arrays.asList("TRACE", "OPTIONS", "GET", "HEAD", "POST", "PUT",
            "DELETE", "PATCH"));
    static final String EXTENDS_FILE = "extends";
    static final String REQUIRES_FILE = "requires";

    private final Log log;

    public MetadataMojo() {
        log = getLog();
    }

    public void execute() throws MojoExecutionException {
        File sdFile = new File(scriptsDirectory);
        if (!sdFile.exists()) {
            sdFile = new File(project.getBasedir(), scriptsDirectory);
            if (!sdFile.exists()) {
                throw new MojoExecutionException("Cannot find file " + scriptsDirectory + ".");
            }
        }
        final AtomicReference<File> scriptsDirectoryReference = new AtomicReference<>();
        scriptsDirectoryReference.set(sdFile);
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(sdFile);
        scanner.setIncludes("**");
        scanner.setExcludes("**/*.class");
        scanner.addDefaultExcludes();
        scanner.scan();

        List<String> includedDirectories = Arrays.asList(scanner.getIncludedDirectories());
        includedDirectories.sort(Collections.reverseOrder());
        List<Path> resourceTypeDirectories =
                includedDirectories.stream()
                        .map(includedDirectory -> Paths.get(scriptsDirectoryReference.get().getAbsolutePath(), includedDirectory))
                        .filter(new ResourceTypeFolderPredicate()).collect(Collectors.toList());
        ResourceTypeFolderAnalyser analyser = new ResourceTypeFolderAnalyser(log, Paths.get(sdFile.getAbsolutePath()));
        log.info("Detected resource type directories: " + resourceTypeDirectories);
        for (Path resourceTypeDirectory : resourceTypeDirectories) {
            Capabilities capabilities = analyser.getCapabilities(resourceTypeDirectory);
            log.info(String.format("folder: %s, require: %s, provide: %s", resourceTypeDirectory.toString(),
                    capabilities.getRequiredCapabilities(), capabilities.getProvidedCapabilities()));
        }
    }

    ProvidedCapability getScript(String script) {
        String[] parts = script.split(Pattern.quote(File.separator));
        String scriptName = null;
        String resourceType = null;
        String scriptExtension = null;
        String extension = null;
        String method = null;
        String version = null;
        List<String> selectors = new ArrayList<>();
        if (parts.length > 2) {
            Version v = null;
            int versionIndex = -1;
            for (int i = parts.length - 1; i >= 1; i--) {
                try {
                    v = new Version(parts[i]);
                    versionIndex = i;
                } catch (IllegalArgumentException e) {
                    // do nothing
                }
            }
            if (v != null) {
                version = v.toString();
                resourceType = String.join("/", Arrays.copyOfRange(parts, 0, versionIndex - 1));
                if (parts.length - versionIndex > 2) {
                    Collections.addAll(selectors, Arrays.copyOfRange(parts, versionIndex + 1, parts.length - 1));
                }
            } else {
                resourceType = String.join("/", Arrays.copyOfRange(parts, 0, versionIndex - 1));
                if (parts.length - versionIndex > 2) {
                    Collections.addAll(selectors, Arrays.copyOfRange(parts, versionIndex + 1, parts.length - 1));
                }
            }
            scriptName = parts[parts.length - 1];
            String[] scriptParts = scriptName.split("\\.");
            if (scriptParts.length < 2) {
                throw new IllegalStateException("Missing script extension.");
            }
            scriptExtension = scriptParts[scriptParts.length - 1];
            String resourceTypeLabel;
            int index = resourceType.lastIndexOf('.') == -1 ? resourceType.lastIndexOf('/') : -1;
            if (index == -1) {
                resourceTypeLabel = resourceType;
            } else {
                resourceTypeLabel = resourceType.substring(index + 1);
            }
            String first = scriptParts[0];
            if (!first.equals(resourceTypeLabel)) {
                int selectorsStart;
                if (METHODS.contains(first)) {
                    method = first;
                    selectorsStart = 1;
                } else {
                    selectorsStart = 0;
                }
                for (int i = selectorsStart; i < scriptParts.length - 3; i++) {
                    selectors.add(scriptParts[i]);
                }
                if (scriptParts.length >= 3) {
                    extension = scriptParts[scriptParts.length - 2];
                }
            }
        } else if (parts.length == 2) {
            resourceType = parts[0];
        }


        if (parts.length >= 2) {
            scriptName = parts[parts.length - 1];
            Version v = null;
            try {
                if (parts.length > 2) {
                    v = new Version(parts[parts.length - 2]);
                }
            } catch (IllegalArgumentException e) {
                log.debug(String.format("No resource type version available for script %s.", script));
            }
            version = v == null ? null : v.toString();
            resourceType = version == null ? String.join("/", Arrays.copyOfRange(parts, 0, parts.length - 1)) : String.join("/",
                    Arrays.copyOfRange(parts, 0, parts.length - 2));
            int idx = scriptName.lastIndexOf('.');
            if (idx != -1) {
                scriptExtension = scriptName.substring(idx + 1);
                scriptName = scriptName.substring(0, idx);
                if (scriptExtension.isEmpty()) {
                    scriptExtension = null;
                }
            }

            idx = scriptName.lastIndexOf('.');
            if (idx != -1) {
                extension = scriptName.substring(idx + 1);
                scriptName = scriptName.substring(0, idx);
                if (extension.isEmpty() || extension.equalsIgnoreCase("html")) {
                    extension = null;
                }
            } else {
                extension = null;
            }

            idx = scriptName.indexOf('.');
            if (idx != -1) {
                String methodString = scriptName.substring(0, idx).toUpperCase();
                if (METHODS.contains(methodString)) {
                    method = methodString;
                    scriptName = scriptName.substring(idx + 1);
                }
            } else if (METHODS.contains(scriptName.toUpperCase())) {
                method = scriptName.toUpperCase();
            }
        }
//        return Capability.builder().withName(scriptName).withResourceType(resourceType).withScriptExtension(scriptExtension)
//                .withVersion(version).withExtension(extension).withMethod(method).build();
        return null;
    }
}
