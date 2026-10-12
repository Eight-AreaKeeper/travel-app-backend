package com.travelapp.content.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("likes")
public class NoteInteraction {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("target_id")
    private Long noteId;
    private Long userId;
    @TableField("target_type")
    private Integer targetType;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
