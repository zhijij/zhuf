package com.ruoyi.system.enums;

/**
 * 房源运营方式。
 */
public enum RentalOperationMode
{
    OWNER_SELF("0", "房东自营"),
    AGENT_ENTRUST("1", "委托中介");

    private final String code;
    private final String label;

    RentalOperationMode(String code, String label)
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
