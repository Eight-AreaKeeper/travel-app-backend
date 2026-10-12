package com.travelapp.itinerary.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Corresponds to section 5.3 of the "API Interface Document": PATCH /itinerary/plans/{id}/items/{itemId} */
@Getter
@Setter
public class AdjustItemRequestDTO {
    /** replace / remove / edit */
    private String action;
    private Long poiId;
    private String title;
    private String description;
    private BigDecimal estimatedCost;
}
