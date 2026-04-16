# BRAND GUIDELINES — MNSW

Gegenereerd voor het MNSW (Maritime National Single Window) project.
Gebaseerd op de Nederlandse Rijkshuisstijl (rijkshuisstijl.nl).

---

## 1. Kleurpalet

### Primaire kleuren

| Naam | Hex | RGB | Gebruik |
|------|-----|-----|---------|
| Rijksblauw | `#007BC7` | rgb(0, 123, 199) | Primaire acties, headers, links, actieve navigatie |
| Wit | `#FFFFFF` | rgb(255, 255, 255) | Achtergrond pagina, formuliervelden, kaarten |
| Donkerblauw | `#01689B` | rgb(1, 104, 155) | Hover state primaire knop, actieve staat |
| Diepblauw | `#154273` | rgb(21, 66, 115) | Header navigatie achtergrond, zware accenten |

### Secondaire kleuren

| Naam | Hex | RGB | Gebruik |
|------|-----|-----|---------|
| Grijs-licht | `#F3F3F3` | rgb(243, 243, 243) | Pagina-achtergrond, alternerende tabelrijen |
| Grijs-medium | `#B4B4B4` | rgb(180, 180, 180) | Disabled state, borders, dividers |
| Grijs-donker | `#696969` | rgb(105, 105, 105) | Secondaire tekst, labels, helptekst |
| Tekst-zwart | `#000000` | rgb(0, 0, 0) | Primaire bodytekst |

### Status/Feedback kleuren

| Naam | Hex | Gebruik |
|------|-----|---------|
| Succes-groen | `#39870C` | ACCEPTED status, succesbericht, validatiegroen |
| Waarschuwing-oranje | `#E17000` | UNDER_REVIEW status, waarschuwingen |
| Fout-rood | `#D52B1E` | REJECTED status, foutmeldingen, verplichte velden |
| Info-blauw | `#007BC7` | SUBMITTED status, informatieve meldingen |
| Aanvulling-geel | `#F9E11E` | Highlight, aandachtskleur (gebruik spaarzaam) |

---

## 2. Typografie

### Fonts

| Rol | Font | Gewicht | Grootte |
|-----|------|---------|---------|
| Primair (headings + body) | Rijksoverheid Sans Web | Regular (400), Bold (700) | — |
| Fallback | Arial, sans-serif | — | — |
| Monospace (codes, IDs) | Courier New, monospace | Regular | — |

### Schaalverdeling

| Element | Grootte | Gewicht | Line-height |
|---------|---------|---------|-------------|
| H1 — Paginatitel | 32px | Bold | 1.25 |
| H2 — Sectietitel | 24px | Bold | 1.3 |
| H3 — Subsectie | 20px | Bold | 1.35 |
| Body | 16px | Regular | 1.5 |
| Label / caption | 14px | Regular | 1.4 |
| Klein / helper | 12px | Regular | 1.4 |

---

## 3. Ruimte & Grid

### Baseline-grid
- Basisunit: 8px
- Ruimtes: 4px, 8px, 16px, 24px, 32px, 48px, 64px

### Maximale inhoudbreedte
- Container max-width: 1200px
- Formulierkolom max-width: 640px
- Volle breedte: tabellen, overzichtspagina's

### Padding-conventies
- Pagina-padding horizontaal: 24px (mobiel), 48px (desktop)
- Kaart-padding: 24px
- Formulier-sectie-padding: 16px 0

---

## 4. Componentpatronen

### 4.1 Knoppen

```
Primair (Rijksblauw):
  background: #007BC7
  color: #FFFFFF
  border: none
  padding: 12px 24px
  border-radius: 0  (geen afgeronde hoeken — Rijkshuisstijl)
  font-weight: bold
  hover: background #01689B
  focus: outline 3px solid #F9E11E (geel, WCAG)
  disabled: background #B4B4B4, cursor not-allowed

Secundair (omlijnd):
  background: transparent
  color: #007BC7
  border: 2px solid #007BC7
  hover: background #007BC7, color #FFFFFF

Gevaar/Destructief:
  background: #D52B1E
  color: #FFFFFF
  hover: background #AB1D14
```

### 4.2 Formuliervelden

```
Input / Select / Textarea:
  border: 2px solid #696969
  border-radius: 0
  padding: 8px 12px
  font-size: 16px
  focus: border-color #007BC7, outline 3px solid #F9E11E
  error: border-color #D52B1E
  disabled: background #F3F3F3, border-color #B4B4B4

Label:
  font-weight: bold
  margin-bottom: 4px
  display: block

Foutmelding:
  color: #D52B1E
  font-size: 14px
  margin-top: 4px
  voorafgegaan door icoon (uitroepteken)

Helptekst:
  color: #696969
  font-size: 14px
  margin-top: 4px
```

### 4.3 Statusbadges (formality status)

```
SUBMITTED:   background #007BC7, color #FFFFFF
ACCEPTED:    background #39870C, color #FFFFFF
REJECTED:    background #D52B1E, color #FFFFFF
UNDER_REVIEW: background #E17000, color #FFFFFF
SUPERSEDED:  background #B4B4B4, color #FFFFFF
DRAFT:       background #F3F3F3, color #696969, border 1px solid #B4B4B4

padding: 4px 8px
font-size: 12px
font-weight: bold
border-radius: 0  (Rijkshuisstijl)
text-transform: uppercase
```

### 4.4 Tabellen (formality-overzicht)

```
Header-rij:
  background: #154273
  color: #FFFFFF
  font-weight: bold
  padding: 12px 16px

Data-rij:
  border-bottom: 1px solid #B4B4B4
  padding: 12px 16px
  hover: background #F3F3F3

Alternerende rijen (optioneel):
  oneven: #FFFFFF
  even: #F3F3F3

Gesorteerde kolom: header-achtergrond #01689B
```

### 4.5 Navigatie (sidebar)

```
Zijbalkbreedte: 240px
Achtergrond: #154273
Tekstkleur: #FFFFFF

Navigatie-item:
  padding: 12px 16px
  hover: background #01689B
  actief: background #007BC7, border-left 4px solid #F9E11E

Logo/naam bovenaan:
  padding: 24px 16px
  font-size: 20px
  font-weight: bold
```

### 4.6 Broodkruimelnavigatie (breadcrumbs)

```
Formality-detail pagina: Formalities > NOA > [Visit ID afgekorte weergave]
Scheidingsteken: > (chevron)
Kleur links: #007BC7
Kleur huidig: #696969 (niet klikbaar)
```

### 4.7 Notificaties / Alerts

```
Succes:
  background: #E8F5E1
  border-left: 4px solid #39870C
  color: #000000

Fout:
  background: #FDECEA
  border-left: 4px solid #D52B1E
  color: #000000

Waarschuwing:
  background: #FEF3E2
  border-left: 4px solid #E17000
  color: #000000

Info:
  background: #E5F3FB
  border-left: 4px solid #007BC7
  color: #000000

padding: 16px
margin-bottom: 16px
```

---

## 5. Iconen

- Gebruik de Rijksoverheid icoonenset waar beschikbaar
- Alternatief: Material Icons (outlined variant) of Heroicons (24px)
- Schip-icoon voor navigatie-item "Formalities"
- Persoon-icoon voor gebruikersbeheer
- Sluit-icoon (X) voor verwijderacties (rood bij destructieve acties)
- Pijl-omlaag / -omhoog voor sortering
- Filter-icoon voor filteropties

---

## 6. Toegankelijkheid (WCAG 2.1 AA)

- Minimale contrastverhouding tekst op achtergrond: 4.5:1
  - Rijksblauw `#007BC7` op wit `#FFFFFF`: 4.63:1 (AA voldoet)
  - Donkerblauw `#154273` op wit `#FFFFFF`: 10.8:1 (AAA)
- Focus-indicator: gele outline `3px solid #F9E11E` op alle interactieve elementen
- Alle formuliervelden hebben een `<label>` met `for`-attribuut
- Statusbadges zijn niet kleur-only: altijd vergezeld van tekstlabel
- Foutmeldingen zijn gekoppeld aan veld via `aria-describedby`
- Tabel-headers gebruiken `<th scope="col">`
- Navigatie via toetsenbord volledig ondersteund
- Taaltag: `<html lang="nl">`

---

## 7. Schermindeling

### Formality-overzichtspagina (lijst)

```
[Header: MNSW logo + gebruikersnaam + uitlogknop]
[Sidebar links: navigatiemenu]
[Hoofdinhoud:]
  [H1: Formalities]
  [Filterbalk: type, status, haven, datumrange]
  [Tabel met kolommen: Type | Status | Visit ID | Scheepsnaam | Haven | Ingediend op | Acties]
  [Paginering]
```

### Formality-detailpagina

```
[Header]
[Sidebar]
[Hoofdinhoud:]
  [Broodkruimel: Formalities > NOA > {message_identifier}]
  [H1: NOA — {scheepsnaam}]
  [Statusbadge: ACCEPTED]
  [Tabbladen: Algemeen | Payload | Versiehistorie | FRM Response]
  [Actieknoppen: Correctie indienen (indien indiener)]
```

### Indiening-formulierpagina

```
[Header]
[Sidebar]
[Hoofdinhoud:]
  [Broodkruimel: Formalities > Nieuwe NOA]
  [H1: Nieuwe NOA indienen]
  [Formulier in secties met legenda per sectie]
  [Onderaan: [Annuleren] [Opslaan als concept] [Indienen]]
```

---

## 8. Specifieke MNSW UI-patronen

### Visit ID weergave
- Volledige UUID te lang voor tabellen: toon eerste 8 tekens + ellipsis
- Tooltip of kopieknop voor volledig UUID
- Monospace font voor UUID-weergave

### Versiehistorie
- Chronologisch aflopend (nieuwste bovenaan)
- Actieve versie gemarkeerd als "Huidig"
- Gesupersedede versies grijs met "Vervangen" label

### FRM Response weergave
- Duidelijk onderscheid: groen kader voor ACCEPTED, rood voor REJECTED
- Foutcodes weergeven met Nederlandse omschrijving
- Tijdstempel van response tonen

### RIM-ingediende formalities
- Badge "RIM" naast statusbadge om kanaal aan te geven
- Kleur: neutraal grijs, niet storend
