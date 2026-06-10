package com.ruoyi.web.controller.rental;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import com.ruoyi.common.core.domain.AjaxResult;

@RestController
@RequestMapping("/rental/ai")
public class RentalAiController
{
    private final RestTemplate restTemplate;

    @Value("${ai.service.base-url}")
    private String aiBaseUrl;

    public RentalAiController(RestTemplate restTemplate)
    {
        this.restTemplate = restTemplate;
    }

    @PostMapping("/chat")
    public AjaxResult chat(@RequestBody Map<String, Object> request)
    {
        ResponseEntity<Map> response = restTemplate.postForEntity(aiBaseUrl + "/api/v1/agent/chat", request, Map.class);
        return AjaxResult.success(response.getBody());
    }

    @PostMapping("/recommend")
    public AjaxResult recommend(@RequestBody Map<String, Object> request)
    {
        ResponseEntity<Map> response = restTemplate.postForEntity(aiBaseUrl + "/api/v1/agent/recommend", request, Map.class);
        return AjaxResult.success(response.getBody());
    }
}
