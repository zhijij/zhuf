package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.RentalTenantPreferenceMapper;
import com.ruoyi.system.domain.RentalTenantPreference;
import com.ruoyi.system.service.IRentalTenantPreferenceService;

/**
 * 租户租房偏好Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
@Service
public class RentalTenantPreferenceServiceImpl implements IRentalTenantPreferenceService 
{
    @Autowired
    private RentalTenantPreferenceMapper rentalTenantPreferenceMapper;

    /**
     * 查询租户租房偏好
     * 
     * @param preferenceId 租户租房偏好主键
     * @return 租户租房偏好
     */
    @Override
    public RentalTenantPreference selectRentalTenantPreferenceByPreferenceId(Long preferenceId)
    {
        return rentalTenantPreferenceMapper.selectRentalTenantPreferenceByPreferenceId(preferenceId);
    }

    /**
     * 查询租户租房偏好列表
     * 
     * @param rentalTenantPreference 租户租房偏好
     * @return 租户租房偏好
     */
    @Override
    public List<RentalTenantPreference> selectRentalTenantPreferenceList(RentalTenantPreference rentalTenantPreference)
    {
        return rentalTenantPreferenceMapper.selectRentalTenantPreferenceList(rentalTenantPreference);
    }

    /**
     * 新增租户租房偏好
     * 
     * @param rentalTenantPreference 租户租房偏好
     * @return 结果
     */
    @Override
    public int insertRentalTenantPreference(RentalTenantPreference rentalTenantPreference)
    {
        rentalTenantPreference.setCreateTime(DateUtils.getNowDate());
        return rentalTenantPreferenceMapper.insertRentalTenantPreference(rentalTenantPreference);
    }

    /**
     * 修改租户租房偏好
     * 
     * @param rentalTenantPreference 租户租房偏好
     * @return 结果
     */
    @Override
    public int updateRentalTenantPreference(RentalTenantPreference rentalTenantPreference)
    {
        rentalTenantPreference.setUpdateTime(DateUtils.getNowDate());
        return rentalTenantPreferenceMapper.updateRentalTenantPreference(rentalTenantPreference);
    }

    /**
     * 批量删除租户租房偏好
     * 
     * @param preferenceIds 需要删除的租户租房偏好主键
     * @return 结果
     */
    @Override
    public int deleteRentalTenantPreferenceByPreferenceIds(Long[] preferenceIds)
    {
        return rentalTenantPreferenceMapper.deleteRentalTenantPreferenceByPreferenceIds(preferenceIds);
    }

    /**
     * 删除租户租房偏好信息
     * 
     * @param preferenceId 租户租房偏好主键
     * @return 结果
     */
    @Override
    public int deleteRentalTenantPreferenceByPreferenceId(Long preferenceId)
    {
        return rentalTenantPreferenceMapper.deleteRentalTenantPreferenceByPreferenceId(preferenceId);
    }
}
