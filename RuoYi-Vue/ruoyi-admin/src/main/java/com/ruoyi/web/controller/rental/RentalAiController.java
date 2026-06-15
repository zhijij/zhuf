package com.ruoyi.web.controller.rental;

import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.AiConversation;
import com.ruoyi.system.domain.AiConversationMessage;
import com.ruoyi.system.domain.AiVectorIndexTask;
import com.ruoyi.system.domain.RentalHouse;
import com.ruoyi.system.service.IAiConversationService;
import com.ruoyi.system.service.IAiVectorIndexTaskService;
import com.ruoyi.system.service.IRentalHouseService;

@RestController
@RequestMapping("/rental/ai")
public class RentalAiController
{
    private final RestTemplate restTemplate;

    @Autowired
    private IAiVectorIndexTaskService aiVectorIndexTaskService;

    @Autowired
    private IRentalHouseService rentalHouseService;

    @Autowired
    private IAiConversationService aiConversationService;

    @Value("${ai.service.base-url}")
    private String aiBaseUrl;

    public RentalAiController(RestTemplate restTemplate)
    {
        this.restTemplate = restTemplate;
    }

    @PostMapping("/chat")
    @PreAuthorize("@ss.hasAnyExactRoles('user,tenant,owner,agent,auditor')")
    public AjaxResult chat(@RequestBody Map<String, Object> request)
    {
        return forward("/api/v1/agent/chat", enrichRequest(request));
    }

    @PostMapping("/recommend")
    @PreAuthorize("@ss.hasAnyExactRoles('user,tenant,owner,agent,auditor')")
    public AjaxResult recommend(@RequestBody Map<String, Object> request)
    {
        return forward("/api/v1/agent/recommend", enrichRequest(request));
    }

    @GetMapping("/conversations")
    @PreAuthorize("@ss.hasAnyExactRoles('user,tenant,owner,agent,auditor')")
    public AjaxResult listConversations(@RequestParam(required = false) String workMode)
    {
        return AjaxResult.success(aiConversationService.listMyConversations(workMode));
    }

    @PostMapping("/conversations")
    @PreAuthorize("@ss.hasAnyExactRoles('user,tenant,owner,agent,auditor')")
    public AjaxResult createConversation(@RequestBody(required = false) Map<String, Object> request)
    {
        return AjaxResult.success(aiConversationService.createConversation(request == null ? new HashMap<>() : request));
    }

    @GetMapping("/conversations/{conversationId}")
    @PreAuthorize("@ss.hasAnyExactRoles('user,tenant,owner,agent,auditor')")
    public AjaxResult getConversation(@PathVariable Long conversationId)
    {
        return AjaxResult.success(aiConversationService.getMyConversation(conversationId));
    }

    @DeleteMapping("/conversations/{conversationId}")
    @PreAuthorize("@ss.hasAnyExactRoles('user,tenant,owner,agent,auditor')")
    public AjaxResult deleteConversation(@PathVariable Long conversationId)
    {
        aiConversationService.deleteMyConversation(conversationId);
        return AjaxResult.success();
    }

    @PutMapping("/conversations/{conversationId}/context")
    @PreAuthorize("@ss.hasAnyExactRoles('user,tenant,owner,agent,auditor')")
    public AjaxResult saveConversationContext(@PathVariable Long conversationId, @RequestBody Map<String, Object> request)
    {
        return AjaxResult.success(aiConversationService.saveContext(conversationId, request));
    }

    @PostMapping("/conversations/{conversationId}/summary")
    @PreAuthorize("@ss.hasAnyExactRoles('user,tenant,owner,agent,auditor')")
    public AjaxResult summarizeConversation(@PathVariable Long conversationId)
    {
        Map<String, Object> result = new HashMap<>();
        result.put("summary", aiConversationService.summarizeConversation(conversationId));
        return AjaxResult.success(result);
    }

    @PostMapping("/conversations/{conversationId}/messages")
    @PreAuthorize("@ss.hasAnyExactRoles('user,tenant,owner,agent,auditor')")
    public AjaxResult sendConversationMessage(@PathVariable Long conversationId, @RequestBody Map<String, Object> request)
    {
        Object messageValue = request == null ? null : request.get("message");
        String content = messageValue == null ? null : String.valueOf(messageValue).trim();
        if (StringUtils.isEmpty(content))
        {
            return AjaxResult.error("消息内容不能为空");
        }

        Map<String, Object> context = extractMap(request, "context");
        if (context != null)
        {
            aiConversationService.saveContext(conversationId, context);
        }

        Map<String, Object> userMetadata = new HashMap<>();
        userMetadata.put("contextSnapshot", context);
        List<Map<String, String>> history = aiConversationService.buildHistory(conversationId, 12);
        AiConversationMessage userMessage = aiConversationService.addMessage(conversationId, "user", content, userMetadata);

        Map<String, Object> payload = enrichRequest(request);
        AiConversation conversation = aiConversationService.getMyConversation(conversationId);
        String sessionKey = conversation.getSession() == null ? "ai-conversation-" + conversationId : conversation.getSession().getSessionKey();
        payload.put("message", content);
        payload.put("sessionId", sessionKey);
        payload.put("history", history);

        try
        {
            ResponseEntity<Map> response = restTemplate.postForEntity(aiBaseUrl + "/api/v1/agent/chat", payload, Map.class);
            Map body = response.getBody();
            String answer = body == null ? "" : String.valueOf(body.getOrDefault("answer", ""));
            if (StringUtils.isEmpty(answer))
            {
                answer = "智能体接口已收到请求，等待后端返回标准化结果。";
            }
            AiConversationMessage assistantMessage = aiConversationService.addMessage(
                    conversationId,
                    "assistant",
                    answer,
                    buildAssistantMetadata(body, context));

            Map<String, Object> result = new HashMap<>();
            result.put("conversation", aiConversationService.getMyConversation(conversationId));
            result.put("userMessage", userMessage);
            result.put("assistantMessage", assistantMessage);
            result.put("assistant", body);
            return AjaxResult.success(result);
        }
        catch (Exception e)
        {
            return AjaxResult.error("智能体服务暂不可用，请稍后再试");
        }
    }

    @GetMapping("/capabilities")
    @PreAuthorize("@ss.hasAnyExactRoles('user,tenant,owner,agent,auditor')")
    public AjaxResult capabilities()
    {
        try
        {
            ResponseEntity<Map> response = restTemplate.getForEntity(aiBaseUrl + "/api/v1/agent/capabilities", Map.class);
            return AjaxResult.success(response.getBody());
        }
        catch (Exception e)
        {
            return AjaxResult.error("智能体能力服务暂不可用，请稍后再试");
        }
    }

    @PostMapping("/index/knowledge")
    @PreAuthorize("@ss.hasRole('admin')")
    public AjaxResult indexKnowledge(@RequestBody Map<String, Object> request)
    {
        Map<String, Object> payload = enrichRequest(request);
        payload.putIfAbsent("action", "upsert");
        payload.putIfAbsent("sourceType", "faq");
        return forward("/api/v1/index/knowledge", payload);
    }

    @PostMapping("/index/knowledge/seed")
    @PreAuthorize("@ss.hasRole('admin')")
    public AjaxResult seedKnowledge()
    {
        return forward("/api/v1/index/knowledge/seed", enrichRequest(null));
    }

    @PostMapping("/index/tasks")
    @PreAuthorize("@ss.hasRole('admin')")
    public AjaxResult createIndexTask(@RequestBody(required = false) Map<String, Object> request)
    {
        Map<String, Object> payload = enrichRequest(request);
        Long houseId = parseLong(payload.get("houseId"));
        String action = payload.get("action") == null || String.valueOf(payload.get("action")).isBlank()
                ? (houseId == null ? "sync" : "upsert")
                : String.valueOf(payload.get("action"));
        RentalHouse house = null;
        if (houseId == null && !hasAdminRole())
        {
            return AjaxResult.error("只有管理员可以创建全量索引任务");
        }
        if (houseId != null)
        {
            house = rentalHouseService.selectRentalHouseByHouseId(houseId);
            if (house == null)
            {
                return AjaxResult.error("房源不存在，无法创建索引任务");
            }
            if (!canOperateHouseAi(house))
            {
                return AjaxResult.error("无权限操作该房源的 AI 索引");
            }
        }

        AiVectorIndexTask task = new AiVectorIndexTask();
        task.setSourceType("house");
        task.setSourceId(houseId == null ? 0L : houseId);
        task.setAction(action);
        task.setStatus("0");
        task.setRetryCount(0L);
        task.setErrorMsg("等待向量库任务消费者处理");
        aiVectorIndexTaskService.insertAiVectorIndexTask(task);

        Map<String, Object> taskView = new HashMap<>();
        taskView.put("taskId", task.getTaskId());
        taskView.put("sourceType", task.getSourceType());
        taskView.put("sourceId", task.getSourceId());
        taskView.put("action", task.getAction());
        taskView.put("status", task.getStatus());
        taskView.put("retryCount", task.getRetryCount());
        taskView.put("errorMsg", task.getErrorMsg());
        taskView.put("createTime", task.getCreateTime());
        taskView.put("message", houseId == null
                ? "全量同步索引任务已创建"
                : "房源索引任务已创建，等待向量库和文档解析流程处理");
        return AjaxResult.success(taskView);
    }

    @GetMapping("/index/tasks")
    @PreAuthorize("@ss.hasRole('admin')")
    public AjaxResult listIndexTasks()
    {
        AiVectorIndexTask query = new AiVectorIndexTask();
        query.setSourceType("house");
        List<AiVectorIndexTask> tasks = aiVectorIndexTaskService.selectAiVectorIndexTaskList(query).stream()
                .filter(this::canViewIndexTask)
                .sorted(Comparator.comparing(
                        AiVectorIndexTask::getCreateTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(12)
                .collect(Collectors.toList());
        return AjaxResult.success(tasks);
    }

    @PostMapping("/index/tasks/{taskId}/process")
    @PreAuthorize("@ss.hasRole('admin')")
    public AjaxResult processIndexTask(@PathVariable Long taskId)
    {
        AiVectorIndexTask task = aiVectorIndexTaskService.selectAiVectorIndexTaskByTaskId(taskId);
        if (task == null)
        {
            return AjaxResult.error("索引任务不存在");
        }
        if (!canViewIndexTask(task))
        {
            return AjaxResult.error("无权限处理该索引任务");
        }
        return AjaxResult.success(processIndexTaskInternal(task));
    }

    @PostMapping("/index/tasks/process-pending")
    @PreAuthorize("@ss.hasRole('admin')")
    public AjaxResult processPendingIndexTasks(@RequestBody(required = false) Map<String, Object> request)
    {
        Map<String, Object> payload = request == null ? new HashMap<>() : new HashMap<>(request);
        int limit = 5;
        Long limitValue = parseLong(payload.get("limit"));
        if (limitValue != null && limitValue > 0)
        {
            limit = Math.min(limitValue.intValue(), 20);
        }

        AiVectorIndexTask query = new AiVectorIndexTask();
        query.setSourceType("house");
        query.setStatus("0");
        List<Map<String, Object>> results = aiVectorIndexTaskService.selectAiVectorIndexTaskList(query).stream()
                .filter(this::canViewIndexTask)
                .sorted(Comparator.comparing(
                        AiVectorIndexTask::getCreateTime,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .limit(limit)
                .map(this::processIndexTaskInternal)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("processed", results.size());
        result.put("items", results);
        return AjaxResult.success(result);
    }

    @GetMapping("/houses/{houseId}/document")
    @PreAuthorize("@ss.hasRole('admin')")
    public AjaxResult inspectHouseDocument(@PathVariable Long houseId)
    {
        RentalHouse house = rentalHouseService.selectRentalHouseByHouseId(houseId);
        if (house == null)
        {
            return AjaxResult.error("房源不存在");
        }
        if (!canOperateHouseAi(house))
        {
            return AjaxResult.error("无权限查看该房源的 AI 文档");
        }

        AiVectorIndexTask query = new AiVectorIndexTask();
        query.setSourceType("house");
        query.setSourceId(houseId);
        List<AiVectorIndexTask> relatedTasks = aiVectorIndexTaskService.selectAiVectorIndexTaskList(query).stream()
                .sorted(Comparator.comparing(
                        AiVectorIndexTask::getCreateTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
        AiVectorIndexTask latestTask = relatedTasks.isEmpty() ? null : relatedTasks.get(0);

        Map<String, Object> document = new HashMap<>();
        document.put("houseId", houseId);
        document.put("title", house.getTitle());
        document.put("indexed", "1".equals(house.getAiIndexStatus()));
        document.put("aiIndexStatus", house.getAiIndexStatus());
        document.put("latestTask", latestTask);
        document.put("message", buildDocumentMessage(house.getAiIndexStatus(), latestTask));
        return AjaxResult.success(document);
    }

    private Map<String, Object> processIndexTaskInternal(AiVectorIndexTask task)
    {
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", task.getTaskId());
        result.put("sourceId", task.getSourceId());
        result.put("action", task.getAction());

        if (isFullSyncTask(task))
        {
            return processFullHouseSyncTask(task, result);
        }

        if (!"house".equals(task.getSourceType()))
        {
            markTaskFailed(task, "当前处理器只支持房源索引任务");
            result.put("success", false);
            result.put("message", "当前处理器只支持房源索引任务");
            return result;
        }
        if ("2".equals(task.getStatus()))
        {
            result.put("success", true);
            result.put("message", "任务已处理完成");
            return result;
        }

        RentalHouse house = null;
        if (task.getSourceId() != null && task.getSourceId() > 0)
        {
            house = rentalHouseService.selectRentalHouseByHouseId(task.getSourceId());
        }
        if (house == null && !"sync".equals(task.getAction()))
        {
            markTaskFailed(task, "房源不存在，无法处理索引任务");
            result.put("success", false);
            result.put("message", "房源不存在，无法处理索引任务");
            return result;
        }

        markTaskProcessing(task);
        try
        {
            Map body = postHouseIndex(task, house, task.getAction());
            markTaskSuccess(task, house);
            result.put("success", true);
            result.put("message", "索引任务处理完成");
            result.put("aiResult", body);
            return result;
        }
        catch (Exception e)
        {
            String message = safeError(e);
            markTaskFailed(task, message);
            if (house != null)
            {
                updateHouseIndexStatus(house.getHouseId(), "2");
            }
            result.put("success", false);
            result.put("message", message);
            return result;
        }
    }

    private Map<String, Object> processFullHouseSyncTask(AiVectorIndexTask task, Map<String, Object> result)
    {
        markTaskProcessing(task);
        List<RentalHouse> houses = rentalHouseService.selectPublicRentalHouseList(new RentalHouse());
        int successCount = 0;
        int failedCount = 0;
        List<Map<String, Object>> itemResults = houses.stream().map(house -> {
            Map<String, Object> item = new HashMap<>();
            item.put("houseId", house.getHouseId());
            item.put("title", house.getTitle());
            try
            {
                Map body = postHouseIndex(task, house, "upsert");
                updateHouseIndexStatus(house.getHouseId(), "1");
                item.put("success", true);
                item.put("aiResult", body);
            }
            catch (Exception e)
            {
                updateHouseIndexStatus(house.getHouseId(), "2");
                item.put("success", false);
                item.put("message", safeError(e));
            }
            return item;
        }).collect(Collectors.toList());

        for (Map<String, Object> item : itemResults)
        {
            if (Boolean.TRUE.equals(item.get("success")))
            {
                successCount++;
            }
            else
            {
                failedCount++;
            }
        }

        result.put("success", failedCount == 0);
        result.put("total", houses.size());
        result.put("successCount", successCount);
        result.put("failedCount", failedCount);
        result.put("items", itemResults.stream().limit(20).collect(Collectors.toList()));
        if (failedCount == 0)
        {
            markTaskSuccess(task, null);
            result.put("message", "全量房源索引同步完成");
        }
        else
        {
            markTaskFailed(task, "全量同步完成，失败 " + failedCount + " 套房源");
            result.put("message", "全量同步完成，部分房源失败");
        }
        return result;
    }

    private Map postHouseIndex(AiVectorIndexTask task, RentalHouse house, String action)
    {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", task.getTaskId());
        payload.put("houseId", house == null ? task.getSourceId() : house.getHouseId());
        payload.put("action", action);
        payload.put("document", house == null ? Collections.emptyMap() : buildHouseDocument(house));

        ResponseEntity<Map> response = restTemplate.postForEntity(aiBaseUrl + "/api/v1/index/house", payload, Map.class);
        Map body = response.getBody();
        if (body != null && Boolean.FALSE.equals(body.get("success")))
        {
            throw new IllegalStateException(String.valueOf(body.getOrDefault("message", "AI 索引服务处理失败")));
        }
        return body;
    }

    private AjaxResult forward(String path, Map<String, Object> payload)
    {
        try
        {
            ResponseEntity<Map> response = restTemplate.postForEntity(aiBaseUrl + path, payload, Map.class);
            return AjaxResult.success(response.getBody());
        }
        catch (Exception e)
        {
            return AjaxResult.error("智能体服务暂不可用，请稍后再试");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMap(Map<String, Object> request, String key)
    {
        if (request == null || !(request.get(key) instanceof Map))
        {
            return null;
        }
        return (Map<String, Object>) request.get(key);
    }

    private Map<String, Object> buildAssistantMetadata(Map body, Map<String, Object> context)
    {
        Map<String, Object> metadata = new HashMap<>();
        if (body != null)
        {
            metadata.put("intent", body.get("intent"));
            metadata.put("intentLabel", body.get("intentLabel"));
            metadata.put("toolCalls", body.get("toolCalls"));
            metadata.put("collaboration", body.get("collaboration"));
        }
        metadata.put("contextSnapshot", context);
        return metadata;
    }

    private Map<String, Object> enrichRequest(Map<String, Object> request)
    {
        Map<String, Object> payload = request == null ? new HashMap<>() : new HashMap<>(request);
        try
        {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            SysUser user = loginUser.getUser();
            List<SysRole> roles = user == null || user.getRoles() == null ? Collections.emptyList() : user.getRoles();
            payload.put("userId", loginUser.getUserId());
            payload.put("username", loginUser.getUsername());
            payload.put("roles", roles.stream()
                    .map(SysRole::getRoleKey)
                    .collect(Collectors.toList()));
            payload.put("isAdmin", SecurityUtils.isAdmin(loginUser.getUserId()));
        }
        catch (Exception e)
        {
            payload.putIfAbsent("role", "anonymous");
        }
        return payload;
    }

    private Map<String, Object> buildHouseDocument(RentalHouse house)
    {
        Map<String, Object> document = new HashMap<>();
        document.put("houseId", house.getHouseId());
        document.put("ownerId", house.getOwnerId());
        document.put("agentId", house.getAgentId());
        document.put("title", house.getTitle());
        document.put("city", house.getCity());
        document.put("district", house.getDistrict());
        document.put("street", house.getStreet());
        document.put("community", house.getCommunity());
        document.put("address", house.getAddress());
        document.put("longitude", house.getLongitude());
        document.put("latitude", house.getLatitude());
        document.put("location", house.getLongitude() == null || house.getLatitude() == null ? null
                : house.getLongitude().stripTrailingZeros().toPlainString() + ","
                        + house.getLatitude().stripTrailingZeros().toPlainString());
        document.put("rentAmount", house.getRentAmount());
        document.put("depositAmount", house.getDepositAmount());
        document.put("area", house.getArea());
        document.put("roomCount", house.getRoomCount());
        document.put("hallCount", house.getHallCount());
        document.put("toiletCount", house.getToiletCount());
        document.put("orientation", house.getOrientation());
        document.put("rentType", house.getRentType());
        document.put("decoration", house.getDecoration());
        document.put("facilities", house.getFacilities());
        document.put("tags", house.getTags());
        document.put("description", house.getDescription());
        document.put("status", house.getStatus());
        document.put("auditStatus", house.getAuditStatus());
        document.put("content", buildHouseContent(house));
        return document;
    }

    private boolean canViewIndexTask(AiVectorIndexTask task)
    {
        if (task == null)
        {
            return false;
        }
        if (hasAdminRole())
        {
            return true;
        }
        if (task.getSourceId() == null || task.getSourceId() <= 0)
        {
            return false;
        }
        RentalHouse house = rentalHouseService.selectRentalHouseByHouseId(task.getSourceId());
        return canOperateHouseAi(house);
    }

    private boolean isFullSyncTask(AiVectorIndexTask task)
    {
        return "house".equals(task.getSourceType())
                && "sync".equals(task.getAction())
                && (task.getSourceId() == null || task.getSourceId() <= 0);
    }

    private boolean canOperateHouseAi(RentalHouse house)
    {
        if (house == null)
        {
            return false;
        }
        if (hasAdminRole())
        {
            return true;
        }
        Long userId = SecurityUtils.getUserId();
        if (hasExactRole("owner") && userId.equals(house.getOwnerId()))
        {
            return true;
        }
        return hasExactRole("agent") && userId.equals(house.getAgentId());
    }

    private boolean hasExactRole(String expectedRole)
    {
        try
        {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            SysUser user = loginUser.getUser();
            List<SysRole> roles = user == null || user.getRoles() == null ? Collections.emptyList() : user.getRoles();
            return roles.stream().anyMatch(role -> expectedRole.equals(role.getRoleKey()));
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private boolean hasAdminRole()
    {
        try
        {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            return SecurityUtils.isAdmin(loginUser.getUserId()) || hasExactRole("admin");
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private String buildHouseContent(RentalHouse house)
    {
        StringBuilder content = new StringBuilder();
        appendContent(content, "标题", house.getTitle());
        appendContent(content, "城市", house.getCity());
        appendContent(content, "区域", house.getDistrict());
        appendContent(content, "小区", house.getCommunity());
        appendContent(content, "地址", house.getAddress());
        appendContent(content, "经度", house.getLongitude());
        appendContent(content, "纬度", house.getLatitude());
        appendContent(content, "租金", house.getRentAmount());
        appendContent(content, "押金", house.getDepositAmount());
        appendContent(content, "面积", house.getArea());
        appendContent(content, "户型", house.getRoomCount() == null ? null
                : house.getRoomCount() + "室" + nullToZero(house.getHallCount()) + "厅" + nullToZero(house.getToiletCount()) + "卫");
        appendContent(content, "朝向", house.getOrientation());
        appendContent(content, "出租方式", house.getRentType());
        appendContent(content, "装修", house.getDecoration());
        appendContent(content, "设施", house.getFacilities());
        appendContent(content, "标签", house.getTags());
        appendContent(content, "描述", house.getDescription());
        return content.toString().trim();
    }

    private void appendContent(StringBuilder content, String label, Object value)
    {
        if (value == null || String.valueOf(value).isBlank())
        {
            return;
        }
        content.append(label).append("：").append(value).append("\n");
    }

    private long nullToZero(Long value)
    {
        return value == null ? 0L : value;
    }

    private void markTaskProcessing(AiVectorIndexTask task)
    {
        AiVectorIndexTask update = new AiVectorIndexTask();
        update.setTaskId(task.getTaskId());
        update.setStatus("1");
        update.setErrorMsg("索引任务处理中");
        aiVectorIndexTaskService.updateAiVectorIndexTask(update);
    }

    private void markTaskSuccess(AiVectorIndexTask task, RentalHouse house)
    {
        AiVectorIndexTask update = new AiVectorIndexTask();
        update.setTaskId(task.getTaskId());
        update.setStatus("2");
        update.setErrorMsg("索引任务处理完成");
        aiVectorIndexTaskService.updateAiVectorIndexTask(update);
        if (house != null)
        {
            updateHouseIndexStatus(house.getHouseId(), "delete".equals(task.getAction()) ? "0" : "1");
        }
    }

    private void markTaskFailed(AiVectorIndexTask task, String message)
    {
        AiVectorIndexTask update = new AiVectorIndexTask();
        update.setTaskId(task.getTaskId());
        update.setStatus("3");
        update.setRetryCount((task.getRetryCount() == null ? 0L : task.getRetryCount()) + 1);
        update.setErrorMsg(limitMessage(message));
        aiVectorIndexTaskService.updateAiVectorIndexTask(update);
    }

    private void updateHouseIndexStatus(Long houseId, String status)
    {
        RentalHouse update = new RentalHouse();
        update.setHouseId(houseId);
        update.setAiIndexStatus(status);
        rentalHouseService.updateRentalHouse(update);
    }

    private String safeError(Exception e)
    {
        String message = e.getMessage();
        if (message == null || message.isBlank())
        {
            message = e.getClass().getSimpleName();
        }
        return limitMessage(message);
    }

    private String limitMessage(String message)
    {
        if (message == null)
        {
            return null;
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private Long parseLong(Object value)
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

    private String buildDocumentMessage(String indexStatus, AiVectorIndexTask latestTask)
    {
        if ("1".equals(indexStatus))
        {
            return "房源文档已完成索引，可用于后续检索和推荐。";
        }
        if ("2".equals(indexStatus))
        {
            return "房源索引曾失败，请检查最近一次任务记录。";
        }
        if (latestTask != null)
        {
            return "房源已有索引任务记录，当前仍待后续处理。";
        }
        return "房源尚未进入索引流程。";
    }
}
