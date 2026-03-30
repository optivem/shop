# Starter

A catalog of project templates organized by two independent dimensions: **system** (the application) and **system-test** (the test harness). Each template is self-contained and copy-paste-ready.

## System Templates

Pick based on your architecture and language:

### Monolith

| Language | Directory | Framework | Port |
|---|---|---|---|
| Java | `system/monolith/java/` | Spring Boot + Thymeleaf (SSR) | 8080 |
| .NET | `system/monolith/dotnet/` | ASP.NET Core Razor Pages | 8080 |
| TypeScript | `system/monolith/typescript/` | Next.js (SSR) | 3000 |

### Multitier

#### Frontend

| Language | Directory | Framework | Port |
|---|---|---|---|
| TypeScript | `system/multitier/frontend-react/` | React + Nginx | 8080 |

#### Backend

| Language | Directory | Framework | Port |
|---|---|---|---|
| Java | `system/multitier/backend-java/` | Spring Boot API | 8081 |
| .NET | `system/multitier/backend-dotnet/` | ASP.NET Core API | 8081 |
| TypeScript | `system/multitier/backend-typescript/` | NestJS API | 8081 |

## System-Test Templates

Pick based on your preferred test language (independent of system language):

| Language | Directory | Framework |
|---|---|---|
| Java | `system-test/java/` | JUnit 5 + Playwright |
| .NET | `system-test/dotnet/` | xUnit + Playwright |
| TypeScript | `system-test/typescript/` | Jest + Playwright |

Each system-test includes `docker-compose.single.yml` and `docker-compose.multi.yml` — rename the appropriate one to `docker-compose.yml`.

## CI/CD Pipelines

The `.github/workflows/` directory contains runnable pipelines for all 6 matched-language combinations (system + system-test):

| Combination | Commit | Acceptance | QA | QA Signoff | Prod |
|---|---|---|---|---|---|
| Monolith Java | [![CI](https://github.com/optivem/starter/actions/workflows/monolith-java-commit-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/monolith-java-commit-stage.yml) | [![CI](https://github.com/optivem/starter/actions/workflows/monolith-java-acceptance-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/monolith-java-acceptance-stage.yml) | [![CI](https://github.com/optivem/starter/actions/workflows/monolith-java-qa-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/monolith-java-qa-stage.yml) | [![CI](https://github.com/optivem/starter/actions/workflows/monolith-java-qa-signoff.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/monolith-java-qa-signoff.yml) | [![CI](https://github.com/optivem/starter/actions/workflows/monolith-java-prod-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/monolith-java-prod-stage.yml) |
| Monolith .NET | [![CI](https://github.com/optivem/starter/actions/workflows/monolith-dotnet-commit-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/monolith-dotnet-commit-stage.yml) | [![CI](https://github.com/optivem/starter/actions/workflows/monolith-dotnet-acceptance-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/monolith-dotnet-acceptance-stage.yml) | [![CI](https://github.com/optivem/starter/actions/workflows/monolith-dotnet-qa-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/monolith-dotnet-qa-stage.yml) | [![CI](https://github.com/optivem/starter/actions/workflows/monolith-dotnet-qa-signoff.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/monolith-dotnet-qa-signoff.yml) | [![CI](https://github.com/optivem/starter/actions/workflows/monolith-dotnet-prod-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/monolith-dotnet-prod-stage.yml) |
| Monolith TypeScript | [![CI](https://github.com/optivem/starter/actions/workflows/monolith-typescript-commit-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/monolith-typescript-commit-stage.yml) | [![CI](https://github.com/optivem/starter/actions/workflows/monolith-typescript-acceptance-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/monolith-typescript-acceptance-stage.yml) | [![CI](https://github.com/optivem/starter/actions/workflows/monolith-typescript-qa-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/monolith-typescript-qa-stage.yml) | [![CI](https://github.com/optivem/starter/actions/workflows/monolith-typescript-qa-signoff.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/monolith-typescript-qa-signoff.yml) | [![CI](https://github.com/optivem/starter/actions/workflows/monolith-typescript-prod-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/monolith-typescript-prod-stage.yml) |
| Multitier Java | [![BE](https://github.com/optivem/starter/actions/workflows/multitier-backend-java-commit-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-backend-java-commit-stage.yml) [![FE](https://github.com/optivem/starter/actions/workflows/multitier-frontend-react-commit-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-frontend-react-commit-stage.yml) | [![CI](https://github.com/optivem/starter/actions/workflows/multitier-system-java-acceptance-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-system-java-acceptance-stage.yml) | [![CI](https://github.com/optivem/starter/actions/workflows/multitier-system-java-qa-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-system-java-qa-stage.yml) | [![CI](https://github.com/optivem/starter/actions/workflows/multitier-system-java-qa-signoff.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-system-java-qa-signoff.yml) | [![CI](https://github.com/optivem/starter/actions/workflows/multitier-system-java-prod-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-system-java-prod-stage.yml) |
| Multitier .NET | [![BE](https://github.com/optivem/starter/actions/workflows/multitier-backend-dotnet-commit-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-backend-dotnet-commit-stage.yml) [![FE](https://github.com/optivem/starter/actions/workflows/multitier-frontend-react-commit-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-frontend-react-commit-stage.yml) | [![CI](https://github.com/optivem/starter/actions/workflows/multitier-system-dotnet-acceptance-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-system-dotnet-acceptance-stage.yml) | [![CI](https://github.com/optivem/starter/actions/workflows/multitier-system-dotnet-qa-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-system-dotnet-qa-stage.yml) | [![CI](https://github.com/optivem/starter/actions/workflows/multitier-system-dotnet-qa-signoff.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-system-dotnet-qa-signoff.yml) | [![CI](https://github.com/optivem/starter/actions/workflows/multitier-system-dotnet-prod-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-system-dotnet-prod-stage.yml) |
| Multitier TypeScript | [![BE](https://github.com/optivem/starter/actions/workflows/multitier-backend-typescript-commit-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-backend-typescript-commit-stage.yml) [![FE](https://github.com/optivem/starter/actions/workflows/multitier-frontend-react-commit-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-frontend-react-commit-stage.yml) | [![CI](https://github.com/optivem/starter/actions/workflows/multitier-system-typescript-acceptance-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-system-typescript-acceptance-stage.yml) | [![CI](https://github.com/optivem/starter/actions/workflows/multitier-system-typescript-qa-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-system-typescript-qa-stage.yml) | [![CI](https://github.com/optivem/starter/actions/workflows/multitier-system-typescript-qa-signoff.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-system-typescript-qa-signoff.yml) | [![CI](https://github.com/optivem/starter/actions/workflows/multitier-system-typescript-prod-stage.yml/badge.svg)](https://github.com/optivem/starter/actions/workflows/multitier-system-typescript-prod-stage.yml) |

- **Commit stages** trigger automatically on push via path filters
- **Acceptance/QA/Prod stages** are workflow_dispatch (manual trigger)
