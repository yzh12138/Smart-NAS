package yzh.nas.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("book")
public class Book {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String author;
    private String isbn;
    private String category;
    private String tags;
    private String fileName;
    private String storagePath;
    private Long fileSize;
    private String fileFormat;
    private String visibility;
    private String coverPath;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
