package fr.graynaud.alerterappel.api.service.source.rappelconso.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class IdentificationProduitsParser {

    private static final Pattern GTIN_PATTERN = Pattern.compile("^\\d{13,14}$");
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Set<String> TYPE_DATES = Set.of(
            "date limite de consommation",
            "date de durabilité minimale",
            "date de consommation recommandée",
            "non concerné"
    );

    private IdentificationProduitsParser() {}

    public static IdentificationProduits parse(List<String> tokens, String categorie) {
        if (tokens == null || tokens.isEmpty()) {
            return new IdentificationProduits(List.of(), tokens == null ? List.of() : tokens, "vide", categorie);
        }

        List<String> raw = List.copyOf(tokens);
        List<String> preprocessed = preprocess(tokens);

        if (preprocessed.size() == 1 && !isGtin(preprocessed.getFirst()) && !isTypeDate(preprocessed.getFirst())) {
            ProduitIdentifie bloc = new ProduitIdentifie(null, preprocessed.getFirst(), null, null, null);
            return new IdentificationProduits(List.of(bloc), raw, "texte_libre", categorie);
        }

        List<ClassifiedToken> classified = preprocessed.stream()
                                                       .map(IdentificationProduitsParser::classify)
                                                       .filter(ct -> ct.type() != TokenType.SEP)
                                                       .toList();

        List<ProduitIdentifie> blocs = parseBlocs(classified);

        if (blocs.isEmpty()) {
            return new IdentificationProduits(blocs, raw, "incomplet", categorie);
        }

        String pattern = detectPattern(blocs, classified);
        return new IdentificationProduits(blocs, raw, pattern, categorie);
    }

    private static List<String> preprocess(List<String> tokens) {
        List<String> result = new ArrayList<>();
        for (String token : tokens) {
            if (token == null || token.isBlank()) {
                continue;
            }

            if (token.contains("|") && !"|".equals(token.trim())) {
                for (String part : token.split("\\|", -1)) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        result.add(trimmed);
                    }
                }
            } else {
                String trimmed = token.trim();
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
        }
        return result;
    }

    private static ClassifiedToken classify(String token) {
        if ("|".equals(token)) {
            return new ClassifiedToken(token, TokenType.SEP);
        }
        if (isGtin(token)) {
            return new ClassifiedToken(token, TokenType.GTIN);
        }
        if (isDate(token)) {
            return new ClassifiedToken(token, TokenType.DATE);
        }
        if (isTypeDate(token)) {
            return new ClassifiedToken(token, TokenType.TYPE_DATE);
        }
        return new ClassifiedToken(token, TokenType.LOT);
    }

    private static boolean isGtin(String token) {
        return GTIN_PATTERN.matcher(token).matches();
    }

    private static boolean isDate(String token) {
        return DATE_PATTERN.matcher(token).matches();
    }

    private static boolean isTypeDate(String token) {
        return TYPE_DATES.contains(token);
    }

    private static List<ProduitIdentifie> parseBlocs(List<ClassifiedToken> tokens) {
        List<ProduitIdentifie> blocs = new ArrayList<>();

        String gtin = null;
        String lot = null;
        String typeDate = null;
        String dateDebut = null;
        String dateFin = null;
        boolean inBloc = false;

        for (int i = 0; i < tokens.size(); i++) {
            ClassifiedToken ct = tokens.get(i);

            if (ct.type() == TokenType.GTIN) {
                if (inBloc) {
                    blocs.add(new ProduitIdentifie(gtin, lot, typeDate, dateDebut, dateFin));
                    lot = null;
                    typeDate = null;
                    dateDebut = null;
                    dateFin = null;
                }
                gtin = ct.value();
                inBloc = true;
                continue;
            }

            if (!inBloc && ct.type() == TokenType.LOT) {
                gtin = null;
                lot = ct.value();
                inBloc = true;
                continue;
            }

            if (!inBloc && ct.type() == TokenType.TYPE_DATE) {
                gtin = null;
                lot = null;
                typeDate = ct.value();
                inBloc = true;
                continue;
            }

            if (inBloc) {
                switch (ct.type()) {
                    case LOT -> {
                        if (lot == null || typeDate == null) {
                            lot = lot == null ? ct.value() : lot + " " + ct.value();
                        } else {
                            blocs.add(new ProduitIdentifie(gtin, lot, typeDate, dateDebut, dateFin));
                            gtin = null;
                            typeDate = null;
                            dateDebut = null;
                            dateFin = null;
                            lot = ct.value();
                        }
                    }
                    case TYPE_DATE -> typeDate = ct.value();
                    case DATE -> {
                        if (dateDebut == null) {
                            dateDebut = ct.value();
                        } else {
                            dateFin = ct.value();
                        }
                    }
                    default -> {}
                }
            }
        }

        if (inBloc) {
            blocs.add(new ProduitIdentifie(gtin, lot, typeDate, dateDebut, dateFin));
        }

        return blocs;
    }

    private static String detectPattern(List<ProduitIdentifie> blocs, List<ClassifiedToken> tokens) {
        if (blocs.size() > 1) {
            return "multi_blocs";
        }

        ProduitIdentifie bloc = blocs.getFirst();
        if (bloc.typeDate() != null || bloc.dateDebut() != null) {
            return "single_bloc";
        }

        boolean hasAnyStructured = tokens.stream().anyMatch(ct -> ct.type() == TokenType.GTIN || ct.type() == TokenType.TYPE_DATE || ct.type() == TokenType.DATE);
        if (!hasAnyStructured) {
            return "texte_libre";
        }

        return "incomplet";
    }

    private enum TokenType {
        GTIN, DATE, TYPE_DATE, SEP, LOT
    }

    private record ClassifiedToken(String value, TokenType type) {}
}
