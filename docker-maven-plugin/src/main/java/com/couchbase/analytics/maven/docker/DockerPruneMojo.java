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

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;

@Mojo(name = "docker-prune")
public class DockerPruneMojo extends AbstractMojo {

    @Parameter(required = true)
    String ttl;

    @Parameter
    boolean skipPrune;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skipPrune) {
            getLog().info("Skipping docker prune (forced)");
            return;
        }

        Duration ttlDuration;
        try {
            ttlDuration = Duration.parse(ttl);
        } catch (DateTimeParseException e) {
            throw new MojoFailureException("Invalid ttl specified: " + ttl, e);
        }

        try {
            ZonedDateTime now = ZonedDateTime.now();
            ZonedDateTime pruneBefore = now.minus(ttlDuration);

            getLog().info(String.format("Removing docker objects created before: %s (now()-%s)",
                    DockerObjectAccessor.ISO_DATE_TIME_WITH_LOCAL_ZONE.format(pruneBefore), ttlDuration));

            DockerClient dockerClient = DockerClientBuilder.getInstance().build();

            List<DockerObjectAccessor<?, ?>> accessorList = new ArrayList<>();
            accessorList.add(new DockerContainerAccessor(dockerClient, getLog()));
            accessorList.add(new DockerNetworkAccessor(dockerClient, getLog()));
            accessorList.add(new DockerImageAccessor(dockerClient, getLog()));

            for (DockerObjectAccessor<?, ?> accessor : accessorList) {
                accessor.prune(pruneBefore, DockerObjectAccessor.CBAS_DOCKER_LABEL_NAME);
            }
        } catch (Exception e) {
            getLog().warn("Ignoring docker error: " + e, e);
        }
    }
}
