package com.travelapp.itinerary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Corresponds to the response structure in section 5.3 of the "API Interface Document"; item is null when action=remove */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustItemResponseDTO {
    private ItineraryItemDTO item;
}
