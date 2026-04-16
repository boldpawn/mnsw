# Spec: REST API

Base URL: `/api/v1`

Authenticatie: Bearer JWT in `Authorization` header op alle beveiligde endpoints.

---

## Authenticatie

### POST /auth/login
Gebruiker logt in.

**Request:**
```json
{
  "email": "agent@rederij.nl",
  "password": "geheim"
}
```

**Response 200:**
```json
{
  "token": "eyJ...",
  "expiresAt": "2026-04-16T12:00:00Z",
  "user": {
    "id": "uuid",
    "email": "agent@rederij.nl",
    "fullName": "Jan Jansen",
    "role": "SCHEEPSAGENT"
  }
}
```

**Response 401:** ongeldige credentials

---

## Visits

### GET /visits
Lijst van visits. Gefilterd op rechten (havenautoriteit ziet alleen eigen haven).

**Query parameters:**
- `portLocode` (optioneel)
- `imoNumber` (optioneel)
- `status` (optioneel)
- `page`, `size` (paginering, default: page=0, size=20)

**Response 200:**
```json
{
  "content": [
    {
      "id": "uuid",
      "imoNumber": "9234567",
      "vesselName": "MV Rotterdam",
      "portLocode": "NLRTM",
      "eta": "2026-04-20T08:00:00Z",
      "etd": "2026-04-22T16:00:00Z"
    }
  ],
  "totalElements": 42,
  "totalPages": 3,
  "page": 0,
  "size": 20
}
```

### GET /visits/{visitId}
Detail van één visit inclusief gekoppelde formalities.

**Response 200:**
```json
{
  "id": "uuid",
  "imoNumber": "9234567",
  "vesselName": "MV Rotterdam",
  "vesselFlag": "NLD",
  "portLocode": "NLRTM",
  "eta": "2026-04-20T08:00:00Z",
  "etd": "2026-04-22T16:00:00Z",
  "formalities": [
    {
      "id": "uuid",
      "type": "NOA",
      "version": 1,
      "status": "ACCEPTED",
      "submittedAt": "2026-04-15T10:00:00Z"
    }
  ]
}
```

---

## Formalities

### POST /formalities
Dient een nieuwe formality in.

**Autorisatie:** SCHEEPSAGENT, LADINGAGENT, ADMIN

**Request:**
```json
{
  "visitId": "uuid",
  "type": "NOA",
  "lrn": "AGENT-REF-2026-001",
  "messageIdentifier": "MSG-20260415-001",
  "payload": {
    "expectedArrival": "2026-04-20T08:00:00Z",
    "lastPortLocode": "GBFXT",
    "nextPortLocode": "NLRTM",
    "purposeOfCall": "Lossing containers",
    "personsOnBoard": 22,
    "dangerousGoods": false,
    "wasteDelivery": true,
    "maxStaticDraught": 11.5
  }
}
```

**Response 202 Accepted:**
```json
{
  "formalityId": "uuid",
  "messageIdentifier": "MSG-20260415-001",
  "status": "SUBMITTED",
  "statusUrl": "/api/v1/formalities/uuid"
}
```

**Response 422 Unprocessable Entity** (validatiefouten):
```json
{
  "errors": [
    {
      "field": "payload.expectedArrival",
      "code": "NOA_ARRIVAL_IN_PAST",
      "message": "De verwachte aankomsttijd mag niet in het verleden liggen"
    }
  ]
}
```

### GET /formalities
Lijst van formalities.

**Autorisatie:**
- SCHEEPSAGENT/LADINGAGENT: alleen eigen ingediende formalities
- HAVENAUTORITEIT: alle formalities voor hun haven
- ADMIN: alles

**Query parameters:**
- `type` — NOA, NOS, NOD, VID, SID
- `status` — SUBMITTED, ACCEPTED, REJECTED, UNDER_REVIEW, SUPERSEDED
- `visitId` — filter op bezoek
- `portLocode` — filter op haven (ADMIN/HAVENAUTORITEIT)
- `fromDate`, `toDate` — filter op ingediend op
- `includeSuperseded` — boolean, default false
- `page`, `size`

**Response 200:**
```json
{
  "content": [
    {
      "id": "uuid",
      "visitId": "uuid",
      "type": "NOA",
      "version": 1,
      "status": "ACCEPTED",
      "submitterName": "Jan Jansen",
      "lrn": "AGENT-REF-2026-001",
      "messageIdentifier": "MSG-20260415-001",
      "submittedAt": "2026-04-15T10:00:00Z",
      "channel": "WEB",
      "vessel": {
        "imoNumber": "9234567",
        "vesselName": "MV Rotterdam",
        "portLocode": "NLRTM"
      }
    }
  ],
  "totalElements": 15,
  "totalPages": 1,
  "page": 0,
  "size": 20
}
```

### GET /formalities/{formalityId}
Detail van één formality inclusief payload en versiehistorie.

**Response 200:**
```json
{
  "id": "uuid",
  "visitId": "uuid",
  "type": "NOA",
  "version": 2,
  "status": "ACCEPTED",
  "submitterId": "uuid",
  "lrn": "AGENT-REF-2026-001",
  "messageIdentifier": "MSG-20260415-001",
  "submittedAt": "2026-04-15T10:00:00Z",
  "channel": "WEB",
  "payload": {
    "expectedArrival": "2026-04-20T08:00:00Z",
    "lastPortLocode": "GBFXT",
    "nextPortLocode": "NLRTM",
    "purposeOfCall": "Lossing containers",
    "personsOnBoard": 22,
    "dangerousGoods": false,
    "wasteDelivery": true,
    "maxStaticDraught": 11.5
  },
  "frmResponse": {
    "status": "ACCEPTED",
    "sentAt": "2026-04-15T10:05:00Z"
  },
  "versionHistory": [
    {
      "id": "uuid-v1",
      "version": 1,
      "status": "SUPERSEDED",
      "submittedAt": "2026-04-14T09:00:00Z"
    },
    {
      "id": "uuid-v2",
      "version": 2,
      "status": "ACCEPTED",
      "submittedAt": "2026-04-15T10:00:00Z"
    }
  ]
}
```

**Response 404:** formality niet gevonden
**Response 403:** geen toegang tot deze formality

### POST /formalities/{formalityId}/corrections
Dient een correctie in op een bestaande formality. Maakt een nieuwe versie aan.

**Autorisatie:** Alleen de originele indiener (submitter_id == authenticated user)

**Request:** zelfde structuur als POST /formalities, maar zonder `type` (type wijzigt niet) en zonder `visitId`.

**Response 202:**
```json
{
  "formalityId": "uuid-v3",
  "messageIdentifier": "MSG-20260415-002",
  "version": 3,
  "status": "SUBMITTED",
  "statusUrl": "/api/v1/formalities/uuid-v3",
  "previousVersionId": "uuid-v2"
}
```

**Response 409 Conflict:** de formality heeft al een nieuwere versie (concurrent correctie)
**Response 403:** niet de originele indiener

### PUT /formalities/{formalityId}/review
Havenautoriteit zet formality op UNDER_REVIEW.

**Autorisatie:** HAVENAUTORITEIT (eigen haven), ADMIN

**Response 200:**
```json
{
  "formalityId": "uuid",
  "status": "UNDER_REVIEW"
}
```

### PUT /formalities/{formalityId}/approve
Havenautoriteit keurt formality goed.

**Autorisatie:** HAVENAUTORITEIT (eigen haven), ADMIN

**Response 200:**
```json
{
  "formalityId": "uuid",
  "status": "ACCEPTED",
  "frmResponse": {
    "status": "ACCEPTED",
    "sentAt": "2026-04-16T11:00:00Z"
  }
}
```

### PUT /formalities/{formalityId}/reject
Havenautoriteit wijst formality af.

**Autorisatie:** HAVENAUTORITEIT (eigen haven), ADMIN

**Request:**
```json
{
  "reasonCode": "INVALID_IMO",
  "reasonDescription": "Het opgegeven IMO-nummer is ongeldig"
}
```

**Response 200:**
```json
{
  "formalityId": "uuid",
  "status": "REJECTED",
  "frmResponse": {
    "status": "REJECTED",
    "reasonCode": "INVALID_IMO",
    "reasonDescription": "Het opgegeven IMO-nummer is ongeldig",
    "sentAt": "2026-04-16T11:00:00Z"
  }
}
```

---

## Gebruikersbeheer

### GET /users
Lijst van gebruikers.

**Autorisatie:** ADMIN

**Response 200:** paginering conform /formalities

### POST /users
Maak nieuwe gebruiker aan.

**Autorisatie:** ADMIN

**Request:**
```json
{
  "email": "autoriteit@portofrotterdam.com",
  "password": "tijdelijkWachtwoord123!",
  "fullName": "A. Havenmeester",
  "role": "HAVENAUTORITEIT",
  "portLocode": "NLRTM"
}
```

**Response 201:**
```json
{
  "id": "uuid",
  "email": "autoriteit@portofrotterdam.com",
  "fullName": "A. Havenmeester",
  "role": "HAVENAUTORITEIT",
  "portLocode": "NLRTM"
}
```

### PUT /users/{userId}
Bewerk gebruiker (bijv. activeer/deactiveer).

**Autorisatie:** ADMIN

### DELETE /users/{userId}
Deactiveer gebruiker (soft delete via `active = false`).

**Autorisatie:** ADMIN

---

## Health & Status

### GET /actuator/health
Spring Boot Actuator health endpoint.

**Response 200:**
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "pulsar": { "status": "UP" }
  }
}
```

---

## HTTP Status Codes

| Code | Betekenis | Wanneer |
|------|-----------|---------|
| 200 | OK | Succesvolle GET, PUT |
| 201 | Created | Succesvolle POST met directe creatie |
| 202 | Accepted | Formality ontvangen en asynchroon verwerkt |
| 400 | Bad Request | Malformed JSON, missing required field |
| 401 | Unauthorized | JWT ontbreekt of verlopen |
| 403 | Forbidden | Ingelogd maar geen toegang |
| 404 | Not Found | Resource bestaat niet |
| 409 | Conflict | Concurrent update conflicteert |
| 422 | Unprocessable Entity | Validatiefout (business rule) |
| 500 | Internal Server Error | Onverwachte fout |

---

## OpenAPI / Swagger

Beschikbaar op `/swagger-ui.html` in dev en test. Niet beschikbaar in productie.
Configuratie via `springdoc-openapi-starter-webmvc-ui` dependency.
