package yzh.stock.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("family_media")
public class FamilyMedia {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long familyId;
    private Long photoId;
    private Long sharedBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime shareTime;
}
