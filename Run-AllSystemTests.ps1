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

function Invoke-PhaseInParallel {
    param(
        [string]$Phase,
        [string[]]$Languages,
        [hashtable]$ScriptArgs
    )

    Write-Heading -Text "PHASE: $Phase (parallel: $($Languages -join ', '))"

    $jobs = @()
    foreach ($lang in $Languages) {
        $langDir = Join-Path $SystemTestRoot $lang
        $script  = Join-Path $langDir "Run-SystemTests.ps1"

        if (-not (Test-Path $script)) {
            Write-Host "[$lang] Script not found: $script" -ForegroundColor Red
            continue
        }

        $exitCodeFile = Join-Path ([System.IO.Path]::GetTempPath()) "shop-systemtest-$Phase-$lang-$PID.exit"
        if (Test-Path $exitCodeFile) { Remove-Item $exitCodeFile -Force }

        $startTime = Get-Date

        $job = Start-Job -Name "$Phase/$lang" -ScriptBlock {
            param($LangDir, $ScriptArgs, $ExitCodeFile)
            Set-Location $LangDir
            & ".\Run-SystemTests.ps1" @ScriptArgs *>&1
            $LASTEXITCODE | Out-File -FilePath $ExitCodeFile -Encoding ascii
        } -ArgumentList $langDir, $ScriptArgs, $exitCodeFile

        $jobs += [pscustomobject]@{
            Job          = $job
            Language     = $lang
            Phase        = $Phase
            ExitCodeFile = $exitCodeFile
            StartTime    = $startTime
            EndTime      = $null
            OutputLines  = [System.Collections.Generic.List[string]]::new()
        }
    }

    # Stream output as it arrives (prefixed with [lang]) and accumulate for later parsing
    while ($jobs.Job | Where-Object { $_.State -eq 'Running' -or $_.HasMoreData }) {
        foreach ($entry in $jobs) {
            $data = Receive-Job -Job $entry.Job -ErrorAction SilentlyContinue
            if ($data) {
                foreach ($line in $data) {
                    $lineStr = "$line"
                    Write-Host "[$($entry.Language)] $lineStr"
                    $entry.OutputLines.Add($lineStr)
                }
            }
            if ($entry.Job.State -ne 'Running' -and -not $entry.EndTime) {
                $entry.EndTime = Get-Date
            }
        }
        Start-Sleep -Milliseconds 300
    }

    # Build results
    $results = @()
    foreach ($entry in $jobs) {
        if (-not $entry.EndTime) { $entry.EndTime = Get-Date }

        $exitCode = $null
        if (Test-Path $entry.ExitCodeFile) {
            $raw = (Get-Content $entry.ExitCodeFile -Raw).Trim()
            if ($raw -ne '') { $exitCode = [int]$raw }
            Remove-Item $entry.ExitCodeFile -Force -ErrorAction SilentlyContinue
        }

        $status = if ($exitCode -eq 0 -or $null -eq $exitCode) { "PASSED" } else { "FAILED" }
        $counts = Get-PassFailCounts -OutputLines $entry.OutputLines
        $duration = $entry.EndTime - $entry.StartTime

        $results += [pscustomobject]@{
            Phase    = $entry.Phase
            Language = $entry.Language
            Status   = $status
            ExitCode = $exitCode
            Passed   = $counts.Passed
            Failed   = $counts.Failed
            Duration = $duration
        }

        Remove-Job -Job $entry.Job -Force -ErrorAction SilentlyContinue
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
    $allResults += Invoke-PhaseInParallel -Phase "Rebuild" -Languages $Languages -ScriptArgs $rebuildArgs
}

$allResults += Invoke-PhaseInParallel -Phase "Latest" -Languages $Languages -ScriptArgs $baseArgs

$legacyArgs = $baseArgs.Clone()
$legacyArgs.Legacy = $true
$allResults += Invoke-PhaseInParallel -Phase "Legacy" -Languages $Languages -ScriptArgs $legacyArgs

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
