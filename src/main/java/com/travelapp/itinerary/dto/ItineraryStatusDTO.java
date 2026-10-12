package com.travelapp.itinerary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Corresponds to section 5.2 of the "API Interface Document": GET /itinerary/plans/{id}/status */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryStatusDTO {
    /** generating/generated/failed */
    private String status;
    private int progress;
}
