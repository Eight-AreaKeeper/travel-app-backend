package com.travelapp.itinerary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** Corresponds to section 5.1 of the "API Interface Document": GET /itinerary/plans/{id} */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryPlanDetailDTO {
    private Long id;
    private Long sessionId;
    private Integer version;
    /** generating/generated/failed */
    private String status;
    private BigDecimal totalEstimatedCost;
    /** Primary destination (the first one for multi-destination trips), kept for backward compatibility with the original single-destination response structure */
    private ItineraryDestinationDTO destination;
    /** All destinations, in visiting order; for single-destination trips this list has only one element, same as destination */
    private List<ItineraryDestinationDTO> destinations;
    private List<ItineraryDayDTO> days;
}
