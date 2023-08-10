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
import com.github.dockerjava.api.command.InspectImageCmd;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.ListImagesCmd;
import com.github.dockerjava.api.command.RemoveImageCmd;
import com.github.dockerjava.api.model.Image;

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
