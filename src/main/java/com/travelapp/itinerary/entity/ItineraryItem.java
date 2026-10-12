package com.travelapp.itinerary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

/** Corresponds to the itinerary_items table in the "Database Design Document" -- daily itinerary items, the most granular data. */
@Getter
@Setter
@TableName("itinerary_items")
public class ItineraryItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long dayId;
    private Integer sortOrder;
    private LocalTime startTime;
    /** 1=scenic spot / 2=dining / 3=transport / 4=accommodation */
    private Integer category;
    private Long poiId;
    private String title;
    private String description;
    private BigDecimal estimatedCost;
    private Boolean isUserEdited;
}
