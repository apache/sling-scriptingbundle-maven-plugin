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
package org.apache.sling.scriptingbundle.plugin.processor.filevault;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.jackrabbit.vault.util.DocViewProperty;
import org.apache.sling.scriptingbundle.plugin.processor.Constants;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class VaultContentXmlReader {

    private static final DocumentBuilderFactory documentBuilderFactory;

    static {
        try {
            documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            documentBuilderFactory.setAttribute(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
            documentBuilderFactory.setExpandEntityReferences(false);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Cannot disable DTD features.", e);
        }
    }

    private final Path path;
    private final String resourceSuperType;
    private final Set<String> requiredResourceTypes;

    public VaultContentXmlReader(@NotNull Path path) throws IOException {
        this.path = path;
        this.requiredResourceTypes = new HashSet<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(new InputSource(reader));
            Element documentElement = document.getDocumentElement();
            if ("jcr:root".equals(documentElement.getTagName())) {
                this.resourceSuperType = document.getDocumentElement().getAttribute(Constants.SLING_RESOURCE_SUPER_TYPE);
                String requiredResourceTypesValue = document.getDocumentElement().getAttribute(Constants.SLING_REQUIRED_RESOURCE_TYPES);
                if (requiredResourceTypesValue != null) {
                    DocViewProperty requiredResourceTypesDocView = DocViewProperty.parse(Constants.SLING_REQUIRED_RESOURCE_TYPES,
                            requiredResourceTypesValue);
                    requiredResourceTypes.addAll(Arrays.asList(requiredResourceTypesDocView.values));
                }
            } else {
                throw new IllegalArgumentException(String.format("Path %s does not seem to be a valid Vault .content.xml file.", path));
            }
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException(e);
        }


    }

    @NotNull
    public Path getPath() {
        return path;
    }

    @NotNull
    public Optional<String> getSlingResourceSuperType() {
        return Optional.ofNullable(resourceSuperType);
    }

    @NotNull
    public Set<String> getSlingRequiredResourceTypes() {
        return Collections.unmodifiableSet(requiredResourceTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, resourceSuperType, requiredResourceTypes);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof VaultContentXmlReader) {
            VaultContentXmlReader other = (VaultContentXmlReader) obj;
            return Objects.equals(path, other.path) && Objects.equals(resourceSuperType, other.resourceSuperType) &&
                    Objects.equals(requiredResourceTypes, other.requiredResourceTypes);
         }
        return false;
    }
}
