package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.system.domain.RentalContractConfirm;
import com.ruoyi.system.domain.RentalHouse;
import com.ruoyi.system.domain.vo.RentalContractDetailVo;
import com.ruoyi.system.enums.RentalContractStatus;
import com.ruoyi.system.enums.RentalHouseStatus;
import com.ruoyi.system.mapper.RentalContractMapper;
import com.ruoyi.system.mapper.RentalHouseMapper;
import com.ruoyi.system.domain.RentalContract;
import com.ruoyi.system.service.IRentalContractConfirmService;
import com.ruoyi.system.service.IRentalContractService;

/**
 * 租赁合同Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
@Service
public class RentalContractServiceImpl implements IRentalContractService 
{
    @Autowired
    private RentalContractMapper rentalContractMapper;

    @Autowired
    private IRentalContractConfirmService rentalContractConfirmService;

    @Autowired
    private RentalHouseMapper rentalHouseMapper;

    /**
     * 查询租赁合同
     * 
     * @param contractId 租赁合同主键
     * @return 租赁合同
     */
    @Override
    public RentalContract selectRentalContractByContractId(Long contractId)
    {
        return rentalContractMapper.selectRentalContractByContractId(contractId);
    }

    @Override
    public RentalContractDetailVo selectRentalContractDetail(Long contractId, Long viewerId, boolean platformAdmin)
    {
        RentalContract contract = requireContract(contractId);
        if (!platformAdmin && !isContractParticipant(contract, viewerId))
        {
            throw new ServiceException("当前用户无权查看合同");
        }
        rentalContractConfirmService.initContractConfirms(contract);
        RentalContractConfirm query = new RentalContractConfirm();
        query.setContractId(contractId);

        RentalContractDetailVo detail = new RentalContractDetailVo();
        detail.setContract(contract);
        detail.setConfirms(rentalContractConfirmService.selectRentalContractConfirmList(query));
        return detail;
    }

    /**
     * 查询租赁合同列表
     * 
     * @param rentalContract 租赁合同
     * @return 租赁合同
     */
    @Override
    public List<RentalContract> selectRentalContractList(RentalContract rentalContract)
    {
        return rentalContractMapper.selectRentalContractList(rentalContract);
    }

    @Override
    public List<RentalContract> selectMyRentalContractList(Long userId)
    {
        if (userId == null)
        {
            throw new ServiceException("用户ID不能为空");
        }
        return rentalContractMapper.selectRentalContractListByParticipant(userId);
    }

    /**
     * 新增租赁合同
     * 
     * @param rentalContract 租赁合同
     * @return 结果
     */
    @Override
    public int insertRentalContract(RentalContract rentalContract)
    {
        rentalContract.setCreateTime(DateUtils.getNowDate());
        int rows = rentalContractMapper.insertRentalContract(rentalContract);
        rentalContractConfirmService.initContractConfirms(rentalContract);
        return rows;
    }

    /**
     * 修改租赁合同
     * 
     * @param rentalContract 租赁合同
     * @return 结果
     */
    @Override
    public int updateRentalContract(RentalContract rentalContract)
    {
        rentalContract.setUpdateTime(DateUtils.getNowDate());
        return rentalContractMapper.updateRentalContract(rentalContract);
    }

    @Override
    public int submitContractSign(Long contractId, Long operatorId, boolean platformAdmin)
    {
        RentalContract contract = requireManageableContract(contractId, operatorId, platformAdmin);
        RentalContractStatus status = RentalContractStatus.of(contract.getStatus());
        if (status != RentalContractStatus.DRAFT)
        {
            throw new ServiceException("只有草稿合同可以提交确认");
        }
        rentalContractConfirmService.initContractConfirms(contract);
        RentalContract update = new RentalContract();
        update.setContractId(contractId);
        update.setStatus(RentalContractStatus.WAIT_SIGN.code());
        return updateRentalContract(update);
    }

    @Override
    public int confirmContract(Long contractId, Long userId, String userRole, String opinion)
    {
        RentalContract contract = requireContract(contractId);
        ensureWaitSign(contract);
        int rows = rentalContractConfirmService.confirmContract(contractId, userId, userRole, opinion);
        if (rentalContractConfirmService.isAllPartiesConfirmed(contract))
        {
            activateContract(contractId, userId, false);
        }
        return rows;
    }

    @Override
    public int rejectContract(Long contractId, Long userId, String userRole, String opinion)
    {
        RentalContract contract = requireContract(contractId);
        ensureWaitSign(contract);
        rentalContractConfirmService.rejectContract(contractId, userId, userRole, opinion);
        RentalContract update = new RentalContract();
        update.setContractId(contractId);
        update.setStatus(RentalContractStatus.VOIDED.code());
        return updateRentalContract(update);
    }

    @Override
    @Transactional
    public int activateContract(Long contractId, Long operatorId, boolean platformAdmin)
    {
        RentalContract contract = requireManageableContract(contractId, operatorId, platformAdmin);
        if (!rentalContractConfirmService.isAllPartiesConfirmed(contract))
        {
            throw new ServiceException("合同参与方尚未全部确认");
        }
        RentalContract update = new RentalContract();
        update.setContractId(contractId);
        update.setStatus(RentalContractStatus.ACTIVE.code());
        int rows = updateRentalContract(update);

        RentalHouse house = new RentalHouse();
        house.setHouseId(contract.getHouseId());
        house.setStatus(RentalHouseStatus.RENTED.code());
        house.setAiIndexStatus("0");
        house.setUpdateTime(DateUtils.getNowDate());
        rentalHouseMapper.updateRentalHouse(house);
        return rows;
    }

    @Override
    public int voidContract(Long contractId, Long operatorId, boolean platformAdmin, String reason)
    {
        RentalContract contract = requireManageableContract(contractId, operatorId, platformAdmin);
        RentalContractStatus status = RentalContractStatus.of(contract.getStatus());
        if (status == RentalContractStatus.ACTIVE || status == RentalContractStatus.TERMINATED)
        {
            throw new ServiceException("已生效或已终止合同不可作废");
        }
        RentalContract update = new RentalContract();
        update.setContractId(contractId);
        update.setStatus(RentalContractStatus.VOIDED.code());
        return updateRentalContract(update);
    }

    @Override
    public int terminateContract(Long contractId, Long operatorId, boolean platformAdmin, String reason)
    {
        RentalContract contract = requireManageableContract(contractId, operatorId, platformAdmin);
        RentalContractStatus status = RentalContractStatus.of(contract.getStatus());
        if (status != RentalContractStatus.ACTIVE)
        {
            throw new ServiceException("只有生效合同可以终止");
        }
        RentalContract update = new RentalContract();
        update.setContractId(contractId);
        update.setStatus(RentalContractStatus.TERMINATED.code());
        return updateRentalContract(update);
    }

    /**
     * 批量删除租赁合同
     * 
     * @param contractIds 需要删除的租赁合同主键
     * @return 结果
     */
    @Override
    public int deleteRentalContractByContractIds(Long[] contractIds)
    {
        return rentalContractMapper.deleteRentalContractByContractIds(contractIds);
    }

    /**
     * 删除租赁合同信息
     * 
     * @param contractId 租赁合同主键
     * @return 结果
     */
    @Override
    public int deleteRentalContractByContractId(Long contractId)
    {
        return rentalContractMapper.deleteRentalContractByContractId(contractId);
    }

    private RentalContract requireContract(Long contractId)
    {
        RentalContract contract = rentalContractMapper.selectRentalContractByContractId(contractId);
        if (contract == null)
        {
            throw new ServiceException("合同不存在");
        }
        return contract;
    }

    private RentalContract requireManageableContract(Long contractId, Long operatorId, boolean platformAdmin)
    {
        RentalContract contract = requireContract(contractId);
        if (!platformAdmin && !isContractParticipant(contract, operatorId))
        {
            throw new ServiceException("当前用户无权操作合同");
        }
        return contract;
    }

    private boolean isContractParticipant(RentalContract contract, Long userId)
    {
        return userId != null && (userId.equals(contract.getTenantId())
                || userId.equals(contract.getOwnerId())
                || userId.equals(contract.getAgentId()));
    }

    private void ensureWaitSign(RentalContract contract)
    {
        if (!RentalContractStatus.WAIT_SIGN.code().equals(contract.getStatus()))
        {
            throw new ServiceException("只有待确认合同可以进行确认操作");
        }
    }
}
