package com.ruoyi.system.domain.dto;

/**
 * 中介跟进租赁意向请求。
 */
public class RentalIntentionFollowRequest
{
    /** 意向等级：1低，2中，3高 */
    private String intentionLevel;

    /** 跟进备注 */
    private String note;

    public String getIntentionLevel()
    {
        return intentionLevel;
    }

    public void setIntentionLevel(String intentionLevel)
    {
        this.intentionLevel = intentionLevel;
    }

    public String getNote()
    {
        return note;
    }

    public void setNote(String note)
    {
        this.note = note;
    }
}
