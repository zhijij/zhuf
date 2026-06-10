package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 户主资料对象 rental_owner_profile
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
public class RentalOwnerProfile extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 户主用户ID，对应sys_user.user_id */
    private Long ownerId;

    /** 真实姓名 */
    @Excel(name = "真实姓名")
    private String realName;

    /** 证件号，可加密存储 */
    @Excel(name = "证件号，可加密存储")
    private String idCardNo;

    /** 联系电话 */
    @Excel(name = "联系电话")
    private String contactPhone;

    /** 收款账户，可加密存储 */
    @Excel(name = "收款账户，可加密存储")
    private String bankAccount;

    /** 认证状态:0未认证,1待审核,2已认证,3拒绝 */
    @Excel(name = "认证状态:0未认证,1待审核,2已认证,3拒绝")
    private String verifyStatus;

    /** 审核意见 */
    @Excel(name = "审核意见")
    private String verifyReason;

    public void setOwnerId(Long ownerId) 
    {
        this.ownerId = ownerId;
    }

    public Long getOwnerId() 
    {
        return ownerId;
    }

    public void setRealName(String realName) 
    {
        this.realName = realName;
    }

    public String getRealName() 
    {
        return realName;
    }

    public void setIdCardNo(String idCardNo) 
    {
        this.idCardNo = idCardNo;
    }

    public String getIdCardNo() 
    {
        return idCardNo;
    }

    public void setContactPhone(String contactPhone) 
    {
        this.contactPhone = contactPhone;
    }

    public String getContactPhone() 
    {
        return contactPhone;
    }

    public void setBankAccount(String bankAccount) 
    {
        this.bankAccount = bankAccount;
    }

    public String getBankAccount() 
    {
        return bankAccount;
    }

    public void setVerifyStatus(String verifyStatus) 
    {
        this.verifyStatus = verifyStatus;
    }

    public String getVerifyStatus() 
    {
        return verifyStatus;
    }

    public void setVerifyReason(String verifyReason) 
    {
        this.verifyReason = verifyReason;
    }

    public String getVerifyReason() 
    {
        return verifyReason;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("ownerId", getOwnerId())
            .append("realName", getRealName())
            .append("idCardNo", getIdCardNo())
            .append("contactPhone", getContactPhone())
            .append("bankAccount", getBankAccount())
            .append("verifyStatus", getVerifyStatus())
            .append("verifyReason", getVerifyReason())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
