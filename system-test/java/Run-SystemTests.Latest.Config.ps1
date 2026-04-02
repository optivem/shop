# System Test Configuration (Latest)
# This file contains latest suite configuration values for Run-SystemTests.ps1

$Config = @{

    Suites = @(

        # === Smoke Tests (stub) ===
        @{  Id = "smoke-stub";
            Name = "latest - Smoke (stub)";
            Command = "& .\gradlew.bat test -Dversion=latest -Dtype=smoke -DexternalSystemMode=stub -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },

        # === Smoke Tests (real) ===
        @{  Id = "smoke-real";
            Name = "latest - Smoke (real)";
            Command = "& .\gradlew.bat test -Dversion=latest -Dtype=smoke -DexternalSystemMode=real -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },

        # === Acceptance Tests (stub) - API ===
        @{  Id = "acceptance-api";
            Name = "latest - Acceptance (stub) - API";
            Command = "& .\gradlew.bat test -Dversion=latest -Dtype=acceptance -DexcludeTags=isolated -Dchannel=API -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },

        # === Acceptance Tests (stub) - UI ===
        @{  Id = "acceptance-ui";
            Name = "latest - Acceptance (stub) - UI";
            Command = "& .\gradlew.bat test -Dversion=latest -Dtype=acceptance -DexcludeTags=isolated -Dchannel=UI -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },

        # === Acceptance Tests Isolated (stub) - API ===
        @{  Id = "acceptance-isolated-api";
            Name = "latest - Acceptance Isolated (stub) - API";
            Command = "& .\gradlew.bat test -Dversion=latest -Dtype=acceptance -DincludeTags=isolated -Dchannel=API -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },

        # === Acceptance Tests Isolated (stub) - UI ===
        @{  Id = "acceptance-isolated-ui";
            Name = "latest - Acceptance Isolated (stub) - UI";
            Command = "& .\gradlew.bat test -Dversion=latest -Dtype=acceptance -DincludeTags=isolated -Dchannel=UI -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },

        # === Contract Tests (stub) ===
        @{  Id = "contract-stub";
            Name = "latest - Contract (stub)";
            Command = "& .\gradlew.bat test -Dversion=latest -Dtype=contract -DexternalSystemMode=stub -Dmode=stub -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },

        # === Contract Tests (real) ===
        @{  Id = "contract-real";
            Name = "latest - Contract (real)";
            Command = "& .\gradlew.bat test -Dversion=latest -Dtype=contract -DexternalSystemMode=real -Dmode=real -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },

        # === E2E Tests (real) - API ===
        @{  Id = "e2e-api";
            Name = "latest - E2E (real) - API";
            Command = "& .\gradlew.bat test -Dversion=latest -Dtype=e2e -DexternalSystemMode=real -Dchannel=API -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },

        # === E2E Tests (real) - UI ===
        @{  Id = "e2e-ui";
            Name = "latest - E2E (real) - UI";
            Command = "& .\gradlew.bat test -Dversion=latest -Dtype=e2e -DexternalSystemMode=real -Dchannel=UI -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; }

    )
}

# Export the configuration
return $Config
