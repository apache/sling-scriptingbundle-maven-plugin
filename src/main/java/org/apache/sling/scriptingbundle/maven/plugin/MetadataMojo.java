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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.script.ScriptEngineFactory;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.io.DirectoryScanner;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Constants;
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
     * Defines where this goal will look for scripts in the project.
     *
     * @since 0.1.0
     */
    @Parameter(property = "scriptingbundle.sourceDirectory", defaultValue = "${project.build.scriptSourceDirectory}")
    private String sourceDirectory;

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
    static final String CAPABILITY_NS = "sling.resourceType";
    static final String CAPABILITY_RESOURCE_TYPE_AT = "sling.resourceType:List<String>";
    static final String CAPABILITY_SELECTORS_AT = CAPABILITY_NS + ".selectors:List<String>";
    static final String CAPABILITY_EXTENSIONS_AT = CAPABILITY_NS + ".extensions:List<String>";
    static final String CAPABILITY_METHODS_AT = "sling.servlet.methods:List<String>";
    static final String CAPABILITY_VERSION_AT = "version:Version";
    static final String CAPABILITY_EXTENDS_AT = "extends";
    static final String CAPABILITY_SCRIPT_ENGINE_AT = "scriptEngine";
    static final Map<String, String> DEFAULT_EXTENSION_TO_SCRIPT_ENGINE_MAPPING;
    static final Set<String> DEFAULT_SEARCH_PATHS;

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
    }

    private Capabilities capabilities;

    public void execute() throws MojoExecutionException {
        File sdFile = new File(sourceDirectory);
        if (!sdFile.exists()) {
            sdFile = new File(project.getBasedir(), sourceDirectory);
            if (!sdFile.exists()) {
                throw new MojoExecutionException("Cannot find file " + sourceDirectory + ".");
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
                        .filter(new ResourceTypeFolderPredicate(getLog())).collect(Collectors.toList());

        getLog().info("Detected resource type directories: " + resourceTypeDirectories);
        Map<String, String> mappings = new HashMap<>(DEFAULT_EXTENSION_TO_SCRIPT_ENGINE_MAPPING);
        if (scriptEngineMappings != null) {
            mappings.putAll(scriptEngineMappings);
        }
        scriptEngineMappings = mappings;
        Set<String> searchPathsSet;
        if (searchPaths == null || searchPaths.isEmpty()) {
            searchPaths = DEFAULT_SEARCH_PATHS;
        }
        capabilities = generateCapabilities(root, resourceTypeDirectories);
        String providedCapabilitiesDefinition = getProvidedCapabilitiesString(capabilities);
        String requiredCapabilitiesDefinition = getRequiredCapabilitiesString(capabilities);
        project.getProperties().put(this.getClass().getPackage().getName() + "." + Constants.PROVIDE_CAPABILITY,
                    providedCapabilitiesDefinition);
        project.getProperties().put(this.getClass().getPackage().getName() + "." + Constants.REQUIRE_CAPABILITY,
                    requiredCapabilitiesDefinition);
    }

    @NotNull
    private Capabilities generateCapabilities(@NotNull String root, @NotNull List<Path> resourceTypeDirectories) {
        ResourceTypeFolderAnalyser analyser = new ResourceTypeFolderAnalyser(getLog(), Paths.get(root), scriptEngineMappings,
                searchPaths);
        Set<ProvidedCapability> providedCapabilities = new LinkedHashSet<>();
        Set<RequiredCapability> requiredCapabilities = new LinkedHashSet<>();
        for (Path resourceTypeDirectory : resourceTypeDirectories) {
            Capabilities resourceTypeCapabilities = analyser.getCapabilities(resourceTypeDirectory);
            providedCapabilities.addAll(resourceTypeCapabilities.getProvidedCapabilities());
            requiredCapabilities.addAll(resourceTypeCapabilities.getRequiredCapabilities());
        }
        return new Capabilities(providedCapabilities, requiredCapabilities);
    }

    @NotNull
    private String getProvidedCapabilitiesString(Capabilities capabilities) {
        StringBuilder builder = new StringBuilder();
        int pcNum = capabilities.getProvidedCapabilities().size();
        int pcIndex = 0;
        for (ProvidedCapability capability : capabilities.getProvidedCapabilities()) {
            builder.append(CAPABILITY_NS).append(";");
            processListAttribute(CAPABILITY_RESOURCE_TYPE_AT, builder,capability.getResourceTypes());
            Optional.ofNullable(capability.getScriptEngine()).ifPresent(scriptEngine ->
                    builder.append(";")
                            .append(CAPABILITY_SCRIPT_ENGINE_AT).append("=").append("\"").append(scriptEngine).append("\"")
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
        int pcNum = capabilities.getRequiredCapabilities().size();
        int pcIndex = 0;
        for (RequiredCapability capability : capabilities.getRequiredCapabilities()) {
            builder.append(CAPABILITY_NS).append(";filter:=\"").append("(&(!(sling.resourceType.selectors=*))");
            VersionRange versionRange = capability.getVersionRange();
            if (versionRange != null) {
                builder.append("(&").append(versionRange.toFilterString("version")).append("(").append(CAPABILITY_NS).append("=").append(capability.getResourceType()).append("))");
            } else {
                builder.append("(").append(CAPABILITY_NS).append("=").append(capability.getResourceType()).append(")");
            }
            builder.append(")\"");
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

}
