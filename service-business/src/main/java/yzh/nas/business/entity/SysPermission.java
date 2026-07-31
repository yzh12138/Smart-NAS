package yzh.nas.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("sys_permission")
public class SysPermission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private String permName;
    private String permKey;
    private Integer permType;  // 1=目录 2=菜单 3=按钮
    private String path;
    private String component;
    private String icon;
    private Integer sortOrder;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private List<SysPermission> children;
}
