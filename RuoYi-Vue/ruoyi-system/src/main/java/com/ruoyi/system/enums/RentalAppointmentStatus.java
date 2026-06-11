package com.ruoyi.system.enums;

import com.ruoyi.common.exception.ServiceException;

/**
 * 看房预约状态。
 */
public enum RentalAppointmentStatus
{
    PENDING("0", "待确认"),
    CONFIRMED("1", "已确认"),
    COMPLETED("2", "已完成"),
    CANCELED("3", "已取消"),
    REJECTED("4", "已拒绝");

    private final String code;
    private final String label;

    RentalAppointmentStatus(String code, String label)
    {
        this.code = code;
        this.label = label;
    }

    public String code()
    {
        return code;
    }

    public String label()
    {
        return label;
    }

    public static RentalAppointmentStatus of(String code)
    {
        for (RentalAppointmentStatus status : values())
        {
            if (status.code.equals(code))
            {
                return status;
            }
        }
        throw new ServiceException("未知预约状态：" + code);
    }

    public boolean canCancel()
    {
        return this == PENDING || this == CONFIRMED;
    }

    public boolean canConfirmOrReject()
    {
        return this == PENDING;
    }

    public boolean canComplete()
    {
        return this == CONFIRMED;
    }
}
