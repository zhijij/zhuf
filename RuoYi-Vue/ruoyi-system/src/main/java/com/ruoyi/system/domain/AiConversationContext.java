package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;

public class AiConversationContext extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long contextId;
    private Long conversationId;
    private String bizType;
    private Long bizId;
    private Long houseId;
    private Long contractId;
    private Long appointmentId;
    private Long intentionId;
    private Long entrustId;
    private String snapshotJson;

    public Long getContextId()
    {
        return contextId;
    }

    public void setContextId(Long contextId)
    {
        this.contextId = contextId;
    }

    public Long getConversationId()
    {
        return conversationId;
    }

    public void setConversationId(Long conversationId)
    {
        this.conversationId = conversationId;
    }

    public String getBizType()
    {
        return bizType;
    }

    public void setBizType(String bizType)
    {
        this.bizType = bizType;
    }

    public Long getBizId()
    {
        return bizId;
    }

    public void setBizId(Long bizId)
    {
        this.bizId = bizId;
    }

    public Long getHouseId()
    {
        return houseId;
    }

    public void setHouseId(Long houseId)
    {
        this.houseId = houseId;
    }

    public Long getContractId()
    {
        return contractId;
    }

    public void setContractId(Long contractId)
    {
        this.contractId = contractId;
    }

    public Long getAppointmentId()
    {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId)
    {
        this.appointmentId = appointmentId;
    }

    public Long getIntentionId()
    {
        return intentionId;
    }

    public void setIntentionId(Long intentionId)
    {
        this.intentionId = intentionId;
    }

    public Long getEntrustId()
    {
        return entrustId;
    }

    public void setEntrustId(Long entrustId)
    {
        this.entrustId = entrustId;
    }

    public String getSnapshotJson()
    {
        return snapshotJson;
    }

    public void setSnapshotJson(String snapshotJson)
    {
        this.snapshotJson = snapshotJson;
    }
}
