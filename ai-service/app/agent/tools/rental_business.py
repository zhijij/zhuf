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
        return {"success": False, "message": "Java AI 工具层未配置"}
    headers = {}
    token = java_tools_token()
    if token:
        headers["X-AI-Tool-Token"] = token
    response = httpx.post(f"{base_url}{path}", json=payload, headers=headers, timeout=8)
    response.raise_for_status()
    data = response.json()
    return data.get("data") if isinstance(data, dict) and isinstance(data.get("data"), dict) else data


def search_houses(state: dict[str, Any], query: str | None = None, city: str | None = None, max_rent: int | None = None) -> dict[str, Any]:
    java_result = safe_call(
        _post_java_tool,
        fallback=None,
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
    if java_result and java_result.get("success") is not False:
        return java_result
    return call_tool("search_public_houses", state, query=query, city=city, maxRent=max_rent).get("output") or {}


def get_house_detail(state: dict[str, Any], house_id: int | str | None) -> dict[str, Any]:
    if not house_id:
        return {"success": False, "message": "缺少房源 ID"}
    return safe_call(
        _post_java_tool,
        fallback={"success": False, "message": "房源详情工具暂不可用"},
        name="java.house_detail",
        path="/rental/ai/tools/houses/detail",
        payload={"userId": state.get("user", {}).get("userId"), "houseId": house_id},
    )


def get_contract(state: dict[str, Any], contract_id: int | str | None) -> dict[str, Any]:
    if not contract_id:
        return {"success": False, "message": "缺少合同 ID"}
    return safe_call(
        _post_java_tool,
        fallback={"success": False, "message": "合同工具暂不可用"},
        name="java.contract",
        path="/rental/ai/tools/contracts/detail",
        payload={"userId": state.get("user", {}).get("userId"), "contractId": contract_id},
    )


def save_long_term_memory(state: dict[str, Any], content: str, memory_type: str = "summary") -> dict[str, Any]:
    return safe_call(
        _post_java_tool,
        fallback={"success": False, "message": "长期记忆工具暂不可用"},
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
        fallback={"success": False, "message": "动作工具暂不可用"},
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
