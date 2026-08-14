/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.scriptingbundle.plugin.bnd;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Consumer;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;
import org.apache.sling.scriptingbundle.plugin.AbstractPluginTest;
import org.apache.sling.scriptingbundle.plugin.capability.Capabilities;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class BundledScriptsScannerPluginTest extends AbstractPluginTest {

    interface Verifications {
        void verify(Capabilities capabilities, Map<String, String> scriptEngineMappings);
    }

    static Stream<Arguments> projects() {
        return Stream.of(
                arguments("project-1", (Verifications) AbstractPluginTest::assertTestProject1, null),
                arguments("project-2", (Verifications) AbstractPluginTest::assertTestProject2, null),
                arguments("project-3", (Verifications) AbstractPluginTest::assertTestProject3, null),
                arguments("project-4", (Verifications) AbstractPluginTest::assertTestProject4, null),
                arguments("filevault-1", (Verifications) AbstractPluginTest::assertTestFileVault1, (Consumer<Builder>)
                        b -> b.set("project.packaging", "content-package")));
    }

    @ParameterizedTest
    @MethodSource("projects")
    void testProject(String projectName, Verifications verifications, Consumer<Builder> builderCallback)
            throws Exception {
        URL url = getClass().getClassLoader().getResource(projectName);
        Path projectRootFolder = Paths.get(requireNonNull(url).toURI());
        assertTrue(Files.exists(projectRootFolder));
        Path projectWorkFolder = projectRootFolder.resolve("target");
        try (Builder builder = prepareBuilder(projectRootFolder, projectWorkFolder)) {
            if (builderCallback != null) {
                builderCallback.accept(builder);
            }
            BundledScriptsScannerPlugin plugin = builder.getPlugin(BundledScriptsScannerPlugin.class);
            assertNotNull(plugin);
            builder.build();
            verifications.verify(plugin.getCapabilities(), plugin.getScriptEngineMappings());
        } finally {
            cleanUp(projectWorkFolder);
        }
    }

    public Builder prepareBuilder(Path projectRootFolder, Path projectTargetFolder) throws IOException {
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
        return builder;
    }
}
