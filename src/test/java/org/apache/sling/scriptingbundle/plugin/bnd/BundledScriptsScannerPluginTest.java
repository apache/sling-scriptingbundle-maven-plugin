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

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Manifest;

import org.apache.commons.io.FileUtils;
import org.apache.sling.scriptingbundle.plugin.AbstractPluginTest;
import org.apache.sling.scriptingbundle.plugin.PluginExecution;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;

public class BundledScriptsScannerPluginTest extends AbstractPluginTest {

    public Builder prepareBuilder(String projectName) throws IOException {
        Path projectRootFolder = Paths.get("src", "test", "resources", projectName);
        Path projectTargetFolder = projectRootFolder.resolve("target");
        Path projectClassesFolder = projectTargetFolder.resolve("classes");
        Files.createDirectories(projectClassesFolder);
        Builder builder = new Builder();
        Jar jar = new Jar("test.jar", projectClassesFolder.toFile());
        jar.setManifest(new Manifest());
        builder.setJar(jar);
        File bndFile = projectRootFolder.resolve("bnd.bnd").toFile();
        builder.setProperties(bndFile.getParentFile(), builder.loadProperties(bndFile));
        builder.set(BundledScriptsScannerPlugin.PROJECT_ROOT_FOLDER, projectRootFolder.toString());
        builder.set(BundledScriptsScannerPlugin.PROJECT_BUILD_FOLDER, projectTargetFolder.toString());
        if (FILEVAULT_PROJECTS.contains(projectName)) {
            builder.set("project.packaging", "content-package");
        }
        return builder;
    }

    @Override
    public void cleanUp(String projectName) throws IOException {
        Path projectTargetFolder = Paths.get("src", "test", "resources", "bnd", projectName, "target");
        FileUtils.forceDeleteOnExit(projectTargetFolder.toFile());
    }

    @Override
    public PluginExecution executePluginOnProject(String projectName) throws Exception {
        try (Builder builder = prepareBuilder(projectName)) {
            BundledScriptsScannerPlugin plugin = builder.getPlugin(BundledScriptsScannerPlugin.class);
            assertNotNull(plugin);
            builder.build();
            return new PluginExecution(plugin.getCapabilities(), plugin.getScriptEngineMappings());
        }
    }

}
