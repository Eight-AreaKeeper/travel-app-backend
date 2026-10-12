package com.travelapp.content.service;

import com.travelapp.common.PageResult;
import com.travelapp.content.dto.CommentDTO;
import com.travelapp.content.dto.CreateCommentRequestDTO;
import com.travelapp.content.dto.CreateTravelNoteRequestDTO;
import com.travelapp.content.dto.TravelNoteDetailDTO;
import com.travelapp.content.dto.TravelNoteSummaryDTO;
import com.travelapp.content.dto.UpdateTravelNoteRequestDTO;

import java.math.BigDecimal;

public interface TravelNoteService {
    /**
     * @param sort recommend (default, by engagement)/latest (most recently published)/budget_asc (lowest to highest budget per person), corresponds to API Documentation 3.3
     */
    PageResult<TravelNoteSummaryDTO> list(Long destinationId, String keyword, Integer daysMin, Integer daysMax,
            BigDecimal budgetMin, BigDecimal budgetMax, String sort, long page, long pageSize, Long viewerId);
    TravelNoteDetailDTO getById(Long noteId, Long viewerId);
    TravelNoteDetailDTO create(Long userId, CreateTravelNoteRequestDTO request);
    TravelNoteDetailDTO update(Long userId, Long noteId, UpdateTravelNoteRequestDTO request);
    void delete(Long userId, Long noteId);
    PageResult<CommentDTO> listComments(Long noteId, long page, long pageSize, Long viewerId);
    CommentDTO createComment(Long userId, Long noteId, CreateCommentRequestDTO request);
    void deleteComment(Long userId, Long noteId, Long commentId);
    void like(Long userId, Long noteId);
    void unlike(Long userId, Long noteId);
    void favorite(Long userId, Long noteId);
    void unfavorite(Long userId, Long noteId);
    PageResult<TravelNoteSummaryDTO> listFavorites(Long userId, long page, long pageSize);
}
