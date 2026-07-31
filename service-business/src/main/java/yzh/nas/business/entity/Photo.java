package yzh.nas.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("photo")
public class Photo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String originalName;
    private String storagePath;
    private String thumbnailPath;
    private Long fileSize;
    private String fileHash;
    private String mimeType;
    private Integer width;
    private Integer height;
    private BigDecimal gpsLat;
    private BigDecimal gpsLng;
    private String city;
    private String province;
    private LocalDateTime shootTime;
    private Integer aiAnalyzed;
    private Integer duration;
    private String mediaType;
    private Integer isDeleted;
    private LocalDateTime deletedTime;
    private Integer recycleDays;
    private Integer clickCount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
