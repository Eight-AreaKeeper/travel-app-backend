package com.travelapp.itinerary.engine;

import com.travelapp.content.entity.Poi;
import com.travelapp.customize.entity.CustomizeAnswer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AI engine: uses Gemini to do "personalized ranking + copywriting generation" for one destination segment
 * (itinerary_plans.generated_by = 2), corresponding to the "LLM personalized ranking + rule-based fallback"
 * design in section 3 (Core Challenges) of the "Project Technical Architecture Document".
 *
 * Hard constraint: candidate POIs can only be selected from the existing data in the pois table (the candidate
 * list passed in by the caller); the prompt explicitly requires that the model not invent nonexistent places.
 * The response is also validated a second time, day by day, in {@link #planSegment} -- any day where
 * "a nonexistent poiId is referenced / category is invalid / a required field is missing from the structure"
 * is discarded entirely; the caller (ItineraryPlanBuilder) is responsible for falling back to
 * {@link RuleEngineDayPlanner} for discarded days -- degrading at day granularity, not failing the whole order.
 */
@Slf4j
@Component
public class GeminiItineraryPlanner {

    private final ChatClient chatClient;
    private final boolean enabled;
    private final String apiKey;

    public GeminiItineraryPlanner(ChatClient.Builder chatClientBuilder,
            @Value("${app.ai.gemini.enabled:true}") boolean enabled,
            @Value("${app.ai.gemini.api-key:}") String apiKey) {
        this.chatClient = chatClientBuilder.build();
        this.enabled = enabled;
        this.apiKey = apiKey;
    }

    /** When enabled=false or api-key isn't configured, AI is treated as unavailable right away; the caller should skip the Gemini call and go straight to the rule engine instead of waiting on a network request that's bound to fail anyway. */
    public boolean isAvailable() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    /**
     * Generates a complete day-by-day draft for one destination segment.
     *
     * @return key = day number within the segment (1-based), value = the validated items for that day; days that
     *         fail validation will not appear in the returned Map, and the caller must fill in the missing keys
     *         using the rule engine. Hard failures at the network/parsing level throw an exception directly, and
     *         the caller should fall back for the entire segment.
     */
    public Map<Integer, List<DraftItem>> planSegment(DestinationSegment segment, int tripTotalDays,
            CustomizeAnswer answer, boolean isFirstSegment, boolean isLastSegment, String nextDestinationName) {
        Map<Long, Poi> poiById = new HashMap<>();
        segment.scenicPois().forEach(p -> poiById.put(p.getId(), p));
        segment.foodPois().forEach(p -> poiById.put(p.getId(), p));
        segment.hotelPois().forEach(p -> poiById.put(p.getId(), p));

        String prompt = buildPrompt(segment, tripTotalDays, answer, isFirstSegment, isLastSegment, nextDestinationName);
        AiSegmentPlan response = chatClient.prompt()
                .system("You are a professional travel itinerary planner. Output strictly in the required structured format, with no explanatory text.")
                .user(prompt)
                .call()
                .entity(AiSegmentPlan.class);

        Map<Integer, List<DraftItem>> result = new LinkedHashMap<>();
        if (response == null || response.days() == null) {
            return result;
        }
        Set<Long> allowedPoiIds = poiById.keySet();
        for (AiSegmentPlan.AiDayPlan day : response.days()) {
            List<DraftItem> drafts = validateAndConvert(day, segment, allowedPoiIds);
            if (drafts != null) {
                result.put(day.dayIndex(), drafts);
            } else {
                log.warn("Day {} returned by Gemini failed validation, falling back to the rule engine for this day, destination={}", day.dayIndex(), segment.destination().name());
            }
        }
        return result;
    }

    private List<DraftItem> validateAndConvert(AiSegmentPlan.AiDayPlan day, DestinationSegment segment, Set<Long> allowedPoiIds) {
        if (day == null || day.dayIndex() == null || day.items() == null || day.items().isEmpty()) {
            return null;
        }
        if (day.dayIndex() < 1 || day.dayIndex() > segment.segmentTotalDays()) {
            return null;
        }
        if (day.items().size() < 3) {
            return null; // too sparse, clearly not a complete day's itinerary
        }
        List<DraftItem> drafts = new java.util.ArrayList<>();
        for (AiSegmentPlan.AiItem item : day.items()) {
            if (item.category() == null || item.title() == null || item.title().isBlank()) {
                return null;
            }
            Integer category = ItineraryCategories.fromCode(item.category());
            if (category == null) {
                return null;
            }
            if (item.poiId() != null && !allowedPoiIds.contains(item.poiId())) {
                return null; // references a nonexistent candidate, meaning the model made up a place
            }
            java.time.LocalTime startTime = parseTime(item.startTime());
            BigDecimal cost = item.estimatedCost();
            drafts.add(new DraftItem(startTime, category, item.poiId(), item.title(), item.description(), cost));
        }
        return drafts;
    }

    private java.time.LocalTime parseTime(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return java.time.LocalTime.parse(raw.length() == 5 ? raw : raw.substring(0, 5));
        } catch (Exception e) {
            return null;
        }
    }

    private String buildPrompt(DestinationSegment segment, int tripTotalDays, CustomizeAnswer answer,
            boolean isFirstSegment, boolean isLastSegment, String nextDestinationName) {
        StringBuilder sb = new StringBuilder();
        sb.append("Please plan destination \"").append(segment.destination().name()).append("\" for day")
                .append(segment.dayFrom()).append(" through day").append(segment.dayTo())
                .append(" (").append(segment.segmentTotalDays()).append(" days total). Number dayIndex starting from 1 up to ")
                .append(segment.segmentTotalDays()).append(" (corresponding to day ").append(segment.dayFrom())
                .append(" through day ").append(segment.dayTo()).append(" of the whole trip).\n");
        sb.append("This is part of a trip that is ").append(tripTotalDays).append(" days in total. ");
        if (isFirstSegment) sb.append("This destination is the starting point of the whole trip; the first item on day 1 should be arrival transport (category=transport, poiId=null). ");
        if (isLastSegment) sb.append("This destination is the final stop of the whole trip; the last item on the last day should be departure transport (category=transport, poiId=null). ");
        if (!isLastSegment && nextDestinationName != null) {
            sb.append("This destination is not the final stop; at the end of the last day the traveler needs to head to the next stop \"").append(nextDestinationName)
                    .append("\" — make the last item transport to the next stop (category=transport, poiId=null), and do not schedule accommodation. ");
        }
        sb.append("\n\nAvailable candidate locations (choose only from the ids listed below; do not invent locations outside this list; try to spread each category across the day rather than repeating):\n");
        appendPoiList(sb, "Scenic spots (category=scenic)", segment.scenicPois());
        appendPoiList(sb, "Food (category=food)", segment.foodPois());
        appendPoiList(sb, "Accommodation (category=hotel, usually the same one for the whole stay at this destination)", segment.hotelPois());

        sb.append("\nUser preferences:\n");
        if (answer != null) {
            if (answer.getTravelStyle() != null) sb.append("- Travel style: ").append(answer.getTravelStyle()).append("\n");
            if (answer.getBudgetMin() != null || answer.getBudgetMax() != null) {
                sb.append("- Budget range: ").append(answer.getBudgetMin()).append(" - ").append(answer.getBudgetMax()).append(" CNY (total budget for the whole trip; please keep each day's spending within a reasonable range)\n");
            }
            if (answer.getTravelerCount() != null) sb.append("- Number of travelers: ").append(answer.getTravelerCount()).append("\n");
            if (answer.getTravelerComposition() != null) sb.append("- Traveler composition: ").append(answer.getTravelerComposition()).append("\n");
            if (answer.getPacePreference() != null) sb.append("- Pace preference: ").append(answer.getPacePreference()).append("\n");
            if (answer.getAccommodationType() != null) sb.append("- Accommodation type preference: ").append(answer.getAccommodationType()).append("\n");
            if (answer.getSpecialNeeds() != null && !answer.getSpecialNeeds().isBlank()) sb.append("- Special requirements: ").append(answer.getSpecialNeeds()).append("\n");
        }

        sb.append("\nEach day's plan should include: a morning scenic spot, lunch, an afternoon scenic spot, dinner, and a transport or accommodation item to close out the day (see the first/last day requirements above for specifics). ");
        sb.append("estimatedCost is the per-person estimated cost of this item (in CNY); for scenic spots/food use the avgCost from the candidate location as a reference, and provide a reasonable number yourself when no candidate is available; transport items can have estimatedCost of 0. ");
        sb.append("summary should describe this day's itinerary style in one English sentence.");
        return sb.toString();
    }

    private void appendPoiList(StringBuilder sb, String label, List<Poi> pois) {
        sb.append(label).append(":\n");
        if (pois == null || pois.isEmpty()) {
            sb.append("  (No candidates available yet — for this category, use a reasonable placeholder description and pass poiId as null)\n");
            return;
        }
        for (Poi poi : pois) {
            sb.append("  - id=").append(poi.getId()).append(" name=").append(poi.getName());
            if (poi.getAvgCost() != null) sb.append(" avgCostPerPerson=").append(poi.getAvgCost());
            if (poi.getRating() != null) sb.append(" rating=").append(poi.getRating());
            sb.append("\n");
        }
    }
}
