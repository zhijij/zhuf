import os
from typing import Any

import httpx

from app.core.guardrails import safe_call
from app.tooling import call_tool


def java_tools_base_url() -> str:
    return os.getenv("JAVA_AI_TOOLS_BASE_URL", "").rstrip("/")


def java_tools_token() -> str:
    return os.getenv("AI_INTERNAL_TOOL_TOKEN", "")


def _post_java_tool(path: str, payload: dict[str, Any]) -> dict[str, Any]:
    base_url = java_tools_base_url()
    if not base_url:
        raise RuntimeError("JAVA_AI_TOOLS_BASE_URL 未配置，无法调用 Java AI 工具层")
    headers = {}
    token = java_tools_token()
    if token:
        headers["X-AI-Tool-Token"] = token
    response = httpx.post(f"{base_url}{path}", json=payload, headers=headers, timeout=20)
    response.raise_for_status()
    data = response.json()
    return data.get("data") if isinstance(data, dict) and isinstance(data.get("data"), dict) else data


def search_houses(state: dict[str, Any], query: str | None = None, city: str | None = None, max_rent: int | None = None) -> dict[str, Any]:
    java_result = safe_call(
        _post_java_tool,
        name="java.search_houses",
        path="/rental/ai/tools/houses/search",
        payload={
            "userId": state.get("user", {}).get("userId"),
            "role": state.get("user", {}).get("role") or state.get("role"),
            "query": query or state.get("message"),
            "city": city,
            "maxRent": max_rent,
        },
    )
    if java_result.get("success") is False:
        raise RuntimeError(str(java_result.get("message") or "Java 房源检索工具返回失败"))
    return java_result


def get_house_detail(state: dict[str, Any], house_id: int | str | None) -> dict[str, Any]:
    if not house_id:
        return {"success": False, "message": "缺少房源 ID"}
    return safe_call(
        _post_java_tool,
        name="java.house_detail",
        path="/rental/ai/tools/houses/detail",
        payload={"userId": state.get("user", {}).get("userId"), "houseId": house_id},
    )


def get_contract(state: dict[str, Any], contract_id: int | str | None) -> dict[str, Any]:
    if not contract_id:
        return {"success": False, "message": "缺少合同 ID"}
    return safe_call(
        _post_java_tool,
        name="java.contract",
        path="/rental/ai/tools/contracts/detail",
        payload={"userId": state.get("user", {}).get("userId"), "contractId": contract_id},
    )


def get_house_map_context(
    state: dict[str, Any],
    house_id: int | str | None,
    destination: str | None = None,
    mode: str = "transit",
) -> dict[str, Any]:
    if not house_id:
        return {"success": False, "message": "缺少房源 ID，无法查询通勤与周边"}
    return safe_call(
        _post_java_tool,
        name="java.amap.house_context",
        path="/rental/ai/tools/amap/house-context",
        payload={
            "userId": state.get("user", {}).get("userId"),
            "role": state.get("user", {}).get("role") or state.get("role"),
            "houseId": house_id,
            "destination": destination,
            "mode": mode or "transit",
        },
    )


def search_amap_around(
    state: dict[str, Any],
    location: str | None,
    keywords: str | None = None,
    city: str | None = None,
    radius: int | None = None,
    limit: int | None = None,
) -> dict[str, Any]:
    if not location:
        return {"success": False, "message": "缺少坐标，无法查询周边"}
    return safe_call(
        _post_java_tool,
        name="java.amap.around",
        path="/rental/ai/tools/amap/around",
        payload={
            "userId": state.get("user", {}).get("userId"),
            "role": state.get("user", {}).get("role") or state.get("role"),
            "location": location,
            "keywords": keywords or "地铁站|公交站|超市|商场|医院|药店|学校|幼儿园",
            "city": city,
            "radius": radius,
            "limit": limit,
        },
    )


def save_long_term_memory(state: dict[str, Any], content: str, memory_type: str = "summary") -> dict[str, Any]:
    return safe_call(
        _post_java_tool,
        name="java.memory.save",
        path="/rental/ai/tools/memory/save",
        payload={
            "userId": state.get("user", {}).get("userId"),
            "role": state.get("user", {}).get("role") or state.get("role"),
            "memoryType": memory_type,
            "content": content,
        },
    )


def execute_action(state: dict[str, Any], action: str, payload: dict[str, Any], confirmed: bool = False) -> dict[str, Any]:
    return safe_call(
        _post_java_tool,
        name=f"java.action.{action}",
        path="/rental/ai/tools/actions/execute",
        payload={
            "userId": state.get("user", {}).get("userId"),
            "role": state.get("user", {}).get("role") or state.get("role"),
            "action": action,
            "confirmed": confirmed,
            "payload": payload,
        },
    )
