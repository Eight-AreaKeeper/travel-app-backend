package com.travelapp.itinerary.engine;

import com.travelapp.content.entity.Poi;

/** Central definition of the itinerary_items.category and pois.type codes, to avoid the two numbering schemes getting scattered and mixed up elsewhere. */
public final class ItineraryCategories {

    private ItineraryCategories() {
    }

    /** itinerary_items.category codes: 1=scenic / 2=dining / 3=transport / 4=accommodation -- not the same numbering as Poi.TYPE_*, be careful not to mix them up */
    public static final int SCENIC = 1;
    public static final int FOOD = 2;
    public static final int TRANSPORT = 3;
    public static final int HOTEL = 4;

    /** pois.type codes, directly reusing the definitions on the Poi entity to avoid the literals drifting apart in two places */
    public static final int POI_TYPE_SCENIC = Poi.TYPE_SCENIC;
    public static final int POI_TYPE_FOOD = Poi.TYPE_FOOD;
    public static final int POI_TYPE_HOTEL = Poi.TYPE_HOTEL;

    public static String toCode(Integer category) {
        if (category == null) return null;
        return switch (category) {
            case SCENIC -> "scenic";
            case FOOD -> "food";
            case TRANSPORT -> "transport";
            case HOTEL -> "hotel";
            default -> "other";
        };
    }

    /** Converts the AI/string form of category back to the database code; returns null when unrecognized, caller treats that as "invalid" */
    public static Integer fromCode(String code) {
        if (code == null) return null;
        return switch (code.trim().toLowerCase()) {
            case "scenic" -> SCENIC;
            case "food" -> FOOD;
            case "transport" -> TRANSPORT;
            case "hotel" -> HOTEL;
            default -> null;
        };
    }

    public static Integer categoryToPoiType(Integer category) {
        if (category == null) return null;
        if (category == SCENIC) return POI_TYPE_SCENIC;
        if (category == FOOD) return POI_TYPE_FOOD;
        if (category == HOTEL) return POI_TYPE_HOTEL;
        return null; // the transport category has no corresponding POI candidates
    }
}
