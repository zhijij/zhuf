package com.ruoyi.web.controller.rental;

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

    @PreAuthorize("@ss.hasAnyRoles('admin,agent')")
    @GetMapping("/entrusts")
    public AjaxResult listMyEntrusts(RentalHouseEntrust query)
    {
        if (!SecurityUtils.isAdmin())
        {
            query.setAgentId(SecurityUtils.getUserId());
        }
        return AjaxResult.success(rentalHouseEntrustService.selectRentalHouseEntrustList(query));
    }

    @PreAuthorize("@ss.hasAnyRoles('admin,agent')")
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
        return AjaxResult.success("委托已确认，房源将按合法性审核结果展示")
                .put("entrust", entrust)
                .put("chatSession", chatSession);
    }

    @PreAuthorize("@ss.hasAnyRoles('admin,agent')")
    @Log(title = "拒绝房源委托", businessType = BusinessType.UPDATE)
    @PostMapping("/entrusts/{entrustId}/reject")
    public AjaxResult rejectEntrust(@PathVariable Long entrustId)
    {
        RentalHouseEntrust entrust = assertAgentEntrust(entrustId);
        entrust.setStatus(RentalEntrustStatus.REJECTED.code());
        rentalHouseEntrustService.updateRentalHouseEntrust(entrust);

        rentalHouseService.clearRentalHouseAgent(entrust.getHouseId());
        return AjaxResult.success("委托已拒绝，房源恢复为房东自营状态");
    }

    @PreAuthorize("@ss.hasAnyRoles('admin,agent')")
    @GetMapping("/houses")
    public AjaxResult listEntrustedHouses(RentalHouse query)
    {
        if (!SecurityUtils.isAdmin())
        {
            query.setAgentId(SecurityUtils.getUserId());
        }
        query.setOperationMode(RentalOperationMode.AGENT_ENTRUST.code());
        return AjaxResult.success(rentalHouseService.selectRentalHouseList(query));
    }

    @PreAuthorize("@ss.hasAnyRoles('admin,agent')")
    @GetMapping("/houses/{houseId}")
    public AjaxResult getEntrustedHouse(@PathVariable Long houseId)
    {
        return AjaxResult.success(rentalHouseService.selectRentalHouseDetail(houseId, SecurityUtils.getUserId(), SecurityUtils.isAdmin()));
    }

    @PreAuthorize("@ss.hasAnyRoles('admin,agent')")
    @GetMapping("/appointments")
    public AjaxResult listMyAppointments(RentalAppointment query)
    {
        if (!SecurityUtils.isAdmin())
        {
            query.setAgentId(SecurityUtils.getUserId());
        }
        return AjaxResult.success(rentalAppointmentService.selectRentalAppointmentList(query));
    }

    @PreAuthorize("@ss.hasAnyRoles('admin,agent')")
    @Log(title = "确认看房预约", businessType = BusinessType.UPDATE)
    @PostMapping("/appointments/{appointmentId}/confirm")
    public AjaxResult confirmAppointment(@PathVariable Long appointmentId)
    {
        rentalAppointmentService.confirmAppointment(appointmentId, SecurityUtils.getUserId(), SecurityUtils.isAdmin());
        BizChatSession chatSession = bizChatService.openSession("appointment", appointmentId);
        return AjaxResult.success("预约已确认").put("chatSession", chatSession);
    }

    @PreAuthorize("@ss.hasAnyRoles('admin,agent')")
    @Log(title = "拒绝看房预约", businessType = BusinessType.UPDATE)
    @PostMapping("/appointments/{appointmentId}/reject")
    public AjaxResult rejectAppointment(@PathVariable Long appointmentId, @RequestBody(required = false) RentalAppointmentActionRequest request)
    {
        String reason = request == null ? null : request.getReason();
        rentalAppointmentService.rejectAppointment(appointmentId, SecurityUtils.getUserId(), SecurityUtils.isAdmin(), reason);
        BizChatSession chatSession = bizChatService.openSession("appointment", appointmentId);
        return AjaxResult.success("预约已拒绝").put("chatSession", chatSession);
    }

    @PreAuthorize("@ss.hasAnyRoles('admin,agent')")
    @Log(title = "完成看房预约", businessType = BusinessType.UPDATE)
    @PostMapping("/appointments/{appointmentId}/complete")
    public AjaxResult completeAppointment(@PathVariable Long appointmentId)
    {
        rentalAppointmentService.completeAppointment(appointmentId, SecurityUtils.getUserId(), SecurityUtils.isAdmin());
        BizChatSession chatSession = bizChatService.openSession("appointment", appointmentId);
        return AjaxResult.success("预约已完成").put("chatSession", chatSession);
    }

    @PreAuthorize("@ss.hasAnyRoles('admin,agent')")
    @GetMapping("/intentions")
    public AjaxResult listIntentions(RentalIntention query)
    {
        if (!SecurityUtils.isAdmin())
        {
            query.setAgentId(SecurityUtils.getUserId());
        }
        return AjaxResult.success(rentalIntentionService.selectRentalIntentionList(query));
    }

    @PreAuthorize("@ss.hasAnyRoles('admin,agent')")
    @Log(title = "跟进租房意向", businessType = BusinessType.UPDATE)
    @PostMapping("/intentions/{intentionId}/follow")
    public AjaxResult followIntention(@PathVariable Long intentionId, @RequestBody(required = false) RentalIntentionFollowRequest request)
    {
        String level = request == null ? null : request.getIntentionLevel();
        String note = request == null ? null : request.getNote();
        rentalIntentionService.followAgentIntention(intentionId, SecurityUtils.getUserId(), level, note);
        BizChatSession chatSession = bizChatService.openSession("intention", intentionId);
        return AjaxResult.success("意向已跟进").put("chatSession", chatSession);
    }

    @PreAuthorize("@ss.hasAnyRoles('admin,agent')")
    @Log(title = "标记无效租房意向", businessType = BusinessType.UPDATE)
    @PostMapping("/intentions/{intentionId}/invalid")
    public AjaxResult invalidIntention(@PathVariable Long intentionId, @RequestBody(required = false) RentalAppointmentActionRequest request)
    {
        String reason = request == null ? null : request.getReason();
        return toAjax(rentalIntentionService.invalidAgentIntention(intentionId, SecurityUtils.getUserId(), reason));
    }

    @PreAuthorize("@ss.hasAnyRoles('admin,agent')")
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
                intention.getHouseId(), request, SecurityUtils.getUserId(), SecurityUtils.getUsername(), SecurityUtils.isAdmin());
        rentalIntentionService.markAgentIntentionDeal(intentionId, SecurityUtils.getUserId());
        BizChatSession chatSession = bizChatService.openSession("contract", contract.getContractId());
        return AjaxResult.success("成交已确认")
                .put("contract", contract)
                .put("chatSession", chatSession);
    }

    @PreAuthorize("@ss.hasAnyRoles('admin,agent')")
    @Log(title = "确认房源成交", businessType = BusinessType.INSERT)
    @PostMapping("/houses/{houseId}/deal")
    public AjaxResult dealHouse(@PathVariable Long houseId, @RequestBody RentalHouseDealRequest request)
    {
        RentalContract contract = rentalHouseService.completeRentalHouseDeal(
                houseId, request, SecurityUtils.getUserId(), SecurityUtils.getUsername(), SecurityUtils.isAdmin());
        BizChatSession chatSession = bizChatService.openSession("contract", contract.getContractId());
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
        if (!SecurityUtils.isAdmin() && !SecurityUtils.getUserId().equals(entrust.getAgentId()))
        {
            throw new ServiceException("只能处理分配给自己的委托");
        }
        return entrust;
    }
}
