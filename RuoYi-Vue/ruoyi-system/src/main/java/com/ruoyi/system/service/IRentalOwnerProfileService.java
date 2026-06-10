package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.RentalOwnerProfile;

/**
 * 户主资料Service接口
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
public interface IRentalOwnerProfileService 
{
    /**
     * 查询户主资料
     * 
     * @param ownerId 户主资料主键
     * @return 户主资料
     */
    public RentalOwnerProfile selectRentalOwnerProfileByOwnerId(Long ownerId);

    /**
     * 查询户主资料列表
     * 
     * @param rentalOwnerProfile 户主资料
     * @return 户主资料集合
     */
    public List<RentalOwnerProfile> selectRentalOwnerProfileList(RentalOwnerProfile rentalOwnerProfile);

    /**
     * 新增户主资料
     * 
     * @param rentalOwnerProfile 户主资料
     * @return 结果
     */
    public int insertRentalOwnerProfile(RentalOwnerProfile rentalOwnerProfile);

    /**
     * 修改户主资料
     * 
     * @param rentalOwnerProfile 户主资料
     * @return 结果
     */
    public int updateRentalOwnerProfile(RentalOwnerProfile rentalOwnerProfile);

    /**
     * 批量删除户主资料
     * 
     * @param ownerIds 需要删除的户主资料主键集合
     * @return 结果
     */
    public int deleteRentalOwnerProfileByOwnerIds(Long[] ownerIds);

    /**
     * 删除户主资料信息
     * 
     * @param ownerId 户主资料主键
     * @return 结果
     */
    public int deleteRentalOwnerProfileByOwnerId(Long ownerId);
}
