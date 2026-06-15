package com.ruoyi.web.controller.rental;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.web.service.RentalAmapService;
import com.ruoyi.system.domain.RentalHouse;
import com.ruoyi.system.service.IRentalHouseService;

@RestController
@RequestMapping("/rental/portal/map")
public class RentalAmapController extends BaseController
{
    @Autowired
    private IRentalHouseService rentalHouseService;

    @Autowired
    private RentalAmapService rentalAmapService;

    @PreAuthorize("@ss.hasAnyExactRoles('owner,agent,auditor,user,tenant')")
    @GetMapping("/geocode")
    public AjaxResult geocode(@RequestParam String address,
            @RequestParam(required = false) String city)
    {
        return AjaxResult.success(rentalAmapService.geocode(address, city));
    }

    @PreAuthorize("@ss.hasAnyExactRoles('user,tenant')")
    @GetMapping("/houses/{houseId}/context")
    public AjaxResult houseMapContext(@PathVariable Long houseId,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false, defaultValue = "transit") String mode)
    {
        RentalHouse house = rentalHouseService.selectRentalHouseDetail(houseId, currentUserIdOrNull(), false);
        Map<String, Object> context = rentalAmapService.buildHouseContext(house, destination, mode);
        return AjaxResult.success(context);
    }

    @PreAuthorize("@ss.hasAnyExactRoles('user,tenant')")
    @GetMapping("/around")
    public AjaxResult around(@RequestParam String location,
            @RequestParam(required = false) String keywords,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Integer radius,
            @RequestParam(required = false) Integer limit)
    {
        return AjaxResult.success(rentalAmapService.searchAround(location, keywords, city, radius, limit));
    }

    private Long currentUserIdOrNull()
    {
        try
        {
            return SecurityUtils.getUserId();
        }
        catch (Exception e)
        {
            return null;
        }
    }
}
