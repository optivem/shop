# System Test Configuration (Latest)
# This file contains latest suite configuration values for Run-SystemTests.ps1

$Config = @{

    Suites = @(

        # === Smoke Tests (stub) ===
        @{  Id = "smoke-stub";
            SampleTest = "shouldBeAbleToGoToShop";
            Name = "latest - Smoke (stub)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'stub'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=smoke-test tests/latest/smoke";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },

        # === Smoke Tests (real) ===
        @{  Id = "smoke-real";
            SampleTest = "shouldBeAbleToGoToShop";
            Name = "latest - Smoke (real)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'real'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=smoke-test tests/latest/smoke";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },

        # === Acceptance Tests (stub) - API ===
        @{  Id = "acceptance-api";
            SampleTest = "shouldBeAbleToBrowseCoupons";
            Name = "latest - Acceptance (stub) - API";
            Command = "`$env:CHANNEL = 'API'; `$env:EXTERNAL_SYSTEM_MODE = 'stub'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=acceptance-test --grep-invert '@isolated' tests/latest/acceptance";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },

        # === Acceptance Tests (stub) - UI ===
        @{  Id = "acceptance-ui";
            SampleTest = "shouldBeAbleToBrowseCoupons";
            Name = "latest - Acceptance (stub) - UI";
            Command = "`$env:CHANNEL = 'UI'; `$env:CHANNEL_MODE = 'static'; `$env:EXTERNAL_SYSTEM_MODE = 'stub'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=acceptance-test --grep-invert '@isolated' tests/latest/acceptance";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },

        # === Acceptance Tests Isolated (stub) - API ===
        @{  Id = "acceptance-isolated-api";
            SampleTest = "shouldBeAbleToCancelOrder";
            Name = "latest - Acceptance Isolated (stub) - API";
            Command = "`$env:CHANNEL = 'API'; `$env:EXTERNAL_SYSTEM_MODE = 'stub'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=acceptance-test --workers=1 --grep '@isolated' tests/latest/acceptance";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },

        # === Acceptance Tests Isolated (stub) - UI ===
        @{  Id = "acceptance-isolated-ui";
            SampleTest = "shouldBeAbleToCancelOrder";
            Name = "latest - Acceptance Isolated (stub) - UI";
            Command = "`$env:CHANNEL = 'UI'; `$env:CHANNEL_MODE = 'static'; `$env:EXTERNAL_SYSTEM_MODE = 'stub'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=acceptance-test --workers=1 --grep '@isolated' tests/latest/acceptance";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },

        # === Contract Tests (stub) ===
        @{  Id = "contract-stub";
            SampleTest = "shouldBeAbleToGetTime";
            Name = "latest - Contract (stub)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'stub'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=external-system-contract-test --workers=1 --grep-invert '@isolated' 'tests/latest/contract/.*-stub-.*\.spec\.ts'";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },

        # === Contract Tests Isolated (stub) ===
        @{  Id = "contract-isolated-stub";
            SampleTest = "shouldBeAbleToGetConfiguredTime";
            Name = "latest - Contract Isolated (stub)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'stub'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=external-system-contract-test --workers=1 --grep '@isolated' 'tests/latest/contract/.*-stub-.*\.spec\.ts'";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },

        # === Contract Tests (real) ===
        @{  Id = "contract-real";
            SampleTest = "shouldBeAbleToGetTime";
            Name = "latest - Contract (real)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'real'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=external-system-contract-test --workers=1 'tests/latest/contract/.*-real-.*\.spec\.ts'";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },

        # === E2E Tests (real) - API ===
        @{  Id = "e2e-api";
            SampleTest = "shouldPlaceOrder";
            Name = "latest - E2E (real) - API";
            Command = "`$env:CHANNEL = 'API'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=e2e-test tests/latest/e2e";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },

        # === E2E Tests (real) - UI ===
        @{  Id = "e2e-ui";
            SampleTest = "shouldPlaceOrder";
            Name = "latest - E2E (real) - UI";
            Command = "`$env:CHANNEL = 'UI'; `$env:CHANNEL_MODE = 'static'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=e2e-test tests/latest/e2e";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null }

    )
}

# Export the configuration
return $Config
