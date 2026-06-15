package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.RentalHouse;
import com.ruoyi.system.domain.RentalContract;
import com.ruoyi.system.domain.RentalHouseEntrust;
import com.ruoyi.system.domain.dto.RentalHouseAuditRequest;
import com.ruoyi.system.domain.dto.RentalHouseCancelRequest;
import com.ruoyi.system.domain.dto.RentalHouseDealRequest;

/**
 * 租赁房源Service接口
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
public interface IRentalHouseService 
{
    /**
     * 查询租赁房源
     * 
     * @param houseId 租赁房源主键
     * @return 租赁房源
     */
    public RentalHouse selectRentalHouseByHouseId(Long houseId);

    /**
     * 查询租赁房源列表
     * 
     * @param rentalHouse 租赁房源
     * @return 租赁房源集合
     */
    public List<RentalHouse> selectRentalHouseList(RentalHouse rentalHouse);

    /**
     * 查询租户可见房源列表。
     * 房源必须合法审核通过，且为房东自营或委托双方已确认的房源。
     *
     * @param rentalHouse 查询条件
     * @return 租户可见房源集合
     */
    public List<RentalHouse> selectPublicRentalHouseList(RentalHouse rentalHouse);

    /**
     * 查询管理员房源合规审核队列。
     *
     * @param rentalHouse 查询条件
     * @return 待审核房源集合
     */
    public List<RentalHouse> selectAuditRentalHouseList(RentalHouse rentalHouse);

    /**
     * 查询房源详情，已成交/下架房源仅对相关方和后台可见。
     *
     * @param houseId 房源ID
     * @param viewerId 查看人用户ID
     * @param platformAdmin 是否后台管理员或超级管理员
     * @return 房源详情
     */
    public RentalHouse selectRentalHouseDetail(Long houseId, Long viewerId, boolean platformAdmin);

    /**
     * 新增租赁房源
     * 
     * @param rentalHouse 租赁房源
     * @return 结果
     */
    public int insertRentalHouse(RentalHouse rentalHouse);

    /**
     * 用户提交房源，进入待审核生命周期。
     *
     * @param rentalHouse 房源信息
     * @param ownerId 房东用户ID
     * @param operator 操作人账号
     * @return 结果
     */
    public int submitRentalHouse(RentalHouse rentalHouse, Long ownerId, String operator);

    /**
     * 修改租赁房源
     * 
     * @param rentalHouse 租赁房源
     * @return 结果
     */
    public int updateRentalHouse(RentalHouse rentalHouse);

    /**
     * 清除委托代理信息，恢复房东自营。
     *
     * @param houseId 房源ID
     * @return 结果
     */
    public int clearRentalHouseAgent(Long houseId);

    /**
     * 用户在审核通过前修改房源信息。
     *
     * @param houseId 房源ID
     * @param rentalHouse 新房源信息
     * @param ownerId 房东用户ID
     * @param operator 操作人账号
     * @return 结果
     */
    public int updateRentalHouseBeforeApproval(Long houseId, RentalHouse rentalHouse, Long ownerId, String operator);

    /**
     * 用户重新提交待审核房源。
     *
     * @param houseId 房源ID
     * @param ownerId 房东用户ID
     * @param operator 操作人账号
     * @return 结果
     */
    public int resubmitRentalHouseAudit(Long houseId, Long ownerId, String operator);

    /**
     * 用户取消申请或下架房源。
     *
     * @param houseId 房源ID
     * @param request 取消请求
     * @param ownerId 房东用户ID
     * @param operator 操作人账号
     * @return 结果
     */
    public int cancelRentalHouse(Long houseId, RentalHouseCancelRequest request, Long ownerId, String operator);

    /**
     * 管理员审核房源。
     *
     * @param houseId 房源ID
     * @param request 审核请求
     * @param operator 操作人账号
     * @return 结果
     */
    public int auditRentalHouse(Long houseId, RentalHouseAuditRequest request, String operator);

    /**
     * 房东将房源委托给中介代理。
     *
     * @param houseId 房源ID
     * @param entrust 委托信息
     * @param ownerId 房东用户ID
     * @param operator 操作人账号
     * @return 委托关系
     */
    public RentalHouseEntrust entrustRentalHouse(Long houseId, RentalHouseEntrust entrust, Long ownerId, String operator);

    /**
     * 房源成交并从公开房源和RAG索引中移除。
     *
     * @param houseId 房源ID
     * @param request 成交请求
     * @param operatorId 操作人用户ID
     * @param operator 操作人账号
     * @param platformAdmin 是否后台管理员或超级管理员
     * @return 合同草稿/生效记录
     */
    public RentalContract completeRentalHouseDeal(Long houseId, RentalHouseDealRequest request, Long operatorId, String operator, boolean platformAdmin);

    /**
     * 租户发起成交申请，创建待确认合同。
     *
     * @param houseId 房源ID
     * @param request 成交请求
     * @param tenantId 租户ID
     * @param operator 操作人账号
     * @return 合同记录
     */
    public RentalContract applyRentalHouseDeal(Long houseId, RentalHouseDealRequest request, Long tenantId, String operator);

    /**
     * 批量删除租赁房源
     * 
     * @param houseIds 需要删除的租赁房源主键集合
     * @return 结果
     */
    public int deleteRentalHouseByHouseIds(Long[] houseIds);

    /**
     * 删除租赁房源信息
     * 
     * @param houseId 租赁房源主键
     * @return 结果
     */
    public int deleteRentalHouseByHouseId(Long houseId);
}
