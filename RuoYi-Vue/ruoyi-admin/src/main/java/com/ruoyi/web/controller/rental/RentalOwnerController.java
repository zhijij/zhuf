package com.ruoyi.web.controller.rental;

import java.util.List;
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
import com.ruoyi.system.domain.RentalHouse;
import com.ruoyi.system.domain.RentalHouseEntrust;
import com.ruoyi.system.enums.RentalEntrustStatus;
import com.ruoyi.system.enums.RentalOperationMode;
import com.ruoyi.system.service.IBizChatService;
import com.ruoyi.system.service.IRentalHouseEntrustService;
import com.ruoyi.system.service.IRentalHouseService;

/**
 * Owner-side rental business APIs.
 */
@RestController
@RequestMapping("/rental/owner")
public class RentalOwnerController extends BaseController
{
    @Autowired
    private IRentalHouseService rentalHouseService;

    @Autowired
    private IRentalHouseEntrustService rentalHouseEntrustService;

    @Autowired
    private IBizChatService bizChatService;

    @PreAuthorize("@ss.hasAnyExactRoles('owner')")
    @GetMapping("/houses")
    public AjaxResult listMyHouses(RentalHouse query)
    {
        query.setOwnerId(SecurityUtils.getUserId());
        List<RentalHouse> list = rentalHouseService.selectRentalHouseList(query);
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasAnyExactRoles('owner')")
    @GetMapping("/houses/{houseId}")
    public AjaxResult getHouse(@PathVariable Long houseId)
    {
        assertOwnerHouse(houseId);
        return AjaxResult.success(rentalHouseService.selectRentalHouseDetail(houseId, SecurityUtils.getUserId(), false));
    }

    @PreAuthorize("@ss.hasAnyExactRoles('owner')")
    @Log(title = "房东提交房源", businessType = BusinessType.INSERT)
    @PostMapping("/houses")
    public AjaxResult createHouse(@RequestBody RentalHouse rentalHouse)
    {
        rentalHouseService.submitRentalHouse(rentalHouse, SecurityUtils.getUserId(), SecurityUtils.getUsername());
        return AjaxResult.success("房源已提交，等待管理员审核", rentalHouse);
    }

    @PreAuthorize("@ss.hasAnyExactRoles('owner')")
    @Log(title = "重新提交房源审核", businessType = BusinessType.UPDATE)
    @PostMapping("/houses/{houseId}/submit-audit")
    public AjaxResult submitAudit(@PathVariable Long houseId)
    {
        assertOwnerHouse(houseId);
        return toAjax(rentalHouseService.resubmitRentalHouseAudit(houseId, SecurityUtils.getUserId(), SecurityUtils.getUsername()));
    }

    @PreAuthorize("@ss.hasAnyExactRoles('owner')")
    @Log(title = "申请房源委托", businessType = BusinessType.INSERT)
    @PostMapping("/houses/{houseId}/entrust")
    public AjaxResult createEntrust(@PathVariable Long houseId, @RequestBody RentalHouseEntrust entrust)
    {
        entrust = rentalHouseService.entrustRentalHouse(houseId, entrust, SecurityUtils.getUserId(), SecurityUtils.getUsername());
        BizChatSession chatSession = bizChatService.openSession("entrust", entrust.getEntrustId());
        bizChatService.sendSystemMessage("entrust", entrust.getEntrustId(), "户主已发起委托邀请，请中介确认服务范围、佣金和协作方式。");
        return AjaxResult.success("委托申请已提交，等待中介确认")
                .put("entrust", entrust)
                .put("chatSession", chatSession);
    }

    @PreAuthorize("@ss.hasAnyExactRoles('owner')")
    @GetMapping("/entrusts")
    public AjaxResult listMyEntrusts(RentalHouseEntrust query)
    {
        query.setOwnerId(SecurityUtils.getUserId());
        return AjaxResult.success(rentalHouseEntrustService.selectRentalHouseEntrustList(query));
    }

    @PreAuthorize("@ss.hasAnyExactRoles('owner')")
    @GetMapping("/entrust-applications")
    public AjaxResult listEntrustApplications(RentalHouseEntrust query)
    {
        query.setOwnerId(SecurityUtils.getUserId());
        if (query.getStatus() == null)
        {
            query.setStatus(RentalEntrustStatus.PENDING.code());
        }
        return AjaxResult.success(rentalHouseEntrustService.selectRentalHouseEntrustList(query));
    }

    @PreAuthorize("@ss.hasAnyExactRoles('owner')")
    @Log(title = "户主确认中介申请", businessType = BusinessType.UPDATE)
    @PostMapping("/entrusts/{entrustId}/confirm")
    public AjaxResult confirmEntrust(@PathVariable Long entrustId)
    {
        RentalHouseEntrust entrust = assertOwnerEntrust(entrustId);
        entrust.setStatus(RentalEntrustStatus.ACTIVE.code());
        rentalHouseEntrustService.updateRentalHouseEntrust(entrust);

        RentalHouse house = new RentalHouse();
        house.setHouseId(entrust.getHouseId());
        house.setAgentId(entrust.getAgentId());
        house.setOperationMode(RentalOperationMode.AGENT_ENTRUST.code());
        house.setUpdateBy(SecurityUtils.getUsername());
        rentalHouseService.updateRentalHouse(house);

        BizChatSession chatSession = bizChatService.openSession("entrust", entrustId);
        bizChatService.sendSystemMessage("entrust", entrustId, "户主已确认中介申请，委托关系生效。");
        return AjaxResult.success("中介申请已确认")
                .put("entrust", entrust)
                .put("chatSession", chatSession);
    }

    @PreAuthorize("@ss.hasAnyExactRoles('owner')")
    @Log(title = "户主拒绝中介申请", businessType = BusinessType.UPDATE)
    @PostMapping("/entrusts/{entrustId}/reject")
    public AjaxResult rejectEntrust(@PathVariable Long entrustId)
    {
        RentalHouseEntrust entrust = assertOwnerEntrust(entrustId);
        entrust.setStatus(RentalEntrustStatus.REJECTED.code());
        rentalHouseEntrustService.updateRentalHouseEntrust(entrust);
        BizChatSession chatSession = bizChatService.openSession("entrust", entrustId);
        bizChatService.sendSystemMessage("entrust", entrustId, "户主已拒绝该中介承接申请。");
        return AjaxResult.success("中介申请已拒绝")
                .put("entrust", entrust)
                .put("chatSession", chatSession);
    }

    private RentalHouse assertOwnerHouse(Long houseId)
    {
        RentalHouse house = rentalHouseService.selectRentalHouseByHouseId(houseId);
        if (house == null)
        {
            throw new ServiceException("房源不存在");
        }
        if (!SecurityUtils.getUserId().equals(house.getOwnerId()))
        {
            throw new ServiceException("只能操作自己的房源");
        }
        return house;
    }

    private RentalHouseEntrust assertOwnerEntrust(Long entrustId)
    {
        RentalHouseEntrust entrust = rentalHouseEntrustService.selectRentalHouseEntrustByEntrustId(entrustId);
        if (entrust == null)
        {
            throw new ServiceException("委托关系不存在");
        }
        if (!SecurityUtils.getUserId().equals(entrust.getOwnerId()))
        {
            throw new ServiceException("只能由户主处理该委托关系");
        }
        return entrust;
    }

}
