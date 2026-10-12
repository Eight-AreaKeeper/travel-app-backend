package com.travelapp.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCommentRequestDTO {
    private Long parentId;
    @NotBlank(message = "comment content must not be empty")
    @Size(max = 500, message = "comment must not exceed 500 characters")
    private String content;
}
