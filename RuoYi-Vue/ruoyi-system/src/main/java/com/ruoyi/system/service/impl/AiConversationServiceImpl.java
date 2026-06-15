package com.ruoyi.system.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.AiConversation;
import com.ruoyi.system.domain.AiConversationContext;
import com.ruoyi.system.domain.AiConversationMessage;
import com.ruoyi.system.domain.AiConversationSession;
import com.ruoyi.system.mapper.AiConversationContextMapper;
import com.ruoyi.system.mapper.AiConversationMapper;
import com.ruoyi.system.mapper.AiConversationMessageMapper;
import com.ruoyi.system.mapper.AiConversationSessionMapper;
import com.ruoyi.system.service.IAiConversationService;

@Service
public class AiConversationServiceImpl implements IAiConversationService
{
    @Autowired
    private AiConversationMapper aiConversationMapper;

    @Autowired
    private AiConversationSessionMapper aiConversationSessionMapper;

    @Autowired
    private AiConversationMessageMapper aiConversationMessageMapper;

    @Autowired
    private AiConversationContextMapper aiConversationContextMapper;

    @Override
    public List<AiConversation> listMyConversations(String workMode)
    {
        AiConversation query = new AiConversation();
        query.setUserId(SecurityUtils.getUserId());
        query.setStatus("0");
        if (StringUtils.isNotEmpty(workMode))
        {
            query.setWorkMode(workMode);
        }
        return aiConversationMapper.selectAiConversationList(query);
    }

    @Override
    @Transactional
    public AiConversation createConversation(Map<String, Object> payload)
    {
        Date now = DateUtils.getNowDate();
        Long userId = SecurityUtils.getUserId();

        AiConversation conversation = new AiConversation();
        conversation.setUserId(userId);
        conversation.setTitle(defaultTitle(stringValue(payload, "title")));
        conversation.setRole(stringValue(payload, "role"));
        conversation.setWorkMode(stringValue(payload, "workMode"));
        conversation.setStatus("0");
        conversation.setLastMessageTime(now);
        conversation.setCreateTime(now);
        conversation.setUpdateTime(now);
        aiConversationMapper.insertAiConversation(conversation);

        AiConversationSession session = new AiConversationSession();
        session.setConversationId(conversation.getConversationId());
        session.setSessionKey("ai-conversation-" + conversation.getConversationId());
        session.setProvider(stringValue(payload, "provider"));
        session.setModelName(stringValue(payload, "modelName"));
        session.setStatus("0");
        session.setStartedAt(now);
        session.setCreateTime(now);
        session.setUpdateTime(now);
        aiConversationSessionMapper.insertAiConversationSession(session);
        conversation.setSession(session);

        Object context = payload == null ? null : payload.get("context");
        if (context instanceof Map)
        {
            @SuppressWarnings("unchecked")
            Map<String, Object> contextMap = (Map<String, Object>) context;
            conversation.setContext(saveContext(conversation.getConversationId(), contextMap));
        }
        return conversation;
    }

    @Override
    public AiConversation getMyConversation(Long conversationId)
    {
        AiConversation conversation = assertOwnConversation(conversationId);
        conversation.setSession(aiConversationSessionMapper.selectAiConversationSessionByConversationId(conversationId));
        conversation.setContext(aiConversationContextMapper.selectAiConversationContextByConversationId(conversationId));
        AiConversationMessage query = new AiConversationMessage();
        query.setConversationId(conversationId);
        conversation.setMessages(aiConversationMessageMapper.selectAiConversationMessageList(query));
        return conversation;
    }

    @Override
    public void deleteMyConversation(Long conversationId)
    {
        AiConversation conversation = assertOwnConversation(conversationId);
        AiConversation update = new AiConversation();
        update.setConversationId(conversation.getConversationId());
        update.setStatus("1");
        update.setUpdateTime(DateUtils.getNowDate());
        aiConversationMapper.updateAiConversation(update);
    }

    @Override
    @Transactional
    public AiConversationContext saveContext(Long conversationId, Map<String, Object> payload)
    {
        assertOwnConversation(conversationId);
        Date now = DateUtils.getNowDate();
        AiConversationContext context = aiConversationContextMapper.selectAiConversationContextByConversationId(conversationId);
        if (context == null)
        {
            context = new AiConversationContext();
            context.setConversationId(conversationId);
            fillContext(context, payload);
            context.setCreateTime(now);
            context.setUpdateTime(now);
            aiConversationContextMapper.insertAiConversationContext(context);
            return context;
        }
        fillContext(context, payload);
        context.setUpdateTime(now);
        aiConversationContextMapper.updateAiConversationContext(context);
        return context;
    }

    @Override
    @Transactional
    public AiConversationMessage addMessage(Long conversationId, String role, String content, Map<String, Object> metadata)
    {
        AiConversation conversation = assertOwnConversation(conversationId);
        if (StringUtils.isEmpty(content))
        {
            throw new ServiceException("消息内容不能为空");
        }

        Date now = DateUtils.getNowDate();
        AiConversationMessage message = new AiConversationMessage();
        message.setConversationId(conversationId);
        message.setUserId("assistant".equals(role) || "system".equals(role) || "tool".equals(role) ? null : SecurityUtils.getUserId());
        message.setRole(StringUtils.isEmpty(role) ? "user" : role);
        message.setContent(content);
        if (metadata != null)
        {
            message.setIntent(stringValue(metadata, "intent"));
            message.setIntentLabel(stringValue(metadata, "intentLabel"));
            message.setToolCalls(jsonValue(metadata.get("toolCalls")));
            message.setCollaboration(jsonValue(metadata.get("collaboration")));
            Object contextSnapshot = metadata.containsKey("contextSnapshot") ? metadata.get("contextSnapshot") : metadata.get("context");
            message.setContextSnapshot(jsonValue(contextSnapshot));
        }
        message.setCreateTime(now);
        aiConversationMessageMapper.insertAiConversationMessage(message);

        AiConversation update = new AiConversation();
        update.setConversationId(conversationId);
        if (StringUtils.isEmpty(conversation.getTitle()) || "新对话".equals(conversation.getTitle()))
        {
            update.setTitle(defaultTitle(content));
        }
        update.setLastMessage(limit(content, 500));
        update.setLastMessageTime(now);
        update.setUpdateTime(now);
        aiConversationMapper.updateAiConversation(update);
        return message;
    }

    @Override
    public List<Map<String, String>> buildHistory(Long conversationId, int limit)
    {
        assertOwnConversation(conversationId);
        AiConversationMessage query = new AiConversationMessage();
        query.setConversationId(conversationId);
        List<AiConversationMessage> messages = aiConversationMessageMapper.selectAiConversationMessageList(query);
        int fromIndex = Math.max(0, messages.size() - Math.max(limit, 1));
        List<Map<String, String>> history = new ArrayList<>();
        for (AiConversationMessage message : messages.subList(fromIndex, messages.size()))
        {
            if (!"user".equals(message.getRole()) && !"assistant".equals(message.getRole()))
            {
                continue;
            }
            Map<String, String> item = new LinkedHashMap<>();
            item.put("role", message.getRole());
            item.put("content", message.getContent());
            history.add(item);
        }
        return history;
    }

    @Override
    public String summarizeConversation(Long conversationId)
    {
        AiConversation conversation = getMyConversation(conversationId);
        List<AiConversationMessage> messages = conversation.getMessages();
        if (messages == null || messages.isEmpty())
        {
            return "";
        }
        List<String> snippets = new ArrayList<>();
        int fromIndex = Math.max(0, messages.size() - 8);
        for (AiConversationMessage message : messages.subList(fromIndex, messages.size()))
        {
            snippets.add(message.getRole() + ": " + limit(message.getContent(), 120));
        }
        String summary = limit(String.join("\n", snippets), 1200);
        AiConversation update = new AiConversation();
        update.setConversationId(conversationId);
        update.setSummary(summary);
        update.setUpdateTime(DateUtils.getNowDate());
        aiConversationMapper.updateAiConversation(update);
        return summary;
    }

    private AiConversation assertOwnConversation(Long conversationId)
    {
        if (conversationId == null)
        {
            throw new ServiceException("会话ID不能为空");
        }
        AiConversation conversation = aiConversationMapper.selectAiConversationByConversationId(conversationId);
        if (conversation == null || "1".equals(conversation.getStatus()))
        {
            throw new ServiceException("AI会话不存在");
        }
        if (!SecurityUtils.getUserId().equals(conversation.getUserId()))
        {
            throw new ServiceException("无权限访问该AI会话");
        }
        return conversation;
    }

    private void fillContext(AiConversationContext context, Map<String, Object> payload)
    {
        Map<String, Object> source = unwrapContext(payload);
        context.setBizType(firstText(source, "bizType", "recordType"));
        context.setHouseId(longValue(source.get("houseId")));
        context.setContractId(longValue(source.get("contractId")));
        context.setAppointmentId(longValue(source.get("appointmentId")));
        context.setIntentionId(longValue(source.get("intentionId")));
        context.setEntrustId(longValue(source.get("entrustId")));
        context.setBizId(resolveBizId(context));
        context.setSnapshotJson(jsonValue(source));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> unwrapContext(Map<String, Object> payload)
    {
        if (payload == null)
        {
            return new LinkedHashMap<>();
        }
        Object selected = payload.get("selected");
        if (selected instanceof Map)
        {
            return (Map<String, Object>) selected;
        }
        return payload;
    }

    private Long resolveBizId(AiConversationContext context)
    {
        if (context.getContractId() != null)
        {
            return context.getContractId();
        }
        if (context.getAppointmentId() != null)
        {
            return context.getAppointmentId();
        }
        if (context.getIntentionId() != null)
        {
            return context.getIntentionId();
        }
        if (context.getEntrustId() != null)
        {
            return context.getEntrustId();
        }
        return context.getHouseId();
    }

    private String defaultTitle(String title)
    {
        if (StringUtils.isNotEmpty(title))
        {
            return limit(title.trim(), 40);
        }
        return "新对话";
    }

    private String firstText(Map<String, Object> map, String... keys)
    {
        for (String key : keys)
        {
            String value = stringValue(map, key);
            if (StringUtils.isNotEmpty(value))
            {
                return value;
            }
        }
        return null;
    }

    private String stringValue(Map<String, Object> map, String key)
    {
        if (map == null || !map.containsKey(key) || map.get(key) == null)
        {
            return null;
        }
        String value = String.valueOf(map.get(key));
        return StringUtils.isEmpty(value) ? null : value;
    }

    private Long longValue(Object value)
    {
        if (value == null || String.valueOf(value).isBlank())
        {
            return null;
        }
        try
        {
            return Long.valueOf(String.valueOf(value));
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    private String jsonValue(Object value)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof String)
        {
            return (String) value;
        }
        return JSON.toJSONString(value);
    }

    private String limit(String value, int maxLength)
    {
        if (value == null || value.length() <= maxLength)
        {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
