package com.travelapp.itinerary.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Corresponds to the itinerary_plans table in the "Database Design Document" -- generated itinerary plans, supports multiple versions.
 */
@Getter
@Setter
@TableName("itinerary_plans")
public class ItineraryPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long userId;
    /** A new version is created on each "partial adjustment" / regeneration */
    private Integer version;
    /** 0=generating / 1=generated / 2=user-edited / 3=confirmed / 4=generation failed */
    private Integer status;
    /** 0=pending generation (placeholder while generating) / 1=rule engine / 2=AI-generated / 3=human advisor */
    private Integer generatedBy;
    private BigDecimal totalEstimatedCost;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
