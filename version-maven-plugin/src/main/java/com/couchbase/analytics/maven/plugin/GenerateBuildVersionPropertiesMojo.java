/*
 * Copyright 2016-2018 Couchbase, Inc.
 */
package com.couchbase.analytics.maven.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Mojo(name = "generate-version-info")
public class GenerateBuildVersionPropertiesMojo extends BuildVersionMojo {

    @Parameter(required = true)
    String productVersion;

    @Parameter(required = true)
    File outputFile;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            ObjectNode jsonObject = getBuildVersionJson();
            jsonObject.putPOJO("build.date", String.valueOf(new Date())).put("build.version", productVersion);
            outputFile.getParentFile().mkdirs();
            try (Writer outWriter = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
                final ObjectMapper mapper =
                        new ObjectMapper().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
                outWriter.write(mapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(mapper.treeToValue(jsonObject, Object.class)));
            }
        } catch (Exception e) {
            getLog().warn("Ignoring unexpected exception: " + e, e);
        }
    }
}
