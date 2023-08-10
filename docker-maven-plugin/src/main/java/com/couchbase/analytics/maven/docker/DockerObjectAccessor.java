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

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.chrono.ChronoZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.logging.Log;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.SyncDockerCmd;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.exception.DockerException;

abstract class DockerObjectAccessor<TO, TOI> {

    static final String CBAS_DOCKER_LABEL_NAME = "cbas-docker-test";

    static final String CBAS_DOCKER_LABEL_VALUE_FINAL = "1";

    static final String CBAS_DOCKER_CREATED_LABEL_NAME = CBAS_DOCKER_LABEL_NAME + ".created";

    static final DateTimeFormatter ISO_DATE_TIME_WITH_LOCAL_ZONE =
            DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.systemDefault());

    final DockerClient dockerClient;

    final Log log;

    DockerObjectAccessor(DockerClient dockerClient, Log log) {
        this.dockerClient = dockerClient;
        this.log = log;
    }

    final void prune(ChronoZonedDateTime<?> pruneBefore, String labelFilter) {
        for (TO object : listObjects(labelFilter)) {
            Map<String, String> labels = getLabels(object);
            if (labels != null && !labels.containsKey(labelFilter)) {
                continue;
            }
            String objId = getId(object);
            TOI objInfo = inspectObject(objId);
            if (objInfo == null) {
                continue;
            }
            ChronoZonedDateTime<?> objCreatedTimestamp = getCreatedTimestamp(objInfo);
            if (objCreatedTimestamp == null) {
                getLog().warn(
                        String.format("Cannot obtain created timestamp for %s: %s. Skipping", getObjectKind(), objId));
                continue;
            }
            if (objCreatedTimestamp.isBefore(pruneBefore)) {
                removeObject(objId, objCreatedTimestamp);
            }
        }
    }

    final TO findFirst(String labelFilter, Comparator<TO> orderBy) {
        return listObjects(labelFilter).stream().sorted(orderBy).findFirst().orElse(null);
    }

    final Log getLog() {
        return log;
    }

    private List<TO> listObjects(String labelFilter) {
        return createListObjectsCmd(labelFilter).exec();
    }

    private TOI inspectObject(String objId) {
        try {
            return createInspectObjectCmd(objId).exec();
        } catch (DockerException | DockerClientException e) {
            getLog().warn(String.format("Failed to inspect %s %s", getObjectKind(), objId), e);
            return null;
        }
    }

    private void removeObject(String objId, ChronoZonedDateTime<?> objCreatedTimestamp) {
        String objCreatedLocalTimestamp = ISO_DATE_TIME_WITH_LOCAL_ZONE.format(objCreatedTimestamp);
        getLog().info(String.format("Removing %s %s created on %s", getObjectKind(), objId, objCreatedLocalTimestamp));
        try {
            createRemoveObjectCmd(objId, true).exec();
        } catch (DockerException | DockerClientException e) {
            getLog().warn(String.format("Failed to remove expired %s %s created on %s", getObjectKind(), objId,
                    objCreatedLocalTimestamp), e);
        }
    }

    final ChronoZonedDateTime<?> parseDateTime(String value) throws DateTimeException {
        return value != null ? ChronoZonedDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(value)) : null;
    }

    abstract SyncDockerCmd<List<TO>> createListObjectsCmd(String labelFilter);

    abstract SyncDockerCmd<TOI> createInspectObjectCmd(String objectId);

    abstract SyncDockerCmd<Void> createRemoveObjectCmd(String objectId, boolean force);

    abstract String getId(TO object);

    abstract Map<String, String> getLabels(TO object);

    abstract ChronoZonedDateTime getCreatedTimestamp(TOI objectInfo);

    abstract String getObjectKind();
}
