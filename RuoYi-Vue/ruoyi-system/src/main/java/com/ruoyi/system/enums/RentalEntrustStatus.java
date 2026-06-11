package com.ruoyi.system.enums;

/**
 * 房源委托状态。
 */
public enum RentalEntrustStatus
{
    PENDING("0", "待确认"),
    ACTIVE("1", "生效中"),
    REJECTED("2", "已拒绝"),
    TERMINATED("3", "已终止"),
    EXPIRED("4", "已过期");

    private final String code;
    private final String label;

    RentalEntrustStatus(String code, String label)
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
}
