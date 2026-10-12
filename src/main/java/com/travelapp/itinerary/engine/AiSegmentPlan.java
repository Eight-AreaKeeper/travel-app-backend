package com.travelapp.itinerary.engine;

import java.math.BigDecimal;
import java.util.List;

/**
 * Target schema for Gemini's structured output, corresponding to {@code ChatClient...call().entity(AiSegmentPlan.class)}.
 * Spring AI auto-generates the format instructions from this record's field structure and injects them into the
 * prompt, so there's no need to hand-write a JSON schema.
 */
public record AiSegmentPlan(List<AiDayPlan> days) {

    public record AiDayPlan(
            /** Which day within this destination segment, 1-based, must correspond one-to-one with the day range given in the request */
            Integer dayIndex,
            /** A one-sentence Chinese description of the day's style, e.g. "Leisurely stroll along the coastline, watch the sunset in the evening" */
            String summary,
            List<AiItem> items) {
    }

    public record AiItem(
            /** HH:mm format, e.g. "09:00" */
            String startTime,
            /** Must be one of scenic/food/transport/hotel */
            String category,
            /** Must be one of the candidate POI ids given in the request; pass null for the transport category or when there really is no suitable candidate */
            Long poiId,
            String title,
            String description,
            BigDecimal estimatedCost) {
    }
}
