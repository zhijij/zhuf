package com.ruoyi.system.domain.dto;

/**
 * 预约处理请求。
 */
public class RentalAppointmentActionRequest
{
    /** 取消或拒绝原因 */
    private String reason;

    public String getReason()
    {
        return reason;
    }

    public void setReason(String reason)
    {
        this.reason = reason;
    }
}
