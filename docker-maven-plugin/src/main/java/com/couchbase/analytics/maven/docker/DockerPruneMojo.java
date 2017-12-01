/*
 * Copyright 2017 Couchbase, Inc.
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
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.exception.DockerException;
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
        } catch (DockerException | DockerClientException e) {
            getLog().warn("Docker error: " + e, e);
        }
    }
}
