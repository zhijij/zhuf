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
import com.ruoyi.system.domain.AiChatMessage;
import com.ruoyi.system.service.IAiChatMessageService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * AI消息Controller
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/system/message")
public class AiChatMessageController extends BaseController
{
    @Autowired
    private IAiChatMessageService aiChatMessageService;

    /**
     * 查询AI消息列表
     */
    @PreAuthorize("@ss.hasRole('admin') and @ss.hasPermi('system:message:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiChatMessage aiChatMessage)
    {
        startPage();
        List<AiChatMessage> list = aiChatMessageService.selectAiChatMessageList(aiChatMessage);
        return getDataTable(list);
    }

    /**
     * 导出AI消息列表
     */
    @PreAuthorize("@ss.hasRole('admin') and @ss.hasPermi('system:message:export')")
    @Log(title = "AI消息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AiChatMessage aiChatMessage)
    {
        List<AiChatMessage> list = aiChatMessageService.selectAiChatMessageList(aiChatMessage);
        ExcelUtil<AiChatMessage> util = new ExcelUtil<AiChatMessage>(AiChatMessage.class);
        util.exportExcel(response, list, "AI消息数据");
    }

    /**
     * 获取AI消息详细信息
     */
    @PreAuthorize("@ss.hasRole('admin') and @ss.hasPermi('system:message:query')")
    @GetMapping(value = "/{messageId}")
    public AjaxResult getInfo(@PathVariable("messageId") Long messageId)
    {
        return success(aiChatMessageService.selectAiChatMessageByMessageId(messageId));
    }

    /**
     * 新增AI消息
     */
    @PreAuthorize("@ss.hasRole('admin') and @ss.hasPermi('system:message:add')")
    @Log(title = "AI消息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiChatMessage aiChatMessage)
    {
        return toAjax(aiChatMessageService.insertAiChatMessage(aiChatMessage));
    }

    /**
     * 修改AI消息
     */
    @PreAuthorize("@ss.hasRole('admin') and @ss.hasPermi('system:message:edit')")
    @Log(title = "AI消息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiChatMessage aiChatMessage)
    {
        return toAjax(aiChatMessageService.updateAiChatMessage(aiChatMessage));
    }

    /**
     * 删除AI消息
     */
    @PreAuthorize("@ss.hasRole('admin') and @ss.hasPermi('system:message:remove')")
    @Log(title = "AI消息", businessType = BusinessType.DELETE)
	@DeleteMapping("/{messageIds}")
    public AjaxResult remove(@PathVariable Long[] messageIds)
    {
        return toAjax(aiChatMessageService.deleteAiChatMessageByMessageIds(messageIds));
    }
}
