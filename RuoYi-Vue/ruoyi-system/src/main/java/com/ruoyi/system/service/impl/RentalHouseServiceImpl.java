package com.ruoyi.system.service.impl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.system.domain.AiVectorIndexTask;
import com.ruoyi.system.domain.RentalContract;
import com.ruoyi.system.mapper.RentalHouseMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.domain.RentalHouse;
import com.ruoyi.system.domain.RentalHouseEntrust;
import com.ruoyi.system.domain.RentalHouseImage;
import com.ruoyi.system.domain.dto.RentalHouseAuditRequest;
import com.ruoyi.system.domain.dto.RentalHouseCancelRequest;
import com.ruoyi.system.domain.dto.RentalHouseDealRequest;
import com.ruoyi.system.enums.RentalAuditStatus;
import com.ruoyi.system.enums.RentalEntrustStatus;
import com.ruoyi.system.enums.RentalHouseStatus;
import com.ruoyi.system.enums.RentalOperationMode;
import com.ruoyi.system.service.IAiVectorIndexTaskService;
import com.ruoyi.system.service.IRentalContractService;
import com.ruoyi.system.service.IRentalHouseEntrustService;
import com.ruoyi.system.service.IRentalHouseImageService;
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
    private SysUserMapper sysUserMapper;

    @Autowired
    private IRentalHouseEntrustService rentalHouseEntrustService;

    @Autowired
    private IRentalContractService rentalContractService;

    @Autowired
    private IRentalHouseImageService rentalHouseImageService;

    @Autowired
    private IAiVectorIndexTaskService aiVectorIndexTaskService;

    /**
     * 查询租赁房源
     * 
     * @param houseId 租赁房源主键
     * @return 租赁房源
     */
    @Override
    public RentalHouse selectRentalHouseByHouseId(Long houseId)
    {
        RentalHouse house = rentalHouseMapper.selectRentalHouseByHouseId(houseId);
        fillHouseImages(house);
        return house;
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
        List<RentalHouse> list = rentalHouseMapper.selectRentalHouseList(rentalHouse);
        fillHouseImages(list);
        return list;
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
        List<RentalHouse> list = rentalHouseMapper.selectRentalHouseList(rentalHouse).stream()
                .filter(this::isTenantVisiblePublishedHouse)
                .collect(Collectors.toList());
        fillHouseImages(list);
        return list;
    }

    @Override
    public List<RentalHouse> selectAuditRentalHouseList(RentalHouse rentalHouse)
    {
        if (rentalHouse == null)
        {
            rentalHouse = new RentalHouse();
        }
        rentalHouse.setStatus(RentalHouseStatus.PENDING_AUDIT.code());
        rentalHouse.setAuditStatus(RentalAuditStatus.PENDING.code());
        List<RentalHouse> list = rentalHouseMapper.selectRentalHouseList(rentalHouse);
        fillHouseImages(list);
        return list;
    }

    @Override
    public RentalHouse selectRentalHouseDetail(Long houseId, Long viewerId, boolean platformAdmin)
    {
        RentalHouse house = requireHouse(houseId);
        if (isTenantVisiblePublishedHouse(house) || platformAdmin || isOwnerOrAgent(house, viewerId)
                || hasDealContract(houseId, viewerId))
        {
            fillHouseImages(house);
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
        int rows = rentalHouseMapper.insertRentalHouse(rentalHouse);
        if (rows > 0)
        {
            syncHouseImages(rentalHouse);
        }
        return rows;
    }

    @Override
    @Transactional
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
        int rows = rentalHouseMapper.updateRentalHouse(rentalHouse);
        if (rows > 0 && rentalHouse.getImageUrls() != null)
        {
            syncHouseImages(rentalHouse);
        }
        return rows;
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
    @Transactional
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
        int rows = updateRentalHouse(update);
        if (rows > 0)
        {
            enqueueHouseIndexTask(houseId, "delete", "房源已下架，等待向量库删除任务处理");
        }
        return rows;
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
    @Transactional
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
        }
        else
        {
            update.setStatus(RentalHouseStatus.REJECTED.code());
            update.setAiIndexStatus("0");
        }
        int rows = updateRentalHouse(update);
        if (rows > 0 && auditStatus == RentalAuditStatus.PASSED)
        {
            enqueueHouseIndexTask(houseId, "upsert", "房源审核通过，等待向量库同步最新房源内容");
        }
        return rows;
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
        if (!isAgentUser(entrust.getAgentId()))
        {
            throw new ServiceException("请选择有效的中介用户");
        }
        if (hasPendingOrActiveEntrust(houseId, entrust.getAgentId()))
        {
            throw new ServiceException("该中介已有待确认或生效中的委托");
        }

        entrust.setHouseId(houseId);
        entrust.setOwnerId(house.getOwnerId());
        entrust.setStatus(RentalEntrustStatus.PENDING.code());
        rentalHouseEntrustService.insertRentalHouseEntrust(entrust);
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
        enqueueHouseIndexTask(houseId, "delete", "房源已成交，等待向量库删除任务处理");
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
    @Transactional
    public int deleteRentalHouseByHouseIds(Long[] houseIds)
    {
        if (houseIds != null)
        {
            Arrays.stream(houseIds)
                    .filter(id -> id != null)
                    .forEach(rentalHouseImageService::deleteRentalHouseImageByHouseId);
        }
        return rentalHouseMapper.deleteRentalHouseByHouseIds(houseIds);
    }

    /**
     * 删除租赁房源信息
     * 
     * @param houseId 租赁房源主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteRentalHouseByHouseId(Long houseId)
    {
        rentalHouseImageService.deleteRentalHouseImageByHouseId(houseId);
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

    private void syncHouseImages(RentalHouse house)
    {
        if (house == null || house.getHouseId() == null)
        {
            return;
        }
        rentalHouseImageService.deleteRentalHouseImageByHouseId(house.getHouseId());
        if (StringUtils.isEmpty(house.getImageUrls()))
        {
            return;
        }
        List<String> urls = Arrays.stream(house.getImageUrls().split(","))
                .map(String::trim)
                .distinct()
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toList());
        for (int index = 0; index < urls.size(); index++)
        {
            RentalHouseImage image = new RentalHouseImage();
            image.setHouseId(house.getHouseId());
            image.setImageUrl(urls.get(index));
            image.setImageType(index == 0 ? "1" : "0");
            image.setSortNo((long) index);
            rentalHouseImageService.insertRentalHouseImage(image);
        }
    }

    private void fillHouseImages(List<RentalHouse> houses)
    {
        if (houses == null || houses.isEmpty())
        {
            return;
        }
        houses.forEach(this::fillHouseImages);
    }

    private void fillHouseImages(RentalHouse house)
    {
        if (house == null || house.getHouseId() == null)
        {
            return;
        }
        RentalHouseImage query = new RentalHouseImage();
        query.setHouseId(house.getHouseId());
        String imageUrls = rentalHouseImageService.selectRentalHouseImageList(query).stream()
                .map(RentalHouseImage::getImageUrl)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.joining(","));
        house.setImageUrls(imageUrls);
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
        contract.setAgentId(effectiveAgentId(house));
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

    private Long effectiveAgentId(RentalHouse house)
    {
        return house != null && RentalOperationMode.AGENT_ENTRUST.code().equals(house.getOperationMode())
                ? house.getAgentId() : null;
    }

    private boolean isAgentUser(Long agentId)
    {
        return agentId != null && sysUserMapper.selectUsersByRoleKey("agent").stream()
                .anyMatch(user -> agentId.equals(user.getUserId()));
    }

    private boolean hasPendingOrActiveEntrust(Long houseId, Long agentId)
    {
        RentalHouseEntrust query = new RentalHouseEntrust();
        query.setHouseId(houseId);
        query.setAgentId(agentId);
        return rentalHouseEntrustService.selectRentalHouseEntrustList(query).stream()
                .anyMatch(entrust -> RentalEntrustStatus.PENDING.code().equals(entrust.getStatus())
                        || RentalEntrustStatus.ACTIVE.code().equals(entrust.getStatus()));
    }

    private void enqueueHouseIndexTask(Long houseId, String action, String message)
    {
        AiVectorIndexTask task = new AiVectorIndexTask();
        task.setSourceType("house");
        task.setSourceId(houseId);
        task.setAction(action);
        task.setStatus("0");
        task.setRetryCount(0L);
        task.setErrorMsg(message);
        aiVectorIndexTaskService.insertAiVectorIndexTask(task);
    }

    private String defaultContractNo(Long houseId)
    {
        return "HT" + DateUtils.dateTimeNow("yyyyMMddHHmmss") + houseId;
    }
}
