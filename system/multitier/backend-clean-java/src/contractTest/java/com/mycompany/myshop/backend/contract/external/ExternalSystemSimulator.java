package com.mycompany.myshop.backend.contract.external;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * The real external-system simulator ({@code external-systems/simulators/mock-server.js}), supplied
 * to the {@code *RealParityContractTest} classes by Testcontainers.
 *
 * <p>One container serves all three externals — the simulator exposes ERP, tax and clock as path
 * prefixes off a single port — so it is started once for the whole {@code contractTest} run rather
 * than once per external. Follows the module's singleton-container convention (see
 * {@code BaseComponentTest}): started in a static initializer and never explicitly stopped, leaving
 * teardown to Testcontainers' reaper.
 *
 * <p>The random mapped port is the point: nothing binds the fixed {@code 9111} the compose stacks
 * use, so this can run in commit stage without colliding with a deployed stack, and no state
 * survives between runs.
 *
 * <p>Set {@code EXTERNAL_SIMULATOR_BASE_URL} to the simulator root (e.g. {@code
 * http://localhost:9111}) to run against an already-running instance instead — the path for the
 * compose-based local workflow:
 * {@code docker compose -f docker/java/multitier/docker-compose.local.real.yml up external-system-simulators}
 */
public final class ExternalSystemSimulator {

    private static final int PORT = 9000;

    /**
     * Built by the {@code externalSimulatorImage} Gradle task rather than Testcontainers'
     * {@code ImageFromDockerfile}: the simulator's Dockerfile uses BuildKit-only syntax
     * ({@code RUN --mount=type=cache}), and {@code ImageFromDockerfile} drives docker-java's classic
     * builder, which rejects it with "the --mount option requires BuildKit".
     */
    private static final DockerImageName IMAGE =
        DockerImageName.parse("myshop/external-system-simulators:contract-test");

    private static final String ROOT_URL = resolveRootUrl();

    private ExternalSystemSimulator() {
    }

    /**
     * Base URL for one external system's slice of the simulator — {@code baseUrl("/erp")},
     * {@code baseUrl("/tax")}, {@code baseUrl("/clock")}. The returned value is what the production
     * gateway's URL property gets set to, so the gateway appends its own {@code /api/...} path.
     */
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
