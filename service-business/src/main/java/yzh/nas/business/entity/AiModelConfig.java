package yzh.nas.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_model_config")
public class AiModelConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String modelName;
    private String modelType;
    private String modelId;
    private String apiUrl;
    private String apiKey;
    private String promptTemplate;
    private Integer isDefault;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
