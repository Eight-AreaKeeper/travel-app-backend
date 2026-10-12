package com.travelapp.customize.entity;
import com.baomidou.mybatisplus.annotation.*; import lombok.Getter; import lombok.Setter;
@Getter @Setter @TableName("customize_destinations") public class CustomizeDestination { @TableId(type=IdType.AUTO) private Long id; private Long sessionId; /** Only set when matched against the local destinations table; not required */ private Long destinationId; /** Destination name (free text) */ private String destinationName; private Integer sortOrder; }
