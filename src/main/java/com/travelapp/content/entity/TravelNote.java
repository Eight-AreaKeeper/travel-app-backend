package com.travelapp.content.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("travel_notes")
public class TravelNote {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("author_id")
    private Long userId;
    private Long destinationId;
    private String title;
    private String content;
    private String coverImage;
    private Integer days;
    private BigDecimal budgetPerPerson;
    private String transportType;
    private String bestSeason;
    private Integer source;
    private LocalDateTime publishedAt;
    /** 0=draft/1=pending review/2=published/3=rejected -- the current version has no review workflow, so a new note is created straight to 2 (published) */
    private Integer status;
    private Integer viewCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer commentCount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
