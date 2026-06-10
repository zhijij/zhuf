package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.RentalIntentionMapper;
import com.ruoyi.system.domain.RentalIntention;
import com.ruoyi.system.service.IRentalIntentionService;

/**
 * 租赁意向Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
@Service
public class RentalIntentionServiceImpl implements IRentalIntentionService 
{
    @Autowired
    private RentalIntentionMapper rentalIntentionMapper;

    /**
     * 查询租赁意向
     * 
     * @param intentionId 租赁意向主键
     * @return 租赁意向
     */
    @Override
    public RentalIntention selectRentalIntentionByIntentionId(Long intentionId)
    {
        return rentalIntentionMapper.selectRentalIntentionByIntentionId(intentionId);
    }

    /**
     * 查询租赁意向列表
     * 
     * @param rentalIntention 租赁意向
     * @return 租赁意向
     */
    @Override
    public List<RentalIntention> selectRentalIntentionList(RentalIntention rentalIntention)
    {
        return rentalIntentionMapper.selectRentalIntentionList(rentalIntention);
    }

    /**
     * 新增租赁意向
     * 
     * @param rentalIntention 租赁意向
     * @return 结果
     */
    @Override
    public int insertRentalIntention(RentalIntention rentalIntention)
    {
        rentalIntention.setCreateTime(DateUtils.getNowDate());
        return rentalIntentionMapper.insertRentalIntention(rentalIntention);
    }

    /**
     * 修改租赁意向
     * 
     * @param rentalIntention 租赁意向
     * @return 结果
     */
    @Override
    public int updateRentalIntention(RentalIntention rentalIntention)
    {
        rentalIntention.setUpdateTime(DateUtils.getNowDate());
        return rentalIntentionMapper.updateRentalIntention(rentalIntention);
    }

    /**
     * 批量删除租赁意向
     * 
     * @param intentionIds 需要删除的租赁意向主键
     * @return 结果
     */
    @Override
    public int deleteRentalIntentionByIntentionIds(Long[] intentionIds)
    {
        return rentalIntentionMapper.deleteRentalIntentionByIntentionIds(intentionIds);
    }

    /**
     * 删除租赁意向信息
     * 
     * @param intentionId 租赁意向主键
     * @return 结果
     */
    @Override
    public int deleteRentalIntentionByIntentionId(Long intentionId)
    {
        return rentalIntentionMapper.deleteRentalIntentionByIntentionId(intentionId);
    }
}
