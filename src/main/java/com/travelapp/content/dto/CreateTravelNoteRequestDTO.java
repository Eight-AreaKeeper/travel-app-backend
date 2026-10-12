package com.travelapp.content.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class CreateTravelNoteRequestDTO {
    private Long destinationId;
    @NotBlank(message = "title must not be empty")
    @Size(max = 100, message = "title must not exceed 100 characters")
    private String title;
    @NotBlank(message = "content must not be empty")
    @Size(max = 10000, message = "content must not exceed 10000 characters")
    private String content;
    @Size(max = 500, message = "cover image URL must not exceed 500 characters")
    private String coverImage;
    @Min(value = 1, message = "days must be greater than 0")
    private Integer days;
    private BigDecimal budgetPerPerson;
    @Size(max = 30, message = "transport type must not exceed 30 characters")
    private String transportType;
    @Size(max = 20, message = "best season must not exceed 20 characters")
    private String bestSeason;
    @Size(max = 9, message = "at most 9 images allowed")
    private List<@Size(max = 500, message = "image URL must not exceed 500 characters") String> images;
    @Size(max = 10, message = "at most 10 tags allowed")
    private List<@Size(max = 30, message = "tag must not exceed 30 characters") String> tags;
    @Size(max = 30, message = "at most 30 days of itinerary details allowed")
    private List<@Valid DayPlanDTO> dayPlans;
}
