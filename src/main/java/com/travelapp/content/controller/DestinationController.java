package com.travelapp.content.controller;

import com.travelapp.common.ApiResponse;
import com.travelapp.content.dto.DestinationSummaryDTO;
import com.travelapp.content.service.DestinationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Corresponds to API Documentation Section 3, Content Module (Travel Notes & Guides) 3.1/3.2
 */
@RestController
@RequestMapping("/api/v1/destinations")
@RequiredArgsConstructor
@Validated
@Tag(name = "Content Module - Destination")
public class DestinationController {

    private final DestinationService destinationService;

    @GetMapping("/search")
    public ApiResponse<List<DestinationSummaryDTO>> search(
            @RequestParam @NotBlank(message = "keyword must not be empty") String keyword,
            @RequestParam(defaultValue = "10") @Max(value = 20, message = "limit must not exceed 20") int limit) {
        return ApiResponse.success(destinationService.search(keyword, limit));
    }

    @GetMapping("/hot")
    public ApiResponse<List<DestinationSummaryDTO>> hot(
            @RequestParam(defaultValue = "10") @Max(value = 20, message = "limit must not exceed 20") int limit) {
        return ApiResponse.success(destinationService.hot(limit));
    }
}
