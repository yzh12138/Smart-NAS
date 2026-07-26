package yzh.stock.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tag")
public class Tag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tagName;
    private String tagColor;
    private String tagCategory;
    private String tagParent;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
