package com.travelapp.customize.entity;
import com.baomidou.mybatisplus.annotation.*; import lombok.Getter; import lombok.Setter;
@Getter @Setter @TableName("qa_rules") public class QaRule { @TableId(type=IdType.AUTO) private Long id; private String questionKey; private String questionText; private String inputType; private String options; private Boolean isRequired; private Integer sortOrder; private Boolean isActive; }
