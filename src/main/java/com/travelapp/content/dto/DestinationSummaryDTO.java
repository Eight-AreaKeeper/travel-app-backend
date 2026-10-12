package com.travelapp.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Corresponds to the response structure in API Documentation 3.1/3.2
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DestinationSummaryDTO {
    private Long id;
    private String name;
    private Integer type;
    private String coverImage;
}
