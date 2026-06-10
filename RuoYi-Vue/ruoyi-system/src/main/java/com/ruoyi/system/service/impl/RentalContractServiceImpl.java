package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.RentalContractMapper;
import com.ruoyi.system.domain.RentalContract;
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
        return rentalContractMapper.insertRentalContract(rentalContract);
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
}
