package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.RentalHouseEntrust;

/**
 * 房源委托关系Service接口
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
public interface IRentalHouseEntrustService 
{
    /**
     * 查询房源委托关系
     * 
     * @param entrustId 房源委托关系主键
     * @return 房源委托关系
     */
    public RentalHouseEntrust selectRentalHouseEntrustByEntrustId(Long entrustId);

    /**
     * 查询房源委托关系列表
     * 
     * @param rentalHouseEntrust 房源委托关系
     * @return 房源委托关系集合
     */
    public List<RentalHouseEntrust> selectRentalHouseEntrustList(RentalHouseEntrust rentalHouseEntrust);

    /**
     * 新增房源委托关系
     * 
     * @param rentalHouseEntrust 房源委托关系
     * @return 结果
     */
    public int insertRentalHouseEntrust(RentalHouseEntrust rentalHouseEntrust);

    /**
     * 修改房源委托关系
     * 
     * @param rentalHouseEntrust 房源委托关系
     * @return 结果
     */
    public int updateRentalHouseEntrust(RentalHouseEntrust rentalHouseEntrust);

    /**
     * 批量删除房源委托关系
     * 
     * @param entrustIds 需要删除的房源委托关系主键集合
     * @return 结果
     */
    public int deleteRentalHouseEntrustByEntrustIds(Long[] entrustIds);

    /**
     * 删除房源委托关系信息
     * 
     * @param entrustId 房源委托关系主键
     * @return 结果
     */
    public int deleteRentalHouseEntrustByEntrustId(Long entrustId);
}
