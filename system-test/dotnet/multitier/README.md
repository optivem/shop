# System Test (C#/.NET, multitier)

## Running Tests

Run all latest test suites:

```powershell
../Run-SystemTests.ps1 -Architecture multitier
```

Run legacy test suites:

```powershell
../Run-SystemTests.ps1 -Architecture multitier -Legacy
```

Run a specific suite by ID:

```powershell
../Run-SystemTests.ps1 -Architecture multitier -Suite acceptance-api
```

Rebuild containers before running:

```powershell
../Run-SystemTests.ps1 -Architecture multitier -Rebuild
```

See [../README.md](../README.md) for prerequisites, available suite IDs, and the suites shared across both architectures.
