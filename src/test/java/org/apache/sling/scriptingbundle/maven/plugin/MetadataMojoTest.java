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
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.SilentLog;
import org.apache.maven.project.MavenProject;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.sonatype.plexus.build.incremental.DefaultBuildContext;

import static org.junit.Assert.assertTrue;

public class MetadataMojoTest {

    @Rule
    public MojoRule mojoRule = new MojoRule();

    @After
    public void after() {
        System.clearProperty("basedir");
    }

    @Test
    public void testProject1() throws Exception {
        MojoProject mojoProject = getMojoProject(getProjectLocation("project-1"));
        mojoProject.mojo.execute();
        Properties properties = mojoProject.project.getProperties();
        assertTrue(properties.containsKey(MetadataMojo.class.getPackage() + "." + Constants.PROVIDE_CAPABILITY));
    }

    private MojoProject getMojoProject(File projectDirectory) throws Exception {
        SilentLog log = new SilentLog();
        DefaultBuildContext buildContext = new DefaultBuildContext();
        buildContext.enableLogging(log);
        MavenProject project = mojoRule.readMavenProject(projectDirectory);
        MavenSession session = mojoRule.newMavenSession(project);
        MojoExecution execution = mojoRule.newMojoExecution("metadata");
        MetadataMojo validateMojo = (MetadataMojo) mojoRule.lookupConfiguredMojo(session, execution);
        validateMojo.setLog(log);
        MojoProject mojoProject = new MojoProject();
        mojoProject.mojo = validateMojo;
        mojoProject.project = project;
        return mojoProject;
    }

    private class MojoProject {
        MetadataMojo mojo;
        MavenProject project;
    }

    private File getProjectLocation(String projectName) {
        return Paths.get("src", "test", "resources", projectName).toFile();
    }
}
