package yzh.stock.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import yzh.stock.business.entity.SysRole;
import yzh.stock.business.mapper.SysRoleMapper;

import java.util.List;

@Service
public class SysRoleService {

    private final SysRoleMapper roleMapper;

    public SysRoleService(SysRoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    public List<SysRole> listAll() {
        return roleMapper.selectList(
                new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getSortOrder)
        );
    }

    public List<SysRole> getRolesByUserId(Long userId) {
        return roleMapper.selectRolesByUserId(userId);
    }

    public void createRole(SysRole role) {
        role.setStatus(1);
        roleMapper.insert(role);
    }

    public void updateRole(SysRole role) {
        roleMapper.updateById(role);
    }

    public void deleteRole(Long id) {
        roleMapper.deleteById(id);
    }
}
