# System Test Configuration (Latest)
# This file contains latest suite configuration values for Run-SystemTests.ps1

$Config = @{

    Suites = @(

        # === Smoke Tests (stub) ===
        @{  Id = "smoke-stub";
            Name = "latest - Smoke (stub)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'stub'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=smoke-test tests/latest/smoke";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },

        # === Smoke Tests (real) ===
        @{  Id = "smoke-real";
            Name = "latest - Smoke (real)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'real'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=smoke-test tests/latest/smoke";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },

        # === Acceptance Tests (stub) ===
        @{  Id = "acceptance";
            Name = "latest - Acceptance (stub)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'stub'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=acceptance-test tests/latest/acceptance";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },

        # === Contract Tests (stub) ===
        @{  Id = "contract-stub";
            Name = "latest - Contract (stub)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'stub'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=external-system-contract-test tests/latest/contract";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },

        # === Contract Tests (real) ===
        @{  Id = "contract-real";
            Name = "latest - Contract (real)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'real'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=external-system-contract-test tests/latest/contract";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },

        # === E2E Tests (real) ===
        @{  Id = "e2e";
            Name = "latest - E2E (real)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'real'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=e2e-test tests/latest/e2e";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null }

    )
}

# Export the configuration
return $Config
