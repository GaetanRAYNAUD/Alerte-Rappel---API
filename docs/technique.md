# Documentation technique

## Vue d'ensemble

Alerte Rappel API est une application **Spring Boot 4.0.5** tournant sur **Java 25** qui agrège les rappels de produits
de consommation provenant de deux sources officielles et les expose via une API REST unifiée.

L'application fonctionne entièrement en mémoire : les données sont chargées depuis un fichier JSON au démarrage,
indexées, puis servies directement depuis le cache. Il n'y a pas de base de données.

## Stack technique

- **Java 25** avec virtual threads
- **Spring Boot 4.0.5** (Web, Validation, Scheduling)
- **Apache Lucene 10.4** pour la recherche fulltext en mémoire
- **Jackson** pour la sérialisation JSON
- **Maven** (wrapper inclus)

## Architecture

### Sources de données

Deux sources sont importées via l'API Opendatasoft Explore 2.1 :

| Source                  | Organisme                                     | Cron              | Propriétés             |
|-------------------------|-----------------------------------------------|-------------------|------------------------|
| **RappelConso**         | Gouvernement français (data.economie.gouv.fr) | Toutes les 15 min | `source.rappelconso.*` |
| **Safety Gate / RAPEX** | Commission européenne                         | Toutes les heures | `source.rapex.*`       |

Chaque source est implémentée comme un service héritant de `Explore21Service<D>`, qui gère :

- L'appel paginé à l'API Opendatasoft
- La détection des nouvelles données via la date du dernier import
- Le verrouillage par fichier (`FileLock`) pour éviter les imports concurrents
- La journalisation du rate limit

Les données RAPEX sont d'abord listées via Opendatasoft, puis chaque notification est récupérée individuellement via
l'API officielle Safety Gate (`/public/api/notification/{id}`) pour obtenir les détails complets.

### Schéma unifié

Les deux sources sont fusionnées dans un schéma unifié (`Alert`) documenté dans `docs/schema.md`. La clé de jointure est
le champ `alertNumber` (toujours en majuscules), qui correspond à `reference` (RAPEX) et `numero_fiche` (RappelConso).

Lorsqu'une alerte existe dans les deux sources, `AlertMerger` fusionne les données avec priorité à RAPEX pour la plupart
des champs. Les listes sont dédupliquées par comparaison normalisée (suppression des caractères non-alphanumériques,
passage en minuscules).

### Persistance

Les données sont stockées dans un fichier JSON unique situé à `${DATA_PATH}/data.json`. Chaque source maintient aussi un
fichier de suivi sous `${DATA_PATH}/source/<nom>.json` contenant la date du dernier import.

Un fichier `.lock` adjacent assure le verrouillage inter-processus via `java.nio.channels.FileLock`.

Au démarrage, `AlertService` charge le fichier JSON en mémoire et reconstruit les index. Un `WatchService` surveille le
fichier pour recharger automatiquement si une modification externe est détectée.

### Index en mémoire

`AlertService` maintient trois structures :

| Index          | Type                               | Usage                                                 |
|----------------|------------------------------------|-------------------------------------------------------|
| `alerts`       | `ConcurrentHashMap<String, Alert>` | Accès par numéro d'alerte                             |
| `barcodeIndex` | `ConcurrentHashMap<String, Alert>` | Accès par code-barres (alerte la plus récente)        |
| `sortedByDate` | `List<Alert>` (immuable)           | Pagination triée par date de publication décroissante |

### Recherche fulltext (Lucene)

`AlertSearchIndex` gère un index Lucene en mémoire (`ByteBuffersDirectory`), reconstruit à chaque rechargement des
données.

**Champs indexés :**

| Champ Lucene      | Source                           | Boost |
|-------------------|----------------------------------|-------|
| `productName`     | `product.specificName`           | x2    |
| `alertNumber`     | `alertNumber`                    | x1    |
| `brand`           | `product.brand`                  | x1    |
| `description`     | `product.description`            | x1    |
| `barcodes`        | `product.barcodes`               | x1    |
| `batchNumbers`    | `product.batchNumbers`           | x1    |
| `modelReferences` | `product.modelReferences`        | x1    |
| `riskDescription` | `riskDescription`                | x1    |
| `distributors`    | `commercialization.distributors` | x1    |

**Comportement de la recherche :**

- La requête utilisateur est tokenisée via le `StandardAnalyzer` (sans stopwords)
- Chaque token génère une requête OR sur tous les champs
- Les tokens de 3 caractères ou plus bénéficient d'une recherche floue (fuzzy) avec une distance d'édition de 1 (
  similaire au mode `AUTO` d'Elasticsearch)
- Les résultats sont triés par score de pertinence (TF-IDF)

## API REST

Base path : `/public/alerts`

### Endpoints

| Méthode | Path                 | Paramètres                       | Description                                         |
|---------|----------------------|----------------------------------|-----------------------------------------------------|
| `GET`   | `/search`            | `q` (requis), `page` (défaut: 0) | Recherche fulltext paginée (15 résultats/page)      |
| `GET`   | `/latest`            | `page` (défaut: 0)               | Dernières alertes par date de publication (15/page) |
| `GET`   | `/{alertNumber}`     | -                                | Alerte par numéro (ex: `/SR/00842/26`)              |
| `GET`   | `/barcode/{barcode}` | -                                | Alerte la plus récente pour un code-barres          |

### Format de réponse paginée

```json
{
    "content": [
        ...
    ],
    "page": 0,
    "size": 15,
    "totalPages": 42,
    "totalElements": 625
}
```

## Configuration

### Profils Spring

| Profil  | Fichier                                          | Usage               |
|---------|--------------------------------------------------|---------------------|
| default | `application.properties`                         | Production          |
| local   | `application-local.properties`                   | Développement local |
| test    | `src/test/resources/application-test.properties` | Tests               |

### Propriétés principales

| Propriété                 | Description                        | Défaut                   |
|---------------------------|------------------------------------|--------------------------|
| `server.port`             | Port HTTP                          | `8080` (env `PORT`)      |
| `data.path`               | Répertoire de stockage des données | `data` (env `DATA_PATH`) |
| `cors.allowed-origins`    | Origines CORS autorisées           | env `CORS`               |
| `source.rappelconso.cron` | Cron d'import RappelConso          | `0 */15 * * * *`         |
| `source.rapex.cron`       | Cron d'import RAPEX                | `0 0 * * * *`            |

### JVM

Lucene nécessite le module Vector API pour des performances optimales :

```
--add-modules jdk.incubator.vector
```

Ce paramètre est configuré dans :

- `pom.xml` (plugins `spring-boot-maven-plugin` et `maven-surefire-plugin`)
- `.run/AlerteRappelApiApplication.run.xml` (configuration IntelliJ)

## Lancement

```bash
# Build
./mvnw clean package

# Lancement
./mvnw spring-boot:run

# Lancement en profil local
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Tests
./mvnw test
```
