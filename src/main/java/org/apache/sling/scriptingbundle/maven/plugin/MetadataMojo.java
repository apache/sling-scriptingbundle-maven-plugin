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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
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
import org.apache.sling.scriptingbundle.maven.plugin.capability.Capabilities;
import org.apache.sling.scriptingbundle.maven.plugin.capability.ProvidedResourceTypeCapability;
import org.apache.sling.scriptingbundle.maven.plugin.capability.ProvidedScriptCapability;
import org.apache.sling.scriptingbundle.maven.plugin.capability.RequiredResourceTypeCapability;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

/**
 * The {@code metadata} goal will generate two Maven project properties, namely
 * {@code org.apache.sling.scriptingbundle.maven.plugin.Require-Capability} and
 * {@code org.apache.sling.scriptingbundle.maven.plugin.Provide-Capability} which can be used to generate the
 * corresponding OSGi bundle headers for bundles providing scripts executable by a {@link javax.script.ScriptEngine}.
 */
@Mojo(name = "metadata",
      defaultPhase = LifecyclePhase.PACKAGE)
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
     * <p>
     * The following list provides the default excluded files and patterns:
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
     * sure to use the script extension as the key and one of the Script Engine's name (obtained from
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
     */
    @Parameter(property = "scriptingbundle.searchPaths")
    private Set<String> searchPaths;

    static final Set<String> METHODS = new HashSet<>(Arrays.asList("TRACE", "OPTIONS", "GET", "HEAD", "POST", "PUT",
            "DELETE", "PATCH"));
    static final String EXTENDS_FILE = "extends";
    static final String REQUIRES_FILE = "requires";
    static final String CAPABILITY_NS = "sling.servlet";
    static final String CAPABILITY_RESOURCE_TYPE_AT = ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + ":List<String>";
    static final String CAPABILITY_SELECTORS_AT = ServletResolverConstants.SLING_SERVLET_SELECTORS + ":List<String>";
    static final String CAPABILITY_EXTENSIONS_AT = ServletResolverConstants.SLING_SERVLET_EXTENSIONS;
    static final String CAPABILITY_METHODS_AT = ServletResolverConstants.SLING_SERVLET_METHODS;
    static final String CAPABILITY_PATH_AT = ServletResolverConstants.SLING_SERVLET_PATHS;
    static final String CAPABILITY_VERSION_AT = "version:Version";
    static final String CAPABILITY_EXTENDS_AT = "extends";
    static final String CAPABILITY_SCRIPT_ENGINE_AT = "scriptEngine";
    static final String CAPABILITY_SCRIPT_EXTENSION_AT = "scriptExtension";
    static final Map<String, String> DEFAULT_EXTENSION_TO_SCRIPT_ENGINE_MAPPING;
    static final Set<String> DEFAULT_SEARCH_PATHS;
    static final Set<String> DEFAULT_SOURCE_DIRECTORIES;
    static final String[] DEFAULT_EXCLUDES = {
            // Miscellaneous typical temporary files
            "**/*~", "**/#*#", "**/.#*", "**/%*%", "**/._*",

            // CVS
            "**/CVS", "**/CVS/**", "**/.cvsignore",

            // Subversion
            "**/.svn", "**/.svn/**",

            // Bazaar
            "**/.bzr", "**/.bzr/**",

            // Mac
            "**/.DS_Store",

            // Mercurial
            "**/.hg", "**/.hg/**",

            // git
            "**/.git", "**/.git/**",
    };

    static {
        DEFAULT_EXTENSION_TO_SCRIPT_ENGINE_MAPPING = new HashMap<>();
        DEFAULT_EXTENSION_TO_SCRIPT_ENGINE_MAPPING.put("ftl", "freemarker");
        DEFAULT_EXTENSION_TO_SCRIPT_ENGINE_MAPPING.put("gst", "gstring");
        DEFAULT_EXTENSION_TO_SCRIPT_ENGINE_MAPPING.put("html", "htl");
        DEFAULT_EXTENSION_TO_SCRIPT_ENGINE_MAPPING.put("java", "java");
        DEFAULT_EXTENSION_TO_SCRIPT_ENGINE_MAPPING.put("esp", "rhino");
        DEFAULT_EXTENSION_TO_SCRIPT_ENGINE_MAPPING.put("ecma", "rhino");
        DEFAULT_EXTENSION_TO_SCRIPT_ENGINE_MAPPING.put("jsp", "jsp");
        DEFAULT_EXTENSION_TO_SCRIPT_ENGINE_MAPPING.put("jspf", "jsp");
        DEFAULT_EXTENSION_TO_SCRIPT_ENGINE_MAPPING.put("jspx", "jsp");
        /*
         * commented out since Thymeleaf uses the same 'html' extension like HTL
         */
//        DEFAULT_EXTENSION_TO_SCRIPT_ENGINE_MAPPING.put("html", "thymeleaf");

        DEFAULT_SEARCH_PATHS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("/libs", "/apps")));
        DEFAULT_SOURCE_DIRECTORIES = new HashSet<>();
        DEFAULT_SOURCE_DIRECTORIES.add(Paths.get("src", "main", "scripts").toString());
        DEFAULT_SOURCE_DIRECTORIES.add(Paths.get("src", "main", "resources", "javax.script").toString());
    }

    private Capabilities capabilities;

    public void execute() {
        capabilities = new Capabilities(new HashSet<>(), new HashSet<>(), new HashSet<>());
        if (sourceDirectories.isEmpty()) {
            sourceDirectories = new HashSet<>(DEFAULT_SOURCE_DIRECTORIES);
        }
        File workDirectory = new File(new File(project.getBuild().getDirectory()), "scriptingbundle-maven-plugin");
        if (workDirectory.exists() || workDirectory.mkdirs()) {
            sourceDirectories.stream().map(sourceDirectory -> {
                File sdFile = new File(sourceDirectory);
                if (!sdFile.exists()) {
                    sdFile = new File(project.getBasedir(), sourceDirectory);
                }
                return sdFile;
            }).filter(sourceDirectory -> sourceDirectory.exists() && sourceDirectory.isDirectory()).forEach(sourceDirectory -> {
                DirectoryScanner scanner = getDirectoryScanner(sourceDirectory);
                try {
                    for (String file : scanner.getIncludedFiles()) {
                        String fileName = FilenameUtils.getName(file);
                        if (EXTENDS_FILE.equals(fileName) || REQUIRES_FILE.equals(fileName)) {
                            Files.createDirectories(Paths.get(workDirectory.getAbsolutePath(), file).getParent());
                            Files.copy(Paths.get(sourceDirectory.getAbsolutePath(), file), Paths.get(workDirectory.getAbsolutePath(),
                                    file), StandardCopyOption.REPLACE_EXISTING);
                        } else {
                            Files.createDirectories(Paths.get(workDirectory.getAbsolutePath(), file).getParent());
                            new FileOutputStream(new File(workDirectory, file)).close();
                        }
                    }
                } catch (IOException e) {
                    getLog().error("Unable to analyse project files.", e);
                }
            });
        }
        Map<String, String> mappings = new HashMap<>(DEFAULT_EXTENSION_TO_SCRIPT_ENGINE_MAPPING);
        if (scriptEngineMappings != null) {
            mappings.putAll(scriptEngineMappings);
        }
        scriptEngineMappings = mappings;
        if (searchPaths == null || searchPaths.isEmpty()) {
            searchPaths = DEFAULT_SEARCH_PATHS;
        }
        DirectoryScanner scanner = getDirectoryScanner(workDirectory);
        capabilities = generateCapabilities(workDirectory.getAbsolutePath(), scanner);
        String providedCapabilitiesDefinition = getProvidedCapabilitiesString(capabilities);
        String requiredCapabilitiesDefinition = getRequiredCapabilitiesString(capabilities);
        String unresolvedRequiredCapabilitiesDefinition = getUnresolvedRequiredCapabilitiesString(capabilities);
        project.getProperties().put(this.getClass().getPackage().getName() + "." + Constants.PROVIDE_CAPABILITY,
                providedCapabilitiesDefinition);
        project.getProperties().put(this.getClass().getPackage().getName() + "." + Constants.REQUIRE_CAPABILITY,
                requiredCapabilitiesDefinition);
        project.getProperties().put(this.getClass().getPackage().getName() + "." + "Unresolved-" + Constants.REQUIRE_CAPABILITY,
                unresolvedRequiredCapabilitiesDefinition);
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
            scanner.setExcludes(DEFAULT_EXCLUDES);
        } else {
            scanner.setExcludes(excludes);
        }
        scanner.scan();
        return scanner;
    }

    @NotNull
    private Capabilities generateCapabilities(@NotNull String root, @NotNull DirectoryScanner scanner) {
        Set<ProvidedResourceTypeCapability> providedResourceTypeCapabilities = new LinkedHashSet<>();
        Set<ProvidedScriptCapability> providedScriptCapabilities = new LinkedHashSet<>();
        Set<RequiredResourceTypeCapability> requiredResourceTypeCapabilities = new LinkedHashSet<>();
        FileProcessor fileProcessor = new FileProcessor(getLog(), searchPaths, scriptEngineMappings);
        ResourceTypeFolderAnalyser resourceTypeFolderAnalyser = new ResourceTypeFolderAnalyser(getLog(), Paths.get(root), fileProcessor);
        PathOnlyScriptAnalyser pathOnlyScriptAnalyser = new PathOnlyScriptAnalyser(getLog(), Paths.get(root), scriptEngineMappings, fileProcessor);
        Set<String> includedDirectories = new HashSet<>();
        for (String file : scanner.getIncludedFiles()) {
            includedDirectories.add(FilenameUtils.getFullPath(file));
        }
        includedDirectories.stream().map(dir -> Paths.get(root, dir)).forEach(folder -> {
            Capabilities resourceTypeCapabilities = resourceTypeFolderAnalyser.getCapabilities(folder);
            providedResourceTypeCapabilities.addAll(resourceTypeCapabilities.getProvidedResourceTypeCapabilities());
            requiredResourceTypeCapabilities.addAll(resourceTypeCapabilities.getRequiredResourceTypeCapabilities());
        });
        Arrays.stream(scanner.getIncludedFiles()).map(file -> Paths.get(root, file)).forEach(file -> {
            Capabilities pathCapabilities = pathOnlyScriptAnalyser.getProvidedScriptCapability(file);
            providedScriptCapabilities.addAll(pathCapabilities.getProvidedScriptCapabilities());
            requiredResourceTypeCapabilities.addAll(pathCapabilities.getRequiredResourceTypeCapabilities());
        });

        return new Capabilities(providedResourceTypeCapabilities, providedScriptCapabilities, requiredResourceTypeCapabilities);
    }

    @NotNull
    private String getProvidedCapabilitiesString(Capabilities capabilities) {
        StringBuilder builder = new StringBuilder();
        int pcNum = capabilities.getProvidedResourceTypeCapabilities().size();
        int psNum = capabilities.getProvidedScriptCapabilities().size();
        int pcIndex = 0;
        int psIndex = 0;
        for (ProvidedResourceTypeCapability capability : capabilities.getProvidedResourceTypeCapabilities()) {
            builder.append(CAPABILITY_NS).append(";");
            processListAttribute(CAPABILITY_RESOURCE_TYPE_AT, builder,capability.getResourceTypes());
            Optional.ofNullable(capability.getScriptEngine()).ifPresent(scriptEngine ->
                    builder.append(";")
                            .append(CAPABILITY_SCRIPT_ENGINE_AT).append("=").append("\"").append(scriptEngine).append("\"")
            );
            Optional.ofNullable(capability.getScriptExtension()).ifPresent(scriptExtension ->
                    builder.append(";")
                            .append(CAPABILITY_SCRIPT_EXTENSION_AT).append("=").append("\"").append(scriptExtension).append("\"")
            );
            Optional.ofNullable(capability.getVersion()).ifPresent(version ->
                    builder.append(";")
                            .append(CAPABILITY_VERSION_AT).append("=").append("\"").append(version).append("\"")
            );
            Optional.ofNullable(capability.getExtendsResourceType()).ifPresent(extendedResourceType ->
                    builder.append(";")
                            .append(CAPABILITY_EXTENDS_AT).append("=").append("\"").append(extendedResourceType).append("\"")
            );
            Optional.ofNullable(capability.getRequestMethod()).ifPresent(method ->
                    builder.append(";")
                            .append(CAPABILITY_METHODS_AT).append("=").append("\"").append(method).append("\"")
            );
            Optional.ofNullable(capability.getRequestExtension()).ifPresent(requestExtension ->
                    builder.append(";")
                            .append(CAPABILITY_EXTENSIONS_AT).append("=").append("\"").append(requestExtension).append("\"")
            );
            if (!capability.getSelectors().isEmpty()) {
                builder.append(";");
                processListAttribute(CAPABILITY_SELECTORS_AT, builder, capability.getSelectors());
            }
            if (pcIndex < pcNum - 1) {
                builder.append(",");
            }
            pcIndex++;
        }
        if (builder.length() > 0 && psNum > 0) {
            builder.append(",");
        }
        for (ProvidedScriptCapability scriptCapability : capabilities.getProvidedScriptCapabilities()) {
            builder.append(CAPABILITY_NS).append(";").append(CAPABILITY_PATH_AT).append("=\"").append(scriptCapability.getPath()).append(
                    "\";").append(CAPABILITY_SCRIPT_ENGINE_AT).append("=").append(scriptCapability.getScriptEngine()).append(";")
                    .append(CAPABILITY_SCRIPT_EXTENSION_AT).append("=").append(scriptCapability.getScriptExtension());
            if (psIndex < psNum - 1) {
                builder.append(",");
            }
            psIndex++;
        }
        return builder.toString();
    }

    private void processListAttribute(@NotNull String capabilityAttribute, @NotNull StringBuilder builder, @NotNull Set<String> values) {
        builder.append(capabilityAttribute).append("=").append("\"");
        int valuesSize = values.size();
        int valueIndex = 0;
        for (String item : values) {
            builder.append(item);
            if (valueIndex < valuesSize - 1) {
                builder.append(",");
            }
            valueIndex++;
        }
        builder.append("\"");
    }

    @NotNull
    private String getRequiredCapabilitiesString(Capabilities capabilities) {
        StringBuilder builder = new StringBuilder();
        int pcNum = capabilities.getRequiredResourceTypeCapabilities().size();
        int pcIndex = 0;
        for (RequiredResourceTypeCapability capability : capabilities.getRequiredResourceTypeCapabilities()) {
            builder.append(CAPABILITY_NS).append(";filter:=\"").append("(&(!(" + ServletResolverConstants.SLING_SERVLET_SELECTORS + "=*))");
            VersionRange versionRange = capability.getVersionRange();
            if (versionRange != null) {
                builder.append("(&").append(versionRange.toFilterString("version")).append("(").append(ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES).append(
                        "=").append(capability.getResourceType()).append(")))\"");
            } else {
                builder.append("(").append(ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES).append("=").append(capability.getResourceType()).append("))\"");
            }
            if (pcIndex < pcNum - 1) {
                builder.append(",");
            }
            pcIndex++;
        }
        return builder.toString();
    }

    @NotNull
    private String getUnresolvedRequiredCapabilitiesString(Capabilities capabilities) {
        StringBuilder builder = new StringBuilder();
        int pcNum = capabilities.getUnresolvedRequiredResourceTypeCapabilities().size();
        int pcIndex = 0;
        for (RequiredResourceTypeCapability capability : capabilities.getUnresolvedRequiredResourceTypeCapabilities()) {
            builder.append(CAPABILITY_NS).append(";");
            builder.append(ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES).append("=").append(capability.getResourceType());
            VersionRange versionRange = capability.getVersionRange();
            if (versionRange != null) {
                Version left = versionRange.getLeft();
                if (versionRange.getLeftType() == VersionRange.LEFT_OPEN) {
                    left = new Version(left.getMajor(), left.getMinor(), left.getMicro() + 1);
                }
                builder.append(";").append(CAPABILITY_VERSION_AT).append("=").append(left);
            }
            if (pcIndex < pcNum - 1) {
                builder.append(",");
            }
            pcIndex++;
        }
        return builder.toString();
    }

    Capabilities getCapabilities() {
        return capabilities;
    }

    Map<String, String> getScriptEngineMappings() {
        return scriptEngineMappings;
    }

}
