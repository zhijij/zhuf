package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.RentalHouseMapper;
import com.ruoyi.system.domain.RentalHouse;
import com.ruoyi.system.service.IRentalHouseService;

/**
 * 租赁房源Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
@Service
public class RentalHouseServiceImpl implements IRentalHouseService 
{
    @Autowired
    private RentalHouseMapper rentalHouseMapper;

    /**
     * 查询租赁房源
     * 
     * @param houseId 租赁房源主键
     * @return 租赁房源
     */
    @Override
    public RentalHouse selectRentalHouseByHouseId(Long houseId)
    {
        return rentalHouseMapper.selectRentalHouseByHouseId(houseId);
    }

    /**
     * 查询租赁房源列表
     * 
     * @param rentalHouse 租赁房源
     * @return 租赁房源
     */
    @Override
    public List<RentalHouse> selectRentalHouseList(RentalHouse rentalHouse)
    {
        return rentalHouseMapper.selectRentalHouseList(rentalHouse);
    }

    /**
     * 新增租赁房源
     * 
     * @param rentalHouse 租赁房源
     * @return 结果
     */
    @Override
    public int insertRentalHouse(RentalHouse rentalHouse)
    {
        rentalHouse.setCreateTime(DateUtils.getNowDate());
        return rentalHouseMapper.insertRentalHouse(rentalHouse);
    }

    /**
     * 修改租赁房源
     * 
     * @param rentalHouse 租赁房源
     * @return 结果
     */
    @Override
    public int updateRentalHouse(RentalHouse rentalHouse)
    {
        rentalHouse.setUpdateTime(DateUtils.getNowDate());
        return rentalHouseMapper.updateRentalHouse(rentalHouse);
    }

    /**
     * 批量删除租赁房源
     * 
     * @param houseIds 需要删除的租赁房源主键
     * @return 结果
     */
    @Override
    public int deleteRentalHouseByHouseIds(Long[] houseIds)
    {
        return rentalHouseMapper.deleteRentalHouseByHouseIds(houseIds);
    }

    /**
     * 删除租赁房源信息
     * 
     * @param houseId 租赁房源主键
     * @return 结果
     */
    @Override
    public int deleteRentalHouseByHouseId(Long houseId)
    {
        return rentalHouseMapper.deleteRentalHouseByHouseId(houseId);
    }
}
