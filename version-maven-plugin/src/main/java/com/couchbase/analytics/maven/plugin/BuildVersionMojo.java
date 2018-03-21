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
import org.apache.maven.plugin.AbstractMojo;
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
public class BuildVersionMojo extends AbstractMojo {

    private static final String MANIFEST_FILE_NAME = "manifest.xml";
    private static final String BUILD_NUMBER_FIELD = "build.number";

    @Parameter(required = true)
    String productVersion;

    @Parameter()
    File inputFile;

    @Parameter()
    File baseDir;

    @Parameter(required = true)
    File outputFile;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode jsonObject = objectMapper.createObjectNode();
            jsonObject.putPOJO("build.date", String.valueOf(new Date())).put("build.version", productVersion);
            if (inputFile == null) {
                inputFile = getManifestFile();
            }
            final String missingBuildNumberMsg = "<local build on " + InetAddress.getLocalHost().getHostName() + ">";
            if (inputFile != null) {
                getLog().info("Populating build info from manifest at: " + inputFile.getAbsolutePath());
                JAXBContext ctx = JAXBContext.newInstance(Manifest.class);
                Manifest manifest = (Manifest) ctx.createUnmarshaller().unmarshal(inputFile);
                ArrayNode projectJSONs = jsonObject.putArray("projects");
                for (Project project : manifest.getProject()) {
                    ObjectNode projectJSON = projectJSONs.addObject().put("name", project.getName()).put("revision",
                            project.getRevision());
                    if (project.getUpstream() != null) {
                        projectJSON.put("upstream", project.getUpstream());
                    }
                    if ("build".equals(project.getName())) {
                        final Optional<Annotation> buildNumAnno = project.getAnnotation().stream()
                                .filter(annotation -> "BLD_NUM".equals(annotation.getName())).findFirst();
                        if (buildNumAnno.isPresent()) {
                            jsonObject.put(BUILD_NUMBER_FIELD, buildNumAnno.get().getValue());
                        } else {
                            jsonObject.put(BUILD_NUMBER_FIELD, missingBuildNumberMsg);
                        }
                    }
                }
            } else {
                jsonObject.put(BUILD_NUMBER_FIELD, missingBuildNumberMsg);
                ArrayNode projects = jsonObject.putArray("projects");
                if (!SystemUtils.IS_OS_WINDOWS) {
                    Process process = new ProcessBuilder("repo", "forall", "-r", ".*", "-c",
                            "echo $REPO_PROJECT:$REPO_LREV:$REPO_RREV").redirectErrorStream(true).start();
                    process.waitFor();
                    IOUtils.readLines(process.getInputStream(), StandardCharsets.UTF_8).stream()
                            .map(line -> StringUtils.split(line, ":"))
                            .forEach(pair -> projects.addPOJO(new ProjectRevision(pair[0], pair[1], pair[2])));
                }
            }
            outputFile.getParentFile().mkdirs();
            try (Writer outWriter = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
                outWriter.write(JSONUtil.convertNode(jsonObject));
            }
        } catch (Exception e) {
            throw new MojoFailureException("error", e);
        }
    }

    @JsonInclude(Include.NON_NULL)
    private static class ProjectRevision {
        @JsonProperty
        String name;

        @JsonProperty
        String revision;

        @JsonProperty
        String upstream;

        ProjectRevision(String name, String revision, String manifestRevision) {
            this.name = name;
            this.revision = revision;
            this.upstream = Objects.equals(revision, manifestRevision) ? null : manifestRevision;
        }
    }

    private File getManifestFile() {
        Path currentPath = baseDir != null ? baseDir.toPath() : Paths.get(".").toAbsolutePath();
        getLog().info("Looking for manifest file starting here: " + currentPath);
        File manifestFile = null;
        while (manifestFile == null) {
            manifestFile = findManifestFile(currentPath);
            currentPath = currentPath.getParent();
            if (currentPath == null) {
                break;
            }
        }
        getLog().info("Determined manifest file to be: " + manifestFile);
        return manifestFile;
    }

    private static File findManifestFile(Path dir) {
        File candidate = new File(dir.toFile(), MANIFEST_FILE_NAME);
        return candidate.exists() ? candidate : null;
    }
}
