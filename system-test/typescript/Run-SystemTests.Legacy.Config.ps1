# System Test Configuration (Legacy)
# This file contains legacy suite configuration values for Run-SystemTests.ps1

$Config = @{

    Suites = @(

        # === Legacy Smoke Test ===
        @{  Id = "smoke";
            Name = "legacy - Smoke";
            Command = "npx jest test/smoke-tests --forceExit";
            Path = ".";
            TestReportPath = "test-results\jest-results.html";
            TestInstallCommands = @("npx playwright install chromium") }

    )
}

# Export the configuration
return $Config
