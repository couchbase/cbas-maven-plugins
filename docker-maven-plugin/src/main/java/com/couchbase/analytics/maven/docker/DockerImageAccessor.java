/*
 * Copyright 2017-2018 Couchbase, Inc.
 */
package com.couchbase.analytics.maven.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageCmd;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.ListImagesCmd;
import com.github.dockerjava.api.command.RemoveImageCmd;
import com.github.dockerjava.api.model.Image;
import org.apache.maven.plugin.logging.Log;

import java.time.chrono.ChronoZonedDateTime;
import java.util.Map;

class DockerImageAccessor extends DockerObjectAccessor<Image, InspectImageResponse> {

    DockerImageAccessor(DockerClient dockerClient, Log log) {
        super(dockerClient, log);
    }

    @Override
    ListImagesCmd createListObjectsCmd(String labelFilter) {
        return dockerClient.listImagesCmd().withLabelFilter(labelFilter);
    }

    @Override
    InspectImageCmd createInspectObjectCmd(String imageId) {
        return dockerClient.inspectImageCmd(imageId);
    }

    @Override
    RemoveImageCmd createRemoveObjectCmd(String imageId, boolean force) {
        return dockerClient.removeImageCmd(imageId).withForce(true);
    }

    @Override
    String getId(Image image) {
        return image.getId();
    }

    @Override
    Map<String, String> getLabels(Image image) {
        // not supported by the API
        return null;
    }

    @Override
    ChronoZonedDateTime getCreatedTimestamp(InspectImageResponse imageInfo) {
        return parseDateTime(imageInfo.getCreated());
    }

    @Override
    String getObjectKind() {
        return "image";
    }
}
