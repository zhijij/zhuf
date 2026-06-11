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

    @PreAuthorize("@ss.hasAnyRoles('admin,owner')")
    @GetMapping("/houses")
    public AjaxResult listMyHouses(RentalHouse query)
    {
        if (!SecurityUtils.isAdmin())
        {
            query.setOwnerId(SecurityUtils.getUserId());
        }
        List<RentalHouse> list = rentalHouseService.selectRentalHouseList(query);
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasAnyRoles('admin,owner')")
    @Log(title = "房东提交房源", businessType = BusinessType.INSERT)
    @PostMapping("/houses")
    public AjaxResult createHouse(@RequestBody RentalHouse rentalHouse)
    {
        rentalHouseService.submitRentalHouse(rentalHouse, SecurityUtils.getUserId(), SecurityUtils.getUsername());
        return AjaxResult.success("房源已提交，等待管理员审核", rentalHouse);
    }

    @PreAuthorize("@ss.hasAnyRoles('admin,owner')")
    @Log(title = "重新提交房源审核", businessType = BusinessType.UPDATE)
    @PostMapping("/houses/{houseId}/submit-audit")
    public AjaxResult submitAudit(@PathVariable Long houseId)
    {
        assertOwnerHouse(houseId);
        return toAjax(rentalHouseService.resubmitRentalHouseAudit(houseId, SecurityUtils.getUserId(), SecurityUtils.getUsername()));
    }

    @PreAuthorize("@ss.hasAnyRoles('admin,owner')")
    @Log(title = "申请房源委托", businessType = BusinessType.INSERT)
    @PostMapping("/houses/{houseId}/entrust")
    public AjaxResult createEntrust(@PathVariable Long houseId, @RequestBody RentalHouseEntrust entrust)
    {
        entrust = rentalHouseService.entrustRentalHouse(houseId, entrust, SecurityUtils.getUserId(), SecurityUtils.getUsername());
        BizChatSession chatSession = bizChatService.openSession("entrust", entrust.getEntrustId());
        return AjaxResult.success("委托申请已提交，等待中介确认")
                .put("entrust", entrust)
                .put("chatSession", chatSession);
    }

    @PreAuthorize("@ss.hasAnyRoles('admin,owner')")
    @GetMapping("/entrusts")
    public AjaxResult listMyEntrusts(RentalHouseEntrust query)
    {
        if (!SecurityUtils.isAdmin())
        {
            query.setOwnerId(SecurityUtils.getUserId());
        }
        return AjaxResult.success(rentalHouseEntrustService.selectRentalHouseEntrustList(query));
    }

    private RentalHouse assertOwnerHouse(Long houseId)
    {
        RentalHouse house = rentalHouseService.selectRentalHouseByHouseId(houseId);
        if (house == null)
        {
            throw new ServiceException("房源不存在");
        }
        if (!SecurityUtils.isAdmin() && !SecurityUtils.getUserId().equals(house.getOwnerId()))
        {
            throw new ServiceException("只能操作自己的房源");
        }
        return house;
    }

}
