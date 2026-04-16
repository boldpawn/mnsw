# Proposal: MNSW Code Module

## Wat

Implementatie van de MNSW Code-module: het centrale systeem voor het ontvangen, valideren, opslaan en routeren van maritieme meldingsverplichtingen (formalities) conform de Europese EMSWe-standaard (Richtlijn 2019/1239/EU).

De module ondersteunt vijf inkomende formality-typen (NOA, NOS, NOD, VID, SID) en één uitgaand antwoordbericht (FRM). Formalities kunnen via twee kanalen worden ingediend: via de Angular-webfrontend door scheepsagenten en ladingagenten, en via het RIM-koppelvlak (AS4/eDelivery) door geautomatiseerde systemen.

## Waarom

Nederland is conform EU-richtlijn 2019/1239/EU verplicht een nationaal maritiem single window te exploiteren dat aangesloten is op het Europese EMSWe-ecosysteem. MNSW is die voorziening. Zonder de Code-module kunnen geen formalities worden ontvangen, verwerkt of gerouteerd naar de bevoegde havenautoriteiten.

## Scope

### In scope
- Indiening van NOA, NOS, NOD, VID en SID via webfrontend
- Indiening van NOA, NOS, NOD, VID en SID via RIM (AS4/eDelivery, via Pulsar)
- Validatie van ingediende formalities conform EMSWe MIG v2.0.1
- Opslag van formalities in PostgreSQL met volledige versiehistorie
- Routering van gevalideerde formalities naar havenautoriteiten via Apache Pulsar
- Generatie en verzending van FRM-response (Formality Response Message) naar indiener
- Correctie/amendement van eerder ingediende formalities (nieuwe versie)
- Roltoegangsmodel: Scheepsagent, Ladingagent, Havenautoriteit, ADMIN
- Angular 21 webfrontend met Nederlandse Rijkshuisstijl

### Buiten scope
- Integratie met externe haveninformatiesystemen (separate module)
- EMSWe-gefedereerde opvraging bij andere lidstaten
- Afhandeling van gevaarlijke stoffen (DGS) als aparte formality — valt onder NOA-payload
- Betaling of leges
- SMS/e-mailnotificaties (kan later worden toegevoegd)

## Betrokkenen

| Rol | Belang |
|-----|--------|
| Scheepsagent | Primaire indiener van NOA, NOS, NOD, VID, SID |
| Ladingagent | Secundaire indiener van lading-gerelateerde gegevens |
| Havenautoriteit | Ontvangt en beoordeelt formalities voor eigen haven(s) |
| ADMIN | Beheert gebruikers, inzage in alle data |
| RIM-systemen | Geautomatiseerde machine-to-machine indiening |

## Aangenomen beslissingen

1. Hexagonale architectuur (ports & adapters) met type-specifieke payload-tabellen per formality type
2. Synchrone validatie in de applicatielaag; Pulsar voor routering naar havenautoriteiten
3. Versioning via `superseded_by` zelfverwijzing op `formality` tabel
4. REST API antwoordt met 202 Accepted bij indiening; status opvraagbaar via polling
5. JPA + Spring Data met Flyway-migraties voor schemawijzigingen
6. Spring Security + JWT voor autorisatie; rollen opgeslagen in de database
7. Angular 21 met standalone components, Angular Material met Rijkshuisstijl-overrides

## Aanpak

Implementatie in twee fasen:

**Fase 1 (MVP):** NOA, NOS, NOD via webfrontend + volledige backend (alle 5 typen). Basisrollen en rolcontroles. Pulsar-routering naar havenautoriteiten.

**Fase 2:** RIM-kanaal (AS4 consumer), VID en SID via webfrontend, correctieworkflow in UI, FRM-weergave, gebruikersbeheer voor ADMIN.
