package com.ruoyi.web.controller.rental;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.RentalAppointment;
import com.ruoyi.system.domain.RentalContract;
import com.ruoyi.system.domain.RentalHouse;
import com.ruoyi.system.service.IRentalAppointmentService;
import com.ruoyi.system.service.IRentalContractService;
import com.ruoyi.system.service.IRentalHouseService;

@RestController
@RequestMapping("/rental/portal")
public class RentalPortalController extends BaseController
{
    @Autowired
    private IRentalHouseService rentalHouseService;

    @Autowired
    private IRentalAppointmentService rentalAppointmentService;

    @Autowired
    private IRentalContractService rentalContractService;

    @GetMapping("/houses")
    public AjaxResult listHouses(RentalHouse rentalHouse)
    {
        rentalHouse.setStatus("2");
        List<RentalHouse> list = rentalHouseService.selectRentalHouseList(rentalHouse);
        return AjaxResult.success(list);
    }

    @GetMapping("/houses/{houseId}")
    public AjaxResult getHouse(@PathVariable Long houseId)
    {
        return AjaxResult.success(rentalHouseService.selectRentalHouseByHouseId(houseId));
    }

    @PostMapping("/appointments")
    public AjaxResult createAppointment(@RequestBody RentalAppointment rentalAppointment)
    {
        rentalAppointment.setTenantId(SecurityUtils.getUserId());
        rentalAppointment.setSource("0");
        return toAjax(rentalAppointmentService.insertRentalAppointment(rentalAppointment));
    }

    @GetMapping("/summary")
    public AjaxResult summary()
    {
        RentalHouse houseQuery = new RentalHouse();
        houseQuery.setStatus("2");
        int houseCount = rentalHouseService.selectRentalHouseList(houseQuery).size();
        int appointmentCount = rentalAppointmentService.selectRentalAppointmentList(new RentalAppointment()).size();
        int contractCount = rentalContractService.selectRentalContractList(new RentalContract()).size();

        Map<String, Object> summary = new HashMap<>();
        summary.put("houseCount", houseCount);
        summary.put("appointmentCount", appointmentCount);
        summary.put("contractCount", contractCount);
        summary.put("avgRent", BigDecimal.ZERO);
        return AjaxResult.success(summary);
    }

    @GetMapping("/panels")
    public AjaxResult panels()
    {
        return AjaxResult.success(new Object[] {
            panel("tenant", "租户端", "找房、收藏、预约、AI 找房助手。", new String[] { "房源搜索", "房源详情", "收藏房源", "预约看房", "AI 对话" }),
            panel("owner", "户主端", "房源发布、委托中介、合同与收益查看。", new String[] { "房源管理", "委托关系", "预约查看", "合同查看", "收益分析" }),
            panel("agent", "中介端", "受托房源、客户跟进、AI 文案与匹配。", new String[] { "受托房源", "租户意向", "预约管理", "AI 文案", "客户画像" })
        });
    }

    private Map<String, Object> panel(String key, String title, String desc, String[] items)
    {
        Map<String, Object> panel = new HashMap<>();
        panel.put("key", key);
        panel.put("title", title);
        panel.put("desc", desc);
        panel.put("items", items);
        return panel;
    }
}
