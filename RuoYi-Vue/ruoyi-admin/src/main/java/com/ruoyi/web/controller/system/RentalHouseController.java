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
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.RentalHouse;
import com.ruoyi.system.domain.RentalHouseEntrust;
import com.ruoyi.system.domain.dto.RentalHouseAuditRequest;
import com.ruoyi.system.domain.dto.RentalHouseCancelRequest;
import com.ruoyi.system.domain.dto.RentalHouseDealRequest;
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

    // =========================
    // 房源业务生命周期接口
    // =========================

    /**
     * 所有角色查看公开房源列表。
     * 已出租、下架、待审核、驳回房源不会出现在主页/RAG检索列表。
     */
    @PreAuthorize("@ss.hasAnyRoles('admin,user,owner,agent')")
    @GetMapping("/public-list")
    public TableDataInfo publicList(RentalHouse rentalHouse)
    {
        startPage();
        List<RentalHouse> list = rentalHouseService.selectPublicRentalHouseList(rentalHouse);
        return getDataTable(list);
    }

    /**
     * 所有角色查看房源详情。
     * 非公开房源仅房东、中介、成交租户、后台管理员/超级管理员可见。
     */
    @PreAuthorize("@ss.hasAnyRoles('admin,user,owner,agent')")
    @GetMapping("/detail/{houseId}")
    public AjaxResult detail(@PathVariable("houseId") Long houseId)
    {
        boolean platformAdmin = SecurityUtils.isAdmin() || SecurityUtils.hasRole("admin");
        return success(rentalHouseService.selectRentalHouseDetail(houseId, getUserId(), platformAdmin));
    }

    /**
     * 用户提交房源，进入待审核状态。
     */
    @PreAuthorize("@ss.hasAnyRoles('admin,owner')")
    @Log(title = "房源提交", businessType = BusinessType.INSERT)
    @PostMapping("/submit")
    public AjaxResult submit(@RequestBody RentalHouse rentalHouse)
    {
        return toAjax(rentalHouseService.submitRentalHouse(rentalHouse, getUserId(), getUsername()));
    }

    /**
     * 用户在申请成功前修改房源信息。
     */
    @PreAuthorize("@ss.hasAnyRoles('admin,owner')")
    @Log(title = "房源审核前修改", businessType = BusinessType.UPDATE)
    @PutMapping("/{houseId}/before-approval")
    public AjaxResult editBeforeApproval(@PathVariable("houseId") Long houseId, @RequestBody RentalHouse rentalHouse)
    {
        return toAjax(rentalHouseService.updateRentalHouseBeforeApproval(houseId, rentalHouse, getUserId(), getUsername()));
    }

    /**
     * 用户修改驳回房源后再次提交审核。
     */
    @PreAuthorize("@ss.hasAnyRoles('admin,owner')")
    @Log(title = "房源重新提交审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{houseId}/resubmit")
    public AjaxResult resubmit(@PathVariable("houseId") Long houseId)
    {
        return toAjax(rentalHouseService.resubmitRentalHouseAudit(houseId, getUserId(), getUsername()));
    }

    /**
     * 用户取消申请或下架房源。
     */
    @PreAuthorize("@ss.hasAnyRoles('admin,owner')")
    @Log(title = "房源取消/下架", businessType = BusinessType.UPDATE)
    @PostMapping("/{houseId}/cancel")
    public AjaxResult cancel(@PathVariable("houseId") Long houseId, @RequestBody(required = false) RentalHouseCancelRequest request)
    {
        return toAjax(rentalHouseService.cancelRentalHouse(houseId, request, getUserId(), getUsername()));
    }

    /**
     * 非超级管理员审核房源。
     */
    @PreAuthorize("@ss.hasPermi('system:house:audit')")
    @Log(title = "房源合法性审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{houseId}/audit")
    public AjaxResult audit(@PathVariable("houseId") Long houseId, @RequestBody RentalHouseAuditRequest request)
    {
        if (SecurityUtils.isAdmin())
        {
            throw new ServiceException("超级管理员不参与业务审核，请使用普通管理员账号审核");
        }
        return toAjax(rentalHouseService.auditRentalHouse(houseId, request, getUsername()));
    }

    /**
     * 房东将房源交给中介委托代理。
     */
    @PreAuthorize("@ss.hasAnyRoles('admin,owner')")
    @Log(title = "房源委托中介", businessType = BusinessType.UPDATE)
    @PostMapping("/{houseId}/entrust")
    public AjaxResult entrust(@PathVariable("houseId") Long houseId, @RequestBody RentalHouseEntrust entrust)
    {
        return success(rentalHouseService.entrustRentalHouse(houseId, entrust, getUserId(), getUsername()));
    }

    /**
     * 租户与中介/房东完成交易，房源从公开列表和RAG检索中移除。
     */
    @PreAuthorize("@ss.hasAnyRoles('admin,owner,agent')")
    @Log(title = "房源成交", businessType = BusinessType.UPDATE)
    @PostMapping("/{houseId}/deal")
    public AjaxResult completeDeal(@PathVariable("houseId") Long houseId, @RequestBody RentalHouseDealRequest request)
    {
        boolean platformAdmin = SecurityUtils.isAdmin() || SecurityUtils.hasRole("admin");
        return success(rentalHouseService.completeRentalHouseDeal(houseId, request, getUserId(), getUsername(), platformAdmin));
    }

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
