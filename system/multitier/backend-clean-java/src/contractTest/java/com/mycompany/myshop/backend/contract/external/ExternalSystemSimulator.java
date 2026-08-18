package com.mycompany.myshop.backend.contract.external;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public final class ExternalSystemSimulator {

    private static final int PORT = 9000;

    private static final DockerImageName IMAGE =
        DockerImageName.parse("myshop/external-system-simulators:contract-test");

    private static final String ROOT_URL = resolveRootUrl();

    private ExternalSystemSimulator() {
    }

    public static String baseUrl(String systemPath) {
        return ROOT_URL + systemPath;
    }

    private static String resolveRootUrl() {
        var override = System.getenv("EXTERNAL_SIMULATOR_BASE_URL");
        if (override != null && !override.isBlank()) {
            return override.replaceAll("/+$", "");
        }
        return start();
    }

    private static String start() {
        var simulator = new GenericContainer<>(IMAGE)
            .withExposedPorts(PORT)
            .waitingFor(Wait.forHttp("/erp/health").forPort(PORT).forStatusCode(200));

        try {
            simulator.start();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Could not start the external-system simulator from image "
                + IMAGE + ". Build it first with `./gradlew externalSimulatorImage` — the "
                + "`external-contract-real` suite in component-tests.yaml runs that task for you. To "
                + "run against an already-running simulator instead, set EXTERNAL_SIMULATOR_BASE_URL "
                + "to its root URL.", e);
        }

        return "http://" + simulator.getHost() + ":" + simulator.getMappedPort(PORT);
    }
}
