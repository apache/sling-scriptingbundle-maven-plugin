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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;

import org.osgi.framework.Version;

public class ResourceTypeFolderPredicate implements Predicate<Path> {

    private final Logger logger;

    public ResourceTypeFolderPredicate(Logger logger) {
        this.logger = logger;
    }

    @Override
    public boolean test(Path folder) {
        if (folder == null) {
            return false;
        }
        Path lastSegment = folder.getFileName();
        if (lastSegment == null) {
            return false;
        }
        try {
            Version.parseVersion(lastSegment.toString());
            Path parent = folder.getParent();
            if (parent != null) {
                lastSegment = parent.getFileName();
            }
        } catch (IllegalArgumentException ignored) {
            // last segment does not denote a version
        }
        if (lastSegment != null) {
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(folder, Files::isRegularFile)) {
                for (Path path : directoryStream) {
                    Path fileName = path.getFileName();
                    if (fileName != null) {
                        String childName = fileName.toString();
                        Script script = Script.parseScript(childName);
                        if (Constants.EXTENDS_FILE.equals(childName) || script != null) {
                            return true;
                        }
                    }
                }
            } catch (IOException e) {
                logger.error(String.format("Could not check if folder %s denotes a resource type.", folder.toString()), e);
            }
        }
        return false;
    }
}
