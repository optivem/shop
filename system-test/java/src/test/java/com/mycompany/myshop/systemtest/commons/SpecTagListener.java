package com.mycompany.myshop.systemtest.commons;

import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Records the {@code @Tag} values of every planned test method so the Gradle
 * {@code test} task can write them into the JUnit XML as {@code <property>}
 * entries (Gradle's own report writer omits tags). Gradle runs one launcher
 * execution per test class, so each forked test JVM appends to its own file
 * under the directory named by {@code spec.tags.dir}; the listener is
 * inactive when that property is unset.
 */
public class SpecTagListener implements TestExecutionListener {
    private static final String DIR_PROPERTY = "spec.tags.dir";

    @Override
    public void testPlanExecutionStarted(TestPlan plan) {
        var dir = System.getProperty(DIR_PROPERTY);
        if (dir == null) {
            return;
        }

        List<String> lines = new ArrayList<>();
        for (var root : plan.getRoots()) {
            for (var test : plan.getDescendants(root)) {
                // @TestTemplate invocations are created while running, so the
                // planned identifier for them is a container with a MethodSource.
                if (!test.getTags().isEmpty() && test.getSource().filter(MethodSource.class::isInstance).isPresent()) {
                    lines.add(line(test));
                }
            }
        }

        try {
            Files.createDirectories(Path.of(dir));
            Files.write(Path.of(dir, ProcessHandle.current().pid() + ".tsv"), lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write spec tags to " + dir, e);
        }
    }

    private static String line(TestIdentifier test) {
        var source = test.getSource().filter(MethodSource.class::isInstance).map(MethodSource.class::cast);
        if (source.isEmpty()) {
            return "";
        }
        var tags = test.getTags().stream().map(t -> t.getName()).sorted().collect(Collectors.joining(","));
        return source.get().getClassName() + "\t" + source.get().getMethodName() + "\t" + tags;
    }
}
