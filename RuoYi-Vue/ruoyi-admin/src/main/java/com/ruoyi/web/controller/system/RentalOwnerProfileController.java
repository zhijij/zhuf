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
import com.ruoyi.system.domain.RentalOwnerProfile;
import com.ruoyi.system.service.IRentalOwnerProfileService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 户主资料Controller
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/system/profile")
public class RentalOwnerProfileController extends BaseController
{
    @Autowired
    private IRentalOwnerProfileService rentalOwnerProfileService;

    /**
     * 查询户主资料列表
     */
    @PreAuthorize("@ss.hasPermi('system:profile:list')")
    @GetMapping("/list")
    public TableDataInfo list(RentalOwnerProfile rentalOwnerProfile)
    {
        startPage();
        List<RentalOwnerProfile> list = rentalOwnerProfileService.selectRentalOwnerProfileList(rentalOwnerProfile);
        return getDataTable(list);
    }

    /**
     * 导出户主资料列表
     */
    @PreAuthorize("@ss.hasPermi('system:profile:export')")
    @Log(title = "户主资料", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, RentalOwnerProfile rentalOwnerProfile)
    {
        List<RentalOwnerProfile> list = rentalOwnerProfileService.selectRentalOwnerProfileList(rentalOwnerProfile);
        ExcelUtil<RentalOwnerProfile> util = new ExcelUtil<RentalOwnerProfile>(RentalOwnerProfile.class);
        util.exportExcel(response, list, "户主资料数据");
    }

    /**
     * 获取户主资料详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:profile:query')")
    @GetMapping(value = "/{ownerId}")
    public AjaxResult getInfo(@PathVariable("ownerId") Long ownerId)
    {
        return success(rentalOwnerProfileService.selectRentalOwnerProfileByOwnerId(ownerId));
    }

    /**
     * 新增户主资料
     */
    @PreAuthorize("@ss.hasPermi('system:profile:add')")
    @Log(title = "户主资料", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody RentalOwnerProfile rentalOwnerProfile)
    {
        return toAjax(rentalOwnerProfileService.insertRentalOwnerProfile(rentalOwnerProfile));
    }

    /**
     * 修改户主资料
     */
    @PreAuthorize("@ss.hasPermi('system:profile:edit')")
    @Log(title = "户主资料", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody RentalOwnerProfile rentalOwnerProfile)
    {
        return toAjax(rentalOwnerProfileService.updateRentalOwnerProfile(rentalOwnerProfile));
    }

    /**
     * 删除户主资料
     */
    @PreAuthorize("@ss.hasPermi('system:profile:remove')")
    @Log(title = "户主资料", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ownerIds}")
    public AjaxResult remove(@PathVariable Long[] ownerIds)
    {
        return toAjax(rentalOwnerProfileService.deleteRentalOwnerProfileByOwnerIds(ownerIds));
    }
}
