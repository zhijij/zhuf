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
import com.ruoyi.system.domain.AiUserMemory;
import com.ruoyi.system.service.IAiUserMemoryService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * AI用户记忆Controller
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/system/memory")
public class AiUserMemoryController extends BaseController
{
    @Autowired
    private IAiUserMemoryService aiUserMemoryService;

    /**
     * 查询AI用户记忆列表
     */
    @PreAuthorize("@ss.hasPermi('system:memory:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiUserMemory aiUserMemory)
    {
        startPage();
        List<AiUserMemory> list = aiUserMemoryService.selectAiUserMemoryList(aiUserMemory);
        return getDataTable(list);
    }

    /**
     * 导出AI用户记忆列表
     */
    @PreAuthorize("@ss.hasPermi('system:memory:export')")
    @Log(title = "AI用户记忆", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AiUserMemory aiUserMemory)
    {
        List<AiUserMemory> list = aiUserMemoryService.selectAiUserMemoryList(aiUserMemory);
        ExcelUtil<AiUserMemory> util = new ExcelUtil<AiUserMemory>(AiUserMemory.class);
        util.exportExcel(response, list, "AI用户记忆数据");
    }

    /**
     * 获取AI用户记忆详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:memory:query')")
    @GetMapping(value = "/{memoryId}")
    public AjaxResult getInfo(@PathVariable("memoryId") Long memoryId)
    {
        return success(aiUserMemoryService.selectAiUserMemoryByMemoryId(memoryId));
    }

    /**
     * 新增AI用户记忆
     */
    @PreAuthorize("@ss.hasPermi('system:memory:add')")
    @Log(title = "AI用户记忆", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiUserMemory aiUserMemory)
    {
        return toAjax(aiUserMemoryService.insertAiUserMemory(aiUserMemory));
    }

    /**
     * 修改AI用户记忆
     */
    @PreAuthorize("@ss.hasPermi('system:memory:edit')")
    @Log(title = "AI用户记忆", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiUserMemory aiUserMemory)
    {
        return toAjax(aiUserMemoryService.updateAiUserMemory(aiUserMemory));
    }

    /**
     * 删除AI用户记忆
     */
    @PreAuthorize("@ss.hasPermi('system:memory:remove')")
    @Log(title = "AI用户记忆", businessType = BusinessType.DELETE)
	@DeleteMapping("/{memoryIds}")
    public AjaxResult remove(@PathVariable Long[] memoryIds)
    {
        return toAjax(aiUserMemoryService.deleteAiUserMemoryByMemoryIds(memoryIds));
    }
}
