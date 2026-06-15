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
import com.ruoyi.system.domain.AiToolAuditLog;
import com.ruoyi.system.service.IAiToolAuditLogService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * AI工具调用审计Controller
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/system/log")
public class AiToolAuditLogController extends BaseController
{
    @Autowired
    private IAiToolAuditLogService aiToolAuditLogService;

    /**
     * 查询AI工具调用审计列表
     */
    @PreAuthorize("@ss.hasRole('admin') and @ss.hasPermi('system:log:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiToolAuditLog aiToolAuditLog)
    {
        startPage();
        List<AiToolAuditLog> list = aiToolAuditLogService.selectAiToolAuditLogList(aiToolAuditLog);
        return getDataTable(list);
    }

    /**
     * 导出AI工具调用审计列表
     */
    @PreAuthorize("@ss.hasRole('admin') and @ss.hasPermi('system:log:export')")
    @Log(title = "AI工具调用审计", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AiToolAuditLog aiToolAuditLog)
    {
        List<AiToolAuditLog> list = aiToolAuditLogService.selectAiToolAuditLogList(aiToolAuditLog);
        ExcelUtil<AiToolAuditLog> util = new ExcelUtil<AiToolAuditLog>(AiToolAuditLog.class);
        util.exportExcel(response, list, "AI工具调用审计数据");
    }

    /**
     * 获取AI工具调用审计详细信息
     */
    @PreAuthorize("@ss.hasRole('admin') and @ss.hasPermi('system:log:query')")
    @GetMapping(value = "/{logId}")
    public AjaxResult getInfo(@PathVariable("logId") Long logId)
    {
        return success(aiToolAuditLogService.selectAiToolAuditLogByLogId(logId));
    }

    /**
     * 新增AI工具调用审计
     */
    @PreAuthorize("@ss.hasRole('admin') and @ss.hasPermi('system:log:add')")
    @Log(title = "AI工具调用审计", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiToolAuditLog aiToolAuditLog)
    {
        return toAjax(aiToolAuditLogService.insertAiToolAuditLog(aiToolAuditLog));
    }

    /**
     * 修改AI工具调用审计
     */
    @PreAuthorize("@ss.hasRole('admin') and @ss.hasPermi('system:log:edit')")
    @Log(title = "AI工具调用审计", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiToolAuditLog aiToolAuditLog)
    {
        return toAjax(aiToolAuditLogService.updateAiToolAuditLog(aiToolAuditLog));
    }

    /**
     * 删除AI工具调用审计
     */
    @PreAuthorize("@ss.hasRole('admin') and @ss.hasPermi('system:log:remove')")
    @Log(title = "AI工具调用审计", businessType = BusinessType.DELETE)
	@DeleteMapping("/{logIds}")
    public AjaxResult remove(@PathVariable Long[] logIds)
    {
        return toAjax(aiToolAuditLogService.deleteAiToolAuditLogByLogIds(logIds));
    }
}
