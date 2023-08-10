/*
 * Copyright (c) 2017-2023 Couchbase, Inc.
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
