package yzh.stock.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import yzh.stock.business.entity.AiUserPrompt;
import yzh.stock.business.mapper.AiUserPromptMapper;

import java.util.List;

@Service
public class AiUserPromptService {

    private final AiUserPromptMapper promptMapper;

    public AiUserPromptService(AiUserPromptMapper promptMapper) {
        this.promptMapper = promptMapper;
    }

    public List<AiUserPrompt> listPrompts(Long userId) {
        return promptMapper.selectList(
                new LambdaQueryWrapper<AiUserPrompt>()
                        .eq(AiUserPrompt::getUserId, userId)
                        .orderByDesc(AiUserPrompt::getIsDefault)
                        .orderByDesc(AiUserPrompt::getUpdateTime)
        );
    }

    public AiUserPrompt createPrompt(Long userId, String name, String content) {
        AiUserPrompt prompt = new AiUserPrompt();
        prompt.setUserId(userId);
        prompt.setName(name);
        prompt.setContent(content);
        prompt.setIsDefault(0);
        promptMapper.insert(prompt);
        return prompt;
    }

    public AiUserPrompt updatePrompt(Long id, Long userId, String name, String content) {
        AiUserPrompt prompt = promptMapper.selectOne(
                new LambdaQueryWrapper<AiUserPrompt>()
                        .eq(AiUserPrompt::getId, id)
                        .eq(AiUserPrompt::getUserId, userId)
        );
        if (prompt == null) return null;
        if (name != null) prompt.setName(name);
        if (content != null) prompt.setContent(content);
        promptMapper.updateById(prompt);
        return prompt;
    }

    public void deletePrompt(Long id, Long userId) {
        promptMapper.delete(
                new LambdaQueryWrapper<AiUserPrompt>()
                        .eq(AiUserPrompt::getId, id)
                        .eq(AiUserPrompt::getUserId, userId)
        );
    }

    public void setDefault(Long id, Long userId) {
        // 取消所有默认
        List<AiUserPrompt> prompts = listPrompts(userId);
        for (AiUserPrompt p : prompts) {
            if (p.getIsDefault() == 1) {
                p.setIsDefault(0);
                promptMapper.updateById(p);
            }
        }
        // 设置新的默认
        AiUserPrompt prompt = promptMapper.selectOne(
                new LambdaQueryWrapper<AiUserPrompt>()
                        .eq(AiUserPrompt::getId, id)
                        .eq(AiUserPrompt::getUserId, userId)
        );
        if (prompt != null) {
            prompt.setIsDefault(1);
            promptMapper.updateById(prompt);
        }
    }

    public AiUserPrompt getDefault(Long userId) {
        return promptMapper.selectOne(
                new LambdaQueryWrapper<AiUserPrompt>()
                        .eq(AiUserPrompt::getUserId, userId)
                        .eq(AiUserPrompt::getIsDefault, 1)
        );
    }
}
