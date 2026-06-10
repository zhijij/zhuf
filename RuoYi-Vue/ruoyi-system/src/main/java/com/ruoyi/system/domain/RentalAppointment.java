package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 看房预约对象 rental_appointment
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
public class RentalAppointment extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 预约ID */
    private Long appointmentId;

    /** 房源ID */
    @Excel(name = "房源ID")
    private Long houseId;

    /** 租户用户ID */
    @Excel(name = "租户用户ID")
    private Long tenantId;

    /** 户主用户ID */
    @Excel(name = "户主用户ID")
    private Long ownerId;

    /** 中介用户ID，自主出租时为空 */
    @Excel(name = "中介用户ID，自主出租时为空")
    private Long agentId;

    /** 预约时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "预约时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date appointmentTime;

    /** 联系人 */
    @Excel(name = "联系人")
    private String contactName;

    /** 联系电话 */
    @Excel(name = "联系电话")
    private String contactPhone;

    /** 留言 */
    @Excel(name = "留言")
    private String message;

    /** 状态:0待确认,1已确认,2已完成,3已取消,4已拒绝 */
    @Excel(name = "状态:0待确认,1已确认,2已完成,3已取消,4已拒绝")
    private String status;

    /** 取消/拒绝原因 */
    @Excel(name = "取消/拒绝原因")
    private String cancelReason;

    /** 来源:0手动,1AI助手 */
    @Excel(name = "来源:0手动,1AI助手")
    private String source;

    public void setAppointmentId(Long appointmentId) 
    {
        this.appointmentId = appointmentId;
    }

    public Long getAppointmentId() 
    {
        return appointmentId;
    }

    public void setHouseId(Long houseId) 
    {
        this.houseId = houseId;
    }

    public Long getHouseId() 
    {
        return houseId;
    }

    public void setTenantId(Long tenantId) 
    {
        this.tenantId = tenantId;
    }

    public Long getTenantId() 
    {
        return tenantId;
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

    public void setAppointmentTime(Date appointmentTime) 
    {
        this.appointmentTime = appointmentTime;
    }

    public Date getAppointmentTime() 
    {
        return appointmentTime;
    }

    public void setContactName(String contactName) 
    {
        this.contactName = contactName;
    }

    public String getContactName() 
    {
        return contactName;
    }

    public void setContactPhone(String contactPhone) 
    {
        this.contactPhone = contactPhone;
    }

    public String getContactPhone() 
    {
        return contactPhone;
    }

    public void setMessage(String message) 
    {
        this.message = message;
    }

    public String getMessage() 
    {
        return message;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setCancelReason(String cancelReason) 
    {
        this.cancelReason = cancelReason;
    }

    public String getCancelReason() 
    {
        return cancelReason;
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
            .append("appointmentId", getAppointmentId())
            .append("houseId", getHouseId())
            .append("tenantId", getTenantId())
            .append("ownerId", getOwnerId())
            .append("agentId", getAgentId())
            .append("appointmentTime", getAppointmentTime())
            .append("contactName", getContactName())
            .append("contactPhone", getContactPhone())
            .append("message", getMessage())
            .append("status", getStatus())
            .append("cancelReason", getCancelReason())
            .append("source", getSource())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
