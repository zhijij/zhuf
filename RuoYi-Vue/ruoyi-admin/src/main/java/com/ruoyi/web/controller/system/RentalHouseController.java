package com.ruoyi.system.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.RentalHouse;
import com.ruoyi.system.service.IRentalHouseService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 租赁房源Controller
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/system/house")
public class RentalHouseController extends BaseController
{
    @Autowired
    private IRentalHouseService rentalHouseService;

    /**
     * 查询租赁房源列表
     */
    @PreAuthorize("@ss.hasPermi('system:house:list')")
    @GetMapping("/list")
    public TableDataInfo list(RentalHouse rentalHouse)
    {
        startPage();
        List<RentalHouse> list = rentalHouseService.selectRentalHouseList(rentalHouse);
        return getDataTable(list);
    }

    /**
     * 导出租赁房源列表
     */
    @PreAuthorize("@ss.hasPermi('system:house:export')")
    @Log(title = "租赁房源", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, RentalHouse rentalHouse)
    {
        List<RentalHouse> list = rentalHouseService.selectRentalHouseList(rentalHouse);
        ExcelUtil<RentalHouse> util = new ExcelUtil<RentalHouse>(RentalHouse.class);
        util.exportExcel(response, list, "租赁房源数据");
    }

    /**
     * 获取租赁房源详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:house:query')")
    @GetMapping(value = "/{houseId}")
    public AjaxResult getInfo(@PathVariable("houseId") Long houseId)
    {
        return success(rentalHouseService.selectRentalHouseByHouseId(houseId));
    }

    /**
     * 新增租赁房源
     */
    @PreAuthorize("@ss.hasPermi('system:house:add')")
    @Log(title = "租赁房源", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody RentalHouse rentalHouse)
    {
        return toAjax(rentalHouseService.insertRentalHouse(rentalHouse));
    }

    /**
     * 修改租赁房源
     */
    @PreAuthorize("@ss.hasPermi('system:house:edit')")
    @Log(title = "租赁房源", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody RentalHouse rentalHouse)
    {
        return toAjax(rentalHouseService.updateRentalHouse(rentalHouse));
    }

    /**
     * 删除租赁房源
     */
    @PreAuthorize("@ss.hasPermi('system:house:remove')")
    @Log(title = "租赁房源", businessType = BusinessType.DELETE)
	@DeleteMapping("/{houseIds}")
    public AjaxResult remove(@PathVariable Long[] houseIds)
    {
        return toAjax(rentalHouseService.deleteRentalHouseByHouseIds(houseIds));
    }
}
