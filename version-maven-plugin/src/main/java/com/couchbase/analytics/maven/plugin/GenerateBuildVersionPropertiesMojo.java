/*
 * Copyright 2016-2018 Couchbase, Inc.
 */
package com.couchbase.analytics.maven.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

import javax.xml.bind.JAXBContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.hyracks.util.JSONUtil;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.couchbase.analytics.maven.plugin.jaxb.Annotation;
import com.couchbase.analytics.maven.plugin.jaxb.Manifest;
import com.couchbase.analytics.maven.plugin.jaxb.Project;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
                outWriter.write(JSONUtil.convertNode(jsonObject));
            }
        } catch (Exception e) {
            getLog().warn("Ignoring unexpected exception: " + e, e);
        }
    }
}
