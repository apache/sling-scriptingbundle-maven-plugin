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

import org.apache.maven.plugin.logging.Log;
import org.apache.sling.scriptingbundle.plugin.processor.Logger;
import org.jetbrains.annotations.NotNull;

public class MavenLogger implements Logger {

    private final Log log;

    public MavenLogger(Log log) {
        this.log = log;
    }

    @Override
    public void error(@NotNull String message) {
        log.error(message);
    }

    @Override
    public void error(@NotNull String message, @NotNull Throwable t) {
        log.error(message, t);
    }

    @Override
    public void info(@NotNull String message) {
        log.info(message);
    }

    @Override
    public void warn(@NotNull String message) {
        log.warn(message);
    }

    @Override
    public void warn(@NotNull String message, Throwable t) {
        log.warn(message, t);
    }
}
