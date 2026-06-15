package com.ruoyi.system.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 租赁房源对象 rental_house
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
public class RentalHouse extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 房源ID */
    private Long houseId;

    /** 户主用户ID */
    @Excel(name = "户主用户ID")
    private Long ownerId;

    /** 中介用户ID，自主出租时为空 */
    @Excel(name = "中介用户ID，自主出租时为空")
    private Long agentId;

    /** 运营方式:0户主自主出租,1委托中介 */
    @Excel(name = "运营方式:0户主自主出租,1委托中介")
    private String operationMode;

    /** 房源标题 */
    @Excel(name = "房源标题")
    private String title;

    /** 城市 */
    @Excel(name = "城市")
    private String city;

    /** 区域 */
    @Excel(name = "区域")
    private String district;

    /** 街道 */
    @Excel(name = "街道")
    private String street;

    /** 小区 */
    @Excel(name = "小区")
    private String community;

    /** 详细地址 */
    @Excel(name = "详细地址")
    private String address;

    /** 经度 */
    @Excel(name = "经度")
    private BigDecimal longitude;

    /** 纬度 */
    @Excel(name = "纬度")
    private BigDecimal latitude;

    /** 月租金 */
    @Excel(name = "月租金")
    private BigDecimal rentAmount;

    /** 押金 */
    @Excel(name = "押金")
    private BigDecimal depositAmount;

    /** 面积 */
    @Excel(name = "面积")
    private BigDecimal area;

    /** 室 */
    @Excel(name = "室")
    private Long roomCount;

    /** 厅 */
    @Excel(name = "厅")
    private Long hallCount;

    /** 卫 */
    @Excel(name = "卫")
    private Long toiletCount;

    /** 所在楼层 */
    @Excel(name = "所在楼层")
    private Long floorNo;

    /** 总楼层 */
    @Excel(name = "总楼层")
    private Long totalFloor;

    /** 朝向 */
    @Excel(name = "朝向")
    private String orientation;

    /** 出租方式:0整租,1合租 */
    @Excel(name = "出租方式:0整租,1合租")
    private String rentType;

    /** 装修 */
    @Excel(name = "装修")
    private String decoration;

    /** 配套设施JSON或逗号分隔 */
    @Excel(name = "配套设施JSON或逗号分隔")
    private String facilities;

    /** 标签 */
    @Excel(name = "标签")
    private String tags;

    /** 房源描述 */
    @Excel(name = "房源描述")
    private String description;

    /** 状态:0草稿,1待审核,2已发布,3驳回,4已出租,5下架 */
    @Excel(name = "状态:0草稿,1待审核,2已发布,3驳回,4已出租,5下架")
    private String status;

    /** 审核状态:0未提交,1待审,2通过,3拒绝 */
    @Excel(name = "审核状态:0未提交,1待审,2通过,3拒绝")
    private String auditStatus;

    /** 审核意见 */
    @Excel(name = "审核意见")
    private String auditReason;

    /** 浏览次数 */
    @Excel(name = "浏览次数")
    private Long viewCount;

    /** 收藏次数 */
    @Excel(name = "收藏次数")
    private Long favoriteCount;

    /** 向量索引状态:0未索引,1已索引,2失败 */
    @Excel(name = "向量索引状态:0未索引,1已索引,2失败")
    private String aiIndexStatus;

    /** 删除标志 */
    private String delFlag;

    /** 房源图片地址，多个地址用英文逗号分隔；不直接映射 rental_house 表 */
    private String imageUrls;

    public void setHouseId(Long houseId) 
    {
        this.houseId = houseId;
    }

    public Long getHouseId() 
    {
        return houseId;
    }

    public void setOwnerId(Long ownerId) 
    {
        this.ownerId = ownerId;
    }

    public Long getOwnerId() 
    {
        return ownerId;
    }

    public void setAgentId(Long agentId) 
    {
        this.agentId = agentId;
    }

    public Long getAgentId() 
    {
        return agentId;
    }

    public void setOperationMode(String operationMode) 
    {
        this.operationMode = operationMode;
    }

    public String getOperationMode() 
    {
        return operationMode;
    }

    public void setTitle(String title) 
    {
        this.title = title;
    }

    public String getTitle() 
    {
        return title;
    }

    public void setCity(String city) 
    {
        this.city = city;
    }

    public String getCity() 
    {
        return city;
    }

    public void setDistrict(String district) 
    {
        this.district = district;
    }

    public String getDistrict() 
    {
        return district;
    }

    public void setStreet(String street) 
    {
        this.street = street;
    }

    public String getStreet() 
    {
        return street;
    }

    public void setCommunity(String community) 
    {
        this.community = community;
    }

    public String getCommunity() 
    {
        return community;
    }

    public void setAddress(String address) 
    {
        this.address = address;
    }

    public String getAddress() 
    {
        return address;
    }

    public void setLongitude(BigDecimal longitude) 
    {
        this.longitude = longitude;
    }

    public BigDecimal getLongitude() 
    {
        return longitude;
    }

    public void setLatitude(BigDecimal latitude) 
    {
        this.latitude = latitude;
    }

    public BigDecimal getLatitude() 
    {
        return latitude;
    }

    public void setRentAmount(BigDecimal rentAmount) 
    {
        this.rentAmount = rentAmount;
    }

    public BigDecimal getRentAmount() 
    {
        return rentAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount) 
    {
        this.depositAmount = depositAmount;
    }

    public BigDecimal getDepositAmount() 
    {
        return depositAmount;
    }

    public void setArea(BigDecimal area) 
    {
        this.area = area;
    }

    public BigDecimal getArea() 
    {
        return area;
    }

    public void setRoomCount(Long roomCount) 
    {
        this.roomCount = roomCount;
    }

    public Long getRoomCount() 
    {
        return roomCount;
    }

    public void setHallCount(Long hallCount) 
    {
        this.hallCount = hallCount;
    }

    public Long getHallCount() 
    {
        return hallCount;
    }

    public void setToiletCount(Long toiletCount) 
    {
        this.toiletCount = toiletCount;
    }

    public Long getToiletCount() 
    {
        return toiletCount;
    }

    public void setFloorNo(Long floorNo) 
    {
        this.floorNo = floorNo;
    }

    public Long getFloorNo() 
    {
        return floorNo;
    }

    public void setTotalFloor(Long totalFloor) 
    {
        this.totalFloor = totalFloor;
    }

    public Long getTotalFloor() 
    {
        return totalFloor;
    }

    public void setOrientation(String orientation) 
    {
        this.orientation = orientation;
    }

    public String getOrientation() 
    {
        return orientation;
    }

    public void setRentType(String rentType) 
    {
        this.rentType = rentType;
    }

    public String getRentType() 
    {
        return rentType;
    }

    public void setDecoration(String decoration) 
    {
        this.decoration = decoration;
    }

    public String getDecoration() 
    {
        return decoration;
    }

    public void setFacilities(String facilities) 
    {
        this.facilities = facilities;
    }

    public String getFacilities() 
    {
        return facilities;
    }

    public void setTags(String tags) 
    {
        this.tags = tags;
    }

    public String getTags() 
    {
        return tags;
    }

    public void setDescription(String description) 
    {
        this.description = description;
    }

    public String getDescription() 
    {
        return description;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setAuditStatus(String auditStatus) 
    {
        this.auditStatus = auditStatus;
    }

    public String getAuditStatus() 
    {
        return auditStatus;
    }

    public void setAuditReason(String auditReason) 
    {
        this.auditReason = auditReason;
    }

    public String getAuditReason() 
    {
        return auditReason;
    }

    public void setViewCount(Long viewCount) 
    {
        this.viewCount = viewCount;
    }

    public Long getViewCount() 
    {
        return viewCount;
    }

    public void setFavoriteCount(Long favoriteCount) 
    {
        this.favoriteCount = favoriteCount;
    }

    public Long getFavoriteCount() 
    {
        return favoriteCount;
    }

    public void setAiIndexStatus(String aiIndexStatus) 
    {
        this.aiIndexStatus = aiIndexStatus;
    }

    public String getAiIndexStatus() 
    {
        return aiIndexStatus;
    }

    public void setDelFlag(String delFlag) 
    {
        this.delFlag = delFlag;
    }

    public String getDelFlag() 
    {
        return delFlag;
    }

    public void setImageUrls(String imageUrls)
    {
        this.imageUrls = imageUrls;
    }

    public String getImageUrls()
    {
        return imageUrls;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("houseId", getHouseId())
            .append("ownerId", getOwnerId())
            .append("agentId", getAgentId())
            .append("operationMode", getOperationMode())
            .append("title", getTitle())
            .append("city", getCity())
            .append("district", getDistrict())
            .append("street", getStreet())
            .append("community", getCommunity())
            .append("address", getAddress())
            .append("longitude", getLongitude())
            .append("latitude", getLatitude())
            .append("rentAmount", getRentAmount())
            .append("depositAmount", getDepositAmount())
            .append("area", getArea())
            .append("roomCount", getRoomCount())
            .append("hallCount", getHallCount())
            .append("toiletCount", getToiletCount())
            .append("floorNo", getFloorNo())
            .append("totalFloor", getTotalFloor())
            .append("orientation", getOrientation())
            .append("rentType", getRentType())
            .append("decoration", getDecoration())
            .append("facilities", getFacilities())
            .append("tags", getTags())
            .append("description", getDescription())
            .append("status", getStatus())
            .append("auditStatus", getAuditStatus())
            .append("auditReason", getAuditReason())
            .append("viewCount", getViewCount())
            .append("favoriteCount", getFavoriteCount())
            .append("aiIndexStatus", getAiIndexStatus())
            .append("delFlag", getDelFlag())
            .append("imageUrls", getImageUrls())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
