package com.travelapp.itinerary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Corresponds to the itinerary_days table in the "Database Design Document" -- plan-to-day mapping. */
@Getter
@Setter
@TableName("itinerary_days")
public class ItineraryDay {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    private Integer dayNumber;
    private LocalDate date;
    /** Only populated when matched against the local destinations table; not required */
    private Long destinationId;
    /** Which destination this day belongs to for multi-destination trips (free text); fixed to the plan's single destination for single-destination trips */
    private String destinationName;
    private BigDecimal dailyEstimatedCost;
}
