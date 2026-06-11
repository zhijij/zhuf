package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.List;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.RentalContract;
import com.ruoyi.system.domain.RentalContractConfirm;
import com.ruoyi.system.enums.RentalContractConfirmStatus;
import com.ruoyi.system.enums.RentalContractPartyRole;
import com.ruoyi.system.mapper.RentalContractConfirmMapper;
import com.ruoyi.system.service.IRentalContractConfirmService;

/**
 * 合同确认Service业务层处理。
 */
@Service
public class RentalContractConfirmServiceImpl implements IRentalContractConfirmService
{
    @Autowired
    private RentalContractConfirmMapper rentalContractConfirmMapper;

    @Override
    public RentalContractConfirm selectRentalContractConfirmByConfirmId(Long confirmId)
    {
        return rentalContractConfirmMapper.selectRentalContractConfirmByConfirmId(confirmId);
    }

    @Override
    public List<RentalContractConfirm> selectRentalContractConfirmList(RentalContractConfirm rentalContractConfirm)
    {
        return rentalContractConfirmMapper.selectRentalContractConfirmList(rentalContractConfirm);
    }

    @Override
    public int insertRentalContractConfirm(RentalContractConfirm rentalContractConfirm)
    {
        rentalContractConfirm.setCreateTime(DateUtils.getNowDate());
        return rentalContractConfirmMapper.insertRentalContractConfirm(rentalContractConfirm);
    }

    @Override
    public int updateRentalContractConfirm(RentalContractConfirm rentalContractConfirm)
    {
        rentalContractConfirm.setUpdateTime(DateUtils.getNowDate());
        return rentalContractConfirmMapper.updateRentalContractConfirm(rentalContractConfirm);
    }

    @Override
    public int deleteRentalContractConfirmByConfirmIds(Long[] confirmIds)
    {
        return rentalContractConfirmMapper.deleteRentalContractConfirmByConfirmIds(confirmIds);
    }

    @Override
    public int deleteRentalContractConfirmByConfirmId(Long confirmId)
    {
        return rentalContractConfirmMapper.deleteRentalContractConfirmByConfirmId(confirmId);
    }

    @Override
    public void initContractConfirms(RentalContract contract)
    {
        ensureConfirm(contract.getContractId(), contract.getTenantId(), RentalContractPartyRole.TENANT.code());
        ensureConfirm(contract.getContractId(), contract.getOwnerId(), RentalContractPartyRole.OWNER.code());
        if (contract.getAgentId() != null)
        {
            ensureConfirm(contract.getContractId(), contract.getAgentId(), RentalContractPartyRole.AGENT.code());
        }
    }

    @Override
    public int confirmContract(Long contractId, Long userId, String userRole, String opinion)
    {
        RentalContractConfirm confirm = requireConfirm(contractId, userId, userRole);
        confirm.setConfirmStatus(RentalContractConfirmStatus.CONFIRMED.code());
        confirm.setConfirmOpinion(opinion);
        confirm.setConfirmTime(new Date());
        return updateRentalContractConfirm(confirm);
    }

    @Override
    public int rejectContract(Long contractId, Long userId, String userRole, String opinion)
    {
        RentalContractConfirm confirm = requireConfirm(contractId, userId, userRole);
        confirm.setConfirmStatus(RentalContractConfirmStatus.REJECTED.code());
        confirm.setConfirmOpinion(opinion);
        confirm.setConfirmTime(new Date());
        return updateRentalContractConfirm(confirm);
    }

    @Override
    public boolean isAllPartiesConfirmed(RentalContract contract)
    {
        return isConfirmed(contract.getContractId(), RentalContractPartyRole.TENANT.code())
                && isConfirmed(contract.getContractId(), RentalContractPartyRole.OWNER.code())
                && (contract.getAgentId() == null || isConfirmed(contract.getContractId(), RentalContractPartyRole.AGENT.code()));
    }

    private void ensureConfirm(Long contractId, Long userId, String userRole)
    {
        if (userId == null)
        {
            return;
        }
        RentalContractConfirm query = new RentalContractConfirm();
        query.setContractId(contractId);
        query.setUserRole(userRole);
        RentalContractConfirm old = rentalContractConfirmMapper.selectRentalContractConfirmByContractAndRole(query);
        if (old != null)
        {
            return;
        }

        RentalContractConfirm confirm = new RentalContractConfirm();
        confirm.setContractId(contractId);
        confirm.setUserId(userId);
        confirm.setUserRole(userRole);
        confirm.setConfirmStatus(RentalContractConfirmStatus.PENDING.code());
        insertRentalContractConfirm(confirm);
    }

    private RentalContractConfirm requireConfirm(Long contractId, Long userId, String userRole)
    {
        RentalContractConfirm query = new RentalContractConfirm();
        query.setContractId(contractId);
        query.setUserRole(userRole);
        RentalContractConfirm confirm = rentalContractConfirmMapper.selectRentalContractConfirmByContractAndRole(query);
        if (confirm == null)
        {
            throw new ServiceException("合同确认记录不存在");
        }
        if (userId == null || !userId.equals(confirm.getUserId()))
        {
            throw new ServiceException("当前用户不能以该角色确认合同");
        }
        return confirm;
    }

    private boolean isConfirmed(Long contractId, String userRole)
    {
        RentalContractConfirm query = new RentalContractConfirm();
        query.setContractId(contractId);
        query.setUserRole(userRole);
        RentalContractConfirm confirm = rentalContractConfirmMapper.selectRentalContractConfirmByContractAndRole(query);
        return confirm != null && RentalContractConfirmStatus.CONFIRMED.code().equals(confirm.getConfirmStatus());
    }
}
