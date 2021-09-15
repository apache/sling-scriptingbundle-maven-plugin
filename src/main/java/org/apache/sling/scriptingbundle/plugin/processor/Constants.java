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
package org.apache.sling.scriptingbundle.plugin.processor;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.servlets.ServletResolverConstants;

public final class Constants {

    private Constants() {}

    public static final Set<String> METHODS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("TRACE", "OPTIONS", "GET", "HEAD", "POST", "PUT",
            "DELETE", "PATCH")));
    public static final String EXTENDS_FILE = "extends";
    public static final String REQUIRES_FILE = "requires";
    public static final String CAPABILITY_NS = "sling.servlet";
    public static final String CAPABILITY_RESOURCE_TYPE_AT = ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES;
    public static final String CAPABILITY_SELECTORS_AT = ServletResolverConstants.SLING_SERVLET_SELECTORS;
    public static final String CAPABILITY_EXTENSIONS_AT = ServletResolverConstants.SLING_SERVLET_EXTENSIONS;
    public static final String CAPABILITY_METHODS_AT = ServletResolverConstants.SLING_SERVLET_METHODS;
    public static final String CAPABILITY_PATH_AT = ServletResolverConstants.SLING_SERVLET_PATHS;
    public static final String CAPABILITY_VERSION_AT = aQute.bnd.osgi.Constants.VERSION_ATTRIBUTE;
    public static final String CAPABILITY_EXTENDS_AT = "extends";
    public static final String CAPABILITY_SCRIPT_ENGINE_AT = "scriptEngine";
    public static final String CAPABILITY_SCRIPT_EXTENSION_AT = "scriptExtension";

    public static final String BND_SOURCE_DIRECTORIES = "sourceDirectories";
    public static final String BND_EXCLUDES = "excludes";
    public static final String BND_INCLUDES = "includes";
    public static final String BND_SCRIPT_ENGINE_MAPPINGS = "scriptEngineMappings";
    public static final String BND_SEARCH_PATHS = "searchPaths";
    public static final String BND_MISSING_REQUIREMENTS_OPTIONAL = "missingRequirementsOptional";

    public static final String VAULT_CONTEXT_XML = ".content.xml";
    public static final String SLING_RESOURCE_SUPER_TYPE = "sling:resourceSuperType";
    public static final String SLING_REQUIRED_RESOURCE_TYPES = "sling:requiredResourceTypes";

    public static final Map<String, String> DEFAULT_EXTENSION_TO_SCRIPT_ENGINE_MAPPING;
    public static final Set<String> DEFAULT_SEARCH_PATHS;
    public static final Set<String> DEFAULT_SOURCE_DIRECTORIES;
    public static final Set<String> DEFAULT_EXCLUDES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
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
        "**/.git", "**/.git/**"
    )));

    static {
        HashMap<String, String> scriptEngineMapping = new HashMap<>();
        scriptEngineMapping.put("ftl", "freemarker");
        scriptEngineMapping.put("gst", "gstring");
        scriptEngineMapping.put("html", "htl");
        scriptEngineMapping.put("java", "java");
        scriptEngineMapping.put("esp", "rhino");
        scriptEngineMapping.put("ecma", "rhino");
        scriptEngineMapping.put("jsp", "jsp");
        scriptEngineMapping.put("jspf", "jsp");
        scriptEngineMapping.put("jspx", "jsp");
        /**
         * commented out since Thymeleaf uses the same 'html' extension like HTL
         * scriptEngineMapping.put("html", "thymeleaf");
         */
        DEFAULT_EXTENSION_TO_SCRIPT_ENGINE_MAPPING = Collections.unmodifiableMap(scriptEngineMapping);

        DEFAULT_SEARCH_PATHS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("/libs", "/apps")));
        DEFAULT_SOURCE_DIRECTORIES =
                Collections.unmodifiableSet(new HashSet<>(Arrays.asList(Paths.get("src", "main", "scripts").toString(), Paths.get("src",
                        "main", "resources", "javax.script").toString())));
    }

}
