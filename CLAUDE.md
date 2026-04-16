# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## ⛔ MANDATORY FIRST-RUN GATE — Read This Before Doing ANYTHING

**STOP. Before responding to ANY user request, check this file for `TODO` comments.**

If ANY `TODO` comments remain in this file, you MUST complete ALL of the following steps before doing any other work — no exceptions:

1. **Scan the codebase**: Read `pom.xml`, `package.json`, `build.gradle`, `docker-compose.yml`, directory structure, source files, and any other config files to learn the project.
2. **EDIT this file**: Use the Edit tool to replace every `TODO` placeholder in this file with the actual values you discovered. Write the real tech stack, real build commands, real project description, real conventions — directly into this file.
3. **Fill PROJECT_STRUCTURE.md**: If `PROJECT_STRUCTURE.md` exists and has TODO placeholders, edit that file too with the real project tree and API endpoints.
4. **Fill GLOSSARY.md**: If `GLOSSARY.md` exists and has TODO placeholders, add the domain terms you discovered from the codebase.
5. **Know your toolkit**: Read the agent definitions in `.claude/agents/` and skill definitions in `.claude/skills/`. Understand what each agent does and when to invoke it before starting any work.

**WHY THIS IS NON-NEGOTIABLE:**
- Other agents and teammates read this file for context. If the TODOs remain unfilled, every agent you spawn will lack critical project information.
- Your memory is NOT a substitute for editing this file. Memory is local to your session — teammates cannot access it.
- "I already know this" is not an acceptable reason to skip editing. The file must contain the actual values.

**GATE CHECK:** After completing the steps above, re-read this file and confirm zero `TODO` comments remain. Only then proceed with the user's request.

## Project Overview

MNSW (Maritime National Single Window) is the Dutch maritime port notification system for the EMSWe (European Maritime Single Window environment, EU Directive 2019/1239/EU). It receives vessel reporting formalities (NOA, NOS, NOD, VID, SID) from scheepsagenten and ladingagenten via an Angular 21 web frontend and via the RIM (Remote Interface Module) using the AS4/eDelivery protocol over Apache Pulsar. MNSW validates, stores, and routes these formalities to the relevant Dutch port authorities, and returns FRM (Formality Response Message) responses to submitters.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Kotlin 2.x, Spring Boot 4.x, Spring Security + JJWT |
| Frontend | Angular 21, TypeScript 5.x, Angular Material (Rijkshuisstijl theme), SCSS |
| Database | PostgreSQL 16, Flyway 10.x (migrations) |
| Messaging | Apache Pulsar 3.x (Spring for Apache Pulsar) |
| Testing | JUnit 5 + Mockito + Testcontainers (backend), Jest + Angular Testing Library (frontend) |
| Build | Maven 3.9 (backend), Angular CLI (frontend) |
| Dev Environment | Docker Compose (PostgreSQL 16 + Pulsar 3.x) |

## Build Commands

```bash
# Build backend (from mnsw-backend/)
mvn clean install

# Run all backend tests
mvn test

# Run backend tests with Testcontainers (requires Docker)
mvn verify

# Start dev dependencies (PostgreSQL + Pulsar)
docker compose up -d

# Start backend (from mnsw-backend/)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Frontend (from mnsw-frontend/)
npm install
ng build
ng serve          # dev server op http://localhost:4200
ng test           # Jest unit tests
ng test --watch   # watch mode
```

## Testing

- Always run the full test suite after making changes
- After fixing one bug, verify no regressions were introduced before moving on
- When writing controller tests, check if the test security config has specific auth behavior

## Project Structure

See **[PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md)** for the full project tree and API endpoints. Search by domain keyword to locate any file.

## Project-Specific Conventions

- EMSA EMSWe MIG v2.0.1 (16/12/2025) is de leidende standaard voor alle datamodellen
- Formality type codes zijn altijd 3 letters: NOA, NOS, NOD, VID, SID (inkomend); FRM is uitkomend antwoord
- Visit ID is de centrale correlatie-sleutel voor alle berichten binnen een havenbezoek
- LRN (Local Reference Number) is de door de indiener gekozen correlatie-sleutel
- Message Identifier in MAI-header correleert verzoek met antwoord (FRM response)
- Alle datumvelden gebruiken ISO 8601 met tijdzone-aanduiding (conform EMSA spec)
- Hexagonale architectuur: domeinlaag heeft GEEN Spring-afhankelijkheden; JPA-entiteiten zijn aparte klassen
- Versioning via `superseded_by` zelfverwijzing; nooit bestaande formalities overschrijven
- REST endpoints antwoorden met 202 Accepted bij formality-indiening (async verwerking)
- Angular 21: standalone components only (geen NgModules); Signals voor state management
- Alle visuele code MOET `BRAND_GUIDELINES.md` volgen (Nederlandse Rijkshuisstijl)

### Frontend Design Rules

**MANDATORY**: When implementing or modifying ANY frontend visual code (components, pages, layouts, styles), you MUST:

1. Read `skills/frontend-design/SKILL.md` and follow its instructions BEFORE writing any UI code
2. Read `skills/ui-ux-pro-max/SKILL.md` and follow its instructions with `--design-system --persist` to generate brand guidelines
3. Persist the output as `BRAND_GUIDELINES.md` in the project root
4. Reference `BRAND_GUIDELINES.md` for all color palettes, font pairings, spacing, and UX patterns
5. Never output plain/generic styling — every component must reflect the brand guidelines

`BRAND_GUIDELINES.md` is the single source of truth for visual design decisions. All frontend agents and reviewers reference it. If it doesn't exist when frontend visual code is being written, that is a CRITICAL review finding.

## Roles

| Rol | Toegang |
|-----|---------|
| Scheepsagent | Eigen ingediende formalities lezen en wijzigen |
| Ladingagent | Eigen ingediende formalities lezen en wijzigen |
| Havenautoriteit | Formalities lezen voor hun havens, CLR beslissingen nemen |
| ADMIN | Alles |

Alleen de indiener (scheepsagent of ladingagent die het origineel heeft ingediend) mag wijzigingen sturen.

## Key References

- **GLOSSARY.md** -- Ubiquitous Language glossary. Consult before using domain terms in specs, designs, and code.
- **PROJECT_STRUCTURE.md** -- Full project tree + all API endpoints. Search by domain keyword to locate any file.
- **BRAND_GUIDELINES.md** -- Design system (colors, fonts, spacing, component patterns). Generated by following the ui-ux-pro-max skill with `--design-system --persist`. Required before any frontend visual work.
- **EMSA MIG v2.0.1** — https://emsa.europa.eu/emswe-mig/ — officieel datamodel voor alle formalities
- **NEXT-STEPS.md** — AI Accelerator setup checklist

---

## Agent Teams

This project uses custom AI agents that work together as a team. The lead agent (Claude Code) orchestrates the pipeline -- it does NOT implement code itself but distributes tasks to sub-agents.

### Agent Architecture

Agents are markdown files in `.claude/agents/` that define specialized roles. They are spawned as sub-agents via the `Agent` tool. Sub-agents use skills by reading the skill's SKILL.md file and following its instructions -- they do NOT have access to a `Skill()` tool.

### How Agents Use Skills

Skills are `.md` files in `.claude/skills/`. Agents use them by:
1. Reading the skill file (e.g., `skills/design-grill/SKILL.md`)
2. Following its instructions completely — executing the full process described in that file

Agents must NOT just summarize or paraphrase a skill. They must read and execute.

### Human-in-the-Loop Rule (CRITICAL)

**NEVER:**
- Answer your own design questions or auto-approve decisions
- Assume you know what the human would choose
- Skip asking the human because the answer seems obvious

**ALWAYS:**
- Present decisions and options directly to the human
- Wait for the human's explicit response before proceeding
- Confirm alignment before moving to the next phase

### Available Agents

| Agent | Role | When to Use |
|-------|------|-------------|
| `design-flow` | Full design phase: explore, stress-test, glossary alignment, spec generation | Before implementation — when a feature needs design |
| `workflow-orchestrator` | Pipeline orchestration, team coordination, glossary enforcement | Full Pipeline Mode — orchestrates all other agents, enforces glossary term consistency |
| `backend-dev` | Back-end implementation + unit tests | Any back-end work (reads tech stack from CLAUDE.md) |
| `frontend-dev` | Frontend implementation + unit/e2e tests | Any frontend work (reads tech stack from CLAUDE.md) |
| `full-e2e-test` | E2E test audit + Playwright execution | When OpenSpec + frontend are enabled |
| `code-review-final` | Code quality review of PRs | After all implementation is complete |
| `security-review-final` | Security review of PRs (conditional) | Only when diff touches security-sensitive files |

### Security Review Trigger

Run `security-review-final` only when the diff includes files matching ANY of:
- `**/config/Security*`, `**/config/Cors*`, `**/config/Jwt*`, `**/config/Auth*`
- `**/auth/**`, `**/filter/**`
- Any new `@PreAuthorize` or `@Secured` annotation added
- Any new controller class (new attack surface)
- `application.yml`, `application-*.yml` (config changes)
- `pom.xml`, `package.json`, `build.gradle` (dependency changes -- supply chain risk)
- Any file with `password`, `secret`, `token`, `credential` in its path

<!--
  Add your tech-stack-specific security trigger patterns below. Examples:
  - Dockerfile, docker-compose.yml (container security)
  - nginx.conf, .htaccess (web server config)
  - Kubernetes manifests (deployment security)
-->

Skip security review when the diff only touches: service logic, DTOs, repositories, frontend components, tests, migrations, README/docs.

### How to Use the Agents

#### Assisted Mode (small changes, bug fixes)

For changes under ~5 files or single-module work, use agents directly without orchestration:

1. Spawn the appropriate implementation agent (e.g., `backend-dev`)
2. Agent implements + tests + commits
3. Run build verification
4. Read `skills/review-local/SKILL.md` and follow its instructions on the changed code
5. Spawn `code-review-final` agent
6. Fix review comments, push, merge

#### Full Pipeline Mode (features, multi-module changes)

For larger features, spawn the `workflow-orchestrator` agent. It will:

- Distribute tasks to implementation agents in parallel
- Run build verification, pre-commit quality checks, and local review
- Create the PR and spawn `code-review-final` (and `security-review-final` if the diff touches security-sensitive files)
- Spawn Playwright e2e test agents if the project has a frontend
- For optional or ambiguous steps, ask the human before executing

See the `workflow-orchestrator` agent definition for the full pipeline details.

### Available Skills

Skills are invoked by reading their SKILL.md file and following the instructions. Available in `.claude/skills/`:

- `design-grill` — Stress-test design decisions
- `parallel-plan` — Fan-out parallel approach comparison
- `glossary` — Ubiquitous language glossary management
- `review-local` — 4-agent parallel local code review
- `gitlab-code-review` — PR review via VCS CLI
- `playwright-cli` — Browser automation for e2e testing
- `frontend-design` — Component-level design guidance
- `ui-ux-pro-max` — UX patterns, interaction design, design system
- `write-simply` — Plain language writing
- `structure-clearly` — Pyramid principle document structure
- `retro` — Retrospective on the change

### Key Rules

- **Sweep enforcement**: Every review finding includes a grep command. Fix agents MUST run the sweep and fix ALL matches -- not just the reported file
- **Cross-stack contract alignment**: When backend and frontend run in parallel, the frontend API service task MUST read the actual backend DTOs before writing interfaces
- **PR Key Decisions**: Include a "Key Decisions" section in PR descriptions listing intentional design choices. Prevents review agents from flagging them as bugs

## Code Review Workflow

- When reviewing PRs, structure findings by category: security, efficiency, code quality, reuse
- Post findings directly to PR via `gh` CLI (or `gh` for GitHub)
- Two review passes max -- after pass 2, the PR is considered ready for merge

## Docker & Database

- Docker Compose start PostgreSQL 16 op poort 5432 en Apache Pulsar 3.x op poort 6650
- Database naam: `mnsw`, gebruiker: `mnsw`, wachtwoord: `mnsw` (development)
- Flyway-migraties draaien automatisch bij applicatiestart (locatie: `src/main/resources/db/migration/`)
- Schema-wijzigingen ALTIJD via Flyway — nooit `ddl-auto: create` of `ddl-auto: update`
- Testcontainers worden gebruikt voor integratietests (vereist Docker draaiend op dev-machine)
- Pulsar standalone mode in Docker Compose voor development
