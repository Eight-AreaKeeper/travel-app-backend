package com.travelapp.user.controller;

import com.travelapp.common.ApiResponse;
import com.travelapp.common.PageResult;
import com.travelapp.content.dto.TravelNoteSummaryDTO;
import com.travelapp.content.service.TravelNoteService;
import com.travelapp.itinerary.dto.ItineraryPlanSummaryDTO;
import com.travelapp.itinerary.service.ItineraryService;
import com.travelapp.security.CurrentUserProvider;
import com.travelapp.user.dto.UpdateProfileRequestDTO;
import com.travelapp.user.dto.UserProfileDTO;
import com.travelapp.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Corresponds to the User Module (Section 2) in the "API Specification".
 * This is the "reference module" for this project — the other modules (content/customize/itinerary)
 * should be built using the same four-layer structure (controller/service/repository/dto);
 * the entity can be placed directly under the module package without a separate entity
 * subpackage (split it out later if the data volume grows).
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "User Module")
public class UserController {

    private final UserService userService;
    private final TravelNoteService travelNoteService;
    private final ItineraryService itineraryService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/me")
    public ApiResponse<UserProfileDTO> getCurrentUser() {
        Long currentUserId = currentUserProvider.requireUserId();
        return ApiResponse.success(userService.getProfile(currentUserId));
    }

    @PatchMapping("/me")
    public ApiResponse<UserProfileDTO> updateCurrentUser(@Valid @RequestBody UpdateProfileRequestDTO request) {
        Long currentUserId = currentUserProvider.requireUserId();
        return ApiResponse.success(userService.updateProfile(currentUserId, request));
    }

    @GetMapping("/me/favorites")
    public ApiResponse<PageResult<TravelNoteSummaryDTO>> listFavorites(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "page must be greater than 0") long page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "pageSize must be greater than 0") @Max(value = 50, message = "pageSize cannot exceed 50") long pageSize) {
        Long currentUserId = currentUserProvider.requireUserId();
        return ApiResponse.success(travelNoteService.listFavorites(currentUserId, page, pageSize));
    }

    @GetMapping("/me/itinerary-plans")
    public ApiResponse<PageResult<ItineraryPlanSummaryDTO>> listMyItineraryPlans(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "page must be greater than 0") long page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "pageSize must be greater than 0") @Max(value = 50, message = "pageSize cannot exceed 50") long pageSize) {
        Long currentUserId = currentUserProvider.requireUserId();
        return ApiResponse.success(itineraryService.listMyPlans(currentUserId, page, pageSize));
    }
}
