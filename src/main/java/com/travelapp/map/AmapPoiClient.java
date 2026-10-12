package com.travelapp.map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Thin wrapper around the AMap Web Service v3 POI search ("keyword search") API.
 * Manual parsing with JsonNode is used instead of strict POJO mapping because AMap's biz_ext
 * field returns "[]" (array) instead of "{}" (object) when there's no data; strict type mapping
 * would easily throw exceptions over this inconsistency. JsonNode.path() tolerates this
 * (accessing a missing field returns MissingNode instead of throwing), which better suits
 * integrating with this somewhat irregular third-party JSON.
 */
@Slf4j
@Component
public class AmapPoiClient {

    private final RestClient restClient;
    private final AmapProperties properties;
    private final ObjectMapper objectMapper;

    public AmapPoiClient(RestClient.Builder builder, AmapProperties properties, ObjectMapper objectMapper) {
        this.restClient = builder.baseUrl("https://restapi.amap.com").build();
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * @param city     used as AMap's city parameter; pass the destination name directly (e.g. "Dali")
     * @param typeCode AMap POI type code: scenic 110000 / food 050000 / lodging 100000
     * @param keywords supplementary keywords (e.g. "scenic spot"/"food"/"hotel"), used together with types to improve relevance
     */
    public List<AmapPoiResult> search(String city, String typeCode, String keywords) {
        try {
            String json = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/v3/place/text")
                            .queryParam("key", properties.getKey())
                            .queryParam("city", city)
                            .queryParam("citylimit", "true")
                            .queryParam("types", typeCode)
                            .queryParam("keywords", keywords)
                            .queryParam("offset", 20)
                            .queryParam("page", 1)
                            .queryParam("extensions", "all")
                            .build())
                    .retrieve()
                    .body(String.class);
            return parse(json);
        } catch (Exception e) {
            log.warn("AMap POI search call failed: city={}, types={}", city, typeCode, e);
            return List.of();
        }
    }

    private List<AmapPoiResult> parse(String json) {
        List<AmapPoiResult> results = new ArrayList<>();
        if (json == null || json.isBlank()) return results;
        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception e) {
            log.warn("Failed to parse AMap POI response JSON", e);
            return results;
        }
        if (!"1".equals(root.path("status").asText())) {
            log.warn("AMap POI search returned a non-success status: info={}", root.path("info").asText());
            return results;
        }
        for (JsonNode poiNode : root.path("pois")) {
            String name = poiNode.path("name").asText(null);
            if (name == null || name.isBlank()) continue;
            String address = poiNode.path("address").asText(null);
            BigDecimal[] lngLat = parseLocation(poiNode.path("location").asText(null));
            BigDecimal avgCost = parseDecimal(poiNode.path("biz_ext").path("cost").asText(null));
            BigDecimal rating = parseDecimal(poiNode.path("biz_ext").path("rating").asText(null));
            results.add(new AmapPoiResult(name, address, lngLat[0], lngLat[1], avgCost, rating));
        }
        return results;
    }

    private BigDecimal[] parseLocation(String raw) {
        if (raw == null || !raw.contains(",")) return new BigDecimal[]{null, null};
        try {
            String[] parts = raw.split(",", 2);
            return new BigDecimal[]{new BigDecimal(parts[0].trim()), new BigDecimal(parts[1].trim())};
        } catch (Exception e) {
            return new BigDecimal[]{null, null};
        }
    }

    private BigDecimal parseDecimal(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return new BigDecimal(raw.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
