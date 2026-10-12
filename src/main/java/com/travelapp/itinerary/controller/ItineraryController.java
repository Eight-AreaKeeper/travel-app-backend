package com.travelapp.itinerary.controller;

import com.travelapp.common.ApiResponse;
import com.travelapp.itinerary.dto.AdjustItemRequestDTO;
import com.travelapp.itinerary.dto.AdjustItemResponseDTO;
import com.travelapp.itinerary.dto.AlternativeDTO;
import com.travelapp.itinerary.dto.ItineraryPlanDetailDTO;
import com.travelapp.itinerary.dto.ItineraryStatusDTO;
import com.travelapp.itinerary.dto.RegenerateRequestDTO;
import com.travelapp.itinerary.dto.RegenerateResultDTO;
import com.travelapp.itinerary.service.ItineraryService;
import com.travelapp.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Corresponds to section 5 (Itinerary Generation Engine Module) of the "API Interface Document" */
@RestController
@RequestMapping("/api/v1/itinerary/plans")
@RequiredArgsConstructor
@Tag(name = "Itinerary Generation Engine Module")
public class ItineraryController {

    private final ItineraryService itineraryService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/{id}")
    public ApiResponse<ItineraryPlanDetailDTO> getDetail(@PathVariable Long id) {
        return ApiResponse.success(itineraryService.getDetail(currentUserProvider.requireUserId(), id));
    }

    @GetMapping("/{id}/status")
    public ApiResponse<ItineraryStatusDTO> getStatus(@PathVariable Long id) {
        return ApiResponse.success(itineraryService.getStatus(currentUserProvider.requireUserId(), id));
    }

    @PatchMapping("/{id}/items/{itemId}")
    public ApiResponse<AdjustItemResponseDTO> adjustItem(@PathVariable Long id, @PathVariable Long itemId,
            @RequestBody AdjustItemRequestDTO request) {
        return ApiResponse.success(itineraryService.adjustItem(currentUserProvider.requireUserId(), id, itemId, request));
    }

    @GetMapping("/{id}/items/{itemId}/alternatives")
    public ApiResponse<List<AlternativeDTO>> alternatives(@PathVariable Long id, @PathVariable Long itemId) {
        return ApiResponse.success(itineraryService.alternatives(currentUserProvider.requireUserId(), id, itemId));
    }

    @PostMapping("/{id}/regenerate")
    public ApiResponse<RegenerateResultDTO> regenerate(@PathVariable Long id, @RequestBody(required = false) RegenerateRequestDTO request) {
        return ApiResponse.success(itineraryService.regenerate(currentUserProvider.requireUserId(), id, request));
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable Long id) {
        byte[] pdf = itineraryService.exportPdf(currentUserProvider.requireUserId(), id);
        String filename = URLEncoder.encode("itinerary-" + id + ".pdf", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }
}
