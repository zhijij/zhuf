package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.RentalHouseImageMapper;
import com.ruoyi.system.domain.RentalHouseImage;
import com.ruoyi.system.service.IRentalHouseImageService;

/**
 * 房源图片Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
@Service
public class RentalHouseImageServiceImpl implements IRentalHouseImageService 
{
    @Autowired
    private RentalHouseImageMapper rentalHouseImageMapper;

    /**
     * 查询房源图片
     * 
     * @param imageId 房源图片主键
     * @return 房源图片
     */
    @Override
    public RentalHouseImage selectRentalHouseImageByImageId(Long imageId)
    {
        return rentalHouseImageMapper.selectRentalHouseImageByImageId(imageId);
    }

    /**
     * 查询房源图片列表
     * 
     * @param rentalHouseImage 房源图片
     * @return 房源图片
     */
    @Override
    public List<RentalHouseImage> selectRentalHouseImageList(RentalHouseImage rentalHouseImage)
    {
        return rentalHouseImageMapper.selectRentalHouseImageList(rentalHouseImage);
    }

    /**
     * 新增房源图片
     * 
     * @param rentalHouseImage 房源图片
     * @return 结果
     */
    @Override
    public int insertRentalHouseImage(RentalHouseImage rentalHouseImage)
    {
        rentalHouseImage.setCreateTime(DateUtils.getNowDate());
        return rentalHouseImageMapper.insertRentalHouseImage(rentalHouseImage);
    }

    /**
     * 修改房源图片
     * 
     * @param rentalHouseImage 房源图片
     * @return 结果
     */
    @Override
    public int updateRentalHouseImage(RentalHouseImage rentalHouseImage)
    {
        return rentalHouseImageMapper.updateRentalHouseImage(rentalHouseImage);
    }

    /**
     * 批量删除房源图片
     * 
     * @param imageIds 需要删除的房源图片主键
     * @return 结果
     */
    @Override
    public int deleteRentalHouseImageByImageIds(Long[] imageIds)
    {
        return rentalHouseImageMapper.deleteRentalHouseImageByImageIds(imageIds);
    }

    /**
     * 删除房源图片信息
     * 
     * @param imageId 房源图片主键
     * @return 结果
     */
    @Override
    public int deleteRentalHouseImageByImageId(Long imageId)
    {
        return rentalHouseImageMapper.deleteRentalHouseImageByImageId(imageId);
    }
}
