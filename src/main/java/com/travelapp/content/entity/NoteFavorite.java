package com.travelapp.content.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Getter @Setter @TableName("favorites")
public class NoteFavorite { @TableId(type = IdType.AUTO) private Long id; private Long userId; private Long noteId; @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt; }
