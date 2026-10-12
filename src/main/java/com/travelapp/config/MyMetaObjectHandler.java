package com.travelapp.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Automatically fills created_at / updated_at, replacing the effect of @PrePersist/@PreUpdate in JPA.
 * Entity fields must be annotated with @TableField(fill = FieldFill.INSERT or INSERT_UPDATE) to be
 * handled here; entities without the corresponding field (e.g. Destination has no updatedAt) are
 * automatically skipped by MyBatis-Plus without errors.
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
