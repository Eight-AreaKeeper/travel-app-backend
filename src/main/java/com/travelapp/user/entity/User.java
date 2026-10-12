package com.travelapp.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Corresponds to the users table in Section 3 of the "Database Design Document"
 */
@Getter
@Setter
@TableName("users")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String nickname;

    private String avatarUrl;

    private String phone;

    private String email;

    private String passwordHash;

    /** 0 unknown / 1 male / 2 female */
    private Integer gender;

    private LocalDate birthday;

    /** 0 normal / 1 banned */
    private Integer status = 0;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
