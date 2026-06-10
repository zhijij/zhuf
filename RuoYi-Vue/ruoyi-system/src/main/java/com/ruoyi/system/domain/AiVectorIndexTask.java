package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * AI向量索引任务对象 ai_vector_index_task
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
public class AiVectorIndexTask extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 任务ID */
    private Long taskId;

    /** 来源类型:house/knowledge/memory */
    @Excel(name = "来源类型:house/knowledge/memory")
    private String sourceType;

    /** 来源ID */
    @Excel(name = "来源ID")
    private Long sourceId;

    /** 动作:upsert/delete */
    @Excel(name = "动作:upsert/delete")
    private String action;

    /** 状态:0待处理,1处理中,2成功,3失败 */
    @Excel(name = "状态:0待处理,1处理中,2成功,3失败")
    private String status;

    /** 重试次数 */
    @Excel(name = "重试次数")
    private Long retryCount;

    /** 错误信息 */
    @Excel(name = "错误信息")
    private String errorMsg;

    public void setTaskId(Long taskId) 
    {
        this.taskId = taskId;
    }

    public Long getTaskId() 
    {
        return taskId;
    }

    public void setSourceType(String sourceType) 
    {
        this.sourceType = sourceType;
    }

    public String getSourceType() 
    {
        return sourceType;
    }

    public void setSourceId(Long sourceId) 
    {
        this.sourceId = sourceId;
    }

    public Long getSourceId() 
    {
        return sourceId;
    }

    public void setAction(String action) 
    {
        this.action = action;
    }

    public String getAction() 
    {
        return action;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setRetryCount(Long retryCount) 
    {
        this.retryCount = retryCount;
    }

    public Long getRetryCount() 
    {
        return retryCount;
    }

    public void setErrorMsg(String errorMsg) 
    {
        this.errorMsg = errorMsg;
    }

    public String getErrorMsg() 
    {
        return errorMsg;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("taskId", getTaskId())
            .append("sourceType", getSourceType())
            .append("sourceId", getSourceId())
            .append("action", getAction())
            .append("status", getStatus())
            .append("retryCount", getRetryCount())
            .append("errorMsg", getErrorMsg())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
