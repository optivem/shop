# System Test Configuration (Latest)
# This file contains latest suite configuration values for Run-SystemTests.ps1

$Config = @{

    Suites = @(

        # === Smoke Tests (stub) ===
        @{  Id = "smoke-stub";
            Name = "latest - Smoke (stub)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'stub'; `$env:ENVIRONMENT = 'local'; npx jest test/latest/smoke --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = @("npx playwright install chromium") },

        # === Smoke Tests (real) ===
        @{  Id = "smoke-real";
            Name = "latest - Smoke (real)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'real'; `$env:ENVIRONMENT = 'local'; npx jest test/latest/smoke --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = @("npx playwright install chromium") },

        # === Acceptance Tests (stub) - API ===
        @{  Id = "acceptance-api";
            Name = "latest - Acceptance (stub) - API";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'stub'; `$env:CHANNEL = 'API'; `$env:ENVIRONMENT = 'local'; npx jest test/latest/acceptance --testPathIgnorePatterns=isolated --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = $null },

        # === Acceptance Tests (stub) - UI ===
        @{  Id = "acceptance-ui";
            Name = "latest - Acceptance (stub) - UI";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'stub'; `$env:CHANNEL = 'UI'; `$env:CHANNEL_MODE = 'dynamic'; `$env:ENVIRONMENT = 'local'; npx jest test/latest/acceptance --testPathIgnorePatterns=isolated --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = @("npx playwright install chromium") },

        # === Acceptance Tests Isolated (stub) - API ===
        @{  Id = "acceptance-isolated-api";
            Name = "latest - Acceptance Isolated (stub) - API";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'stub'; `$env:CHANNEL = 'API'; `$env:ENVIRONMENT = 'local'; npx jest `"test/latest/acceptance/.*isolated`" --runInBand --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = $null },

        # === Acceptance Tests Isolated (stub) - UI ===
        @{  Id = "acceptance-isolated-ui";
            Name = "latest - Acceptance Isolated (stub) - UI";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'stub'; `$env:CHANNEL = 'UI'; `$env:ENVIRONMENT = 'local'; npx jest `"test/latest/acceptance/.*isolated`" --runInBand --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = @("npx playwright install chromium") },

        # === Contract Tests (stub) ===
        @{  Id = "contract-stub";
            Name = "latest - Contract (stub)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'stub'; `$env:ENVIRONMENT = 'local'; npx jest test/latest/contract --testPathIgnorePatterns=isolated --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = $null },

        # === Contract Tests Isolated (stub) ===
        @{  Id = "contract-stub-isolated";
            Name = "latest - Contract Isolated (stub)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'stub'; `$env:ENVIRONMENT = 'local'; npx jest `"test/latest/contract/.*isolated`" --runInBand --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = $null },

        # === Contract Tests (real) ===
        @{  Id = "contract-real";
            Name = "latest - Contract (real)";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'real'; `$env:ENVIRONMENT = 'local'; npx jest test/latest/contract --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = $null },

        # === E2E Tests (real) - API ===
        @{  Id = "e2e-api";
            Name = "latest - E2E (real) - API";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'real'; `$env:CHANNEL = 'API'; `$env:ENVIRONMENT = 'local'; npx jest test/latest/e2e --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = $null },

        # === E2E Tests (real) - UI ===
        @{  Id = "e2e-ui";
            Name = "latest - E2E (real) - UI";
            Command = "`$env:EXTERNAL_SYSTEM_MODE = 'real'; `$env:CHANNEL = 'UI'; `$env:ENVIRONMENT = 'local'; npx jest test/latest/e2e --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = @("npx playwright install chromium") }

    )
}

# Export the configuration
return $Config
