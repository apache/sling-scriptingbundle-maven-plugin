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
package org.apache.sling.scriptingbundle.plugin.maven;

import java.io.IOException;
import java.nio.file.Path;

import com.google.inject.Inject;
import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.project.MavenProject;
import org.apache.sling.scriptingbundle.plugin.AbstractPluginTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.when;

@MojoTest
class MetadataMojoTest extends AbstractPluginTest {

    @Inject
    MavenProject mavenProject;

    @Test
    @Basedir("/project-1")
    @InjectMojo(goal = "metadata")
    void testProject1(MetadataMojo mojo) {
        mojo.execute();
        assertTestProject1(mojo.getCapabilities(), mojo.getScriptEngineMappings());
    }

    @Test
    @Basedir("/project-2")
    @InjectMojo(goal = "metadata")
    void testProject2(MetadataMojo mojo) {
        mojo.execute();
        assertTestProject2(mojo.getCapabilities(), mojo.getScriptEngineMappings());
    }

    @Test
    @Basedir("/project-3")
    @InjectMojo(goal = "metadata")
    void testProject3(MetadataMojo mojo) {
        mojo.execute();
        assertTestProject3(mojo.getCapabilities(), mojo.getScriptEngineMappings());
    }

    @Test
    @Basedir("/project-4")
    @InjectMojo(goal = "metadata")
    void testProject4(MetadataMojo mojo) {
        mojo.execute();
        assertTestProject4(mojo.getCapabilities(), mojo.getScriptEngineMappings());
    }

    @Test
    @Basedir("/filevault-1")
    @InjectMojo(goal = "metadata")
    void testFileVault1(MetadataMojo mojo) {
        when(mavenProject.getPackaging()).thenReturn("content-package");
        mojo.execute();
        assertTestFileVault1(mojo.getCapabilities(), mojo.getScriptEngineMappings());
    }

    @AfterEach
    void cleanUp() throws IOException {
        Path workDir = mavenProject.getBasedir().toPath().resolve("target");
        cleanUp(workDir);
    }
}
