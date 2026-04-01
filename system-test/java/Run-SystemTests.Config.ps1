# System Test Configuration
# This file contains configuration values for Run-SystemTests.ps1

$Config = @{

    TestFilter = "--tests '*.<test>'"

    BuildCommands = @(
        @{  Name = "Clean Build";
            Command = ".\gradlew.bat clean compileJava compileTestJava"
        }
    )

    Suites = @(

        # === Smoke Tests (stub) ===
        @{  Id = "smoke-stub";
            Name = "Smoke (stub)";
            Command = "& .\gradlew.bat test --tests '*smoke*' -DexternalSystemMode=stub -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },

        # === Smoke Tests (real) ===
        @{  Id = "smoke-real";
            Name = "Smoke (real)";
            Command = "& .\gradlew.bat test --tests '*smoke*' -DexternalSystemMode=real -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },

        # === Acceptance Tests (stub) - API ===
        @{  Id = "acceptance-api";
            Name = "Acceptance (stub) - API";
            Command = "& .\gradlew.bat test --tests '*acceptance*' -DexcludeTags=isolated -Dchannel=API -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },

        # === Acceptance Tests (stub) - UI ===
        @{  Id = "acceptance-ui";
            Name = "Acceptance (stub) - UI";
            Command = "& .\gradlew.bat test --tests '*acceptance*' -DexcludeTags=isolated -Dchannel=UI -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },

        # === Acceptance Tests Isolated (stub) - API ===
        @{  Id = "acceptance-isolated-api";
            Name = "Acceptance Isolated (stub) - API";
            Command = "& .\gradlew.bat test --tests '*acceptance*' -DincludeTags=isolated -Dchannel=API -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },

        # === Acceptance Tests Isolated (stub) - UI ===
        @{  Id = "acceptance-isolated-ui";
            Name = "Acceptance Isolated (stub) - UI";
            Command = "& .\gradlew.bat test --tests '*acceptance*' -DincludeTags=isolated -Dchannel=UI -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },

        # === E2E Tests (real) - API ===
        @{  Id = "e2e-api";
            Name = "E2E (real) - API";
            Command = "& .\gradlew.bat test --tests '*e2e*' -DexternalSystemMode=real -Dchannel=API -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },

        # === E2E Tests (real) - UI ===
        @{  Id = "e2e-ui";
            Name = "E2E (real) - UI";
            Command = "& .\gradlew.bat test --tests '*e2e*' -DexternalSystemMode=real -Dchannel=UI -Denvironment=local";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; }

    )
}

# Export the configuration
return $Config
