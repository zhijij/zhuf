package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AiVectorIndexTaskMapper;
import com.ruoyi.system.domain.AiVectorIndexTask;
import com.ruoyi.system.service.IAiVectorIndexTaskService;

/**
 * AI向量索引任务Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
@Service
public class AiVectorIndexTaskServiceImpl implements IAiVectorIndexTaskService 
{
    @Autowired
    private AiVectorIndexTaskMapper aiVectorIndexTaskMapper;

    /**
     * 查询AI向量索引任务
     * 
     * @param taskId AI向量索引任务主键
     * @return AI向量索引任务
     */
    @Override
    public AiVectorIndexTask selectAiVectorIndexTaskByTaskId(Long taskId)
    {
        return aiVectorIndexTaskMapper.selectAiVectorIndexTaskByTaskId(taskId);
    }

    /**
     * 查询AI向量索引任务列表
     * 
     * @param aiVectorIndexTask AI向量索引任务
     * @return AI向量索引任务
     */
    @Override
    public List<AiVectorIndexTask> selectAiVectorIndexTaskList(AiVectorIndexTask aiVectorIndexTask)
    {
        return aiVectorIndexTaskMapper.selectAiVectorIndexTaskList(aiVectorIndexTask);
    }

    /**
     * 新增AI向量索引任务
     * 
     * @param aiVectorIndexTask AI向量索引任务
     * @return 结果
     */
    @Override
    public int insertAiVectorIndexTask(AiVectorIndexTask aiVectorIndexTask)
    {
        aiVectorIndexTask.setCreateTime(DateUtils.getNowDate());
        return aiVectorIndexTaskMapper.insertAiVectorIndexTask(aiVectorIndexTask);
    }

    /**
     * 修改AI向量索引任务
     * 
     * @param aiVectorIndexTask AI向量索引任务
     * @return 结果
     */
    @Override
    public int updateAiVectorIndexTask(AiVectorIndexTask aiVectorIndexTask)
    {
        aiVectorIndexTask.setUpdateTime(DateUtils.getNowDate());
        return aiVectorIndexTaskMapper.updateAiVectorIndexTask(aiVectorIndexTask);
    }

    /**
     * 批量删除AI向量索引任务
     * 
     * @param taskIds 需要删除的AI向量索引任务主键
     * @return 结果
     */
    @Override
    public int deleteAiVectorIndexTaskByTaskIds(Long[] taskIds)
    {
        return aiVectorIndexTaskMapper.deleteAiVectorIndexTaskByTaskIds(taskIds);
    }

    /**
     * 删除AI向量索引任务信息
     * 
     * @param taskId AI向量索引任务主键
     * @return 结果
     */
    @Override
    public int deleteAiVectorIndexTaskByTaskId(Long taskId)
    {
        return aiVectorIndexTaskMapper.deleteAiVectorIndexTaskByTaskId(taskId);
    }
}
