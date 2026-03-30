# System Test (TypeScript)

## Instructions

Open up the 'system-test' folder

```shell
cd system-test
```

Check that you have Powershell 7

```shell
$PSVersionTable.PSVersion
```

Check that you have Node.js 22 installed

```shell
node -v
```

Install dependencies

```shell
npm install
```

Install Playwright

```shell
npx playwright install
```

Start Docker Containers

```shell
docker compose up -d
```

Run All Tests

```shell
npm test
```

Run Smoke Tests Only

```shell
npm test -- test/smoke-tests
```

Stop Docker Containers

```shell
docker compose down
```
