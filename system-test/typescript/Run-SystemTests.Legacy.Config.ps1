# System Test Configuration (Legacy Modules)
# This file contains legacy suite configuration values for Run-SystemTests.ps1

$Config = @{

    Suites = @(

        # === mod02: Smoke ===
        @{  Id = "mod02-smoke";
            Name = "mod02 - Smoke (real)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'real'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=smoke-test tests/legacy/mod02/smoke";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },

        # === mod03: E2E ===
        @{  Id = "mod03-e2e";
            Name = "mod03 - E2E";
            Command = "`$env:ENVIRONMENT = 'local'; npx playwright test --project=e2e-test tests/legacy/mod03/e2e";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },

        # === mod04: Smoke + E2E ===
        @{  Id = "mod04-smoke";
            Name = "mod04 - Smoke (real)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'real'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=smoke-test tests/legacy/mod04/smoke";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },
        @{  Id = "mod04-e2e";
            Name = "mod04 - E2E";
            Command = "`$env:ENVIRONMENT = 'local'; npx playwright test --project=e2e-test tests/legacy/mod04/e2e";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },

        # === mod05: Smoke + E2E ===
        @{  Id = "mod05-smoke";
            Name = "mod05 - Smoke (real)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'real'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=smoke-test tests/legacy/mod05/smoke";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },
        @{  Id = "mod05-e2e";
            Name = "mod05 - E2E";
            Command = "`$env:ENVIRONMENT = 'local'; npx playwright test --project=e2e-test tests/legacy/mod05/e2e";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },

        # === mod06: Smoke + E2E ===
        @{  Id = "mod06-smoke";
            Name = "mod06 - Smoke (real)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'real'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=smoke-test tests/legacy/mod06/smoke";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },
        @{  Id = "mod06-e2e";
            Name = "mod06 - E2E";
            Command = "`$env:ENVIRONMENT = 'local'; npx playwright test --project=e2e-test tests/legacy/mod06/e2e";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },

        # === mod07: Smoke + E2E ===
        @{  Id = "mod07-smoke";
            Name = "mod07 - Smoke (real)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'real'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=smoke-test tests/legacy/mod07/smoke";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },
        @{  Id = "mod07-e2e";
            Name = "mod07 - E2E";
            Command = "`$env:ENVIRONMENT = 'local'; npx playwright test --project=e2e-test tests/legacy/mod07/e2e";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },

        # === mod08: Smoke + E2E ===
        @{  Id = "mod08-smoke";
            Name = "mod08 - Smoke (real)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'real'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=smoke-test tests/legacy/mod08/smoke";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },
        @{  Id = "mod08-e2e";
            Name = "mod08 - E2E";
            Command = "`$env:ENVIRONMENT = 'local'; npx playwright test --project=e2e-test tests/legacy/mod08/e2e";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },

        # === mod09: Smoke (stub + real) ===
        @{  Id = "mod09-smoke-stub";
            Name = "mod09 - Smoke (stub)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'stub'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=smoke-test tests/legacy/mod09/smoke";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },
        @{  Id = "mod09-smoke-real";
            Name = "mod09 - Smoke (real)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'real'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=smoke-test tests/legacy/mod09/smoke";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },

        # === mod10: Acceptance ===
        @{  Id = "mod10-acceptance";
            Name = "mod10 - Acceptance (stub)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'stub'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=acceptance-test tests/legacy/mod10/acceptance";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },

        # === mod11: Contract + E2E ===
        @{  Id = "mod11-contract-stub";
            Name = "mod11 - Contract (stub)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'stub'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=external-system-contract-test tests/legacy/mod11/contract";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },
        @{  Id = "mod11-contract-real";
            Name = "mod11 - Contract (real)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'real'; `$env:ENVIRONMENT = 'local'; npx playwright test --project=external-system-contract-test tests/legacy/mod11/contract";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null },
        @{  Id = "mod11-e2e";
            Name = "mod11 - E2E";
            Command = "`$env:ENVIRONMENT = 'local'; npx playwright test --project=e2e-test tests/legacy/mod11/e2e";
            Path = ".";
            TestReportPath = "playwright-report\index.html";
            TestInstallCommands = $null }

    )
}

# Export the configuration
return $Config
