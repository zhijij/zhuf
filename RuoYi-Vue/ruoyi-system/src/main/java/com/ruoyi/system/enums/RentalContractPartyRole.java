package com.ruoyi.system.enums;

/**
 * 合同参与方角色。
 */
public enum RentalContractPartyRole
{
    TENANT("tenant", "租户"),
    OWNER("owner", "房东"),
    AGENT("agent", "中介");

    private final String code;
    private final String label;

    RentalContractPartyRole(String code, String label)
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
