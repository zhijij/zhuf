package com.ruoyi.system.domain.dto;

/**
 * 房源取消/下架请求。
 */
public class RentalHouseCancelRequest
{
    /** 取消或下架原因 */
    private String cancelReason;

    public String getCancelReason()
    {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason)
    {
        this.cancelReason = cancelReason;
    }
}
