package com.travelapp.content.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class TravelNoteDetailDTO {
    private Long id;
    private Long destinationId;
    private NoteDestinationDTO destination;
    private String title;
    private String content;
    private String coverImage;
    private Integer days;
    private BigDecimal budgetPerPerson;
    private String transportType;
    private String bestSeason;
    private List<DayPlanDTO> dayPlans;
    private List<String> images;
    private List<String> tags;
    private Long authorId;
    private String authorNickname;
    private String authorAvatarUrl;
    private Integer viewCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean liked;
    private boolean favorited;
    private boolean editable;
}
