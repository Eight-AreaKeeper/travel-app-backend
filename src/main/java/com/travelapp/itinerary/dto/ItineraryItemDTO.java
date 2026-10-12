package com.travelapp.itinerary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Corresponds to days[].items[] in the response structure of section 5.1 of the "API Interface Document" */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryItemDTO {
    private Long id;
    private Integer sortOrder;
    private String startTime;
    /** scenic/food/transport/hotel */
    private String category;
    private String title;
    private String description;
    private BigDecimal estimatedCost;
    private Long poiId;
    private boolean userEdited;
}
