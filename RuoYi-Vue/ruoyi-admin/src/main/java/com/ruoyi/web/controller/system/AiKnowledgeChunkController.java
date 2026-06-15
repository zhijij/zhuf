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
import com.ruoyi.system.domain.AiKnowledgeChunk;
import com.ruoyi.system.service.IAiKnowledgeChunkService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * AI知识库分片Controller
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/system/chunk")
public class AiKnowledgeChunkController extends BaseController
{
    @Autowired
    private IAiKnowledgeChunkService aiKnowledgeChunkService;

    /**
     * 查询AI知识库分片列表
     */
    @PreAuthorize("@ss.hasRole('admin') and @ss.hasPermi('system:chunk:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiKnowledgeChunk aiKnowledgeChunk)
    {
        startPage();
        List<AiKnowledgeChunk> list = aiKnowledgeChunkService.selectAiKnowledgeChunkList(aiKnowledgeChunk);
        return getDataTable(list);
    }

    /**
     * 导出AI知识库分片列表
     */
    @PreAuthorize("@ss.hasRole('admin') and @ss.hasPermi('system:chunk:export')")
    @Log(title = "AI知识库分片", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AiKnowledgeChunk aiKnowledgeChunk)
    {
        List<AiKnowledgeChunk> list = aiKnowledgeChunkService.selectAiKnowledgeChunkList(aiKnowledgeChunk);
        ExcelUtil<AiKnowledgeChunk> util = new ExcelUtil<AiKnowledgeChunk>(AiKnowledgeChunk.class);
        util.exportExcel(response, list, "AI知识库分片数据");
    }

    /**
     * 获取AI知识库分片详细信息
     */
    @PreAuthorize("@ss.hasRole('admin') and @ss.hasPermi('system:chunk:query')")
    @GetMapping(value = "/{chunkId}")
    public AjaxResult getInfo(@PathVariable("chunkId") Long chunkId)
    {
        return success(aiKnowledgeChunkService.selectAiKnowledgeChunkByChunkId(chunkId));
    }

    /**
     * 新增AI知识库分片
     */
    @PreAuthorize("@ss.hasRole('admin') and @ss.hasPermi('system:chunk:add')")
    @Log(title = "AI知识库分片", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiKnowledgeChunk aiKnowledgeChunk)
    {
        return toAjax(aiKnowledgeChunkService.insertAiKnowledgeChunk(aiKnowledgeChunk));
    }

    /**
     * 修改AI知识库分片
     */
    @PreAuthorize("@ss.hasRole('admin') and @ss.hasPermi('system:chunk:edit')")
    @Log(title = "AI知识库分片", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiKnowledgeChunk aiKnowledgeChunk)
    {
        return toAjax(aiKnowledgeChunkService.updateAiKnowledgeChunk(aiKnowledgeChunk));
    }

    /**
     * 删除AI知识库分片
     */
    @PreAuthorize("@ss.hasRole('admin') and @ss.hasPermi('system:chunk:remove')")
    @Log(title = "AI知识库分片", businessType = BusinessType.DELETE)
	@DeleteMapping("/{chunkIds}")
    public AjaxResult remove(@PathVariable Long[] chunkIds)
    {
        return toAjax(aiKnowledgeChunkService.deleteAiKnowledgeChunkByChunkIds(chunkIds));
    }
}
