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
import com.ruoyi.system.domain.AiVectorIndexTask;
import com.ruoyi.system.service.IAiVectorIndexTaskService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * AI向量索引任务Controller
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/system/task")
public class AiVectorIndexTaskController extends BaseController
{
    @Autowired
    private IAiVectorIndexTaskService aiVectorIndexTaskService;

    /**
     * 查询AI向量索引任务列表
     */
    @PreAuthorize("@ss.hasRole('admin') and @ss.hasPermi('system:task:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiVectorIndexTask aiVectorIndexTask)
    {
        startPage();
        List<AiVectorIndexTask> list = aiVectorIndexTaskService.selectAiVectorIndexTaskList(aiVectorIndexTask);
        return getDataTable(list);
    }

    /**
     * 导出AI向量索引任务列表
     */
    @PreAuthorize("@ss.hasRole('admin') and @ss.hasPermi('system:task:export')")
    @Log(title = "AI向量索引任务", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AiVectorIndexTask aiVectorIndexTask)
    {
        List<AiVectorIndexTask> list = aiVectorIndexTaskService.selectAiVectorIndexTaskList(aiVectorIndexTask);
        ExcelUtil<AiVectorIndexTask> util = new ExcelUtil<AiVectorIndexTask>(AiVectorIndexTask.class);
        util.exportExcel(response, list, "AI向量索引任务数据");
    }

    /**
     * 获取AI向量索引任务详细信息
     */
    @PreAuthorize("@ss.hasRole('admin') and @ss.hasPermi('system:task:query')")
    @GetMapping(value = "/{taskId}")
    public AjaxResult getInfo(@PathVariable("taskId") Long taskId)
    {
        return success(aiVectorIndexTaskService.selectAiVectorIndexTaskByTaskId(taskId));
    }

    /**
     * 新增AI向量索引任务
     */
    @PreAuthorize("@ss.hasRole('admin') and @ss.hasPermi('system:task:add')")
    @Log(title = "AI向量索引任务", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiVectorIndexTask aiVectorIndexTask)
    {
        return toAjax(aiVectorIndexTaskService.insertAiVectorIndexTask(aiVectorIndexTask));
    }

    /**
     * 修改AI向量索引任务
     */
    @PreAuthorize("@ss.hasRole('admin') and @ss.hasPermi('system:task:edit')")
    @Log(title = "AI向量索引任务", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiVectorIndexTask aiVectorIndexTask)
    {
        return toAjax(aiVectorIndexTaskService.updateAiVectorIndexTask(aiVectorIndexTask));
    }

    /**
     * 删除AI向量索引任务
     */
    @PreAuthorize("@ss.hasRole('admin') and @ss.hasPermi('system:task:remove')")
    @Log(title = "AI向量索引任务", businessType = BusinessType.DELETE)
	@DeleteMapping("/{taskIds}")
    public AjaxResult remove(@PathVariable Long[] taskIds)
    {
        return toAjax(aiVectorIndexTaskService.deleteAiVectorIndexTaskByTaskIds(taskIds));
    }
}
