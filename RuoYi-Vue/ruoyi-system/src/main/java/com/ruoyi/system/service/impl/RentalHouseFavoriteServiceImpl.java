package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.RentalHouseFavoriteMapper;
import com.ruoyi.system.domain.RentalHouseFavorite;
import com.ruoyi.system.service.IRentalHouseFavoriteService;

/**
 * 房源收藏Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
@Service
public class RentalHouseFavoriteServiceImpl implements IRentalHouseFavoriteService 
{
    @Autowired
    private RentalHouseFavoriteMapper rentalHouseFavoriteMapper;

    /**
     * 查询房源收藏
     * 
     * @param favoriteId 房源收藏主键
     * @return 房源收藏
     */
    @Override
    public RentalHouseFavorite selectRentalHouseFavoriteByFavoriteId(Long favoriteId)
    {
        return rentalHouseFavoriteMapper.selectRentalHouseFavoriteByFavoriteId(favoriteId);
    }

    /**
     * 查询房源收藏列表
     * 
     * @param rentalHouseFavorite 房源收藏
     * @return 房源收藏
     */
    @Override
    public List<RentalHouseFavorite> selectRentalHouseFavoriteList(RentalHouseFavorite rentalHouseFavorite)
    {
        return rentalHouseFavoriteMapper.selectRentalHouseFavoriteList(rentalHouseFavorite);
    }

    /**
     * 新增房源收藏
     * 
     * @param rentalHouseFavorite 房源收藏
     * @return 结果
     */
    @Override
    public int insertRentalHouseFavorite(RentalHouseFavorite rentalHouseFavorite)
    {
        rentalHouseFavorite.setCreateTime(DateUtils.getNowDate());
        return rentalHouseFavoriteMapper.insertRentalHouseFavorite(rentalHouseFavorite);
    }

    /**
     * 修改房源收藏
     * 
     * @param rentalHouseFavorite 房源收藏
     * @return 结果
     */
    @Override
    public int updateRentalHouseFavorite(RentalHouseFavorite rentalHouseFavorite)
    {
        return rentalHouseFavoriteMapper.updateRentalHouseFavorite(rentalHouseFavorite);
    }

    /**
     * 批量删除房源收藏
     * 
     * @param favoriteIds 需要删除的房源收藏主键
     * @return 结果
     */
    @Override
    public int deleteRentalHouseFavoriteByFavoriteIds(Long[] favoriteIds)
    {
        return rentalHouseFavoriteMapper.deleteRentalHouseFavoriteByFavoriteIds(favoriteIds);
    }

    /**
     * 删除房源收藏信息
     * 
     * @param favoriteId 房源收藏主键
     * @return 结果
     */
    @Override
    public int deleteRentalHouseFavoriteByFavoriteId(Long favoriteId)
    {
        return rentalHouseFavoriteMapper.deleteRentalHouseFavoriteByFavoriteId(favoriteId);
    }
}
