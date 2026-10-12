package com.travelapp.map;

import java.math.BigDecimal;

/** Internal representation of an AMap POI search result; fields are kept aligned with the Poi entity as much as possible so AmapPoiService can map it directly into the database. */
public record AmapPoiResult(String name, String address, BigDecimal longitude, BigDecimal latitude,
        BigDecimal avgCost, BigDecimal rating) {
}
