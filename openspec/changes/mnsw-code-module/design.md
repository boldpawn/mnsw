# Design: MNSW Code Module

## Architectuurkeuze: Hexagonale Monoliet

Uit de parallel-plan vergelijking van vijf benaderingen (Vertical Slices, Event Sourcing, Hexagonale architectuur, Microservices, Pulsar-pipeline) is **Aanpak C — Hexagonale architectuur met type-specifieke payload-tabellen** gekozen.

**Motivering:**
- Type-specifieke payload-tabellen garanderen type-safety op DB-niveau en in Kotlin (sealed classes)
- Hexagonale structuur maakt de domeinlogica testbaar zonder Spring-context
- Versioning via `superseded_by` is eenvoudig en correct voor EMSWe-correctieflows
- Eén deployment unit is operationeel behapbaar voor dit team en deze fase

---

## Architectuuroverzicht

```
┌─────────────────────────────────────────────────────────────────┐
│                        MNSW Code Module                         │
│                                                                 │
│  ┌──────────────┐    ┌──────────────────────────────────────┐  │
│  │  Angular 21  │───▶│         REST API (Spring MVC)        │  │
│  │  Frontend    │    │  /api/v1/visits, /formalities, ...   │  │
│  └──────────────┘    └──────────────┬───────────────────────┘  │
│                                      │                           │
│  ┌──────────────┐    ┌──────────────▼───────────────────────┐  │
│  │  RIM Channel │───▶│       Application Layer              │  │
│  │  (Pulsar     │    │  SubmitFormalityUseCase               │  │
│  │   Consumer)  │    │  CorrectFormalityUseCase              │  │
│  └──────────────┘    │  ApproveFormalityUseCase              │  │
│                       │  GetFormalityUseCase                  │  │
│                       └──────────────┬───────────────────────┘  │
│                                      │                           │
│                       ┌──────────────▼───────────────────────┐  │
│                       │         Domain Layer                  │  │
│                       │  Formality (aggregate root)           │  │
│                       │  Visit                                │  │
│                       │  FormalityPayload (sealed)            │  │
│                       │  FormalityValidator                   │  │
│                       └──────────────┬───────────────────────┘  │
│                                      │                           │
│              ┌───────────────────────┼──────────────────┐       │
│              │                       │                  │       │
│  ┌───────────▼──────┐  ┌────────────▼──────┐  ┌────────▼───┐  │
│  │  PostgreSQL 16   │  │  Apache Pulsar    │  │  Security  │  │
│  │  JPA/Hibernate   │  │  Producer/Consumer│  │  JWT/Roles │  │
│  │  Flyway          │  │  Topics per type  │  │            │  │
│  └──────────────────┘  └───────────────────┘  └────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Pakketstructuur (backend)

```
src/main/kotlin/nl/mnsw/
├── config/
│   ├── SecurityConfig.kt         # Spring Security + JWT
│   ├── PulsarConfig.kt           # Pulsar topics & client config
│   └── WebConfig.kt              # CORS, Jackson, OpenAPI
│
├── formality/
│   ├── domain/
│   │   ├── Formality.kt          # Aggregate root (@Entity)
│   │   ├── FormalityStatus.kt    # Enum: SUBMITTED, ACCEPTED, REJECTED, UNDER_REVIEW, SUPERSEDED
│   │   ├── FormalityType.kt      # Enum: NOA, NOS, NOD, VID, SID
│   │   ├── payload/
│   │   │   ├── FormalityPayload.kt   # Sealed class
│   │   │   ├── NoaPayload.kt
│   │   │   ├── NosPayload.kt
│   │   │   ├── NodPayload.kt
│   │   │   ├── VidPayload.kt
│   │   │   └── SidPayload.kt
│   │   └── FormalityValidator.kt # Business rule validatie
│   │
│   ├── application/
│   │   ├── SubmitFormalityUseCase.kt
│   │   ├── CorrectFormalityUseCase.kt
│   │   ├── ApproveFormalityUseCase.kt   # Havenautoriteit: accept/reject
│   │   ├── GetFormalityUseCase.kt
│   │   └── ListFormalitiesUseCase.kt
│   │
│   └── infrastructure/
│       ├── persistence/
│       │   ├── FormalityRepository.kt
│       │   ├── FormalityJpaEntity.kt    # JPA entiteit (scheiding van domein)
│       │   ├── NoaPayloadJpaEntity.kt
│       │   ├── NosPayloadJpaEntity.kt
│       │   ├── NodPayloadJpaEntity.kt
│       │   ├── VidPayloadJpaEntity.kt
│       │   └── SidPayloadJpaEntity.kt
│       ├── messaging/
│       │   ├── FormalityPulsarProducer.kt  # Publiceert na opslaan
│       │   └── RimPulsarConsumer.kt        # Ontvangt van RIM
│       └── web/
│           ├── FormalityController.kt
│           ├── dto/
│           │   ├── SubmitFormalityRequest.kt
│           │   ├── FormalityResponse.kt
│           │   ├── NoaPayloadDto.kt
│           │   ├── NosPayloadDto.kt
│           │   ├── NodPayloadDto.kt
│           │   ├── VidPayloadDto.kt
│           │   ├── SidPayloadDto.kt
│           │   └── FrmResponseDto.kt
│           └── FormalityMapper.kt
│
├── visit/
│   ├── domain/
│   │   └── Visit.kt
│   ├── application/
│   │   ├── CreateVisitUseCase.kt
│   │   └── GetVisitUseCase.kt
│   └── infrastructure/
│       ├── persistence/
│       │   └── VisitRepository.kt
│       └── web/
│           └── VisitController.kt
│
├── auth/
│   ├── domain/
│   │   ├── User.kt
│   │   └── Role.kt               # Enum: SCHEEPSAGENT, LADINGAGENT, HAVENAUTORITEIT, ADMIN
│   ├── application/
│   │   ├── AuthenticateUseCase.kt
│   │   └── ManageUserUseCase.kt
│   └── infrastructure/
│       ├── persistence/
│       │   └── UserRepository.kt
│       └── web/
│           ├── AuthController.kt
│           └── UserController.kt
│
└── shared/
    ├── exception/
    │   ├── FormalityNotFoundException.kt
    │   ├── UnauthorizedAccessException.kt
    │   └── ValidationException.kt
    └── GlobalExceptionHandler.kt
```

---

## Datamodel

### Ontwerpprincipes

1. **Scheiding domein/JPA**: Kotlin domain classes zijn geen JPA-entiteiten. Aparte `*JpaEntity` klassen bevatten de JPA-annotaties. Expliciete mapping via `FormalityMapper`.
2. **Type-specifieke tabellen**: Elke formality type heeft een eigen payload-tabel (`noa_payload`, etc.). Geen JSONB voor payload — type-safety in DB en Kotlin.
3. **Versioning via superseded_by**: Een correctie maakt een nieuw `formality`-record aan. Het oude record krijgt `superseded_by = nieuw_id` en status `SUPERSEDED`.
4. **Immutable history**: Eenmaal ingediende formalities worden nooit overschreven.

### Centrale tabellen

```sql
-- visit: een havenbezoek
CREATE TABLE visit (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  imo_number VARCHAR(10) NOT NULL,
  vessel_name VARCHAR(255) NOT NULL,
  vessel_flag VARCHAR(3),
  port_locode VARCHAR(10) NOT NULL,
  eta TIMESTAMPTZ,
  etd TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- formality: supertype (alle velden die alle typen gemeen hebben)
CREATE TABLE formality (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  visit_id UUID NOT NULL REFERENCES visit(id),
  type formality_type NOT NULL,
  version INTEGER NOT NULL DEFAULT 1,
  status formality_status NOT NULL DEFAULT 'SUBMITTED',
  submitter_id UUID NOT NULL REFERENCES app_user(id),
  lrn VARCHAR(255),
  message_identifier VARCHAR(255) NOT NULL,
  submitted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  superseded_by UUID REFERENCES formality(id) NULL,
  channel submission_channel NOT NULL DEFAULT 'WEB'
);

CREATE TYPE formality_type AS ENUM ('NOA', 'NOS', 'NOD', 'VID', 'SID');
CREATE TYPE formality_status AS ENUM ('SUBMITTED', 'ACCEPTED', 'REJECTED', 'UNDER_REVIEW', 'SUPERSEDED');
CREATE TYPE submission_channel AS ENUM ('WEB', 'RIM');

-- frm_response: uitgaand antwoord per formality
CREATE TABLE frm_response (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  formality_id UUID NOT NULL REFERENCES formality(id),
  status frm_status NOT NULL,
  reason_code VARCHAR(50),
  reason_description TEXT,
  sent_at TIMESTAMPTZ,
  channel submission_channel NOT NULL
);

CREATE TYPE frm_status AS ENUM ('ACCEPTED', 'REJECTED', 'UNDER_REVIEW');
```

### Payload-tabellen (per formality type)

```sql
-- noa_payload
CREATE TABLE noa_payload (
  formality_id UUID PRIMARY KEY REFERENCES formality(id),
  expected_arrival TIMESTAMPTZ NOT NULL,
  last_port_locode VARCHAR(10),
  next_port_locode VARCHAR(10),
  purpose_of_call VARCHAR(255),
  persons_on_board INTEGER,
  dangerous_goods BOOLEAN DEFAULT false,
  waste_delivery BOOLEAN DEFAULT false,
  max_static_draught DECIMAL(5,2)
);

-- nos_payload
CREATE TABLE nos_payload (
  formality_id UUID PRIMARY KEY REFERENCES formality(id),
  actual_sailing TIMESTAMPTZ NOT NULL,
  next_port_locode VARCHAR(10),
  destination_country VARCHAR(3)
);

-- nod_payload
CREATE TABLE nod_payload (
  formality_id UUID PRIMARY KEY REFERENCES formality(id),
  expected_departure TIMESTAMPTZ NOT NULL,
  next_port_locode VARCHAR(10),
  destination_country VARCHAR(3),
  last_cargo_operations TIMESTAMPTZ
);

-- vid_payload
CREATE TABLE vid_payload (
  formality_id UUID PRIMARY KEY REFERENCES formality(id),
  certificate_nationality VARCHAR(255),
  gross_tonnage DECIMAL(10,2),
  net_tonnage DECIMAL(10,2),
  deadweight DECIMAL(10,2),
  length_overall DECIMAL(8,2),
  ship_type VARCHAR(100),
  call_sign VARCHAR(20),
  mmsi VARCHAR(15)
);

-- sid_payload (ISPS / Security)
CREATE TABLE sid_payload (
  formality_id UUID PRIMARY KEY REFERENCES formality(id),
  isps_level INTEGER NOT NULL CHECK (isps_level IN (1, 2, 3)),
  last_10_ports JSONB,  -- JSONB gerechtvaardigd: ongestructureerde lijst van havens
  security_declaration VARCHAR(50),
  ship_to_ship_activities BOOLEAN DEFAULT false,
  designated_authority VARCHAR(255),
  ssas_activated BOOLEAN DEFAULT false
);

-- app_user
CREATE TABLE app_user (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(255) NOT NULL,
  role user_role NOT NULL,
  port_locode VARCHAR(10),  -- alleen voor HAVENAUTORITEIT
  active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TYPE user_role AS ENUM ('SCHEEPSAGENT', 'LADINGAGENT', 'HAVENAUTORITEIT', 'ADMIN');
```

---

## Apache Pulsar Topics

| Topic | Richting | Doel |
|-------|----------|------|
| `mnsw.rim.inbound.noa` | Inkomend (RIM) | RIM-ingediende NOA berichten |
| `mnsw.rim.inbound.nos` | Inkomend (RIM) | RIM-ingediende NOS berichten |
| `mnsw.rim.inbound.nod` | Inkomend (RIM) | RIM-ingediende NOD berichten |
| `mnsw.rim.inbound.vid` | Inkomend (RIM) | RIM-ingediende VID berichten |
| `mnsw.rim.inbound.sid` | Inkomend (RIM) | RIM-ingediende SID berichten |
| `mnsw.formalities.submitted` | Intern | Gepubliceerd na opslaan; havenautoriteit-consumers luisteren |
| `mnsw.frm.outbound` | Uitgaand (RIM) | FRM-responses terug naar RIM-indieners |

**Subscription-model voor havenautoriteiten:**
- Elke havenautoriteit heeft een eigen subscription op `mnsw.formalities.submitted` met een key-shared consumer die filtert op `port_locode`
- Bij schaalgroei: aparte topics per haven (`mnsw.formalities.submitted.{locode}`)

---

## Pulsar Message Schemas

### FormalitySubmittedEvent (op `mnsw.formalities.submitted`)

```json
{
  "formalityId": "uuid",
  "visitId": "uuid",
  "type": "NOA",
  "portLocode": "NLRTM",
  "submittedAt": "2025-12-01T10:00:00Z",
  "channel": "WEB",
  "submitterId": "uuid",
  "messageIdentifier": "string"
}
```

### RimInboundMessage (op `mnsw.rim.inbound.{type}`)

```json
{
  "messageIdentifier": "string",
  "lrn": "string",
  "senderEori": "string",
  "payload": { /* type-specifieke velden conform MIG */ },
  "receivedAt": "2025-12-01T10:00:00Z"
}
```

---

## Validatiestrategie

Validatie verloopt in lagen:

1. **Bean Validation** (javax.validation): verplichte velden, formaten (IMO-nummer, LOCODE), datumbereiken
2. **Domeinvalidatie** (FormalityValidator): business rules conform MIG (bijv. expected_arrival mag niet in verleden liggen bij NOA)
3. **DB constraints**: foreign key constraints, NOT NULL, ENUM-types als vangnet

Bij validatiefouten: HTTP 422 met gestructureerde foutresponse + FRM met status REJECTED wordt gegenereerd voor RIM-indieners.

---

## Autorisatiemodel

| Rol | POST /formalities | GET eigen | GET alle (haven) | PUT approval |
|-----|------------------|-----------|-----------------|--------------|
| SCHEEPSAGENT | JA (eigen) | JA | NEE | NEE |
| LADINGAGENT | JA (eigen) | JA | NEE | NEE |
| HAVENAUTORITEIT | NEE | NEE | JA (eigen haven) | JA |
| ADMIN | JA | JA | JA | JA |

Autorisatie wordt afgedwongen met Spring Security `@PreAuthorize` annotaties op de use cases.

Havenautoriteit-filtering: de `port_locode` van de ingelogde gebruiker wordt vergeleken met de `port_locode` van de Visit. Spring Data query-methode: `findByVisitPortLocodeAndStatus(portLocode, status)`.

---

## Correctieworkflow

```
Indiener vraagt correctie aan (PUT /formalities/{id}/corrections)
  |
  +-- Nieuw formality-record aangemaakt (version = N+1, status = SUBMITTED)
  |   met zelfde visit_id, nieuw message_identifier
  |
  +-- Oud record: superseded_by = nieuw_id, status = SUPERSEDED
  |
  +-- Nieuwe formality doorloopt validatie en Pulsar-pipeline opnieuw
  |
  +-- FRM-response gestuurd voor nieuwe versie
```

Wie mag corrigeren: alleen de originele indiener (`submitter_id == authenticated_user_id`).

---

## API Response bij indiening

```
POST /api/v1/formalities
→ 202 Accepted
{
  "formalityId": "uuid",
  "messageIdentifier": "string",
  "status": "SUBMITTED",
  "statusUrl": "/api/v1/formalities/{formalityId}"
}
```

Status opvragen via polling: `GET /api/v1/formalities/{id}`. Geen WebSocket/SSE in fase 1.

---

## Frontend Architectuurkeuzes

- Angular 21 met standalone components (geen NgModules)
- Angular Material als component-bibliotheek met Rijkshuisstijl-kleuroverschrijvingen (zie `BRAND_GUIDELINES.md`)
- Angular Router met lazy-loaded routes per sectie (formalities, visits, users)
- Angular Signals voor state management (geen NgRx voor fase 1)
- HTTP Interceptors voor JWT-injectie en globale foutafhandeling
- Reactive Forms voor alle indiening- en correctieformulieren
- Environments: `environment.ts` (dev) en `environment.prod.ts`

---

## Tech Stack Overzicht

| Laag | Technologie | Versie |
|------|-------------|--------|
| Backend | Kotlin | 2.x |
| Backend framework | Spring Boot | 4.x |
| Beveiliging | Spring Security + JWT (JJWT) | — |
| ORM | Spring Data JPA / Hibernate | — |
| Database | PostgreSQL | 16 |
| Migraties | Flyway | 10.x |
| Messaging | Apache Pulsar | 3.x (Spring Pulsar) |
| Build | Maven | 3.9 |
| Frontend | Angular | 21 |
| UI-bibliotheek | Angular Material | 21 |
| Frontend build | Vite (via Angular DevKit) / Angular CLI | — |
| Testing backend | JUnit 5 + Mockito + Testcontainers | — |
| Testing frontend | Jest + Angular Testing Library | — |
| Dev Environment | Docker Compose | — |
