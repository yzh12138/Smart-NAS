package yzh.stock.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import yzh.stock.business.entity.OperationLog;
import yzh.stock.business.mapper.OperationLogMapper;

@Service
public class LogService {

    private final OperationLogMapper logMapper;

    public LogService(OperationLogMapper logMapper) {
        this.logMapper = logMapper;
    }

    public void log(Long userId, String username, String action, String targetType, Long targetId, String detail, String ip) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        log.setIpAddress(ip);
        logMapper.insert(log);
    }

    public Page<OperationLog> listLogs(int page, int size, Long userId, String action, String startTime, String endTime) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) wrapper.eq(OperationLog::getUserId, userId);
        if (action != null && !action.isEmpty()) wrapper.like(OperationLog::getAction, action);
        if (startTime != null) wrapper.ge(OperationLog::getCreateTime, startTime);
        if (endTime != null) wrapper.le(OperationLog::getCreateTime, endTime);
        wrapper.orderByDesc(OperationLog::getCreateTime);
        return logMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
