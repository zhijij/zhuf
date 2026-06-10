package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.RentalAppointmentMapper;
import com.ruoyi.system.domain.RentalAppointment;
import com.ruoyi.system.service.IRentalAppointmentService;

/**
 * 看房预约Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
@Service
public class RentalAppointmentServiceImpl implements IRentalAppointmentService 
{
    @Autowired
    private RentalAppointmentMapper rentalAppointmentMapper;

    /**
     * 查询看房预约
     * 
     * @param appointmentId 看房预约主键
     * @return 看房预约
     */
    @Override
    public RentalAppointment selectRentalAppointmentByAppointmentId(Long appointmentId)
    {
        return rentalAppointmentMapper.selectRentalAppointmentByAppointmentId(appointmentId);
    }

    /**
     * 查询看房预约列表
     * 
     * @param rentalAppointment 看房预约
     * @return 看房预约
     */
    @Override
    public List<RentalAppointment> selectRentalAppointmentList(RentalAppointment rentalAppointment)
    {
        return rentalAppointmentMapper.selectRentalAppointmentList(rentalAppointment);
    }

    /**
     * 新增看房预约
     * 
     * @param rentalAppointment 看房预约
     * @return 结果
     */
    @Override
    public int insertRentalAppointment(RentalAppointment rentalAppointment)
    {
        rentalAppointment.setCreateTime(DateUtils.getNowDate());
        return rentalAppointmentMapper.insertRentalAppointment(rentalAppointment);
    }

    /**
     * 修改看房预约
     * 
     * @param rentalAppointment 看房预约
     * @return 结果
     */
    @Override
    public int updateRentalAppointment(RentalAppointment rentalAppointment)
    {
        rentalAppointment.setUpdateTime(DateUtils.getNowDate());
        return rentalAppointmentMapper.updateRentalAppointment(rentalAppointment);
    }

    /**
     * 批量删除看房预约
     * 
     * @param appointmentIds 需要删除的看房预约主键
     * @return 结果
     */
    @Override
    public int deleteRentalAppointmentByAppointmentIds(Long[] appointmentIds)
    {
        return rentalAppointmentMapper.deleteRentalAppointmentByAppointmentIds(appointmentIds);
    }

    /**
     * 删除看房预约信息
     * 
     * @param appointmentId 看房预约主键
     * @return 结果
     */
    @Override
    public int deleteRentalAppointmentByAppointmentId(Long appointmentId)
    {
        return rentalAppointmentMapper.deleteRentalAppointmentByAppointmentId(appointmentId);
    }
}
