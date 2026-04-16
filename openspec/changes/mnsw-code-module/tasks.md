# Tasks: MNSW Code Module

Implementatietaken voor backend-dev en frontend-dev agents.

Lees `design.md`, `specs/backend.md`, `specs/frontend.md`, `specs/data-model.md` en `specs/api.md` voordat je begint.
Frontend-agents: lees ook `BRAND_GUIDELINES.md` — dit is verplicht voor alle visuele code.

---

## FASE 1 — Backend MVP

- [x] B01: Maven/Gradle projectstructuur en dependencies (pom.xml, docker-compose.yml, application*.yml)
- [x] B02: Flyway migraties — databaseschema (V1 visit, V2 formality, V3-V7 payload tabellen, V8 frm_response, V9 app_user, V10 indexen)
- [x] B03: Domeinlaag en sealed classes (Formality, Visit, FormalityPayload sealed class + 5 subklassen, JPA-entiteiten, FormalityMapper)
- [x] B04: Repositories (FormalityRepository, VisitRepository, UserRepository + Testcontainers integratietests)
- [x] B05: Validatielogica — FormalityValidator met MIG-regels per type + unit tests
- [x] B06: Use cases (SubmitFormality, CorrectFormality, ApproveFormality, RejectFormality, GetFormality, ListFormalities, CreateVisit, GetVisit, Authenticate)
- [x] B07: REST controllers en Spring Security (FormalityController, VisitController, AuthController, UserController, SecurityConfig, JwtAuthenticationFilter, GlobalExceptionHandler + integratietests)
- [x] B08: Pulsar integratie (PulsarConfig, FormalityPulsarProducer, RimPulsarConsumer, FrmPulsarProducer + unit tests)

## FASE 1 — Frontend MVP

- [x] F01: Angular 21 project setup (standalone components, Angular Material, Rijkshuisstijl SCSS, environment files)
- [x] F02: Core services en TypeScript modellen (formality.model.ts, visit.model.ts, user.model.ts, auth.service.ts, jwt.interceptor.ts, auth.guard.ts, formality.service.ts, visit.service.ts)
- [x] F03: Layout en navigatie (AppLayoutComponent, SidebarComponent, HeaderComponent, BreadcrumbComponent, routing met lazy loading, LoginComponent)
- [x] F04: Gedeelde components (StatusBadgeComponent, PageHeaderComponent, ConfirmDialogComponent, ErrorDisplayComponent, FormalityTypeLabelPipe, TruncateUuidPipe)
- [x] F05: Formality overzicht (FormalityListComponent met filters, paginering, rolgebaseerde weergave)
- [x] F06: Formality detail (FormalityDetailComponent met 4 tabbladen: Algemeen, Payload, Versiehistorie, FRM Response)
- [x] F07: Indiening formulieren NOA/NOS/NOD (FormalitySubmitComponent, NoaFormComponent, NosFormComponent, NodFormComponent, VisitFormSectionComponent)
- [x] F08: Indiening formulieren VID/SID + correctieworkflow (VidFormComponent, SidFormComponent met dynamisch FormArray, FormalityCorrectComponent)

## FASE 2 — Aanvullende taken (later)

- [ ] B09: Gebruikersbeheer endpoints (ADMIN-endpoints voor aanmaken/beheren gebruikers)
- [ ] F09: Gebruikersbeheer schermen (UserListComponent, UserFormComponent — alleen voor ADMIN)
- [ ] B10: RIM AS4 Consumer (volledige AS4/eDelivery integratie)

---

## Taakvolgorde en Parallelisatie

```
Backend:   B01 → B02+B03 (parallel) → B04+B05 (parallel) → B06 → B07+B08 (parallel)
Frontend:  F01 → F02 → F03+F04 (parallel) → F05+F06 (parallel) → F07+F08 (parallel)
Backend en Frontend kunnen parallel lopen na B03 (API-contracten zijn dan bekend)
```

**Contractafstemming:** Frontend-agent MOET `specs/api.md` en `specs/data-model.md` lezen voordat TypeScript interfaces worden geschreven.

---

## Definition of Done per taak

1. Code compileert zonder fouten
2. Alle unit tests slagen (`mvn test` of `ng test`)
3. Integratietests slagen (backend: Testcontainers)
4. Code volgt hexagonale architectuurprincipes (backend) / standalone component-patroon (frontend)
5. Geen TODO-commentaar in code
6. Alle REST endpoints werken conform `specs/api.md` (backend)
7. Alle UI-elementen volgen `BRAND_GUIDELINES.md` (frontend)
