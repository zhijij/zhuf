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
import com.ruoyi.system.domain.RentalHouseImage;
import com.ruoyi.system.service.IRentalHouseImageService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 房源图片Controller
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/system/image")
public class RentalHouseImageController extends BaseController
{
    @Autowired
    private IRentalHouseImageService rentalHouseImageService;

    /**
     * 查询房源图片列表
     */
    @PreAuthorize("@ss.hasPermi('system:image:list')")
    @GetMapping("/list")
    public TableDataInfo list(RentalHouseImage rentalHouseImage)
    {
        startPage();
        List<RentalHouseImage> list = rentalHouseImageService.selectRentalHouseImageList(rentalHouseImage);
        return getDataTable(list);
    }

    /**
     * 导出房源图片列表
     */
    @PreAuthorize("@ss.hasPermi('system:image:export')")
    @Log(title = "房源图片", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, RentalHouseImage rentalHouseImage)
    {
        List<RentalHouseImage> list = rentalHouseImageService.selectRentalHouseImageList(rentalHouseImage);
        ExcelUtil<RentalHouseImage> util = new ExcelUtil<RentalHouseImage>(RentalHouseImage.class);
        util.exportExcel(response, list, "房源图片数据");
    }

    /**
     * 获取房源图片详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:image:query')")
    @GetMapping(value = "/{imageId}")
    public AjaxResult getInfo(@PathVariable("imageId") Long imageId)
    {
        return success(rentalHouseImageService.selectRentalHouseImageByImageId(imageId));
    }

    /**
     * 新增房源图片
     */
    @PreAuthorize("@ss.hasPermi('system:image:add')")
    @Log(title = "房源图片", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody RentalHouseImage rentalHouseImage)
    {
        return toAjax(rentalHouseImageService.insertRentalHouseImage(rentalHouseImage));
    }

    /**
     * 修改房源图片
     */
    @PreAuthorize("@ss.hasPermi('system:image:edit')")
    @Log(title = "房源图片", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody RentalHouseImage rentalHouseImage)
    {
        return toAjax(rentalHouseImageService.updateRentalHouseImage(rentalHouseImage));
    }

    /**
     * 删除房源图片
     */
    @PreAuthorize("@ss.hasPermi('system:image:remove')")
    @Log(title = "房源图片", businessType = BusinessType.DELETE)
	@DeleteMapping("/{imageIds}")
    public AjaxResult remove(@PathVariable Long[] imageIds)
    {
        return toAjax(rentalHouseImageService.deleteRentalHouseImageByImageIds(imageIds));
    }
}
