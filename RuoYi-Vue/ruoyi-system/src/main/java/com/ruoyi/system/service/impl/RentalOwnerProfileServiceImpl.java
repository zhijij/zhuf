package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.RentalOwnerProfileMapper;
import com.ruoyi.system.domain.RentalOwnerProfile;
import com.ruoyi.system.service.IRentalOwnerProfileService;

/**
 * 户主资料Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
@Service
public class RentalOwnerProfileServiceImpl implements IRentalOwnerProfileService 
{
    @Autowired
    private RentalOwnerProfileMapper rentalOwnerProfileMapper;

    /**
     * 查询户主资料
     * 
     * @param ownerId 户主资料主键
     * @return 户主资料
     */
    @Override
    public RentalOwnerProfile selectRentalOwnerProfileByOwnerId(Long ownerId)
    {
        return rentalOwnerProfileMapper.selectRentalOwnerProfileByOwnerId(ownerId);
    }

    /**
     * 查询户主资料列表
     * 
     * @param rentalOwnerProfile 户主资料
     * @return 户主资料
     */
    @Override
    public List<RentalOwnerProfile> selectRentalOwnerProfileList(RentalOwnerProfile rentalOwnerProfile)
    {
        return rentalOwnerProfileMapper.selectRentalOwnerProfileList(rentalOwnerProfile);
    }

    /**
     * 新增户主资料
     * 
     * @param rentalOwnerProfile 户主资料
     * @return 结果
     */
    @Override
    public int insertRentalOwnerProfile(RentalOwnerProfile rentalOwnerProfile)
    {
        rentalOwnerProfile.setCreateTime(DateUtils.getNowDate());
        return rentalOwnerProfileMapper.insertRentalOwnerProfile(rentalOwnerProfile);
    }

    /**
     * 修改户主资料
     * 
     * @param rentalOwnerProfile 户主资料
     * @return 结果
     */
    @Override
    public int updateRentalOwnerProfile(RentalOwnerProfile rentalOwnerProfile)
    {
        rentalOwnerProfile.setUpdateTime(DateUtils.getNowDate());
        return rentalOwnerProfileMapper.updateRentalOwnerProfile(rentalOwnerProfile);
    }

    /**
     * 批量删除户主资料
     * 
     * @param ownerIds 需要删除的户主资料主键
     * @return 结果
     */
    @Override
    public int deleteRentalOwnerProfileByOwnerIds(Long[] ownerIds)
    {
        return rentalOwnerProfileMapper.deleteRentalOwnerProfileByOwnerIds(ownerIds);
    }

    /**
     * 删除户主资料信息
     * 
     * @param ownerId 户主资料主键
     * @return 结果
     */
    @Override
    public int deleteRentalOwnerProfileByOwnerId(Long ownerId)
    {
        return rentalOwnerProfileMapper.deleteRentalOwnerProfileByOwnerId(ownerId);
    }
}
