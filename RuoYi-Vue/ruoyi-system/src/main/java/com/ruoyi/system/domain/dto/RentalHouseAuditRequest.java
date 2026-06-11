package com.ruoyi.system.domain.dto;

/**
 * 房源审核请求。
 */
public class RentalHouseAuditRequest
{
    /** 审核状态：2通过，3拒绝 */
    private String auditStatus;

    /** 审核意见 */
    private String auditReason;

    public String getAuditStatus()
    {
        return auditStatus;
    }

    public void setAuditStatus(String auditStatus)
    {
        this.auditStatus = auditStatus;
    }

    public String getAuditReason()
    {
        return auditReason;
    }

    public void setAuditReason(String auditReason)
    {
        this.auditReason = auditReason;
    }
}
