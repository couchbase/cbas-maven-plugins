/*
 * Copyright 2016-2021 Couchbase, Inc.
 */
package com.couchbase.analytics.maven.plugin;

import java.util.Iterator;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;

@Mojo(name = "set-version-properties", defaultPhase = LifecyclePhase.INITIALIZE)
public class SetVersionPropertiesMojo extends BuildVersionMojo {

    // inject the project
    @Parameter(defaultValue = "${project}")
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            ObjectNode node = getBuildVersionJson(ensureManifestFile(), true);
            ArrayNode projects = (ArrayNode) node.get("projects");
            Properties properties = project.getProperties();
            for (Iterator<JsonNode> iter = projects.elements(); iter.hasNext();) {
                ProjectRevision repoProject = (ProjectRevision) ((POJONode) iter.next()).getPojo();
                properties.setProperty("repo.revision." + repoProject.getName(), repoProject.getRevision());
            }
            properties.put(buildNumberField, node.get(buildNumberField).asText());
            properties.put(productVersionOnlyField, node.get(productVersionOnlyField).asText());
            properties.put(productVersionField, node.get(productVersionField).asText());
        } catch (Exception e) {
            getLog().warn("Ignoring unexpected exception: " + e, e);
        }
    }

}
