package com.travelapp.itinerary.engine;

import com.travelapp.content.entity.Poi;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Rule engine: the fallback implementation for itinerary generation (itinerary_plans.generated_by = 1).
 * Doesn't depend on any external service and is always available -- falls back to here at day granularity
 * whenever the AI engine (GeminiItineraryPlanner) is unavailable or fails validation. buildDayItems/pick/toDraft,
 * originally written directly in ItineraryServiceImpl, were moved into this class with no logic changes, aside
 * from additionally supporting the "last day of a segment -> heading to the next destination" transport item
 * for multi-destination scenarios.
 */
@Component
public class RuleEngineDayPlanner {

    private static final BigDecimal SCENIC_FALLBACK_COST = BigDecimal.valueOf(50);
    private static final BigDecimal FOOD_FALLBACK_COST = BigDecimal.valueOf(60);
    private static final BigDecimal HOTEL_FALLBACK_COST = BigDecimal.valueOf(280);
    private static final BigDecimal TRANSFER_FALLBACK_COST = BigDecimal.valueOf(150);

    /**
     * Generates the itinerary items for a given day within this destination segment.
     *
     * @param segmentDayIndex   which day this is within the segment (1-based)
     * @param segmentTotalDays  total number of days in the segment
     * @param destinationName   the current destination's name
     * @param isTripFirstDay    whether this is the first day of the whole trip (global scope, not segment scope)
     * @param isTripLastDay     whether this is the last day of the whole trip
     * @param nextDestinationName the name of the next destination, when this is the segment's last day but not the whole trip's last day; null otherwise
     */
    public List<DraftItem> buildDayItems(int segmentDayIndex, int segmentTotalDays, String destinationName,
            List<Poi> scenicPois, List<Poi> foodPois, List<Poi> hotelPois,
            boolean isTripFirstDay, boolean isTripLastDay, String nextDestinationName) {
        List<DraftItem> items = new ArrayList<>();
        int dayIndex = segmentDayIndex - 1;
        boolean isSegmentLastDay = segmentDayIndex == segmentTotalDays;

        if (isTripFirstDay) {
            items.add(new DraftItem(LocalTime.of(9, 0), ItineraryCategories.TRANSPORT, null,
                    "Arrive in " + destinationName, "Recommend confirming check-in time with your accommodation in advance", BigDecimal.ZERO));
        }

        Poi morningScenic = pick(scenicPois, dayIndex * 2);
        items.add(toDraft(LocalTime.of(isTripFirstDay ? 11 : 9, 0), ItineraryCategories.SCENIC, morningScenic,
                "Free time: explore " + destinationName, SCENIC_FALLBACK_COST));

        Poi lunch = pick(foodPois, dayIndex);
        items.add(toDraft(LocalTime.of(12, 0), ItineraryCategories.FOOD, lunch, "Local specialty lunch", FOOD_FALLBACK_COST));

        Poi afternoonScenic = pick(scenicPois, dayIndex * 2 + 1);
        items.add(toDraft(LocalTime.of(14, 30), ItineraryCategories.SCENIC, afternoonScenic,
                "Continue sightseeing in " + destinationName, SCENIC_FALLBACK_COST));

        Poi dinner = pick(foodPois, dayIndex + 1);
        items.add(toDraft(LocalTime.of(18, 30), ItineraryCategories.FOOD, dinner, "Local specialty dinner", FOOD_FALLBACK_COST));

        if (isTripLastDay) {
            items.add(new DraftItem(LocalTime.of(20, 30), ItineraryCategories.TRANSPORT, null,
                    "Depart from " + destinationName, null, BigDecimal.ZERO));
        } else if (isSegmentLastDay && nextDestinationName != null) {
            items.add(new DraftItem(LocalTime.of(20, 30), ItineraryCategories.TRANSPORT, null,
                    "Travel from " + destinationName + " to " + nextDestinationName,
                    "Multi-destination transfer — recommend checking local transit schedules in advance", TRANSFER_FALLBACK_COST));
        } else {
            Poi hotel = pick(hotelPois, 0); // stay at the same hotel throughout the segment, closer to real booking habits
            items.add(toDraft(LocalTime.of(20, 30), ItineraryCategories.HOTEL, hotel, "Hotel check-in", HOTEL_FALLBACK_COST));
        }
        return items;
    }

    /** Rotates through POIs by day number, so the same morning/afternoon slot doesn't hit the same place; returns null when there's no data for the category (caller falls back to placeholder text). */
    private Poi pick(List<Poi> pois, int offset) {
        if (pois == null || pois.isEmpty()) return null;
        return pois.get(offset % pois.size());
    }

    private DraftItem toDraft(LocalTime time, int category, Poi poi, String fallbackTitle, BigDecimal fallbackCost) {
        if (poi == null) {
            return new DraftItem(time, category, null, fallbackTitle, null, fallbackCost);
        }
        BigDecimal cost = poi.getAvgCost() != null ? poi.getAvgCost() : fallbackCost;
        return new DraftItem(time, category, poi.getId(), poi.getName(), poi.getAddress(), cost);
    }
}
