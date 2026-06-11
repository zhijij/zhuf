package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.RentalContract;
import com.ruoyi.system.domain.RentalContractConfirm;

/**
 * 合同确认Service接口。
 */
public interface IRentalContractConfirmService
{
    public RentalContractConfirm selectRentalContractConfirmByConfirmId(Long confirmId);

    public List<RentalContractConfirm> selectRentalContractConfirmList(RentalContractConfirm rentalContractConfirm);

    public int insertRentalContractConfirm(RentalContractConfirm rentalContractConfirm);

    public int updateRentalContractConfirm(RentalContractConfirm rentalContractConfirm);

    public int deleteRentalContractConfirmByConfirmIds(Long[] confirmIds);

    public int deleteRentalContractConfirmByConfirmId(Long confirmId);

    /**
     * 初始化合同参与方确认记录。
     *
     * @param contract 合同
     */
    public void initContractConfirms(RentalContract contract);

    /**
     * 参与方确认合同。
     *
     * @param contractId 合同ID
     * @param userId 当前用户ID
     * @param userRole 参与方角色
     * @param opinion 确认意见
     * @return 结果
     */
    public int confirmContract(Long contractId, Long userId, String userRole, String opinion);

    /**
     * 参与方拒绝合同。
     *
     * @param contractId 合同ID
     * @param userId 当前用户ID
     * @param userRole 参与方角色
     * @param opinion 拒绝意见
     * @return 结果
     */
    public int rejectContract(Long contractId, Long userId, String userRole, String opinion);

    /**
     * 判断合同所有必要参与方是否均已确认。
     *
     * @param contract 合同
     * @return 是否全部确认
     */
    public boolean isAllPartiesConfirmed(RentalContract contract);
}
