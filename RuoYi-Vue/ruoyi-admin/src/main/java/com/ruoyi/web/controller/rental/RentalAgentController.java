package com.ruoyi.web.controller.rental;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.BizChatSession;
import com.ruoyi.system.domain.RentalHouse;
import com.ruoyi.system.domain.RentalHouseEntrust;
import com.ruoyi.system.service.IBizChatService;
import com.ruoyi.system.service.IRentalHouseEntrustService;
import com.ruoyi.system.service.IRentalHouseService;

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
    @PostMapping("/entrusts/{entrustId}/confirm")
    public AjaxResult confirmEntrust(@PathVariable Long entrustId)
    {
        RentalHouseEntrust entrust = assertAgentEntrust(entrustId);
        entrust.setStatus("1");
        rentalHouseEntrustService.updateRentalHouseEntrust(entrust);

        RentalHouse house = new RentalHouse();
        house.setHouseId(entrust.getHouseId());
        house.setAgentId(entrust.getAgentId());
        house.setOperationMode("1");
        house.setStatus("2");
        house.setAuditStatus("2");
        house.setUpdateBy(SecurityUtils.getUsername());
        rentalHouseService.updateRentalHouse(house);

        BizChatSession chatSession = bizChatService.openSession("entrust", entrustId);
        return AjaxResult.success("委托已确认，房源已进入房库")
                .put("entrust", entrust)
                .put("chatSession", chatSession);
    }

    @PreAuthorize("@ss.hasAnyRoles('admin,agent')")
    @PostMapping("/entrusts/{entrustId}/reject")
    public AjaxResult rejectEntrust(@PathVariable Long entrustId)
    {
        RentalHouseEntrust entrust = assertAgentEntrust(entrustId);
        entrust.setStatus("2");
        rentalHouseEntrustService.updateRentalHouseEntrust(entrust);

        RentalHouse house = new RentalHouse();
        house.setHouseId(entrust.getHouseId());
        house.setStatus("0");
        house.setAuditStatus("0");
        house.setUpdateBy(SecurityUtils.getUsername());
        rentalHouseService.updateRentalHouse(house);
        return AjaxResult.success("委托已拒绝，房源未进入房库");
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
