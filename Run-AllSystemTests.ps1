param(
    [string[]]$Languages = @("dotnet", "java", "typescript"),

    [ValidateSet("local", "pipeline")]
    [string]$Mode = "local",

    [ValidateSet("multitier", "monolith")]
    [string]$Architecture,

    [switch]$Rebuild
)

$ErrorActionPreference = "Continue"
$RepoRoot = $PSScriptRoot
$SystemTestRoot = Join-Path $RepoRoot "system-test"

function Write-Heading {
    param(
        [string]$Text,
        [string]$Color = "Cyan"
    )
    Write-Host ""
    Write-Host "================================================" -ForegroundColor $Color
    Write-Host $Text -ForegroundColor $Color
    Write-Host "================================================" -ForegroundColor $Color
    Write-Host ""
}

function Get-PassFailCounts {
    param([string[]]$OutputLines)

    # Match suite result lines from inner script's summary block:
    #   "  <name>                   PASSED   mm:ss.fff"
    #   "  <name>                   FAILED   mm:ss.fff"
    $passed = 0
    $failed = 0
    foreach ($line in $OutputLines) {
        if ($line -match '\s(PASSED|FAILED)\s+\d+:\d+') {
            if ($matches[1] -eq 'PASSED') { $passed++ } else { $failed++ }
        }
    }
    return @{ Passed = $passed; Failed = $failed }
}

function Format-Duration {
    param([TimeSpan]$Span)
    if ($Span.TotalHours -ge 1) {
        return $Span.ToString('h\:mm\:ss')
    }
    return $Span.ToString('m\:ss')
}

function Invoke-Phase {
    param(
        [string]$Phase,
        [string[]]$Languages,
        [hashtable]$ScriptArgs
    )

    Write-Heading -Text "PHASE: $Phase (sequential: $($Languages -join ', '))"

    $results = @()
    foreach ($lang in $Languages) {
        $langDir = Join-Path $SystemTestRoot $lang
        $script  = Join-Path $langDir "Run-SystemTests.ps1"

        if (-not (Test-Path $script)) {
            Write-Host "[$lang] Script not found: $script" -ForegroundColor Red
            continue
        }

        Write-Host ""
        Write-Host "--- $Phase / $lang ---" -ForegroundColor Cyan

        $outputLines = [System.Collections.Generic.List[string]]::new()
        $startTime = Get-Date
        $exitCode = $null

        Push-Location $langDir
        try {
            & ".\Run-SystemTests.ps1" @ScriptArgs *>&1 | ForEach-Object {
                $lineStr = "$_"
                Write-Host "[$lang] $lineStr"
                $outputLines.Add($lineStr)
            }
            $exitCode = $LASTEXITCODE
        } finally {
            Pop-Location
        }

        $endTime = Get-Date
        $status = if ($exitCode -eq 0 -or $null -eq $exitCode) { "PASSED" } else { "FAILED" }
        $counts = Get-PassFailCounts -OutputLines $outputLines

        $results += [pscustomobject]@{
            Phase    = $Phase
            Language = $lang
            Status   = $status
            ExitCode = $exitCode
            Passed   = $counts.Passed
            Failed   = $counts.Failed
            Duration = $endTime - $startTime
        }
    }

    return $results
}

$baseArgs = @{ Mode = $Mode }
if ($Architecture) { $baseArgs.Architecture = $Architecture }

$allResults = @()
$overallStart = Get-Date

if ($Rebuild) {
    $rebuildArgs = $baseArgs.Clone()
    $rebuildArgs.Rebuild   = $true
    $rebuildArgs.SkipTests = $true
    $allResults += Invoke-Phase -Phase "Rebuild" -Languages $Languages -ScriptArgs $rebuildArgs
}

$allResults += Invoke-Phase -Phase "Latest" -Languages $Languages -ScriptArgs $baseArgs

$legacyArgs = $baseArgs.Clone()
$legacyArgs.Legacy = $true
$allResults += Invoke-Phase -Phase "Legacy" -Languages $Languages -ScriptArgs $legacyArgs

$overallDuration = (Get-Date) - $overallStart

# Summary (Rebuild phase skipped tests so it isn't shown here)
$testResults = $allResults | Where-Object { $_.Phase -ne 'Rebuild' }
$failed = $testResults | Where-Object { $_.Status -eq "FAILED" }
$total  = $testResults.Count

Write-Heading -Text "SUMMARY" -Color Cyan

if ($failed) {
    Write-Host "$($failed.Count) of $total run(s) FAILED." -ForegroundColor Red
} else {
    Write-Host "All $total runs completed successfully." -ForegroundColor Green
}
Write-Host ""

$header = "{0,-12} {1,-8} {2,-28} {3}" -f "Language", "Suite", "Result", "Duration"
Write-Host $header -ForegroundColor Cyan
Write-Host ("-" * $header.Length) -ForegroundColor Cyan

foreach ($r in $testResults) {
    $resultText = if ($r.Failed -gt 0) {
        "$($r.Passed) PASSED, $($r.Failed) FAILED"
    } elseif ($r.Status -eq "FAILED") {
        "FAILED (no parsed counts)"
    } elseif ($r.Passed -eq 0) {
        "PASSED (no parsed counts)"
    } else {
        "$($r.Passed) suites PASSED"
    }

    $color = if ($r.Status -eq "PASSED") { "Green" } else { "Red" }
    Write-Host ("{0,-12} {1,-8} {2,-28} {3}" -f $r.Language, $r.Phase, $resultText, (Format-Duration $r.Duration)) -ForegroundColor $color
}

Write-Host ("-" * $header.Length) -ForegroundColor Cyan
Write-Host ("Total duration: {0}" -f (Format-Duration $overallDuration)) -ForegroundColor Cyan

if ($failed) {
    Write-Host ""
    Write-Host "Zero failures required — $($failed.Count) failed." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "All passing, zero failures." -ForegroundColor Green
exit 0
