package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.RentalContract;

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
     * 查询租赁合同列表
     * 
     * @param rentalContract 租赁合同
     * @return 租赁合同集合
     */
    public List<RentalContract> selectRentalContractList(RentalContract rentalContract);

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
