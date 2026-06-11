package com.ruoyi.system.enums;

/**
 * 合同参与方确认状态。
 */
public enum RentalContractConfirmStatus
{
    PENDING("0", "未确认"),
    CONFIRMED("1", "已确认"),
    REJECTED("2", "已拒绝");

    private final String code;
    private final String label;

    RentalContractConfirmStatus(String code, String label)
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
