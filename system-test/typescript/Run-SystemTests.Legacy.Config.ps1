# System Test Configuration (Legacy Modules)
# This file contains legacy suite configuration values for Run-SystemTests.ps1

$Config = @{

    Suites = @(

        # === mod02: Smoke ===
        @{  Id = "mod02-smoke";
            Name = "mod02 - Smoke (real)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'real'; `$env:ENVIRONMENT = 'local'; npx jest test/legacy/mod02/smoke --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = @("npx playwright install chromium") },

        # === mod03: E2E ===
        @{  Id = "mod03-e2e";
            Name = "mod03 - E2E";
            Command = "`$env:ENVIRONMENT = 'local'; npx jest test/legacy/mod03/e2e --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = @("npx playwright install chromium") },

        # === mod04: Smoke + E2E ===
        @{  Id = "mod04-smoke";
            Name = "mod04 - Smoke (real)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'real'; `$env:ENVIRONMENT = 'local'; npx jest test/legacy/mod04/smoke --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = @("npx playwright install chromium") },
        @{  Id = "mod04-e2e";
            Name = "mod04 - E2E";
            Command = "`$env:ENVIRONMENT = 'local'; npx jest test/legacy/mod04/e2e --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = @("npx playwright install chromium") },

        # === mod05: Smoke + E2E ===
        @{  Id = "mod05-smoke";
            Name = "mod05 - Smoke (real)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'real'; `$env:ENVIRONMENT = 'local'; npx jest test/legacy/mod05/smoke --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = @("npx playwright install chromium") },
        @{  Id = "mod05-e2e";
            Name = "mod05 - E2E";
            Command = "`$env:ENVIRONMENT = 'local'; npx jest test/legacy/mod05/e2e --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = @("npx playwright install chromium") },

        # === mod06: Smoke + E2E ===
        @{  Id = "mod06-smoke";
            Name = "mod06 - Smoke (real)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'real'; `$env:ENVIRONMENT = 'local'; npx jest test/legacy/mod06/smoke --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = @("npx playwright install chromium") },
        @{  Id = "mod06-e2e-api";
            Name = "mod06 - E2E - API";
            Command = "`$env:CHANNEL = 'API'; `$env:ENVIRONMENT = 'local'; npx jest test/legacy/mod06/e2e --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = $null },
        @{  Id = "mod06-e2e-ui";
            Name = "mod06 - E2E - UI";
            Command = "`$env:CHANNEL = 'UI'; `$env:ENVIRONMENT = 'local'; npx jest test/legacy/mod06/e2e --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = @("npx playwright install chromium") },

        # === mod07: Smoke + E2E ===
        @{  Id = "mod07-smoke";
            Name = "mod07 - Smoke (real)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'real'; `$env:ENVIRONMENT = 'local'; npx jest test/legacy/mod07/smoke --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = @("npx playwright install chromium") },
        @{  Id = "mod07-e2e-api";
            Name = "mod07 - E2E - API";
            Command = "`$env:CHANNEL = 'API'; `$env:ENVIRONMENT = 'local'; npx jest test/legacy/mod07/e2e --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = $null },
        @{  Id = "mod07-e2e-ui";
            Name = "mod07 - E2E - UI";
            Command = "`$env:CHANNEL = 'UI'; `$env:ENVIRONMENT = 'local'; npx jest test/legacy/mod07/e2e --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = @("npx playwright install chromium") },

        # === mod08: Smoke + E2E ===
        @{  Id = "mod08-smoke";
            Name = "mod08 - Smoke (real)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'real'; `$env:ENVIRONMENT = 'local'; npx jest test/legacy/mod08/smoke --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = @("npx playwright install chromium") },
        @{  Id = "mod08-e2e-api";
            Name = "mod08 - E2E - API";
            Command = "`$env:CHANNEL = 'API'; `$env:ENVIRONMENT = 'local'; npx jest test/legacy/mod08/e2e --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = $null },
        @{  Id = "mod08-e2e-ui";
            Name = "mod08 - E2E - UI";
            Command = "`$env:CHANNEL = 'UI'; `$env:ENVIRONMENT = 'local'; npx jest test/legacy/mod08/e2e --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = @("npx playwright install chromium") },

        # === mod09: Smoke (stub + real) ===
        @{  Id = "mod09-smoke-stub";
            Name = "mod09 - Smoke (stub)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'stub'; `$env:ENVIRONMENT = 'local'; npx jest test/legacy/mod09/smoke --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = @("npx playwright install chromium") },
        @{  Id = "mod09-smoke-real";
            Name = "mod09 - Smoke (real)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'real'; `$env:ENVIRONMENT = 'local'; npx jest test/legacy/mod09/smoke --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = @("npx playwright install chromium") },

        # === mod10: Acceptance ===
        @{  Id = "mod10-acceptance-api";
            Name = "mod10 - Acceptance (stub) - API";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'stub'; `$env:CHANNEL = 'API'; `$env:ENVIRONMENT = 'local'; npx jest test/legacy/mod10/acceptance --testPathIgnorePatterns=isolated --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = $null },
        @{  Id = "mod10-acceptance-ui";
            Name = "mod10 - Acceptance (stub) - UI";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'stub'; `$env:CHANNEL = 'UI'; `$env:ENVIRONMENT = 'local'; npx jest test/legacy/mod10/acceptance --testPathIgnorePatterns=isolated --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = @("npx playwright install chromium") },
        @{  Id = "mod10-acceptance-isolated-api";
            Name = "mod10 - Acceptance Isolated (stub) - API";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'stub'; `$env:CHANNEL = 'API'; `$env:ENVIRONMENT = 'local'; npx jest `"test/legacy/mod10/acceptance/.*isolated`" --runInBand --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = $null },
        @{  Id = "mod10-acceptance-isolated-ui";
            Name = "mod10 - Acceptance Isolated (stub) - UI";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'stub'; `$env:CHANNEL = 'UI'; `$env:ENVIRONMENT = 'local'; npx jest `"test/legacy/mod10/acceptance/.*isolated`" --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = @("npx playwright install chromium") },

        # === mod11: Contract + E2E ===
        @{  Id = "mod11-contract-stub";
            Name = "mod11 - Contract (stub)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'stub'; `$env:ENVIRONMENT = 'local'; npx jest test/legacy/mod11/contract --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = $null },
        @{  Id = "mod11-contract-real";
            Name = "mod11 - Contract (real)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'real'; `$env:ENVIRONMENT = 'local'; npx jest test/legacy/mod11/contract --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = $null },
        @{  Id = "mod11-e2e-api";
            Name = "mod11 - E2E - API";
            Command = "`$env:CHANNEL = 'API'; `$env:ENVIRONMENT = 'local'; npx jest test/legacy/mod11/e2e --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = $null },
        @{  Id = "mod11-e2e-ui";
            Name = "mod11 - E2E - UI";
            Command = "`$env:CHANNEL = 'UI'; `$env:ENVIRONMENT = 'local'; npx jest test/legacy/mod11/e2e --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = @("npx playwright install chromium") }

    )
}

# Export the configuration
return $Config
