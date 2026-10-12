package com.travelapp.content.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CommentDTO {
    private Long id;
    private Long parentId;
    private String content;
    private Long authorId;
    private String authorNickname;
    private String authorAvatarUrl;
    private LocalDateTime createdAt;
    private boolean editable;
}
