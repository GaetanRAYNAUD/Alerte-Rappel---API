package fr.graynaud.alerterappel.api.service.source.rappelconso.dto;

public record ProduitIdentifie(
        String gtin,
        String lot,
        String typeDate,
        String dateDebut,
        String dateFin
) {}
