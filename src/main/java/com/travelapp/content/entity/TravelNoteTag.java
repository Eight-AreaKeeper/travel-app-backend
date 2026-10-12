package com.travelapp.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/** Corresponds to Database Design Document travel_note_tags table -- many-to-many between travel notes and tags, composite primary key (note_id, tag_id), no auto-increment id. */
@Getter
@Setter
@TableName("travel_note_tags")
public class TravelNoteTag {
    private Long noteId;
    private Long tagId;
}
