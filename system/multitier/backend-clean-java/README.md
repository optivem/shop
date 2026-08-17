# Backend — Clean Architecture variant

An alternative implementation of the same backend as [`../backend-java`](../backend-java), structured
the clean-architecture way instead of the CRUD/layered way.

**Same HTTP contract. Same database schema. Same component tests.** Only `src/main` differs — and
that is the whole point: the acceptance-level specs do not change when the inside is rearranged.

## Status

`src/main` currently holds a verbatim copy of `backend-java`'s CRUD implementation. That is the
intended starting point, not the destination: the variant starts green so the clean-architecture
refactor can proceed with the component suite as its safety net.

## What this variant is not

- **Not scaffolded.** `gh optivem init` copies `system/multitier/backend-java/` and nothing else, so
  students never receive this directory. The CRUD implementation stays the one canonical template.
- **Not deployed and not system-tested.** No `VERSION`, no `Dockerfile`, no release tag, no image
  push, no docker-compose service, no entry in any `gh-optivem-*.yaml`. It has a commit stage only.
- **Not on SonarCloud.** No project exists for it; the `sonarqube` plugin and `sonar {}` block are
  removed rather than pointed at a key that would 404.

## Test layers

| Layer | Present | Why |
|---|---|---|
| `test` (unit) | yes | |
| `componentTest` | yes | Subject is the whole app over HTTP — architecture-independent |
| `contractTest` | no | Subject is the gateway adapter, which this variant rewrites |
| `integrationTest` | no | Subject is the adapter / repository / controller — same reason |

Contract and narrow-integration tests take an *adapter* as their subject, and adapters are precisely
what changes between architectures. They will be written per-variant once the design has settled.

## Shared vs duplicated

`src/testSupport` and `src/componentTest` are currently duplicated from `backend-java`. This is
deliberate and temporary — duplicating first turns "these files should port unchanged" from an
assumption into a measurement.

Once the clean-architecture refactor is green, diff both trees. If nearly everything came across
untouched, replace the copies with borrowed source roots in `build.gradle`:

```groovy
def canonical = '../backend-java/src'
sourceSets {
    testSupport {
        java.srcDirs = ["$canonical/testSupport/java", 'src/testSupport/java']
        java.exclude '**/testkit/driver/adapter/sut/**'
    }
    componentTest {
        java.srcDirs = ["$canonical/componentTest/java", 'src/componentTest/java']
        java.exclude 'com/mycompany/myshop/backend/BaseComponentTest.java'
    }
}
```

If instead a large fraction had to fork, the boundary-type design is what needs revisiting — not the
sharing mechanism.

## Instructions

```shell
./gradlew build                 # compile + unit tests
./gradlew componentTest         # in-process component tests (requires Docker)
./gradlew checkstyleAll         # lint every source set
```
