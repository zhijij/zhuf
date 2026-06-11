package com.ruoyi.system.domain.vo;

import java.util.List;
import com.ruoyi.system.domain.RentalContract;
import com.ruoyi.system.domain.RentalContractConfirm;

/**
 * 租赁合同详情视图。
 */
public class RentalContractDetailVo
{
    private RentalContract contract;

    private List<RentalContractConfirm> confirms;

    public RentalContract getContract()
    {
        return contract;
    }

    public void setContract(RentalContract contract)
    {
        this.contract = contract;
    }

    public List<RentalContractConfirm> getConfirms()
    {
        return confirms;
    }

    public void setConfirms(List<RentalContractConfirm> confirms)
    {
        this.confirms = confirms;
    }
}
