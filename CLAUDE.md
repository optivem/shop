# Starter Repo Guidelines

## Pre-Commit Verification

Before committing any code changes, always verify compilation locally for the affected components:

- **Java** (monolith/multitier): `./gradlew build` in the project directory
- **TypeScript** (monolith): `npx tsc --noEmit` in the project directory
- **TypeScript** (multitier frontend-react): `npx tsc --noEmit` in the project directory
- **TypeScript** (multitier backend-typescript): `npx tsc --noEmit` in the project directory
- **.NET** (monolith/multitier): `dotnet build` in the project directory

Never commit code that does not compile. If multiple components are changed, verify each one before committing.
