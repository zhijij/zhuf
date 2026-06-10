package com.ruoyi.system.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.BizChatMessage;
import com.ruoyi.system.domain.BizChatSession;
import com.ruoyi.system.domain.BizChatSessionUser;
import com.ruoyi.system.domain.RentalAppointment;
import com.ruoyi.system.domain.RentalContract;
import com.ruoyi.system.domain.RentalHouseEntrust;
import com.ruoyi.system.domain.RentalIntention;
import com.ruoyi.system.mapper.BizChatMessageMapper;
import com.ruoyi.system.mapper.BizChatSessionMapper;
import com.ruoyi.system.mapper.BizChatSessionUserMapper;
import com.ruoyi.system.service.IBizChatService;
import com.ruoyi.system.service.IRentalAppointmentService;
import com.ruoyi.system.service.IRentalContractService;
import com.ruoyi.system.service.IRentalHouseEntrustService;
import com.ruoyi.system.service.IRentalIntentionService;

@Service
public class BizChatServiceImpl implements IBizChatService
{
    private static final String BIZ_ENTRUST = "entrust";
    private static final String BIZ_APPOINTMENT = "appointment";
    private static final String BIZ_INTENTION = "intention";
    private static final String BIZ_CONTRACT = "contract";

    @Autowired
    private BizChatSessionMapper bizChatSessionMapper;

    @Autowired
    private BizChatSessionUserMapper bizChatSessionUserMapper;

    @Autowired
    private BizChatMessageMapper bizChatMessageMapper;

    @Autowired
    private IRentalHouseEntrustService rentalHouseEntrustService;

    @Autowired
    private IRentalAppointmentService rentalAppointmentService;

    @Autowired
    private IRentalIntentionService rentalIntentionService;

    @Autowired
    private IRentalContractService rentalContractService;

    @Override
    @Transactional
    public BizChatSession openSession(String bizType, Long bizId)
    {
        if (StringUtils.isEmpty(bizType) || bizId == null)
        {
            throw new ServiceException("业务类型和业务ID不能为空");
        }

        ChatBizParticipants participants = resolveParticipants(bizType, bizId);
        Long currentUserId = SecurityUtils.getUserId();
        if (!SecurityUtils.isAdmin() && !participants.contains(currentUserId))
        {
            throw new ServiceException("当前用户无权打开该业务聊天");
        }

        BizChatSession session = bizChatSessionMapper.selectBizChatSessionByBiz(bizType, bizId);
        if (session == null)
        {
            session = new BizChatSession();
            session.setBizType(bizType);
            session.setBizId(bizId);
            session.setTitle(participants.getTitle());
            session.setStatus("0");
            session.setCreateTime(DateUtils.getNowDate());
            session.setUpdateTime(DateUtils.getNowDate());
            bizChatSessionMapper.insertBizChatSession(session);
        }

        for (Map.Entry<Long, String> entry : participants.getUsers().entrySet())
        {
            addMemberIfAbsent(session.getSessionId(), entry.getKey(), entry.getValue());
        }
        session.setMembers(listMembers(session.getSessionId()));
        return session;
    }

    @Override
    public List<BizChatSession> listMySessions()
    {
        Long currentUserId = SecurityUtils.getUserId();
        List<BizChatSession> sessions;
        if (SecurityUtils.isAdmin())
        {
            BizChatSession query = new BizChatSession();
            query.setStatus("0");
            sessions = bizChatSessionMapper.selectBizChatSessionList(query);
        }
        else
        {
            sessions = bizChatSessionMapper.selectBizChatSessionListByUserId(currentUserId);
        }
        for (BizChatSession session : sessions)
        {
            session.setMembers(listMembers(session.getSessionId()));
        }
        return sessions;
    }

    @Override
    public List<BizChatMessage> listMessages(Long sessionId)
    {
        assertCanAccessSession(sessionId);
        BizChatMessage query = new BizChatMessage();
        query.setSessionId(sessionId);
        return bizChatMessageMapper.selectBizChatMessageList(query);
    }

    @Override
    @Transactional
    public BizChatMessage sendMessage(Long sessionId, String content, String messageType)
    {
        assertCanAccessSession(sessionId);
        if (StringUtils.isEmpty(content))
        {
            throw new ServiceException("消息内容不能为空");
        }

        BizChatMessage message = new BizChatMessage();
        message.setSessionId(sessionId);
        message.setSenderId(SecurityUtils.getUserId());
        message.setMessageType(StringUtils.isEmpty(messageType) ? "text" : messageType);
        message.setContent(content);
        message.setCreateTime(DateUtils.getNowDate());
        bizChatMessageMapper.insertBizChatMessage(message);

        BizChatSession session = new BizChatSession();
        session.setSessionId(sessionId);
        session.setLastMessage(content.length() > 200 ? content.substring(0, 200) : content);
        session.setLastMessageTime(DateUtils.getNowDate());
        session.setUpdateTime(DateUtils.getNowDate());
        bizChatSessionMapper.updateBizChatSession(session);
        return message;
    }

    private void assertCanAccessSession(Long sessionId)
    {
        if (sessionId == null)
        {
            throw new ServiceException("会话ID不能为空");
        }
        BizChatSession session = bizChatSessionMapper.selectBizChatSessionBySessionId(sessionId);
        if (session == null)
        {
            throw new ServiceException("业务聊天会话不存在");
        }
        if (!SecurityUtils.isAdmin()
                && bizChatSessionUserMapper.selectBizChatSessionUser(sessionId, SecurityUtils.getUserId()) == null)
        {
            throw new ServiceException("当前用户无权访问该业务聊天");
        }
    }

    private void addMemberIfAbsent(Long sessionId, Long userId, String userRole)
    {
        if (userId == null || bizChatSessionUserMapper.selectBizChatSessionUser(sessionId, userId) != null)
        {
            return;
        }
        BizChatSessionUser member = new BizChatSessionUser();
        member.setSessionId(sessionId);
        member.setUserId(userId);
        member.setUserRole(userRole);
        member.setUnreadCount(0);
        member.setCreateTime(DateUtils.getNowDate());
        member.setUpdateTime(DateUtils.getNowDate());
        bizChatSessionUserMapper.insertBizChatSessionUser(member);
    }

    private List<BizChatSessionUser> listMembers(Long sessionId)
    {
        BizChatSessionUser query = new BizChatSessionUser();
        query.setSessionId(sessionId);
        return bizChatSessionUserMapper.selectBizChatSessionUserList(query);
    }

    private ChatBizParticipants resolveParticipants(String bizType, Long bizId)
    {
        if (BIZ_ENTRUST.equals(bizType))
        {
            RentalHouseEntrust entrust = rentalHouseEntrustService.selectRentalHouseEntrustByEntrustId(bizId);
            if (entrust == null)
            {
                throw new ServiceException("委托关系不存在");
            }
            return new ChatBizParticipants("房源委托沟通")
                    .add(entrust.getOwnerId(), "owner")
                    .add(entrust.getAgentId(), "agent");
        }
        if (BIZ_APPOINTMENT.equals(bizType))
        {
            RentalAppointment appointment = rentalAppointmentService.selectRentalAppointmentByAppointmentId(bizId);
            if (appointment == null)
            {
                throw new ServiceException("看房预约不存在");
            }
            return new ChatBizParticipants("看房预约沟通")
                    .add(appointment.getTenantId(), "tenant")
                    .add(appointment.getOwnerId(), "owner")
                    .add(appointment.getAgentId(), "agent");
        }
        if (BIZ_INTENTION.equals(bizType))
        {
            RentalIntention intention = rentalIntentionService.selectRentalIntentionByIntentionId(bizId);
            if (intention == null)
            {
                throw new ServiceException("租赁意向不存在");
            }
            return new ChatBizParticipants("租赁意向沟通")
                    .add(intention.getTenantId(), "tenant")
                    .add(intention.getOwnerId(), "owner")
                    .add(intention.getAgentId(), "agent");
        }
        if (BIZ_CONTRACT.equals(bizType))
        {
            RentalContract contract = rentalContractService.selectRentalContractByContractId(bizId);
            if (contract == null)
            {
                throw new ServiceException("租赁合同不存在");
            }
            return new ChatBizParticipants("租赁合同沟通")
                    .add(contract.getTenantId(), "tenant")
                    .add(contract.getOwnerId(), "owner")
                    .add(contract.getAgentId(), "agent");
        }
        throw new ServiceException("不支持的业务聊天类型");
    }

    private static class ChatBizParticipants
    {
        private final String title;
        private final Map<Long, String> users = new LinkedHashMap<>();

        ChatBizParticipants(String title)
        {
            this.title = title;
        }

        ChatBizParticipants add(Long userId, String role)
        {
            if (userId != null)
            {
                users.put(userId, role);
            }
            return this;
        }

        boolean contains(Long userId)
        {
            return userId != null && users.containsKey(userId);
        }

        String getTitle()
        {
            return title;
        }

        Map<Long, String> getUsers()
        {
            return users;
        }

        @SuppressWarnings("unused")
        List<Long> getUserIds()
        {
            return new ArrayList<>(users.keySet());
        }
    }
}
