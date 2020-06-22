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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.sling.scriptingbundle.maven.plugin.capability.Capabilities;
import org.apache.sling.scriptingbundle.maven.plugin.capability.ProvidedScriptCapability;
import org.apache.sling.scriptingbundle.maven.plugin.capability.RequiredResourceTypeCapability;
import org.jetbrains.annotations.NotNull;

public class PathOnlyScriptAnalyser {

    private final Path scriptsDirectory;
    private final Predicate<Path> isNotAResourceTypeFolder;
    private final Map<String, String> scriptEngineMappings;
    private final FileProcessor fileProcessor;

    public PathOnlyScriptAnalyser(@NotNull Log log, @NotNull Path scriptsDirectory, @NotNull Map<String, String> scriptEngineMappings,
                                  @NotNull FileProcessor fileProcessor) {
        this.scriptsDirectory = scriptsDirectory;
        this.isNotAResourceTypeFolder = new ResourceTypeFolderPredicate(log).negate();
        this.scriptEngineMappings = scriptEngineMappings;
        this.fileProcessor = fileProcessor;
    }

    public @NotNull Capabilities getProvidedScriptCapability(@NotNull Path file) {
        if (Files.isRegularFile(file) && file.startsWith(scriptsDirectory)) {
            String filePath = file.toString();
            String extension = FilenameUtils.getExtension(filePath);
            if (StringUtils.isNotEmpty(extension) && scriptEngineMappings.containsKey(extension)) {
                boolean useFile = true;
                Path parent = file.getParent();
                Path loopParent = parent;
                while (useFile && loopParent != null && !loopParent.equals(scriptsDirectory)) {
                    useFile = isNotAResourceTypeFolder.test(loopParent);
                    loopParent = loopParent.getParent();
                }
                if (parent != null && useFile) {
                    Path fileName = file.getFileName();
                    if (fileName != null) {
                        String name = fileName.toString();
                        int dotLastIndex = name.lastIndexOf('.');
                        if (dotLastIndex > -1 && dotLastIndex != name.length() - 1) {
                            String scriptPath = FilenameUtils.normalize("/" + scriptsDirectory.relativize(file).toString(), true);
                            ProvidedScriptCapability providedScriptCapability =
                                    ProvidedScriptCapability.builder(scriptEngineMappings).withPath(scriptPath).build();
                            Path requires = parent.resolve(MetadataMojo.REQUIRES_FILE);
                            Set<RequiredResourceTypeCapability> requiredCapabilities = new HashSet<>();
                            if (Files.exists(requires)) {
                                fileProcessor.processRequiresFile(requires, requiredCapabilities);
                            }
                            return new Capabilities(Collections.emptySet(),
                                    new HashSet<>(Arrays.asList(providedScriptCapability)),
                                    requiredCapabilities);
                        }
                    }
                }
            }
        }
        return Capabilities.EMPTY;
    }
}
