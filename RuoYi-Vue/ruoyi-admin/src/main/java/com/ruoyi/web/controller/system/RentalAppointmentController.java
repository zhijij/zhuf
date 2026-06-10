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
import com.ruoyi.system.domain.RentalAppointment;
import com.ruoyi.system.service.IRentalAppointmentService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 看房预约Controller
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/system/appointment")
public class RentalAppointmentController extends BaseController
{
    @Autowired
    private IRentalAppointmentService rentalAppointmentService;

    /**
     * 查询看房预约列表
     */
    @PreAuthorize("@ss.hasPermi('system:appointment:list')")
    @GetMapping("/list")
    public TableDataInfo list(RentalAppointment rentalAppointment)
    {
        startPage();
        List<RentalAppointment> list = rentalAppointmentService.selectRentalAppointmentList(rentalAppointment);
        return getDataTable(list);
    }

    /**
     * 导出看房预约列表
     */
    @PreAuthorize("@ss.hasPermi('system:appointment:export')")
    @Log(title = "看房预约", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, RentalAppointment rentalAppointment)
    {
        List<RentalAppointment> list = rentalAppointmentService.selectRentalAppointmentList(rentalAppointment);
        ExcelUtil<RentalAppointment> util = new ExcelUtil<RentalAppointment>(RentalAppointment.class);
        util.exportExcel(response, list, "看房预约数据");
    }

    /**
     * 获取看房预约详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:appointment:query')")
    @GetMapping(value = "/{appointmentId}")
    public AjaxResult getInfo(@PathVariable("appointmentId") Long appointmentId)
    {
        return success(rentalAppointmentService.selectRentalAppointmentByAppointmentId(appointmentId));
    }

    /**
     * 新增看房预约
     */
    @PreAuthorize("@ss.hasPermi('system:appointment:add')")
    @Log(title = "看房预约", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody RentalAppointment rentalAppointment)
    {
        return toAjax(rentalAppointmentService.insertRentalAppointment(rentalAppointment));
    }

    /**
     * 修改看房预约
     */
    @PreAuthorize("@ss.hasPermi('system:appointment:edit')")
    @Log(title = "看房预约", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody RentalAppointment rentalAppointment)
    {
        return toAjax(rentalAppointmentService.updateRentalAppointment(rentalAppointment));
    }

    /**
     * 删除看房预约
     */
    @PreAuthorize("@ss.hasPermi('system:appointment:remove')")
    @Log(title = "看房预约", businessType = BusinessType.DELETE)
	@DeleteMapping("/{appointmentIds}")
    public AjaxResult remove(@PathVariable Long[] appointmentIds)
    {
        return toAjax(rentalAppointmentService.deleteRentalAppointmentByAppointmentIds(appointmentIds));
    }
}
