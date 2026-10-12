package com.travelapp.itinerary.mq;

/**
 * Message body for an itinerary generation task. When basedOnPlanId is non-null, it's a "regeneration" scenario --
 * the consumer should use it to look up items with isUserEdited=true in the old plan and preserve them as much as
 * possible when generating the new version; basedOnPlanId is null on the first generation (complete()).
 */
public record ItineraryGenerationMessage(Long planId, Long sessionId, Long userId, Long basedOnPlanId) {
}
