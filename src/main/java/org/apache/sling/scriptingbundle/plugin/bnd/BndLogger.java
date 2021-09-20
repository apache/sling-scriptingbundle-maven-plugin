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

import org.apache.sling.scriptingbundle.plugin.processor.Logger;
import org.jetbrains.annotations.NotNull;

import aQute.lib.exceptions.Exceptions;
import aQute.service.reporter.Reporter;

public class BndLogger implements Logger {

    private final Reporter reporter;

    public BndLogger(Reporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public void error(@NotNull String message) {
        reporter.error(message);
    }

    @Override
    public void error(@NotNull String message, @NotNull Throwable t) {
        reporter.error(message, t);
    }

    @Override
    public void info(@NotNull String message) {
        // no BND equivalent
    }

    @Override
    public void warn(@NotNull String message) {
        reporter.warning(message);
    }

    @Override
    public void warn(@NotNull String message, @NotNull Throwable t) {
        String exception = Exceptions.causes(t);
        reporter.warning(message + ": " + exception);
    }

    @Override
    public void debug(@NotNull String message) {
        reporter.trace(message);
    }
}
