package com.ruoyi.system.enums;

import com.ruoyi.common.exception.ServiceException;

/**
 * 租赁合同状态。
 */
public enum RentalContractStatus
{
    DRAFT("0", "草稿"),
    WAIT_SIGN("1", "待确认/待签"),
    ACTIVE("2", "生效"),
    TERMINATED("3", "终止"),
    VOIDED("4", "作废");

    private final String code;
    private final String label;

    RentalContractStatus(String code, String label)
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

    public static RentalContractStatus of(String code)
    {
        for (RentalContractStatus status : values())
        {
            if (status.code.equals(code))
            {
                return status;
            }
        }
        throw new ServiceException("未知合同状态：" + code);
    }
}
