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

        # === Smoke Tests ===
        @{  Id = "smoke";
            Name = "Smoke Tests";
            Command = "& .\gradlew.bat test --tests '*SmokeTest*' -Denvironment=local -DexternalSystemMode=real";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; },

        # === E2E Tests ===
        @{  Id = "e2e";
            Name = "E2E Tests";
            Command = "& .\gradlew.bat test --tests '*E2eTest*' -Denvironment=local -DexternalSystemMode=real";
            Path = ".";
            TestReportPath = "build\reports\tests\test\index.html";
            TestInstallCommands = $null; }

    )
}

# Export the configuration
return $Config
