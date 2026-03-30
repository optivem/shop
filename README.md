# Starter

A catalog of project templates organized by two independent dimensions: **system** (the application) and **system-test** (the test harness). Each template is self-contained and copy-paste-ready.

## System Templates

Pick based on your architecture and language:

| Architecture | Language | Directory | Framework | Port |
|---|---|---|---|---|
| Single-component | Java | `system/monolith/java/` | Spring Boot + Thymeleaf (SSR) | 8080 |
| Single-component | .NET | `system/monolith/dotnet/` | ASP.NET Core Razor Pages | 8080 |
| Single-component | TypeScript | `system/monolith/typescript/` | Next.js (SSR) | 3000 |
| Multi-component | Java (backend) | `system/multitier/backend-java/` | Spring Boot API | 8081 |
| Multi-component | .NET (backend) | `system/multitier/backend-dotnet/` | ASP.NET Core API | 8081 |
| Multi-component | TypeScript (backend) | `system/multitier/backend-typescript/` | NestJS API | 8081 |
| Multi-component | TypeScript (frontend) | `system/multitier/frontend-react/` | React + Nginx | 8080 |

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

| Combination | Commit Stage | Acceptance | QA | Prod |
|---|---|---|---|---|
| Single-component Java | `monolith-java-commit-stage` | `monolith-java-acceptance-stage` | `monolith-java-qa-stage` | `monolith-java-prod-stage` |
| Single-component .NET | `monolith-dotnet-commit-stage` | `monolith-dotnet-acceptance-stage` | `monolith-dotnet-qa-stage` | `monolith-dotnet-prod-stage` |
| Single-component TypeScript | `monolith-typescript-commit-stage` | `monolith-typescript-acceptance-stage` | `monolith-typescript-qa-stage` | `monolith-typescript-prod-stage` |
| Multi-component Java | `multitier-backend-java-commit-stage` | `multitier-system-java-acceptance-stage` | `multitier-system-java-qa-stage` | `multitier-system-java-prod-stage` |
| Multi-component .NET | `multitier-backend-dotnet-commit-stage` | `multitier-system-dotnet-acceptance-stage` | `multitier-system-dotnet-qa-stage` | `multitier-system-dotnet-prod-stage` |
| Multi-component TypeScript | `multitier-backend-typescript-commit-stage` | `multitier-system-typescript-acceptance-stage` | `multitier-system-typescript-qa-stage` | `multitier-system-typescript-prod-stage` |

Plus `multitier-frontend-react-commit-stage` (shared across all multitier variants).

- **Commit stages** trigger automatically on push via path filters
- **Acceptance/QA/Prod stages** are workflow_dispatch (manual trigger)
