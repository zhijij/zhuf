package com.ruoyi.system.enums;

import com.ruoyi.common.exception.ServiceException;

/**
 * 房源审核状态。
 */
public enum RentalAuditStatus
{
    NOT_SUBMITTED("0", "未提交"),
    PENDING("1", "待审"),
    PASSED("2", "通过"),
    REJECTED("3", "拒绝");

    private final String code;
    private final String label;

    RentalAuditStatus(String code, String label)
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

    public static RentalAuditStatus of(String code)
    {
        for (RentalAuditStatus status : values())
        {
            if (status.code.equals(code))
            {
                return status;
            }
        }
        throw new ServiceException("未知审核状态：" + code);
    }

    public boolean isFinalAuditAction()
    {
        return this == PASSED || this == REJECTED;
    }
}
