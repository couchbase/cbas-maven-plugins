/*
 * Copyright 2022 Couchbase, Inc.
 */
package com.couchbase.analytics.maven.plugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "split-bom-list")
public class SplitBomListMojo extends AbstractMojo {

    @Parameter()
    File inputFile;

    @Parameter()
    File baseDir;

    @Parameter(defaultValue = "${project.build.directory}")
    private File projectBuildDir;

    @Parameter()
    private File bomListFile;

    @Parameter(defaultValue = "${project.build.directory}/boms")
    private File outputDir;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Map<Pair<String, String>, NavigableSet<String>> gavMap = new TreeMap<>();
        try {
            getLog().info("reading gavs from " + bomListFile + "...");
            List<String> inputList = FileUtils.readLines(bomListFile, StandardCharsets.UTF_8);
            inputList.forEach(gav -> {
                String[] split = gav.split(":");
                gavMap.computeIfAbsent(Pair.of(split[0], split[1]), ga -> new TreeSet<>()).add(split[2]);
            });
            getLog().info(
                    "read " + gavMap.size() + " unique groupId:artifactId pairs from " + inputList.size() + " gavs...");
            outputDir.mkdirs();
            // ok, now we have sorted the master bom by g:a; now let's write out multiple unique boms
            MutableBoolean keepGoing = new MutableBoolean(true);
            for (int counter = 0; keepGoing.booleanValue(); counter++) {
                File outputFile = new File(outputDir, "bom" + counter + ".txt");
                try (FileWriter out = new FileWriter(outputFile)) {
                    for (Map.Entry<Pair<String, String>, NavigableSet<String>> entry : gavMap.entrySet()) {
                        out.write(entry.getKey().getLeft() + ":" + entry.getKey().getRight() + ":"
                                + entry.getValue().first() + "\n");
                    }
                }
                getLog().info("wrote " + outputFile);
                keepGoing.setFalse();
                gavMap.forEach((key, value) -> {
                    if (value.size() > 1) {
                        value.pollFirst();
                        keepGoing.setTrue();
                    }
                });
            }
        } catch (IOException e) {
            throw new MojoExecutionException(e.toString(), e);
        }
    }
}
