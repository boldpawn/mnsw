# Spec: Frontend

## Overzicht

Angular 21 webapplicatie voor scheepsagenten, ladingagenten, havenautoriteiten en beheerders.
Visueel ontwerp conform Nederlandse Rijkshuisstijl (zie `BRAND_GUIDELINES.md`).

---

## Tech Stack (frontend)

| Component | Technologie | Versie |
|-----------|-------------|--------|
| Framework | Angular | 21 |
| UI bibliotheek | Angular Material | 21 |
| Stijlen | SCSS + Rijkshuisstijl theme | — |
| State management | Angular Signals | (ingebouwd) |
| HTTP client | Angular HttpClient | (ingebouwd) |
| Forms | Angular Reactive Forms | (ingebouwd) |
| Router | Angular Router | (ingebouwd) |
| Testing | Jest + Angular Testing Library | — |
| Build | Angular CLI / Vite | — |
| Taal | TypeScript | 5.x |

---

## Directorystructuur

```
mnsw-frontend/
├── src/
│   ├── app/
│   │   ├── core/
│   │   │   ├── auth/
│   │   │   │   ├── auth.service.ts
│   │   │   │   ├── auth.guard.ts
│   │   │   │   └── jwt.interceptor.ts
│   │   │   ├── api/
│   │   │   │   ├── formality.service.ts
│   │   │   │   ├── visit.service.ts
│   │   │   │   └── user.service.ts
│   │   │   └── models/
│   │   │       ├── formality.model.ts
│   │   │       ├── visit.model.ts
│   │   │       └── user.model.ts
│   │   ├── shared/
│   │   │   ├── components/
│   │   │   │   ├── status-badge/
│   │   │   │   ├── page-header/
│   │   │   │   ├── breadcrumb/
│   │   │   │   ├── confirm-dialog/
│   │   │   │   └── error-display/
│   │   │   └── pipes/
│   │   │       ├── formality-type-label.pipe.ts
│   │   │       └── truncate-uuid.pipe.ts
│   │   ├── layout/
│   │   │   ├── app-layout/          # Hoofdlayout (sidebar + content)
│   │   │   ├── sidebar/
│   │   │   └── header/
│   │   ├── features/
│   │   │   ├── auth/
│   │   │   │   └── login/
│   │   │   │       └── login.component.ts
│   │   │   ├── formalities/
│   │   │   │   ├── formality-list/
│   │   │   │   ├── formality-detail/
│   │   │   │   ├── formality-submit/
│   │   │   │   │   ├── noa-form/
│   │   │   │   │   ├── nos-form/
│   │   │   │   │   ├── nod-form/
│   │   │   │   │   ├── vid-form/
│   │   │   │   │   └── sid-form/
│   │   │   │   └── formality-correct/
│   │   │   ├── visits/
│   │   │   │   ├── visit-list/
│   │   │   │   └── visit-detail/
│   │   │   └── users/               # Alleen ADMIN
│   │   │       ├── user-list/
│   │   │       └── user-form/
│   │   ├── app.routes.ts
│   │   └── app.component.ts
│   ├── assets/
│   │   ├── rijkshuisstijl-logo.svg
│   │   └── i18n/
│   │       └── nl.json              # Alle Nederlandse UI-teksten
│   ├── styles/
│   │   ├── _rijkshuisstijl.scss     # Kleurvariabelen en overrides
│   │   ├── _buttons.scss
│   │   ├── _forms.scss
│   │   ├── _tables.scss
│   │   ├── _badges.scss
│   │   └── styles.scss              # Global import
│   ├── environments/
│   │   ├── environment.ts
│   │   └── environment.prod.ts
│   └── index.html
├── package.json
└── angular.json
```

---

## Routing

```typescript
// app.routes.ts
export const routes: Routes = [
  { path: '', redirectTo: '/formalities', pathMatch: 'full' },
  { path: 'login', loadComponent: () => import('./features/auth/login/login.component') },
  {
    path: '',
    component: AppLayoutComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'formalities',
        loadChildren: () => import('./features/formalities/formalities.routes')
      },
      {
        path: 'visits',
        loadChildren: () => import('./features/visits/visits.routes')
      },
      {
        path: 'users',
        loadChildren: () => import('./features/users/users.routes'),
        canActivate: [roleGuard('ADMIN')]
      }
    ]
  }
];
```

---

## Schermen per rol

### Alle rollen (na login)

**Formality-overzicht** (`/formalities`)
- Tabel met kolommen: Type | Status | Visit (scheepsnaam + haven) | Ingediend op | Indiener | Acties
- Filterbar: type, status, haven (ADMIN/HAVENAUTORITEIT), datumrange
- Knoppen per rij: "Details"
- Boven tabel (SCHEEPSAGENT/LADINGAGENT): knop "Nieuwe formality indienen"
- Paginering
- Lege staat: illustratie + "Geen formalities gevonden"

**Formality-detail** (`/formalities/:id`)
- Header: type-badge + scheepsnaam + haven + status-badge
- Tabbladen: Algemeen | Payload | Versiehistorie | FRM Response
- Tab Algemeen: visit-info, indiener, tijdstip, kanaal (WEB/RIM)
- Tab Payload: type-specifieke velden in een readonly formulierweergave
- Tab Versiehistorie: chronologisch aflopend, actieve versie gemarkeerd
- Tab FRM Response: FRM status + eventuele foutcodes en beschrijvingen
- Actieknop (indien indiener en niet SUPERSEDED): "Correctie indienen"
- Actieknoppen (HAVENAUTORITEIT/ADMIN): "Goedkeuren" | "Afwijzen"

### Scheepsagent / Ladingagent

**Formality indienen — Type kiezen** (`/formalities/new`)
- Kaartweergave: NOA, NOS, NOD, VID, SID als kiesopties
- Elke optie met korte Nederlandse beschrijving

**Formality indienen — Formulier** (`/formalities/new/:type`)
- Sectie 1: Visit gegevens (IMO-nummer, scheepsnaam, havenkeuze, ETA/ETD)
- Sectie 2: Type-specifieke velden (NOA/NOS/NOD/VID/SID)
- Sectie 3: Referentiegegevens (LRN, optioneel)
- Veldvalidatie in real-time (bij blur)
- Knoppenrij onderaan: [Annuleren] [Concept opslaan — fase 2] [Indienen]
- Na indienen: success-toast + redirect naar formality-detail

**Correctie indienen** (`/formalities/:id/correct`)
- Zelfde formulier als indienen, voorgevuld met huidige payload
- Banner "Let op: dit is een correctie op versie {N}"

### Havenautoriteit

- Formality-overzicht gefilterd op eigen haven (portLocode automatisch ingevuld)
- Knoppenrij op detail-pagina: "Beoordelen" (-> UNDER_REVIEW) | "Goedkeuren" | "Afwijzen"
- Afwijzen: dialoog met reden-dropdown + vrije tekst

### ADMIN

- Alle bovenstaande views plus gebruikersbeheer

---

## TypeScript Modellen (core/models)

```typescript
// formality.model.ts

export type FormalityType = 'NOA' | 'NOS' | 'NOD' | 'VID' | 'SID';
export type FormalityStatus = 'SUBMITTED' | 'ACCEPTED' | 'REJECTED' | 'UNDER_REVIEW' | 'SUPERSEDED';
export type SubmissionChannel = 'WEB' | 'RIM';

export interface Formality {
  id: string;
  visitId: string;
  type: FormalityType;
  version: number;
  status: FormalityStatus;
  submitterId: string;
  lrn?: string;
  messageIdentifier: string;
  submittedAt: string; // ISO 8601
  channel: SubmissionChannel;
  payload?: FormalityPayload;
  frmResponse?: FrmResponse;
  versionHistory?: FormalityVersionSummary[];
  vessel?: {
    imoNumber: string;
    vesselName: string;
    portLocode: string;
  };
}

export interface FrmResponse {
  status: 'ACCEPTED' | 'REJECTED' | 'UNDER_REVIEW';
  reasonCode?: string;
  reasonDescription?: string;
  sentAt?: string;
}

export interface FormalityVersionSummary {
  id: string;
  version: number;
  status: FormalityStatus;
  submittedAt: string;
}

// Payload types
export interface NoaPayload {
  expectedArrival: string;
  lastPortLocode?: string;
  nextPortLocode?: string;
  purposeOfCall?: string;
  personsOnBoard?: number;
  dangerousGoods?: boolean;
  wasteDelivery?: boolean;
  maxStaticDraught?: number;
}

export interface NosPayload {
  actualSailing: string;
  nextPortLocode?: string;
  destinationCountry?: string;
}

export interface NodPayload {
  expectedDeparture: string;
  nextPortLocode?: string;
  destinationCountry?: string;
  lastCargoOperations?: string;
}

export interface VidPayload {
  certificateNationality?: string;
  grossTonnage?: number;
  netTonnage?: number;
  deadweight?: number;
  lengthOverall?: number;
  shipType?: string;
  callSign?: string;
  mmsi?: string;
}

export interface SidPayload {
  ispsLevel: 1 | 2 | 3;
  last10Ports?: PortCall[];
  securityDeclaration?: string;
  shipToShipActivities?: boolean;
  designatedAuthority?: string;
  ssasActivated?: boolean;
}

export interface PortCall {
  locode: string;
  arrival?: string;
  departure?: string;
  ispsLevel?: number;
}

export type FormalityPayload = NoaPayload | NosPayload | NodPayload | VidPayload | SidPayload;
```

---

## Services

### FormalityService (core/api/formality.service.ts)

**Methoden:**
- `list(filters: FormalityFilters): Observable<Page<Formality>>`
- `get(id: string): Observable<Formality>`
- `submit(command: SubmitFormalityRequest): Observable<SubmitFormalityResult>`
- `correct(id: string, command: CorrectFormalityRequest): Observable<SubmitFormalityResult>`
- `approve(id: string): Observable<Formality>`
- `reject(id: string, request: RejectRequest): Observable<Formality>`
- `setUnderReview(id: string): Observable<Formality>`

**Implementatiedetails:**
- Baseurl uit `environment.apiUrl`
- Foutafhandeling in interceptor (401 -> logout, 403 -> melding, 5xx -> toast)

### AuthService (core/auth/auth.service.ts)

**Methoden:**
- `login(email, password): Observable<AuthResult>`
- `logout(): void`
- `currentUser(): Signal<User | null>`
- `hasRole(role: string): boolean`
- `isAuthenticated(): boolean`

**JWT opslag:** `localStorage` (voor MVP; HttpOnly cookie beter voor productie)

---

## Formulieren

### NOA Form (noa-form.component.ts)

Reactive Form met secties:

```
Sectie "Aankomstgegevens":
  - expectedArrival (datetime-local, verplicht)
  - lastPortLocode (text, optioneel, validatie: 2 letters + 3 alfanumeriek)
  - nextPortLocode (text, optioneel)
  - purposeOfCall (text, optioneel)

Sectie "Scheepsgegevens":
  - personsOnBoard (number, optioneel, min 0)
  - maxStaticDraught (number, optioneel, min 0, max 50)

Sectie "Bijzonderheden":
  - dangerousGoods (checkbox)
  - wasteDelivery (checkbox)
```

### SID Form

Bevat dynamisch veld `last10Ports` als FormArray — maximaal 10 havens toevoegbaar.

---

## Styling en Rijkshuisstijl

Alle kleuren, fonts, knoppen en formulierstijlen uit `BRAND_GUIDELINES.md` implementeren als SCSS-variabelen:

```scss
// _rijkshuisstijl.scss
:root {
  --color-primary: #007BC7;
  --color-primary-dark: #01689B;
  --color-primary-darkest: #154273;
  --color-white: #FFFFFF;
  --color-gray-light: #F3F3F3;
  --color-gray-medium: #B4B4B4;
  --color-gray-dark: #696969;
  --color-text: #000000;
  --color-success: #39870C;
  --color-warning: #E17000;
  --color-error: #D52B1E;
  --color-focus: #F9E11E;
}
```

Angular Material theme overschrijven met Rijkshuisstijl-kleuren (mat-palette).
Geen afgeronde hoeken — Rijkshuisstijl gebruikt rechte hoeken (`border-radius: 0`).

---

## Toegankelijkheid

- Taal: `<html lang="nl">`
- Alle formuliervelden: `<label for="...">` gekoppeld
- Foutmeldingen: `aria-describedby` gekoppeld aan veld
- Focus-indicator: gele outline conform WCAG 2.1 AA
- Tabelheaders: `<th scope="col">`
- Statusbadges bevatten altijd zichtbare tekst (niet kleur-only)
- Toetsenbordnavigatie volledig ondersteund

---

## i18n

- Taal: Nederlands (nl)
- Alle teksten in `assets/i18n/nl.json`
- Geen Angular i18n module nodig in fase 1 — directe tekst in templates en i18n pipe voor labels
- Alle foutmeldingen en statuslabels in nl.json

---

## Testen (frontend)

### Unit tests
- Services testen met `HttpClientTestingModule`
- Components testen met `TestBed` + Angular Testing Library
- Formuliervalidatie testen: verplichte velden, grenswaarden, businessrules
- Pipes testen

### E2E tests (Playwright — fase 2)
- Zie `openspec/e2e-test-plan.md`

### Testconventies
- Testnamen in Nederlands beschrijvend formaat
- Geen snapshot tests voor components
- HTTP calls mocken via `HttpClientTestingModule`, niet via `jest.fn()`

---

## Environment variabelen

```typescript
// environment.ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api/v1'
};

// environment.prod.ts
export const environment = {
  production: true,
  apiUrl: '/api/v1'  // Relatief — geserveerd door backend of reverse proxy
};
```

---

## BRAND_GUIDELINES.md

Alle visuele implementatiebeslissingen zijn gedocumenteerd in `/BRAND_GUIDELINES.md`.
Frontend-agents en reviewers MOETEN dit bestand raadplegen voordat enige visuele code wordt geschreven.
