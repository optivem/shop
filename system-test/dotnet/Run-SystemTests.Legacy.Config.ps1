# System Test Configuration (Legacy Modules)
# This file contains legacy suite configuration values for Run-SystemTests.ps1

$Config = @{

    Suites = @(

        # === mod02: Smoke ===
        @{  Id = "mod02-smoke";
            SampleTest = "ShouldBeAbleToGoToShop";
            Name = "mod02 - Smoke (real)";
            Command = "dotnet test --filter 'FullyQualifiedName~.Legacy.Mod02.SmokeTests' -e EXTERNAL_SYSTEM_MODE=real --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html";
            TestInstallCommands = "pwsh bin/Debug/net8.0/playwright.ps1 install"; },

        # === mod03: E2E ===
        @{  Id = "mod03-e2e";
            SampleTest = "ShouldPlaceOrder";
            Name = "mod03 - E2E";
            Command = "dotnet test --filter 'FullyQualifiedName~.Legacy.Mod03.E2eTests' --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html";
            TestInstallCommands = "pwsh bin/Debug/net8.0/playwright.ps1 install"; },

        # === mod04: Smoke + E2E ===
        @{  Id = "mod04-smoke";
            SampleTest = "ShouldBeAbleToGoToShop";
            Name = "mod04 - Smoke (real)";
            Command = "dotnet test --filter 'FullyQualifiedName~.Legacy.Mod04.SmokeTests' -e EXTERNAL_SYSTEM_MODE=real --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html";
            TestInstallCommands = "pwsh bin/Debug/net8.0/playwright.ps1 install"; },
        @{  Id = "mod04-e2e";
            SampleTest = "ShouldPlaceOrder";
            Name = "mod04 - E2E";
            Command = "dotnet test --filter 'FullyQualifiedName~.Legacy.Mod04.E2eTests' --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html";
            TestInstallCommands = "pwsh bin/Debug/net8.0/playwright.ps1 install"; },

        # === mod05: Smoke + E2E ===
        @{  Id = "mod05-smoke";
            SampleTest = "ShouldBeAbleToGoToShop";
            Name = "mod05 - Smoke (real)";
            Command = "dotnet test --filter 'FullyQualifiedName~.Legacy.Mod05.SmokeTests' -e EXTERNAL_SYSTEM_MODE=real --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html";
            TestInstallCommands = "pwsh bin/Debug/net8.0/playwright.ps1 install"; },
        @{  Id = "mod05-e2e";
            SampleTest = "ShouldPlaceOrder";
            Name = "mod05 - E2E";
            Command = "dotnet test --filter 'FullyQualifiedName~.Legacy.Mod05.E2eTests' --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html";
            TestInstallCommands = "pwsh bin/Debug/net8.0/playwright.ps1 install"; },

        # === mod06: Smoke + E2E ===
        @{  Id = "mod06-smoke";
            SampleTest = "ShouldBeAbleToGoToShop";
            Name = "mod06 - Smoke (real)";
            Command = "dotnet test --filter 'FullyQualifiedName~.Legacy.Mod06.SmokeTests' -e EXTERNAL_SYSTEM_MODE=real --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html";
            TestInstallCommands = "pwsh bin/Debug/net8.0/playwright.ps1 install"; },
        @{  Id = "mod06-e2e-api";
            SampleTest = "ShouldPlaceOrder";
            Name = "mod06 - E2E - API";
            Command = "dotnet test --filter 'FullyQualifiedName~.Legacy.Mod06.E2eTests' -e CHANNEL=API --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html" },
        @{  Id = "mod06-e2e-ui";
            SampleTest = "ShouldPlaceOrder";
            Name = "mod06 - E2E - UI";
            Command = "dotnet test --filter 'FullyQualifiedName~.Legacy.Mod06.E2eTests' -e CHANNEL=UI --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html";
            TestInstallCommands = "pwsh bin/Debug/net8.0/playwright.ps1 install"; },

        # === mod07: Smoke + E2E ===
        @{  Id = "mod07-smoke";
            SampleTest = "ShouldBeAbleToGoToShop";
            Name = "mod07 - Smoke (real)";
            Command = "dotnet test --filter 'FullyQualifiedName~.Legacy.Mod07.SmokeTests' -e EXTERNAL_SYSTEM_MODE=real --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html";
            TestInstallCommands = "pwsh bin/Debug/net8.0/playwright.ps1 install"; },
        @{  Id = "mod07-e2e-api";
            SampleTest = "ShouldPlaceOrder";
            Name = "mod07 - E2E - API";
            Command = "dotnet test --filter 'FullyQualifiedName~.Legacy.Mod07.E2eTests' -e CHANNEL=API --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html" },
        @{  Id = "mod07-e2e-ui";
            SampleTest = "ShouldPlaceOrder";
            Name = "mod07 - E2E - UI";
            Command = "dotnet test --filter 'FullyQualifiedName~.Legacy.Mod07.E2eTests' -e CHANNEL=UI --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html";
            TestInstallCommands = "pwsh bin/Debug/net8.0/playwright.ps1 install"; },

        # === mod08: Smoke + E2E ===
        @{  Id = "mod08-smoke";
            SampleTest = "ShouldBeAbleToGoToShop";
            Name = "mod08 - Smoke (real)";
            Command = "dotnet test --filter 'FullyQualifiedName~.Legacy.Mod08.SmokeTests' -e EXTERNAL_SYSTEM_MODE=real --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html";
            TestInstallCommands = "pwsh bin/Debug/net8.0/playwright.ps1 install"; },
        @{  Id = "mod08-e2e-api";
            SampleTest = "ShouldPlaceOrder";
            Name = "mod08 - E2E - API";
            Command = "dotnet test --filter 'FullyQualifiedName~.Legacy.Mod08.E2eTests' -e CHANNEL=API --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html" },
        @{  Id = "mod08-e2e-ui";
            SampleTest = "ShouldPlaceOrder";
            Name = "mod08 - E2E - UI";
            Command = "dotnet test --filter 'FullyQualifiedName~.Legacy.Mod08.E2eTests' -e CHANNEL=UI -e CHANNEL_MODE=static --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html";
            TestInstallCommands = "pwsh bin/Debug/net8.0/playwright.ps1 install"; },

        # === mod09: Smoke (stub + real) ===
        @{  Id = "mod09-smoke-stub";
            SampleTest = "ShouldBeAbleToGoToShop";
            Name = "mod09 - Smoke (stub)";
            Command = "dotnet test --filter 'FullyQualifiedName~.Legacy.Mod09.SmokeTests' -e EXTERNAL_SYSTEM_MODE=stub --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html";
            TestInstallCommands = "pwsh bin/Debug/net8.0/playwright.ps1 install"; },
        @{  Id = "mod09-smoke-real";
            SampleTest = "ShouldBeAbleToGoToShop";
            Name = "mod09 - Smoke (real)";
            Command = "dotnet test --filter 'FullyQualifiedName~.Legacy.Mod09.SmokeTests' -e EXTERNAL_SYSTEM_MODE=real --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html";
            TestInstallCommands = "pwsh bin/Debug/net8.0/playwright.ps1 install"; },

        # === mod10: Acceptance ===
        @{  Id = "mod10-acceptance-api";
            SampleTest = "OrderNumberShouldStartWithORD";
            Name = "mod10 - Acceptance (stub) - API";
            Command = "dotnet test --filter 'FullyQualifiedName~.Legacy.Mod10.AcceptanceTests&Category!=isolated' -e CHANNEL=API --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html" },
        @{  Id = "mod10-acceptance-ui";
            SampleTest = "OrderNumberShouldStartWithORD";
            Name = "mod10 - Acceptance (stub) - UI";
            Command = "dotnet test --filter 'FullyQualifiedName~.Legacy.Mod10.AcceptanceTests&Category!=isolated' -e CHANNEL=UI -e CHANNEL_MODE=static --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html";
            TestInstallCommands = "pwsh bin/Debug/net8.0/playwright.ps1 install"; },
        @{  Id = "mod10-acceptance-isolated-api";
            SampleTest = "ShouldRecordPlacementTimestamp";
            Name = "mod10 - Acceptance Isolated (stub) - API";
            Command = "dotnet test --filter 'FullyQualifiedName~.Legacy.Mod10.AcceptanceTests&Category=isolated' -e CHANNEL=API --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html" },
        @{  Id = "mod10-acceptance-isolated-ui";
            SampleTest = "ShouldRecordPlacementTimestamp";
            Name = "mod10 - Acceptance Isolated (stub) - UI";
            Command = "dotnet test --filter 'FullyQualifiedName~.Legacy.Mod10.AcceptanceTests&Category=isolated' -e CHANNEL=UI -e CHANNEL_MODE=static --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html";
            TestInstallCommands = "pwsh bin/Debug/net8.0/playwright.ps1 install"; },

        # === mod11: Contract + E2E ===
        @{  Id = "mod11-contract-stub";
            SampleTest = "ShouldBeAbleToGetTime";
            Name = "mod11 - Contract (stub)";
            Command = "dotnet test --filter 'FullyQualifiedName~.Legacy.Mod11.ExternalSystemContractTests&FullyQualifiedName~Stub&Category!=isolated' --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html" },
        @{  Id = "mod11-contract-stub-isolated";
            SampleTest = "ShouldBeAbleToGetConfiguredTime";
            Name = "mod11 - Contract Isolated (stub)";
            Command = "dotnet test --filter 'FullyQualifiedName~.Legacy.Mod11.ExternalSystemContractTests&FullyQualifiedName~Stub&Category=isolated' --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html" },
        @{  Id = "mod11-contract-real";
            SampleTest = "ShouldBeAbleToGetTime";
            Name = "mod11 - Contract (real)";
            Command = "dotnet test --filter 'FullyQualifiedName~.Legacy.Mod11.ExternalSystemContractTests&FullyQualifiedName~Real' --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html" },
        @{  Id = "mod11-e2e-api";
            SampleTest = "ShouldPlaceOrder";
            Name = "mod11 - E2E - API";
            Command = "dotnet test --filter 'FullyQualifiedName~.Legacy.Mod11.E2eTests' -e CHANNEL=API --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html" },
        @{  Id = "mod11-e2e-ui";
            SampleTest = "ShouldPlaceOrder";
            Name = "mod11 - E2E - UI";
            Command = "dotnet test --filter 'FullyQualifiedName~.Legacy.Mod11.E2eTests' -e CHANNEL=UI -e CHANNEL_MODE=static --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html";
            TestInstallCommands = "pwsh bin/Debug/net8.0/playwright.ps1 install"; }

    )
}

# Export the configuration
return $Config
