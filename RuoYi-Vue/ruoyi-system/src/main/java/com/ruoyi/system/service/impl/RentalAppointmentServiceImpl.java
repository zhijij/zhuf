package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.RentalAppointmentMapper;
import com.ruoyi.system.domain.RentalAppointment;
import com.ruoyi.system.domain.RentalHouse;
import com.ruoyi.system.enums.RentalAppointmentStatus;
import com.ruoyi.system.service.IRentalAppointmentService;
import com.ruoyi.system.service.IRentalHouseService;

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

    @Autowired
    private IRentalHouseService rentalHouseService;

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

    @Override
    public int createTenantAppointment(RentalAppointment rentalAppointment, Long tenantId)
    {
        if (rentalAppointment == null || rentalAppointment.getHouseId() == null)
        {
            throw new ServiceException("请选择预约房源");
        }
        if (rentalAppointment.getAppointmentTime() == null || rentalAppointment.getAppointmentTime().before(new Date()))
        {
            throw new ServiceException("预约时间必须晚于当前时间");
        }

        RentalHouse house = rentalHouseService.selectRentalHouseDetail(rentalAppointment.getHouseId(), tenantId, false);
        ensureNoActiveAppointment(rentalAppointment.getHouseId(), tenantId);

        rentalAppointment.setTenantId(tenantId);
        rentalAppointment.setOwnerId(house.getOwnerId());
        rentalAppointment.setAgentId(house.getAgentId());
        rentalAppointment.setStatus(RentalAppointmentStatus.PENDING.code());
        if (rentalAppointment.getSource() == null)
        {
            rentalAppointment.setSource("0");
        }
        return insertRentalAppointment(rentalAppointment);
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

    @Override
    public int cancelTenantAppointment(Long appointmentId, Long tenantId, String reason)
    {
        RentalAppointment appointment = requireAppointment(appointmentId);
        if (!tenantId.equals(appointment.getTenantId()))
        {
            throw new ServiceException("只能取消自己的预约");
        }
        if (!RentalAppointmentStatus.of(appointment.getStatus()).canCancel())
        {
            throw new ServiceException("当前预约状态不允许取消");
        }
        RentalAppointment update = new RentalAppointment();
        update.setAppointmentId(appointmentId);
        update.setStatus(RentalAppointmentStatus.CANCELED.code());
        update.setCancelReason(reason);
        return updateRentalAppointment(update);
    }

    @Override
    public int confirmAppointment(Long appointmentId, Long operatorId, boolean platformAdmin)
    {
        RentalAppointment appointment = requireManageableAppointment(appointmentId, operatorId, platformAdmin);
        if (!RentalAppointmentStatus.of(appointment.getStatus()).canConfirmOrReject())
        {
            throw new ServiceException("只有待确认预约可以确认");
        }
        RentalAppointment update = new RentalAppointment();
        update.setAppointmentId(appointmentId);
        update.setStatus(RentalAppointmentStatus.CONFIRMED.code());
        return updateRentalAppointment(update);
    }

    @Override
    public int rejectAppointment(Long appointmentId, Long operatorId, boolean platformAdmin, String reason)
    {
        RentalAppointment appointment = requireManageableAppointment(appointmentId, operatorId, platformAdmin);
        if (!RentalAppointmentStatus.of(appointment.getStatus()).canConfirmOrReject())
        {
            throw new ServiceException("只有待确认预约可以拒绝");
        }
        RentalAppointment update = new RentalAppointment();
        update.setAppointmentId(appointmentId);
        update.setStatus(RentalAppointmentStatus.REJECTED.code());
        update.setCancelReason(reason);
        return updateRentalAppointment(update);
    }

    @Override
    public int completeAppointment(Long appointmentId, Long operatorId, boolean platformAdmin)
    {
        RentalAppointment appointment = requireManageableAppointment(appointmentId, operatorId, platformAdmin);
        if (!RentalAppointmentStatus.of(appointment.getStatus()).canComplete())
        {
            throw new ServiceException("只有已确认预约可以标记完成");
        }
        RentalAppointment update = new RentalAppointment();
        update.setAppointmentId(appointmentId);
        update.setStatus(RentalAppointmentStatus.COMPLETED.code());
        return updateRentalAppointment(update);
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

    private void ensureNoActiveAppointment(Long houseId, Long tenantId)
    {
        RentalAppointment query = new RentalAppointment();
        query.setHouseId(houseId);
        query.setTenantId(tenantId);
        List<RentalAppointment> appointments = rentalAppointmentMapper.selectRentalAppointmentList(query);
        for (RentalAppointment appointment : appointments)
        {
            if (RentalAppointmentStatus.PENDING.code().equals(appointment.getStatus())
                    || RentalAppointmentStatus.CONFIRMED.code().equals(appointment.getStatus()))
            {
                throw new ServiceException("该房源已有待处理预约，请勿重复预约");
            }
        }
    }

    private RentalAppointment requireAppointment(Long appointmentId)
    {
        RentalAppointment appointment = rentalAppointmentMapper.selectRentalAppointmentByAppointmentId(appointmentId);
        if (appointment == null)
        {
            throw new ServiceException("预约不存在");
        }
        return appointment;
    }

    private RentalAppointment requireManageableAppointment(Long appointmentId, Long operatorId, boolean platformAdmin)
    {
        RentalAppointment appointment = requireAppointment(appointmentId);
        if (!platformAdmin && !operatorId.equals(appointment.getOwnerId()) && !operatorId.equals(appointment.getAgentId()))
        {
            throw new ServiceException("只能处理自己房源的预约");
        }
        return appointment;
    }
}
