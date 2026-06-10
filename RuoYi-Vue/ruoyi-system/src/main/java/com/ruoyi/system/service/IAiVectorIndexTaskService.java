package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AiVectorIndexTask;

/**
 * AI向量索引任务Service接口
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
public interface IAiVectorIndexTaskService 
{
    /**
     * 查询AI向量索引任务
     * 
     * @param taskId AI向量索引任务主键
     * @return AI向量索引任务
     */
    public AiVectorIndexTask selectAiVectorIndexTaskByTaskId(Long taskId);

    /**
     * 查询AI向量索引任务列表
     * 
     * @param aiVectorIndexTask AI向量索引任务
     * @return AI向量索引任务集合
     */
    public List<AiVectorIndexTask> selectAiVectorIndexTaskList(AiVectorIndexTask aiVectorIndexTask);

    /**
     * 新增AI向量索引任务
     * 
     * @param aiVectorIndexTask AI向量索引任务
     * @return 结果
     */
    public int insertAiVectorIndexTask(AiVectorIndexTask aiVectorIndexTask);

    /**
     * 修改AI向量索引任务
     * 
     * @param aiVectorIndexTask AI向量索引任务
     * @return 结果
     */
    public int updateAiVectorIndexTask(AiVectorIndexTask aiVectorIndexTask);

    /**
     * 批量删除AI向量索引任务
     * 
     * @param taskIds 需要删除的AI向量索引任务主键集合
     * @return 结果
     */
    public int deleteAiVectorIndexTaskByTaskIds(Long[] taskIds);

    /**
     * 删除AI向量索引任务信息
     * 
     * @param taskId AI向量索引任务主键
     * @return 结果
     */
    public int deleteAiVectorIndexTaskByTaskId(Long taskId);
}
