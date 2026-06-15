package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.RentalHouseImage;

/**
 * 房源图片Mapper接口
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
public interface RentalHouseImageMapper 
{
    /**
     * 查询房源图片
     * 
     * @param imageId 房源图片主键
     * @return 房源图片
     */
    public RentalHouseImage selectRentalHouseImageByImageId(Long imageId);

    /**
     * 查询房源图片列表
     * 
     * @param rentalHouseImage 房源图片
     * @return 房源图片集合
     */
    public List<RentalHouseImage> selectRentalHouseImageList(RentalHouseImage rentalHouseImage);

    /**
     * 新增房源图片
     * 
     * @param rentalHouseImage 房源图片
     * @return 结果
     */
    public int insertRentalHouseImage(RentalHouseImage rentalHouseImage);

    /**
     * 修改房源图片
     * 
     * @param rentalHouseImage 房源图片
     * @return 结果
     */
    public int updateRentalHouseImage(RentalHouseImage rentalHouseImage);

    /**
     * 删除房源图片
     * 
     * @param imageId 房源图片主键
     * @return 结果
     */
    public int deleteRentalHouseImageByImageId(Long imageId);

    /**
     * 删除某个房源的全部图片
     *
     * @param houseId 房源ID
     * @return 结果
     */
    public int deleteRentalHouseImageByHouseId(Long houseId);

    /**
     * 批量删除房源图片
     * 
     * @param imageIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteRentalHouseImageByImageIds(Long[] imageIds);
}
