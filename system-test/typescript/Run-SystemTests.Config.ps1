# System Test Configuration (Shared)
# This file contains shared configuration values for Run-SystemTests.ps1

$Config = @{

    TestFilter = "--grep '<test>'"

    BuildCommands = @(
        @{  Name = "Install Dependencies";
            Command = "npm ci"
        },
        @{  Name = "Install Playwright Browsers";
            Command = "npx playwright install chromium"
        }
    )

}

# Export the configuration
return $Config
