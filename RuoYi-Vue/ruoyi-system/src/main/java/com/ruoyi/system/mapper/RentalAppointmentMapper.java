package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.RentalAppointment;

/**
 * 看房预约Mapper接口
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
public interface RentalAppointmentMapper 
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
     * 修改看房预约
     * 
     * @param rentalAppointment 看房预约
     * @return 结果
     */
    public int updateRentalAppointment(RentalAppointment rentalAppointment);

    /**
     * 删除看房预约
     * 
     * @param appointmentId 看房预约主键
     * @return 结果
     */
    public int deleteRentalAppointmentByAppointmentId(Long appointmentId);

    /**
     * 批量删除看房预约
     * 
     * @param appointmentIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteRentalAppointmentByAppointmentIds(Long[] appointmentIds);
}
