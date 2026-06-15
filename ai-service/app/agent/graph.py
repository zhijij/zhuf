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
from app.business_rules import as_int, extract_budget, extract_city, knowledge_match_line
from app.config import INTENT_LABELS
from app.langchain_runtime import invoke_structured_json, refine_with_langchain
from app.skill_registry import select_skill, skill_to_dict

_COMPILED_GRAPH = None
_CHECKPOINTER = get_checkpointer()


class TenantRoute(BaseModel):
    intent: str = Field(default="house_recommend")
    route: str = Field(default="collaboration")
    slots: dict[str, Any] = Field(default_factory=dict)
    missing_slots: list[str] = Field(default_factory=list)
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
    try:
        from langgraph.graph import END, StateGraph

        builder = StateGraph(TenantAgentState)
        builder.add_node("rag_prefetch", rag_prefetch)
        builder.add_node("router", router)
        builder.add_node("coordinator", coordinator)
        builder.add_node("llm_unconfigured", llm_unconfigured)
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
                "llm_unconfigured": "llm_unconfigured",
                "smalltalk": "smalltalk",
                "slot_filling": "slot_filling",
                "collaboration": "collaboration",
                "retrieval_executor": "retrieval_executor",
            },
        )
        builder.add_edge("llm_unconfigured", "synthesis")
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
    except Exception:
        return FallbackGraph()


class FallbackGraph:
    def invoke(self, state: TenantAgentState, config: dict[str, Any] | None = None) -> TenantAgentState:
        state = reset_turn_state(state)
        state = rag_prefetch(state)
        state = router(state)
        state = coordinator(state)
        next_node = route_after_coordinator(state)
        flow = {
            "llm_unconfigured": [llm_unconfigured],
            "smalltalk": [smalltalk],
            "slot_filling": [slot_filling],
            "retrieval_executor": [retrieval_executor, analyst],
            "collaboration": [collaboration],
        }.get(next_node, [collaboration])
        for node in [*flow, synthesis, persist_memory]:
            state = node(state)
        return state


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
    if llm_route:
        return llm_route

    message = state["message"]
    slots = {
        "city": (
            extract_city(message)
            or (state.get("recommend") or {}).get("city")
            or (state["context"].get("filters") or {}).get("city")
        ),
        "maxRent": extract_budget(message) or (state.get("recommend") or {}).get("maxRent"),
        "houseId": (state["context"].get("selected") or {}).get("houseId"),
        "contractId": (state["context"].get("selected") or {}).get("contractId"),
    }
    text = message.lower()
    if not message.strip():
        route = TenantRoute(intent="smalltalk", route="smalltalk", slots=slots, confidence=0.8)
    elif any(word in text for word in ["你好", "在吗", "谢谢"]):
        route = TenantRoute(intent="smalltalk", route="smalltalk", slots=slots, confidence=0.85)
    elif any(word in text for word in ["不对", "不对吧", "不太对", "不准确", "不是这个", "你理解错了", "重新来", "重说", "纠正一下"]):
        route = TenantRoute(intent="correction", route="smalltalk", slots=slots, confidence=0.95)
    elif any(word in text for word in ["合同", "押金", "违约", "风险"]):
        route = TenantRoute(intent="contract_risk", route="collaboration", slots=slots, confidence=0.86)
    elif any(word in text for word in ["推荐", "找房", "房源", "预算", "地铁", "通勤", "整租", "合租"]):
        missing = []
        if not slots["city"]:
            missing.append("city")
        route = TenantRoute(intent="house_recommend", route="slot_filling" if missing else "collaboration", slots=slots, missing_slots=missing, confidence=0.9)
    elif any(word in text for word in ["政策", "规则", "流程", "知识", "faq"]):
        route = TenantRoute(intent="knowledge_answer", route="retrieval_executor", slots=slots, confidence=0.78)
    else:
        route = TenantRoute(intent="context_answer", route="collaboration", slots=slots, confidence=0.68)
    return route.model_dump()


def should_prefetch_rag(state: TenantAgentState) -> bool:
    route = state.get("route") or {}
    intent = route.get("intent")
    next_route = route.get("route")
    if intent in {"smalltalk", "correction"}:
        return False
    return next_route in {"retrieval_executor", "collaboration"}


def infer_rag_source_types(state: TenantAgentState) -> list[str]:
    intent = (state.get("route") or {}).get("intent")
    message = str(state.get("message") or "")
    if intent == "contract_risk":
        return ["contract", "policy", "faq"]
    if intent == "knowledge_answer":
        if any(word in message for word in ["合同", "押金", "违约", "签约"]):
            return ["contract", "policy", "faq"]
        if any(word in message for word in ["流程", "政策", "规则", "制度", "看房"]):
            return ["faq", "policy", "enterprise", "contract"]
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
        return None

    message = state["message"]
    selected = (state.get("context") or {}).get("selected") or {}
    filters = (state.get("context") or {}).get("filters") or {}
    recommend = state.get("recommend") or {}
    messages = [
        ("system",
         "你是租户智能体的路由器。"
         "请根据用户消息和上下文，输出 JSON。"
         "只允许 intent 为 smalltalk, correction, house_recommend, contract_risk, knowledge_answer, context_answer。"
         "只允许 route 为 smalltalk, slot_filling, collaboration, retrieval_executor。"
         "如果是找房但缺少城市，可给 missing_slots=[\"city\"]。"),
        ("user",
         json.dumps({
             "message": message,
             "selected": selected,
             "filters": filters,
             "recommend": recommend,
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
        return None
    try:
        route = TenantRoute(
            intent=str(data.get("intent") or "context_answer"),
            route=str(data.get("route") or "collaboration"),
            slots=data.get("slots") if isinstance(data.get("slots"), dict) else {},
            missing_slots=data.get("missing_slots") if isinstance(data.get("missing_slots"), list) else [],
            confidence=float(data.get("confidence") or 0.75),
        )
        return route.model_dump()
    except Exception:
        return None


def coordinator(state: TenantAgentState) -> TenantAgentState:
    state["checkpoints"].append("coordinator")
    if should_prefetch_rag(state):
        state = ensure_rag_loaded(state)
    llm_plan = llm_coordinator_plan(state)
    if llm_plan:
        state["coordinator"] = llm_plan
        return state

    route = state.get("route") or {}
    experts = ["house_search_specialist", "map_life_specialist", "risk_analysis_specialist"]
    if route.get("intent") == "contract_risk":
        experts = ["risk_analysis_specialist", "house_search_specialist"]
    if route.get("intent") == "knowledge_answer":
        experts = []
    state["coordinator"] = {
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
    }
    return state


def llm_coordinator_plan(state: TenantAgentState) -> dict[str, Any] | None:
    base_url = os.getenv("AI_LLM_BASE_URL", "").rstrip("/")
    api_key = os.getenv("AI_LLM_API_KEY", "")
    model = os.getenv("AI_LLM_MODEL", "")
    if not base_url or not api_key or not model:
        return None

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
        return None
    try:
        plan = TenantCoordinatorPlan(
            route=str(data.get("route") or route.get("route") or "collaboration"),
            experts=[str(item) for item in (data.get("experts") or []) if item],
            tool_plan=[str(item) for item in (data.get("tool_plan") or []) if item],
            ask_user=[str(item) for item in (data.get("ask_user") or []) if item],
            reason=str(data.get("reason") or ""),
        )
        return {
            "agent": "coordinator",
            "toolPlan": plan.tool_plan or ["search_houses", "vector_search", "amap_search_poi"],
            "confirmationPolicy": {
                "mediumHighRiskRequiresConfirmed": True,
                "destructiveActionsDeniedByDefault": True,
            },
            "collaborationPlan": {
                "parallel": bool(plan.experts),
                "experts": plan.experts,
            },
            "askUser": plan.ask_user,
            "reason": plan.reason,
        }
    except Exception:
        return None


def route_after_coordinator(state: TenantAgentState) -> Literal["llm_unconfigured", "smalltalk", "slot_filling", "collaboration", "retrieval_executor"]:
    route = state.get("route") or {}
    if route.get("route") == "smalltalk":
        return "smalltalk"
    if route.get("missing_slots"):
        return "slot_filling"
    if route.get("route") == "retrieval_executor":
        return "retrieval_executor"
    if not (state.get("coordinator") or {}).get("collaborationPlan", {}).get("experts"):
        return "retrieval_executor"
    return "collaboration"


def llm_unconfigured(state: TenantAgentState) -> TenantAgentState:
    state["checkpoints"].append("llm_unconfigured")
    state["analysis"] = {"summary": "大模型未配置，已使用规则和工具结果生成回复。"}
    return state


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
    return state


def analyst(state: TenantAgentState) -> TenantAgentState:
    state["checkpoints"].append("analyst")
    hits = (state.get("retrieval") or {}).get("hits") or []
    lines = [knowledge_match_line(item) for item in hits[:4]]
    state["analysis"] = {"summary": "\n".join(lines) if lines else "未检索到强相关知识。"}
    return state


def house_search_specialist(state: TenantAgentState) -> dict[str, Any]:
    slots = (state.get("route") or {}).get("slots") or {}
    result = search_houses(
        to_tool_state(state),
        query=state["message"],
        city=slots.get("city"),
        max_rent=as_int(slots.get("maxRent")),
    )
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
        result = amap.search_poi("地铁 商超 医院", city=city, limit=50)
        tool_name = "amap_search_poi"
        tool_label = "查询周边配套"
    checks = ["通勤时间", "地铁/公交步行距离", "商超便利", "噪音与采光", "夜间安全感"]
    analysis = analyze_map_life_context(state, result)
    summary = analysis.get("summary") or ((result.get("summary") or "地图生活专家使用兜底清单") + "；看房时重点核实：" + "、".join(checks))
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
    if llm_text:
        return {
            "summary": llm_text,
            "evidence": evidence,
            "source": "llm",
        }

    highlights = evidence.get("highlights") or []
    totals = evidence.get("totals") or {}
    route = evidence.get("route") or {}
    parts = []
    if route.get("summary"):
        parts.append(f"通勤：{route.get('summary')}")
    for label in ["交通", "生活", "医疗", "教育"]:
        item = totals.get(label) or {}
        nearest = item.get("nearest")
        if nearest:
            parts.append(f"{label}：{item.get('count', 0)} 个点位，最近 {nearest.get('name')} 约 {nearest.get('distance')} 米")
        else:
            parts.append(f"{label}：周边 {item.get('count', 0)} 个点位")
    if highlights:
        parts.append("重点关注：" + "；".join(highlights[:3]))
    summary = "；".join(parts) if parts else (result.get("summary") or "暂无可分析的周边数据")
    return {
        "summary": summary,
        "evidence": evidence,
        "source": "rules",
    }


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
        return None
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
    if intent == "smalltalk":
        answer = (state.get("analysis") or {}).get("summary")
    elif intent == "correction":
        answer = (state.get("analysis") or {}).get("summary") or "我收到纠正了，你可以直接补充正确目标。"
    elif route.get("missing_slots"):
        answer = (state.get("analysis") or {}).get("summary") + "，补齐后我再并行调用房源、地图和风险专家。"
    elif intent == "knowledge_answer":
        answer = "我先查了统一知识库：\n" + ((state.get("analysis") or {}).get("summary") or "暂无命中。")
    else:
        expert_summaries = [
            item.get("summary")
            for item in (state.get("collaboration") or {}).get("experts", [])
            if item.get("summary")
        ]
        base = "我按租户智能体的主管协作流程处理了这次请求。"
        if intent == "house_recommend":
            base = "我先按预算、区域、通勤和签约风险一起看。"
        elif intent == "contract_risk":
            base = "我先按合同、押金和沟通证据风险一起看。"
        answer = "\n".join([base, *[f"- {line}" for line in expert_summaries]])
        if expert_summaries:
            answer += "\n下一步建议：选中具体房源后发起预约或意向，提交前再确认押金、付款周期、维修责任和提前退租条款。"
    state["answer"] = answer or "我已完成本轮分析。"
    state["next_actions"] = build_next_actions(intent)
    state["suggestions"] = None
    state["house_ids"] = collect_house_ids(state)
    return state


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
    selected = (state.get("context") or {}).get("selected") or {}
    house_id = as_int(selected.get("houseId"))
    if house_id:
        ids.append(house_id)
    for call in state.get("tool_calls", []):
        output = call.get("output") or {}
        for key in ["matches", "rows", "items"]:
            for item in output.get(key) or []:
                item_id = as_int(item.get("houseId"))
                if item_id:
                    ids.append(item_id)
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
