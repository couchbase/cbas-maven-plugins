/*
 * Copyright 2017 Couchbase, Inc.
 */
package com.couchbase.analytics.maven.docker;

import java.time.chrono.ChronoZonedDateTime;
import java.util.Map;

import org.apache.maven.plugin.logging.Log;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.model.Container;

class DockerContainerAccessor extends DockerObjectAccessor<Container, InspectContainerResponse> {

    DockerContainerAccessor(DockerClient dockerClient, Log log) {
        super(dockerClient, log);
    }

    @Override
    ListContainersCmd createListObjectsCmd(String labelFilter) {
        return dockerClient.listContainersCmd().withLabelFilter(labelFilter);
    }

    @Override
    InspectContainerCmd createInspectObjectCmd(String containerId) {
        return dockerClient.inspectContainerCmd(containerId);
    }

    @Override
    RemoveContainerCmd createRemoveObjectCmd(String containerId, boolean force) {
        return dockerClient.removeContainerCmd(containerId).withForce(force);
    }

    @Override
    String getId(Container container) {
        return container.getId();
    }

    @Override
    Map<String, String> getLabels(Container container) {
        return container.getLabels();
    }

    @Override
    ChronoZonedDateTime getCreatedTimestamp(InspectContainerResponse containerInfo) {
        return parseDateTime(containerInfo.getCreated());
    }

    @Override
    String getObjectKind() {
        return "container";
    }
}
