package com.travelapp.content.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Corresponds to Database Design Document Section 3, destinations table
 */
@Getter
@Setter
@TableName("destinations")
public class Destination {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String namePinyin;

    /** 1=country/2=province/3=city/4=scenic area */
    private Integer type;

    private Long parentId;

    private String coverImage;

    private String description;

    private Integer popularityScore = 0;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
