/*
 * Copyright 2017-2018 Couchbase, Inc.
 */
package com.couchbase.analytics.maven.docker;

import java.time.chrono.ChronoZonedDateTime;
import java.util.Map;

import org.apache.maven.plugin.logging.Log;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectNetworkCmd;
import com.github.dockerjava.api.command.ListNetworksCmd;
import com.github.dockerjava.api.command.RemoveNetworkCmd;
import com.github.dockerjava.api.model.Network;

class DockerNetworkAccessor extends DockerObjectAccessor<Network, Network> {

    DockerNetworkAccessor(DockerClient dockerClient, Log log) {
        super(dockerClient, log);
    }

    @Override
    ListNetworksCmd createListObjectsCmd(String labelFilter) {
        return dockerClient.listNetworksCmd();
    }

    @Override
    InspectNetworkCmd createInspectObjectCmd(String networkId) {
        return dockerClient.inspectNetworkCmd().withNetworkId(networkId);
    }

    @Override
    RemoveNetworkCmd createRemoveObjectCmd(String networkId, boolean force) {
        return dockerClient.removeNetworkCmd(networkId);
    }

    @Override
    String getId(Network network) {
        return network.getId();
    }

    @Override
    Map<String, String> getLabels(Network network) {
        return network.getLabels();
    }

    @Override
    ChronoZonedDateTime getCreatedTimestamp(Network network) {
        Map<String, String> labels = network.getLabels();
        return labels != null ? parseDateTime(labels.get(CBAS_DOCKER_CREATED_LABEL_NAME)) : null;
    }

    @Override
    String getObjectKind() {
        return "network";
    }
}
