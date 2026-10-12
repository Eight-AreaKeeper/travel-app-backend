package com.travelapp.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Corresponds to the destination field in the travel note detail in API Documentation 3.4. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteDestinationDTO {
    private Long id;
    private String name;
}
