package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.RentalHouse;

/**
 * 租赁房源Mapper接口
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
public interface RentalHouseMapper 
{
    /**
     * 查询租赁房源
     * 
     * @param houseId 租赁房源主键
     * @return 租赁房源
     */
    public RentalHouse selectRentalHouseByHouseId(Long houseId);

    /**
     * 查询租赁房源列表
     * 
     * @param rentalHouse 租赁房源
     * @return 租赁房源集合
     */
    public List<RentalHouse> selectRentalHouseList(RentalHouse rentalHouse);

    /**
     * 新增租赁房源
     * 
     * @param rentalHouse 租赁房源
     * @return 结果
     */
    public int insertRentalHouse(RentalHouse rentalHouse);

    /**
     * 修改租赁房源
     * 
     * @param rentalHouse 租赁房源
     * @return 结果
     */
    public int updateRentalHouse(RentalHouse rentalHouse);

    /**
     * 删除租赁房源
     * 
     * @param houseId 租赁房源主键
     * @return 结果
     */
    public int deleteRentalHouseByHouseId(Long houseId);

    /**
     * 批量删除租赁房源
     * 
     * @param houseIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteRentalHouseByHouseIds(Long[] houseIds);
}
