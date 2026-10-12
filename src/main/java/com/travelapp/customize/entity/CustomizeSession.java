package com.travelapp.customize.entity;
import com.baomidou.mybatisplus.annotation.*; import lombok.Getter; import lombok.Setter; import java.time.LocalDateTime;
@Getter @Setter @TableName("customize_sessions") public class CustomizeSession { @TableId(type=IdType.AUTO) private Long id; private Long userId; private Long sourceNoteId; private Integer status; @TableField(fill=FieldFill.INSERT) private LocalDateTime createdAt; @TableField(fill=FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt; private LocalDateTime completedAt; }
