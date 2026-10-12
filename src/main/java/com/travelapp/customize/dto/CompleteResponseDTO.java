package com.travelapp.customize.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Corresponds to API Spec section 4.5 POST /customize/sessions/{id}/complete */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteResponseDTO {
    private Long planId;
    /** generating/generated/failed */
    private String status;
}
