package com.travelapp.itinerary.engine;

/**
 * The unified representation of a "destination" in the customize/itinerary module: name is always non-null
 * (the raw text entered by the user), id can be null (only populated when matched against the local destinations
 * table, purely for display enhancement, e.g. cover images).
 * From here on, this chain (customize/itinerary/pois) associates by name and doesn't require a matching record
 * to exist in the local database -- the travel notes module (travel_notes) is unaffected and continues to use
 * the separate closed-source Destination entity.
 */
public record DestinationRef(Long id, String name) {
}
