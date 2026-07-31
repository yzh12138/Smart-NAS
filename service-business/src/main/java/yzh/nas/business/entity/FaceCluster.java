package yzh.nas.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("face_cluster")
public class FaceCluster {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String clusterName;
    private Integer photoCount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
