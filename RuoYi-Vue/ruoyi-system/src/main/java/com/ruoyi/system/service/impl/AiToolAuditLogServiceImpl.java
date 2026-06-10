package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AiToolAuditLogMapper;
import com.ruoyi.system.domain.AiToolAuditLog;
import com.ruoyi.system.service.IAiToolAuditLogService;

/**
 * AI工具调用审计Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
@Service
public class AiToolAuditLogServiceImpl implements IAiToolAuditLogService 
{
    @Autowired
    private AiToolAuditLogMapper aiToolAuditLogMapper;

    /**
     * 查询AI工具调用审计
     * 
     * @param logId AI工具调用审计主键
     * @return AI工具调用审计
     */
    @Override
    public AiToolAuditLog selectAiToolAuditLogByLogId(Long logId)
    {
        return aiToolAuditLogMapper.selectAiToolAuditLogByLogId(logId);
    }

    /**
     * 查询AI工具调用审计列表
     * 
     * @param aiToolAuditLog AI工具调用审计
     * @return AI工具调用审计
     */
    @Override
    public List<AiToolAuditLog> selectAiToolAuditLogList(AiToolAuditLog aiToolAuditLog)
    {
        return aiToolAuditLogMapper.selectAiToolAuditLogList(aiToolAuditLog);
    }

    /**
     * 新增AI工具调用审计
     * 
     * @param aiToolAuditLog AI工具调用审计
     * @return 结果
     */
    @Override
    public int insertAiToolAuditLog(AiToolAuditLog aiToolAuditLog)
    {
        aiToolAuditLog.setCreateTime(DateUtils.getNowDate());
        return aiToolAuditLogMapper.insertAiToolAuditLog(aiToolAuditLog);
    }

    /**
     * 修改AI工具调用审计
     * 
     * @param aiToolAuditLog AI工具调用审计
     * @return 结果
     */
    @Override
    public int updateAiToolAuditLog(AiToolAuditLog aiToolAuditLog)
    {
        return aiToolAuditLogMapper.updateAiToolAuditLog(aiToolAuditLog);
    }

    /**
     * 批量删除AI工具调用审计
     * 
     * @param logIds 需要删除的AI工具调用审计主键
     * @return 结果
     */
    @Override
    public int deleteAiToolAuditLogByLogIds(Long[] logIds)
    {
        return aiToolAuditLogMapper.deleteAiToolAuditLogByLogIds(logIds);
    }

    /**
     * 删除AI工具调用审计信息
     * 
     * @param logId AI工具调用审计主键
     * @return 结果
     */
    @Override
    public int deleteAiToolAuditLogByLogId(Long logId)
    {
        return aiToolAuditLogMapper.deleteAiToolAuditLogByLogId(logId);
    }
}
