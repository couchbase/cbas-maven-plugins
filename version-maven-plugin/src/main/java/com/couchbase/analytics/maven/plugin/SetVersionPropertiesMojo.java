/*
 * Copyright (c) 2016-2023 Couchbase, Inc.
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
