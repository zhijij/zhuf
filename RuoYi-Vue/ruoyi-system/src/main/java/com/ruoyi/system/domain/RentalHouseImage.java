package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 房源图片对象 rental_house_image
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
public class RentalHouseImage extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 图片ID */
    private Long imageId;

    /** 房源ID */
    @Excel(name = "房源ID")
    private Long houseId;

    /** 图片地址 */
    @Excel(name = "图片地址")
    private String imageUrl;

    /** 类型:0普通,1封面,2户型图 */
    @Excel(name = "类型:0普通,1封面,2户型图")
    private String imageType;

    /** 排序 */
    @Excel(name = "排序")
    private Long sortNo;

    public void setImageId(Long imageId) 
    {
        this.imageId = imageId;
    }

    public Long getImageId() 
    {
        return imageId;
    }

    public void setHouseId(Long houseId) 
    {
        this.houseId = houseId;
    }

    public Long getHouseId() 
    {
        return houseId;
    }

    public void setImageUrl(String imageUrl) 
    {
        this.imageUrl = imageUrl;
    }

    public String getImageUrl() 
    {
        return imageUrl;
    }

    public void setImageType(String imageType) 
    {
        this.imageType = imageType;
    }

    public String getImageType() 
    {
        return imageType;
    }

    public void setSortNo(Long sortNo) 
    {
        this.sortNo = sortNo;
    }

    public Long getSortNo() 
    {
        return sortNo;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("imageId", getImageId())
            .append("houseId", getHouseId())
            .append("imageUrl", getImageUrl())
            .append("imageType", getImageType())
            .append("sortNo", getSortNo())
            .append("createTime", getCreateTime())
            .toString();
    }
}
