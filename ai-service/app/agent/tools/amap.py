import os
from typing import Any

import httpx

from app.core.guardrails import safe_call


def amap_enabled() -> bool:
    return bool(os.getenv("AMAP_API_KEY"))


def search_poi(keywords: str, city: str | None = None) -> dict[str, Any]:
    if not amap_enabled():
        return {"enabled": False, "summary": "高德工具未配置，使用看房清单兜底", "pois": []}

    def call() -> dict[str, Any]:
        response = httpx.get(
            "https://restapi.amap.com/v3/place/text",
            params={"key": os.getenv("AMAP_API_KEY"), "keywords": keywords, "city": city or "", "offset": 5},
            timeout=6,
        )
        response.raise_for_status()
        data = response.json()
        return {"enabled": True, "summary": f"查询到 {len(data.get('pois') or [])} 个周边点位", "pois": data.get("pois") or []}

    return safe_call(call, fallback={"enabled": False, "summary": "高德周边查询失败", "pois": []}, name="amap.poi")


def estimate_route(origin: str | None, destination: str | None, city: str | None = None) -> dict[str, Any]:
    if not amap_enabled() or not origin or not destination:
        return {"enabled": False, "summary": "路线预算缺少坐标或高德配置"}
    return {"enabled": False, "summary": "路线预算待接入坐标标准化"}
