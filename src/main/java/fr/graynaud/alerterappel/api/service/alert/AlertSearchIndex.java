package fr.graynaud.alerterappel.api.service.alert;

import fr.graynaud.alerterappel.api.service.alert.dto.Alert;
import fr.graynaud.alerterappel.api.service.alert.dto.SearchResult;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FeatureField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.expressions.Expression;
import org.apache.lucene.expressions.SimpleBindings;
import org.apache.lucene.expressions.js.JavascriptCompiler;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.function.FunctionScoreQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class AlertSearchIndex {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertSearchIndex.class);

    private static final String FIELD_FEATURES = "features";

    private static final String FIELD_ALERT_NUMBER = "alertNumber";
    private static final String FIELD_PRODUCT_NAME = "productName";
    private static final String FIELD_BRAND = "brand";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_BARCODES = "barcodes";
    private static final String FIELD_BATCH_NUMBERS = "batchNumbers";
    private static final String FIELD_MODEL_REFERENCES = "modelReferences";
    private static final String FIELD_DISTRIBUTORS = "distributors";
    private static final String FIELD_PUBLICATION_DATE = "publicationDate";

    private static final String[] STANDARD_FIELDS = {FIELD_ALERT_NUMBER, FIELD_BRAND, FIELD_BARCODES, FIELD_BATCH_NUMBERS, FIELD_MODEL_REFERENCES};

    private static final float PRODUCT_NAME_BOOST = 2.0f;
    private static final float DESCRIPTION_BOOST = 0.1f;
    private static final float DISTRIBUTORS_BOOST = 0.25f;

    private static final int MAX_RESULTS = 1000;

    private final Analyzer analyzer = new StandardAnalyzer(CharArraySet.EMPTY_SET);

    private volatile Directory directory;
    private volatile DirectoryReader reader;

    public synchronized void rebuild(Map<String, Alert> alerts) {
        try {
            Directory newDirectory = new ByteBuffersDirectory();
            IndexWriterConfig config = new IndexWriterConfig(this.analyzer);
            try (IndexWriter writer = new IndexWriter(newDirectory, config)) {
                for (Alert alert : alerts.values()) {
                    writer.addDocument(toDocument(alert));
                }
            }

            DirectoryReader oldReader = this.reader;
            this.reader = DirectoryReader.open(newDirectory);

            Directory oldDirectory = this.directory;
            this.directory = newDirectory;

            if (oldReader != null) {
                oldReader.close();
            }
            if (oldDirectory != null) {
                oldDirectory.close();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to rebuild search index", e);
        }
    }

    public SearchResult search(String queryString, int page, int size) {
        DirectoryReader currentReader = this.reader;
        if (currentReader == null) {
            return new SearchResult(List.of(), 0);
        }

        try {
            List<String> tokens = tokenize(queryString);
            if (tokens.isEmpty()) {
                return new SearchResult(List.of(), 0);
            }

            BooleanQuery.Builder outerQuery = new BooleanQuery.Builder();
            for (String token : tokens) {
                outerQuery.add(buildTokenQuery(token), BooleanClause.Occur.SHOULD);
            }

            IndexSearcher searcher = new IndexSearcher(currentReader);

            float pivot = 15768000000f; // 6 months
            Query recencyQuery = FeatureField.newSaturationQuery(FIELD_FEATURES, FIELD_PUBLICATION_DATE, 1f, pivot);

            BooleanQuery.Builder finalBoostedQuery = new BooleanQuery.Builder();
            finalBoostedQuery.add(outerQuery.build(), BooleanClause.Occur.MUST);
            finalBoostedQuery.add(recencyQuery, BooleanClause.Occur.SHOULD);

            int offset = page * size;
            TopDocs topDocs = searcher.search(finalBoostedQuery.build(), Math.min(offset + size, MAX_RESULTS));

            List<String> alertNumbers = new ArrayList<>();
            if (offset < topDocs.scoreDocs.length) {
                int end = Math.min(offset + size, topDocs.scoreDocs.length);
                for (int i = offset; i < end; i++) {
                    Document doc = searcher.storedFields().document(topDocs.scoreDocs[i].doc);
                    alertNumbers.add(doc.get(FIELD_ALERT_NUMBER));
                }
            }
            return new SearchResult(alertNumbers, topDocs.totalHits.value());
        } catch (Exception e) {
            LOGGER.error("Failed to execute search query: {}", queryString, e);
            return new SearchResult(List.of(), 0);
        }
    }

    private Query buildTokenQuery(String token) {
        BooleanQuery.Builder tokenQuery = new BooleanQuery.Builder();
        int maxEdits = autoFuzziness(token);

        tokenQuery.add(new BoostQuery(new TermQuery(new Term(FIELD_PRODUCT_NAME, token)), PRODUCT_NAME_BOOST), BooleanClause.Occur.SHOULD);
        tokenQuery.add(new BoostQuery(new TermQuery(new Term(FIELD_DESCRIPTION, token)), DESCRIPTION_BOOST), BooleanClause.Occur.SHOULD);
        tokenQuery.add(new BoostQuery(new TermQuery(new Term(FIELD_DISTRIBUTORS, token)), DISTRIBUTORS_BOOST), BooleanClause.Occur.SHOULD);
        if (maxEdits > 0) {
            tokenQuery.add(new BoostQuery(new FuzzyQuery(new Term(FIELD_PRODUCT_NAME, token), maxEdits), PRODUCT_NAME_BOOST), BooleanClause.Occur.SHOULD);
        }

        for (String field : STANDARD_FIELDS) {
            tokenQuery.add(new TermQuery(new Term(field, token)), BooleanClause.Occur.SHOULD);
            if (maxEdits > 0 && FIELD_BRAND.equals(field)) {
                tokenQuery.add(new FuzzyQuery(new Term(field, token), maxEdits), BooleanClause.Occur.SHOULD);
            }
        }

        return tokenQuery.build();
    }

    private List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        try (TokenStream stream = this.analyzer.tokenStream("", input)) {
            CharTermAttribute attr = stream.addAttribute(CharTermAttribute.class);
            stream.reset();
            while (stream.incrementToken()) {
                tokens.add(attr.toString());
            }
            stream.end();
        } catch (IOException e) {
            LOGGER.error("Failed to tokenize query: {}", input, e);
        }
        return tokens;
    }

    private static int autoFuzziness(String token) {
        if (token.length() < 3) {
            return 0;
        }
        return 1;
    }

    private Document toDocument(Alert alert) {
        Document doc = new Document();
        addStoredField(doc, FIELD_ALERT_NUMBER, alert.alertNumber());
        addTextField(doc, FIELD_ALERT_NUMBER, alert.alertNumber());

        long epochMillis = alert.publicationDate() != null ? alert.publicationDate().toInstant().toEpochMilli() : 0L;
        doc.add(new LongPoint(FIELD_PUBLICATION_DATE, epochMillis));
        doc.add(new FeatureField(FIELD_FEATURES, FIELD_PUBLICATION_DATE, epochMillis));

        if (alert.product() != null) {
            addTextField(doc, FIELD_PRODUCT_NAME, alert.product().specificName());
            addTextField(doc, FIELD_BRAND, alert.product().brand());
            addTextField(doc, FIELD_DESCRIPTION, alert.product().description());
            addAllTextField(doc, FIELD_BARCODES, alert.product().barcodes());
            addAllTextField(doc, FIELD_BATCH_NUMBERS, alert.product().batchNumbers());
            addAllTextField(doc, FIELD_MODEL_REFERENCES, alert.product().modelReferences());
        }

        if (alert.commercialization() != null) {
            addTextField(doc, FIELD_DISTRIBUTORS, alert.commercialization().distributors());
        }

        return doc;
    }

    private static void addStoredField(Document doc, String field, String value) {
        if (value != null && !value.isEmpty()) {
            doc.add(new StoredField(field, value));
        }
    }

    private static void addTextField(Document doc, String field, String value) {
        if (value != null && !value.isEmpty()) {
            doc.add(new TextField(field, value, Field.Store.NO));
        }
    }

    private static void addAllTextField(Document doc, String field, List<String> values) {
        if (!CollectionUtils.isEmpty(values)) {
            for (String value : values) {
                addTextField(doc, field, value);
            }
        }
    }
}
