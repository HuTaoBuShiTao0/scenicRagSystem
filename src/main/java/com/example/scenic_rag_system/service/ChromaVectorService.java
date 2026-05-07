package com.example.scenic_rag_system.service;

import com.example.scenic_rag_system.dto.VectorDocument;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ChromaVectorService {

    @Value("${chroma.host}")
    private String host;

    @Value("${chroma.port}")
    private int port;

    @Value("${chroma.collection-prefix}")
    private String prefix;

    private final ObjectMapper objectMapper;
    private final LocalEmbeddingService embeddingService;
    private final Map<String, String> cache = new HashMap<>();

    public ChromaVectorService(ObjectMapper objectMapper, LocalEmbeddingService embeddingService) {
        this.objectMapper = objectMapper;
        this.embeddingService = embeddingService;
    }

    private String base() {
        return "http://" + host + ":" + port + "/api/v2";
    }

    private String colUrl() {
        return base() + "/tenants/default_tenant/databases/default_database/collections";
    }

    private String httpPost(String url, String body) {
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                return new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            }
            try {
                return new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            } catch (Exception e2) {
                return null;
            }
        } catch (Exception e) {
            log.warn("POST {} error: {}", url, e.getMessage());
            return null;
        }
    }

    public synchronized String getOrCreateCollection(String name) {
        String cached = cache.get(name);
        if (cached != null) return cached;

        String full = prefix + name;
        ObjectNode body = objectMapper.createObjectNode();
        body.put("name", full);
        body.put("get_or_create", true);

        String resp = httpPost(colUrl(), body.toString());
        if (resp != null) {
            try {
                String id = objectMapper.readTree(resp).path("id").asText("");
                if (!id.isEmpty()) {
                    cache.put(name, id);
                    log.info("Collection ready: {}", full);
                    return id;
                }
            } catch (Exception e) {
                log.warn("Parse collection resp failed: {}", e.getMessage());
            }
        }
        log.error("Failed to get/create collection: {}", full);
        return null;
    }

    public void addDocuments(String collectionName, List<VectorDocument> docs) {
        String colId = getOrCreateCollection(collectionName);
        if (colId == null) return;

        ObjectNode body = objectMapper.createObjectNode();
        ArrayNode ids = body.putArray("ids");
        ArrayNode embeddings = body.putArray("embeddings");
        ArrayNode metadatas = body.putArray("metadatas");
        ArrayNode documents = body.putArray("documents");

        for (VectorDocument doc : docs) {
            ids.add(doc.getId());
            List<Double> emb = doc.getEmbedding();
            if (emb == null || emb.isEmpty()) {
                emb = embeddingService.embed(doc.getText() != null ? doc.getText() : "");
            }
            ArrayNode ea = embeddings.addArray();
            for (double v : emb) ea.add(v);
            if (doc.getMetadata() != null) {
                ObjectNode mn = metadatas.addObject();
                for (Map.Entry<String, Object> e : doc.getMetadata().entrySet()) {
                    mn.putPOJO(e.getKey(), e.getValue());
                }
            } else {
                metadatas.addNull();
            }
            documents.add(doc.getText() != null ? doc.getText() : "");
        }

        String url = colUrl() + "/" + colId + "/add";
        String resp = httpPost(url, body.toString());
        if (resp != null) {
            log.info("Added {} docs to {}", docs.size(), collectionName);
        } else {
            log.error("Add to {} failed (null response)", collectionName);
        }
    }

    public List<VectorDocument> search(String collectionName, String queryText, int topK) {
        log.info("Searching {} for: {}", collectionName, queryText);
        String colId = getOrCreateCollection(collectionName);
        if (colId == null) return Collections.emptyList();

        List<Double> emb = embeddingService.embed(queryText);
        if (emb.isEmpty()) return Collections.emptyList();

        ObjectNode body = objectMapper.createObjectNode();
        ArrayNode qea = body.putArray("query_embeddings");
        ArrayNode ea = qea.addArray();
        for (double v : emb) ea.add(v);
        body.put("n_results", topK);
        ArrayNode inc = body.putArray("include");
        inc.add("metadatas");
        inc.add("documents");
        inc.add("distances");

        String url = colUrl() + "/" + colId + "/query";
        String json = httpPost(url, body.toString());
        if (json == null) {
            log.warn("Search {} returned null", collectionName);
            return Collections.emptyList();
        }

        try {
            List<VectorDocument> results = new ArrayList<>();
            JsonNode root = objectMapper.readTree(json);
            JsonNode idsNode = root.path("ids");
            if (idsNode.isArray() && idsNode.size() > 0) idsNode = idsNode.get(0);
            if (idsNode == null || !idsNode.isArray() || idsNode.isEmpty()) {
                log.info("Search {} returned 0 results", collectionName);
                return results;
            }
            JsonNode distNode = root.path("distances");
            if (distNode.isArray() && distNode.size() > 0) distNode = distNode.get(0);
            JsonNode metaNode = root.path("metadatas");
            if (metaNode.isArray() && metaNode.size() > 0) metaNode = metaNode.get(0);
            JsonNode docNode = root.path("documents");
            if (docNode.isArray() && docNode.size() > 0) docNode = docNode.get(0);

            for (int i = 0; i < idsNode.size(); i++) {
                double dist = (distNode != null && i < distNode.size()) ? distNode.get(i).asDouble(0) : 0;
                Map<String, Object> meta = new HashMap<>();
                if (metaNode != null && i < metaNode.size() && !metaNode.get(i).isNull()) {
                    JsonNode mn = metaNode.get(i);
                    mn.fields().forEachRemaining(f -> meta.put(f.getKey(), f.getValue().asText()));
                }
                results.add(VectorDocument.builder()
                    .id(idsNode.get(i).asText())
                    .score(1.0 / (1.0 + dist))
                    .text(docNode != null && i < docNode.size() ? docNode.get(i).asText() : "")
                    .metadata(meta)
                    .build());
            }
            log.info("Search {} returned {} results", collectionName, results.size());
            return results;
        } catch (Exception e) {
            log.error("Parse search results failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public void deleteCollection(String name) {
        try {
            String id = getOrCreateCollection(name);
            if (id != null) {
                HttpURLConnection conn = (HttpURLConnection) URI.create(colUrl() + "/" + id).toURL().openConnection();
                conn.setRequestMethod("DELETE");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.getResponseCode();
                cache.remove(name);
            }
        } catch (Exception e) {
            log.warn("Delete collection {} failed: {}", name, e.getMessage());
        }
    }
}
