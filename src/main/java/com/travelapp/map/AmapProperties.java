package com.travelapp.map;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AMap Web Service API configuration, corresponding to app.map.amap.* in application.yml.
 * When key is blank or enabled=false, AmapPoiService.ensureCached simply skips, which doesn't
 * affect itinerary generation (existing local pois table data is used instead).
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.map.amap")
public class AmapProperties {
    private boolean enabled = true;
    private String key;
    private int minPoiPerType = 6;

    public boolean isUsable() {
        return enabled && key != null && !key.isBlank();
    }
}
