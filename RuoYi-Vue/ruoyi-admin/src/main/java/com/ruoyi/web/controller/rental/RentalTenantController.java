package com.ruoyi.web.controller.rental;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.BizChatSession;
import com.ruoyi.system.domain.RentalAppointment;
import com.ruoyi.system.domain.RentalContract;
import com.ruoyi.system.domain.RentalHouse;
import com.ruoyi.system.domain.RentalHouseFavorite;
import com.ruoyi.system.domain.RentalIntention;
import com.ruoyi.system.domain.dto.RentalAppointmentActionRequest;
import com.ruoyi.system.domain.dto.RentalHouseDealRequest;
import com.ruoyi.system.service.IBizChatService;
import com.ruoyi.system.service.IRentalAppointmentService;
import com.ruoyi.system.service.IRentalContractService;
import com.ruoyi.system.service.IRentalHouseFavoriteService;
import com.ruoyi.system.service.IRentalHouseService;
import com.ruoyi.system.service.IRentalIntentionService;

/**
 * Tenant-side rental business APIs.
 */
@RestController
@RequestMapping("/rental/tenant")
public class RentalTenantController extends BaseController
{
    @Autowired
    private IRentalHouseService rentalHouseService;

    @Autowired
    private IRentalHouseFavoriteService rentalHouseFavoriteService;

    @Autowired
    private IRentalAppointmentService rentalAppointmentService;

    @Autowired
    private IRentalIntentionService rentalIntentionService;

    @Autowired
    private IRentalContractService rentalContractService;

    @Autowired
    private IBizChatService bizChatService;

    @PreAuthorize("@ss.hasAnyRoles('admin,user,tenant')")
    @GetMapping("/houses")
    public AjaxResult listHouses(RentalHouse query)
    {
        return AjaxResult.success(rentalHouseService.selectPublicRentalHouseList(query));
    }

    @PreAuthorize("@ss.hasAnyRoles('admin,user,tenant')")
    @GetMapping("/houses/{houseId}")
    public AjaxResult getHouse(@PathVariable Long houseId)
    {
        return AjaxResult.success(rentalHouseService.selectRentalHouseDetail(houseId, getUserId(), SecurityUtils.isAdmin()));
    }

    @PreAuthorize("@ss.hasAnyRoles('admin,user,tenant')")
    @GetMapping("/favorites")
    public AjaxResult listFavorites(RentalHouseFavorite query)
    {
        query.setUserId(getUserId());
        return AjaxResult.success(rentalHouseFavoriteService.selectRentalHouseFavoriteList(query));
    }

    @PreAuthorize("@ss.hasAnyRoles('admin,user,tenant')")
    @Log(title = "房源收藏", businessType = BusinessType.INSERT)
    @PostMapping("/favorites/{houseId}")
    public AjaxResult favorite(@PathVariable Long houseId)
    {
        return toAjax(rentalHouseFavoriteService.favoriteHouse(getUserId(), houseId));
    }

    @PreAuthorize("@ss.hasAnyRoles('admin,user,tenant')")
    @Log(title = "取消房源收藏", businessType = BusinessType.DELETE)
    @DeleteMapping("/favorites/{houseId}")
    public AjaxResult cancelFavorite(@PathVariable Long houseId)
    {
        return toAjax(rentalHouseFavoriteService.cancelFavoriteHouse(getUserId(), houseId));
    }

    @PreAuthorize("@ss.hasAnyRoles('admin,user,tenant')")
    @GetMapping("/favorites/{houseId}/status")
    public AjaxResult favoriteStatus(@PathVariable Long houseId)
    {
        return AjaxResult.success(rentalHouseFavoriteService.isHouseFavorited(getUserId(), houseId));
    }

    @PreAuthorize("@ss.hasAnyRoles('admin,user,tenant')")
    @GetMapping("/appointments")
    public AjaxResult listAppointments(RentalAppointment query)
    {
        query.setTenantId(getUserId());
        return AjaxResult.success(rentalAppointmentService.selectRentalAppointmentList(query));
    }

    @PreAuthorize("@ss.hasAnyRoles('admin,user,tenant')")
    @Log(title = "提交看房预约", businessType = BusinessType.INSERT)
    @PostMapping("/appointments")
    public AjaxResult createAppointment(@RequestBody RentalAppointment rentalAppointment)
    {
        rentalAppointmentService.createTenantAppointment(rentalAppointment, getUserId());
        BizChatSession chatSession = bizChatService.openSession("appointment", rentalAppointment.getAppointmentId());
        return AjaxResult.success("预约已提交，等待确认")
                .put("appointment", rentalAppointment)
                .put("chatSession", chatSession);
    }

    @PreAuthorize("@ss.hasAnyRoles('admin,user,tenant')")
    @Log(title = "取消看房预约", businessType = BusinessType.UPDATE)
    @PostMapping("/appointments/{appointmentId}/cancel")
    public AjaxResult cancelAppointment(@PathVariable Long appointmentId, @RequestBody(required = false) RentalAppointmentActionRequest request)
    {
        String reason = request == null ? null : request.getReason();
        return toAjax(rentalAppointmentService.cancelTenantAppointment(appointmentId, getUserId(), reason));
    }

    @PreAuthorize("@ss.hasAnyRoles('admin,user,tenant')")
    @GetMapping("/intentions")
    public AjaxResult listIntentions(RentalIntention query)
    {
        query.setTenantId(getUserId());
        return AjaxResult.success(rentalIntentionService.selectRentalIntentionList(query));
    }

    @PreAuthorize("@ss.hasAnyRoles('admin,user,tenant')")
    @Log(title = "提交租房意向", businessType = BusinessType.INSERT)
    @PostMapping("/intentions")
    public AjaxResult createIntention(@RequestBody RentalIntention rentalIntention)
    {
        rentalIntentionService.createTenantIntention(rentalIntention, getUserId());
        BizChatSession chatSession = bizChatService.openSession("intention", rentalIntention.getIntentionId());
        return AjaxResult.success("意向已提交")
                .put("intention", rentalIntention)
                .put("chatSession", chatSession);
    }

    @PreAuthorize("@ss.hasAnyRoles('admin,user,tenant')")
    @Log(title = "放弃租房意向", businessType = BusinessType.UPDATE)
    @PostMapping("/intentions/{intentionId}/abandon")
    public AjaxResult abandonIntention(@PathVariable Long intentionId)
    {
        return toAjax(rentalIntentionService.abandonTenantIntention(intentionId, getUserId()));
    }

    @PreAuthorize("@ss.hasAnyRoles('admin,user,tenant')")
    @Log(title = "发起房源成交", businessType = BusinessType.INSERT)
    @PostMapping("/houses/{houseId}/deal")
    public AjaxResult applyDeal(@PathVariable Long houseId, @RequestBody RentalHouseDealRequest request)
    {
        request.setTenantId(getUserId());
        RentalContract contract = rentalHouseService.applyRentalHouseDeal(houseId, request, getUserId(), getUsername());
        BizChatSession chatSession = bizChatService.openSession("contract", contract.getContractId());
        return AjaxResult.success("成交申请已提交，合同待确认")
                .put("contract", contract)
                .put("chatSession", chatSession);
    }

    @PreAuthorize("@ss.hasAnyRoles('admin,user,tenant')")
    @GetMapping("/contracts")
    public AjaxResult listContracts(RentalContract query)
    {
        query.setTenantId(getUserId());
        List<RentalContract> contracts = rentalContractService.selectRentalContractList(query);
        return AjaxResult.success(contracts);
    }
}
