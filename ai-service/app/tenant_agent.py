from concurrent.futures import ThreadPoolExecutor
from typing import Any, Callable

from app.business_rules import as_int, extract_budget, extract_city
from app.config import INTENT_LABELS
from app.skill_registry import skill_to_dict


def should_use_tenant_multi_agent(state: dict[str, Any], intent: str) -> bool:
    return state.get("role") == "tenant" and intent in {
        "house_recommend",
        "contract_risk",
        "knowledge_answer",
        "record_summary",
        "context_answer",
    }


def run_tenant_multi_agent(
    state: dict[str, Any],
    intent: str,
    skill: Any | None,
    call_tool_fn: Callable[..., dict[str, Any]],
    render_answer_fn: Callable[[str, dict[str, Any], list[dict[str, Any]]], str],
    refine_answer_fn: Callable[[dict[str, Any], str, list[dict[str, Any]], str, Any | None], str],
) -> dict[str, Any]:
    router = route_tenant_intent(state, intent, call_tool_fn)
    coordinator = coordinate_tenant_plan(state, router)
    collaboration = run_tenant_collaboration(state, coordinator, call_tool_fn)
    tool_calls = flatten_tool_calls(collaboration)
    fallback = synthesize_tenant_answer(state, router, coordinator, collaboration, tool_calls, render_answer_fn)
    answer = refine_answer_fn(state, router["intent"], tool_calls, fallback, skill)
    return {
        "answer": answer,
        "intent": router["intent"],
        "intentLabel": INTENT_LABELS.get(router["intent"], router["intent"]),
        "skill": skill_to_dict(skill),
        "toolCalls": tool_calls,
        "collaboration": {
            "mode": "tenant-supervisor-parallel-hybrid",
            "router": router,
            "coordinator": coordinator,
            "experts": [
                {
                    "name": item["name"],
                    "label": item["label"],
                    "status": item["status"],
                    "summary": item.get("summary"),
                    "toolNames": [call.get("name") for call in item.get("toolCalls", [])],
                }
                for item in collaboration
            ],
            "synthesis": {
                "strategy": "merge expert outputs, tool evidence and tenant context into one reply",
                "expertCount": len(collaboration),
            },
        },
    }


def route_tenant_intent(
    state: dict[str, Any],
    detected_intent: str,
    call_tool_fn: Callable[..., dict[str, Any]],
) -> dict[str, Any]:
    knowledge_call = call_tool_fn("search_knowledge_base", state)
    message = state.get("message") or ""
    city = extract_city(message) or state.get("filters", {}).get("city")
    budget = as_int((state.get("recommend") or {}).get("maxRent")) or extract_budget(message)
    selected = state.get("selected") or {}
    slots = {
        "city": city,
        "maxRent": budget,
        "houseId": selected.get("houseId"),
        "district": selected.get("district"),
        "moveInHint": "入住" in message or "搬" in message,
        "commuteHint": "通勤" in message or "地铁" in message or "上班" in message,
        "contractHint": "合同" in message or "押金" in message or "违约" in message,
    }
    if detected_intent in ["context_answer", "record_summary"] and any(
        slots[key] for key in ["city", "maxRent", "commuteHint", "contractHint"]
    ):
        routed_intent = "house_recommend" if not slots["contractHint"] else "contract_risk"
    else:
        routed_intent = detected_intent
    route = {
        "agent": "router",
        "intent": routed_intent,
        "detectedIntent": detected_intent,
        "slots": slots,
        "ragPrompt": build_rag_prompt(state, knowledge_call),
        "knowledgeToolCall": knowledge_call,
        "confidence": route_confidence(routed_intent, slots),
    }
    return route


def coordinate_tenant_plan(state: dict[str, Any], router: dict[str, Any]) -> dict[str, Any]:
    experts = ["house_search", "map_life", "risk_analysis"]
    intent = router["intent"]
    if intent == "knowledge_answer":
        experts = ["knowledge_policy", "risk_analysis"]
    elif intent == "contract_risk":
        experts = ["risk_analysis", "knowledge_policy", "business_context"]
    elif intent == "record_summary":
        experts = ["business_context", "risk_analysis"]
    return {
        "agent": "coordinator",
        "plan": {
            "tools": ["search_public_houses", "search_knowledge_base", "summarize_business_record"],
            "experts": experts,
            "parallel": True,
            "finalizer": "synthesis",
        },
        "rationale": "主管智能体按租户目标拆成检索、生活便利、风险和上下文专家，并行拿证据后统一生成回复。",
    }


def run_tenant_collaboration(
    state: dict[str, Any],
    coordinator: dict[str, Any],
    call_tool_fn: Callable[..., dict[str, Any]],
) -> list[dict[str, Any]]:
    experts = coordinator.get("plan", {}).get("experts") or []
    handlers = {
        "house_search": run_house_search_expert,
        "map_life": run_map_life_expert,
        "risk_analysis": run_risk_analysis_expert,
        "knowledge_policy": run_knowledge_policy_expert,
        "business_context": run_business_context_expert,
    }
    with ThreadPoolExecutor(max_workers=min(len(experts) or 1, 4)) as pool:
        futures = [
            pool.submit(handlers[name], state, call_tool_fn)
            for name in experts
            if name in handlers
        ]
        return [future.result() for future in futures]


def run_house_search_expert(state: dict[str, Any], call_tool_fn: Callable[..., dict[str, Any]]) -> dict[str, Any]:
    recommend = state.get("recommend") or {}
    call = call_tool_fn(
        "search_public_houses",
        state,
        query=state.get("message") or "",
        city=recommend.get("city") or extract_city(state.get("message") or ""),
        maxRent=as_int(recommend.get("maxRent")) or extract_budget(state.get("message") or ""),
    )
    matches = (call.get("output") or {}).get("matches") or []
    summary = "房源检索专家未命中可推荐房源。"
    if matches:
        summary = f"房源检索专家命中 {len(matches)} 套，优先看 {matches[0].get('title')}。"
    return expert_result("house_search", "房源检索专家", summary, [call])


def run_map_life_expert(state: dict[str, Any], call_tool_fn: Callable[..., dict[str, Any]]) -> dict[str, Any]:
    selected = state.get("selected") or {}
    message = state.get("message") or ""
    district = selected.get("district") or selected.get("city") or extract_city(message) or "目标区域"
    points = []
    if "地铁" in message or "通勤" in message:
        points.append("优先核实到地铁/公交站的步行时间和早晚高峰通勤时长")
    if "学校" in message or "学区" in message:
        points.append("学校和入学政策不能只看房源描述，需要以官方口径为准")
    if "安静" in message or "噪音" in message:
        points.append("看房时重点听临街、楼上楼下和电梯噪音")
    if not points:
        points.append("看房时同步确认通勤、商超、噪音、采光和夜间安全感")
    return expert_result(
        "map_life",
        "地图生活专家",
        f"地图生活专家建议围绕{district}核实：" + "；".join(points),
        [],
        {"district": district, "checks": points},
    )


def run_risk_analysis_expert(state: dict[str, Any], call_tool_fn: Callable[..., dict[str, Any]]) -> dict[str, Any]:
    selected = state.get("selected") or {}
    calls = [call_tool_fn("search_knowledge_base", state)]
    has_contract_context = any(
        selected.get(key) not in (None, "")
        for key in ["contractId", "startDate", "endDate", "depositAmount", "paymentCycle"]
    )
    if has_contract_context:
        calls.append(call_tool_fn("explain_contract_risk", state))
    risks = (calls[-1].get("output") or {}).get("risks") if has_contract_context else []
    summary = "风险分析专家建议确认押金、付款周期、维修责任、交付清单和提前退租规则。"
    if risks:
        summary = "风险分析专家发现：" + "；".join(risks[:4])
    return expert_result("risk_analysis", "风险分析专家", summary, calls)


def run_knowledge_policy_expert(state: dict[str, Any], call_tool_fn: Callable[..., dict[str, Any]]) -> dict[str, Any]:
    call = call_tool_fn("search_knowledge_base", state)
    matches = (call.get("output") or {}).get("matches") or []
    summary = "知识政策专家没有命中强相关知识。"
    if matches:
        titles = "、".join(item.get("title") or item.get("question") or "未命名知识" for item in matches[:3])
        summary = f"知识政策专家命中：{titles}"
    return expert_result("knowledge_policy", "知识政策专家", summary, [call])


def run_business_context_expert(state: dict[str, Any], call_tool_fn: Callable[..., dict[str, Any]]) -> dict[str, Any]:
    call = call_tool_fn("summarize_business_record", state)
    return expert_result(
        "business_context",
        "业务上下文专家",
        call.get("resultSummary") or "当前没有选中的业务记录。",
        [call],
    )


def synthesize_tenant_answer(
    state: dict[str, Any],
    router: dict[str, Any],
    coordinator: dict[str, Any],
    collaboration: list[dict[str, Any]],
    tool_calls: list[dict[str, Any]],
    render_answer_fn: Callable[[str, dict[str, Any], list[dict[str, Any]]], str],
) -> str:
    base = render_answer_fn(router["intent"], state, tool_calls)
    expert_lines = [item["summary"] for item in collaboration if item.get("summary")]
    next_step = "下一步建议：先选一套匹配预算和通勤的房源，再发起预约；看房前把押金、付款周期、噪音、采光和家具家电列成问题清单。"
    if router["intent"] == "contract_risk":
        next_step = "下一步建议：先核对合同字段和交付清单，再决定确认或补充条款。"
    if not expert_lines:
        return base
    return "\n".join([
        base,
        "",
        "多智能体协作结果：",
        *[f"- {line}" for line in expert_lines[:4]],
        next_step,
    ])


def flatten_tool_calls(collaboration: list[dict[str, Any]]) -> list[dict[str, Any]]:
    calls: list[dict[str, Any]] = []
    seen: set[tuple[str, str]] = set()
    for item in collaboration:
        for call in item.get("toolCalls", []):
            key = (call.get("name") or "", call.get("startedAt") or "")
            if key in seen:
                continue
            seen.add(key)
            calls.append(call)
    return calls


def build_rag_prompt(state: dict[str, Any], knowledge_call: dict[str, Any]) -> str:
    output = knowledge_call.get("output") or {}
    matches = output.get("matches") or []
    snippets = []
    for item in matches[:3]:
        title = item.get("title") or item.get("question") or "未命名知识"
        content = str(item.get("summary") or item.get("answer") or item.get("content") or "")[:120]
        snippets.append(f"{title}: {content}")
    return "\n".join([
        f"用户问题：{state.get('message') or ''}",
        f"当前角色：{state.get('roleLabel') or '租户'}",
        "RAG 命中：" + (" | ".join(snippets) if snippets else "无强命中"),
    ])


def route_confidence(intent: str, slots: dict[str, Any]) -> float:
    score = 0.62
    if intent == "house_recommend":
        score += 0.08
    if slots.get("city"):
        score += 0.08
    if slots.get("maxRent"):
        score += 0.08
    if slots.get("commuteHint") or slots.get("contractHint"):
        score += 0.06
    return min(round(score, 2), 0.92)


def expert_result(
    name: str,
    label: str,
    summary: str,
    tool_calls: list[dict[str, Any]],
    output: dict[str, Any] | None = None,
) -> dict[str, Any]:
    return {
        "name": name,
        "label": label,
        "status": "success" if all(call.get("status") == "success" for call in tool_calls) else "partial",
        "summary": summary,
        "toolCalls": tool_calls,
        "output": output or {},
    }
