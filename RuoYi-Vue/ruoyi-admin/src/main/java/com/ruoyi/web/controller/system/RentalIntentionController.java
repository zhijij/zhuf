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
import com.ruoyi.system.domain.RentalIntention;
import com.ruoyi.system.service.IRentalIntentionService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 租赁意向Controller
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/system/intention")
public class RentalIntentionController extends BaseController
{
    @Autowired
    private IRentalIntentionService rentalIntentionService;

    /**
     * 查询租赁意向列表
     */
    @PreAuthorize("@ss.hasPermi('system:intention:list')")
    @GetMapping("/list")
    public TableDataInfo list(RentalIntention rentalIntention)
    {
        startPage();
        List<RentalIntention> list = rentalIntentionService.selectRentalIntentionList(rentalIntention);
        return getDataTable(list);
    }

    /**
     * 导出租赁意向列表
     */
    @PreAuthorize("@ss.hasPermi('system:intention:export')")
    @Log(title = "租赁意向", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, RentalIntention rentalIntention)
    {
        List<RentalIntention> list = rentalIntentionService.selectRentalIntentionList(rentalIntention);
        ExcelUtil<RentalIntention> util = new ExcelUtil<RentalIntention>(RentalIntention.class);
        util.exportExcel(response, list, "租赁意向数据");
    }

    /**
     * 获取租赁意向详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:intention:query')")
    @GetMapping(value = "/{intentionId}")
    public AjaxResult getInfo(@PathVariable("intentionId") Long intentionId)
    {
        return success(rentalIntentionService.selectRentalIntentionByIntentionId(intentionId));
    }

    /**
     * 新增租赁意向
     */
    @PreAuthorize("@ss.hasPermi('system:intention:add')")
    @Log(title = "租赁意向", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody RentalIntention rentalIntention)
    {
        return toAjax(rentalIntentionService.insertRentalIntention(rentalIntention));
    }

    /**
     * 修改租赁意向
     */
    @PreAuthorize("@ss.hasPermi('system:intention:edit')")
    @Log(title = "租赁意向", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody RentalIntention rentalIntention)
    {
        return toAjax(rentalIntentionService.updateRentalIntention(rentalIntention));
    }

    /**
     * 删除租赁意向
     */
    @PreAuthorize("@ss.hasPermi('system:intention:remove')")
    @Log(title = "租赁意向", businessType = BusinessType.DELETE)
	@DeleteMapping("/{intentionIds}")
    public AjaxResult remove(@PathVariable Long[] intentionIds)
    {
        return toAjax(rentalIntentionService.deleteRentalIntentionByIntentionIds(intentionIds));
    }
}
