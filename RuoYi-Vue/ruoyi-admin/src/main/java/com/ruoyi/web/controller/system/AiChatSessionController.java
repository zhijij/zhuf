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
import com.ruoyi.system.domain.AiChatSession;
import com.ruoyi.system.service.IAiChatSessionService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * AI会话Controller
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/system/session")
public class AiChatSessionController extends BaseController
{
    @Autowired
    private IAiChatSessionService aiChatSessionService;

    /**
     * 查询AI会话列表
     */
    @PreAuthorize("@ss.hasPermi('system:session:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiChatSession aiChatSession)
    {
        startPage();
        List<AiChatSession> list = aiChatSessionService.selectAiChatSessionList(aiChatSession);
        return getDataTable(list);
    }

    /**
     * 导出AI会话列表
     */
    @PreAuthorize("@ss.hasPermi('system:session:export')")
    @Log(title = "AI会话", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AiChatSession aiChatSession)
    {
        List<AiChatSession> list = aiChatSessionService.selectAiChatSessionList(aiChatSession);
        ExcelUtil<AiChatSession> util = new ExcelUtil<AiChatSession>(AiChatSession.class);
        util.exportExcel(response, list, "AI会话数据");
    }

    /**
     * 获取AI会话详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:session:query')")
    @GetMapping(value = "/{sessionId}")
    public AjaxResult getInfo(@PathVariable("sessionId") Long sessionId)
    {
        return success(aiChatSessionService.selectAiChatSessionBySessionId(sessionId));
    }

    /**
     * 新增AI会话
     */
    @PreAuthorize("@ss.hasPermi('system:session:add')")
    @Log(title = "AI会话", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiChatSession aiChatSession)
    {
        return toAjax(aiChatSessionService.insertAiChatSession(aiChatSession));
    }

    /**
     * 修改AI会话
     */
    @PreAuthorize("@ss.hasPermi('system:session:edit')")
    @Log(title = "AI会话", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiChatSession aiChatSession)
    {
        return toAjax(aiChatSessionService.updateAiChatSession(aiChatSession));
    }

    /**
     * 删除AI会话
     */
    @PreAuthorize("@ss.hasPermi('system:session:remove')")
    @Log(title = "AI会话", businessType = BusinessType.DELETE)
	@DeleteMapping("/{sessionIds}")
    public AjaxResult remove(@PathVariable Long[] sessionIds)
    {
        return toAjax(aiChatSessionService.deleteAiChatSessionBySessionIds(sessionIds));
    }
}
