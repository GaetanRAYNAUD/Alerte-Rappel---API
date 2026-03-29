package fr.graynaud.alerterappel.api.service.source.rappelconso.dto;

import java.util.List;

public record IdentificationProduits(
        List<ProduitIdentifie> blocs,
        List<String> raw,
        String pattern,
        String categorie
) {}
