package com.travelapp.itinerary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Corresponds to section 5.5 of the "API Interface Document": POST /itinerary/plans/{id}/regenerate */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegenerateResultDTO {
    private Long planId;
    private Integer version;
    /** generating/generated/failed */
    private String status;
}
