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
     * 租户创建租赁意向。
     *
     * @param rentalIntention 意向信息
     * @param tenantId 租户ID
     * @return 结果
     */
    public int createTenantIntention(RentalIntention rentalIntention, Long tenantId);

    /**
     * 修改租赁意向
     * 
     * @param rentalIntention 租赁意向
     * @return 结果
     */
    public int updateRentalIntention(RentalIntention rentalIntention);

    /**
     * 租户放弃意向。
     *
     * @param intentionId 意向ID
     * @param tenantId 租户ID
     * @return 结果
     */
    public int abandonTenantIntention(Long intentionId, Long tenantId);

    /**
     * 中介跟进意向。
     *
     * @param intentionId 意向ID
     * @param agentId 中介ID
     * @param intentionLevel 意向等级
     * @param note 跟进备注
     * @return 结果
     */
    public int followAgentIntention(Long intentionId, Long agentId, String intentionLevel, String note);

    /**
     * 中介标记意向无效。
     *
     * @param intentionId 意向ID
     * @param agentId 中介ID
     * @param reason 无效原因
     * @return 结果
     */
    public int invalidAgentIntention(Long intentionId, Long agentId, String reason);

    /**
     * 中介标记意向成交。
     *
     * @param intentionId 意向ID
     * @param agentId 中介ID
     * @return 结果
     */
    public int markAgentIntentionDeal(Long intentionId, Long agentId);

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
