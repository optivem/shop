# System Test Configuration (Latest)
# This file contains latest suite configuration values for Run-SystemTests.ps1

$Config = @{

    Suites = @(

        # === Smoke Tests (stub) ===
        @{  Id = "smoke-stub";
            Name = "latest - Smoke (stub)";
            Command = "dotnet test --filter 'FullyQualifiedName~.Latest.SmokeTests' -e EXTERNAL_SYSTEM_MODE=stub --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html";
            TestInstallCommands = "pwsh bin/Debug/net8.0/playwright.ps1 install" },

        # === Smoke Tests (real) ===
        @{  Id = "smoke-real";
            Name = "latest - Smoke (real)";
            Command = "dotnet test --filter 'FullyQualifiedName~.Latest.SmokeTests' -e EXTERNAL_SYSTEM_MODE=real --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html";
            TestInstallCommands = "pwsh bin/Debug/net8.0/playwright.ps1 install" },

        # === Acceptance Tests (stub) - API ===
        @{  Id = "acceptance-api";
            Name = "latest - Acceptance (stub) - API";
            Command = "dotnet test --filter 'FullyQualifiedName~.Latest.AcceptanceTests&Category!=isolated' -e CHANNEL=API --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html" },

        # === Acceptance Tests (stub) - UI ===
        @{  Id = "acceptance-ui";
            Name = "latest - Acceptance (stub) - UI";
            Command = "dotnet test --filter 'FullyQualifiedName~.Latest.AcceptanceTests&Category!=isolated' -e CHANNEL=UI -e CHANNEL_MODE=dynamic --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html";
            TestInstallCommands = "pwsh bin/Debug/net8.0/playwright.ps1 install"; },

        # === Acceptance Tests Isolated (stub) - API ===
        @{  Id = "acceptance-isolated-api";
            Name = "latest - Acceptance Isolated (stub) - API";
            Command = "dotnet test --filter 'FullyQualifiedName~.Latest.AcceptanceTests&Category=isolated' -e CHANNEL=API --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html" },

        # === Acceptance Tests Isolated (stub) - UI ===
        @{  Id = "acceptance-isolated-ui";
            Name = "latest - Acceptance Isolated (stub) - UI";
            Command = "dotnet test --filter 'FullyQualifiedName~.Latest.AcceptanceTests&Category=isolated' -e CHANNEL=UI --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html";
            TestInstallCommands = "pwsh bin/Debug/net8.0/playwright.ps1 install"; },

        # === Contract Tests (stub) ===
        @{  Id = "contract-stub";
            Name = "latest - Contract (stub)";
            Command = "dotnet test --filter 'FullyQualifiedName~.Latest.ExternalSystemContractTests&FullyQualifiedName~Stub&Category!=isolated' --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html" },

        # === Contract Tests Isolated (stub) ===
        @{  Id = "contract-stub-isolated";
            Name = "latest - Contract Isolated (stub)";
            Command = "dotnet test --filter 'FullyQualifiedName~.Latest.ExternalSystemContractTests&FullyQualifiedName~Stub&Category=isolated' --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html" },

        # === Contract Tests (real) ===
        @{  Id = "contract-real";
            Name = "latest - Contract (real)";
            Command = "dotnet test --filter 'FullyQualifiedName~.Latest.ExternalSystemContractTests&FullyQualifiedName~Real' --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html" },

        # === E2E Tests (real) - API ===
        @{  Id = "e2e-api";
            Name = "latest - E2E (real) - API";
            Command = "dotnet test --filter 'FullyQualifiedName~.Latest.E2eTests' -e CHANNEL=API --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html";
            TestInstallCommands = "pwsh bin/Debug/net8.0/playwright.ps1 install" },

        # === E2E Tests (real) - UI ===
        @{  Id = "e2e-ui";
            Name = "latest - E2E (real) - UI";
            Command = "dotnet test --filter 'FullyQualifiedName~.Latest.E2eTests' -e CHANNEL=UI --logger 'trx;LogFileName=testResults.trx' --logger 'html;LogFileName=testResults.html' --logger 'console;verbosity=detailed' -e ENVIRONMENT=local";
            Path = "SystemTests";
            TestReportPath = "SystemTests\TestResults\testResults.html";
            TestInstallCommands = "pwsh bin/Debug/net8.0/playwright.ps1 install" }

    )
}

# Export the configuration
return $Config
