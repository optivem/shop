# Starter

A catalog of project templates organized by two independent dimensions: **system** (the application) and **system-test** (the test harness). Each template is self-contained and copy-paste-ready.

## System Templates

Pick based on your architecture and language:

### Monolith

| Language | Directory | Framework | Port | SonarCloud |
|---|---|---|---|---|
| Java | `system/monolith/java/` | Spring Boot + Thymeleaf (SSR) | 8080 | [starter-monolith-java](https://sonarcloud.io/project/overview?id=optivem_starter-monolith-java) |
| .NET | `system/monolith/dotnet/` | ASP.NET Core Razor Pages | 8080 | [starter-monolith-dotnet](https://sonarcloud.io/project/overview?id=optivem_starter-monolith-dotnet) |
| TypeScript | `system/monolith/typescript/` | Next.js (SSR) | 3000 | [starter-monolith-typescript](https://sonarcloud.io/project/overview?id=optivem_starter-monolith-typescript) |

### Multitier

#### Frontend

| Language | Directory | Framework | Port | SonarCloud |
|---|---|---|---|---|
| TypeScript | `system/multitier/frontend-react/` | React + Nginx | 8080 | [starter-multitier-frontend-react](https://sonarcloud.io/project/overview?id=optivem_starter-multitier-frontend-react) |

#### Backend

| Language | Directory | Framework | Port | SonarCloud |
|---|---|---|---|---|
| Java | `system/multitier/backend-java/` | Spring Boot API | 8081 | [starter-multitier-backend-java](https://sonarcloud.io/project/overview?id=optivem_starter-multitier-backend-java) |
| .NET | `system/multitier/backend-dotnet/` | ASP.NET Core API | 8081 | [starter-multitier-backend-dotnet](https://sonarcloud.io/project/overview?id=optivem_starter-multitier-backend-dotnet) |
| TypeScript | `system/multitier/backend-typescript/` | NestJS API | 8081 | [starter-multitier-backend-typescript](https://sonarcloud.io/project/overview?id=optivem_starter-multitier-backend-typescript) |

## System-Test Templates

Pick based on your preferred test language (independent of system language):

| Language | Directory | Framework |
|---|---|---|
| Java | `system-test/java/` | JUnit 5 + Playwright |
| .NET | `system-test/dotnet/` | xUnit + Playwright |
| TypeScript | `system-test/typescript/` | Jest + Playwright |

Each system-test includes `docker-compose.multitier.yml` and `docker-compose.monolith.yml` — rename the appropriate one to `docker-compose.yml`.

## CI/CD Pipelines

The `.github/workflows/` directory contains runnable pipelines for all 6 matched-language combinations (system + system-test):

### Monolith Java

[![monolith-commit-stage](https://github.com/optivem/starter/actions/workflows/monolith-java-commit-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/monolith-java-commit-stage.yml)
[![acceptance-stage](https://github.com/optivem/starter/actions/workflows/monolith-java-acceptance-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/monolith-java-acceptance-stage.yml)
[![qa-stage](https://github.com/optivem/starter/actions/workflows/monolith-java-qa-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/monolith-java-qa-stage.yml)
[![qa-signoff](https://github.com/optivem/starter/actions/workflows/monolith-java-qa-signoff.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/monolith-java-qa-signoff.yml)
[![prod-stage](https://github.com/optivem/starter/actions/workflows/monolith-java-prod-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/monolith-java-prod-stage.yml)

### Monolith .NET

[![monolith-commit-stage](https://github.com/optivem/starter/actions/workflows/monolith-dotnet-commit-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/monolith-dotnet-commit-stage.yml)
[![acceptance-stage](https://github.com/optivem/starter/actions/workflows/monolith-dotnet-acceptance-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/monolith-dotnet-acceptance-stage.yml)
[![qa-stage](https://github.com/optivem/starter/actions/workflows/monolith-dotnet-qa-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/monolith-dotnet-qa-stage.yml)
[![qa-signoff](https://github.com/optivem/starter/actions/workflows/monolith-dotnet-qa-signoff.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/monolith-dotnet-qa-signoff.yml)
[![prod-stage](https://github.com/optivem/starter/actions/workflows/monolith-dotnet-prod-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/monolith-dotnet-prod-stage.yml)

### Monolith TypeScript

[![monolith-commit-stage](https://github.com/optivem/starter/actions/workflows/monolith-typescript-commit-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/monolith-typescript-commit-stage.yml)
[![acceptance-stage](https://github.com/optivem/starter/actions/workflows/monolith-typescript-acceptance-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/monolith-typescript-acceptance-stage.yml)
[![qa-stage](https://github.com/optivem/starter/actions/workflows/monolith-typescript-qa-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/monolith-typescript-qa-stage.yml)
[![qa-signoff](https://github.com/optivem/starter/actions/workflows/monolith-typescript-qa-signoff.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/monolith-typescript-qa-signoff.yml)
[![prod-stage](https://github.com/optivem/starter/actions/workflows/monolith-typescript-prod-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/monolith-typescript-prod-stage.yml)

### Multitier Java

[![backend-commit-stage](https://github.com/optivem/starter/actions/workflows/multitier-backend-java-commit-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-backend-java-commit-stage.yml)
[![frontend-commit-stage](https://github.com/optivem/starter/actions/workflows/multitier-frontend-react-commit-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-frontend-react-commit-stage.yml)
[![acceptance-stage](https://github.com/optivem/starter/actions/workflows/multitier-system-java-acceptance-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-system-java-acceptance-stage.yml)
[![qa-stage](https://github.com/optivem/starter/actions/workflows/multitier-system-java-qa-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-system-java-qa-stage.yml)
[![qa-signoff](https://github.com/optivem/starter/actions/workflows/multitier-system-java-qa-signoff.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-system-java-qa-signoff.yml)
[![prod-stage](https://github.com/optivem/starter/actions/workflows/multitier-system-java-prod-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-system-java-prod-stage.yml)

### Multitier .NET

[![backend-commit-stage](https://github.com/optivem/starter/actions/workflows/multitier-backend-dotnet-commit-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-backend-dotnet-commit-stage.yml)
[![frontend-commit-stage](https://github.com/optivem/starter/actions/workflows/multitier-frontend-react-commit-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-frontend-react-commit-stage.yml)
[![acceptance-stage](https://github.com/optivem/starter/actions/workflows/multitier-system-dotnet-acceptance-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-system-dotnet-acceptance-stage.yml)
[![qa-stage](https://github.com/optivem/starter/actions/workflows/multitier-system-dotnet-qa-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-system-dotnet-qa-stage.yml)
[![qa-signoff](https://github.com/optivem/starter/actions/workflows/multitier-system-dotnet-qa-signoff.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-system-dotnet-qa-signoff.yml)
[![prod-stage](https://github.com/optivem/starter/actions/workflows/multitier-system-dotnet-prod-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-system-dotnet-prod-stage.yml)

### Multitier TypeScript

[![backend-commit-stage](https://github.com/optivem/starter/actions/workflows/multitier-backend-typescript-commit-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-backend-typescript-commit-stage.yml)
[![frontend-commit-stage](https://github.com/optivem/starter/actions/workflows/multitier-frontend-react-commit-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-frontend-react-commit-stage.yml)
[![acceptance-stage](https://github.com/optivem/starter/actions/workflows/multitier-system-typescript-acceptance-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-system-typescript-acceptance-stage.yml)
[![qa-stage](https://github.com/optivem/starter/actions/workflows/multitier-system-typescript-qa-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-system-typescript-qa-stage.yml)
[![qa-signoff](https://github.com/optivem/starter/actions/workflows/multitier-system-typescript-qa-signoff.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-system-typescript-qa-signoff.yml)
[![prod-stage](https://github.com/optivem/starter/actions/workflows/multitier-system-typescript-prod-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-system-typescript-prod-stage.yml)

- **Commit stages** trigger automatically on push via path filters
- **Acceptance/QA/Prod stages** are workflow_dispatch (manual trigger)

## Verification

[![verify-scaffold](https://github.com/optivem/starter/actions/workflows/verify-scaffold.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/verify-scaffold.yml)
[![verify-all](https://github.com/optivem/starter/actions/workflows/verify-all.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/verify-all.yml)

**verify-scaffold** — scaffolds test projects and verifies the full pipeline end-to-end (daily + manual dispatch).

**verify-all** — runs the full pipeline on the starter repo's own templates:

[![monolith-java-verify](https://github.com/optivem/starter/actions/workflows/monolith-java-verify.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/monolith-java-verify.yml)
[![monolith-dotnet-verify](https://github.com/optivem/starter/actions/workflows/monolith-dotnet-verify.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/monolith-dotnet-verify.yml)
[![monolith-typescript-verify](https://github.com/optivem/starter/actions/workflows/monolith-typescript-verify.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/monolith-typescript-verify.yml)
[![multitier-system-java-verify](https://github.com/optivem/starter/actions/workflows/multitier-system-java-verify.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-system-java-verify.yml)
[![multitier-system-dotnet-verify](https://github.com/optivem/starter/actions/workflows/multitier-system-dotnet-verify.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-system-dotnet-verify.yml)
[![multitier-system-typescript-verify](https://github.com/optivem/starter/actions/workflows/multitier-system-typescript-verify.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-system-typescript-verify.yml)
