package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.AiToolAuditLog;

/**
 * AI工具调用审计Mapper接口
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
public interface AiToolAuditLogMapper 
{
    /**
     * 查询AI工具调用审计
     * 
     * @param logId AI工具调用审计主键
     * @return AI工具调用审计
     */
    public AiToolAuditLog selectAiToolAuditLogByLogId(Long logId);

    /**
     * 查询AI工具调用审计列表
     * 
     * @param aiToolAuditLog AI工具调用审计
     * @return AI工具调用审计集合
     */
    public List<AiToolAuditLog> selectAiToolAuditLogList(AiToolAuditLog aiToolAuditLog);

    /**
     * 新增AI工具调用审计
     * 
     * @param aiToolAuditLog AI工具调用审计
     * @return 结果
     */
    public int insertAiToolAuditLog(AiToolAuditLog aiToolAuditLog);

    /**
     * 修改AI工具调用审计
     * 
     * @param aiToolAuditLog AI工具调用审计
     * @return 结果
     */
    public int updateAiToolAuditLog(AiToolAuditLog aiToolAuditLog);

    /**
     * 删除AI工具调用审计
     * 
     * @param logId AI工具调用审计主键
     * @return 结果
     */
    public int deleteAiToolAuditLogByLogId(Long logId);

    /**
     * 批量删除AI工具调用审计
     * 
     * @param logIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAiToolAuditLogByLogIds(Long[] logIds);
}
