package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.RentalIntentionMapper;
import com.ruoyi.system.domain.RentalHouse;
import com.ruoyi.system.domain.RentalIntention;
import com.ruoyi.system.enums.RentalIntentionStatus;
import com.ruoyi.system.enums.RentalOperationMode;
import com.ruoyi.system.service.IRentalHouseService;
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

    @Autowired
    private IRentalHouseService rentalHouseService;

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

    @Override
    public int createTenantIntention(RentalIntention rentalIntention, Long tenantId)
    {
        if (rentalIntention == null || rentalIntention.getHouseId() == null)
        {
            throw new ServiceException("请选择意向房源");
        }
        RentalHouse house = rentalHouseService.selectRentalHouseDetail(rentalIntention.getHouseId(), tenantId, false);
        rentalIntention.setTenantId(tenantId);
        rentalIntention.setOwnerId(house.getOwnerId());
        rentalIntention.setAgentId(effectiveAgentId(house));
        if (StringUtils.isEmpty(rentalIntention.getIntentionLevel()))
        {
            rentalIntention.setIntentionLevel("1");
        }
        rentalIntention.setStatus(RentalIntentionStatus.FOLLOWING.code());
        return insertRentalIntention(rentalIntention);
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

    @Override
    public int abandonTenantIntention(Long intentionId, Long tenantId)
    {
        RentalIntention intention = requireIntention(intentionId);
        if (!tenantId.equals(intention.getTenantId()))
        {
            throw new ServiceException("只能放弃自己的租赁意向");
        }
        if (!RentalIntentionStatus.of(intention.getStatus()).canClose())
        {
            throw new ServiceException("当前意向状态不可放弃");
        }
        RentalIntention update = new RentalIntention();
        update.setIntentionId(intentionId);
        update.setStatus(RentalIntentionStatus.ABANDONED.code());
        return updateRentalIntention(update);
    }

    @Override
    public int followAgentIntention(Long intentionId, Long agentId, String intentionLevel, String note)
    {
        RentalIntention intention = requireAgentIntention(intentionId, agentId);
        if (!RentalIntentionStatus.of(intention.getStatus()).canClose())
        {
            throw new ServiceException("当前意向状态不可继续跟进");
        }
        RentalIntention update = new RentalIntention();
        update.setIntentionId(intentionId);
        update.setIntentionLevel(StringUtils.isEmpty(intentionLevel) ? intention.getIntentionLevel() : intentionLevel);
        update.setNote(note);
        return updateRentalIntention(update);
    }

    @Override
    public int invalidAgentIntention(Long intentionId, Long agentId, String reason)
    {
        RentalIntention intention = requireAgentIntention(intentionId, agentId);
        if (!RentalIntentionStatus.of(intention.getStatus()).canClose())
        {
            throw new ServiceException("当前意向状态不可标记无效");
        }
        RentalIntention update = new RentalIntention();
        update.setIntentionId(intentionId);
        update.setStatus(RentalIntentionStatus.INVALID.code());
        update.setNote(reason);
        return updateRentalIntention(update);
    }

    @Override
    public int markAgentIntentionDeal(Long intentionId, Long agentId)
    {
        RentalIntention intention = requireAgentIntention(intentionId, agentId);
        if (!RentalIntentionStatus.of(intention.getStatus()).canClose())
        {
            throw new ServiceException("当前意向状态不可成交");
        }
        RentalIntention update = new RentalIntention();
        update.setIntentionId(intentionId);
        update.setStatus(RentalIntentionStatus.DEAL.code());
        return updateRentalIntention(update);
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

    private RentalIntention requireIntention(Long intentionId)
    {
        RentalIntention intention = rentalIntentionMapper.selectRentalIntentionByIntentionId(intentionId);
        if (intention == null)
        {
            throw new ServiceException("租赁意向不存在");
        }
        return intention;
    }

    private Long effectiveAgentId(RentalHouse house)
    {
        return house != null && RentalOperationMode.AGENT_ENTRUST.code().equals(house.getOperationMode())
                ? house.getAgentId() : null;
    }

    private RentalIntention requireAgentIntention(Long intentionId, Long agentId)
    {
        RentalIntention intention = requireIntention(intentionId);
        if (agentId == null || !agentId.equals(intention.getAgentId()))
        {
            throw new ServiceException("只能处理自己受托房源的租赁意向");
        }
        return intention;
    }
}
