package com.travelapp.itinerary.engine;

import java.math.BigDecimal;
import java.time.LocalTime;

/** A draft itinerary item not yet persisted; both the rule engine and the AI engine produce this structure, and the layer above (persistDays) only understands this one format. */
public record DraftItem(LocalTime startTime, int category, Long poiId, String title, String description, BigDecimal cost) {
}
