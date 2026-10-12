package com.travelapp.itinerary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Corresponds to days[] in the response structure of section 5.1 of the "API Interface Document" */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryDayDTO {
    private Integer dayNumber;
    private LocalDate date;
    /** The destination for this day; matches the plan-level destination for single-destination trips, differs per day for multi-destination trips */
    private ItineraryDestinationDTO destination;
    private BigDecimal dailyEstimatedCost;
    private List<ItineraryItemDTO> items;
}
