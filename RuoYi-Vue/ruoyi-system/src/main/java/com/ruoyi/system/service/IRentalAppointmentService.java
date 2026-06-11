package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.RentalAppointment;

/**
 * 看房预约Service接口
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
public interface IRentalAppointmentService 
{
    /**
     * 查询看房预约
     * 
     * @param appointmentId 看房预约主键
     * @return 看房预约
     */
    public RentalAppointment selectRentalAppointmentByAppointmentId(Long appointmentId);

    /**
     * 查询看房预约列表
     * 
     * @param rentalAppointment 看房预约
     * @return 看房预约集合
     */
    public List<RentalAppointment> selectRentalAppointmentList(RentalAppointment rentalAppointment);

    /**
     * 新增看房预约
     * 
     * @param rentalAppointment 看房预约
     * @return 结果
     */
    public int insertRentalAppointment(RentalAppointment rentalAppointment);

    /**
     * 租户创建看房预约。
     *
     * @param rentalAppointment 预约信息
     * @param tenantId 租户ID
     * @return 结果
     */
    public int createTenantAppointment(RentalAppointment rentalAppointment, Long tenantId);

    /**
     * 修改看房预约
     * 
     * @param rentalAppointment 看房预约
     * @return 结果
     */
    public int updateRentalAppointment(RentalAppointment rentalAppointment);

    /**
     * 租户取消预约。
     *
     * @param appointmentId 预约ID
     * @param tenantId 租户ID
     * @param reason 取消原因
     * @return 结果
     */
    public int cancelTenantAppointment(Long appointmentId, Long tenantId, String reason);

    /**
     * 房东或中介确认预约。
     *
     * @param appointmentId 预约ID
     * @param operatorId 操作人ID
     * @param platformAdmin 是否后台管理员
     * @return 结果
     */
    public int confirmAppointment(Long appointmentId, Long operatorId, boolean platformAdmin);

    /**
     * 房东或中介拒绝预约。
     *
     * @param appointmentId 预约ID
     * @param operatorId 操作人ID
     * @param platformAdmin 是否后台管理员
     * @param reason 拒绝原因
     * @return 结果
     */
    public int rejectAppointment(Long appointmentId, Long operatorId, boolean platformAdmin, String reason);

    /**
     * 房东或中介标记看房完成。
     *
     * @param appointmentId 预约ID
     * @param operatorId 操作人ID
     * @param platformAdmin 是否后台管理员
     * @return 结果
     */
    public int completeAppointment(Long appointmentId, Long operatorId, boolean platformAdmin);

    /**
     * 批量删除看房预约
     * 
     * @param appointmentIds 需要删除的看房预约主键集合
     * @return 结果
     */
    public int deleteRentalAppointmentByAppointmentIds(Long[] appointmentIds);

    /**
     * 删除看房预约信息
     * 
     * @param appointmentId 看房预约主键
     * @return 结果
     */
    public int deleteRentalAppointmentByAppointmentId(Long appointmentId);
}
