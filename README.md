# Starter

A catalog of self-contained, copy-paste-ready project templates organized by two independent dimensions.

## Dimensions

### System (the application)

Pick **one** system template based on your architecture and language:

| Architecture | Language | Directory | Framework | Port |
|---|---|---|---|---|
| Single-component | Java | `system/single-component/java/` | Spring Boot + Thymeleaf (SSR) | 8080 |
| Single-component | .NET | `system/single-component/dotnet/` | ASP.NET Core Razor Pages | 8080 |
| Single-component | TypeScript | `system/single-component/typescript/` | Next.js (SSR) | 3000 |
| Multi-component | Java (backend) | `system/multi-component/backend-java/` | Spring Boot API | 8081 |
| Multi-component | .NET (backend) | `system/multi-component/backend-dotnet/` | ASP.NET Core API | 8081 |
| Multi-component | TypeScript (backend) | `system/multi-component/backend-typescript/` | NestJS API | 8081 |
| Multi-component | TypeScript (frontend) | `system/multi-component/frontend-react/` | React + Nginx | 8080 |

### System Test (the test harness)

Pick **one** system-test template based on your preferred test language (independent of system language):

| Language | Directory | Framework |
|---|---|---|
| Java | `system-test/java/` | JUnit 5 + Playwright |
| .NET | `system-test/dotnet/` | xUnit + Playwright |
| TypeScript | `system-test/typescript/` | Jest + Playwright |

### Pipeline (CI/CD workflows)

Pick pipeline workflows matching your system and system-test choices:

| Architecture | Directory | Contents |
|---|---|---|
| Single-component (Java) | `pipeline/single-component/java/` | 5 workflow files |
| Single-component (.NET) | `pipeline/single-component/dotnet/` | 5 workflow files |
| Single-component (TypeScript) | `pipeline/single-component/typescript/` | 5 workflow files |
| Multi-component (Java system-test) | `pipeline/multi-component/java/` | 6 workflow files |
| Multi-component (.NET system-test) | `pipeline/multi-component/dotnet/` | 6 workflow files |
| Multi-component (TypeScript system-test) | `pipeline/multi-component/typescript/` | 6 workflow files |

## Quick Start

1. Create a new repo
2. Copy your chosen **system** template into the repo (as `monolith/`, `backend/`, or `frontend/`)
3. Copy your chosen **system-test** template into the repo (as `system-test/`)
4. Copy the matching **pipeline** workflow files into `.github/workflows/`
5. Replace placeholders (`YOUR_SONAR_PROJECT_KEY`, `<owner>/<repo>`, image paths)
6. In `system-test/`, rename `docker-compose.single.yml` or `docker-compose.multi.yml` to `docker-compose.yml`
