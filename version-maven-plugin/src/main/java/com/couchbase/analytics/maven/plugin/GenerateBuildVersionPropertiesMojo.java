/*
 * Copyright (c) 2016-2023 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.couchbase.analytics.maven.plugin;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Date;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Mojo(name = "generate-version-info")
public class GenerateBuildVersionPropertiesMojo extends BuildVersionMojo {

    @Parameter(required = true)
    File outputFile;

    @Override
    public void execute() {
        try {
            File manifestFile = ensureManifestFile();
            productVersionOnlyField = "build.version";
            productVersionField = null;
            ObjectNode jsonObject = getBuildVersionJson(manifestFile, inputFile == null);
            jsonObject.putPOJO("build.date", String.valueOf(new Date()));
            outputFile.getParentFile().mkdirs();
            try (Writer outWriter = new FileWriter(outputFile)) {
                final ObjectMapper mapper =
                        new ObjectMapper().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
                mapper.writer().writeValue(outWriter, mapper.treeToValue(jsonObject, Object.class));
            }
        } catch (Exception e) {
            getLog().warn("Ignoring unexpected exception: " + e, e);
        }
    }
}
