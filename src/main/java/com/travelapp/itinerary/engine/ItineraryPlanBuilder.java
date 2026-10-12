package com.travelapp.itinerary.engine;

import com.travelapp.customize.entity.CustomizeAnswer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestration entry point for itinerary generation: for each destination segment, try Gemini first, validate
 * day by day, and fill in with the rule engine for days that fail validation or hit a call exception.
 * ItineraryServiceImpl only needs to call {@link #build} and doesn't need to care how the AI and rule engine
 * coordinate internally.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ItineraryPlanBuilder {

    private final GeminiItineraryPlanner aiPlanner;
    private final RuleEngineDayPlanner ruleEngine;

    public record BuildResult(Map<Integer, List<DraftItem>> itemsByDay, boolean usedAi) {
    }

    public BuildResult build(List<DestinationSegment> segments, int tripTotalDays, CustomizeAnswer answer) {
        Map<Integer, List<DraftItem>> itemsByDay = new LinkedHashMap<>();
        boolean usedAiAny = false;

        for (int i = 0; i < segments.size(); i++) {
            DestinationSegment segment = segments.get(i);
            boolean isFirstSegment = i == 0;
            boolean isLastSegment = i == segments.size() - 1;
            String nextDestinationName = isLastSegment ? null : segments.get(i + 1).destination().name();

            Map<Integer, List<DraftItem>> aiDaysForSegment = Map.of();
            if (aiPlanner.isAvailable()) {
                try {
                    aiDaysForSegment = aiPlanner.planSegment(segment, tripTotalDays, answer, isFirstSegment, isLastSegment, nextDestinationName);
                } catch (Exception e) {
                    log.warn("Gemini generation threw an exception, destination={}, falling back to the rule engine for the whole segment", segment.destination().name(), e);
                }
            }

            for (int tripDay = segment.dayFrom(); tripDay <= segment.dayTo(); tripDay++) {
                int segmentDayIndex = segment.segmentDayIndex(tripDay);
                List<DraftItem> aiItems = aiDaysForSegment.get(segmentDayIndex);
                if (aiItems != null) {
                    itemsByDay.put(tripDay, aiItems);
                    usedAiAny = true;
                } else {
                    boolean isTripFirstDay = tripDay == 1;
                    boolean isTripLastDay = tripDay == tripTotalDays;
                    itemsByDay.put(tripDay, ruleEngine.buildDayItems(segmentDayIndex, segment.segmentTotalDays(),
                            segment.destination().name(), segment.scenicPois(), segment.foodPois(), segment.hotelPois(),
                            isTripFirstDay, isTripLastDay, nextDestinationName));
                }
            }
        }
        return new BuildResult(itemsByDay, usedAiAny);
    }
}
