package com.ruoyi.web.controller.rental;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.BizChatSession;
import com.ruoyi.system.domain.RentalAppointment;
import com.ruoyi.system.domain.RentalContract;
import com.ruoyi.system.domain.RentalHouse;
import com.ruoyi.system.domain.RentalHouseEntrust;
import com.ruoyi.system.domain.dto.RentalAppointmentActionRequest;
import com.ruoyi.system.domain.dto.RentalHouseDealRequest;
import com.ruoyi.system.domain.dto.RentalIntentionFollowRequest;
import com.ruoyi.system.enums.RentalEntrustStatus;
import com.ruoyi.system.enums.RentalHouseStatus;
import com.ruoyi.system.enums.RentalOperationMode;
import com.ruoyi.system.service.IBizChatService;
import com.ruoyi.system.service.IRentalAppointmentService;
import com.ruoyi.system.service.IRentalHouseEntrustService;
import com.ruoyi.system.service.IRentalHouseService;
import com.ruoyi.system.service.IRentalIntentionService;
import com.ruoyi.system.domain.RentalIntention;

/**
 * Agent-side rental business APIs.
 */
@RestController
@RequestMapping("/rental/agent")
public class RentalAgentController extends BaseController
{
    @Autowired
    private IRentalHouseEntrustService rentalHouseEntrustService;

    @Autowired
    private IRentalHouseService rentalHouseService;

    @Autowired
    private IBizChatService bizChatService;

    @Autowired
    private IRentalAppointmentService rentalAppointmentService;

    @Autowired
    private IRentalIntentionService rentalIntentionService;

    @PreAuthorize("@ss.hasAnyExactRoles('agent')")
    @GetMapping("/entrusts")
    public AjaxResult listMyEntrusts(RentalHouseEntrust query)
    {
        query.setAgentId(SecurityUtils.getUserId());
        return AjaxResult.success(rentalHouseEntrustService.selectRentalHouseEntrustList(query));
    }

    @PreAuthorize("@ss.hasAnyExactRoles('agent')")
    @Log(title = "确认房源委托", businessType = BusinessType.UPDATE)
    @PostMapping("/entrusts/{entrustId}/confirm")
    public AjaxResult confirmEntrust(@PathVariable Long entrustId)
    {
        RentalHouseEntrust entrust = assertAgentEntrust(entrustId);
        entrust.setStatus(RentalEntrustStatus.ACTIVE.code());
        rentalHouseEntrustService.updateRentalHouseEntrust(entrust);

        RentalHouse house = new RentalHouse();
        house.setHouseId(entrust.getHouseId());
        house.setAgentId(entrust.getAgentId());
        house.setOperationMode(RentalOperationMode.AGENT_ENTRUST.code());
        house.setUpdateBy(SecurityUtils.getUsername());
        rentalHouseService.updateRentalHouse(house);

        BizChatSession chatSession = bizChatService.openSession("entrust", entrustId);
        bizChatService.sendSystemMessage("entrust", entrustId, "中介已确认接受委托，后续可在此会话沟通房源运营和带看安排。");
        return AjaxResult.success("委托已确认，房源将按合法性审核结果展示")
                .put("entrust", entrust)
                .put("chatSession", chatSession);
    }

    @PreAuthorize("@ss.hasAnyExactRoles('agent')")
    @Log(title = "拒绝房源委托", businessType = BusinessType.UPDATE)
    @PostMapping("/entrusts/{entrustId}/reject")
    public AjaxResult rejectEntrust(@PathVariable Long entrustId)
    {
        RentalHouseEntrust entrust = assertAgentEntrust(entrustId);
        entrust.setStatus(RentalEntrustStatus.REJECTED.code());
        rentalHouseEntrustService.updateRentalHouseEntrust(entrust);

        BizChatSession chatSession = bizChatService.openSession("entrust", entrustId);
        bizChatService.sendSystemMessage("entrust", entrustId, "中介已拒绝该委托邀请。");
        return AjaxResult.success("委托已拒绝")
                .put("entrust", entrust)
                .put("chatSession", chatSession);
    }

    @PreAuthorize("@ss.hasAnyExactRoles('agent')")
    @GetMapping("/candidate-houses")
    public AjaxResult listCandidateHouses(RentalHouse query)
    {
        List<RentalHouse> list = rentalHouseService.selectRentalHouseList(query).stream()
                .filter(house -> RentalHouseStatus.of(house.getStatus()).canEntrust())
                .filter(house -> !SecurityUtils.getUserId().equals(house.getOwnerId()))
                .filter(house -> !hasPendingOrActiveEntrust(house.getHouseId(), SecurityUtils.getUserId()))
                .collect(Collectors.toList());
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasAnyExactRoles('agent')")
    @Log(title = "中介申请承接房源", businessType = BusinessType.INSERT)
    @PostMapping("/houses/{houseId}/apply-entrust")
    public AjaxResult applyEntrust(@PathVariable Long houseId, @RequestBody(required = false) RentalHouseEntrust entrust)
    {
        RentalHouse house = rentalHouseService.selectRentalHouseByHouseId(houseId);
        if (house == null)
        {
            throw new ServiceException("房源不存在");
        }
        if (!RentalHouseStatus.of(house.getStatus()).canEntrust())
        {
            throw new ServiceException("当前房源状态不允许申请承接");
        }

        Long agentId = SecurityUtils.getUserId();
        if (agentId.equals(house.getOwnerId()))
        {
            throw new ServiceException("户主不能申请承接自己的房源");
        }
        if (hasPendingOrActiveEntrust(houseId, agentId))
        {
            throw new ServiceException("你已存在待处理或生效中的承接申请");
        }
        if (entrust == null)
        {
            entrust = new RentalHouseEntrust();
        }

        entrust.setHouseId(houseId);
        entrust.setOwnerId(house.getOwnerId());
        entrust.setAgentId(agentId);
        entrust.setStatus(RentalEntrustStatus.PENDING.code());
        rentalHouseEntrustService.insertRentalHouseEntrust(entrust);

        BizChatSession chatSession = bizChatService.openSession("entrust", entrust.getEntrustId());
        bizChatService.sendSystemMessage("entrust", entrust.getEntrustId(), "中介已提交承接申请，请户主确认是否建立委托关系。");
        return AjaxResult.success("承接申请已提交，等待户主确认")
                .put("entrust", entrust)
                .put("chatSession", chatSession);
    }

    @PreAuthorize("@ss.hasAnyExactRoles('agent')")
    @GetMapping("/houses")
    public AjaxResult listEntrustedHouses(RentalHouse query)
    {
        query.setAgentId(SecurityUtils.getUserId());
        query.setOperationMode(RentalOperationMode.AGENT_ENTRUST.code());
        return AjaxResult.success(rentalHouseService.selectRentalHouseList(query));
    }

    @PreAuthorize("@ss.hasAnyExactRoles('agent')")
    @GetMapping("/houses/{houseId}")
    public AjaxResult getEntrustedHouse(@PathVariable Long houseId)
    {
        return AjaxResult.success(rentalHouseService.selectRentalHouseDetail(houseId, SecurityUtils.getUserId(), false));
    }

    @PreAuthorize("@ss.hasAnyExactRoles('agent')")
    @GetMapping("/appointments")
    public AjaxResult listMyAppointments(RentalAppointment query)
    {
        query.setAgentId(SecurityUtils.getUserId());
        return AjaxResult.success(rentalAppointmentService.selectRentalAppointmentList(query));
    }

    @PreAuthorize("@ss.hasAnyExactRoles('agent')")
    @Log(title = "确认看房预约", businessType = BusinessType.UPDATE)
    @PostMapping("/appointments/{appointmentId}/confirm")
    public AjaxResult confirmAppointment(@PathVariable Long appointmentId)
    {
        rentalAppointmentService.confirmAppointment(appointmentId, SecurityUtils.getUserId(), false);
        BizChatSession chatSession = bizChatService.openSession("appointment", appointmentId);
        bizChatService.sendSystemMessage("appointment", appointmentId, "预约已确认，请按约定时间完成看房。");
        return AjaxResult.success("预约已确认").put("chatSession", chatSession);
    }

    @PreAuthorize("@ss.hasAnyExactRoles('agent')")
    @Log(title = "拒绝看房预约", businessType = BusinessType.UPDATE)
    @PostMapping("/appointments/{appointmentId}/reject")
    public AjaxResult rejectAppointment(@PathVariable Long appointmentId, @RequestBody(required = false) RentalAppointmentActionRequest request)
    {
        String reason = request == null ? null : request.getReason();
        rentalAppointmentService.rejectAppointment(appointmentId, SecurityUtils.getUserId(), false, reason);
        BizChatSession chatSession = bizChatService.openSession("appointment", appointmentId);
        bizChatService.sendSystemMessage("appointment", appointmentId,
                reason == null || reason.isBlank() ? "预约已拒绝。" : "预约已拒绝，原因：" + reason);
        return AjaxResult.success("预约已拒绝").put("chatSession", chatSession);
    }

    @PreAuthorize("@ss.hasAnyExactRoles('agent')")
    @Log(title = "完成看房预约", businessType = BusinessType.UPDATE)
    @PostMapping("/appointments/{appointmentId}/complete")
    public AjaxResult completeAppointment(@PathVariable Long appointmentId)
    {
        rentalAppointmentService.completeAppointment(appointmentId, SecurityUtils.getUserId(), false);
        BizChatSession chatSession = bizChatService.openSession("appointment", appointmentId);
        bizChatService.sendSystemMessage("appointment", appointmentId, "看房已完成，请继续跟进租赁意向或合同流程。");
        return AjaxResult.success("预约已完成").put("chatSession", chatSession);
    }

    @PreAuthorize("@ss.hasAnyExactRoles('agent')")
    @GetMapping("/intentions")
    public AjaxResult listIntentions(RentalIntention query)
    {
        query.setAgentId(SecurityUtils.getUserId());
        return AjaxResult.success(rentalIntentionService.selectRentalIntentionList(query));
    }

    @PreAuthorize("@ss.hasAnyExactRoles('agent')")
    @Log(title = "跟进租房意向", businessType = BusinessType.UPDATE)
    @PostMapping("/intentions/{intentionId}/follow")
    public AjaxResult followIntention(@PathVariable Long intentionId, @RequestBody(required = false) RentalIntentionFollowRequest request)
    {
        String level = request == null ? null : request.getIntentionLevel();
        String note = request == null ? null : request.getNote();
        rentalIntentionService.followAgentIntention(intentionId, SecurityUtils.getUserId(), level, note);
        BizChatSession chatSession = bizChatService.openSession("intention", intentionId);
        bizChatService.sendSystemMessage("intention", intentionId,
                note == null || note.isBlank() ? "中介已更新租赁意向跟进状态。" : "中介已更新租赁意向跟进：" + note);
        return AjaxResult.success("意向已跟进").put("chatSession", chatSession);
    }

    @PreAuthorize("@ss.hasAnyExactRoles('agent')")
    @Log(title = "标记无效租房意向", businessType = BusinessType.UPDATE)
    @PostMapping("/intentions/{intentionId}/invalid")
    public AjaxResult invalidIntention(@PathVariable Long intentionId, @RequestBody(required = false) RentalAppointmentActionRequest request)
    {
        String reason = request == null ? null : request.getReason();
        rentalIntentionService.invalidAgentIntention(intentionId, SecurityUtils.getUserId(), reason);
        BizChatSession chatSession = bizChatService.openSession("intention", intentionId);
        bizChatService.sendSystemMessage("intention", intentionId,
                reason == null || reason.isBlank() ? "租赁意向已标记为无效。" : "租赁意向已标记为无效，原因：" + reason);
        return AjaxResult.success("意向已标记无效").put("chatSession", chatSession);
    }

    @PreAuthorize("@ss.hasAnyExactRoles('agent')")
    @Log(title = "意向转成交", businessType = BusinessType.INSERT)
    @PostMapping("/intentions/{intentionId}/deal")
    public AjaxResult dealByIntention(@PathVariable Long intentionId, @RequestBody RentalHouseDealRequest request)
    {
        RentalIntention intention = rentalIntentionService.selectRentalIntentionByIntentionId(intentionId);
        if (intention == null)
        {
            throw new ServiceException("租赁意向不存在");
        }
        request.setTenantId(intention.getTenantId());
        RentalContract contract = rentalHouseService.completeRentalHouseDeal(
                intention.getHouseId(), request, SecurityUtils.getUserId(), SecurityUtils.getUsername(), false);
        rentalIntentionService.markAgentIntentionDeal(intentionId, SecurityUtils.getUserId());
        BizChatSession chatSession = bizChatService.openSession("contract", contract.getContractId());
        bizChatService.sendSystemMessage("contract", contract.getContractId(), "意向已转为成交合同，请相关方进入合同确认流程。");
        return AjaxResult.success("成交已确认")
                .put("contract", contract)
                .put("chatSession", chatSession);
    }

    @PreAuthorize("@ss.hasAnyExactRoles('agent')")
    @Log(title = "确认房源成交", businessType = BusinessType.INSERT)
    @PostMapping("/houses/{houseId}/deal")
    public AjaxResult dealHouse(@PathVariable Long houseId, @RequestBody RentalHouseDealRequest request)
    {
        RentalContract contract = rentalHouseService.completeRentalHouseDeal(
                houseId, request, SecurityUtils.getUserId(), SecurityUtils.getUsername(), false);
        BizChatSession chatSession = bizChatService.openSession("contract", contract.getContractId());
        bizChatService.sendSystemMessage("contract", contract.getContractId(), "房源成交已确认，请相关方进入合同确认流程。");
        return AjaxResult.success("成交已确认")
                .put("contract", contract)
                .put("chatSession", chatSession);
    }

    private RentalHouseEntrust assertAgentEntrust(Long entrustId)
    {
        RentalHouseEntrust entrust = rentalHouseEntrustService.selectRentalHouseEntrustByEntrustId(entrustId);
        if (entrust == null)
        {
            throw new ServiceException("委托关系不存在");
        }
        if (!SecurityUtils.getUserId().equals(entrust.getAgentId()))
        {
            throw new ServiceException("只能处理分配给自己的委托");
        }
        return entrust;
    }

    private boolean hasPendingOrActiveEntrust(Long houseId, Long agentId)
    {
        RentalHouseEntrust query = new RentalHouseEntrust();
        query.setHouseId(houseId);
        query.setAgentId(agentId);
        return rentalHouseEntrustService.selectRentalHouseEntrustList(query).stream()
                .anyMatch(item -> RentalEntrustStatus.PENDING.code().equals(item.getStatus())
                        || RentalEntrustStatus.ACTIVE.code().equals(item.getStatus()));
    }
}
