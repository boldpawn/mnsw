# Project Structure

Authoritative reference for this project's directory layout and API endpoints.

Use this to locate backend packages, frontend components/pages, and API routes for any feature area. Search by domain keyword (e.g., `formality`, `visit`, `user`, `auth`) to find related files.

---

## Repository Layout

```
mnsw/
├── CLAUDE.md                    # Project guidance for all agents
├── GLOSSARY.md                  # Ubiquitous language glossary
├── BRAND_GUIDELINES.md          # Rijkshuisstijl design system
├── PROJECT_STRUCTURE.md         # This file
├── docker-compose.yml           # Dev dependencies: PostgreSQL 16 + Pulsar 3.x
├── pom.xml                      # Maven parent POM
│
├── mnsw-backend/                # Spring Boot 4 Kotlin backend
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── kotlin/nl/mnsw/
│       │   │   ├── MnswApplication.kt
│       │   │   ├── config/
│       │   │   │   ├── SecurityConfig.kt
│       │   │   │   ├── PulsarConfig.kt
│       │   │   │   └── WebConfig.kt
│       │   │   ├── formality/
│       │   │   │   ├── domain/
│       │   │   │   │   ├── Formality.kt
│       │   │   │   │   ├── FormalityStatus.kt
│       │   │   │   │   ├── FormalityType.kt
│       │   │   │   │   ├── SubmissionChannel.kt
│       │   │   │   │   ├── FormalityValidator.kt
│       │   │   │   │   └── payload/
│       │   │   │   │       ├── FormalityPayload.kt    (sealed class)
│       │   │   │   │       ├── NoaPayload.kt
│       │   │   │   │       ├── NosPayload.kt
│       │   │   │   │       ├── NodPayload.kt
│       │   │   │   │       ├── VidPayload.kt
│       │   │   │   │       └── SidPayload.kt
│       │   │   │   ├── application/
│       │   │   │   │   ├── SubmitFormalityUseCase.kt
│       │   │   │   │   ├── CorrectFormalityUseCase.kt
│       │   │   │   │   ├── ApproveFormalityUseCase.kt
│       │   │   │   │   ├── RejectFormalityUseCase.kt
│       │   │   │   │   ├── GetFormalityUseCase.kt
│       │   │   │   │   └── ListFormalitiesUseCase.kt
│       │   │   │   └── infrastructure/
│       │   │   │       ├── persistence/
│       │   │   │       │   ├── FormalityRepository.kt
│       │   │   │       │   ├── FormalityJpaEntity.kt
│       │   │   │       │   ├── NoaPayloadJpaEntity.kt
│       │   │   │       │   ├── NosPayloadJpaEntity.kt
│       │   │   │       │   ├── NodPayloadJpaEntity.kt
│       │   │   │       │   ├── VidPayloadJpaEntity.kt
│       │   │   │       │   └── SidPayloadJpaEntity.kt
│       │   │   │       ├── messaging/
│       │   │   │       │   ├── FormalityPulsarProducer.kt
│       │   │   │       │   ├── RimPulsarConsumer.kt
│       │   │   │       │   └── FrmPulsarProducer.kt
│       │   │   │       └── web/
│       │   │   │           ├── FormalityController.kt
│       │   │   │           ├── FormalityMapper.kt
│       │   │   │           └── dto/
│       │   │   │               ├── SubmitFormalityRequest.kt
│       │   │   │               ├── FormalityResponse.kt
│       │   │   │               ├── NoaPayloadDto.kt
│       │   │   │               ├── NosPayloadDto.kt
│       │   │   │               ├── NodPayloadDto.kt
│       │   │   │               ├── VidPayloadDto.kt
│       │   │   │               ├── SidPayloadDto.kt
│       │   │   │               └── FrmResponseDto.kt
│       │   │   ├── visit/
│       │   │   │   ├── domain/Visit.kt
│       │   │   │   ├── application/
│       │   │   │   │   ├── CreateVisitUseCase.kt
│       │   │   │   │   └── GetVisitUseCase.kt
│       │   │   │   └── infrastructure/
│       │   │   │       ├── persistence/
│       │   │   │       │   ├── VisitRepository.kt
│       │   │   │       │   └── VisitJpaEntity.kt
│       │   │   │       └── web/VisitController.kt
│       │   │   ├── auth/
│       │   │   │   ├── domain/
│       │   │   │   │   ├── User.kt
│       │   │   │   │   └── Role.kt
│       │   │   │   ├── application/
│       │   │   │   │   ├── AuthenticateUseCase.kt
│       │   │   │   │   └── ManageUserUseCase.kt
│       │   │   │   └── infrastructure/
│       │   │   │       ├── persistence/
│       │   │   │       │   ├── UserRepository.kt
│       │   │   │       │   └── UserJpaEntity.kt
│       │   │   │       ├── security/
│       │   │   │       │   └── JwtAuthenticationFilter.kt
│       │   │   │       └── web/
│       │   │   │           ├── AuthController.kt
│       │   │   │           └── UserController.kt
│       │   │   └── shared/
│       │   │       ├── exception/
│       │   │       │   ├── FormalityNotFoundException.kt
│       │   │       │   ├── UnauthorizedAccessException.kt
│       │   │       │   ├── ValidationException.kt
│       │   │       │   └── ConcurrentCorrectionException.kt
│       │   │       └── GlobalExceptionHandler.kt
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-dev.yml
│       │       ├── application-test.yml
│       │       └── db/migration/
│       │           ├── V1__create_visit_table.sql
│       │           ├── V2__create_formality_table.sql
│       │           ├── V3__create_noa_payload_table.sql
│       │           ├── V4__create_nos_payload_table.sql
│       │           ├── V5__create_nod_payload_table.sql
│       │           ├── V6__create_vid_payload_table.sql
│       │           ├── V7__create_sid_payload_table.sql
│       │           ├── V8__create_frm_response_table.sql
│       │           ├── V9__create_app_user_table.sql
│       │           └── V10__create_indexes.sql
│       └── test/kotlin/nl/mnsw/
│           ├── formality/
│           │   ├── FormalityValidatorTest.kt
│           │   ├── SubmitFormalityUseCaseTest.kt
│           │   └── FormalityControllerIT.kt
│           └── TestFixtures.kt
│
├── mnsw-frontend/               # Angular 21 frontend
│   ├── angular.json
│   ├── package.json
│   └── src/
│       ├── app/
│       │   ├── core/
│       │   │   ├── auth/
│       │   │   │   ├── auth.service.ts
│       │   │   │   ├── auth.guard.ts
│       │   │   │   └── jwt.interceptor.ts
│       │   │   ├── api/
│       │   │   │   ├── formality.service.ts
│       │   │   │   ├── visit.service.ts
│       │   │   │   └── user.service.ts
│       │   │   └── models/
│       │   │       ├── formality.model.ts
│       │   │       ├── visit.model.ts
│       │   │       └── user.model.ts
│       │   ├── shared/
│       │   │   ├── components/
│       │   │   │   ├── status-badge/
│       │   │   │   ├── page-header/
│       │   │   │   ├── breadcrumb/
│       │   │   │   ├── confirm-dialog/
│       │   │   │   └── error-display/
│       │   │   └── pipes/
│       │   │       ├── formality-type-label.pipe.ts
│       │   │       └── truncate-uuid.pipe.ts
│       │   ├── layout/
│       │   │   ├── app-layout/
│       │   │   ├── sidebar/
│       │   │   └── header/
│       │   ├── features/
│       │   │   ├── auth/login/
│       │   │   ├── formalities/
│       │   │   │   ├── formality-list/
│       │   │   │   ├── formality-detail/
│       │   │   │   ├── formality-submit/
│       │   │   │   │   ├── noa-form/
│       │   │   │   │   ├── nos-form/
│       │   │   │   │   ├── nod-form/
│       │   │   │   │   ├── vid-form/
│       │   │   │   │   └── sid-form/
│       │   │   │   └── formality-correct/
│       │   │   ├── visits/
│       │   │   └── users/
│       │   ├── app.routes.ts
│       │   └── app.component.ts
│       ├── assets/
│       │   ├── rijkshuisstijl-logo.svg
│       │   └── i18n/nl.json
│       ├── styles/
│       │   ├── _rijkshuisstijl.scss
│       │   ├── _buttons.scss
│       │   ├── _forms.scss
│       │   ├── _tables.scss
│       │   ├── _badges.scss
│       │   └── styles.scss
│       └── environments/
│           ├── environment.ts
│           └── environment.prod.ts
│
└── openspec/
    ├── e2e-test-plan.md
    └── changes/
        └── mnsw-code-module/
            ├── proposal.md
            ├── design.md
            ├── tasks.md
            └── specs/
                ├── backend.md
                ├── frontend.md
                ├── data-model.md
                └── api.md
```

---

## API Endpoints

| Method | Path | Auth | Rol | Beschrijving |
|--------|------|------|-----|-------------|
| POST | /api/v1/auth/login | Nee | — | Inloggen, JWT ophalen |
| GET | /api/v1/visits | JWT | Alle rollen | Lijst van havenbezoeken |
| GET | /api/v1/visits/{id} | JWT | Alle rollen | Detail havenbezoek |
| POST | /api/v1/formalities | JWT | AGENT, ADMIN | Nieuwe formality indienen |
| GET | /api/v1/formalities | JWT | Alle rollen | Lijst formalities (gefilterd op rol) |
| GET | /api/v1/formalities/{id} | JWT | Alle rollen | Detail formality + payload + FRM |
| POST | /api/v1/formalities/{id}/corrections | JWT | AGENT, ADMIN | Correctie indienen |
| PUT | /api/v1/formalities/{id}/review | JWT | AUTHORITY, ADMIN | Status -> UNDER_REVIEW |
| PUT | /api/v1/formalities/{id}/approve | JWT | AUTHORITY, ADMIN | Goedkeuren formality |
| PUT | /api/v1/formalities/{id}/reject | JWT | AUTHORITY, ADMIN | Afwijzen formality |
| GET | /api/v1/users | JWT | ADMIN | Lijst gebruikers |
| POST | /api/v1/users | JWT | ADMIN | Nieuwe gebruiker aanmaken |
| PUT | /api/v1/users/{id} | JWT | ADMIN | Gebruiker bewerken |
| DELETE | /api/v1/users/{id} | JWT | ADMIN | Gebruiker deactiveren |
| GET | /actuator/health | Nee | — | Applicatie health check |

## Frontend Routes

| Route | Component | Toegang |
|-------|-----------|---------|
| /login | LoginComponent | Publiek |
| /formalities | FormalityListComponent | Alle rollen |
| /formalities/new | FormalitySubmitComponent | AGENT, ADMIN |
| /formalities/new/:type | {Type}FormComponent | AGENT, ADMIN |
| /formalities/:id | FormalityDetailComponent | Alle rollen |
| /formalities/:id/correct | FormalityCorrectComponent | AGENT (eigen), ADMIN |
| /visits | VisitListComponent | Alle rollen |
| /visits/:id | VisitDetailComponent | Alle rollen |
| /users | UserListComponent | ADMIN |
| /users/new | UserFormComponent | ADMIN |
| /users/:id | UserFormComponent | ADMIN |
