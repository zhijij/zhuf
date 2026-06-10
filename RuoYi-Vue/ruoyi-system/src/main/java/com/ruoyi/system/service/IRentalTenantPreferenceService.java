package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.RentalTenantPreference;

/**
 * 租户租房偏好Service接口
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
public interface IRentalTenantPreferenceService 
{
    /**
     * 查询租户租房偏好
     * 
     * @param preferenceId 租户租房偏好主键
     * @return 租户租房偏好
     */
    public RentalTenantPreference selectRentalTenantPreferenceByPreferenceId(Long preferenceId);

    /**
     * 查询租户租房偏好列表
     * 
     * @param rentalTenantPreference 租户租房偏好
     * @return 租户租房偏好集合
     */
    public List<RentalTenantPreference> selectRentalTenantPreferenceList(RentalTenantPreference rentalTenantPreference);

    /**
     * 新增租户租房偏好
     * 
     * @param rentalTenantPreference 租户租房偏好
     * @return 结果
     */
    public int insertRentalTenantPreference(RentalTenantPreference rentalTenantPreference);

    /**
     * 修改租户租房偏好
     * 
     * @param rentalTenantPreference 租户租房偏好
     * @return 结果
     */
    public int updateRentalTenantPreference(RentalTenantPreference rentalTenantPreference);

    /**
     * 批量删除租户租房偏好
     * 
     * @param preferenceIds 需要删除的租户租房偏好主键集合
     * @return 结果
     */
    public int deleteRentalTenantPreferenceByPreferenceIds(Long[] preferenceIds);

    /**
     * 删除租户租房偏好信息
     * 
     * @param preferenceId 租户租房偏好主键
     * @return 结果
     */
    public int deleteRentalTenantPreferenceByPreferenceId(Long preferenceId);
}
