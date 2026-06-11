package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.RentalContractConfirm;

/**
 * 合同确认Mapper接口。
 */
public interface RentalContractConfirmMapper
{
    public RentalContractConfirm selectRentalContractConfirmByConfirmId(Long confirmId);

    public List<RentalContractConfirm> selectRentalContractConfirmList(RentalContractConfirm rentalContractConfirm);

    public RentalContractConfirm selectRentalContractConfirmByContractAndRole(RentalContractConfirm rentalContractConfirm);

    public int insertRentalContractConfirm(RentalContractConfirm rentalContractConfirm);

    public int updateRentalContractConfirm(RentalContractConfirm rentalContractConfirm);

    public int deleteRentalContractConfirmByConfirmId(Long confirmId);

    public int deleteRentalContractConfirmByConfirmIds(Long[] confirmIds);
}
