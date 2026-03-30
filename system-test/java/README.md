# System Test (Java)

## Instructions

Open up the 'system-test' folder

```shell
cd system-test
```

Check that you have Powershell 7

```shell
$PSVersionTable.PSVersion
```

Start Docker Containers

```shell
docker compose up -d
```

Run All Tests

```shell
./gradlew test
```

Run Smoke Tests Only

```shell
./gradlew test --tests com.optivem.starter.systemtest.smoketests.*
```

Stop Docker Containers

```shell
docker compose down
```
