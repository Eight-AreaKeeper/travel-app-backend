package com.travelapp.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/** Corresponds to Database Design Document travel_note_images table -- images embedded in a travel note's body. */
@Getter
@Setter
@TableName("travel_note_images")
public class TravelNoteImage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long noteId;
    private String imageUrl;
    private Integer sortOrder;
    private String caption;
}
