/*
 * Copyright 2016-2017 Couchbase, Inc.
 */
package com.couchbase.analytics.maven.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.xml.bind.JAXBContext;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.json.JSONObject;

import com.couchbase.analytics.maven.plugin.jaxb.Annotation;
import com.couchbase.analytics.maven.plugin.jaxb.Manifest;
import com.couchbase.analytics.maven.plugin.jaxb.Project;

@Mojo(name = "generate-version-info")
public class BuildVersionMojo extends AbstractMojo {

    private static final String MANIFEST_FILE_NAME = "manifest.xml";
    private static final String REPO_DIR_NAME = ".repo";
    private static final String BUILD_NUMBER_FIELD = "build.number";

    @Parameter(required = true)
    String productVersion;

    @Parameter()
    File inputFile;

    @Parameter(required = true)
    File outputFile;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            JSONObject jsonObject = new JSONObject().put("build.date", new Date()).put("build.version", productVersion);
            inputFile = getManifestFile();
            final String missingBuildNumberMsg = "<local build on " + InetAddress.getLocalHost().getHostName() + ">";
            if (inputFile != null) {
                getLog().info("Populating build info from manifest at: " + inputFile.getAbsolutePath());
                JAXBContext ctx = JAXBContext.newInstance(Manifest.class);
                Manifest manifest = (Manifest) ctx.createUnmarshaller().unmarshal(inputFile);
                Set<JSONObject> projectJSONs = new HashSet<>();
                for (Project project : manifest.getProject()) {
                    JSONObject projectJSON =
                            new JSONObject().put("name", project.getName()).put("revision", project.getRevision());
                    if (project.getUpstream() != null) {
                        projectJSON.put("upstream", project.getUpstream());
                    }
                    projectJSONs.add(projectJSON);
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
                jsonObject.put("projects", projectJSONs);
            } else {
                jsonObject.put(BUILD_NUMBER_FIELD, missingBuildNumberMsg);
            }
            outputFile.getParentFile().mkdirs();
            try (Writer outWriter = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
                outWriter.write(jsonObject.toString(4));
            }
        } catch (Exception e) {
            throw new MojoFailureException("error", e);
        }
    }

    private static File getManifestFile() throws IOException {
        Path currentPath = Paths.get("").toAbsolutePath();
        File manifestFile = null;
        while (manifestFile == null) {
            manifestFile = findManifestFile(currentPath);
            currentPath = currentPath.getParent();
            if (currentPath == null) {
                break;
            }
        }
        return manifestFile;
    }

    private static File findManifestFile(Path dir) throws IOException {
        try (DirectoryStream<Path> dirFiles = Files.newDirectoryStream(dir.toAbsolutePath())) {
            for (Path file : dirFiles) {
                if (file.endsWith(MANIFEST_FILE_NAME)) {
                    return file.toFile();
                }
                if (file.toFile().isDirectory() && file.endsWith(REPO_DIR_NAME)) {
                    // found .repo dir --> should find manifest inside it
                    return findManifestFile(file);
                }
            }
        }
        return null;
    }
}
