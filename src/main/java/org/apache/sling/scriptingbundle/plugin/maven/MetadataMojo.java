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
package org.apache.sling.scriptingbundle.plugin.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.script.ScriptEngineFactory;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.io.DirectoryScanner;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.scriptingbundle.plugin.capability.Capabilities;
import org.apache.sling.scriptingbundle.plugin.capability.RequiredResourceTypeCapability;
import org.apache.sling.scriptingbundle.plugin.processor.Constants;
import org.apache.sling.scriptingbundle.plugin.processor.Logger;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

/**
 * The {@code metadata} goal will generate two Maven project properties, namely
 * {@code org.apache.sling.scriptingbundle.maven.plugin.Require-Capability} and
 * {@code org.apache.sling.scriptingbundle.maven.plugin.Provide-Capability} which can be used to generate the
 * corresponding OSGi bundle headers for bundles providing scripts executable by a {@link javax.script.ScriptEngine}.
 */
@Mojo(name = "metadata",
      defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class MetadataMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Defines where this goal will look for scripts in the project. By default the {@code src/main/scripts} and
     * {@code src/main/resources/javax.script} folders will be considered.
     *
     * @since 0.2.0
     */
    @Parameter(property = "scriptingbundle.sourceDirectories")
    private Set<String> sourceDirectories;

    /**
     * Allows defining a list of included files and folders or patterns to filter which files should be included in the analysis for
     * generating capabilities. By default all files are included.
     *
     * @since 0.2.0
     */
    @Parameter(property = "scriptingbundle.includes", defaultValue = "**")
    private String[] includes;

    /**
     * Allows defining a list of excluded files and folders or patterns to filter which files should be excluded in the analysis for
     * generating capabilities.
     * <p>The following list provides the default excluded files and patterns:
     * <dl>
     * <dt>
     *     Miscellaneous typical temporary files
     * </dt>
     * <dd>
     *     **&#47;*~, **&#47;#*#, **&#47;.#*, **&#47;%*%, **&#47;._*
     * </dd>
     * <dt>
     *     CVS
     * </dt>
     * <dd>**&#47;CVS, **&#47;CVS/**, **&#47;.cvsignore</dd>
     * <dt>Subversion</dt>
     * <dd>**&#47;.svn, **&#47;.svn/**</dd>
     * <dt>Bazaar</dt>
     * <dd>**&#47;.bzr, **&#47;.bzr/**</dd>
     * <dt>Mac</dt>
     * <dd>**&#47;.DS_Store</dd>
     * <dt>Mercurial</dt>
     * <dd>**&#47;.hg, **&#47;.hg/**</dd>
     * <dt>git</dt>
     * <dd>**&#47;.git, **&#47;.git/**</dd>
     * </dl>
     *
     * @since 0.2.0
     */
    @Parameter(property = "scriptingbundle.excludes")
    private String[] excludes;

    /**
     * Allows overriding the default extension to script engine mapping, in order to correctly generate the
     * {@code sling.resourceType;scriptEngine} {@code Provide-Capability} attribute value. When configuring this mapping, please make
     * sure to use the script extension as the key and one of the Script Engine's names (obtained from
     * {@link ScriptEngineFactory#getNames()}) as the value.
     *
     * <p>The following list represents the default extension to Script Engine mapping:</p>
     * <dl>
     *     <dt>ftl</dt>
     *     <dd>freemarker</dd>
     *
     *     <dt>gst</dt>
     *     <dd>gstring</dd>
     *
     *     <dt>html</dt>
     *     <dd>htl</dd>
     *
     *     <dt>java</dt>
     *     <dd>java</dd>
     *
     *     <dt>esp, ecma</dt>
     *     <dd>rhino</dd>
     *
     *     <dt>jsp, jspf, jspx</dt>
     *     <dd>jsp</dd>
     * </dl>
     *
     * @since 0.2.0
     */
    @Parameter
    private Map<String, String> scriptEngineMappings;

    /**
     * Allows overriding the default search paths ({@code /apps} and {@code /libs}). When scripts are organised in folders which follow
     * the search path structure, the Mojo will generate two resource types for each resource type folder. For example:
     * <pre>
     *     src/main/scripts/apps/org/apache/sling/example/example.html
     * </pre>
     * will generate the following two resource types
     * <pre>
     *     org/apache/sling/example
     *     /apps/org/apache/sling/example
     * </pre>
     *
     * However, the following script
     * <pre>
     *     src/main/scripts/org/apache/sling/example/example.html
     * </pre>
     * will generate only one resource type
     * <pre>
     *     org/apache/sling/example
     * </pre>
     *
     * @since 0.2.0
     */
    @Parameter(property = "scriptingbundle.searchPaths")
    private Set<String> searchPaths;

    /**
     * When set to "true", the requirements which are not satisfied directly by this project will be marked as optional.
     *
     * @since 0.5.0
     */
    @Parameter(property = "scriptingbundle.missingRequirementsOptional", defaultValue = "true")
    private boolean missingRequirementsOptional = true;

    private Capabilities capabilities;

    public void execute() {
        Logger logger = new MavenLogger(getLog());
        Path workDirectory = Paths.get(project.getBuild().getDirectory(), "scriptingbundle-maven-plugin");
        try {
            Files.createDirectories(workDirectory);
            if (sourceDirectories.isEmpty()) {
                sourceDirectories = new HashSet<>(Constants.DEFAULT_SOURCE_DIRECTORIES);
            }
            sourceDirectories.stream().map(sourceDirectory -> {
                Path sourceDirectoryPath = Paths.get(sourceDirectory);
                if (!Files.exists(sourceDirectoryPath)) {
                    sourceDirectoryPath = Paths.get(project.getBasedir().getAbsolutePath(), sourceDirectory);
                }
                return sourceDirectoryPath;
            }).filter(sourceDirectory -> Files.exists(sourceDirectory) && Files.isDirectory(sourceDirectory)).forEach(sourceDirectoryPath -> {
                DirectoryScanner scanner = getDirectoryScanner(sourceDirectoryPath.toFile());
                Arrays.stream(scanner.getIncludedFiles()).map(sourceDirectoryPath::resolve).forEach(
                        file -> {
                            try {
                                if (!Files.isDirectory(file)) {
                                    Path workingCopy = workDirectory.resolve(sourceDirectoryPath.relativize(file));
                                    Files.createDirectories(workingCopy.getParent());
                                    Files.copy(file, workingCopy, StandardCopyOption.REPLACE_EXISTING);
                                }
                            } catch (IOException e) {
                                logger.error("Cannot copy file into working directory.", e);
                            }
                        }
                );
            });
            Map<String, String> mappings = new HashMap<>(Constants.DEFAULT_EXTENSION_TO_SCRIPT_ENGINE_MAPPING);
            if (scriptEngineMappings != null) {
                mappings.putAll(scriptEngineMappings);
            }
            scriptEngineMappings = mappings;
            if (searchPaths == null || searchPaths.isEmpty()) {
                searchPaths = Constants.DEFAULT_SEARCH_PATHS;
            }
            DirectoryScanner scanner = getDirectoryScanner(workDirectory.toFile());
            List<String> scannerPaths = new ArrayList<>(Arrays.asList(scanner.getIncludedFiles()));
            for (String file : scanner.getIncludedFiles()) {
                scannerPaths.add(FilenameUtils.getFullPath(file));
            }
            capabilities = Capabilities.fromFileSystemTree(
                workDirectory,
                scannerPaths.stream().map(workDirectory::resolve),
                logger,
                searchPaths,
                scriptEngineMappings,
                missingRequirementsOptional
            );
            String providedCapabilitiesDefinition = capabilities.getProvidedCapabilitiesString();
            String requiredCapabilitiesDefinition = capabilities.getRequiredCapabilitiesString();
            project.getProperties().put("org.apache.sling.scriptingbundle.maven.plugin." + org.osgi.framework.Constants.PROVIDE_CAPABILITY,
                    providedCapabilitiesDefinition);
            project.getProperties().put("org.apache.sling.scriptingbundle.maven.plugin." + org.osgi.framework.Constants.REQUIRE_CAPABILITY,
                    requiredCapabilitiesDefinition);
        } catch (IOException e) {
            logger.error("Unable to generate working directory.", e);
        }
    }

    @NotNull
    private DirectoryScanner getDirectoryScanner(@NotNull File directory) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(directory);
        if (includes == null || includes.length == 0) {
            scanner.setIncludes("**");
        } else {
            scanner.setIncludes(includes);
        }
        if (excludes == null || excludes.length == 0) {
            scanner.setExcludes(Constants.DEFAULT_EXCLUDES.toArray(new String[0]));
        } else {
            scanner.setExcludes(excludes);
        }
        scanner.scan();
        return scanner;
    }

    Capabilities getCapabilities() {
        return capabilities;
    }

    Map<String, String> getScriptEngineMappings() {
        return scriptEngineMappings;
    }

}
