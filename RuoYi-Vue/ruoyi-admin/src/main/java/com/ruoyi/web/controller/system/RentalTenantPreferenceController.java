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
import com.ruoyi.system.domain.RentalTenantPreference;
import com.ruoyi.system.service.IRentalTenantPreferenceService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 租户租房偏好Controller
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/system/preference")
public class RentalTenantPreferenceController extends BaseController
{
    @Autowired
    private IRentalTenantPreferenceService rentalTenantPreferenceService;

    /**
     * 查询租户租房偏好列表
     */
    @PreAuthorize("@ss.hasPermi('system:preference:list')")
    @GetMapping("/list")
    public TableDataInfo list(RentalTenantPreference rentalTenantPreference)
    {
        startPage();
        List<RentalTenantPreference> list = rentalTenantPreferenceService.selectRentalTenantPreferenceList(rentalTenantPreference);
        return getDataTable(list);
    }

    /**
     * 导出租户租房偏好列表
     */
    @PreAuthorize("@ss.hasPermi('system:preference:export')")
    @Log(title = "租户租房偏好", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, RentalTenantPreference rentalTenantPreference)
    {
        List<RentalTenantPreference> list = rentalTenantPreferenceService.selectRentalTenantPreferenceList(rentalTenantPreference);
        ExcelUtil<RentalTenantPreference> util = new ExcelUtil<RentalTenantPreference>(RentalTenantPreference.class);
        util.exportExcel(response, list, "租户租房偏好数据");
    }

    /**
     * 获取租户租房偏好详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:preference:query')")
    @GetMapping(value = "/{preferenceId}")
    public AjaxResult getInfo(@PathVariable("preferenceId") Long preferenceId)
    {
        return success(rentalTenantPreferenceService.selectRentalTenantPreferenceByPreferenceId(preferenceId));
    }

    /**
     * 新增租户租房偏好
     */
    @PreAuthorize("@ss.hasPermi('system:preference:add')")
    @Log(title = "租户租房偏好", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody RentalTenantPreference rentalTenantPreference)
    {
        return toAjax(rentalTenantPreferenceService.insertRentalTenantPreference(rentalTenantPreference));
    }

    /**
     * 修改租户租房偏好
     */
    @PreAuthorize("@ss.hasPermi('system:preference:edit')")
    @Log(title = "租户租房偏好", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody RentalTenantPreference rentalTenantPreference)
    {
        return toAjax(rentalTenantPreferenceService.updateRentalTenantPreference(rentalTenantPreference));
    }

    /**
     * 删除租户租房偏好
     */
    @PreAuthorize("@ss.hasPermi('system:preference:remove')")
    @Log(title = "租户租房偏好", businessType = BusinessType.DELETE)
	@DeleteMapping("/{preferenceIds}")
    public AjaxResult remove(@PathVariable Long[] preferenceIds)
    {
        return toAjax(rentalTenantPreferenceService.deleteRentalTenantPreferenceByPreferenceIds(preferenceIds));
    }
}
