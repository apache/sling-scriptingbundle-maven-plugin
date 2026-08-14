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
package org.apache.sling.scriptingbundle.plugin.processor;

import org.jetbrains.annotations.NotNull;

public class Slf4jLogger implements Logger {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Slf4jLogger.class);

    @Override
    public void error(@NotNull String message) {
        logger.error(message);
    }

    @Override
    public void error(@NotNull String message, @NotNull Throwable t) {
        logger.error(message, t);
    }

    @Override
    public void info(@NotNull String message) {
        logger.info(message);
    }

    @Override
    public void warn(@NotNull String message) {
        logger.warn(message);
    }

    @Override
    public void warn(@NotNull String message, @NotNull Throwable t) {
        logger.warn(message, t);
    }

    @Override
    public void debug(@NotNull String message) {
        logger.debug(message);
    }
}
