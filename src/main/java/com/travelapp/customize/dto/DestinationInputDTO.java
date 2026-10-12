package com.travelapp.customize.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Destination selected/entered by the user in the customize Q&A flow. id is optional (only
 * present when the frontend matches a result from the local destinations table search,
 * purely for display enhancement); name is required—itinerary generation does not require
 * id to correspond to a record in the local database, free-text destinations are supported.
 */
@Getter
@Setter
public class DestinationInputDTO {
    private Long id;
    @NotBlank(message = "Destination name must not be blank")
    @Size(max = 100, message = "Destination name must not exceed 100 characters")
    private String name;
}
