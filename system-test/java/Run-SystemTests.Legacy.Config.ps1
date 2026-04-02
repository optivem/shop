# System Test Configuration (Legacy Modules)
# This file contains legacy suite configuration values for Run-SystemTests.ps1

$Config = @{

    Suites = @(

        # === mod02: Smoke ===
        @{  Id = "mod02-smoke";
            Name = "mod02 (smoke) - Smoke (real)";
            Command = "& .\gradlew.bat test -Dversion=mod02 -Dtype=smoke -DexternalSystemMode=real -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },

        # === mod03: E2E ===
        @{  Id = "mod03-e2e";
            Name = "mod03 (e2e) - E2E (real)";
            Command = "& .\gradlew.bat test -Dversion=mod03 -Dtype=e2e -DexternalSystemMode=real -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },

        # === mod04: Clients ===
        @{  Id = "mod04-smoke";
            Name = "mod04 (clients) - Smoke (real)";
            Command = "& .\gradlew.bat test -Dversion=mod04 -Dtype=smoke -DexternalSystemMode=real -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },
        @{  Id = "mod04-e2e";
            Name = "mod04 (clients) - E2E (real)";
            Command = "& .\gradlew.bat test -Dversion=mod04 -Dtype=e2e -DexternalSystemMode=real -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },

        # === mod05: Drivers ===
        @{  Id = "mod05-smoke";
            Name = "mod05 (drivers) - Smoke (real)";
            Command = "& .\gradlew.bat test -Dversion=mod05 -Dtype=smoke -DexternalSystemMode=real -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },
        @{  Id = "mod05-e2e";
            Name = "mod05 (drivers) - E2E (real)";
            Command = "& .\gradlew.bat test -Dversion=mod05 -Dtype=e2e -DexternalSystemMode=real -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },

        # === mod06: Channels ===
        @{  Id = "mod06-smoke";
            Name = "mod06 (channels) - Smoke (real)";
            Command = "& .\gradlew.bat test -Dversion=mod06 -Dtype=smoke -DexternalSystemMode=real -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },
        @{  Id = "mod06-e2e-api";
            Name = "mod06 (channels) - E2E (real) - API";
            Command = "& .\gradlew.bat test -Dversion=mod06 -Dtype=e2e -DexternalSystemMode=real -Dchannel=API -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },
        @{  Id = "mod06-e2e-ui";
            Name = "mod06 (channels) - E2E (real) - UI";
            Command = "& .\gradlew.bat test -Dversion=mod06 -Dtype=e2e -DexternalSystemMode=real -Dchannel=UI -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },

        # === mod07: App DSL ===
        @{  Id = "mod07-smoke";
            Name = "mod07 (app dsl) - Smoke (real)";
            Command = "& .\gradlew.bat test -Dversion=mod07 -Dtype=smoke -DexternalSystemMode=real -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },
        @{  Id = "mod07-e2e-api";
            Name = "mod07 (app dsl) - E2E (real) - API";
            Command = "& .\gradlew.bat test -Dversion=mod07 -Dtype=e2e -DexternalSystemMode=real -Dchannel=API -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },
        @{  Id = "mod07-e2e-ui";
            Name = "mod07 (app dsl) - E2E (real) - UI";
            Command = "& .\gradlew.bat test -Dversion=mod07 -Dtype=e2e -DexternalSystemMode=real -Dchannel=UI -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },

        # === mod08: Scenario DSL ===
        @{  Id = "mod08-smoke";
            Name = "mod08 (scenario dsl) - Smoke (real)";
            Command = "& .\gradlew.bat test -Dversion=mod08 -Dtype=smoke -DexternalSystemMode=real -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },
        @{  Id = "mod08-e2e-api";
            Name = "mod08 (scenario dsl) - E2E (real) - API";
            Command = "& .\gradlew.bat test -Dversion=mod08 -Dtype=e2e -DexternalSystemMode=real -Dchannel=API -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },
        @{  Id = "mod08-e2e-ui";
            Name = "mod08 (scenario dsl) - E2E (real) - UI";
            Command = "& .\gradlew.bat test -Dversion=mod08 -Dtype=e2e -DexternalSystemMode=real -Dchannel=UI -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },

        # === mod09: External Stubs ===
        @{  Id = "mod09-smoke-stub";
            Name = "mod09 (external stubs) - Smoke (stub)";
            Command = "& .\gradlew.bat test -Dversion=mod09 -Dtype=smoke -DexternalSystemMode=stub -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },
        @{  Id = "mod09-smoke-real";
            Name = "mod09 (external stubs) - Smoke (real)";
            Command = "& .\gradlew.bat test -Dversion=mod09 -Dtype=smoke -DexternalSystemMode=real -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },

        # === mod10: Acceptance Tests ===
        @{  Id = "mod10-acceptance-api";
            Name = "mod10 (acceptance) - Acceptance (stub) - API";
            Command = "& .\gradlew.bat test -Dversion=mod10 -Dtype=acceptance -DexcludeTags=isolated -Dchannel=API -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },
        @{  Id = "mod10-acceptance-ui";
            Name = "mod10 (acceptance) - Acceptance (stub) - UI";
            Command = "& .\gradlew.bat test -Dversion=mod10 -Dtype=acceptance -DexcludeTags=isolated -Dchannel=UI -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },
        @{  Id = "mod10-acceptance-isolated-api";
            Name = "mod10 (acceptance) - Acceptance Isolated (stub) - API";
            Command = "& .\gradlew.bat test -Dversion=mod10 -Dtype=acceptance -DincludeTags=isolated -Dchannel=API -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },
        @{  Id = "mod10-acceptance-isolated-ui";
            Name = "mod10 (acceptance) - Acceptance Isolated (stub) - UI";
            Command = "& .\gradlew.bat test -Dversion=mod10 -Dtype=acceptance -DincludeTags=isolated -Dchannel=UI -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },

        # === mod11: Contract Tests ===
        @{  Id = "mod11-contract-stub";
            Name = "mod11 (contract) - Contract (stub)";
            Command = "& .\gradlew.bat test -Dversion=mod11 -Dtype=contract -DexternalSystemMode=stub -Dmode=stub -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },
        @{  Id = "mod11-contract-real";
            Name = "mod11 (contract) - Contract (real)";
            Command = "& .\gradlew.bat test -Dversion=mod11 -Dtype=contract -DexternalSystemMode=real -Dmode=real -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },
        @{  Id = "mod11-e2e-api";
            Name = "mod11 (contract) - E2E (real) - API";
            Command = "& .\gradlew.bat test -Dversion=mod11 -Dtype=e2e -DexternalSystemMode=real -Dchannel=API -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },
        @{  Id = "mod11-e2e-ui";
            Name = "mod11 (contract) - E2E (real) - UI";
            Command = "& .\gradlew.bat test -Dversion=mod11 -Dtype=e2e -DexternalSystemMode=real -Dchannel=UI -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; }

    )
}

# Export the configuration
return $Config
