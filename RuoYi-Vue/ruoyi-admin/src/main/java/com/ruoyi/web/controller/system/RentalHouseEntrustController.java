package com.ruoyi.web.controller.system;

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
import com.ruoyi.system.domain.RentalHouseEntrust;
import com.ruoyi.system.service.IRentalHouseEntrustService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 房源委托关系Controller
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/system/entrust")
public class RentalHouseEntrustController extends BaseController
{
    @Autowired
    private IRentalHouseEntrustService rentalHouseEntrustService;

    /**
     * 查询房源委托关系列表
     */
    @PreAuthorize("@ss.hasPermi('system:entrust:list')")
    @GetMapping("/list")
    public TableDataInfo list(RentalHouseEntrust rentalHouseEntrust)
    {
        startPage();
        List<RentalHouseEntrust> list = rentalHouseEntrustService.selectRentalHouseEntrustList(rentalHouseEntrust);
        return getDataTable(list);
    }

    /**
     * 导出房源委托关系列表
     */
    @PreAuthorize("@ss.hasPermi('system:entrust:export')")
    @Log(title = "房源委托关系", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, RentalHouseEntrust rentalHouseEntrust)
    {
        List<RentalHouseEntrust> list = rentalHouseEntrustService.selectRentalHouseEntrustList(rentalHouseEntrust);
        ExcelUtil<RentalHouseEntrust> util = new ExcelUtil<RentalHouseEntrust>(RentalHouseEntrust.class);
        util.exportExcel(response, list, "房源委托关系数据");
    }

    /**
     * 获取房源委托关系详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:entrust:query')")
    @GetMapping(value = "/{entrustId}")
    public AjaxResult getInfo(@PathVariable("entrustId") Long entrustId)
    {
        return success(rentalHouseEntrustService.selectRentalHouseEntrustByEntrustId(entrustId));
    }

    /**
     * 新增房源委托关系
     */
    @PreAuthorize("@ss.hasPermi('system:entrust:add')")
    @Log(title = "房源委托关系", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody RentalHouseEntrust rentalHouseEntrust)
    {
        return toAjax(rentalHouseEntrustService.insertRentalHouseEntrust(rentalHouseEntrust));
    }

    /**
     * 修改房源委托关系
     */
    @PreAuthorize("@ss.hasPermi('system:entrust:edit')")
    @Log(title = "房源委托关系", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody RentalHouseEntrust rentalHouseEntrust)
    {
        return toAjax(rentalHouseEntrustService.updateRentalHouseEntrust(rentalHouseEntrust));
    }

    /**
     * 删除房源委托关系
     */
    @PreAuthorize("@ss.hasPermi('system:entrust:remove')")
    @Log(title = "房源委托关系", businessType = BusinessType.DELETE)
	@DeleteMapping("/{entrustIds}")
    public AjaxResult remove(@PathVariable Long[] entrustIds)
    {
        return toAjax(rentalHouseEntrustService.deleteRentalHouseEntrustByEntrustIds(entrustIds));
    }
}
