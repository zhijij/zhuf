from typing import Any, TypedDict


class TenantAgentState(TypedDict, total=False):
    request: dict[str, Any]
    context: dict[str, Any]
    user: dict[str, Any]
    message: str
    session_id: str
    memory: list[dict[str, str]]
    rag: dict[str, Any]
    route: dict[str, Any]
    coordinator: dict[str, Any]
    collaboration: dict[str, Any]
    retrieval: dict[str, Any]
    analysis: dict[str, Any]
    tool_calls: list[dict[str, Any]]
    action_requests: list[dict[str, Any]]
    answer: str
    intent: str
    intent_label: str
    skill: dict[str, Any] | None
    next_actions: list[str]
    suggestions: dict[str, Any] | None
    house_ids: list[int]
    memory_updated: bool
    checkpoints: list[str]
