import os
from typing import Any

import httpx

from app.core.guardrails import safe_call

AMAP_POI_MAX_RESULTS = 200
AMAP_POI_PAGE_SIZE = 25


def amap_enabled() -> bool:
    return bool(os.getenv("AMAP_API_KEY"))


def search_poi(
    keywords: str,
    city: str | None = None,
    location: str | None = None,
    radius: int = 1500,
    limit: int = AMAP_POI_MAX_RESULTS,
) -> dict[str, Any]:
    if not amap_enabled():
        raise RuntimeError("AMAP_API_KEY 未配置，无法查询高德周边")

    def call() -> dict[str, Any]:
        pois = _search_around(keywords, location, city, radius, limit) if location else _search_text(keywords, city)
        return {
            "enabled": True,
            "success": True,
            "source": "amap-webapi-v5-place-around" if location else "amap-webapi-v3-place-text",
            "summary": f"查询到 {len(pois)} 个周边点位",
            "keywords": keywords,
            "city": city,
            "location": location,
            "radius": radius,
            "pois": pois,
        }

    return safe_call(call, name="amap.poi")


def _search_around(keywords: str, location: str | None, city: str | None, radius: int, limit: int) -> list[dict[str, Any]]:
    max_results = min(max(int(limit or AMAP_POI_MAX_RESULTS), 1), AMAP_POI_MAX_RESULTS)
    page_count = (max_results + AMAP_POI_PAGE_SIZE - 1) // AMAP_POI_PAGE_SIZE
    pois: list[dict[str, Any]] = []
    for page_num in range(1, page_count + 1):
        response = httpx.get(
            "https://restapi.amap.com/v5/place/around",
            params={
                "key": os.getenv("AMAP_API_KEY"),
                "keywords": keywords,
                "region": city or "",
                "location": location or "",
                "radius": radius,
                "sortrule": "distance",
                "show_fields": "business",
                "page_size": AMAP_POI_PAGE_SIZE,
                "page_num": page_num,
                "output": "json",
            },
            timeout=6,
        )
        response.raise_for_status()
        page_pois = response.json().get("pois") or []
        if not page_pois:
            break
        pois.extend(page_pois[: max_results - len(pois)])
        if len(page_pois) < AMAP_POI_PAGE_SIZE or len(pois) >= max_results:
            break
    return pois


def _search_text(keywords: str, city: str | None = None) -> list[dict[str, Any]]:
    response = httpx.get(
        "https://restapi.amap.com/v3/place/text",
        params={"key": os.getenv("AMAP_API_KEY"), "keywords": keywords, "city": city or "", "offset": 10},
        timeout=6,
    )
    response.raise_for_status()
    return response.json().get("pois") or []


def estimate_route(
    origin: str | None,
    destination: str | None,
    city: str | None = None,
    mode: str = "transit",
) -> dict[str, Any]:
    if not amap_enabled() or not origin or not destination:
        raise RuntimeError("路线查询缺少坐标或 AMAP_API_KEY 未配置")

    route_mode = mode if mode in {"transit", "driving", "walking"} else "transit"
    path = {
        "transit": "https://restapi.amap.com/v3/direction/transit/integrated",
        "driving": "https://restapi.amap.com/v3/direction/driving",
        "walking": "https://restapi.amap.com/v3/direction/walking",
    }[route_mode]

    def call() -> dict[str, Any]:
        response = httpx.get(
            path,
            params={
                "key": os.getenv("AMAP_API_KEY"),
                "origin": origin,
                "destination": destination,
                "city": city or "",
                "output": "json",
            },
            timeout=6,
        )
        response.raise_for_status()
        data = response.json()
        route = data.get("route") or {}
        if route_mode == "transit":
            plans = route.get("transits") or []
        else:
            plans = route.get("paths") or []
        first = plans[0] if plans else {}
        duration = _minutes(first.get("duration"))
        distance = _kilometers(first.get("distance"))
        summary = "未查询到路线"
        if first:
            label = {"transit": "公交", "driving": "驾车", "walking": "步行"}[route_mode]
            summary = f"{label}约 {duration} 分钟，{distance} 公里"
        return {
            "enabled": True,
            "success": bool(first),
            "mode": route_mode,
            "summary": summary,
            "distance": first.get("distance"),
            "duration": first.get("duration"),
            "raw": first,
        }

    return safe_call(call, name="amap.route")


def _minutes(seconds: Any) -> str:
    try:
        return str(round(float(seconds or 0) / 60))
    except (TypeError, ValueError):
        return "-"


def _kilometers(meters: Any) -> str:
    try:
        return f"{float(meters or 0) / 1000:.1f}"
    except (TypeError, ValueError):
        return "-"
