package yzh.stock.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import yzh.stock.business.entity.SysPermission;
import yzh.stock.business.mapper.SysPermissionMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysPermissionService {

    private final SysPermissionMapper permissionMapper;

    public SysPermissionService(SysPermissionMapper permissionMapper) {
        this.permissionMapper = permissionMapper;
    }

    public List<SysPermission> getPermissionTree() {
        List<SysPermission> all = permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>().orderByAsc(SysPermission::getSortOrder)
        );
        return buildTree(all, 0L);
    }

    public List<SysPermission> getPermissionsByUserId(Long userId) {
        List<SysPermission> perms = permissionMapper.selectPermissionsByUserId(userId);
        return buildTree(perms, 0L);
    }

    public List<SysPermission> listAll() {
        return permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>().orderByAsc(SysPermission::getSortOrder)
        );
    }

    public void createPermission(SysPermission perm) {
        perm.setStatus(1);
        permissionMapper.insert(perm);
    }

    public void updatePermission(SysPermission perm) {
        permissionMapper.updateById(perm);
    }

    public void deletePermission(Long id) {
        permissionMapper.deleteById(id);
    }

    private List<SysPermission> buildTree(List<SysPermission> all, Long parentId) {
        return all.stream()
                .filter(p -> parentId.equals(p.getParentId()))
                .peek(p -> p.setChildren(buildTree(all, p.getId())))
                .collect(Collectors.toList());
    }
}
