package com.ruoyi.web.controller.rental;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.AiToolAuditLog;
import com.ruoyi.system.domain.AiUserMemory;
import com.ruoyi.system.domain.RentalAppointment;
import com.ruoyi.system.domain.RentalContract;
import com.ruoyi.system.domain.RentalHouse;
import com.ruoyi.system.domain.RentalIntention;
import com.ruoyi.system.domain.vo.RentalContractDetailVo;
import com.ruoyi.system.service.IAiToolAuditLogService;
import com.ruoyi.system.service.IAiUserMemoryService;
import com.ruoyi.system.service.IRentalAppointmentService;
import com.ruoyi.system.service.IRentalContractService;
import com.ruoyi.system.service.IRentalHouseFavoriteService;
import com.ruoyi.system.service.IRentalHouseService;
import com.ruoyi.system.service.IRentalIntentionService;

/**
 * Internal AI tool boundary. The Python agent calls this controller only with a
 * shared service token; all real rental reads/writes remain in Java services.
 */
@RestController
@RequestMapping("/rental/ai/tools")
public class RentalAiToolController
{
    private static final String TOKEN_HEADER = "X-AI-Tool-Token";

    @Value("${ai.service.internal-token:smart-rental-ai-internal}")
    private String internalToken;

    @Autowired
    private IRentalHouseService rentalHouseService;

    @Autowired
    private IRentalHouseFavoriteService rentalHouseFavoriteService;

    @Autowired
    private IRentalAppointmentService rentalAppointmentService;

    @Autowired
    private IRentalIntentionService rentalIntentionService;

    @Autowired
    private IRentalContractService rentalContractService;

    @Autowired
    private IAiUserMemoryService aiUserMemoryService;

    @Autowired
    private IAiToolAuditLogService aiToolAuditLogService;

    @PostMapping("/houses/search")
    public AjaxResult searchHouses(@RequestHeader(value = TOKEN_HEADER, required = false) String token,
            @RequestBody Map<String, Object> request)
    {
        if (!authorized(token))
        {
            return AjaxResult.error("AI 工具令牌无效");
        }
        long started = System.currentTimeMillis();
        try
        {
            RentalHouse query = new RentalHouse();
            String city = text(request.get("city"));
            if (StringUtils.isNotEmpty(city))
            {
                query.setCity(city);
            }
            List<RentalHouse> houses = rentalHouseService.selectPublicRentalHouseList(query);
            BigDecimal maxRent = decimal(request.get("maxRent"));
            String keyword = text(request.get("query"));
            List<Map<String, Object>> matches = houses.stream()
                    .filter(house -> maxRent == null || house.getRentAmount() == null
                            || house.getRentAmount().compareTo(maxRent) <= 0)
                    .filter(house -> matchesKeyword(house, keyword))
                    .limit(8)
                    .map(this::houseSummary)
                    .collect(Collectors.toList());
            Map<String, Object> data = new HashMap<>();
            data.put("success", true);
            data.put("matches", matches);
            data.put("summary", matches.isEmpty() ? "未查询到符合条件的公开房源"
                    : "查询到 " + matches.size() + " 套公开房源");
            audit(request, "search_houses", true, data, null, started);
            return AjaxResult.success(data);
        }
        catch (Exception e)
        {
            audit(request, "search_houses", false, null, e, started);
            return AjaxResult.error(safeError(e));
        }
    }

    @PostMapping("/houses/detail")
    public AjaxResult houseDetail(@RequestHeader(value = TOKEN_HEADER, required = false) String token,
            @RequestBody Map<String, Object> request)
    {
        if (!authorized(token))
        {
            return AjaxResult.error("AI 工具令牌无效");
        }
        long started = System.currentTimeMillis();
        try
        {
            Long houseId = number(request.get("houseId"));
            Long userId = number(request.get("userId"));
            RentalHouse house = rentalHouseService.selectRentalHouseDetail(houseId, userId, false);
            Map<String, Object> data = new HashMap<>(houseSummary(house));
            data.put("description", house.getDescription());
            data.put("facilities", house.getFacilities());
            data.put("tags", house.getTags());
            data.put("imageUrls", house.getImageUrls());
            data.put("success", true);
            data.put("summary", "已读取房源详情");
            audit(request, "get_house_detail", true, data, null, started);
            return AjaxResult.success(data);
        }
        catch (Exception e)
        {
            audit(request, "get_house_detail", false, null, e, started);
            return AjaxResult.error(safeError(e));
        }
    }

    @PostMapping("/contracts/detail")
    public AjaxResult contractDetail(@RequestHeader(value = TOKEN_HEADER, required = false) String token,
            @RequestBody Map<String, Object> request)
    {
        if (!authorized(token))
        {
            return AjaxResult.error("AI 工具令牌无效");
        }
        long started = System.currentTimeMillis();
        try
        {
            Long contractId = number(request.get("contractId"));
            Long userId = number(request.get("userId"));
            RentalContractDetailVo detail = rentalContractService.selectRentalContractDetail(contractId, userId, false);
            RentalContract contract = detail == null ? null : detail.getContract();
            if (contract == null)
            {
                throw new ServiceException("合同不存在或不可见");
            }
            Map<String, Object> data = new HashMap<>();
            data.put("success", true);
            data.put("contractId", contract.getContractId());
            data.put("houseId", contract.getHouseId());
            data.put("contractNo", contract.getContractNo());
            data.put("startDate", contract.getStartDate());
            data.put("endDate", contract.getEndDate());
            data.put("rentAmount", contract.getRentAmount());
            data.put("depositAmount", contract.getDepositAmount());
            data.put("paymentCycle", contract.getPaymentCycle());
            data.put("contractContent", contract.getContractContent());
            data.put("aiRiskSummary", contract.getAiRiskSummary());
            data.put("status", contract.getStatus());
            data.put("confirms", detail.getConfirms());
            data.put("summary", "已读取合同详情");
            audit(request, "get_contract", true, data, null, started);
            return AjaxResult.success(data);
        }
        catch (Exception e)
        {
            audit(request, "get_contract", false, null, e, started);
            return AjaxResult.error(safeError(e));
        }
    }

    @PostMapping("/memory/save")
    public AjaxResult saveMemory(@RequestHeader(value = TOKEN_HEADER, required = false) String token,
            @RequestBody Map<String, Object> request)
    {
        if (!authorized(token))
        {
            return AjaxResult.error("AI 工具令牌无效");
        }
        long started = System.currentTimeMillis();
        try
        {
            Long userId = number(request.get("userId"));
            String content = limit(text(request.get("content")), 2000);
            if (userId == null || StringUtils.isEmpty(content))
            {
                throw new ServiceException("缺少用户或记忆内容");
            }
            AiUserMemory memory = new AiUserMemory();
            memory.setUserId(userId);
            memory.setRole(defaultText(text(request.get("role")), "tenant"));
            memory.setMemoryType(defaultText(text(request.get("memoryType")), "summary"));
            memory.setContent(content);
            memory.setImportance(decimalOrDefault(request.get("importance"), new BigDecimal("0.50")));
            memory.setVectorStatus("0");
            aiUserMemoryService.insertAiUserMemory(memory);

            Map<String, Object> data = new HashMap<>();
            data.put("success", true);
            data.put("memoryId", memory.getMemoryId());
            data.put("summary", "长期记忆已保存，等待向量化任务处理");
            audit(request, "save_memory", true, data, null, started);
            return AjaxResult.success(data);
        }
        catch (Exception e)
        {
            audit(request, "save_memory", false, null, e, started);
            return AjaxResult.error(safeError(e));
        }
    }

    @PostMapping("/actions/execute")
    public AjaxResult executeAction(@RequestHeader(value = TOKEN_HEADER, required = false) String token,
            @RequestBody Map<String, Object> request)
    {
        if (!authorized(token))
        {
            return AjaxResult.error("AI 工具令牌无效");
        }
        long started = System.currentTimeMillis();
        String action = text(request.get("action"));
        try
        {
            if (!allowedAction(action))
            {
                throw new ServiceException("动作不在 AI 白名单内");
            }
            if (requiresConfirmation(action) && !Boolean.TRUE.equals(request.get("confirmed")))
            {
                Map<String, Object> data = new HashMap<>();
                data.put("success", false);
                data.put("action", action);
                data.put("requiresConfirmation", true);
                data.put("summary", "该动作需要用户确认后执行");
                audit(request, action, false, data, null, started);
                return AjaxResult.success(data);
            }
            Map<String, Object> payload = map(request.get("payload"));
            Map<String, Object> data = dispatchAction(action, request, payload);
            audit(request, action, true, data, null, started);
            return AjaxResult.success(data);
        }
        catch (Exception e)
        {
            audit(request, action, false, null, e, started);
            return AjaxResult.error(safeError(e));
        }
    }

    private Map<String, Object> dispatchAction(String action, Map<String, Object> request, Map<String, Object> payload)
    {
        if ("estimate_monthly_cost".equals(action))
        {
            BigDecimal rent = decimalOrDefault(payload.get("rentAmount"), BigDecimal.ZERO);
            BigDecimal deposit = decimalOrDefault(payload.get("depositAmount"), rent);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("action", action);
            result.put("monthlyRent", rent);
            result.put("deposit", deposit);
            result.put("firstMonthEstimated", rent.add(deposit));
            result.put("summary", "已估算首月租住成本");
            return result;
        }
        if ("favorite_house".equals(action))
        {
            Long userId = number(request.get("userId"));
            Long houseId = number(payload.get("houseId"));
            rentalHouseFavoriteService.favoriteHouse(userId, houseId);
            return successAction(action, "房源已收藏", "houseId", houseId);
        }
        if ("create_appointment".equals(action))
        {
            Long userId = number(request.get("userId"));
            RentalAppointment appointment = new RentalAppointment();
            appointment.setHouseId(number(payload.get("houseId")));
            appointment.setAppointmentTime(date(payload.get("appointmentTime")));
            appointment.setContactName(text(payload.get("contactName")));
            appointment.setContactPhone(text(payload.get("contactPhone")));
            appointment.setMessage(text(payload.get("message")));
            appointment.setSource("1");
            rentalAppointmentService.createTenantAppointment(appointment, userId);
            return successAction(action, "看房预约已创建", "appointmentId", appointment.getAppointmentId());
        }
        if ("create_intention".equals(action))
        {
            Long userId = number(request.get("userId"));
            RentalIntention intention = new RentalIntention();
            intention.setHouseId(number(payload.get("houseId")));
            intention.setBudgetAmount(decimal(payload.get("budgetAmount")));
            intention.setExpectedMoveIn(date(payload.get("expectedMoveIn")));
            intention.setIntentionLevel(defaultText(text(payload.get("intentionLevel")), "2"));
            intention.setNote(text(payload.get("note")));
            intention.setAiSummary(text(payload.get("aiSummary")));
            rentalIntentionService.createTenantIntention(intention, userId);
            return successAction(action, "租赁意向已创建", "intentionId", intention.getIntentionId());
        }
        if ("view_contract".equals(action))
        {
            Long userId = number(request.get("userId"));
            Long contractId = number(payload.get("contractId"));
            RentalContractDetailVo detail = rentalContractService.selectRentalContractDetail(contractId, userId, false);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("action", action);
            result.put("contract", detail == null ? null : detail.getContract());
            result.put("confirms", detail == null ? new ArrayList<>() : detail.getConfirms());
            result.put("summary", "已读取合同");
            return result;
        }
        throw new ServiceException("不支持的 AI 动作");
    }

    private Map<String, Object> successAction(String action, String summary, String idKey, Object idValue)
    {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("action", action);
        result.put(idKey, idValue);
        result.put("summary", summary);
        return result;
    }

    private boolean authorized(String token)
    {
        return StringUtils.isNotEmpty(internalToken) && internalToken.equals(token);
    }

    private boolean allowedAction(String action)
    {
        return List.of("create_appointment", "favorite_house", "create_intention", "estimate_monthly_cost",
                "view_contract").contains(action);
    }

    private boolean requiresConfirmation(String action)
    {
        return List.of("create_appointment", "create_intention", "view_contract").contains(action);
    }

    private boolean matchesKeyword(RentalHouse house, String keyword)
    {
        if (StringUtils.isEmpty(keyword))
        {
            return true;
        }
        String haystack = String.join(" ",
                nullToEmpty(house.getTitle()),
                nullToEmpty(house.getCity()),
                nullToEmpty(house.getDistrict()),
                nullToEmpty(house.getStreet()),
                nullToEmpty(house.getCommunity()),
                nullToEmpty(house.getTags()),
                nullToEmpty(house.getDescription()));
        return haystack.contains(keyword) || keyword.contains(nullToEmpty(house.getCity()))
                || keyword.contains(nullToEmpty(house.getDistrict()));
    }

    private Map<String, Object> houseSummary(RentalHouse house)
    {
        Map<String, Object> item = new HashMap<>();
        item.put("houseId", house.getHouseId());
        item.put("title", house.getTitle());
        item.put("city", house.getCity());
        item.put("district", house.getDistrict());
        item.put("street", house.getStreet());
        item.put("community", house.getCommunity());
        item.put("rentAmount", house.getRentAmount());
        item.put("depositAmount", house.getDepositAmount());
        item.put("area", house.getArea());
        item.put("roomCount", house.getRoomCount());
        item.put("hallCount", house.getHallCount());
        item.put("toiletCount", house.getToiletCount());
        item.put("orientation", house.getOrientation());
        item.put("rentType", house.getRentType());
        item.put("decoration", house.getDecoration());
        item.put("longitude", house.getLongitude());
        item.put("latitude", house.getLatitude());
        return item;
    }

    private void audit(Map<String, Object> request, String toolName, boolean success, Object result, Exception error,
            long started)
    {
        try
        {
            AiToolAuditLog log = new AiToolAuditLog();
            log.setRequestId(defaultText(text(request.get("requestId")), UUID.randomUUID().toString()));
            log.setSessionId(number(request.get("sessionId")));
            log.setUserId(number(request.get("userId")));
            log.setRole(text(request.get("role")));
            log.setToolName(toolName);
            log.setToolArgs(limit(String.valueOf(request), 2000));
            log.setToolResult(limit(String.valueOf(result), 2000));
            log.setSuccess(success ? "1" : "0");
            log.setErrorMsg(error == null ? null : limit(safeError(error), 500));
            log.setCostMs(System.currentTimeMillis() - started);
            aiToolAuditLogService.insertAiToolAuditLog(log);
        }
        catch (Exception ignored)
        {
        }
    }

    private String safeError(Exception e)
    {
        String message = e.getMessage();
        if (StringUtils.isEmpty(message))
        {
            message = e.getClass().getSimpleName();
        }
        return limit(message, 500);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value)
    {
        if (value instanceof Map)
        {
            return (Map<String, Object>) value;
        }
        return new HashMap<>();
    }

    private Long number(Object value)
    {
        if (value == null || StringUtils.isEmpty(String.valueOf(value)))
        {
            return null;
        }
        if (value instanceof Number)
        {
            return ((Number) value).longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private BigDecimal decimal(Object value)
    {
        if (value == null || StringUtils.isEmpty(String.valueOf(value)))
        {
            return null;
        }
        if (value instanceof BigDecimal)
        {
            return (BigDecimal) value;
        }
        return new BigDecimal(String.valueOf(value));
    }

    private BigDecimal decimalOrDefault(Object value, BigDecimal fallback)
    {
        BigDecimal parsed = decimal(value);
        return parsed == null ? fallback : parsed;
    }

    private Date date(Object value)
    {
        return value == null ? null : DateUtils.parseDate(value);
    }

    private String text(Object value)
    {
        return value == null ? null : String.valueOf(value).trim();
    }

    private String defaultText(String value, String fallback)
    {
        return StringUtils.isEmpty(value) ? fallback : value;
    }

    private String limit(String value, int maxLength)
    {
        if (value == null)
        {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private String nullToEmpty(String value)
    {
        return value == null ? "" : value;
    }
}
