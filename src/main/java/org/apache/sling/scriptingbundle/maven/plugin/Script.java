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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class Script {

    private final String name;
    private final String requestExtension;
    private final String requestMethod;
    private final String scriptExtension;

    private Script(@Nullable String name, @Nullable String requestExtension, @Nullable String requestMethod,
                   @NotNull String scriptExtension) {
        this.name = name;
        this.requestExtension = requestExtension;
        this.requestMethod = requestMethod;
        this.scriptExtension = scriptExtension;
    }

    @Nullable
    String getName() {
        return name;
    }

    @Nullable
    String getRequestExtension() {
        return requestExtension;
    }

    @Nullable
    String getRequestMethod() {
        return requestMethod;
    }

    @NotNull
    String getScriptExtension() {
        return scriptExtension;
    }

    @Nullable
    static Script parseScript(@NotNull String fileName) {
        String[] parts = fileName.split("\\.");
        if (parts.length < 2 || parts.length > 4) {
            return null;
        }
        String name = parts[0];
        String scriptExtension = parts[parts.length - 1];
        String requestExtension = null;
        String requestMethod = null;
        if (parts.length == 2 && MetadataMojo.METHODS.contains(name)) {
            requestMethod = name;
            name = null;
        }
        if (parts.length == 3) {
            String middle = parts[1];
            if (MetadataMojo.METHODS.contains(middle)) {
                requestMethod = middle;
            } else {
                requestExtension = middle;
            }
        }
        if (parts.length == 4) {
            requestExtension = parts[1];
            requestMethod = parts[2];
        }
        return new Script(name, requestExtension, requestMethod, scriptExtension);
    }


}
