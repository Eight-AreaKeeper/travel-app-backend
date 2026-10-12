package com.travelapp.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Corresponds to Database Design Document travel_note_days table -- structured daily itinerary for a travel note, used for the "itinerary overview" on the detail page. */
@Getter
@Setter
@TableName("travel_note_days")
public class TravelNoteDay {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long noteId;
    private Integer dayNumber;
    private String title;
    private String description;
    private BigDecimal cost;
}
