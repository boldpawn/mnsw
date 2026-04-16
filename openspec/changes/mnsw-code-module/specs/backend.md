# Spec: Backend

## Overzicht

Kotlin + Spring Boot 4, hexagonale architectuur, PostgreSQL 16, Apache Pulsar 3.x.

Zie `design.md` voor architectuurkeuzes en pakketstructuur.
Zie `data-model.md` voor entiteiten en databaseschema.
Zie `api.md` voor alle REST-endpoints.

---

## Tech Stack (backend)

| Component | Technologie | Versie |
|-----------|-------------|--------|
| Taal | Kotlin | 2.x |
| Framework | Spring Boot | 4.x |
| ORM | Spring Data JPA / Hibernate | 6.x |
| Database | PostgreSQL | 16 |
| Migraties | Flyway | 10.x |
| Messaging | Apache Pulsar (Spring for Apache Pulsar) | 3.x |
| Beveiliging | Spring Security + JJWT | — |
| Build | Maven | 3.9 |
| API docs | springdoc-openapi | 2.x |
| Testing | JUnit 5 + Mockito + Testcontainers | — |

---

## Maven Module Structuur

```
mnsw/
├── pom.xml                  # Parent POM
├── mnsw-backend/            # Spring Boot applicatie
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── kotlin/nl/mnsw/
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-dev.yml
│       │       ├── application-test.yml
│       │       └── db/migration/
│       └── test/
│           └── kotlin/nl/mnsw/
└── mnsw-frontend/           # Angular 21
    └── ...
```

---

## Spring Boot Applicatie Configuratie

### application.yml (base)

```yaml
spring:
  application:
    name: mnsw
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/mnsw}
    username: ${DB_USER:mnsw}
    password: ${DB_PASSWORD:mnsw}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true
    locations: classpath:db/migration
  pulsar:
    client:
      service-url: ${PULSAR_URL:pulsar://localhost:6650}

security:
  jwt:
    secret: ${JWT_SECRET}
    expiration-hours: 8

server:
  port: 8080

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

### application-dev.yml

```yaml
spring:
  jpa:
    show-sql: true
    
springdoc:
  swagger-ui:
    enabled: true
```

---

## Beveiliging (SecurityConfig)

```kotlin
// Pseudocode structuur — implementatie door backend-dev agent

@Configuration
@EnableMethodSecurity
class SecurityConfig {
    
    // JWT filter chain:
    // - /api/auth/login: permit all
    // - /actuator/health: permit all
    // - overige /api/**: authenticated
    
    // JwtAuthenticationFilter: leest Bearer token uit header,
    // valideert, zet SecurityContext
    
    // Roltoewijzing:
    // SCHEEPSAGENT -> ROLE_AGENT
    // LADINGAGENT -> ROLE_AGENT
    // HAVENAUTORITEIT -> ROLE_AUTHORITY
    // ADMIN -> ROLE_ADMIN
}
```

### @PreAuthorize annotaties (op use cases)

```kotlin
// SubmitFormalityUseCase:
@PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")

// ApproveFormalityUseCase:
@PreAuthorize("hasAnyRole('AUTHORITY', 'ADMIN')")

// CorrectFormalityUseCase:
@PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
// + programmatische check: authenticated user == submitter_id

// ListFormalitiesUseCase:
// geen @PreAuthorize — filtering in use case zelf op basis van rol
```

---

## Use Cases

### SubmitFormalityUseCase

**Input:** `SubmitFormalityCommand(visitId, type, lrn, messageIdentifier, payload: FormalityPayload, submitterId, channel)`

**Stappen:**
1. Haal Visit op via visitId (gooi `VisitNotFoundException` indien niet gevonden)
2. Valideer payload conform MIG-regels voor het gegeven type (gooi `ValidationException` met veldfouten)
3. Sla formality + type-specifieke payload op in PostgreSQL (transactie)
4. Publiceer `FormalitySubmittedEvent` op Pulsar topic `mnsw.formalities.submitted`
5. Geef `SubmitFormalityResult(formalityId, messageIdentifier, status=SUBMITTED)` terug

**Transactiegrens:** stap 1-3 in één transactie. Pulsar-publicatie (stap 4) buiten transactie, after-commit.

### CorrectFormalityUseCase

**Input:** `CorrectFormalityCommand(originalFormalityId, lrn, messageIdentifier, payload, submitterId)`

**Stappen:**
1. Haal originele formality op
2. Controleer dat `submitterId == originalFormality.submitterId` (anders `UnauthorizedAccessException`)
3. Controleer dat er geen actievere versie bestaat (anders `ConcurrentCorrectionException`)
4. Valideer nieuwe payload
5. In één transactie:
   - Maak nieuwe formality aan (version = origineel.version + 1)
   - Update originele formality: `superseded_by = nieuw id`, `status = SUPERSEDED`
6. Publiceer event op Pulsar

### ApproveFormalityUseCase

**Input:** `ApproveFormalityCommand(formalityId, reviewerUserId)`

**Stappen:**
1. Haal formality op
2. Controleer haven-autorisatie: `formality.visit.portLocode == reviewer.portLocode`
3. Zet status op `ACCEPTED`
4. Genereer en sla FRM response op (status=ACCEPTED)
5. Publiceer FRM via `mnsw.frm.outbound` (voor RIM-indieners)
6. Geef resultaat terug

### RejectFormalityUseCase

**Input:** `RejectFormalityCommand(formalityId, reasonCode, reasonDescription, reviewerUserId)`

Zelfde als approve maar status=REJECTED, FRM met foutcode.

---

## Validatielogica (FormalityValidator)

Per formality type gelden specifieke MIG-validatieregels:

**NOA:**
- `expectedArrival` moet in de toekomst liggen (of max 24 uur geleden)
- `imoNumber` op Visit moet voldoen aan IMO-checksum algoritme
- `portLocode` op Visit moet een geldig UN/LOCODE formaat hebben (2 letters + 3 alfanumeriek)
- `personsOnBoard` >= 0 indien opgegeven
- `maxStaticDraught` >= 0 en < 50 meter

**NOS:**
- `actualSailing` mag niet meer dan 48 uur in de toekomst liggen

**NOD:**
- `expectedDeparture` moet in de toekomst liggen

**SID:**
- `ispsLevel` moet 1, 2 of 3 zijn
- Indien `ispsLevel` = 2 of 3: `designatedAuthority` is verplicht

**VID:**
- `mmsi` als opgegeven: exact 9 cijfers
- `callSign` als opgegeven: 3-7 alfanumerieke tekens

---

## Pulsar Configuratie

### Topics (automatisch aangemaakt bij eerste gebruik)

```
Persistent topics (retentie 7 dagen):
- persistent://mnsw/default/rim-inbound-noa
- persistent://mnsw/default/rim-inbound-nos
- persistent://mnsw/default/rim-inbound-nod
- persistent://mnsw/default/rim-inbound-vid
- persistent://mnsw/default/rim-inbound-sid
- persistent://mnsw/default/formalities-submitted
- persistent://mnsw/default/frm-outbound
```

### RimPulsarConsumer

Luistert op `rim-inbound-*` topics. Bij binnenkomst:
1. Deserialiseer RimInboundMessage (JSON)
2. Verifieer handtekening / zenderidentiteit (toekomstige fase)
3. Roep `SubmitFormalityUseCase` aan met `channel=RIM`
4. Bij succes: acknowledge bericht
5. Bij fout: negative-acknowledge (retry na backoff)

### FormalityPulsarProducer

Publiceert `FormalitySubmittedEvent` na succesvolle opslag.
Schematype: JSON. Async send met callback voor foutafhandeling.

---

## Testen

### Unit tests (domeinlogica)
- Test `FormalityValidator` voor alle validatieregels per type
- Test use cases met gemockte repositories en Pulsar producer
- Test `FormalityMapper` DTO <-> domein mappings
- Test `Formality` aggregate business rules

### Integratietests (met Testcontainers)
- `@SpringBootTest` met echte PostgreSQL container (via Testcontainers)
- Test volledige submit-flow: POST /formalities -> DB -> Pulsar event gepubliceerd
- Test correctieworkflow: submit -> correctie -> versiehistorie correct
- Test autorisatie: agent mag geen havenautoriteit-acties uitvoeren
- Pulsar mock of embedded voor integratietests (geen echte Pulsar container nodig)

### Testconventies
- Testnaam: `should{Actie}When{Conditie}` in Nederlands of Engels (kies één, consistent)
- Test data via `@Sql` scripts of factory methods in `TestFixtures.kt`
- Geen in-memory H2 — altijd echte PostgreSQL via Testcontainers
- `application-test.yml` met Testcontainers-compatible datasource URL

---

## Docker Compose (development)

```yaml
# docker-compose.yml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: mnsw
      POSTGRES_USER: mnsw
      POSTGRES_PASSWORD: mnsw
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  pulsar:
    image: apachepulsar/pulsar:3.3.0
    command: bin/pulsar standalone
    ports:
      - "6650:6650"
      - "8081:8080"

volumes:
  postgres_data:
```

---

## Foutafhandeling (GlobalExceptionHandler)

```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {
    
    @ExceptionHandler(ValidationException::class)
    fun handleValidation(e: ValidationException): ResponseEntity<ErrorResponse>
    // -> 422 met lijst van veldfouten
    
    @ExceptionHandler(FormalityNotFoundException::class)
    fun handleNotFound(e: FormalityNotFoundException): ResponseEntity<ErrorResponse>
    // -> 404

    @ExceptionHandler(UnauthorizedAccessException::class)
    fun handleUnauthorized(e: UnauthorizedAccessException): ResponseEntity<ErrorResponse>
    // -> 403

    @ExceptionHandler(ConcurrentCorrectionException::class)
    fun handleConflict(e: ConcurrentCorrectionException): ResponseEntity<ErrorResponse>
    // -> 409
}
```
