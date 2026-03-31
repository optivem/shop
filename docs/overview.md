# Overview

## 01-general (shared setup — all architectures)

### 01-prerequisites
- Project Information (owner, domain, name, repo, language, architecture, repo strategy)
- Credentials (Docker Hub token, SonarCloud token)
- Onboarding Mode

### 02-project-repository
- Create System Repository (name, visibility, readme, license)

### 03-repository-setup
- Create Environments (acceptance, qa, production)
- Set Secrets and Variables (DOCKERHUB_TOKEN, SONAR_TOKEN, DOCKERHUB_USERNAME, system URLs per environment)

### 04-apply-template
- Copy Template Files (Monolith | Multitier)
- Replace Repository References (sed commands)
- Docker Compose (lowercase image urls)
- Create SonarCloud Project
- Namespace Replacement
- Commit, Push, and Verify

### 05-customize-template (optional)
- Monolith Language
- System Test Language

### 06-sonarcloud-setup (reference)
- Account setup, troubleshooting, bulk operations

## 02-monolith (pipeline stages)

### 02-commit-stage
- Verify commit stage + SonarCloud analysis

### 03-acceptance-stage
- Trigger + verify acceptance stage

### 04-qa-stage
- Trigger QA stage + QA signoff

### 05-production-stage
- Trigger production stage + verify release

## 03-multitier (pipeline stages)

### 02-commit-stage
- Verify commit stages (frontend + backend) + SonarCloud analysis

### 03-acceptance-stage
- Update image refs + verify

### 04-qa-stage
- Update image refs + verify

### 05-production-stage
- Update image refs + verify

## 04-multirepo (pipeline stages)

### 01-commit-stage
- Split into separate repos

### 02-acceptance-stage
- Cross-repo acceptance stage

### 03-qa-stage
- Cross-repo QA stage

### 04-production-stage
- Cross-repo production stage

## 05-docs (project documentation)

### 01-project-documentation
- GitHub Pages / README docs

### 02-project-ticket-board
- Issue tracking setup

### 03-system-architecture
- Architecture style, tech stack, repo strategy
