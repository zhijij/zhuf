package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.RentalContract;
import com.ruoyi.system.domain.vo.RentalContractDetailVo;

/**
 * 租赁合同Service接口
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
public interface IRentalContractService 
{
    /**
     * 查询租赁合同
     * 
     * @param contractId 租赁合同主键
     * @return 租赁合同
     */
    public RentalContract selectRentalContractByContractId(Long contractId);

    /**
     * 查询合同业务详情，包含参与方确认状态。
     *
     * @param contractId 合同ID
     * @param viewerId 查看人ID
     * @param platformAdmin 是否后台管理员
     * @return 合同详情
     */
    public RentalContractDetailVo selectRentalContractDetail(Long contractId, Long viewerId, boolean platformAdmin);

    /**
     * 查询租赁合同列表
     * 
     * @param rentalContract 租赁合同
     * @return 租赁合同集合
     */
    public List<RentalContract> selectRentalContractList(RentalContract rentalContract);

    /**
     * 查询当前用户作为租户、房东或中介参与的合同列表。
     *
     * @param userId 用户ID
     * @return 租赁合同集合
     */
    public List<RentalContract> selectMyRentalContractList(Long userId);

    /**
     * 新增租赁合同
     * 
     * @param rentalContract 租赁合同
     * @return 结果
     */
    public int insertRentalContract(RentalContract rentalContract);

    /**
     * 修改租赁合同
     * 
     * @param rentalContract 租赁合同
     * @return 结果
     */
    public int updateRentalContract(RentalContract rentalContract);

    /**
     * 合同提交待签。
     *
     * @param contractId 合同ID
     * @param operatorId 操作人ID
     * @param platformAdmin 是否后台管理员
     * @return 结果
     */
    public int submitContractSign(Long contractId, Long operatorId, boolean platformAdmin);

    /**
     * 参与方确认合同。
     *
     * @param contractId 合同ID
     * @param userId 用户ID
     * @param userRole 用户角色
     * @param opinion 确认意见
     * @return 结果
     */
    public int confirmContract(Long contractId, Long userId, String userRole, String opinion);

    /**
     * 参与方拒绝合同。
     *
     * @param contractId 合同ID
     * @param userId 用户ID
     * @param userRole 用户角色
     * @param opinion 拒绝意见
     * @return 结果
     */
    public int rejectContract(Long contractId, Long userId, String userRole, String opinion);

    /**
     * 合同生效。
     *
     * @param contractId 合同ID
     * @param operatorId 操作人ID
     * @param platformAdmin 是否后台管理员
     * @return 结果
     */
    public int activateContract(Long contractId, Long operatorId, boolean platformAdmin);

    /**
     * 合同作废。
     *
     * @param contractId 合同ID
     * @param operatorId 操作人ID
     * @param platformAdmin 是否后台管理员
     * @param reason 作废原因
     * @return 结果
     */
    public int voidContract(Long contractId, Long operatorId, boolean platformAdmin, String reason);

    /**
     * 合同终止。
     *
     * @param contractId 合同ID
     * @param operatorId 操作人ID
     * @param platformAdmin 是否后台管理员
     * @param reason 终止原因
     * @return 结果
     */
    public int terminateContract(Long contractId, Long operatorId, boolean platformAdmin, String reason);

    /**
     * 批量删除租赁合同
     * 
     * @param contractIds 需要删除的租赁合同主键集合
     * @return 结果
     */
    public int deleteRentalContractByContractIds(Long[] contractIds);

    /**
     * 删除租赁合同信息
     * 
     * @param contractId 租赁合同主键
     * @return 结果
     */
    public int deleteRentalContractByContractId(Long contractId);
}
