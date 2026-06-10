package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.RentalHouseFavorite;

/**
 * 房源收藏Service接口
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
public interface IRentalHouseFavoriteService 
{
    /**
     * 查询房源收藏
     * 
     * @param favoriteId 房源收藏主键
     * @return 房源收藏
     */
    public RentalHouseFavorite selectRentalHouseFavoriteByFavoriteId(Long favoriteId);

    /**
     * 查询房源收藏列表
     * 
     * @param rentalHouseFavorite 房源收藏
     * @return 房源收藏集合
     */
    public List<RentalHouseFavorite> selectRentalHouseFavoriteList(RentalHouseFavorite rentalHouseFavorite);

    /**
     * 新增房源收藏
     * 
     * @param rentalHouseFavorite 房源收藏
     * @return 结果
     */
    public int insertRentalHouseFavorite(RentalHouseFavorite rentalHouseFavorite);

    /**
     * 修改房源收藏
     * 
     * @param rentalHouseFavorite 房源收藏
     * @return 结果
     */
    public int updateRentalHouseFavorite(RentalHouseFavorite rentalHouseFavorite);

    /**
     * 批量删除房源收藏
     * 
     * @param favoriteIds 需要删除的房源收藏主键集合
     * @return 结果
     */
    public int deleteRentalHouseFavoriteByFavoriteIds(Long[] favoriteIds);

    /**
     * 删除房源收藏信息
     * 
     * @param favoriteId 房源收藏主键
     * @return 结果
     */
    public int deleteRentalHouseFavoriteByFavoriteId(Long favoriteId);
}
