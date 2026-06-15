package com.ruoyi.web.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.RentalHouse;

@Service
public class RentalAmapService
{
    private static final String AMAP_BASE_URL = "https://restapi.amap.com";
    private static final String AMAP_POI_AROUND_PATH = "/v5/place/around";
    private static final int POI_PAGE_SIZE = 25;
    private static final int POI_MAX_PER_QUERY = 200;
    private static final int DEFAULT_NEARBY_RADIUS = 1500;
    private static final String DEFAULT_NEARBY_KEYWORDS = "地铁站|公交站|超市|商场|便利店|医院|药店|学校|幼儿园";

    private final RestTemplate restTemplate;

    @Value("${amap.api-key:}")
    private String apiKey;

    public RentalAmapService(RestTemplate restTemplate)
    {
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> buildHouseContext(RentalHouse house, String destination, String mode)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("configured", configured());
        if (!configured())
        {
            result.put("summary", "高德地图 Key 未配置");
            result.put("nearbyGroups", new ArrayList<>());
            return result;
        }
        if (house == null)
        {
            result.put("summary", "房源不存在或不可访问");
            result.put("nearbyGroups", new ArrayList<>());
            return result;
        }

        String address = houseAddress(house);
        Map<String, Object> houseLocation = resolveHouseLocation(house, address);
        String location = text(houseLocation.get("location"));
        result.put("address", address);
        result.put("houseLocation", houseLocation);
        List<Map<String, Object>> groups = nearbyGroups(location, house.getCity());
        result.put("nearbyGroups", groups);
        result.put("nearbySummary", nearbySummary(groups));

        if (StringUtils.isNotEmpty(destination) && StringUtils.isNotEmpty(location))
        {
            Map<String, Object> destinationLocation = geocode(destination, house.getCity());
            result.put("destinationLocation", destinationLocation);
            result.put("route", route(location, text(destinationLocation.get("location")), house.getCity(), mode));
        }
        result.put("summary", StringUtils.isNotEmpty(location) ? "已读取高德通勤与周边数据" : "房源坐标缺失，周边查询不可用");
        return result;
    }

    public Map<String, Object> searchAround(String location, String keywords, String city, Integer radius, Integer limit)
    {
        Map<String, Object> result = baseResult(false, "缺少坐标或高德配置");
        if (!configured() || StringUtils.isEmpty(location))
        {
            result.put("pois", new ArrayList<>());
            return result;
        }
        int searchRadius = normalizePositive(radius, DEFAULT_NEARBY_RADIUS);
        int maxResults = Math.min(normalizePositive(limit, POI_MAX_PER_QUERY), POI_MAX_PER_QUERY);
        String searchKeywords = defaultText(keywords, DEFAULT_NEARBY_KEYWORDS);
        List<Map<String, Object>> pois = fetchAroundPois(location, searchKeywords, city, searchRadius, maxResults);
        result = baseResult(true, "周边搜索完成，已获取 " + pois.size() + " 条 POI");
        result.put("location", location);
        result.put("keywords", searchKeywords);
        result.put("city", city);
        result.put("radius", searchRadius);
        result.put("maxResults", maxResults);
        result.put("fetched", pois.size());
        result.put("pois", poiSummaries(pois));
        result.put("source", "amap-webapi-v5-place-around");
        return result;
    }

    public Map<String, Object> geocode(String address, String city)
    {
        Map<String, Object> fallback = baseResult(false, "缺少地址或高德配置");
        if (!configured() || StringUtils.isEmpty(address))
        {
            return fallback;
        }
        Map<String, String> params = new HashMap<>();
        params.put("address", address);
        params.put("city", city);
        Map<String, Object> data = call("/v3/geocode/geo", params);
        List<Map<String, Object>> geocodes = list(data.get("geocodes"));
        if (geocodes.isEmpty())
        {
            return baseResult(false, "未解析到坐标");
        }
        Map<String, Object> first = geocodes.get(0);
        Map<String, Object> result = baseResult(true, "已解析地址坐标");
        result.put("address", first.get("formatted_address"));
        result.put("location", first.get("location"));
        result.put("city", first.get("city"));
        result.put("district", first.get("district"));
        fillCoordinate(result, text(first.get("location")));
        return result;
    }

    private Map<String, Object> resolveHouseLocation(RentalHouse house, String address)
    {
        if (house.getLongitude() != null && house.getLatitude() != null)
        {
            Map<String, Object> result = baseResult(true, "使用房源坐标");
            result.put("address", address);
            result.put("location", coordinate(house.getLongitude(), house.getLatitude()));
            result.put("longitude", house.getLongitude());
            result.put("latitude", house.getLatitude());
            result.put("city", house.getCity());
            result.put("district", house.getDistrict());
            return result;
        }
        return geocode(address, house.getCity());
    }

    private List<Map<String, Object>> nearbyGroups(String location, String city)
    {
        List<Map<String, Object>> groups = new ArrayList<>();
        if (StringUtils.isEmpty(location))
        {
            return groups;
        }
        groups.add(nearbyGroup("交通", "地铁站|公交车站|公交站", location, city));
        groups.add(nearbyGroup("生活", "超市|商场|便利店|菜市场", location, city));
        groups.add(nearbyGroup("医疗", "医院|药店|诊所", location, city));
        groups.add(nearbyGroup("教育", "学校|幼儿园|培训机构", location, city));
        return groups;
    }

    private Map<String, Object> nearbyGroup(String label, String keywords, String location, String city)
    {
        List<Map<String, Object>> pois = fetchAroundPois(location, keywords, city, DEFAULT_NEARBY_RADIUS, POI_MAX_PER_QUERY);
        Map<String, Object> group = new LinkedHashMap<>();
        group.put("label", label);
        group.put("keywords", keywords);
        group.put("radius", DEFAULT_NEARBY_RADIUS);
        group.put("pageSize", POI_PAGE_SIZE);
        group.put("maxResults", POI_MAX_PER_QUERY);
        group.put("fetched", pois.size());
        group.put("truncated", pois.size() >= POI_MAX_PER_QUERY);
        group.put("pois", poiSummaries(pois));
        return group;
    }

    private List<Map<String, Object>> fetchAroundPois(String location, String keywords, String city, int radius, int limit)
    {
        List<Map<String, Object>> result = new ArrayList<>();
        int maxResults = Math.min(Math.max(limit, 1), POI_MAX_PER_QUERY);
        int pageCount = (int) Math.ceil((double) maxResults / POI_PAGE_SIZE);
        for (int pageNum = 1; pageNum <= pageCount && result.size() < maxResults; pageNum++)
        {
            Map<String, String> params = new HashMap<>();
            params.put("keywords", keywords);
            params.put("location", location);
            params.put("radius", String.valueOf(radius));
            params.put("sortrule", "distance");
            params.put("region", city);
            params.put("show_fields", "business");
            params.put("page_size", String.valueOf(POI_PAGE_SIZE));
            params.put("page_num", String.valueOf(pageNum));
            Map<String, Object> data = call(AMAP_POI_AROUND_PATH, params);
            List<Map<String, Object>> pois = list(data.get("pois"));
            if (pois.isEmpty())
            {
                break;
            }
            for (Map<String, Object> poi : pois)
            {
                result.add(poi);
                if (result.size() >= maxResults)
                {
                    break;
                }
            }
            if (pois.size() < POI_PAGE_SIZE)
            {
                break;
            }
        }
        return result;
    }

    private Map<String, Object> route(String origin, String destination, String city, String mode)
    {
        if (StringUtils.isEmpty(origin) || StringUtils.isEmpty(destination))
        {
            return baseResult(false, "缺少起终点坐标");
        }
        String routeMode = normalizeMode(mode);
        Map<String, String> params = new HashMap<>();
        params.put("origin", origin);
        params.put("destination", destination);
        params.put("city", city);

        String path = "transit".equals(routeMode) ? "/v3/direction/transit/integrated"
                : "driving".equals(routeMode) ? "/v3/direction/driving" : "/v3/direction/walking";
        Map<String, Object> data = call(path, params);
        Map<String, Object> route = parseRoute(data, routeMode);
        route.put("mode", routeMode);
        return route;
    }

    private Map<String, Object> parseRoute(Map<String, Object> data, String mode)
    {
        Map<String, Object> route = baseResult(false, "未查询到路线");
        Map<String, Object> routeData = map(data.get("route"));
        if ("transit".equals(mode))
        {
            List<Map<String, Object>> transits = list(routeData.get("transits"));
            if (transits.isEmpty())
            {
                return route;
            }
            Map<String, Object> first = transits.get(0);
            route = baseResult(true, "已生成公交通勤方案");
            route.put("distance", first.get("distance"));
            route.put("duration", first.get("duration"));
            route.put("cost", first.get("cost"));
            route.put("walkingDistance", first.get("walking_distance"));
            route.put("summary", "公交约 " + minutes(first.get("duration")) + " 分钟");
            return route;
        }
        List<Map<String, Object>> paths = list(routeData.get("paths"));
        if (paths.isEmpty())
        {
            return route;
        }
        Map<String, Object> first = paths.get(0);
        route = baseResult(true, "已生成路线方案");
        route.put("distance", first.get("distance"));
        route.put("duration", first.get("duration"));
        route.put("summary", ("driving".equals(mode) ? "驾车" : "步行") + "约 " + minutes(first.get("duration")) + " 分钟，"
                + kilometers(first.get("distance")) + " 公里");
        return route;
    }

    private Map<String, Object> call(String path, Map<String, String> params)
    {
        try
        {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(AMAP_BASE_URL + path)
                    .queryParam("key", apiKey)
                    .queryParam("output", "json");
            params.forEach((key, value) -> {
                if (StringUtils.isNotEmpty(value))
                {
                    builder.queryParam(key, value);
                }
            });
            Map<String, Object> data = restTemplate.getForObject(builder.build().encode().toUri(), Map.class);
            if (data == null)
            {
                return new HashMap<>();
            }
            return data;
        }
        catch (Exception e)
        {
            return new HashMap<>();
        }
    }

    private List<Map<String, Object>> poiSummaries(List<Map<String, Object>> pois)
    {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> poi : pois)
        {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", poi.get("name"));
            item.put("type", poi.get("type"));
            item.put("address", poi.get("address"));
            item.put("distance", poi.get("distance"));
            item.put("location", poi.get("location"));
            item.put("pname", poi.get("pname"));
            item.put("cityname", poi.get("cityname"));
            item.put("adname", poi.get("adname"));
            item.put("tel", poi.get("tel"));
            item.put("businessArea", poi.get("business_area"));
            result.add(item);
        }
        return result;
    }

    private Map<String, Object> nearbySummary(List<Map<String, Object>> groups)
    {
        Map<String, Object> summary = new LinkedHashMap<>();
        int total = 0;
        List<String> highlights = new ArrayList<>();
        for (Map<String, Object> group : groups)
        {
            int fetched = parseInt(group.get("fetched"), 0);
            total += fetched;
            List<Map<String, Object>> pois = list(group.get("pois"));
            if (!pois.isEmpty())
            {
                Map<String, Object> first = pois.get(0);
                highlights.add(group.get("label") + "最近：" + text(first.get("name")) + "，约" + text(first.get("distance")) + "米");
            }
        }
        summary.put("totalFetched", total);
        summary.put("maxPerQuery", POI_MAX_PER_QUERY);
        summary.put("radius", DEFAULT_NEARBY_RADIUS);
        summary.put("highlights", highlights);
        return summary;
    }

    private String houseAddress(RentalHouse house)
    {
        return join(house.getCity(), house.getDistrict(), house.getStreet(), house.getCommunity(), house.getAddress());
    }

    private boolean configured()
    {
        return StringUtils.isNotEmpty(apiKey);
    }

    private String coordinate(BigDecimal longitude, BigDecimal latitude)
    {
        return longitude.stripTrailingZeros().toPlainString() + "," + latitude.stripTrailingZeros().toPlainString();
    }

    private String normalizeMode(String mode)
    {
        if ("driving".equals(mode) || "transit".equals(mode))
        {
            return mode;
        }
        return "walking";
    }

    private String minutes(Object seconds)
    {
        BigDecimal value = decimal(seconds);
        if (value == null)
        {
            return "-";
        }
        return value.divide(BigDecimal.valueOf(60), 0, RoundingMode.UP).toPlainString();
    }

    private String kilometers(Object meters)
    {
        BigDecimal value = decimal(meters);
        if (value == null)
        {
            return "-";
        }
        return value.divide(BigDecimal.valueOf(1000), 1, RoundingMode.HALF_UP).toPlainString();
    }

    private BigDecimal decimal(Object value)
    {
        if (value == null || StringUtils.isEmpty(String.valueOf(value)))
        {
            return null;
        }
        return new BigDecimal(String.valueOf(value));
    }

    private int normalizePositive(Integer value, int fallback)
    {
        return value == null || value <= 0 ? fallback : value;
    }

    private int parseInt(Object value, int fallback)
    {
        try
        {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        }
        catch (NumberFormatException e)
        {
            return fallback;
        }
    }

    private void fillCoordinate(Map<String, Object> result, String location)
    {
        if (StringUtils.isEmpty(location) || !location.contains(","))
        {
            return;
        }
        String[] parts = location.split(",", 2);
        try
        {
            result.put("longitude", new BigDecimal(parts[0]));
            result.put("latitude", new BigDecimal(parts[1]));
        }
        catch (NumberFormatException ignored)
        {
        }
    }

    private Map<String, Object> baseResult(boolean success, String summary)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", success);
        result.put("summary", summary);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value)
    {
        return value instanceof Map ? (Map<String, Object>) value : new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> list(Object value)
    {
        return value instanceof List ? (List<Map<String, Object>>) value : new ArrayList<>();
    }

    private String text(Object value)
    {
        return value == null ? null : String.valueOf(value).trim();
    }

    private String defaultText(String value, String fallback)
    {
        return StringUtils.isEmpty(value) ? fallback : value;
    }

    private String join(String... values)
    {
        StringBuilder builder = new StringBuilder();
        for (String value : values)
        {
            if (StringUtils.isNotEmpty(value) && !builder.toString().contains(value))
            {
                builder.append(value);
            }
        }
        return builder.toString();
    }
}
