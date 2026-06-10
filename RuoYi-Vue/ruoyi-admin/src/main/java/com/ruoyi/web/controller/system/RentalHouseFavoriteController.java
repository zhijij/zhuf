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
import com.ruoyi.system.domain.RentalHouseFavorite;
import com.ruoyi.system.service.IRentalHouseFavoriteService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 房源收藏Controller
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/system/favorite")
public class RentalHouseFavoriteController extends BaseController
{
    @Autowired
    private IRentalHouseFavoriteService rentalHouseFavoriteService;

    /**
     * 查询房源收藏列表
     */
    @PreAuthorize("@ss.hasPermi('system:favorite:list')")
    @GetMapping("/list")
    public TableDataInfo list(RentalHouseFavorite rentalHouseFavorite)
    {
        startPage();
        List<RentalHouseFavorite> list = rentalHouseFavoriteService.selectRentalHouseFavoriteList(rentalHouseFavorite);
        return getDataTable(list);
    }

    /**
     * 导出房源收藏列表
     */
    @PreAuthorize("@ss.hasPermi('system:favorite:export')")
    @Log(title = "房源收藏", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, RentalHouseFavorite rentalHouseFavorite)
    {
        List<RentalHouseFavorite> list = rentalHouseFavoriteService.selectRentalHouseFavoriteList(rentalHouseFavorite);
        ExcelUtil<RentalHouseFavorite> util = new ExcelUtil<RentalHouseFavorite>(RentalHouseFavorite.class);
        util.exportExcel(response, list, "房源收藏数据");
    }

    /**
     * 获取房源收藏详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:favorite:query')")
    @GetMapping(value = "/{favoriteId}")
    public AjaxResult getInfo(@PathVariable("favoriteId") Long favoriteId)
    {
        return success(rentalHouseFavoriteService.selectRentalHouseFavoriteByFavoriteId(favoriteId));
    }

    /**
     * 新增房源收藏
     */
    @PreAuthorize("@ss.hasPermi('system:favorite:add')")
    @Log(title = "房源收藏", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody RentalHouseFavorite rentalHouseFavorite)
    {
        return toAjax(rentalHouseFavoriteService.insertRentalHouseFavorite(rentalHouseFavorite));
    }

    /**
     * 修改房源收藏
     */
    @PreAuthorize("@ss.hasPermi('system:favorite:edit')")
    @Log(title = "房源收藏", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody RentalHouseFavorite rentalHouseFavorite)
    {
        return toAjax(rentalHouseFavoriteService.updateRentalHouseFavorite(rentalHouseFavorite));
    }

    /**
     * 删除房源收藏
     */
    @PreAuthorize("@ss.hasPermi('system:favorite:remove')")
    @Log(title = "房源收藏", businessType = BusinessType.DELETE)
	@DeleteMapping("/{favoriteIds}")
    public AjaxResult remove(@PathVariable Long[] favoriteIds)
    {
        return toAjax(rentalHouseFavoriteService.deleteRentalHouseFavoriteByFavoriteIds(favoriteIds));
    }
}
