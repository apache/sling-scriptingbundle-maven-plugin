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
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.project.MavenProject;
import org.apache.sling.scriptingbundle.plugin.AbstractPluginTest;
import org.apache.sling.scriptingbundle.plugin.PluginExecution;
import org.junit.After;
import org.junit.Rule;

public class MetadataMojoTest extends AbstractPluginTest {

    @Rule
    public MojoRule mojoRule = new MojoRule();

    @After
    public void after() {
        System.clearProperty("basedir");
    }

    private MojoProject getMojoProject(String projectName) throws Exception {
        File projectDirectory = Paths.get("src", "test", "resources", projectName).toFile();
        MavenProject project = mojoRule.readMavenProject(projectDirectory);
        MavenSession session = mojoRule.newMavenSession(project);
        MojoExecution execution = mojoRule.newMojoExecution("metadata");
        MetadataMojo validateMojo = (MetadataMojo) mojoRule.lookupConfiguredMojo(session, execution);
        MojoProject mojoProject = new MojoProject();
        mojoProject.mojo = validateMojo;
        mojoProject.project = project;
        return mojoProject;
    }

    private static class MojoProject {
        MetadataMojo mojo;
        MavenProject project;
    }

    @Override
    public PluginExecution executePluginOnProject(String projectName) throws Exception {
        MojoProject mojoProject = getMojoProject(projectName);
        mojoProject.mojo.execute();
        return new PluginExecution(mojoProject.mojo.getCapabilities(), mojoProject.mojo.getScriptEngineMappings());
    }

    @Override
    public void cleanUp(String projectName) throws Exception {
        MojoProject mojoProject = getMojoProject(projectName);
        FileUtils.forceDeleteOnExit(new File(mojoProject.project.getBuild().getDirectory()));
    }
}
