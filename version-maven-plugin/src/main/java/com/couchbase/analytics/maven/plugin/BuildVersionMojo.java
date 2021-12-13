/*
 * Copyright 2016-2021 Couchbase, Inc.
 */
package com.couchbase.analytics.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.maven.plugin.AbstractMojo;
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

public abstract class BuildVersionMojo extends AbstractMojo {

    private static final String MANIFEST_FILE_NAME = "manifest.xml";
    private static final String BUILD_NUMBER_FIELD = "build.number";
    private static final String PRODUCT_VERSION_ONLY_FIELD = "product.version.only";
    private static final String PRODUCT_VERSION_FIELD = "product.version";
    private static final String DEFAULT_PRODUCT_VERSION_ONLY = "0.0.0";
    private static final String DEFAULT_BUILD_NUMBER = "##HOSTNAME##";
    private static final Map<String, File> manifestFiles = new HashMap<>();

    @Parameter()
    File inputFile;

    @Parameter()
    File baseDir;

    @Parameter(defaultValue = "${project.build.directory}")
    private File projectBuildDir;

    @Parameter(defaultValue = DEFAULT_PRODUCT_VERSION_ONLY)
    private String defaultProductVersionOnly;

    @Parameter(defaultValue = DEFAULT_BUILD_NUMBER)
    private String defaultBuildNumber;

    @Parameter(defaultValue = BUILD_NUMBER_FIELD)
    protected String buildNumberField;

    @Parameter(defaultValue = PRODUCT_VERSION_ONLY_FIELD)
    protected String productVersionOnlyField;

    @Parameter(defaultValue = PRODUCT_VERSION_FIELD)
    protected String productVersionField;

    protected ObjectNode getBuildVersionJson(File manifestFile, final boolean includeProjects)
            throws IOException, JAXBException {
        Manifest manifest =
                (Manifest) JAXBContext.newInstance(Manifest.class).createUnmarshaller().unmarshal(manifestFile);
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode jsonObject = objectMapper.createObjectNode();
        ArrayNode projectJSONs = includeProjects ? jsonObject.putArray("projects") : null;
        for (Project project : manifest.getProject()) {
            if (includeProjects) {
                projectJSONs
                        .addPOJO(new ProjectRevision(project.getName(), project.getRevision(), project.getUpstream()));
            }
            if ("build".equals(project.getName())) {
                String buildNumber = determineBuildNumber(project);
                String productVersionOnly = determineProjectVersionOnly(project);
                putFieldOptional(jsonObject, buildNumberField, buildNumber);
                putFieldOptional(jsonObject, productVersionOnlyField, productVersionOnly);
                putFieldOptional(jsonObject, productVersionField, productVersionOnly + "-" + buildNumber);
            }
        }
        return jsonObject;
    }

    private void putFieldOptional(ObjectNode jsonObject, String fieldName, String value) {
        if (fieldName != null) {
            jsonObject.put(fieldName, value);
        }
    }

    private String determineProjectVersionOnly(Project project) {
        return project.getAnnotation().stream().filter(annotation -> "VERSION".equals(annotation.getName())).findFirst()
                .map(Annotation::getValue).orElse(defaultProductVersionOnly);
    }

    private String determineBuildNumber(Project project) throws UnknownHostException {
        String buildNumber =
                project.getAnnotation().stream().filter(annotation -> "BLD_NUM".equals(annotation.getName()))
                        .findFirst().map(Annotation::getValue).orElse(defaultBuildNumber);
        if (DEFAULT_BUILD_NUMBER.equals(buildNumber)) {
            buildNumber = "<local build on " + InetAddress.getLocalHost().getHostName() + ">";
        }
        return buildNumber;
    }

    protected File ensureManifestFile() throws IOException, InterruptedException {
        File workingDir = baseDir != null ? baseDir : new File(".");
        String cacheKey = workingDir.getCanonicalFile().getAbsolutePath();
        File manifestFile = findManifestFile();
        if (manifestFile != null) {
            getLog().info("Populating build info from manifest at: " + manifestFile.getAbsolutePath());
        } else if (manifestFiles.containsKey(cacheKey)) {
            File cachedFile = manifestFiles.get(cacheKey);
            getLog().info("Populating build info from cached repo manifest -r output at " + cachedFile);
            return cachedFile;
        } else {
            manifestFile = File.createTempFile("manifest", ".xml", projectBuildDir);
            getLog().info("Populating build info from repo manifest -r output - will cache as " + manifestFile);
            projectBuildDir.mkdirs();
            manifestFile.deleteOnExit();
            Process process = new ProcessBuilder("repo", "manifest", "-r").directory(workingDir)
                    .redirectError(ProcessBuilder.Redirect.INHERIT).redirectOutput(manifestFile).start();
            process.waitFor();
            manifestFiles.put(cacheKey, manifestFile);
        }
        return manifestFile;
    }

    @JsonInclude(Include.NON_NULL)
    protected static class ProjectRevision {
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

        public String getName() {
            return name;
        }

        public String getRevision() {
            return revision;
        }

        public String getUpstream() {
            return upstream;
        }
    }

    protected File findManifestFile() {
        if (inputFile == null) {
            Path currentPath = baseDir != null ? baseDir.toPath() : Paths.get(".").toAbsolutePath();
            getLog().info("Looking for manifest file starting here: " + currentPath);
            while (inputFile == null) {
                File candidate = new File(currentPath.toFile(), MANIFEST_FILE_NAME);
                inputFile = candidate.exists() ? candidate : null;
                currentPath = currentPath.getParent();
                if (currentPath == null) {
                    break;
                }
            }
        }
        return inputFile;
    }

}
