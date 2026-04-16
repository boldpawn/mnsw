# Glossary

Shared vocabulary for this project. Managed via the `/glossary` skill.

Consult this file before using domain terms in specs, designs, and code. If a term is missing or ambiguous, use `/glossary add <term>` to propose a definition.

---

## EMSWe & Formalities

**Formality**
Een verplichte melding of aanvraag die een scheepsagent of ladingagent indient bij de havenautoriteit in het kader van een havenbezoek. Een formality heeft altijd een type (NOA, NOS, NOD, VID, SID) en is gekoppeld aan een Visit. Meervoud: formalities.

**NOA — Notification of Arrival (aankomstmelding)**
Formality type waarbij de scheepsagent de verwachte aankomst van een schip in een haven meldt. Wordt ingediend vóór aankomst. Bevat informatie over het schip, de reis, de lading en gevaarlijke stoffen. Conform EMSWe MIG datacategorie "Notification of Arrival".

**NOS — Notification of Sailing**
Formality type waarbij de scheepsagent aangeeft dat het schip vertrek heeft genomen. Wordt ingediend na het vertrek of direct daarvoor. Conform EMSWe MIG datacategorie "Notification of Sailing".

**NOD — Notification of Departure (vertrekmelding)**
Formality type waarbij de scheepsagent de geplande of feitelijke afvaart van een schip uit een haven meldt. Wordt ingediend voor of bij vertrek. Conform EMSWe MIG datacategorie "Notification of Departure". Niet te verwarren met NOS: NOD is de formele vertrekmelding vóóraf, NOS bevestigt het feitelijke vertrek.

**VID — Vessel Identification Documents (scheepsidentificatie en documenten)**
Formality type voor de melding van scheepsidentificatiegegevens en scheepsdocumenten, waaronder het Certificaat van Nationaliteit, meetbrief, en veiligheidscertificaten. Conform EMSWe MIG datacategorie "Ship/Vessel Identification".

**SID — Ship/Security Information Document**
Formality type voor de melding van beveiliging- en veiligheidsgegevens van het schip conform de ISPS-regelgeving (International Ship and Port Facility Security Code). Bevat informatie over de beveiligingsstatus van het schip, de laatste aanloophavens en beveiligingsverklaring. Conform EMSWe MIG datacategorie "Security" / ISPS-melding. Eerder ook aangeduid als ISPS-melding of Security Notification.

**FRM — Formality Response Message**
Een uitgaand bericht van MNSW naar de indiener als antwoord op een ingediende formality. Bevat de beoordelingsstatus (accepted, rejected, under review) en eventuele foutcodes. FRM is GEEN inkomend formality type — het is een systeem-gegenereerd antwoord. Conform EMSWe MAI-header correlatie via Message Identifier.

**EMSWe**
European Maritime Single Window environment. Het Europese raamwerk (Richtlijn 2019/1239/EU) voor geharmoniseerde digitale meldingen in zeehavens. MNSW is de Nederlandse nationale implementatie van EMSWe.

**MIG — Message Implementation Guide**
De technische standaard van EMSA die de datamodellen, berichtstructuren en validatieregels voor alle EMSWe-formalities beschrijft. Versie 2.0.1 (16 december 2025) is de geldende versie voor dit project. Beschikbaar via https://emsa.europa.eu/emswe-mig/

**ISPS — International Ship and Port Facility Security Code**
Internationaal IMO-veiligheidskader voor schepen en havens. De SID-formality implementeert de ISPS-meldingsvereisten.

---

## Visit & Correlatie

**Visit**
Een havenbezoek: de aanwezigheid van een specifiek schip in een specifieke haven gedurende een aaneengesloten periode. Een Visit is de centrale aggregaat waaronder alle formalities van dat bezoek worden gegroepeerd. Uniek geïdentificeerd door het Visit ID.

**Visit ID**
De centrale correlatiesleutel die alle berichten (formalities en FRM-responses) van één havenbezoek aan elkaar koppelt. Gegenereerd door MNSW bij de eerste NOA. Het Visit ID wordt opgenomen in alle opvolgende formalities voor hetzelfde bezoek.

**LRN — Local Reference Number**
Een door de indiener (scheepsagent of ladingagent) gekozen referentienummer waarmee de indiener het bericht in zijn eigen systemen identificeert. MNSW slaat de LRN op en echoot hem terug in de FRM-response zodat de indiener het antwoord kan correleren.

**Message Identifier**
Het unieke berichtnummer in de MAI-header (Message Administration Information). Correleert een inkomend verzoek met de bijbehorende FRM-response. Gegenereerd door de indiener per bericht.

---

## Rollen & Actoren

**Scheepsagent**
De partij die namens de reder optreedt en verantwoordelijk is voor het indienen van scheepsgerelateerde formalities (NOA, NOS, NOD, VID, SID). Heeft toegang tot eigen ingediende formalities.

**Ladingagent**
De partij die namens de lading-eigenaar optreedt en lading-gerelateerde gegevens indient als onderdeel van formalities. Heeft toegang tot eigen ingediende formalities.

**Havenautoriteit**
Bevoegde autoriteit die formalities ontvangt en beoordeelt voor de haven(s) waarvoor zij bevoegd is. Heeft leesrecht op alle formalities voor hun havens.

**ADMIN**
Systeembeheerder met volledige toegang tot alle functies en gegevens in MNSW.

---

## Kanalen & Protocollen

**RIM — Remote Interface Module**
Het machine-to-machine koppelvlak waarmee externe systemen (scheepsvaartsoftware, agentensystemen) formalities kunnen indienen via het AS4/eDelivery-protocol. RIM verwerkt berichten asynchroon via Apache Pulsar.

**AS4 / eDelivery**
Het Europese berichtenprotocol (OASIS AS4 profiel) dat gebruikt wordt voor beveiligde machine-to-machine uitwisseling van EMSWe-berichten. Onderdeel van het CEF (Connecting Europe Facility) eDelivery Building Block.

**MAI — Message Administration Information**
De standaard-berichtheader in EMSWe-berichten. Bevat Message Identifier, verzender, ontvanger, tijdstempel en Message Reference Number voor correlatie met FRM-responses.

---

## Technisch

**Pulsar Topic**
Een Apache Pulsar messaging-kanaal. MNSW gebruikt aparte topics per formality type (bijv. `formalities.noa`, `formalities.nos`) voor ontvangst via RIM en interne verwerking.

**Flyway Migration**
Database-migratiescript (SQL) dat de PostgreSQL-schema-wijzigingen bijhoudt en versioned uitrolt. Alle schemawijzigingen verlopen via Flyway.

**DTO — Data Transfer Object**
Een Kotlin data class die gebruikt wordt voor API-request/response serialisatie. DTOs worden nooit direct als JPA-entiteit gebruikt — er is altijd een expliciete mapping tussen DTO en domeinentiteit.
