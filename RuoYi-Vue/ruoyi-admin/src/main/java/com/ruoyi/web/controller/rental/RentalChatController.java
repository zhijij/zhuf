package com.ruoyi.web.controller.rental;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.service.IBizChatService;

/**
 * Reusable business chat APIs.
 */
@RestController
@RequestMapping("/rental/chat")
public class RentalChatController extends BaseController
{
    @Autowired
    private IBizChatService bizChatService;

    @PreAuthorize("@ss.hasAnyExactRoles('user,tenant,owner,agent')")
    @GetMapping("/sessions")
    public AjaxResult listSessions()
    {
        return AjaxResult.success(bizChatService.listMySessions());
    }

    @PreAuthorize("@ss.hasAnyExactRoles('user,tenant,owner,agent')")
    @GetMapping("/sessions/{sessionId}/messages")
    public AjaxResult listMessages(@PathVariable Long sessionId)
    {
        return AjaxResult.success(bizChatService.listMessages(sessionId));
    }

    @PreAuthorize("@ss.hasAnyExactRoles('user,tenant,owner,agent')")
    @Log(title = "打开业务会话", businessType = BusinessType.OTHER)
    @PostMapping("/sessions/open")
    public AjaxResult openSession(@RequestBody OpenSessionRequest request)
    {
        return AjaxResult.success(bizChatService.openSession(request.getBizType(), request.getBizId()));
    }

    @PreAuthorize("@ss.hasAnyExactRoles('user,tenant,owner,agent')")
    @Log(title = "发送业务消息", businessType = BusinessType.INSERT)
    @PostMapping("/messages")
    public AjaxResult sendMessage(@RequestBody SendMessageRequest request)
    {
        return AjaxResult.success(bizChatService.sendMessage(
                request.getSessionId(), request.getContent(), request.getMessageType()));
    }

    public static class OpenSessionRequest
    {
        private String bizType;
        private Long bizId;

        public String getBizType()
        {
            return bizType;
        }

        public void setBizType(String bizType)
        {
            this.bizType = bizType;
        }

        public Long getBizId()
        {
            return bizId;
        }

        public void setBizId(Long bizId)
        {
            this.bizId = bizId;
        }
    }

    public static class SendMessageRequest
    {
        private Long sessionId;
        private String content;
        private String messageType;

        public Long getSessionId()
        {
            return sessionId;
        }

        public void setSessionId(Long sessionId)
        {
            this.sessionId = sessionId;
        }

        public String getContent()
        {
            return content;
        }

        public void setContent(String content)
        {
            this.content = content;
        }

        public String getMessageType()
        {
            return messageType;
        }

        public void setMessageType(String messageType)
        {
            this.messageType = messageType;
        }
    }
}
