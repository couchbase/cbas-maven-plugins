/*
 * Copyright 2017 Couchbase, Inc.
 */
package com.couchbase.analytics.maven.docker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.ResponseItem;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.async.ResultCallbackTemplate;

@Mojo(name = "docker-build")
public class DockerBuildMojo extends AbstractMojo {

    private static final int BUILD_TIMEOUT_MINUTES = 120;

    private static final String IMAGE_ID = "image_id";
    private static final String IMAGE_TAG = "cbas-tester";
    private static final String IMAGE_VERSION = "1.0";
    private static final String TAG_SEPARATOR = ":";

    @Parameter(required = true)
    File contextFile;

    @Parameter(required = true)
    File outputMetadataFile;

    @Parameter
    boolean forceBuild;

    @Parameter
    boolean skipCache;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try (DockerClient dockerClient = DockerClientBuilder.getInstance().build()) {
            String imageId = null;
            DockerImageAccessor imageAccessor = new DockerImageAccessor(dockerClient, getLog());
            if (!forceBuild) {
                String labelFilter = String.format("%s=%s", DockerObjectAccessor.CBAS_DOCKER_LABEL_NAME,
                        DockerObjectAccessor.CBAS_DOCKER_LABEL_VALUE_FINAL);
                Image image = imageAccessor.findFirst(labelFilter, Comparator.comparing(Image::getCreated).reversed());
                if (image != null) {
                    imageId = image.getId();
                    getLog().warn("Skipping docker image build. Will use the latest one");
                }
            }
            if (imageId == null) {
                imageId = buildImage(dockerClient, imageAccessor);
            }
            getLog().info("Docker image is: " + imageId);
            saveImageMetadata(imageId);
        } catch (IOException | InterruptedException e) {
            throw new MojoFailureException("Error: " + e, e);
        } catch (DockerException | DockerClientException e) {
            throw new MojoFailureException("Docker error: " + e, e);
        }
    }

    private String buildImage(DockerClient dockerClient, DockerImageAccessor imageAccessor)
            throws IOException, InterruptedException {
        getLog().info(String.format("Building docker image from %s %s", contextFile, skipCache ? "(skipCache)" : ""));
        try (FileInputStream fis = new FileInputStream(contextFile)) {
            BuildImageCmd buildImageCmd = dockerClient.buildImageCmd(fis);
            buildImageCmd.withLabels(Collections.singletonMap(DockerObjectAccessor.CBAS_DOCKER_LABEL_NAME,
                    DockerObjectAccessor.CBAS_DOCKER_LABEL_VALUE_FINAL));
            buildImageCmd.withTags(Collections.singleton(IMAGE_TAG + TAG_SEPARATOR + IMAGE_VERSION));
            buildImageCmd.withNoCache(skipCache);
            DockerBuildCallback buildCallback = new DockerBuildCallback();
            buildImageCmd.exec(buildCallback);
            buildCallback.awaitCompletion(BUILD_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            return buildCallback.fetchImageId(imageAccessor);
        }
    }

    private void saveImageMetadata(String imageId) throws IOException {
        getLog().info("Writing docker image metadata to " + outputMetadataFile);
        Properties props = new Properties();
        props.put(IMAGE_ID, imageId);
        outputMetadataFile.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(outputMetadataFile)) {
            props.store(fos, null);
        }
    }

    private final class DockerBuildCallback extends ResultCallbackTemplate<DockerBuildCallback, BuildResponseItem> {

        List<String> errors = new ArrayList<>();

        String imageId;

        @Override
        public void onNext(BuildResponseItem item) {
            String status = coalesce(item.getStatus(), "");
            String stream = coalesce(item.getStream(), "");
            String progress = coalesce(item.getProgress(), "");
            if (!status.isEmpty() || !stream.isEmpty() || !progress.isEmpty()) {
                getLog().info(String.format("%s %s %s", status, stream, progress));
            }

            if (item.isErrorIndicated()) {
                String error = getError(item);
                errors.add(error);
                getLog().error(error);
            }

            if (item.isBuildSuccessIndicated()) {
                imageId = item.getImageId();
            }
        }

        private String getError(BuildResponseItem item) {
            if (!item.isErrorIndicated()) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            String error = item.getError();
            if (error != null) {
                sb.append(error).append("; ");
            }
            ResponseItem.ErrorDetail errorDetail = item.getErrorDetail();
            if (errorDetail != null) {
                sb.append('(').append(errorDetail.getCode()).append(") ").append(errorDetail.getMessage());
            }
            return sb.toString();
        }

        private String fetchImageId(DockerImageAccessor imageAccessor) throws DockerClientException {
            if (!errors.isEmpty()) {
                throw new DockerClientException("Docker build error: " + errors);
            }

            String imageIdCandidate = imageId;
            if (imageIdCandidate != null) {
                getLog().info("Docker image candidate: " + imageIdCandidate);
                try {
                    String resId = imageAccessor.createInspectObjectCmd(imageIdCandidate).exec().getId();
                    if (resId != null) {
                        return resId;
                    }
                } catch (NotFoundException e) {
                    getLog().info("Skipping docker image candidate (not found): " + imageIdCandidate);
                }
            }

            throw new DockerClientException("Docket build error");
        }

        @Override
        public void onError(Throwable t) {
            super.onError(t);
            getLog().error("Docker build error: " + t, t);
            errors.add(t.toString());
        }

        private String coalesce(String v1, String v2) {
            return v1 != null ? v1 : v2;
        }
    }

}
