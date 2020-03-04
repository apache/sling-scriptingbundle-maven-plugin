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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
    private Capabilities capabilities;

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
        final String root = sdFile.getAbsolutePath();
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
                        .map(includedDirectory -> Paths.get(root, includedDirectory))
                        .filter(new ResourceTypeFolderPredicate()).collect(Collectors.toList());
        ResourceTypeFolderAnalyser analyser = new ResourceTypeFolderAnalyser(log, Paths.get(root));
        log.info("Detected resource type directories: " + resourceTypeDirectories);
        Set<ProvidedCapability> providedCapabilities = new LinkedHashSet<>();
        Set<RequiredCapability> requiredCapabilities = new LinkedHashSet<>();
        for (Path resourceTypeDirectory : resourceTypeDirectories) {
            Capabilities resourceTypeCapabilities = analyser.getCapabilities(resourceTypeDirectory);
            providedCapabilities.addAll(resourceTypeCapabilities.getProvidedCapabilities());
            requiredCapabilities.addAll(resourceTypeCapabilities.getRequiredCapabilities());
        }
       capabilities = new Capabilities(providedCapabilities, requiredCapabilities);
    }

    Capabilities getCapabilities() {
        return capabilities;
    }

}
