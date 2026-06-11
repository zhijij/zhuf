package com.ruoyi.system.enums;

import com.ruoyi.common.exception.ServiceException;

/**
 * 租赁意向状态。
 */
public enum RentalIntentionStatus
{
    FOLLOWING("0", "跟进中"),
    DEAL("1", "已成交"),
    INVALID("2", "无效"),
    ABANDONED("3", "放弃");

    private final String code;
    private final String label;

    RentalIntentionStatus(String code, String label)
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

    public static RentalIntentionStatus of(String code)
    {
        for (RentalIntentionStatus status : values())
        {
            if (status.code.equals(code))
            {
                return status;
            }
        }
        throw new ServiceException("未知意向状态：" + code);
    }

    public boolean canClose()
    {
        return this == FOLLOWING;
    }
}
