package com.travelapp.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/** Corresponds to Database Design Document tags table -- tag library. */
@Getter
@Setter
@TableName("tags")
public class Tag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    /** 1=travel style/2=interest preference */
    private Integer category;
}
