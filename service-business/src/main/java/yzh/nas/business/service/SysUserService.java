package yzh.nas.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import yzh.nas.business.entity.SysUser;
import yzh.nas.business.mapper.SysUserMapper;

@Service
public class SysUserService {

    private final SysUserMapper userMapper;

    public SysUserService(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public SysUser findByUsername(String username) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
    }

    public SysUser getById(Long id) {
        return userMapper.selectById(id);
    }

    public Page<SysUser> listUsers(int page, int size) {
        return userMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<SysUser>().orderByDesc(SysUser::getCreateTime)
        );
    }

    public void createUser(SysUser user) {
        if (user.getStatus() == null) user.setStatus(1);
        userMapper.insert(user);
    }

    public void updateUser(SysUser user) {
        if (user.getPassword() == null) {
            // 密码为空时不更新密码字段
            user.setPassword(null);
            // 使用 MyBatis-Plus 的 update 方法，忽略 null 字段
            com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SysUser> wrapper =
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
            wrapper.eq(SysUser::getId, user.getId());
            if (user.getNickname() != null) wrapper.set(SysUser::getNickname, user.getNickname());
            if (user.getAvatar() != null) wrapper.set(SysUser::getAvatar, user.getAvatar());
            if (user.getAiPrompt() != null) wrapper.set(SysUser::getAiPrompt, user.getAiPrompt());
            if (user.getFamilyRole() != null) wrapper.set(SysUser::getFamilyRole, user.getFamilyRole());
            if (user.getStatus() != null) wrapper.set(SysUser::getStatus, user.getStatus());
            userMapper.update(null, wrapper);
        } else {
            userMapper.updateById(user);
        }
    }

    public void deleteUser(Long id) {
        userMapper.deleteById(id);
    }
}
