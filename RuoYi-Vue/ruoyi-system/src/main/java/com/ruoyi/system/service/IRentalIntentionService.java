package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.RentalIntention;

/**
 * 租赁意向Service接口
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
public interface IRentalIntentionService 
{
    /**
     * 查询租赁意向
     * 
     * @param intentionId 租赁意向主键
     * @return 租赁意向
     */
    public RentalIntention selectRentalIntentionByIntentionId(Long intentionId);

    /**
     * 查询租赁意向列表
     * 
     * @param rentalIntention 租赁意向
     * @return 租赁意向集合
     */
    public List<RentalIntention> selectRentalIntentionList(RentalIntention rentalIntention);

    /**
     * 新增租赁意向
     * 
     * @param rentalIntention 租赁意向
     * @return 结果
     */
    public int insertRentalIntention(RentalIntention rentalIntention);

    /**
     * 修改租赁意向
     * 
     * @param rentalIntention 租赁意向
     * @return 结果
     */
    public int updateRentalIntention(RentalIntention rentalIntention);

    /**
     * 批量删除租赁意向
     * 
     * @param intentionIds 需要删除的租赁意向主键集合
     * @return 结果
     */
    public int deleteRentalIntentionByIntentionIds(Long[] intentionIds);

    /**
     * 删除租赁意向信息
     * 
     * @param intentionId 租赁意向主键
     * @return 结果
     */
    public int deleteRentalIntentionByIntentionId(Long intentionId);
}
