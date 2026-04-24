<#
.SYNOPSIS
    Runs SonarScanner analysis on the solution.

.DESCRIPTION
    Requires dotnet-sonarscanner and SonarCloud/SonarQube token.
    Install: dotnet tool install --global dotnet-sonarscanner
    Get token: https://sonarcloud.io/account/security (SonarCloud) or your SonarQube server.

.PARAMETER Token
    SonarCloud/SonarQube token. Or set $env:SONAR_TOKEN.

.EXAMPLE
    .\Run-Sonar.ps1
    .\Run-Sonar.ps1 -Token "your-sonarcloud-token"
#>

param(
    [string]$Token = $env:SONAR_TOKEN
)

$ErrorActionPreference = "Stop"

if (-not $Token) {
    Write-Host "ERROR: Sonar token required. Set SONAR_TOKEN env var or pass -Token" -ForegroundColor Red
    Write-Host "Get token: https://sonarcloud.io/account/security" -ForegroundColor Yellow
    exit 1
}

$projectKey = "optivem_shop-tests-dotnet"
$projectName = "Shop Tests (.NET)"

Write-Host "Running SonarScanner for .NET..." -ForegroundColor Cyan

dotnet sonarscanner begin `
    /k:$projectKey `
    /n:$projectName `
    /d:sonar.host.url="https://sonarcloud.io" `
    /d:sonar.token=$Token

dotnet build MyCompany.MyShop.sln --no-incremental

dotnet sonarscanner end /d:sonar.token=$Token

Write-Host "Sonar analysis complete." -ForegroundColor Green
