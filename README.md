# Alerte Rappel - API

Alerte Rappel est une API qui centralise les alertes de rappel de produits de consommation provenant de deux sources officielles françaises et européennes.

## Principe

Lorsqu'un produit est jugé dangereux ou non conforme, les autorités compétentes publient des avis de rappel afin d'informer les consommateurs et d'organiser le retrait du produit du marché. Ces informations sont diffusées par plusieurs organismes, chacun avec son propre format et ses propres données.

L'objectif d'Alerte Rappel est d'agréger ces sources dans un format unifié, afin de disposer d'un point d'accès unique et cohérent à l'ensemble des rappels disponibles.

## Sources de données

### RappelConso

[RappelConso](https://rappelconso.beta.gouv.fr/) est la plateforme officielle du gouvernement français dédiée aux rappels de produits. Elle recense les avis de rappel publiés par les professionnels, couvrant une large variété de catégories : alimentation, jouets, électroménager, cosmétiques, véhicules, etc.

Les données sont mises à disposition en open data via la plateforme [data.gouv.fr](https://www.data.gouv.fr/).

### Safety Gate / RAPEX

[Safety Gate](https://ec.europa.eu/safety-gate/) (anciennement RAPEX) est le système d'alerte rapide de l'Union européenne pour les produits de consommation dangereux. Il permet aux États membres de signaler et de partager les informations sur les produits présentant un risque sérieux pour la santé ou la sécurité des consommateurs.

Les alertes publiées sur Safety Gate concernent l'ensemble des pays membres de l'UE et sont accessibles via une API officielle de la Commission européenne.

## Technique

### Prérequis

- Java 25+
- Maven (ou utiliser le wrapper inclus `./mvnw`)

### Build

```bash
# Compiler et packager
./mvnw clean package

# Lancer l'application
./mvnw spring-boot:run

# Lancer les tests
./mvnw test
```

## Licence

Ce projet est distribué sous licence [MIT](LICENSE).
