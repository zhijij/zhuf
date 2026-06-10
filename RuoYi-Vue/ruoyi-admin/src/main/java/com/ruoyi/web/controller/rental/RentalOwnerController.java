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
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
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
    @PostMapping("/houses")
    public AjaxResult createHouse(@RequestBody RentalHouse rentalHouse)
    {
        rentalHouse.setOwnerId(SecurityUtils.getUserId());
        rentalHouse.setCreateBy(SecurityUtils.getUsername());
        prepareHouseDefaults(rentalHouse);

        if ("1".equals(rentalHouse.getOperationMode()))
        {
            if (rentalHouse.getAgentId() == null)
            {
                throw new ServiceException("委托中介时必须选择中介用户");
            }
            rentalHouse.setStatus("0");
            rentalHouse.setAuditStatus("0");
            rentalHouseService.insertRentalHouse(rentalHouse);

            RentalHouseEntrust entrust = buildEntrustFromHouse(rentalHouse);
            rentalHouseEntrustService.insertRentalHouseEntrust(entrust);
            BizChatSession chatSession = bizChatService.openSession("entrust", entrust.getEntrustId());
            return AjaxResult.success("委托申请已提交，等待中介确认")
                    .put("house", rentalHouse)
                    .put("entrust", entrust)
                    .put("chatSession", chatSession);
        }

        rentalHouse.setAgentId(null);
        rentalHouse.setOperationMode("0");
        rentalHouse.setStatus("2");
        rentalHouse.setAuditStatus("2");
        rentalHouseService.insertRentalHouse(rentalHouse);
        return AjaxResult.success("房源已直接进入房库", rentalHouse);
    }

    @PreAuthorize("@ss.hasAnyRoles('admin,owner')")
    @PostMapping("/houses/{houseId}/submit-audit")
    public AjaxResult submitAudit(@PathVariable Long houseId)
    {
        RentalHouse house = assertOwnerHouse(houseId);
        house.setStatus("1");
        house.setAuditStatus("1");
        house.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(rentalHouseService.updateRentalHouse(house));
    }

    @PreAuthorize("@ss.hasAnyRoles('admin,owner')")
    @PostMapping("/houses/{houseId}/entrust")
    public AjaxResult createEntrust(@PathVariable Long houseId, @RequestBody RentalHouseEntrust entrust)
    {
        RentalHouse house = assertOwnerHouse(houseId);
        if (entrust.getAgentId() == null)
        {
            throw new ServiceException("请选择中介用户");
        }

        entrust.setHouseId(houseId);
        entrust.setOwnerId(house.getOwnerId());
        entrust.setStatus("0");
        rentalHouseEntrustService.insertRentalHouseEntrust(entrust);

        RentalHouse update = new RentalHouse();
        update.setHouseId(houseId);
        update.setOperationMode("1");
        update.setAgentId(entrust.getAgentId());
        update.setStatus("0");
        update.setAuditStatus("0");
        update.setUpdateBy(SecurityUtils.getUsername());
        rentalHouseService.updateRentalHouse(update);

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

    private void prepareHouseDefaults(RentalHouse rentalHouse)
    {
        if (StringUtils.isEmpty(rentalHouse.getOperationMode()))
        {
            rentalHouse.setOperationMode("0");
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

    private RentalHouseEntrust buildEntrustFromHouse(RentalHouse house)
    {
        RentalHouseEntrust entrust = new RentalHouseEntrust();
        entrust.setHouseId(house.getHouseId());
        entrust.setOwnerId(house.getOwnerId());
        entrust.setAgentId(house.getAgentId());
        entrust.setEntrustScope("发布,预约,带看,签约");
        entrust.setStatus("0");
        return entrust;
    }
}
