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

import org.jetbrains.annotations.NotNull;

/**
 * Generic interface for logging various messages.
 */
public interface Logger {

    /**
     * Log an error message.
     *
     * @param message the message
     */
    void error(@NotNull String message);

    /**
     * Log an error message, together with the {@link Throwable} that caused it.
     *
     * @param message the message
     * @param t       the throwable that caused this error message
     */
    void error(@NotNull String message, @NotNull Throwable t);

    /**
     * Log an info message.
     *
     * @param message the messae
     */
    void info(@NotNull String message);

    /**
     * Log a warning message.
     *
     * @param message the message
     */
    void warn(@NotNull String message);

    /**
     * Log a warning message, together with the {@link Throwable} that caused it.
     *
     * @param message the message
     * @param t       the throwable that caused this error message
     */
    void warn(@NotNull String message, @NotNull Throwable t);

}
