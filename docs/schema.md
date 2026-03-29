# Schéma unifié des rappels produits

Ce document décrit les champs du schéma JSON unifié qui agrège les données de deux sources : **Safety Gate (RAPEX)** (
`/public/api/notification`) et **RappelConso** (API OpenData gouvernement français).

---

## Structure générale

| Section             | Description                                                         |
|---------------------|---------------------------------------------------------------------|
| `_metadata`         | Informations techniques sur l'import (sources, dates, identifiants) |
| Champs racine       | Identifiant de l'alerte, dates, risques                             |
| `product`           | Informations sur le produit rappelé                                 |
| `commercialization` | Pays, distributeurs, période de vente                               |
| `measures`          | Mesures prises, consignes consommateur                              |
| `media`             | Photos et lien vers la fiche officielle                             |

---

## Champs importants

### Identifiants et versioning

**`alert_number`** *(obligatoire)*
Numéro d'alerte commun aux deux sources. C'est la **clé de jointure** entre RAPEX et RappelConso. Toujours normalisé en
majuscules.
Exemples : `SR/00842/26`, `SR/00939/26`

**`version_number`**
Numéro de version de la fiche. Permet de suivre les mises à jour successives d'un rappel. Uniquement présent pour les
fiches RappelConso.

**`_metadata.rappelconso_guid`**
Identifiant unique RappelConso (`rappel_guid`). Complémentaire à `alert_number` pour identifier formellement une fiche.

**`_metadata.sources[].source_id`**
Identifiant interne à la source : numéro de notification RAPEX (ex. `10098375`) ou identifiant RappelConso (ex.
`49410`).

---

### Dates

**`publication_date`**
Date de publication officielle de l'alerte. Format `date-time`.

---

### Risques

**`risks`**
Tableau des types de risques identifiés sous forme de clés normalisées (RAPEX) ou de valeurs textuelles (RappelConso).
Exemples : `["riskType.choking"]`, `["riskType.injuries"]`, `["riskType.chemical"]`

**`risk_description`**
Description narrative du risque. C'est le champ le plus riche pour comprendre pourquoi le produit est rappelé.

**`supplementary_risk_description`**
Informations complémentaires sur le risque, uniquement présentes dans les fiches RappelConso.

---

### Produit

**`product.specific_name`**
Nom commercial précis du produit rappelé.
Exemples : `HAPPY LUTINS FARCEURS SAPERLI & POPETTE`, `Grande Panda, 600`

**`product.type`**
Nom générique ou type du produit (source RAPEX).
Exemples : `Voiture particulière`, `Jouet souple`

**`product.brand`**
Marque du produit. Normalisée lors de l'import.

**`product.family`**
Famille du produit issue de RappelConso.
Exemple : `bébés-enfants (hors alimentaire)`

**`product.category`**
Catégorie du produit. Les valeurs RAPEX sont des clés structurées (`product.category.toys`), RappelConso retourne du
texte libre (`automobiles, motos, scooters`).

**`product.barcodes`**
Tableau des codes-barres EAN associés au produit. Permet l'identification directe en point de vente.

**`product.batch_numbers`**
Tableau des numéros de lot concernés. Permet au consommateur de vérifier si son produit est visé.

**`product.model_references`**
Références de modèles ou homologations type-approval.

**`product.counterfeit`**
Booléen indiquant si le produit rappelé est une contrefaçon (source RAPEX uniquement).

**`product.packaging_description`**
Description de l'emballage du produit.

**`product.production_dates`**
Dates ou plages de production. Peut contenir plusieurs lignes si plusieurs variantes sont concernées.

---

### Commercialisation

**`commercialization.origin_country_name`**
Nom du pays de fabrication du produit (source RAPEX).

**`commercialization.alert_country_name`**
Nom du pays ayant émis l'alerte (RAPEX) ou `France` pour RappelConso.

**`commercialization.reacting_countries`**
Tableau des codes pays ayant réagi à l'alerte (RAPEX) ou zone géographique de vente (RappelConso).
Exemple : `["BG", "DE", "HR", "HU", "LU", "PT", "SE", "SI", "SK"]`

**`commercialization.sold_online`**
Indique si le produit était vendu en ligne (source RAPEX).

**`commercialization.marketing_start_date`** / **`commercialization.marketing_end_date`**
Période de commercialisation du produit (source RappelConso).

**`commercialization.distributors`**
Liste textuelle des distributeurs concernés (source RappelConso).

---

### Mesures

**`measures.recall_published_online`**
Booléen indiquant si l'entreprise a publié un rappel sur son site internet (source RAPEX).

**`measures.measures_list`**
Tableau des mesures détaillées. Chaque mesure contient :

- `category` : type de mesure (retrait du marché, rappel consommateur, suppression marketplace…)
- `other_category` : précision textuelle si la catégorie est `other`
- `type` : volontaire ou obligatoire
- `effective_date` : date d'entrée en vigueur

**`measures.company_recalls`**
Liens vers les pages de rappel publiées par l'entreprise elle-même, avec code langue.

**`measures.consumer_actions`**
Consignes à destination du consommateur : que faire du produit, comment obtenir un remboursement ou une réparation (
source RappelConso).

**`measures.compensation_terms`**
Modalités de compensation proposées (source RappelConso).

**`measures.procedure_end_date`**
Date limite de la procédure de rappel (source RappelConso).

---

### Médias

**`media.photos`**
Tableau d'URLs des photos du produit. Pour RAPEX, construire l'URL à partir de l'`id` de la photo avec le préfixe
`https://ec.europa.eu/safety-gate-alerts/public/api/notification/image/`. Pour RappelConso, les URLs sont séparées par
`|` dans la source.

**`media.recall_sheet_url`**
Lien vers la fiche officielle du rappel. Pour RAPEX, construire avec le préfixe
`https://ec.europa.eu/safety-gate-alerts/screen/webReport/alertDetail/`. Pour RappelConso, champ
`lien_vers_la_fiche_rappel`.

---

### Champ complémentaire

**`additional_information`**
Informations complémentaires publiques (source RappelConso `informations_complementaires_publiques`).

---

## Tableaux de mapping des champs

### Champs racine

| Champ unifié                     | RAPEX                           | RappelConso                               |
|----------------------------------|---------------------------------|-------------------------------------------|
| `alert_number`                   | `reference`                     | `numero_fiche`                            |
| `version_number`                 | —                               | `numero_version`                          |
| `publication_date`               | `publicationDate`               | `date_publication`                        |
| `risks`                          | `risk.riskType[].key`           | `risques_encourus` (splitter sur virgule) |
| `risk_description`               | `risk.versions.riskDescription` | `motif_rappel`                            |
| `supplementary_risk_description` | —                               | `description_complementaire_risque`       |
| `additional_information`         | —                               | `informations_complementaires_publiques`  |

### `_metadata`

| Champ unifié                      | RAPEX                            | RappelConso           |
|-----------------------------------|----------------------------------|-----------------------|
| `_metadata.sources[].origin`      | `"rapex"`                        | `"rappelconso"`       |
| `_metadata.sources[].source_id`   | id notification (ex: `10098375`) | id (ex: `49410`)      |
| `_metadata.sources[].url`         | URL fiche RAPEX                  | URL fiche RappelConso |
| `_metadata.sources[].import_date` | *(date d'import)*                | *(date d'import)*     |
| `_metadata.rappelconso_guid`      | —                                | `rappel_guid`         |

### `product`

| Champ unifié                    | RAPEX                                 | RappelConso                    |
|---------------------------------|---------------------------------------|--------------------------------|
| `product.specific_name`         | `product.nameSpecific`                | `libelle`                      |
| `product.type`                  | `product.versions.name`               | —                              |
| `product.description`           | `product.versions.description`        | `modeles_ou_references`        |
| `product.brand`                 | `product.brands[0].brand`             | `marque_produit`               |
| `product.family`                | —                                     | `categorie_produit`            |
| `product.category`              | `product.productCategory.key`         | `sous_categorie_produit`       |
| `product.counterfeit`           | `product.isCounterfeit.key`           | —                              |
| `product.barcodes`              | `product.barcodes[].barcode`          | `identification_produits`      |
| `product.batch_numbers`         | `product.batchNumbers[].batchNumber`  | `identification_produits`      |
| `product.model_references`      | `product.modelTypes[].modelType`      | `informations_complementaires` |
| `product.packaging_description` | `product.versions.packageDescription` | `conditionnements`             |
| `product.production_dates`      | —                                     | `identification_produits`      |

### `commercialization`

| Champ unifié                             | RAPEX                             | RappelConso                      |
|------------------------------------------|-----------------------------------|----------------------------------|
| `commercialization.origin_country_name`  | `traceability.countryOrigin.name` | —                                |
| `commercialization.alert_country_name`   | `country.name`                    | `"France"`                       |
| `commercialization.reacting_countries`   | `reactingCountries[].country.key` | `zone_geographique_de_vente`     |
| `commercialization.sold_online`          | `traceability.isSoldOnline.key`   | —                                |
| `commercialization.marketing_start_date` | —                                 | *(date début commercialisation)* |
| `commercialization.marketing_end_date`   | —                                 | *(date fin commercialisation)*   |
| `commercialization.distributors`         | —                                 | *(liste distributeurs)*          |

### `measures`

| Champ unifié                              | RAPEX                                                   | RappelConso                             |
|-------------------------------------------|---------------------------------------------------------|-----------------------------------------|
| `measures.recall_published_online`        | `measureTaken.hasPublishedRecallOnline.key`             | —                                       |
| `measures.measures_list[].category`       | `measureTaken.measures[].measureCategory.key`           | —                                       |
| `measures.measures_list[].other_category` | `measureTaken.measures[].versions.measureCategoryOther` | —                                       |
| `measures.measures_list[].type`           | `measureTaken.measures[].measureType.key`               | `nature_juridique_rappel`               |
| `measures.measures_list[].effective_date` | `measureTaken.measures[].entryIntoForceDate`            | —                                       |
| `measures.company_recalls[].url`          | `measureTaken.companyRecalls[].url`                     | —                                       |
| `measures.company_recalls[].language`     | `measureTaken.companyRecalls[].language`                | —                                       |
| `measures.consumer_actions`               | —                                                       | `conduites_a_tenir_par_le_consommateur` |
| `measures.compensation_terms`             | —                                                       | `modalites_de_compensation`             |
| `measures.procedure_end_date`             | —                                                       | `date_de_fin_de_la_procedure_de_rappel` |

### `media`

| Champ unifié             | RAPEX                                 | RappelConso                                |
|--------------------------|---------------------------------------|--------------------------------------------|
| `media.photos`           | `product.photos[].id` (+ préfixe URL) | `liens_vers_les_images` (séparés par `\|`) |
| `media.recall_sheet_url` | `reference` (+ préfixe URL)           | `lien_vers_la_fiche_rappel`                |

---

## Parsing du champ `identification_produits` (RappelConso)

Le champ `identification_produits` de RappelConso est un tableau de tokens textuels dont la structure n'est pas
formellement documentée. Le parser `IdentificationProduitsParser` analyse ces tokens pour en extraire des blocs
structurés de type `ProduitIdentifie`.

### Résultat du parsing

Le parsing produit un objet `IdentificationProduits` contenant :

| Champ       | Description                                                |
|-------------|------------------------------------------------------------|
| `blocs`     | Liste de `ProduitIdentifie` extraits (voir ci-dessous)     |
| `raw`       | Tokens bruts d'origine, conservés tels quels               |
| `pattern`   | Type de structure détecté (voir patterns ci-dessous)       |
| `categorie` | Catégorie du produit, transmise telle quelle pour contexte |

Chaque `ProduitIdentifie` contient :

| Champ       | Description                                                                  |
|-------------|------------------------------------------------------------------------------|
| `gtin`      | Code-barres EAN-13 ou EAN-14 (13-14 chiffres), `null` si absent              |
| `lot`       | Numéro ou description de lot (texte libre), `null` si absent                 |
| `typeDate`  | Type de date parmi les valeurs reconnues (voir ci-dessous), `null` si absent |
| `dateDebut` | Date de début au format `YYYY-MM-DD`, `null` si absente                      |
| `dateFin`   | Date de fin au format `YYYY-MM-DD`, `null` si absente                        |

### Classification des tokens

Chaque token est classifié automatiquement selon ces règles, dans l'ordre de priorité :

1. **SEP** — le token est `|` (séparateur de pipe, filtré avant le parsing des blocs)
2. **GTIN** — correspond à la regex `^\d{13,14}$` (code-barres EAN-13/14)
3. **DATE** — correspond à la regex `^\d{4}-\d{2}-\d{2}$` (date ISO)
4. **TYPE_DATE** — valeur exacte parmi :
    - `date limite de consommation`
    - `date de durabilité minimale`
    - `date de consommation recommandée`
    - `non concerné`
5. **LOT** — tout autre token (numéro de lot, texte libre)

### Pré-traitement

Avant classification, les tokens sont nettoyés :

- Les tokens vides ou blancs sont ignorés
- Les tokens contenant `|` sont éclatés en sous-tokens (ex. `|3666085407515` → `3666085407515`)
- Les tokens `|` isolés sont conservés puis filtrés comme séparateurs

### Logique de construction des blocs

Le parser parcourt les tokens classifiés séquentiellement et construit des blocs `ProduitIdentifie` :

- Un **GTIN** ouvre un nouveau bloc (et clôture le bloc précédent s'il y en avait un)
- Un **LOT** en début de séquence (hors bloc) ouvre un nouveau bloc sans GTIN
- Un **TYPE_DATE** en début de séquence (hors bloc) ouvre un nouveau bloc sans GTIN ni LOT
- À l'intérieur d'un bloc :
    - Un **LOT** est affecté au champ `lot` (concaténé si `lot` déjà rempli mais pas `typeDate`, sinon ouvre un nouveau
      bloc)
    - Un **TYPE_DATE** est affecté au champ `typeDate`
    - La première **DATE** rencontrée va dans `dateDebut`, la seconde dans `dateFin`

### Patterns détectés

Le champ `pattern` indique la structure reconnue :

| Pattern       | Signification                                                                         |
|---------------|---------------------------------------------------------------------------------------|
| `vide`        | Liste de tokens nulle ou vide                                                         |
| `texte_libre` | Un seul token, ni GTIN ni TYPE_DATE → stocké tel quel dans `lot`                      |
| `single_bloc` | Un seul bloc extrait, contenant au moins une date ou un type de date                  |
| `multi_blocs` | Plusieurs blocs extraits (plusieurs produits ou lots dans le même rappel)             |
| `incomplet`   | Un seul bloc sans date ni type de date, mais avec au moins un token structuré (GTIN…) |

### Exemples

**Bloc complet avec GTIN** :
`["3271620030518", "07326", "date limite de consommation", "2026-07-12"]`
→ pattern `single_bloc`, 1 bloc :
`gtin=3271620030518, lot=07326, typeDate=date limite de consommation, dateDebut=2026-07-12`

**Plage de dates** :
`["3760205420719", "tous les lots", "date de durabilité minimale", "2026-04-14", "2026-07-17"]`
→ pattern `single_bloc`, 1 bloc :
`gtin=3760205420719, lot=tous les lots, typeDate=date de durabilité minimale, dateDebut=2026-04-14, dateFin=2026-07-17`

**Multi-blocs** :
`["3760205423185", "lot A", "date de durabilité minimale", "2026-04-14", "2026-07-17", "3760205423208", "lot B", "date de durabilité minimale", "2026-04-14", "2026-07-17"]`
→ pattern `multi_blocs`, 2 blocs séparés par le second GTIN

**Sans GTIN** :
`["lot 2302", "date limite de consommation", "2026-05-03"]`
→ pattern `single_bloc`, 1 bloc : `gtin=null, lot=lot 2302, typeDate=date limite de consommation, dateDebut=2026-05-03`

**Séparateur pipe** :
`["d60760286", "date limite de consommation", "2026-03-25", "|", "d60780361", "date limite de consommation", "2026-03-27"]`
→ pattern `multi_blocs`, 2 blocs séparés par le pipe

**Texte libre** :
`["14.08.2024 - 14.04.2025"]`
→ pattern `texte_libre`, 1 bloc : `lot=14.08.2024 - 14.04.2025`
