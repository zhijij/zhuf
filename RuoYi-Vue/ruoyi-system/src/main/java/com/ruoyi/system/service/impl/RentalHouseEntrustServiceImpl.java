package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.RentalHouseEntrustMapper;
import com.ruoyi.system.domain.RentalHouseEntrust;
import com.ruoyi.system.service.IRentalHouseEntrustService;

/**
 * 房源委托关系Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
@Service
public class RentalHouseEntrustServiceImpl implements IRentalHouseEntrustService 
{
    @Autowired
    private RentalHouseEntrustMapper rentalHouseEntrustMapper;

    /**
     * 查询房源委托关系
     * 
     * @param entrustId 房源委托关系主键
     * @return 房源委托关系
     */
    @Override
    public RentalHouseEntrust selectRentalHouseEntrustByEntrustId(Long entrustId)
    {
        return rentalHouseEntrustMapper.selectRentalHouseEntrustByEntrustId(entrustId);
    }

    /**
     * 查询房源委托关系列表
     * 
     * @param rentalHouseEntrust 房源委托关系
     * @return 房源委托关系
     */
    @Override
    public List<RentalHouseEntrust> selectRentalHouseEntrustList(RentalHouseEntrust rentalHouseEntrust)
    {
        return rentalHouseEntrustMapper.selectRentalHouseEntrustList(rentalHouseEntrust);
    }

    /**
     * 新增房源委托关系
     * 
     * @param rentalHouseEntrust 房源委托关系
     * @return 结果
     */
    @Override
    public int insertRentalHouseEntrust(RentalHouseEntrust rentalHouseEntrust)
    {
        rentalHouseEntrust.setCreateTime(DateUtils.getNowDate());
        return rentalHouseEntrustMapper.insertRentalHouseEntrust(rentalHouseEntrust);
    }

    /**
     * 修改房源委托关系
     * 
     * @param rentalHouseEntrust 房源委托关系
     * @return 结果
     */
    @Override
    public int updateRentalHouseEntrust(RentalHouseEntrust rentalHouseEntrust)
    {
        rentalHouseEntrust.setUpdateTime(DateUtils.getNowDate());
        return rentalHouseEntrustMapper.updateRentalHouseEntrust(rentalHouseEntrust);
    }

    /**
     * 批量删除房源委托关系
     * 
     * @param entrustIds 需要删除的房源委托关系主键
     * @return 结果
     */
    @Override
    public int deleteRentalHouseEntrustByEntrustIds(Long[] entrustIds)
    {
        return rentalHouseEntrustMapper.deleteRentalHouseEntrustByEntrustIds(entrustIds);
    }

    /**
     * 删除房源委托关系信息
     * 
     * @param entrustId 房源委托关系主键
     * @return 结果
     */
    @Override
    public int deleteRentalHouseEntrustByEntrustId(Long entrustId)
    {
        return rentalHouseEntrustMapper.deleteRentalHouseEntrustByEntrustId(entrustId);
    }
}
