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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

public class MimeTypeChecker {

    private static final String CORE = "core_mime.types";
    private static final String EXTENSIONS = "mime.types";
    private static final Map<String, String> EXTENSION_TO_TYPE_MAP = new HashMap<>();

    private MimeTypeChecker(){}

    static {
        try (
                InputStream coreIs = MimeTypeChecker.class.getClassLoader().getResourceAsStream(CORE);
                InputStream extensionsIs = MimeTypeChecker.class.getClassLoader().getResourceAsStream(EXTENSIONS)
                ) {
            if (coreIs != null && extensionsIs != null) {
                BufferedReader coreBr = new BufferedReader(new InputStreamReader(coreIs, StandardCharsets.ISO_8859_1));
                BufferedReader extensionsBr = new BufferedReader(new InputStreamReader(extensionsIs, StandardCharsets.ISO_8859_1));
                populateExtensionToTypeMap(coreBr);
                populateExtensionToTypeMap(extensionsBr);
            }
        } catch (IOException ignored) {

        }
    }


    static boolean hasMimeType(@NotNull String extension) {
        return EXTENSION_TO_TYPE_MAP.containsKey(extension);
    }

    private static void populateExtensionToTypeMap(BufferedReader reader) throws IOException {
        String line = null;
        while ((line = reader.readLine()) != null) {
            // ignore comment lines
            if (line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\s+");
            if (parts.length > 1) {
                String[] extensions = new String[parts.length - 1];
                System.arraycopy(parts, 1, extensions, 0, extensions.length);

                for (String extension : extensions) {
                    EXTENSION_TO_TYPE_MAP.put(extension, parts[0]);
                }
            }
        }
    }

}
