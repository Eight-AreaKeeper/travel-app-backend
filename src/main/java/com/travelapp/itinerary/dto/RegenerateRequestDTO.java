package com.travelapp.itinerary.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Corresponds to section 5.5 of the "API Interface Document": POST /itinerary/plans/{id}/regenerate -- all fields are optional, only the fields provided get updated */
@Getter
@Setter
public class RegenerateRequestDTO {
    private Integer days;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private Integer travelerCount;
    private String travelStyle;
}
