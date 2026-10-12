package com.travelapp.map;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelapp.content.entity.Poi;
import com.travelapp.content.mapper.PoiMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Live AMap POI backfill: when the local pois table doesn't have enough data for a given
 * destination and category, call the AMap API in real time to top up a batch as cache
 * (source=third-party API), so the next itinerary generation for the same destination can use
 * local data directly instead of hitting AMap every time.
 *
 * Any failure (key not configured / disabled / network error / AMap returns an error) is
 * non-fatal — it's simply skipped, and itinerary generation proceeds as usual with whatever
 * local data exists; even an empty list is fine, since
 * {@link com.travelapp.itinerary.engine.RuleEngineDayPlanner} can fall back to default copy/pricing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AmapPoiService {

    private final AmapPoiClient client;
    private final PoiMapper poiMapper;
    private final AmapProperties properties;

    /** Backfills POIs by destination name (free text); destinationName is not required to have a matching record in the local destinations table. */
    public void ensureCached(String destinationName, int poiType) {
        if (!properties.isUsable()) return;
        long existing = poiMapper.selectCount(new LambdaQueryWrapper<Poi>()
                .eq(Poi::getDestinationName, destinationName).eq(Poi::getType, poiType));
        if (existing >= properties.getMinPoiPerType()) return;

        String typeCode = typeCodeFor(poiType);
        String keywords = keywordFor(poiType);
        if (typeCode == null) return;

        List<AmapPoiResult> results = client.search(destinationName, typeCode, keywords);
        if (results.isEmpty()) return;

        Set<String> existingNames = poiMapper.selectList(new LambdaQueryWrapper<Poi>()
                        .eq(Poi::getDestinationName, destinationName).eq(Poi::getType, poiType))
                .stream().map(Poi::getName).collect(Collectors.toSet());

        int inserted = 0;
        for (AmapPoiResult result : results) {
            if (existingNames.contains(result.name())) continue;
            Poi poi = new Poi();
            poi.setName(result.name());
            poi.setType(poiType);
            poi.setDestinationName(destinationName);
            poi.setAddress(result.address());
            poi.setLatitude(result.latitude());
            poi.setLongitude(result.longitude());
            poi.setAvgCost(result.avgCost());
            poi.setRating(result.rating());
            poi.setSource(Poi.SOURCE_THIRD_PARTY_API);
            poiMapper.insert(poi);
            existingNames.add(result.name());
            inserted++;
        }
        log.info("AMap POI backfill complete: destinationName={}, type={}, {} new records added", destinationName, poiType, inserted);
    }

    private String typeCodeFor(int poiType) {
        if (poiType == Poi.TYPE_SCENIC) return "110000";
        if (poiType == Poi.TYPE_FOOD) return "050000";
        if (poiType == Poi.TYPE_HOTEL) return "100000";
        return null;
    }

    private String keywordFor(int poiType) {
        if (poiType == Poi.TYPE_SCENIC) return "scenic spot";
        if (poiType == Poi.TYPE_FOOD) return "food";
        if (poiType == Poi.TYPE_HOTEL) return "hotel";
        return "";
    }
}
