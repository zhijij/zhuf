import json
import os
from concurrent.futures import ThreadPoolExecutor
from typing import Any, Literal

from pydantic import BaseModel, Field

from app.agent.memory.checkpoint import checkpoint_config, get_checkpointer
from app.agent.memory.store import compact_memory, load_memory, save_memory
from app.agent.rag.retriever import rag_prefetch as run_rag_prefetch
from app.agent.state import TenantAgentState
from app.agent.tools import amap
from app.agent.tools.rental_business import get_contract, get_house_map_context, save_long_term_memory, search_amap_around, search_houses
from app.business_rules import as_int, knowledge_match_line
from app.config import INTENT_LABELS, KNOWLEDGE_SOURCE_TYPES
from app.langchain_runtime import invoke_structured_json, refine_with_langchain
from app.skill_registry import select_skill, skill_to_dict

_COMPILED_GRAPH = None
_CHECKPOINTER = get_checkpointer()
TENANT_INTENTS = {
    "smalltalk",
    "correction",
    "house_recommend",
    "contract_risk",
    "knowledge_answer",
    "record_summary",
    "context_answer",
}
TENANT_ROUTES = {"smalltalk", "slot_filling", "collaboration", "retrieval_executor"}
TENANT_EXPERTS = {"house_search_specialist", "map_life_specialist", "risk_analysis_specialist"}
NEARBY_FOCUS_VALUES = {"education", "transport", "medical", "life"}


class TenantRoute(BaseModel):
    intent: str = Field(default="house_recommend")
    route: str = Field(default="collaboration")
    slots: dict[str, Any] = Field(default_factory=dict)
    missing_slots: list[str] = Field(default_factory=list)
    experts: list[str] = Field(default_factory=list)
    source_types: list[str] = Field(default_factory=list)
    confidence: float = Field(default=0.7)


class TenantCoordinatorPlan(BaseModel):
    route: str = Field(default="collaboration")
    experts: list[str] = Field(default_factory=list)
    tool_plan: list[str] = Field(default_factory=list)
    ask_user: list[str] = Field(default_factory=list)
    reason: str = Field(default="")


def run_tenant_graph(request: dict[str, Any]) -> dict[str, Any]:
    state: TenantAgentState = {
        "request": request,
        "context": request.get("context") or {},
        "message": str(request.get("message") or request.get("query") or ""),
        "session_id": str(request.get("sessionId") or request.get("userId") or "anonymous"),
        "user": {
            "userId": request.get("userId"),
            "username": request.get("username"),
            "role": request.get("role") or (request.get("context") or {}).get("workMode") or "tenant",
            "roles": request.get("roles") or [],
        },
        "tool_calls": [],
        "action_requests": [],
        "checkpoints": [],
    }
    state["memory"] = load_memory(state["session_id"])
    recommend = request.get("recommend") or {}
    if request.get("city") or request.get("maxRent") or recommend:
        state["recommend"] = {
            "city": request.get("city") or recommend.get("city"),
            "maxRent": request.get("maxRent") or recommend.get("maxRent"),
        }

    graph = get_graph()
    result = graph.invoke(state, checkpoint_config(state["session_id"]))
    return to_response(result)


def reset_turn_state(state: TenantAgentState) -> TenantAgentState:
    state["tool_calls"] = []
    state["action_requests"] = []
    state["checkpoints"] = []
    state["analysis"] = {}
    state["retrieval"] = {}
    state["collaboration"] = {}
    state["coordinator"] = {}
    state["route"] = {}
    state["answer"] = ""
    state["next_actions"] = []
    state["house_ids"] = []
    state["memory_updated"] = False
    state["suggestions"] = None
    return state


def get_graph():
    global _COMPILED_GRAPH
    if _COMPILED_GRAPH is None:
        _COMPILED_GRAPH = build_graph()
    return _COMPILED_GRAPH


def build_graph():
    from langgraph.graph import END, StateGraph

    builder = StateGraph(TenantAgentState)
    builder.add_node("rag_prefetch", rag_prefetch)
    builder.add_node("router", router)
    builder.add_node("coordinator", coordinator)
    builder.add_node("smalltalk", smalltalk)
    builder.add_node("slot_filling", slot_filling)
    builder.add_node("collaboration", collaboration)
    builder.add_node("retrieval_executor", retrieval_executor)
    builder.add_node("analyst", analyst)
    builder.add_node("synthesis", synthesis)
    builder.add_node("persist_memory", persist_memory)
    builder.set_entry_point("rag_prefetch")
    builder.add_edge("rag_prefetch", "router")
    builder.add_edge("router", "coordinator")
    builder.add_conditional_edges(
        "coordinator",
        route_after_coordinator,
        {
            "smalltalk": "smalltalk",
            "slot_filling": "slot_filling",
            "collaboration": "collaboration",
            "retrieval_executor": "retrieval_executor",
        },
    )
    builder.add_edge("smalltalk", "synthesis")
    builder.add_edge("slot_filling", "synthesis")
    builder.add_edge("collaboration", "synthesis")
    builder.add_edge("retrieval_executor", "analyst")
    builder.add_edge("analyst", "synthesis")
    builder.add_edge("synthesis", "persist_memory")
    builder.add_edge("persist_memory", END)
    if _CHECKPOINTER is not None:
        return builder.compile(checkpointer=_CHECKPOINTER)
    return builder.compile()


def rag_prefetch(state: TenantAgentState) -> TenantAgentState:
    state = reset_turn_state(state)
    state["checkpoints"].append("rag_prefetch")
    state["rag"] = {"source": "deferred", "hits": [], "prompt": ""}
    return state


def router(state: TenantAgentState) -> TenantAgentState:
    state["checkpoints"].append("router")
    state["route"] = structured_route(state)
    state["intent"] = state["route"]["intent"]
    state["intent_label"] = INTENT_LABELS.get(state["intent"], state["intent"])
    skill = select_skill(state["intent"], {"role": "tenant"})
    state["skill"] = skill_to_dict(skill)
    return state


def structured_route(state: TenantAgentState) -> dict[str, Any]:
    llm_route = llm_route_decision(state)
    if not llm_route:
        raise RuntimeError("模型没有返回租户智能体路由 JSON")
    return normalize_route(llm_route, state)


def normalize_route(route: dict[str, Any], state: TenantAgentState) -> dict[str, Any]:
    inferred_slots = infer_route_slots(state)
    llm_slots = {
        key: value
        for key, value in (route.get("slots") or {}).items()
        if value not in (None, "")
    }
    slots = merge_route_slots(inferred_slots, llm_slots)
    intent = normalize_intent(route.get("intent"))
    route_name = normalize_route_name(route.get("route"), intent)
    missing_slots = normalize_missing_slots(route.get("missing_slots") or [], slots)
    experts = normalize_experts(route.get("experts") or slots.get("experts") or [])
    source_types = normalize_source_types(route.get("source_types") or route.get("sourceTypes") or slots.get("sourceTypes") or [])
    confidence = safe_float(route.get("confidence"), 0.75)

    if intent == "house_recommend" and not missing_slots:
        route_name = "collaboration"
    if experts and not missing_slots and route_name != "smalltalk":
        route_name = "collaboration"

    return TenantRoute(
        intent=intent,
        route=route_name,
        slots=slots,
        missing_slots=missing_slots,
        experts=experts,
        source_types=source_types,
        confidence=confidence,
    ).model_dump()


def infer_route_slots(state: TenantAgentState) -> dict[str, Any]:
    context = state.get("context") or {}
    selected = context.get("selected") or {}
    filters = context.get("filters") or {}
    recommend = state.get("recommend") or {}
    return {
        "city": recommend.get("city") or filters.get("city"),
        "district": filters.get("district"),
        "maxRent": recommend.get("maxRent") or filters.get("maxRent"),
        "houseId": selected.get("houseId"),
        "contractId": selected.get("contractId"),
        "selectedCity": selected.get("city"),
        "selectedDistrict": selected.get("district"),
    }


def merge_route_slots(inferred_slots: dict[str, Any], llm_slots: dict[str, Any]) -> dict[str, Any]:
    slots = {**inferred_slots}
    for key in ["city", "district", "maxRent"]:
        value = llm_slots.get(key)
        if value not in (None, ""):
            slots[key] = value
    for key, value in llm_slots.items():
        if key not in slots and value not in (None, ""):
            slots[key] = value
    return normalize_route_slots(slots)


def normalize_route_slots(slots: dict[str, Any]) -> dict[str, Any]:
    normalized = {key: value for key, value in slots.items() if value not in (None, "")}
    focus = normalized.get("nearbyFocus") or normalized.get("nearby_focus")
    if focus in NEARBY_FOCUS_VALUES:
        normalized["nearbyFocus"] = focus
    else:
        normalized.pop("nearbyFocus", None)
        normalized.pop("nearby_focus", None)
    return normalized


def normalize_intent(value: Any) -> str:
    intent = str(value or "context_answer").strip()
    if intent == "record_summary":
        return "context_answer"
    return intent if intent in TENANT_INTENTS else "context_answer"


def normalize_route_name(value: Any, intent: str) -> str:
    route = str(value or "").strip()
    if route in TENANT_ROUTES:
        return route
    if intent in {"smalltalk", "correction"}:
        return "smalltalk"
    if intent == "knowledge_answer":
        return "retrieval_executor"
    return "collaboration"


def normalize_experts(values: Any) -> list[str]:
    if not isinstance(values, list):
        return []
    return unique_ordered([str(item) for item in values if str(item) in TENANT_EXPERTS])


def normalize_source_types(values: Any) -> list[str]:
    if not isinstance(values, list):
        return []
    return unique_ordered([str(item) for item in values if str(item) in KNOWLEDGE_SOURCE_TYPES])


def safe_float(value: Any, default: float) -> float:
    try:
        return float(value)
    except (TypeError, ValueError):
        return default


def normalize_missing_slots(missing_slots: list[str], slots: dict[str, Any]) -> list[str]:
    normalized = []
    if not isinstance(missing_slots, list):
        return normalized
    for item in missing_slots:
        if item != "city":
            continue
        if slots.get("city"):
            continue
        if item not in normalized:
            normalized.append(item)
    return normalized


def should_prefetch_rag(state: TenantAgentState) -> bool:
    route = state.get("route") or {}
    intent = route.get("intent")
    next_route = route.get("route")
    if intent in {"smalltalk", "correction"}:
        return False
    return next_route in {"retrieval_executor", "collaboration"}


def infer_rag_source_types(state: TenantAgentState) -> list[str]:
    route = state.get("route") or {}
    source_types = normalize_source_types(route.get("source_types") or route.get("sourceTypes") or [])
    if source_types:
        return source_types
    intent = route.get("intent")
    if intent == "contract_risk":
        return ["contract", "policy", "faq"]
    if intent == "knowledge_answer":
        return ["faq", "policy", "enterprise", "chat"]
    if intent == "house_recommend":
        return ["faq", "policy", "contract"]
    return ["chat", "enterprise", "faq"]


def ensure_rag_loaded(state: TenantAgentState) -> TenantAgentState:
    current = state.get("rag") or {}
    if current.get("source") != "deferred":
        return state
    state["rag"] = run_rag_prefetch(
        state["message"],
        state["user"].get("role"),
        source_types=infer_rag_source_types(state),
    )
    state["checkpoints"].append("rag_loaded")
    return state


def llm_route_decision(state: TenantAgentState) -> dict[str, Any] | None:
    base_url = os.getenv("AI_LLM_BASE_URL", "").rstrip("/")
    api_key = os.getenv("AI_LLM_API_KEY", "")
    model = os.getenv("AI_LLM_MODEL", "")
    if not base_url or not api_key or not model:
        raise RuntimeError("AI_LLM_BASE_URL、AI_LLM_API_KEY 或 AI_LLM_MODEL 未配置，无法调用模型判断租户路由")

    message = state["message"]
    selected = (state.get("context") or {}).get("selected") or {}
    filters = (state.get("context") or {}).get("filters") or {}
    recommend = state.get("recommend") or {}
    messages = [
        ("system",
         "你是租户智能体的路由器。"
         "请根据用户消息、最近对话和当前业务上下文，输出 JSON。"
         "只允许 intent 为 smalltalk, correction, house_recommend, contract_risk, knowledge_answer, context_answer。"
         "只允许 route 为 smalltalk, slot_filling, collaboration, retrieval_executor。"
         "必须输出 slots 对象，可包含 city、district、maxRent、nearbyFocus。"
         "nearbyFocus 只允许 education, transport, medical, life，无法判断就省略。"
         "必须输出 experts 数组，只允许 house_search_specialist, map_life_specialist, risk_analysis_specialist。"
         "需要查房源时放 house_search_specialist；需要周边/学校/交通/医疗/生活配套时放 map_life_specialist；需要合同风险时放 risk_analysis_specialist。"
         "只要 experts 非空，route 应为 collaboration；知识库问答且不需要专家时，route 才用 retrieval_executor。"
         "用户围绕当前/这个/这套房源问周边、附近、学校、交通、医院或生活配套时，intent 应为 context_answer，experts 应包含 map_life_specialist。"
         "如果用户当前消息只是城市、区县、板块或区域名（例如“海淀”“朝阳区”“浦东有吗”），应理解为更新找房区域，intent=house_recommend，experts=[\"house_search_specialist\"]。"
         "不要把上一轮的周边/学校/交通意图自动延续到新的城市或区县名；只有当前消息明确询问周围、附近、配套、学校、医院、地铁等，才派 map_life_specialist。"
         "可以输出 source_types 数组，只允许 house, contract, policy, faq, chat, enterprise，用于知识库检索范围。"
         "城市/区县判断交给你：如果用户说“有北京的没”，slots.city 应为“北京”；"
         "如果用户接着只说“海淀”，应结合最近对话判断为北京海淀，slots.city='北京', slots.district='海淀区'。"
         "不要因为当前选中房源在延安市，就把用户新问的城市或区县改回延安市/宝塔区。"
         "只有用户确实在找房且无法从消息或最近对话判断城市时，才给 missing_slots=[\"city\"]。"
         "不要输出 budget 这类未支持缺槽；预算缺失不是阻塞找房。"),
        ("user",
         json.dumps({
             "message": message,
             "recentMemory": compact_memory(state.get("memory") or []),
             "selected": selected,
             "filters": filters,
             "recommend": recommend,
             "inferredSlots": infer_route_slots(state),
         }, ensure_ascii=False))
    ]
    data = invoke_structured_json(
        base_url=base_url,
        api_key=api_key,
        model=model,
        messages=messages,
        timeout=8,
    )
    if not data:
        raise RuntimeError("模型没有返回租户智能体路由 JSON")
    route = TenantRoute(
        intent=str(data.get("intent") or "context_answer"),
        route=str(data.get("route") or "collaboration"),
        slots=data.get("slots") if isinstance(data.get("slots"), dict) else {},
        missing_slots=data.get("missing_slots") if isinstance(data.get("missing_slots"), list) else [],
        experts=normalize_experts(data.get("experts") or []),
        source_types=normalize_source_types(data.get("source_types") or data.get("sourceTypes") or []),
        confidence=safe_float(data.get("confidence"), 0.75),
    )
    return route.model_dump()


def coordinator(state: TenantAgentState) -> TenantAgentState:
    state["checkpoints"].append("coordinator")
    if should_prefetch_rag(state):
        state = ensure_rag_loaded(state)
    route = state.get("route") or {}
    route_experts = normalize_experts(route.get("experts") or [])
    if route_experts:
        state["coordinator"] = build_coordinator_payload(route_experts, reason="router-ai-experts")
        return state
    llm_plan = llm_coordinator_plan(state)
    if not llm_plan:
        raise RuntimeError("模型没有返回租户智能体主管规划 JSON")
    state["coordinator"] = llm_plan
    return state


def build_coordinator_payload(experts: list[str], reason: str = "") -> dict[str, Any]:
    return {
        "agent": "coordinator",
        "toolPlan": ["search_houses", "vector_search", "amap_search_poi"],
        "confirmationPolicy": {
            "mediumHighRiskRequiresConfirmed": True,
            "destructiveActionsDeniedByDefault": True,
        },
        "collaborationPlan": {
            "parallel": bool(experts),
            "experts": experts,
        },
        "reason": reason,
    }


def ensure_required_experts(state: TenantAgentState, plan: dict[str, Any]) -> None:
    collaboration_plan = plan.setdefault("collaborationPlan", {})
    required = required_experts_for_state(state)
    current = normalize_experts(collaboration_plan.get("experts") or [])
    merged = unique_ordered([*required, *current])
    collaboration_plan["experts"] = merged
    collaboration_plan["parallel"] = bool(merged)


def required_experts_for_state(state: TenantAgentState) -> list[str]:
    route = state.get("route") or {}
    route_experts = normalize_experts(route.get("experts") or [])
    if route_experts:
        return route_experts
    intent = route.get("intent")
    if intent == "knowledge_answer":
        return []
    if intent == "contract_risk":
        return ["risk_analysis_specialist"]
    if intent == "house_recommend":
        return ["house_search_specialist"]
    return []


def unique_ordered(items: list[str]) -> list[str]:
    result = []
    for item in items:
        if item and item not in result:
            result.append(item)
    return result


def llm_coordinator_plan(state: TenantAgentState) -> dict[str, Any] | None:
    base_url = os.getenv("AI_LLM_BASE_URL", "").rstrip("/")
    api_key = os.getenv("AI_LLM_API_KEY", "")
    model = os.getenv("AI_LLM_MODEL", "")
    if not base_url or not api_key or not model:
        raise RuntimeError("AI_LLM_BASE_URL、AI_LLM_API_KEY 或 AI_LLM_MODEL 未配置，无法调用模型规划租户智能体")

    route = state.get("route") or {}
    messages = [
        ("system",
         "你是租户智能体的主管。"
         "请根据当前 route 决定是否需要并行专家协作。"
         "输出 JSON，experts 只允许使用 house_search_specialist, map_life_specialist, risk_analysis_specialist。"
         "如果 route 是 smalltalk 或 slot_filling，experts 应为空。"),
        ("user",
         json.dumps({
             "message": state.get("message"),
             "route": route,
             "selected": (state.get("context") or {}).get("selected") or {},
             "mapContext": (state.get("context") or {}).get("mapContext") or {},
         }, ensure_ascii=False))
    ]
    data = invoke_structured_json(
        base_url=base_url,
        api_key=api_key,
        model=model,
        messages=messages,
        timeout=8,
    )
    if not data:
        raise RuntimeError("模型没有返回租户智能体主管规划 JSON")
    plan = TenantCoordinatorPlan(
        route=str(data.get("route") or route.get("route") or "collaboration"),
        experts=[str(item) for item in (data.get("experts") or []) if item],
        tool_plan=[str(item) for item in (data.get("tool_plan") or []) if item],
        ask_user=[str(item) for item in (data.get("ask_user") or []) if item],
        reason=str(data.get("reason") or ""),
    )
    return {
        "agent": "coordinator",
        "toolPlan": plan.tool_plan,
        "confirmationPolicy": {
            "mediumHighRiskRequiresConfirmed": True,
            "destructiveActionsDeniedByDefault": True,
        },
        "collaborationPlan": {
            "parallel": bool(plan.experts),
            "experts": normalize_experts(plan.experts),
        },
        "askUser": plan.ask_user,
        "reason": plan.reason,
    }


def route_after_coordinator(state: TenantAgentState) -> Literal["smalltalk", "slot_filling", "collaboration", "retrieval_executor"]:
    route = state.get("route") or {}
    if route.get("route") == "smalltalk":
        return "smalltalk"
    if route.get("missing_slots"):
        return "slot_filling"
    if (state.get("coordinator") or {}).get("collaborationPlan", {}).get("experts"):
        return "collaboration"
    if route.get("route") == "retrieval_executor":
        return "retrieval_executor"
    return "retrieval_executor"

def smalltalk(state: TenantAgentState) -> TenantAgentState:
    state["checkpoints"].append("smalltalk")
    if (state.get("route") or {}).get("intent") == "correction":
        state["analysis"] = {"summary": "我刚才的理解偏了。你直接告诉我你想纠正哪一点：是推荐目标、当前房源、通勤条件，还是合同风险判断？"}
    else:
        state["analysis"] = {"summary": "你好，我可以帮你找房、比较房源、规划看房问题和检查合同风险。"}
    return state


def slot_filling(state: TenantAgentState) -> TenantAgentState:
    state["checkpoints"].append("slot_filling")
    missing = (state.get("route") or {}).get("missing_slots") or []
    labels = {"city": "城市", "maxRent": "预算上限", "houseId": "房源"}
    state["analysis"] = {"summary": "还需要补充：" + "、".join(labels.get(item, item) for item in missing)}
    return state


def collaboration(state: TenantAgentState) -> TenantAgentState:
    state["checkpoints"].append("collaboration")
    expert_names = (state.get("coordinator") or {}).get("collaborationPlan", {}).get("experts") or []
    handlers = {
        "house_search_specialist": house_search_specialist,
        "map_life_specialist": map_life_specialist,
        "risk_analysis_specialist": risk_analysis_specialist,
    }
    with ThreadPoolExecutor(max_workers=min(max(len(expert_names), 1), 4)) as pool:
        futures = [pool.submit(handlers[name], state) for name in expert_names if name in handlers]
        experts = [future.result() for future in futures]
    tool_calls = []
    for item in experts:
        tool_calls.extend(item.get("toolCalls") or [])
    state["tool_calls"] = [*state.get("tool_calls", []), *tool_calls]
    state["collaboration"] = {
        "mode": "langgraph-supervisor-parallel-hybrid",
        "router": state.get("route"),
        "coordinator": state.get("coordinator"),
        "experts": experts,
        "synthesis": {"strategy": "single final answer from merged expert evidence"},
    }
    return state


def retrieval_executor(state: TenantAgentState) -> TenantAgentState:
    state["checkpoints"].append("retrieval_executor")
    state = ensure_rag_loaded(state)
    state["retrieval"] = state.get("rag") or {}
    if should_append_rag_tool_call(state):
        state["tool_calls"] = [
            *state.get("tool_calls", []),
            tool_call("search_knowledge_base", "检索统一知识库", rag_tool_output(state)),
        ]
    if (state.get("route") or {}).get("intent") == "context_answer":
        record = selected_record_tool_output(state)
        state["tool_calls"] = [
            *state.get("tool_calls", []),
            tool_call("summarize_business_record", "读取当前业务", record),
        ]
    return state


def analyst(state: TenantAgentState) -> TenantAgentState:
    state["checkpoints"].append("analyst")
    hits = (state.get("retrieval") or {}).get("hits") or []
    lines = [knowledge_match_line(item) for item in hits[:4]]
    state["analysis"] = {"summary": "\n".join(lines) if lines else "未检索到强相关知识。"}
    return state


def should_append_rag_tool_call(state: TenantAgentState) -> bool:
    intent = (state.get("route") or {}).get("intent")
    return intent in {"knowledge_answer", "context_answer"}


def rag_tool_output(state: TenantAgentState) -> dict[str, Any]:
    rag = state.get("retrieval") or state.get("rag") or {}
    hits = rag.get("hits") or []
    return {
        "summary": "未命中统一知识库内容。" if not hits else "命中知识库：" + "、".join(str(item.get("title") or item.get("sourceId") or "未命名知识") for item in hits[:4]),
        "matches": hits[:6],
        "appliedSourceTypes": rag.get("sourceTypes") or [],
        "source": rag.get("source"),
    }


def selected_record_tool_output(state: TenantAgentState) -> dict[str, Any]:
    selected = (state.get("context") or {}).get("selected") or {}
    if not selected:
        return {"summary": "当前未选中业务记录。", "record": {}}
    parts = []
    for label, key in [
        ("标题", "title"),
        ("城市", "city"),
        ("区域", "district"),
        ("租金", "rentAmount"),
        ("面积", "area"),
        ("房源ID", "houseId"),
        ("合同ID", "contractId"),
        ("状态", "statusLabel"),
    ]:
        value = selected.get(key)
        if value not in (None, ""):
            parts.append(f"{label}：{value}")
    summary = "；".join(parts) if parts else "当前记录字段较少，需要补充业务信息。"
    return {"summary": summary, "record": selected}


def house_search_specialist(state: TenantAgentState) -> dict[str, Any]:
    slots = (state.get("route") or {}).get("slots") or {}
    result = search_houses(
        to_tool_state(state),
        query=house_search_query(state, slots),
        city=slots.get("city"),
        max_rent=as_int(slots.get("maxRent")),
    )
    result = filter_house_result_by_slots(result, slots)
    matches = result.get("matches") or result.get("rows") or result.get("items") or []
    summary = "房源检索专家未命中房源。"
    if matches:
        first = matches[0]
        summary = f"房源检索专家命中 {len(matches)} 套，优先关注 {first.get('title') or first.get('houseTitle') or first.get('houseId')}。"
    return {
        "name": "house_search_specialist",
        "label": "房源检索专家",
        "status": "success",
        "summary": summary,
        "output": result,
        "toolCalls": [tool_call("search_houses", "查询业务房源", result)],
    }


def filter_house_result_by_slots(result: dict[str, Any], slots: dict[str, Any]) -> dict[str, Any]:
    filtered = {**(result or {})}
    for key in ["matches", "rows", "items"]:
        values = filtered.get(key)
        if isinstance(values, list):
            filtered[key] = [item for item in values if isinstance(item, dict) and house_matches_slots(item, slots)]
    matches = filtered.get("matches") or filtered.get("rows") or filtered.get("items") or []
    place = " ".join(str(item) for item in [slots.get("city"), slots.get("district")] if item)
    filtered["summary"] = f"查询到 {len(matches)} 套{place + ' ' if place else ''}匹配房源"
    filtered["success"] = result.get("success", True) if isinstance(result, dict) else True
    return filtered


def house_search_query(state: TenantAgentState, slots: dict[str, Any]) -> str:
    parts = [
        slots.get("city"),
        slots.get("district"),
        state.get("message"),
    ]
    return " ".join(str(item).strip() for item in parts if item)


def map_life_specialist(state: TenantAgentState) -> dict[str, Any]:
    slots = (state.get("route") or {}).get("slots") or {}
    selected = state["context"].get("selected") or {}
    map_context = state["context"].get("mapContext") or {}
    house_location = map_context.get("houseLocation") or {}
    city = slots.get("city") or selected.get("city") or map_context.get("city") or house_location.get("city")
    house_id = selected.get("houseId") or slots.get("houseId")
    destination = map_context.get("destination")
    context_location = selected_location(selected) or selected_location(map_context) or selected_location(house_location)
    if house_id:
        result = get_house_map_context(to_tool_state(state), house_id, destination=destination, mode="transit")
        tool_name = "amap_house_context"
        tool_label = "查询房源通勤与周边"
    elif context_location:
        result = search_amap_around(to_tool_state(state), context_location, city=city, limit=200)
        tool_name = "amap_search_around"
        tool_label = "查询坐标周边 POI"
    else:
        result = amap.search_poi(nearby_keywords(slots.get("nearbyFocus")), city=city, limit=50)
        tool_name = "amap_search_poi"
        tool_label = "查询周边配套"
    checks = ["通勤时间", "地铁/公交步行距离", "商超便利", "噪音与采光", "夜间安全感"]
    analysis = analyze_map_life_context(state, result)
    summary = analysis.get("summary") or result.get("summary") or "周边工具未返回可用数据"
    return {
        "name": "map_life_specialist",
        "label": "地图生活专家",
        "status": "success",
        "summary": summary,
        "output": {**result, "checks": checks, "analysis": analysis},
        "toolCalls": [tool_call(tool_name, tool_label, result)],
    }


def analyze_map_life_context(state: TenantAgentState, result: dict[str, Any]) -> dict[str, Any]:
    evidence = compact_map_evidence(result)
    llm_text = llm_map_life_analysis(state, evidence)
    if not llm_text:
        raise RuntimeError("模型没有生成周边生活分析")
    return {
        "summary": llm_text,
        "evidence": evidence,
        "source": "llm",
    }


def nearby_keywords(focus: str | None) -> str:
    if focus == "education":
        return "学校 幼儿园 培训机构"
    if focus == "transport":
        return "地铁站 公交站"
    if focus == "medical":
        return "医院 药店 诊所"
    if focus == "life":
        return "超市 商场 便利店 菜市场"
    return "地铁站 商超 医院 学校"


def focus_group_labels(focus: str | None) -> list[str]:
    mapping = {
        "education": ["教育"],
        "transport": ["交通"],
        "medical": ["医疗"],
        "life": ["生活"],
    }
    return mapping.get(focus or "", ["交通", "生活", "医疗", "教育"])


def selected_location(record: dict[str, Any]) -> str | None:
    longitude = record.get("longitude")
    latitude = record.get("latitude")
    if longitude is None or latitude is None or longitude == "" or latitude == "":
        location = record.get("location")
        return str(location).strip() if location else None
    return f"{longitude},{latitude}"


def llm_map_life_analysis(state: TenantAgentState, evidence: dict[str, Any]) -> str | None:
    base_url = os.getenv("AI_LLM_BASE_URL", "").rstrip("/")
    api_key = os.getenv("AI_LLM_API_KEY", "")
    model = os.getenv("AI_LLM_MODEL", "")
    if not base_url or not api_key or not model:
        raise RuntimeError("AI_LLM_BASE_URL、AI_LLM_API_KEY 或 AI_LLM_MODEL 未配置，无法调用模型分析周边生活")
    return refine_with_langchain(
        base_url=base_url,
        api_key=api_key,
        model=model,
        timeout=8,
        messages=[
            ("system",
             "你是租房地图生活专家。"
             "只基于给定高德 POI 和路线数据分析，不要编造不存在的设施。"
             "输出 3 到 5 条短建议，覆盖通勤、交通、生活便利、医疗教育和看房核验点。"),
            ("user", json.dumps({
                "message": state.get("message"),
                "selected": (state.get("context") or {}).get("selected") or {},
                "evidence": evidence,
            }, ensure_ascii=False)),
        ],
    )


def compact_map_evidence(result: dict[str, Any]) -> dict[str, Any]:
    groups = result.get("nearbyGroups") or []
    totals: dict[str, Any] = {}
    highlights: list[str] = []
    for group in groups:
        label = str(group.get("label") or "周边")
        pois = group.get("pois") or []
        nearest = nearest_poi(pois)
        totals[label] = {
            "count": int(group.get("fetched") or len(pois) or 0),
            "radius": group.get("radius"),
            "truncated": bool(group.get("truncated")),
            "nearest": nearest,
            "topPois": [poi_digest(item) for item in pois[:8]],
        }
        if nearest:
            highlights.append(f"{label}最近为 {nearest.get('name')}，约 {nearest.get('distance')} 米")
    if not groups and result.get("pois"):
        pois = result.get("pois") or []
        totals["周边"] = {
            "count": len(pois),
            "nearest": nearest_poi(pois),
            "topPois": [poi_digest(item) for item in pois[:8]],
        }
    return {
        "summary": result.get("summary"),
        "address": result.get("address"),
        "houseLocation": result.get("houseLocation"),
        "route": result.get("route"),
        "nearbySummary": result.get("nearbySummary"),
        "totals": totals,
        "highlights": highlights,
    }


def nearest_poi(pois: list[dict[str, Any]]) -> dict[str, Any] | None:
    digests = [poi_digest(item) for item in pois if isinstance(item, dict)]
    digests = [item for item in digests if item.get("name")]
    if not digests:
        return None
    return sorted(digests, key=lambda item: poi_distance_value(item.get("distance")))[0]


def poi_digest(poi: dict[str, Any]) -> dict[str, Any]:
    return {
        "name": poi.get("name"),
        "type": poi.get("type"),
        "address": poi.get("address"),
        "distance": poi.get("distance"),
        "location": poi.get("location"),
    }


def poi_distance_value(value: Any) -> float:
    try:
        return float(value)
    except (TypeError, ValueError):
        return 10**9


def risk_analysis_specialist(state: TenantAgentState) -> dict[str, Any]:
    selected = state["context"].get("selected") or {}
    risks = ["确认押金退还条件", "核对付款周期和提前退租违约责任", "看房时确认家具家电和维修责任"]
    contract_id = selected.get("contractId")
    contract = get_contract(to_tool_state(state), contract_id) if contract_id else {}
    if contract.get("contractContent"):
        risks.append("合同正文已读取，建议进一步核对交付清单")
    summary = "风险分析专家建议：" + "；".join(risks)
    return {
        "name": "risk_analysis_specialist",
        "label": "风险分析专家",
        "status": "success",
        "summary": summary,
        "output": {"risks": risks, "contract": contract},
        "toolCalls": [tool_call("get_contract", "查询合同", contract)] if contract_id else [],
    }


def synthesis(state: TenantAgentState) -> TenantAgentState:
    state["checkpoints"].append("synthesis")
    route = state.get("route") or {}
    intent = route.get("intent") or "context_answer"
    if intent == "knowledge_answer":
        answer = synthesize_knowledge_answer(state)
    elif intent == "smalltalk":
        answer = (state.get("analysis") or {}).get("summary")
    elif intent == "correction":
        answer = (state.get("analysis") or {}).get("summary") or "我收到纠正了，你可以直接补充正确目标。"
    elif route.get("missing_slots"):
        answer = (state.get("analysis") or {}).get("summary")
    elif intent == "knowledge_answer":
        answer = "我先查了统一知识库：\n" + ((state.get("analysis") or {}).get("summary") or "暂无命中。")
    else:
        if has_expert_result(state, "map_life_specialist") and not has_expert_result(state, "house_search_specialist"):
            answer = synthesize_nearby_answer(state)
        elif intent == "house_recommend":
            answer = synthesize_house_recommendation(state)
        elif intent == "contract_risk":
            answer = synthesize_contract_risk(state)
        elif has_expert_result(state, "map_life_specialist"):
            answer = synthesize_nearby_answer(state)
        else:
            answer = synthesize_context_answer(state)
    state["answer"] = answer or "我已完成本轮分析。"
    state["next_actions"] = build_next_actions(intent)
    state["suggestions"] = None
    state["house_ids"] = collect_house_ids(state)
    return state


def synthesize_house_recommendation(state: TenantAgentState) -> str:
    matches = extract_house_matches(state)
    slots = (state.get("route") or {}).get("slots") or {}
    city = slots.get("city")
    district = slots.get("district")
    max_rent = as_int(slots.get("maxRent"))
    place = " ".join(str(item) for item in [city, district] if item)
    target = "、".join(str(item) for item in [place, f"{max_rent}元以内" if max_rent else None] if item)
    if not matches:
        return f"我查了{target or '当前条件'}，暂时没有查到符合条件的公开房源。"

    lines = [f"查到了，{target or '当前条件'}下有 {len(matches)} 套可看的公开房源："]
    for index, item in enumerate(matches[:5], start=1):
        lines.append(f"{index}. {format_house_line(item)}")
    return "\n".join(lines)


def synthesize_nearby_answer(state: TenantAgentState) -> str:
    selected = (state.get("context") or {}).get("selected") or {}
    slots = (state.get("route") or {}).get("slots") or {}
    focus = slots.get("nearbyFocus")
    label = {"education": "学校/教育资源", "transport": "交通", "medical": "医疗", "life": "生活配套"}.get(focus, "周边配套")
    evidence = extract_map_evidence(state)
    title = selected.get("title") or (f"房源 {selected.get('houseId')}" if selected.get("houseId") else "当前房源")
    groups = evidence.get("totals") or {}
    labels = focus_group_labels(focus)
    lines = [f"我查了{title}周围的{label}。"]
    found = False
    for group_label in labels:
        item = groups.get(group_label) or {}
        nearest = item.get("nearest")
        count = int(item.get("count") or 0)
        if nearest:
            found = True
            lines.append(f"{group_label}：查到 {count} 个点位，最近的是 {nearest.get('name')}，约 {nearest.get('distance')} 米。")
            top_pois = [poi for poi in item.get("topPois") or [] if poi.get("name")][:3]
            if top_pois:
                lines.append("可重点看：" + "、".join(format_poi(item) for item in top_pois))
        elif count:
            found = True
            lines.append(f"{group_label}：查到 {count} 个点位，但工具没有返回最近点名称。")

    if not found:
        reason = evidence.get("summary") or "周边工具没有返回相关点位"
        return f"我查了{title}周围的{label}，目前没有拿到可用的{label}数据。{reason}。"
    return "\n".join(lines)


def synthesize_contract_risk(state: TenantAgentState) -> str:
    risks = []
    for expert in (state.get("collaboration") or {}).get("experts", []):
        if expert.get("name") == "risk_analysis_specialist":
            risks.extend((expert.get("output") or {}).get("risks") or [])
    if not risks:
        return "当前没有读取到具体合同风险字段。请选中合同后，我可以继续核对租期、押金、付款周期、维修责任和提前退租条款。"
    return "这次先看合同风险，建议重点确认：\n" + "\n".join(f"- {item}" for item in risks[:5])


def synthesize_knowledge_answer(state: TenantAgentState) -> str:
    knowledge = first_tool_output(state, "search_knowledge_base")
    matches = knowledge.get("matches") or []
    if matches:
        lines = ["我查了统一知识库，可以参考这些内容："]
        lines.extend(knowledge_match_line(item) for item in matches[:4])
        return "\n".join(lines)
    analysis = (state.get("analysis") or {}).get("summary")
    question = str(state.get("message") or "").strip()
    if question:
        return f"我查了统一知识库，暂时没有查到与“{question}”相关的可用内容；不能凭空给押金、退还或合同规则。"
    if analysis:
        return "我查了统一知识库：\n" + analysis
    return "统一知识库里暂时没有查到可用内容。"


def synthesize_context_answer(state: TenantAgentState) -> str:
    nearby = synthesize_nearby_answer(state) if has_expert_result(state, "map_life_specialist") else ""
    if nearby:
        return nearby
    record = first_tool_output(state, "summarize_business_record")
    if record:
        summary = record.get("summary") or "当前记录字段较少，需要补充业务信息。"
        return "当前业务摘要：\n" + summary
    summaries = [
        item.get("summary")
        for item in (state.get("collaboration") or {}).get("experts", [])
        if item.get("summary")
    ]
    return "这次没有拿到可用于回答的业务结果。"


def has_expert_result(state: TenantAgentState, name: str) -> bool:
    return any(
        expert.get("name") == name
        for expert in (state.get("collaboration") or {}).get("experts", [])
    )


def extract_house_matches(state: TenantAgentState) -> list[dict[str, Any]]:
    matches = []
    slots = (state.get("route") or {}).get("slots") or {}
    for call in state.get("tool_calls", []):
        if call.get("name") not in {"search_houses", "search_public_houses"}:
            continue
        output = call.get("output") or {}
        for key in ["matches", "rows", "items"]:
            for item in output.get(key) or []:
                if isinstance(item, dict) and house_matches_slots(item, slots):
                    matches.append(item)
    return matches


def house_matches_slots(item: dict[str, Any], slots: dict[str, Any]) -> bool:
    city = str(slots.get("city") or "").strip()
    district = str(slots.get("district") or "").strip()
    item_city = str(item.get("city") or "")
    item_district = str(item.get("district") or "")
    if city and city not in item_city and city.removesuffix("市") not in item_city:
        return False
    if district and district not in item_district and district.removesuffix("区") not in item_district:
        return False
    return True


def first_tool_output(state: TenantAgentState, name: str) -> dict[str, Any]:
    for call in state.get("tool_calls", []):
        if call.get("name") == name:
            return call.get("output") or {}
    return {}


def extract_map_evidence(state: TenantAgentState) -> dict[str, Any]:
    for expert in (state.get("collaboration") or {}).get("experts", []):
        if expert.get("name") == "map_life_specialist":
            analysis = (expert.get("output") or {}).get("analysis") or {}
            evidence = analysis.get("evidence") or {}
            if evidence:
                return evidence
            return compact_map_evidence(expert.get("output") or {})
    for call in state.get("tool_calls", []):
        if str(call.get("name") or "").startswith("amap_"):
            return compact_map_evidence(call.get("output") or {})
    return {}


def format_house_line(item: dict[str, Any]) -> str:
    title = item.get("title") or item.get("houseTitle") or f"房源 {item.get('houseId') or '-'}"
    place = " ".join(str(part) for part in [item.get("city"), item.get("district"), item.get("community")] if part)
    rent = f"{item.get('rentAmount')}元/月" if item.get("rentAmount") else "租金待确认"
    area = f"{item.get('area')}㎡" if item.get("area") else ""
    house_id = f"#{item.get('houseId')} " if item.get("houseId") else ""
    tail = "，".join(part for part in [place, rent, area] if part)
    return f"{house_id}{title}" + (f"，{tail}" if tail else "")


def format_poi(item: dict[str, Any]) -> str:
    distance = f"约{item.get('distance')}米" if item.get("distance") not in (None, "") else "距离未返回"
    return f"{item.get('name')}（{distance}）"


def persist_memory(state: TenantAgentState) -> TenantAgentState:
    state["checkpoints"].append("persist_memory")
    state["memory_updated"] = save_memory(state["session_id"], state["message"], state.get("answer") or "")
    if state["memory_updated"]:
        save_long_term_memory(to_tool_state(state), f"用户：{state['message']}\n助手：{state.get('answer') or ''}")
    return state


def to_tool_state(state: TenantAgentState) -> dict[str, Any]:
    return {
        "message": state.get("message"),
        "context": state.get("context") or {},
        "selected": (state.get("context") or {}).get("selected") or {},
        "filters": (state.get("context") or {}).get("filters") or {},
        "user": state.get("user") or {},
        "role": "tenant",
    }


def tool_call(name: str, label: str, output: dict[str, Any]) -> dict[str, Any]:
    return {
        "name": name,
        "label": label,
        "status": "success" if output.get("success", True) is not False else "error",
        "resultSummary": output.get("summary") or output.get("message") or label,
        "output": output,
    }


def collect_house_ids(state: TenantAgentState) -> list[int]:
    ids: list[int] = []
    slots = (state.get("route") or {}).get("slots") or {}
    selected = (state.get("context") or {}).get("selected") or {}
    selected_house_id = as_int(selected.get("houseId"))
    if selected_house_id and (state.get("route") or {}).get("intent") != "house_recommend":
        ids.append(selected_house_id)
    for call in state.get("tool_calls", []):
        output = call.get("output") or {}
        for key in ["matches", "rows", "items"]:
            for item in output.get(key) or []:
                if not isinstance(item, dict) or not house_matches_slots(item, slots):
                    continue
                item_id = as_int(item.get("houseId"))
                if item_id:
                    ids.append(item_id)
    if ids:
        return list(dict.fromkeys(ids))
    if selected_house_id and not slots.get("city") and not slots.get("district"):
        ids.append(selected_house_id)
    return list(dict.fromkeys(ids))


def build_next_actions(intent: str) -> list[str]:
    if intent == "correction":
        return ["补充正确需求", "重新推荐", "指定房源", "说明通勤/预算"]
    if intent == "house_recommend":
        return ["补充区域/预算", "选中房源", "发起预约", "提交意向"]
    if intent == "contract_risk":
        return ["查看合同", "核对押金", "补充条款", "提交确认意见"]
    if intent == "knowledge_answer":
        return ["查看命中知识", "转成看房清单", "继续追问"]
    return ["选中房源", "打开业务沟通", "继续追问"]


def to_response(state: TenantAgentState) -> dict[str, Any]:
    return {
        "answer": state.get("answer") or "",
        "intent": state.get("intent") or "context_answer",
        "intentLabel": state.get("intent_label") or INTENT_LABELS.get(state.get("intent") or "", state.get("intent") or ""),
        "skill": state.get("skill"),
        "houseIds": state.get("house_ids") or [],
        "toolCalls": state.get("tool_calls") or [],
        "collaboration": state.get("collaboration") or {
            "mode": "langgraph-supervisor-parallel-hybrid",
            "router": state.get("route"),
            "coordinator": state.get("coordinator"),
            "experts": [],
            "synthesis": {"strategy": "single final answer"},
        },
        "memoryUpdated": bool(state.get("memory_updated")),
        "suggestions": state.get("suggestions"),
        "nextActions": state.get("next_actions") or [],
        "checkpoints": state.get("checkpoints") or [],
        "rag": state.get("rag") or {},
    }
