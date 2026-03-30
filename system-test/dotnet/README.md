# System Test (.NET)

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
dotnet test
```

Run Smoke Tests Only

```shell
dotnet test --filter "FullyQualifiedName~Optivem.Starter.SystemTest.SmokeTests"
```

Stop Docker Containers

```shell
docker compose down
```
