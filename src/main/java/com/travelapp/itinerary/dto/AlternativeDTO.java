package com.travelapp.itinerary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Corresponds to section 5.4 of the "API Interface Document": GET /itinerary/plans/{id}/items/{itemId}/alternatives */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlternativeDTO {
    private Long poiId;
    private String name;
    private BigDecimal estimatedCost;
    private String reason;
}
