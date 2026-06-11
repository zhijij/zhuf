package com.ruoyi.system.domain.dto;

/**
 * 合同确认请求。
 */
public class RentalContractConfirmRequest
{
    /** 确认意见 */
    private String opinion;

    public String getOpinion()
    {
        return opinion;
    }

    public void setOpinion(String opinion)
    {
        this.opinion = opinion;
    }
}
