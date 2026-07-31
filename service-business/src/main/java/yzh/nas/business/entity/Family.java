package yzh.nas.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("family")
public class Family {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String familyName;
    private Long ownerId;
    private String description;
    private String familyCode;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
