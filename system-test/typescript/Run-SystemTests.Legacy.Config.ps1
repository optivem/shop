# System Test Configuration (Legacy Modules)
# This file contains legacy suite configuration values for Run-SystemTests.ps1

$Config = @{

    Suites = @(

        # === mod02: Smoke ===
        @{  Id = "mod02-smoke";
            SampleTest = "shouldBeAbleToGoToShop";
            Name = "mod02 - Smoke (real)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'real'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=smoke-test tests/legacy/mod02/smoke";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },

        # === mod03: E2E ===
        @{  Id = "mod03-e2e";
            SampleTest = "shouldPlaceOrder";
            Name = "mod03 - E2E";
            Command = "`$env:ENVIRONMENT = 'local'; npx playwright test --project=e2e-test tests/legacy/mod03/e2e";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },

        # === mod04: Smoke + E2E ===
        @{  Id = "mod04-smoke";
            SampleTest = "shouldBeAbleToGoToShop";
            Name = "mod04 - Smoke (real)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'real'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=smoke-test tests/legacy/mod04/smoke";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },
        @{  Id = "mod04-e2e";
            SampleTest = "shouldPlaceOrder";
            Name = "mod04 - E2E";
            Command = "`$env:ENVIRONMENT = 'local'; npx playwright test --project=e2e-test tests/legacy/mod04/e2e";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },

        # === mod05: Smoke + E2E ===
        @{  Id = "mod05-smoke";
            SampleTest = "shouldBeAbleToGoToShop";
            Name = "mod05 - Smoke (real)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'real'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=smoke-test tests/legacy/mod05/smoke";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },
        @{  Id = "mod05-e2e";
            SampleTest = "shouldPlaceOrder";
            Name = "mod05 - E2E";
            Command = "`$env:ENVIRONMENT = 'local'; npx playwright test --project=e2e-test tests/legacy/mod05/e2e";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },

        # === mod06: Smoke + E2E ===
        @{  Id = "mod06-smoke";
            SampleTest = "shouldBeAbleToGoToShop";
            Name = "mod06 - Smoke (real)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'real'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=smoke-test tests/legacy/mod06/smoke";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },
        @{  Id = "mod06-e2e-api";
            SampleTest = "shouldPlaceOrder";
            Name = "mod06 - E2E - API";
            Command = "`$env:CHANNEL = 'api'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=e2e-test tests/legacy/mod06/e2e";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },
        @{  Id = "mod06-e2e-ui";
            SampleTest = "shouldPlaceOrder";
            Name = "mod06 - E2E - UI";
            Command = "`$env:CHANNEL = 'ui'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=e2e-test tests/legacy/mod06/e2e";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },

        # === mod07: Smoke + E2E ===
        @{  Id = "mod07-smoke";
            SampleTest = "shouldBeAbleToGoToShop";
            Name = "mod07 - Smoke (real)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'real'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=smoke-test tests/legacy/mod07/smoke";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },
        @{  Id = "mod07-e2e-api";
            SampleTest = "shouldPlaceOrder";
            Name = "mod07 - E2E - API";
            Command = "`$env:CHANNEL = 'api'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=e2e-test tests/legacy/mod07/e2e";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },
        @{  Id = "mod07-e2e-ui";
            SampleTest = "shouldPlaceOrder";
            Name = "mod07 - E2E - UI";
            Command = "`$env:CHANNEL = 'ui'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=e2e-test tests/legacy/mod07/e2e";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },

        # === mod08: Smoke + E2E ===
        @{  Id = "mod08-smoke";
            SampleTest = "shouldBeAbleToGoToShop";
            Name = "mod08 - Smoke (real)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'real'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=smoke-test tests/legacy/mod08/smoke";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },
        @{  Id = "mod08-e2e-api";
            SampleTest = "shouldPlaceOrder";
            Name = "mod08 - E2E - API";
            Command = "`$env:CHANNEL = 'api'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=e2e-test tests/legacy/mod08/e2e";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },
        @{  Id = "mod08-e2e-ui";
            SampleTest = "shouldPlaceOrder";
            Name = "mod08 - E2E - UI";
            Command = "`$env:CHANNEL = 'ui'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=e2e-test tests/legacy/mod08/e2e";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },

        # === mod09: Smoke (stub + real) ===
        @{  Id = "mod09-smoke-stub";
            SampleTest = "shouldBeAbleToGoToShop";
            Name = "mod09 - Smoke (stub)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'stub'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=smoke-test tests/legacy/mod09/smoke";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },
        @{  Id = "mod09-smoke-real";
            SampleTest = "shouldBeAbleToGoToShop";
            Name = "mod09 - Smoke (real)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'real'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=smoke-test tests/legacy/mod09/smoke";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },

        # === mod10: Acceptance Tests ===
        @{  Id = "mod10-acceptance-api";
            SampleTest = "orderNumberShouldStartWithORD";
            Name = "mod10 - Acceptance (stub) - API";
            Command = "`$env:CHANNEL = 'api'; `$env:EXTERNAL_SYSTEM_MODE = 'stub'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=acceptance-test --grep-invert '@isolated' tests/legacy/mod10/acceptance";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },
        @{  Id = "mod10-acceptance-ui";
            SampleTest = "orderNumberShouldStartWithORD";
            Name = "mod10 - Acceptance (stub) - UI";
            Command = "`$env:CHANNEL = 'ui'; `$env:EXTERNAL_SYSTEM_MODE = 'stub'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=acceptance-test --grep-invert '@isolated' tests/legacy/mod10/acceptance";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },
        @{  Id = "mod10-acceptance-isolated-api";
            SampleTest = "shouldRecordPlacementTimestamp";
            Name = "mod10 - Acceptance Isolated (stub) - API";
            Command = "`$env:CHANNEL = 'api'; `$env:EXTERNAL_SYSTEM_MODE = 'stub'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=acceptance-test --workers=1 --grep '@isolated' tests/legacy/mod10/acceptance";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },
        @{  Id = "mod10-acceptance-isolated-ui";
            SampleTest = "shouldRecordPlacementTimestamp";
            Name = "mod10 - Acceptance Isolated (stub) - UI";
            Command = "`$env:CHANNEL = 'ui'; `$env:EXTERNAL_SYSTEM_MODE = 'stub'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=acceptance-test --workers=1 --grep '@isolated' tests/legacy/mod10/acceptance";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },

        # === mod11: Contract + E2E ===
        @{  Id = "mod11-contract-stub";
            SampleTest = "shouldBeAbleToGetTime";
            Name = "mod11 - Contract (stub)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'stub'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=external-system-contract-test --workers=1 --grep-invert '@isolated' tests/legacy/mod11/contract";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },
        @{  Id = "mod11-contract-stub-isolated";
            SampleTest = "shouldBeAbleToGetTime";
            Name = "mod11 - Contract Isolated (stub)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'stub'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=external-system-contract-test --workers=1 --grep '@isolated' tests/legacy/mod11/contract";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },
        @{  Id = "mod11-contract-real";
            SampleTest = "shouldBeAbleToGetTime";
            Name = "mod11 - Contract (real)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'real'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=external-system-contract-test --workers=1 tests/legacy/mod11/contract";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },
        @{  Id = "mod11-e2e-api";
            SampleTest = "shouldPlaceOrder";
            Name = "mod11 - E2E - API";
            Command = "`$env:CHANNEL = 'api'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=e2e-test tests/legacy/mod11/e2e";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },
        @{  Id = "mod11-e2e-ui";
            SampleTest = "shouldPlaceOrder";
            Name = "mod11 - E2E - UI";
            Command = "`$env:CHANNEL = 'ui'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=e2e-test tests/legacy/mod11/e2e";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null }

    )
}

# Export the configuration
return $Config
