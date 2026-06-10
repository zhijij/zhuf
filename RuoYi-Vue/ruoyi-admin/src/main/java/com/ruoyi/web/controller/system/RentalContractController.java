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
import com.ruoyi.system.domain.RentalContract;
import com.ruoyi.system.service.IRentalContractService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 租赁合同Controller
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/system/contract")
public class RentalContractController extends BaseController
{
    @Autowired
    private IRentalContractService rentalContractService;

    /**
     * 查询租赁合同列表
     */
    @PreAuthorize("@ss.hasPermi('system:contract:list')")
    @GetMapping("/list")
    public TableDataInfo list(RentalContract rentalContract)
    {
        startPage();
        List<RentalContract> list = rentalContractService.selectRentalContractList(rentalContract);
        return getDataTable(list);
    }

    /**
     * 导出租赁合同列表
     */
    @PreAuthorize("@ss.hasPermi('system:contract:export')")
    @Log(title = "租赁合同", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, RentalContract rentalContract)
    {
        List<RentalContract> list = rentalContractService.selectRentalContractList(rentalContract);
        ExcelUtil<RentalContract> util = new ExcelUtil<RentalContract>(RentalContract.class);
        util.exportExcel(response, list, "租赁合同数据");
    }

    /**
     * 获取租赁合同详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:contract:query')")
    @GetMapping(value = "/{contractId}")
    public AjaxResult getInfo(@PathVariable("contractId") Long contractId)
    {
        return success(rentalContractService.selectRentalContractByContractId(contractId));
    }

    /**
     * 新增租赁合同
     */
    @PreAuthorize("@ss.hasPermi('system:contract:add')")
    @Log(title = "租赁合同", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody RentalContract rentalContract)
    {
        return toAjax(rentalContractService.insertRentalContract(rentalContract));
    }

    /**
     * 修改租赁合同
     */
    @PreAuthorize("@ss.hasPermi('system:contract:edit')")
    @Log(title = "租赁合同", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody RentalContract rentalContract)
    {
        return toAjax(rentalContractService.updateRentalContract(rentalContract));
    }

    /**
     * 删除租赁合同
     */
    @PreAuthorize("@ss.hasPermi('system:contract:remove')")
    @Log(title = "租赁合同", businessType = BusinessType.DELETE)
	@DeleteMapping("/{contractIds}")
    public AjaxResult remove(@PathVariable Long[] contractIds)
    {
        return toAjax(rentalContractService.deleteRentalContractByContractIds(contractIds));
    }
}
