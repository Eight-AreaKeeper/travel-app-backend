package com.travelapp.content.controller;

import com.travelapp.common.ApiResponse;
import com.travelapp.common.PageResult;
import com.travelapp.content.dto.CommentDTO;
import com.travelapp.content.dto.CreateCommentRequestDTO;
import com.travelapp.content.dto.CreateTravelNoteRequestDTO;
import com.travelapp.content.dto.TravelNoteDetailDTO;
import com.travelapp.content.dto.TravelNoteSummaryDTO;
import com.travelapp.content.dto.UpdateTravelNoteRequestDTO;
import com.travelapp.content.service.TravelNoteService;
import com.travelapp.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/** Content Module 3.3-3.9: travel notes, comments, likes and favorites. */
@RestController
@RequestMapping("/api/v1/notes")
@RequiredArgsConstructor
@Validated
@Tag(name = "Content Module - Travel Notes, Comments & Interactions")
public class TravelNoteController {
    private final TravelNoteService travelNoteService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public ApiResponse<PageResult<TravelNoteSummaryDTO>> list(
            @RequestParam(required = false) Long destinationId, @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer daysMin, @RequestParam(required = false) Integer daysMax,
            @RequestParam(required = false) BigDecimal budgetMin, @RequestParam(required = false) BigDecimal budgetMax,
            @RequestParam(required = false, defaultValue = "recommend") String sort,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "page must be greater than 0") long page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "pageSize must be greater than 0") @Max(value = 50, message = "pageSize must not exceed 50") long pageSize) {
        return ApiResponse.success(travelNoteService.list(destinationId, keyword, daysMin, daysMax, budgetMin, budgetMax,
                sort, page, pageSize, currentUserProvider.getUserIdOrNull()));
    }

    @GetMapping("/{noteId}")
    public ApiResponse<TravelNoteDetailDTO> getById(@PathVariable Long noteId) {
        return ApiResponse.success(travelNoteService.getById(noteId, currentUserProvider.getUserIdOrNull()));
    }

    @PostMapping
    public ApiResponse<TravelNoteDetailDTO> create(@Valid @RequestBody CreateTravelNoteRequestDTO request) {
        return ApiResponse.success(travelNoteService.create(currentUserProvider.requireUserId(), request));
    }

    @PutMapping("/{noteId}")
    public ApiResponse<TravelNoteDetailDTO> update(@PathVariable Long noteId, @Valid @RequestBody UpdateTravelNoteRequestDTO request) {
        return ApiResponse.success(travelNoteService.update(currentUserProvider.requireUserId(), noteId, request));
    }

    @DeleteMapping("/{noteId}")
    public ApiResponse<Void> delete(@PathVariable Long noteId) {
        travelNoteService.delete(currentUserProvider.requireUserId(), noteId);
        return ApiResponse.success();
    }

    @GetMapping("/{noteId}/comments")
    public ApiResponse<PageResult<CommentDTO>> listComments(@PathVariable Long noteId,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "page must be greater than 0") long page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "pageSize must be greater than 0") @Max(value = 50, message = "pageSize must not exceed 50") long pageSize) {
        return ApiResponse.success(travelNoteService.listComments(noteId, page, pageSize, currentUserProvider.getUserIdOrNull()));
    }

    @PostMapping("/{noteId}/comments")
    public ApiResponse<CommentDTO> createComment(@PathVariable Long noteId, @Valid @RequestBody CreateCommentRequestDTO request) {
        return ApiResponse.success(travelNoteService.createComment(currentUserProvider.requireUserId(), noteId, request));
    }

    @DeleteMapping("/{noteId}/comments/{commentId}")
    public ApiResponse<Void> deleteComment(@PathVariable Long noteId, @PathVariable Long commentId) {
        travelNoteService.deleteComment(currentUserProvider.requireUserId(), noteId, commentId);
        return ApiResponse.success();
    }

    @PostMapping("/{noteId}/like")
    public ApiResponse<Void> like(@PathVariable Long noteId) {
        travelNoteService.like(currentUserProvider.requireUserId(), noteId);
        return ApiResponse.success();
    }

    @DeleteMapping("/{noteId}/like")
    public ApiResponse<Void> unlike(@PathVariable Long noteId) {
        travelNoteService.unlike(currentUserProvider.requireUserId(), noteId);
        return ApiResponse.success();
    }

    @PostMapping("/{noteId}/favorite")
    public ApiResponse<Void> favorite(@PathVariable Long noteId) {
        travelNoteService.favorite(currentUserProvider.requireUserId(), noteId);
        return ApiResponse.success();
    }

    @DeleteMapping("/{noteId}/favorite")
    public ApiResponse<Void> unfavorite(@PathVariable Long noteId) {
        travelNoteService.unfavorite(currentUserProvider.requireUserId(), noteId);
        return ApiResponse.success();
    }
}
