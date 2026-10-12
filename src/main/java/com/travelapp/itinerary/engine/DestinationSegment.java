package com.travelapp.itinerary.engine;

import com.travelapp.content.entity.Poi;

import java.util.List;

/**
 * In a multi-destination trip, the day range (dayFrom..dayTo, both being the trip-wide day_number, 1-based)
 * that a given destination occupies within the whole trip.
 * A single-destination trip degenerates to exactly one segment covering all days.
 */
public record DestinationSegment(
        DestinationRef destination,
        int dayFrom,
        int dayTo,
        List<Poi> scenicPois,
        List<Poi> foodPois,
        List<Poi> hotelPois) {

    /** Trip-wide day number -> the day number within this destination (1-based) */
    public int segmentDayIndex(int tripDayNumber) {
        return tripDayNumber - dayFrom + 1;
    }

    public int segmentTotalDays() {
        return dayTo - dayFrom + 1;
    }

    public boolean contains(int tripDayNumber) {
        return tripDayNumber >= dayFrom && tripDayNumber <= dayTo;
    }
}
