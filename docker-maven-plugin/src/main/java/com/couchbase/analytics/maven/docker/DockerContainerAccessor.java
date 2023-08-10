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
import java.util.Collections;
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
        return dockerClient.listContainersCmd().withLabelFilter(Collections.singletonList(labelFilter));
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
