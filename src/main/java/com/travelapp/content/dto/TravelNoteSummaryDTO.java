package com.travelapp.content.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class TravelNoteSummaryDTO {
    private Long id;
    private Long destinationId;
    private String title;
    private String coverImage;
    private Integer days;
    private BigDecimal budgetPerPerson;
    private List<String> tags;
    private Long authorId;
    private String authorNickname;
    private String authorAvatarUrl;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer commentCount;
    private LocalDateTime createdAt;
    private boolean liked;
    private boolean favorited;
}
