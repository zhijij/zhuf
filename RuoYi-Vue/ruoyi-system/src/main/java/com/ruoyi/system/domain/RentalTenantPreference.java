package com.ruoyi.system.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 租户租房偏好对象 rental_tenant_preference
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
public class RentalTenantPreference extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 偏好ID */
    private Long preferenceId;

    /** 租户用户ID */
    @Excel(name = "租户用户ID")
    private Long userId;

    /** 意向城市 */
    @Excel(name = "意向城市")
    private String city;

    /** 意向区域 */
    @Excel(name = "意向区域")
    private String districts;

    /** 最低预算 */
    @Excel(name = "最低预算")
    private BigDecimal minRent;

    /** 最高预算 */
    @Excel(name = "最高预算")
    private BigDecimal maxRent;

    /** 户型偏好 */
    @Excel(name = "户型偏好")
    private String roomType;

    /** 通勤目标 */
    @Excel(name = "通勤目标")
    private String commuteTarget;

    /** 期望通勤分钟 */
    @Excel(name = "期望通勤分钟")
    private Long commuteMinutes;

    /** 必须标签 */
    @Excel(name = "必须标签")
    private String requiredTags;

    /** 偏好标签 */
    @Excel(name = "偏好标签")
    private String preferredTags;

    /** 避雷标签 */
    @Excel(name = "避雷标签")
    private String avoidTags;

    /** 来源:0用户填写,1AI提取 */
    @Excel(name = "来源:0用户填写,1AI提取")
    private String source;

    public void setPreferenceId(Long preferenceId) 
    {
        this.preferenceId = preferenceId;
    }

    public Long getPreferenceId() 
    {
        return preferenceId;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setCity(String city) 
    {
        this.city = city;
    }

    public String getCity() 
    {
        return city;
    }

    public void setDistricts(String districts) 
    {
        this.districts = districts;
    }

    public String getDistricts() 
    {
        return districts;
    }

    public void setMinRent(BigDecimal minRent) 
    {
        this.minRent = minRent;
    }

    public BigDecimal getMinRent() 
    {
        return minRent;
    }

    public void setMaxRent(BigDecimal maxRent) 
    {
        this.maxRent = maxRent;
    }

    public BigDecimal getMaxRent() 
    {
        return maxRent;
    }

    public void setRoomType(String roomType) 
    {
        this.roomType = roomType;
    }

    public String getRoomType() 
    {
        return roomType;
    }

    public void setCommuteTarget(String commuteTarget) 
    {
        this.commuteTarget = commuteTarget;
    }

    public String getCommuteTarget() 
    {
        return commuteTarget;
    }

    public void setCommuteMinutes(Long commuteMinutes) 
    {
        this.commuteMinutes = commuteMinutes;
    }

    public Long getCommuteMinutes() 
    {
        return commuteMinutes;
    }

    public void setRequiredTags(String requiredTags) 
    {
        this.requiredTags = requiredTags;
    }

    public String getRequiredTags() 
    {
        return requiredTags;
    }

    public void setPreferredTags(String preferredTags) 
    {
        this.preferredTags = preferredTags;
    }

    public String getPreferredTags() 
    {
        return preferredTags;
    }

    public void setAvoidTags(String avoidTags) 
    {
        this.avoidTags = avoidTags;
    }

    public String getAvoidTags() 
    {
        return avoidTags;
    }

    public void setSource(String source) 
    {
        this.source = source;
    }

    public String getSource() 
    {
        return source;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("preferenceId", getPreferenceId())
            .append("userId", getUserId())
            .append("city", getCity())
            .append("districts", getDistricts())
            .append("minRent", getMinRent())
            .append("maxRent", getMaxRent())
            .append("roomType", getRoomType())
            .append("commuteTarget", getCommuteTarget())
            .append("commuteMinutes", getCommuteMinutes())
            .append("requiredTags", getRequiredTags())
            .append("preferredTags", getPreferredTags())
            .append("avoidTags", getAvoidTags())
            .append("source", getSource())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
