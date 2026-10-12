package com.travelapp.itinerary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Corresponds to a list item of section 5.6 of the "API Interface Document": GET /users/me/itinerary-plans */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryPlanSummaryDTO {
    private Long id;
    private Integer version;
    private ItineraryDestinationDTO destination;
    private Integer days;
    private BigDecimal totalEstimatedCost;
    /** generating/generated/failed */
    private String status;
    private LocalDateTime createdAt;
}
