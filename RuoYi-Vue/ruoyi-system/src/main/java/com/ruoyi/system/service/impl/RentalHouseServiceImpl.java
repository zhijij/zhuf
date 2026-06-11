package com.ruoyi.system.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.system.domain.RentalContract;
import com.ruoyi.system.mapper.RentalHouseMapper;
import com.ruoyi.system.domain.RentalHouse;
import com.ruoyi.system.domain.RentalHouseEntrust;
import com.ruoyi.system.domain.dto.RentalHouseAuditRequest;
import com.ruoyi.system.domain.dto.RentalHouseCancelRequest;
import com.ruoyi.system.domain.dto.RentalHouseDealRequest;
import com.ruoyi.system.enums.RentalAuditStatus;
import com.ruoyi.system.enums.RentalEntrustStatus;
import com.ruoyi.system.enums.RentalHouseStatus;
import com.ruoyi.system.enums.RentalOperationMode;
import com.ruoyi.system.service.IRentalContractService;
import com.ruoyi.system.service.IRentalHouseEntrustService;
import com.ruoyi.system.service.IRentalHouseService;

/**
 * 租赁房源Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-06-11
 */
@Service
public class RentalHouseServiceImpl implements IRentalHouseService 
{
    @Autowired
    private RentalHouseMapper rentalHouseMapper;

    @Autowired
    private IRentalHouseEntrustService rentalHouseEntrustService;

    @Autowired
    private IRentalContractService rentalContractService;

    /**
     * 查询租赁房源
     * 
     * @param houseId 租赁房源主键
     * @return 租赁房源
     */
    @Override
    public RentalHouse selectRentalHouseByHouseId(Long houseId)
    {
        return rentalHouseMapper.selectRentalHouseByHouseId(houseId);
    }

    /**
     * 查询租赁房源列表
     * 
     * @param rentalHouse 租赁房源
     * @return 租赁房源
     */
    @Override
    public List<RentalHouse> selectRentalHouseList(RentalHouse rentalHouse)
    {
        return rentalHouseMapper.selectRentalHouseList(rentalHouse);
    }

    @Override
    public List<RentalHouse> selectPublicRentalHouseList(RentalHouse rentalHouse)
    {
        if (rentalHouse == null)
        {
            rentalHouse = new RentalHouse();
        }
        rentalHouse.setStatus(RentalHouseStatus.PUBLISHED.code());
        rentalHouse.setAuditStatus(RentalAuditStatus.PASSED.code());
        return rentalHouseMapper.selectRentalHouseList(rentalHouse).stream()
                .filter(this::isTenantVisiblePublishedHouse)
                .collect(Collectors.toList());
    }

    @Override
    public RentalHouse selectRentalHouseDetail(Long houseId, Long viewerId, boolean platformAdmin)
    {
        RentalHouse house = requireHouse(houseId);
        if (isTenantVisiblePublishedHouse(house) || platformAdmin || isOwnerOrAgent(house, viewerId)
                || hasDealContract(houseId, viewerId))
        {
            return house;
        }
        throw new ServiceException("当前房源不可见");
    }

    /**
     * 新增租赁房源
     * 
     * @param rentalHouse 租赁房源
     * @return 结果
     */
    @Override
    public int insertRentalHouse(RentalHouse rentalHouse)
    {
        rentalHouse.setCreateTime(DateUtils.getNowDate());
        return rentalHouseMapper.insertRentalHouse(rentalHouse);
    }

    @Override
    public int submitRentalHouse(RentalHouse rentalHouse, Long ownerId, String operator)
    {
        prepareNewHouse(rentalHouse, ownerId, operator);
        rentalHouse.setStatus(RentalHouseStatus.PENDING_AUDIT.code());
        rentalHouse.setAuditStatus(RentalAuditStatus.PENDING.code());
        rentalHouse.setAuditReason(null);
        return insertRentalHouse(rentalHouse);
    }

    /**
     * 修改租赁房源
     * 
     * @param rentalHouse 租赁房源
     * @return 结果
     */
    @Override
    public int updateRentalHouse(RentalHouse rentalHouse)
    {
        rentalHouse.setUpdateTime(DateUtils.getNowDate());
        return rentalHouseMapper.updateRentalHouse(rentalHouse);
    }

    @Override
    public int clearRentalHouseAgent(Long houseId)
    {
        return rentalHouseMapper.clearRentalHouseAgent(houseId);
    }

    @Override
    public int updateRentalHouseBeforeApproval(Long houseId, RentalHouse rentalHouse, Long ownerId, String operator)
    {
        RentalHouse oldHouse = requireOwnerHouse(houseId, ownerId);
        RentalHouseStatus status = RentalHouseStatus.of(oldHouse.getStatus());
        if (!status.canEditBeforeApproval())
        {
            throw new ServiceException("房源审核通过后不可直接修改，请走变更审核流程");
        }
        rentalHouse.setHouseId(houseId);
        rentalHouse.setOwnerId(oldHouse.getOwnerId());
        rentalHouse.setAgentId(oldHouse.getAgentId());
        rentalHouse.setOperationMode(oldHouse.getOperationMode());
        rentalHouse.setStatus(oldHouse.getStatus());
        rentalHouse.setAuditStatus(oldHouse.getAuditStatus());
        rentalHouse.setAuditReason(oldHouse.getAuditReason());
        rentalHouse.setAiIndexStatus(oldHouse.getAiIndexStatus());
        rentalHouse.setDelFlag(oldHouse.getDelFlag());
        rentalHouse.setUpdateBy(operator);
        return updateRentalHouse(rentalHouse);
    }

    @Override
    public int resubmitRentalHouseAudit(Long houseId, Long ownerId, String operator)
    {
        RentalHouse house = requireOwnerHouse(houseId, ownerId);
        RentalHouseStatus status = RentalHouseStatus.of(house.getStatus());
        if (!status.canSubmitAudit())
        {
            throw new ServiceException("当前房源状态不允许重新提交审核");
        }
        RentalHouse update = new RentalHouse();
        update.setHouseId(houseId);
        update.setStatus(RentalHouseStatus.PENDING_AUDIT.code());
        update.setAuditStatus(RentalAuditStatus.PENDING.code());
        update.setAuditReason(null);
        update.setUpdateBy(operator);
        return updateRentalHouse(update);
    }

    /**
     * 取消租赁房源
     *
     * @param houseId 房源ID
     * @param request 取消请求
     * @param ownerId 房源拥有者ID
     * @param operator 操作人
     * @return 结果
     */
    @Override
    public int cancelRentalHouse(Long houseId, RentalHouseCancelRequest request, Long ownerId, String operator)
    {
        RentalHouse house = requireOwnerHouse(houseId, ownerId);
        RentalHouseStatus status = RentalHouseStatus.of(house.getStatus());
        if (!status.canCancel())
        {
            throw new ServiceException("当前房源状态不允许取消或下架");
        }
        RentalHouse update = new RentalHouse();
        update.setHouseId(houseId);
        update.setStatus(RentalHouseStatus.OFF_SHELF.code());
        update.setAiIndexStatus("0");
        update.setRemark(request == null ? null : request.getCancelReason());
        update.setUpdateBy(operator);
        // TODO 对接AI向量索引任务：房源下架后创建delete任务，确保RAG不可检索。
        return updateRentalHouse(update);
    }

    /**
     * 审核租赁房源
     *
     * @param houseId 房源ID
     * @param request 审核请求
     * @param operator 操作人
     * @return 结果
     */
    @Override
    public int auditRentalHouse(Long houseId, RentalHouseAuditRequest request, String operator)
    {
        RentalHouse house = requireHouse(houseId);
        if (!RentalHouseStatus.of(house.getStatus()).canBeAudited())
        {
            throw new ServiceException("只有待审核房源可以审核");
        }
        if (request == null)
        {
            throw new ServiceException("审核请求不能为空");
        }
        RentalAuditStatus auditStatus = RentalAuditStatus.of(request.getAuditStatus());
        if (!auditStatus.isFinalAuditAction())
        {
            throw new ServiceException("审核结果只能为通过或拒绝");
        }

        RentalHouse update = new RentalHouse();
        update.setHouseId(houseId);
        update.setAuditStatus(auditStatus.code());
        update.setAuditReason(request.getAuditReason());
        update.setUpdateBy(operator);
        if (auditStatus == RentalAuditStatus.PASSED)
        {
            update.setStatus(RentalHouseStatus.PUBLISHED.code());
            update.setAiIndexStatus("0");
            // TODO 对接AI向量索引任务：审核通过后创建upsert任务，让RAG可检索最新房源。
        }
        else
        {
            update.setStatus(RentalHouseStatus.REJECTED.code());
            update.setAiIndexStatus("0");
        }
        return updateRentalHouse(update);
    }

    /**
     * 委托中介
     *
     * @param houseId 房源ID
     * @param entrust 委托信息
     * @param ownerId 房东ID
     * @param operator 操作人
     * @return 结果
     */
    @Override
    @Transactional
    public RentalHouseEntrust entrustRentalHouse(Long houseId, RentalHouseEntrust entrust, Long ownerId, String operator)
    {
        RentalHouse house = requireOwnerHouse(houseId, ownerId);
        if (!RentalHouseStatus.of(house.getStatus()).canEntrust())
        {
            throw new ServiceException("当前房源状态不允许委托中介");
        }
        if (entrust == null || entrust.getAgentId() == null)
        {
            throw new ServiceException("请选择中介用户");
        }

        entrust.setHouseId(houseId);
        entrust.setOwnerId(house.getOwnerId());
        entrust.setStatus("0");
        rentalHouseEntrustService.insertRentalHouseEntrust(entrust);

        RentalHouse update = new RentalHouse();
        update.setHouseId(houseId);
        update.setOperationMode(RentalOperationMode.AGENT_ENTRUST.code());
        update.setAgentId(entrust.getAgentId());
        update.setStatus(house.getStatus());
        update.setAuditStatus(house.getAuditStatus());
        update.setAuditReason(null);
        update.setAiIndexStatus("0");
        update.setUpdateBy(operator);
        updateRentalHouse(update);
        // TODO 对接中介通知/站内信：提示中介确认委托范围、佣金和起止时间。
        return entrust;
    }

    /**
     * 完成成交
     *
     * @param houseId 房源ID
     * @param request 成交请求
     * @param operatorId 操作人ID
     * @param operator 操作人
     * @param platformAdmin 是否平台管理员
     * @return 结果
     */
    @Override
    @Transactional
    public RentalContract completeRentalHouseDeal(Long houseId, RentalHouseDealRequest request, Long operatorId, String operator, boolean platformAdmin)
    {
        RentalHouse house = requireHouse(houseId);
        if (!RentalHouseStatus.of(house.getStatus()).canCompleteDeal())
        {
            throw new ServiceException("只有已发布房源可以成交");
        }
        if (!platformAdmin && !isOwnerOrAgent(house, operatorId))
        {
            throw new ServiceException("只有房东、中介或后台可以确认成交");
        }
        if (request == null || request.getTenantId() == null)
        {
            throw new ServiceException("成交时必须指定租户");
        }

        RentalContract contract = buildDealContract(house, request);
        rentalContractService.insertRentalContract(contract);

        RentalHouse update = new RentalHouse();
        update.setHouseId(houseId);
        update.setStatus(RentalHouseStatus.RENTED.code());
        update.setAiIndexStatus("0");
        update.setUpdateBy(operator);
        updateRentalHouse(update);
        // TODO 对接支付/交付确认：根据押金、租金支付结果将合同从待签/草稿推进到生效。
        // TODO 对接AI向量索引任务：成交后创建delete任务，主页和RAG不再展示该房源。
        return contract;
    }

    @Override
    @Transactional
    public RentalContract applyRentalHouseDeal(Long houseId, RentalHouseDealRequest request, Long tenantId, String operator)
    {
        RentalHouse house = requireHouse(houseId);
        if (!isTenantVisiblePublishedHouse(house))
        {
            throw new ServiceException("当前房源不可发起成交");
        }
        if (request == null)
        {
            request = new RentalHouseDealRequest();
        }
        request.setTenantId(tenantId);
        RentalContract contract = buildDealContract(house, request);
        contract.setStatus("1");
        contract.setCreateBy(operator);
        rentalContractService.insertRentalContract(contract);
        // TODO 支付/签约层：租户发起成交后，应进入合同确认、支付和交付流程；确认前不改变房源出租状态。
        return contract;
    }

    /**
     * 批量删除租赁房源
     * 
     * @param houseIds 需要删除的租赁房源主键
     * @return 结果
     */
    @Override
    public int deleteRentalHouseByHouseIds(Long[] houseIds)
    {
        return rentalHouseMapper.deleteRentalHouseByHouseIds(houseIds);
    }

    /**
     * 删除租赁房源信息
     * 
     * @param houseId 租赁房源主键
     * @return 结果
     */
    @Override
    public int deleteRentalHouseByHouseId(Long houseId)
    {
        return rentalHouseMapper.deleteRentalHouseByHouseId(houseId);
    }

    private void prepareNewHouse(RentalHouse rentalHouse, Long ownerId, String operator)
    {
        rentalHouse.setOwnerId(ownerId);
        rentalHouse.setCreateBy(operator);
        if (StringUtils.isEmpty(rentalHouse.getOperationMode()))
        {
            rentalHouse.setOperationMode(RentalOperationMode.OWNER_SELF.code());
        }
        if (RentalOperationMode.OWNER_SELF.code().equals(rentalHouse.getOperationMode()))
        {
            rentalHouse.setAgentId(null);
        }
        if (rentalHouse.getViewCount() == null)
        {
            rentalHouse.setViewCount(0L);
        }
        if (rentalHouse.getFavoriteCount() == null)
        {
            rentalHouse.setFavoriteCount(0L);
        }
        if (StringUtils.isEmpty(rentalHouse.getAiIndexStatus()))
        {
            rentalHouse.setAiIndexStatus("0");
        }
        if (StringUtils.isEmpty(rentalHouse.getDelFlag()))
        {
            rentalHouse.setDelFlag("0");
        }
    }

    private RentalHouse requireHouse(Long houseId)
    {
        RentalHouse house = rentalHouseMapper.selectRentalHouseByHouseId(houseId);
        if (house == null)
        {
            throw new ServiceException("房源不存在");
        }
        return house;
    }

    private RentalHouse requireOwnerHouse(Long houseId, Long ownerId)
    {
        RentalHouse house = requireHouse(houseId);
        if (ownerId == null || !ownerId.equals(house.getOwnerId()))
        {
            throw new ServiceException("只能操作自己的房源");
        }
        return house;
    }

    private boolean isOwnerOrAgent(RentalHouse house, Long userId)
    {
        return userId != null && (userId.equals(house.getOwnerId()) || userId.equals(house.getAgentId()));
    }

    private boolean hasDealContract(Long houseId, Long userId)
    {
        if (userId == null)
        {
            return false;
        }
        RentalContract query = new RentalContract();
        query.setHouseId(houseId);
        query.setTenantId(userId);
        return !rentalContractService.selectRentalContractList(query).isEmpty();
    }

    private boolean isTenantVisiblePublishedHouse(RentalHouse house)
    {
        if (!RentalHouseStatus.PUBLISHED.code().equals(house.getStatus())
                || !RentalAuditStatus.PASSED.code().equals(house.getAuditStatus()))
        {
            return false;
        }
        if (RentalOperationMode.OWNER_SELF.code().equals(house.getOperationMode()))
        {
            return true;
        }
        if (RentalOperationMode.AGENT_ENTRUST.code().equals(house.getOperationMode()))
        {
            RentalHouseEntrust query = new RentalHouseEntrust();
            query.setHouseId(house.getHouseId());
            query.setAgentId(house.getAgentId());
            query.setStatus(RentalEntrustStatus.ACTIVE.code());
            return !rentalHouseEntrustService.selectRentalHouseEntrustList(query).isEmpty();
        }
        return false;
    }

    private RentalContract buildDealContract(RentalHouse house, RentalHouseDealRequest request)
    {
        RentalContract contract = new RentalContract();
        contract.setHouseId(house.getHouseId());
        contract.setTenantId(request.getTenantId());
        contract.setOwnerId(house.getOwnerId());
        contract.setAgentId(house.getAgentId());
        contract.setContractNo(StringUtils.isEmpty(request.getContractNo()) ? defaultContractNo(house.getHouseId()) : request.getContractNo());
        contract.setStartDate(request.getStartDate());
        contract.setEndDate(request.getEndDate());
        contract.setRentAmount(request.getRentAmount() == null ? house.getRentAmount() : request.getRentAmount());
        contract.setDepositAmount(request.getDepositAmount() == null ? house.getDepositAmount() : request.getDepositAmount());
        contract.setPaymentCycle(request.getPaymentCycle());
        contract.setContractContent(request.getContractContent());
        contract.setStatus("1");
        // TODO 合同层完善：合同编号规则、电子签章、租期合法性、租金押金校验、交付验收单。
        return contract;
    }

    private String defaultContractNo(Long houseId)
    {
        return "HT" + DateUtils.dateTimeNow("yyyyMMddHHmmss") + houseId;
    }
}
