# Fix SonarLint "Could not initialize SonarLint Backend" in VS Code

**Status:** Open — not yet executed.

## Context

The SonarLint VS Code extension fails at startup on this machine:

```
SonarLint Language Server client: couldn't create connection to server.
  Message: Internal error.   Code: -32603
java.lang.IllegalStateException: Could not initialize SonarLint Backend
    at org.sonarsource.sonarlint.ls.backend.BackendService.initialize(BackendService.java:163)
```

The error is generic; the root cause is in the lines before the first stack trace in
Output -> SonarLint. This is a local IDE fault, not a defect in the shop code.

## Work

Try in order, stopping when SonarLint starts cleanly:

1. Read Output -> SonarLint and note the first `[error]` / `Caused by` before the backend-init failure.
2. Close VS Code, delete `%USERPROFILE%\.sonarlint` and
   `%APPDATA%\Code\User\globalStorage\sonarsource.sonarlint-vscode`, reopen.
3. Check `sonarlint.connectedMode.connections.sonarcloud` in user settings; remove and re-add the
   connection if the token is stale.
4. Check `sonarlint.ls.javaHome`: must be a valid JDK 17+, or cleared to use the bundled runtime.
5. Reinstall the extension, or pin the previous version via "Install Another Version".
6. Check antivirus / locked-file errors (`AccessDenied`) in the earlier log lines.

## Done when

SonarLint initialises without the backend error and reports findings on shop sources.
