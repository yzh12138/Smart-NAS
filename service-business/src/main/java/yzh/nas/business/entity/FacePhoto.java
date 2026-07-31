package yzh.nas.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("face_photo")
public class FacePhoto {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long clusterId;
    private Long photoId;
    private String faceBbox;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
