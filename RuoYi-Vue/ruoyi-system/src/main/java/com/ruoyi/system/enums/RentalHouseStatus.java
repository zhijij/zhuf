package com.ruoyi.system.enums;

import com.ruoyi.common.exception.ServiceException;

/**
 * 房源业务状态。
 */
public enum RentalHouseStatus
{
    DRAFT("0", "草稿"),
    PENDING_AUDIT("1", "待审核"),
    PUBLISHED("2", "已发布"),
    REJECTED("3", "审核未通过"),
    RENTED("4", "已出租"),
    OFF_SHELF("5", "已下架");

    private final String code;
    private final String label;

    RentalHouseStatus(String code, String label)
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

    public static RentalHouseStatus of(String code)
    {
        for (RentalHouseStatus status : values())
        {
            if (status.code.equals(code))
            {
                return status;
            }
        }
        throw new ServiceException("未知房源状态：" + code);
    }

    public static boolean isPublicVisible(String code)
    {
        return PUBLISHED.code.equals(code);
    }

    public boolean canSubmitAudit()
    {
        return this == DRAFT || this == REJECTED;
    }

    public boolean canEditBeforeApproval()
    {
        return this == DRAFT || this == REJECTED;
    }

    public boolean canBeAudited()
    {
        return this == PENDING_AUDIT;
    }

    public boolean canEntrust()
    {
        return this == DRAFT || this == PENDING_AUDIT || this == REJECTED || this == PUBLISHED;
    }

    public boolean canCancel()
    {
        return this == DRAFT || this == PENDING_AUDIT || this == PUBLISHED || this == REJECTED;
    }

    public boolean canCompleteDeal()
    {
        return this == PUBLISHED;
    }
}
