package yzh.stock.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_message")
public class AiMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long conversationId;
    private String role;
    private String content;
    private String imagePath;
    private Integer tokens;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
