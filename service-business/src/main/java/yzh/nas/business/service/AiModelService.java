package yzh.nas.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import yzh.nas.business.entity.AiModelConfig;
import yzh.nas.business.mapper.AiModelConfigMapper;

import java.util.List;

@Service
public class AiModelService {

    private final AiModelConfigMapper configMapper;

    public AiModelService(AiModelConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    public List<AiModelConfig> listAll() {
        return configMapper.selectList(
                new LambdaQueryWrapper<AiModelConfig>().orderByDesc(AiModelConfig::getIsDefault)
        );
    }

    public AiModelConfig getDefault() {
        return configMapper.selectOne(
                new LambdaQueryWrapper<AiModelConfig>().eq(AiModelConfig::getIsDefault, 1)
        );
    }

    public AiModelConfig getById(Long id) {
        return configMapper.selectById(id);
    }

    public void create(AiModelConfig config) {
        config.setStatus(1);
        configMapper.insert(config);
    }

    public void update(AiModelConfig config) {
        configMapper.updateById(config);
    }

    public void delete(Long id) {
        configMapper.deleteById(id);
    }

    public void setDefault(Long id) {
        // 取消其他默认
        List<AiModelConfig> all = configMapper.selectList(null);
        for (AiModelConfig c : all) {
            if (c.getIsDefault() == 1) {
                c.setIsDefault(0);
                configMapper.updateById(c);
            }
        }
        // 设置新的默认
        AiModelConfig config = configMapper.selectById(id);
        if (config != null) {
            config.setIsDefault(1);
            configMapper.updateById(config);
        }
    }
}
