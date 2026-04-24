# System Test (C#/.NET, monolith)

## Running Tests

Run all latest test suites:

```powershell
../Run-SystemTests.ps1 -Architecture monolith
```

Run legacy test suites:

```powershell
../Run-SystemTests.ps1 -Architecture monolith -Legacy
```

Run a specific suite by ID:

```powershell
../Run-SystemTests.ps1 -Architecture monolith -Suite acceptance-api
```

Rebuild containers before running:

```powershell
../Run-SystemTests.ps1 -Architecture monolith -Rebuild
```

See [../README.md](../README.md) for prerequisites, available suite IDs, and the suites shared across both architectures.
