package com.travelapp.content.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Structured daily itinerary for a travel note; shared by the request body (submitted when publishing/updating
 * a note) and the response body (dayPlans in the 3.4 detail view).
 * Corresponds to API Documentation 3.5 dayPlans field / 3.4 response structure dayPlans[].
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DayPlanDTO {
    @Min(value = 1, message = "dayNumber must be greater than 0")
    private Integer dayNumber;
    @Size(max = 100, message = "title must not exceed 100 characters")
    private String title;
    @Size(max = 2000, message = "description must not exceed 2000 characters")
    private String description;
    private BigDecimal cost;
}
