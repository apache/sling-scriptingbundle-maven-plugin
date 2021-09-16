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

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

import org.apache.sling.scriptingbundle.plugin.processor.Slf4jLogger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class VaultContentXmlReaderTest {

    @Test
    public void testReader() throws IOException {
        VaultContentXmlReader vaultContentXmlReader = new VaultContentXmlReader(Paths.get("src/test/resources" +
                "/filevault-1/src/main/content/jcr_root/apps/my-scripts/image/.content.xml"));
        Optional<String> resourceSuperType = vaultContentXmlReader.getSlingResourceSuperType();
        assertTrue(resourceSuperType.isPresent());
        assertEquals("generic/image", resourceSuperType.get());

        assertTrue(
                vaultContentXmlReader.getSlingRequiredResourceTypes().size() == 2 &&
                        vaultContentXmlReader.getSlingRequiredResourceTypes().containsAll(Arrays.asList("required/one", "required/two"))
        );

    }

    @Test
    public void multipleValueResourceSuperType() throws IOException {
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> new VaultContentXmlReader(
                Paths.get("src/test/resources/filevault-docview-examples/multiple-resource-super-type.xml")));
        assertTrue(iae.getMessage().contains("Invalid sling:resourceSuperType property value ([generic/image])"));
    }

    @Test
    public void singleValueRequiredResourceTypes() throws IOException {
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> new VaultContentXmlReader(
                Paths.get("src/test/resources/filevault-docview-examples/single-value-required-resource-types.xml")));
        assertTrue(iae.getMessage().contains("Invalid sling:requiredResourceTypes property value (required/one,required/two)"));
    }

    @Test
    public void notADocView() throws IOException {
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> new VaultContentXmlReader(
                Paths.get("src/test/resources/filevault-docview-examples/not-a-docview.xml")));
        assertTrue(iae.getMessage().contains("does not seem to provide a Docview format - https://jackrabbit.apache.org/filevault/docview.html"));
    }
}
