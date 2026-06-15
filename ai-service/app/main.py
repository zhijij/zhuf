import os
from datetime import datetime
from typing import Any

import httpx
from fastapi import FastAPI
from app import runtime_store as store
from app.business_rules import (
    as_int,
    build_audit_reason,
    build_house_content,
    build_knowledge_content,
    chunk_text,
    clean_dict,
    default_knowledge_documents,
    extract_budget,
    extract_city,
    house_brief,
    knowledge_match_line,
    knowledge_reference_lines,
    normalize_source_type,
    role_context_intro,
    role_empty_answer,
    role_style_hint,
    same_id,
    score_house,
    seed_static_knowledge,
    stable_source_id,
)
from app.config import EMBEDDING_MODEL, INTENT_LABELS, KNOWLEDGE_SOURCE_TYPES, ROLE_LABELS
from app.langchain_runtime import build_langchain_tools, langchain_status, refine_with_langchain, tool_specs
from app.schemas import ChatRequest, HouseIndexRequest, KnowledgeIndexRequest, RecommendRequest
from app.skill_registry import list_skills, select_skill, skill_to_dict
from app.tenant_agent import run_tenant_multi_agent, should_use_tenant_multi_agent
from app.tooling import TOOL_REGISTRY, call_tool, tool, tool_description, tool_label
from app.vector_store import (
    count_indexed_houses,
    count_indexed_knowledge,
    delete_house_vectors,
    delete_knowledge_vectors,
    embedding_api_enabled,
    ensure_vector_store,
    search_vector_houses,
    search_vector_knowledge,
    upsert_house_vectors,
    upsert_knowledge_vectors,
)

app = FastAPI(title="Rental AI Service", version="0.2.0")


@app.get("/health")
def health():
    ensure_vector_store()
    langchain_tools = build_langchain_tools(TOOL_REGISTRY, tool_label, tool_description)
    return {
        "status": "ok",
        "indexedHouses": count_indexed_houses(),
        "indexedKnowledge": count_indexed_knowledge(),
        "agent": "ready",
        "vectorDbReady": store.DB_READY,
        "embeddingMode": "remote" if embedding_api_enabled() else "local-hash",
        "langchain": langchain_status(),
        "langchainToolCount": len(langchain_tools),
        "skills": [skill_to_dict(skill) for skill in list_skills()],
        "tools": tool_specs(TOOL_REGISTRY, tool_label, tool_description),
        "multiAgentModes": [
            {
                "role": "tenant",
                "mode": "tenant-supervisor-parallel-hybrid",
                "agents": ["router", "coordinator", "house_search", "map_life", "risk_analysis", "synthesis"],
            }
        ],
    }


@app.get("/api/v1/agent/capabilities")
def agent_capabilities():
    ensure_vector_store()
    langchain_tools = build_langchain_tools(TOOL_REGISTRY, tool_label, tool_description)
    return {
        "agent": "smart-rental-agent",
        "status": "ready",
        "indexedHouses": count_indexed_houses(),
        "indexedKnowledge": count_indexed_knowledge(),
        "vectorDbReady": store.DB_READY,
        "embeddingMode": "remote" if embedding_api_enabled() else "local-hash",
        "embeddingModel": EMBEDDING_MODEL if embedding_api_enabled() else "local-hash",
        "vectorStore": "PostgreSQL + pgvector",
        "knowledgeSources": KNOWLEDGE_SOURCE_TYPES,
        "langchain": langchain_status(),
        "langchainToolCount": len(langchain_tools),
        "skills": [skill_to_dict(skill) for skill in list_skills()],
        "tools": tool_specs(TOOL_REGISTRY, tool_label, tool_description),
        "multiAgentModes": [
            {
                "role": "tenant",
                "mode": "tenant-supervisor-parallel-hybrid",
                "agents": ["router", "coordinator", "house_search", "map_life", "risk_analysis", "synthesis"],
            }
        ],
        "mcpReady": True,
        "mcpPlan": "已提供 MCP 工具/资源清单，后续可独立成 MCP Server 对外注册。",
    }


@app.post("/api/v1/agent/chat")
def chat(request: ChatRequest):
    state = build_agent_state(request)
    intent = detect_intent(request.message, state)
    skill = select_skill(intent, state)
    if should_use_tenant_multi_agent(state, intent):
        result = run_tenant_multi_agent(
            state,
            intent,
            skill,
            call_tool,
            render_agent_answer,
            refine_with_llm_if_configured,
        )
        memory_updated = update_memory(state["sessionKey"], request.message, result["answer"])
        tool_calls = result["toolCalls"]
        return {
            **result,
            "houseIds": collect_house_ids(state, tool_calls),
            "memoryUpdated": memory_updated,
            "suggestions": extract_suggestions(tool_calls),
            "nextActions": build_next_actions(result["intent"], state, tool_calls),
        }

    tool_calls = run_agent_tools(intent, state)
    answer = render_agent_answer(intent, state, tool_calls)
    answer = refine_with_llm_if_configured(state, intent, tool_calls, answer, skill)
    memory_updated = update_memory(state["sessionKey"], request.message, answer)

    return {
        "answer": answer,
        "intent": intent,
        "intentLabel": INTENT_LABELS.get(intent, intent),
        "skill": skill_to_dict(skill),
        "houseIds": collect_house_ids(state, tool_calls),
        "toolCalls": tool_calls,
        "memoryUpdated": memory_updated,
        "suggestions": extract_suggestions(tool_calls),
        "nextActions": build_next_actions(intent, state, tool_calls),
    }


@app.post("/api/v1/agent/recommend")
def recommend(request: RecommendRequest):
    state = {
        "message": request.query,
        "role": request.role or "tenant",
        "roleLabel": ROLE_LABELS.get(request.role or "tenant", request.role or "tenant"),
        "roles": request.roles or [],
        "isAdmin": bool(request.isAdmin),
        "context": request.context or {},
        "selected": (request.context or {}).get("selected") or {},
        "filters": (request.context or {}).get("filters") or {},
        "visibleActions": (request.context or {}).get("visibleActions") or [],
        "summary": (request.context or {}).get("summary") or {},
        "sessionKey": f"user:{request.userId or 'anonymous'}",
        "transactionType": None,
        "transactionTitle": None,
        "recommend": {"city": request.city, "maxRent": request.maxRent},
        "memory": [],
    }
    skill = select_skill("house_recommend", state)
    if should_use_tenant_multi_agent(state, "house_recommend"):
        result = run_tenant_multi_agent(
            state,
            "house_recommend",
            skill,
            call_tool,
            render_agent_answer,
            refine_with_llm_if_configured,
        )
        tool_calls = result["toolCalls"]
        return {
            **result,
            "houseIds": collect_house_ids(state, tool_calls),
            "memoryUpdated": False,
            "suggestions": extract_suggestions(tool_calls),
            "nextActions": build_next_actions(result["intent"], state, tool_calls),
        }

    tool_calls = [
        call_tool(
            "search_public_houses",
            state,
            query=request.query,
            city=request.city,
            maxRent=request.maxRent,
        )
    ]
    answer = render_agent_answer("house_recommend", state, tool_calls)
    answer = refine_with_llm_if_configured(state, "house_recommend", tool_calls, answer, skill)
    return {
        "answer": answer,
        "intent": "house_recommend",
        "intentLabel": INTENT_LABELS["house_recommend"],
        "skill": skill_to_dict(skill),
        "houseIds": collect_house_ids(state, tool_calls),
        "toolCalls": tool_calls,
        "memoryUpdated": False,
        "nextActions": build_next_actions("house_recommend", state, tool_calls),
    }


@app.post("/api/v1/index/house")
def index_house(request: HouseIndexRequest):
    ensure_vector_store()
    document = request.document or {}
    action = (request.action or "upsert").lower()
    house_id = request.houseId or as_int(document.get("houseId"))

    if action == "delete":
        if house_id is not None:
            store.HOUSE_INDEX.pop(int(house_id), None)
            delete_house_vectors(int(house_id))
        return {
            "success": True,
            "taskId": request.taskId,
            "houseId": house_id,
            "action": action,
            "indexed": False,
            "chunkCount": 0,
            "documentTitle": document.get("title"),
            "message": "房源索引已删除",
        }

    content = str(document.get("content") or build_house_content(document))
    chunks = chunk_text(content)
    vectors_written = 0
    if house_id is not None:
        store.HOUSE_INDEX[int(house_id)] = {
            **document,
            "houseId": int(house_id),
            "content": content,
            "chunks": chunks,
            "indexedAt": datetime.now().isoformat(timespec="seconds"),
        }
        vectors_written = upsert_house_vectors(int(house_id), document, chunks)

    return {
        "success": True,
        "taskId": request.taskId,
        "houseId": house_id,
        "action": action,
        "indexed": house_id is not None,
        "chunkCount": len(chunks),
        "vectorCount": vectors_written,
        "documentTitle": document.get("title"),
        "message": "房源文档已解析并写入 AI 检索索引",
    }


@app.post("/api/v1/index/knowledge")
def index_knowledge(request: KnowledgeIndexRequest):
    ensure_vector_store()
    document = request.document or {}
    source_type = normalize_source_type(request.sourceType or document.get("sourceType") or "faq")
    source_id = str(request.sourceId or document.get("sourceId") or stable_source_id(source_type, document))
    action = (request.action or "upsert").lower()
    key = f"{source_type}:{source_id}"

    if action == "delete":
        store.KNOWLEDGE_INDEX.pop(key, None)
        delete_knowledge_vectors(source_type, source_id)
        return {
            "success": True,
            "taskId": request.taskId,
            "sourceType": source_type,
            "sourceId": source_id,
            "indexed": False,
            "chunkCount": 0,
            "message": "知识库文档已删除",
        }

    content = str(document.get("content") or build_knowledge_content(source_type, document))
    chunks = chunk_text(content)
    store.KNOWLEDGE_INDEX[key] = {
        **document,
        "sourceType": source_type,
        "sourceId": source_id,
        "content": content,
        "chunks": chunks,
        "indexedAt": datetime.now().isoformat(timespec="seconds"),
    }
    vectors_written = upsert_knowledge_vectors(source_type, source_id, document, chunks)
    return {
        "success": True,
        "taskId": request.taskId,
        "sourceType": source_type,
        "sourceId": source_id,
        "indexed": True,
        "chunkCount": len(chunks),
        "vectorCount": vectors_written,
        "documentTitle": document.get("title") or document.get("question"),
        "message": "知识库文档已解析并写入统一检索索引",
    }


@app.post("/api/v1/index/knowledge/seed")
def seed_knowledge():
    ensure_vector_store()
    items = default_knowledge_documents()
    written = 0
    documents = []
    for item in items:
        source_type = normalize_source_type(item.get("sourceType") or "faq")
        source_id = str(item.get("sourceId") or stable_source_id(source_type, item))
        document = {key: value for key, value in item.items() if key not in ["sourceType", "sourceId"]}
        content = str(document.get("content") or build_knowledge_content(source_type, document))
        chunks = chunk_text(content)
        key = f"{source_type}:{source_id}"
        store.KNOWLEDGE_INDEX[key] = {
            **document,
            "sourceType": source_type,
            "sourceId": source_id,
            "content": content,
            "chunks": chunks,
            "indexedAt": datetime.now().isoformat(timespec="seconds"),
        }
        written += upsert_knowledge_vectors(source_type, source_id, document, chunks)
        documents.append({
            "sourceType": source_type,
            "sourceId": source_id,
            "title": document.get("title"),
            "chunkCount": len(chunks),
        })
    return {
        "success": True,
        "documentCount": len(documents),
        "vectorCount": written,
        "documents": documents,
        "message": "基础合同、政策、FAQ、聊天记录和企业制度已写入统一知识库",
    }


@app.get("/api/v1/mcp/manifest")
def mcp_manifest():
    return {
        "name": "smart-rental-agent",
        "version": "0.2.0",
        "resources": [
            {"name": "skills", "description": "业务技能规则", "uri": "skill://smart-rental"},
            {
                "name": "knowledge",
                "description": "合同、政策、FAQ、聊天记录、企业制度和房源文档统一知识库",
                "uri": "knowledge://smart-rental",
                "sourceTypes": ["house", "contract", "policy", "faq", "chat", "enterprise"],
            },
        ],
        "tools": tool_specs(TOOL_REGISTRY, tool_label, tool_description),
        "notes": "当前是 MCP 兼容清单，工具调用仍由 ai-service 本地执行；后续可抽出标准 MCP Server 给 IDE、智能体平台或后台任务注册。",
    }


def build_agent_state(request: ChatRequest) -> dict[str, Any]:
    context = request.context or {}
    roles = request.roles or []
    role = context.get("workMode") or request.role or infer_role(roles)
    if role == "admin" and "auditor" in roles:
        role = "auditor"
    session_key = str(request.sessionId or request.userId or "anonymous")
    request_history = request.history or context.get("history") or []
    memory_history = store.SESSION_MEMORY.get(f"session:{session_key}", [])
    return {
        "message": request.message,
        "role": role,
        "roleLabel": ROLE_LABELS.get(role, role),
        "roles": roles,
        "isAdmin": bool(request.isAdmin),
        "context": context,
        "selected": context.get("selected") or {},
        "filters": context.get("filters") or {},
        "visibleActions": context.get("visibleActions") or [],
        "summary": context.get("summary") or {},
        "sessionKey": f"session:{session_key}",
        "transactionType": request.transactionType,
        "transactionTitle": request.transactionTitle,
        "recommend": {},
        "memory": merge_memory(memory_history, request_history),
    }


def infer_role(roles: list[str]) -> str:
    if "agent" in roles:
        return "agent"
    if "owner" in roles:
        return "owner"
    if "auditor" in roles:
        return "auditor"
    if "admin" in roles:
        return "admin"
    return "tenant"


def detect_intent(message: str, state: dict[str, Any]) -> str:
    text = (message or "").lower()
    role = state.get("role")
    if state.get("transactionType"):
        return "transaction_draft"
    if has_any(text, ["索引", "向量", "知识库", "入库", "召回"]):
        return "index_advice"
    if has_any(text, ["政策", "制度", "流程", "faq", "常见问题", "规则", "规范", "资料", "文档", "知识"]):
        return "knowledge_answer"
    if has_any(text, ["审核", "审查", "合规", "违规", "虚假", "待审"]) or (
        role in ["auditor", "admin"] and has_any(text, ["通过", "驳回", "发布", "下架", "风险"])
    ):
        return "compliance_review"
    if has_any(text, ["合同", "条款", "签约", "风险", "违约", "押金"]):
        return "contract_risk"
    if has_any(text, ["文案", "发布", "卖点", "描述", "标题"]):
        return "listing_copy"
    if has_any(text, ["推荐", "匹配", "找房", "房源", "预算", "地铁", "两居", "整租"]):
        return "house_recommend"
    if has_any(text, ["跟进", "沟通", "话术", "回复", "催促", "约看"]):
        return "followup_message"
    if has_any(text, ["总结", "摘要", "当前业务", "进度", "下一步", "梳理"]):
        return "record_summary"
    return "context_answer"


def has_any(text: str, keywords: list[str]) -> bool:
    return any(keyword in text for keyword in keywords)


def run_agent_tools(intent: str, state: dict[str, Any]) -> list[dict[str, Any]]:
    plan = {
        "house_recommend": ["search_public_houses", "search_knowledge_base"],
        "transaction_draft": ["summarize_business_record", "draft_transaction_form"],
        "record_summary": ["summarize_business_record"],
        "compliance_review": ["search_knowledge_base", "summarize_business_record", "review_house_compliance"],
        "contract_risk": ["search_knowledge_base", "summarize_business_record", "explain_contract_risk"],
        "listing_copy": ["search_knowledge_base", "summarize_business_record", "draft_listing_copy"],
        "followup_message": ["search_knowledge_base", "summarize_business_record", "draft_followup_message"],
        "index_advice": ["summarize_index_state", "search_knowledge_base"],
        "knowledge_answer": ["search_knowledge_base", "summarize_business_record"],
        "context_answer": ["search_knowledge_base", "summarize_business_record"],
    }.get(intent, ["summarize_business_record"])
    return [call_tool(name, state) for name in plan]


@tool("search_public_houses")
def search_public_houses(
    state: dict[str, Any],
    query: str | None = None,
    city: str | None = None,
    maxRent: int | None = None,
) -> dict[str, Any]:
    ensure_vector_store()
    query = query or state.get("message") or ""
    city = city or extract_city(query) or state.get("filters", {}).get("city")
    max_rent = maxRent or extract_budget(query)
    candidates = search_vector_houses(query, city, max_rent)
    if not candidates:
        candidates = list(store.HOUSE_INDEX.values())
    selected = state.get("selected") or {}
    if selected.get("houseId") and not any(same_id(item.get("houseId"), selected.get("houseId")) for item in candidates):
        candidates.append(selected)

    scored = []
    for house in candidates:
        score = score_house(house, query, city, max_rent)
        if score > 0:
            scored.append((score, house))
    scored.sort(key=lambda item: item[0], reverse=True)
    matches = [house_brief(house, score) for score, house in scored[:5]]

    if not matches:
        summary = "当前 AI 索引里没有命中房源；请先在业务页处理房源索引任务，或选中房源后再咨询。"
    else:
        summary = f"命中 {len(matches)} 套房源：" + "、".join(item["title"] for item in matches)
    return {
        "summary": summary,
        "matches": matches,
        "query": query,
        "city": city,
        "maxRent": max_rent,
    }


@tool("summarize_business_record")
def summarize_business_record(state: dict[str, Any], **_: Any) -> dict[str, Any]:
    selected = state.get("selected") or {}
    if not selected:
        return {
            "summary": "当前未选中业务记录。",
            "record": {},
        }

    fields = []
    for label, key in [
        ("标题", "title"),
        ("类型", "recordType"),
        ("状态", "statusLabel"),
        ("城市", "city"),
        ("区域", "district"),
        ("租金", "rentAmount"),
        ("面积", "area"),
        ("房源ID", "houseId"),
        ("户主ID", "ownerId"),
        ("中介ID", "agentId"),
        ("租户ID", "tenantId"),
        ("预约时间", "appointmentTime"),
        ("合同开始", "startDate"),
        ("合同结束", "endDate"),
    ]:
        value = selected.get(key)
        if value not in (None, ""):
            fields.append(f"{label}：{value}")
    summary = "；".join(fields) if fields else "当前记录字段较少，需要补充业务信息。"
    return {
        "summary": summary,
        "record": selected,
    }


@tool("search_knowledge_base")
def search_knowledge_base(state: dict[str, Any], **_: Any) -> dict[str, Any]:
    ensure_vector_store()
    query = state.get("message") or ""
    matches = search_vector_knowledge(query, state.get("role"))
    if not matches:
        fallback = seed_static_knowledge(query, state)
        matches = fallback
    summary = "未命中统一知识库内容。" if not matches else "命中知识库：" + "、".join(item["title"] for item in matches[:4])
    return {
        "summary": summary,
        "matches": matches[:6],
        "sourceTypes": sorted({item.get("sourceType") for item in matches if item.get("sourceType")}),
    }


@tool("draft_transaction_form")
def draft_transaction_form(state: dict[str, Any], **_: Any) -> dict[str, Any]:
    selected = state.get("selected") or {}
    tx_type = state.get("transactionType") or ""
    title = state.get("transactionTitle") or "业务处理"
    role_label = state.get("roleLabel") or "当前角色"
    suggestions: dict[str, Any] = {}

    if tx_type == "ownerCreateHouse":
        suggestions = {
            "title": selected.get("title") or "",
            "city": selected.get("city") or "",
            "district": selected.get("district") or "",
            "address": selected.get("address") or "",
            "rentAmount": selected.get("rentAmount"),
            "area": selected.get("area"),
            "description": "建议补充小区、通勤、采光、家具家电、看房时间，提交审核前确认租金和地址准确。",
        }
    elif tx_type in ["tenantAppointment"]:
        suggestions = {
            "remark": "希望确认房源仍可租、看房时间可协调，并重点了解采光、噪音、通勤和押金退还规则。",
        }
    elif tx_type in ["tenantIntention"]:
        suggestions = {
            "intentionLevel": "2",
            "note": "对房源位置和预算基本匹配，需进一步确认入住时间、付款周期、家具家电和合同条款。",
        }
    elif tx_type in ["tenantDeal", "agentDealIntention", "agentDealHouse"]:
        rent = as_int(selected.get("rentAmount"))
        suggestions = {
            "rentAmount": rent,
            "depositAmount": rent,
            "paymentCycle": "月付",
            "contractContent": "交付前核验水电燃气读数、家具家电清单和钥匙数量；押金退还条件、维修责任、提前退租违约责任需写入合同。",
        }
    elif tx_type in ["ownerEntrust", "agentApplyEntrust"]:
        suggestions = {
            "entrustScope": "发布房源、客户筛选、预约带看、意向跟进、合同协同",
            "commissionRate": 0.02,
        }
    elif tx_type == "agentFollowIntention":
        suggestions = {
            "intentionLevel": selected.get("intentionLevel") or "2",
            "note": "已同步房源核心信息，下一步确认预算上限、入住时间、付款周期，并约定复看或签约时间。",
        }
    elif tx_type in ["adminApproveHouse", "adminRejectHouse"]:
        review = review_house_compliance(state)
        suggestions = review.get("suggestions") or {}
        if tx_type == "adminApproveHouse" and review.get("recommendation") == "reject":
            suggestions = {
                **suggestions,
                "auditStatus": "3",
                "auditReason": "不建议直接通过：" + suggestions.get("auditReason", review.get("summary", "当前房源仍存在合规风险。")),
            }
        elif tx_type == "adminRejectHouse" and review.get("recommendation") == "approve":
            suggestions = {
                "auditStatus": "3",
                "auditReason": "系统规则未发现硬性驳回项；如仍需驳回，请补充明确证据，例如地址不实、权属不清、图片或描述与事实不符。",
            }
    elif "Reject" in tx_type or "Void" in tx_type or "Terminate" in tx_type or tx_type in ["agentInvalidIntention"]:
        suggestions = {
            "reason": f"基于当前{role_label}视角，建议先核实双方沟通记录、业务状态和合同约束，再记录明确原因。",
        }
    elif "contract" in tx_type.lower():
        suggestions = {
            "opinion": "已阅读合同核心信息，建议确认租金、押金、付款周期、租期、维修责任、提前解约和交付清单后再提交确认。",
        }
    else:
        suggestions = {
            "note": f"{title}建议先核对当前记录状态、相关用户和可执行动作，提交后同步到业务沟通会话。",
        }

    summary = f"已为“{title}”生成可写入表单的字段建议。"
    return {
        "summary": summary,
        "suggestions": clean_dict(suggestions),
        "transactionType": tx_type,
        "transactionTitle": title,
    }


@tool("review_house_compliance")
def review_house_compliance(state: dict[str, Any], **_: Any) -> dict[str, Any]:
    selected = state.get("selected") or {}
    if not selected:
        return {
            "summary": "当前未选中房源，无法形成合规结论。",
            "recommendation": "reject",
            "checks": [],
            "risks": ["未选择待审核房源"],
            "suggestions": {
                "auditStatus": "3",
                "auditReason": "未选择待审核房源，无法完成合规审查。",
            },
        }

    checks: list[dict[str, Any]] = []
    risks: list[str] = []
    warnings: list[str] = []

    required_fields = [
        ("标题", "title"),
        ("城市", "city"),
        ("区域", "district"),
        ("详细地址", "address"),
        ("租金", "rentAmount"),
        ("面积", "area"),
        ("户主", "ownerId"),
    ]
    for label, key in required_fields:
        value = selected.get(key)
        passed = value not in (None, "")
        checks.append({"label": label, "passed": passed})
        if not passed:
            risks.append(f"{label}缺失")

    rent = as_int(selected.get("rentAmount"))
    area = as_int(selected.get("area"))
    if rent is not None and rent <= 0:
        risks.append("租金必须大于 0")
    if area is not None and area <= 0:
        risks.append("面积必须大于 0")

    description = str(selected.get("description") or "").strip()
    if not description:
        warnings.append("缺少房源描述，建议补充小区、通勤、家具家电、看房时间。")
    elif len(description) < 20:
        warnings.append("房源描述过短，建议补充影响租户决策的信息。")

    status = str(selected.get("auditStatus") or selected.get("status") or "")
    if status and status not in ["1", "pending", "待审核", "待合规审核"]:
        warnings.append("当前房源看起来不是待审核状态，提交前请确认是否重复审核。")

    risk_text = " ".join(
        str(selected.get(key) or "")
        for key in ["title", "subtitle", "description", "tags", "facilities", "address", "community"]
    )
    risky_keywords = ["群租", "隔断", "虚假", "无证", "无合同", "先打款", "私下转账", "百分百", "低价急租", "学区承诺", "押一付十二"]
    matched_keywords = [keyword for keyword in risky_keywords if keyword in risk_text]
    if matched_keywords:
        risks.append("存在需人工核验的风险词：" + "、".join(matched_keywords))

    recommendation = "reject" if risks else "approve"
    audit_reason = build_audit_reason(selected, recommendation, risks, warnings)
    summary = "建议通过并发布。" if recommendation == "approve" else "建议驳回，待户主补充或修正后重新提交。"
    if risks:
        summary += " 主要问题：" + "；".join(risks[:4])
    elif warnings:
        summary += " 注意事项：" + "；".join(warnings[:3])

    return {
        "summary": summary,
        "recommendation": recommendation,
        "checks": checks,
        "risks": risks,
        "warnings": warnings,
        "suggestions": {
            "auditStatus": "2" if recommendation == "approve" else "3",
            "auditReason": audit_reason,
        },
    }


@tool("explain_contract_risk")
def explain_contract_risk(state: dict[str, Any], **_: Any) -> dict[str, Any]:
    selected = state.get("selected") or {}
    risks = []
    if not selected.get("startDate") or not selected.get("endDate"):
        risks.append("租期开始和结束日期需要明确。")
    if not selected.get("rentAmount"):
        risks.append("月租金未在当前记录中体现。")
    if not selected.get("depositAmount"):
        risks.append("押金金额或退还规则需要补充。")
    if not selected.get("tenantId"):
        risks.append("租户身份信息需要确认。")
    if not selected.get("ownerId"):
        risks.append("户主身份信息需要确认。")
    if not risks:
        risks.append("核心字段较完整，仍建议核对交付清单、维修责任、违约责任和提前退租约定。")
    return {
        "summary": "；".join(risks),
        "risks": risks,
    }


@tool("draft_listing_copy")
def draft_listing_copy(state: dict[str, Any], **_: Any) -> dict[str, Any]:
    selected = state.get("selected") or {}
    title = selected.get("title") or "优质出租房源"
    city = selected.get("city") or "本地"
    district = selected.get("district") or "核心区域"
    rent = selected.get("rentAmount")
    area = selected.get("area")
    features = [
        f"{city}{district}",
        f"{area}平米" if area else "",
        f"月租{rent}元" if rent else "",
        selected.get("tags") or "",
    ]
    feature_text = "，".join(item for item in features if item)
    copy = f"{title}：{feature_text}。建议突出通勤、采光、家具家电、周边生活配套和可看房时间，减少夸张描述，保证与审核信息一致。"
    return {
        "summary": copy,
        "copy": copy,
    }


@tool("draft_followup_message")
def draft_followup_message(state: dict[str, Any], **_: Any) -> dict[str, Any]:
    selected = state.get("selected") or {}
    role = state.get("role")
    if role == "agent":
        text = "您好，我已整理该房源的租金、入住时间和看房安排。方便的话请确认预算上限和可看房时间，我会同步推进下一步。"
    elif role == "owner":
        text = "您好，我这边已确认房源信息。请中介同步客户意向、带看反馈和签约风险点，便于我判断是否继续委托。"
    else:
        text = "您好，我对这套房源比较感兴趣，想进一步确认入住时间、付款周期、押金退还规则和家具家电情况。"
    return {
        "summary": text,
        "message": text,
        "bizId": selected.get("houseId") or selected.get("entrustId") or selected.get("contractId"),
    }


@tool("summarize_index_state")
def summarize_index_state(state: dict[str, Any], **_: Any) -> dict[str, Any]:
    ensure_vector_store()
    selected = state.get("selected") or {}
    house_id = selected.get("houseId")
    indexed = store.HOUSE_INDEX.get(int(house_id)) if house_id is not None and str(house_id).isdigit() else None
    if indexed:
        summary = f"当前房源 #{house_id} 已进入 AI 检索索引，共 {len(indexed.get('chunks') or [])} 个文本块。"
    else:
        summary = f"当前向量库中共有 {count_indexed_houses()} 套房源、{count_indexed_knowledge()} 条通用知识。若业务页显示未索引，请先创建并处理索引任务。"
    return {
        "summary": summary,
        "indexedHouseCount": count_indexed_houses(),
        "indexedKnowledgeCount": count_indexed_knowledge(),
        "selectedIndexed": bool(indexed),
        "vectorDbReady": store.DB_READY,
    }


def render_agent_answer(intent: str, state: dict[str, Any], tool_calls: list[dict[str, Any]]) -> str:
    selected = state.get("selected") or {}
    role_label = state.get("roleLabel") or "当前角色"
    role = state.get("role") or "tenant"
    context_title = selected.get("title") or selected.get("subtitle") or ""
    summaries = [call.get("resultSummary") for call in tool_calls if call.get("resultSummary")]
    next_actions = build_next_actions(intent, state, tool_calls)

    if intent == "house_recommend":
        search = first_output(tool_calls, "search_public_houses")
        matches = search.get("matches") or []
        if not matches:
            return search.get("summary") or "现在还没检索到合适房源。你可以换一个预算、区域，或者先在业务页选中房源再继续问我。"
        lines = [f"我先按{role_label}当前需求筛出几套更值得优先看的房源："]
        for index, item in enumerate(matches, start=1):
            rent = f"{item.get('rentAmount')}元/月" if item.get("rentAmount") else "租金待确认"
            place = " ".join(part for part in [item.get("city"), item.get("district")] if part)
            lines.append(f"{index}. {item.get('title')}，{place}，{rent}")
        if role == "tenant":
            lines.append("建议先选一套最贴近预算和通勤的房源，我可以继续帮你列看房问题、预约备注和合同风险。")
        elif role == "agent":
            lines.append("如果这是给客户匹配，下一步建议确认预算上限、入住时间和可看房时间，再推进预约。")
        else:
            lines.append("你可以选中其中一套，我再结合当前业务判断下一步动作。")
        return "\n".join(lines)

    if intent == "transaction_draft":
        draft = first_output(tool_calls, "draft_transaction_form")
        fields = "、".join((draft.get("suggestions") or {}).keys()) or "备注"
        return f"我已经结合当前业务把“{state.get('transactionTitle') or '事务'}”要填的重点整理出来了，建议你先看这几个字段：{fields}。确认后再提交，会更稳。"

    if intent == "compliance_review":
        review = first_output(tool_calls, "review_house_compliance")
        knowledge = first_output(tool_calls, "search_knowledge_base")
        risks = review.get("risks") or []
        warnings = review.get("warnings") or []
        lines = [review.get("summary") or "我先把当前房源的合规情况过了一遍。"]
        lines.extend(knowledge_reference_lines(knowledge))
        if risks:
            lines.append("现在最需要优先处理的是：" + "；".join(risks))
        if warnings:
            lines.append("另外建议一起补齐：" + "；".join(warnings))
        lines.append("审核结论还是要由审核员手动提交，我这里给的是辅助判断和意见草稿。")
        return "\n".join(lines)

    if intent == "contract_risk":
        knowledge = first_output(tool_calls, "search_knowledge_base")
        risks = first_output(tool_calls, "explain_contract_risk").get("risks") or []
        references = knowledge_reference_lines(knowledge)
        if not risks:
            lines = ["当前合同没有读到明显风险，但我还是建议再核对租期、押金、付款周期和违约责任。"]
            lines.extend(references)
            return "\n".join(lines)
        lines = ["我先把这份合同里值得重点确认的地方拎出来："]
        lines.extend(f"- {risk}" for risk in risks)
        lines.extend(references)
        return "\n".join(lines)

    if intent == "listing_copy":
        copy = first_output(tool_calls, "draft_listing_copy").get("copy")
        references = knowledge_reference_lines(first_output(tool_calls, "search_knowledge_base"))
        if copy:
            return "\n".join([copy, *references])
        return "先选中一套具体房源，我再按当前信息帮你写更像正式发布稿的文案。"

    if intent == "followup_message":
        message = first_output(tool_calls, "draft_followup_message").get("message")
        references = knowledge_reference_lines(first_output(tool_calls, "search_knowledge_base"))
        if message:
            return "\n".join([message, *references])
        return "当前还缺少足够的业务上下文。你先选中委托、预约或意向记录，我再帮你生成更贴场景的话术。"

    if intent == "index_advice":
        return first_output(tool_calls, "summarize_index_state").get("summary") or "当前还拿不到索引状态，你可以稍后再试一次。"

    if intent == "knowledge_answer":
        knowledge = first_output(tool_calls, "search_knowledge_base")
        matches = knowledge.get("matches") or []
        if not matches:
            return "我现在没有在统一知识库里找到可靠内容。可以先把政策、FAQ、合同模板或企业制度导入知识库，再让我基于它回答。"
        lines = [f"我按{role_label}视角先查了统一知识库，能参考的是："]
        for item in matches[:4]:
            lines.append(knowledge_match_line(item))
        lines.append("如果要落到当前业务，我可以继续把这些依据整理成审核意见、沟通话术、合同检查项或推荐理由。")
        return "\n".join(lines)

    if intent == "record_summary":
        prefix = f"当前业务：{context_title}。" if context_title else "当前业务摘要："
        action_text = f"\n下一步建议：{'、'.join(next_actions[:3])}" if next_actions else ""
        return prefix + "\n" + "\n".join(summaries) + action_text

    if context_title:
        action_text = f" 下一步可以先做：{'、'.join(next_actions[:3])}。" if next_actions else ""
        return f"{role_context_intro(role, role_label)}我已结合“{context_title}”看当前上下文。{(' '.join(summaries) if summaries else '')}{action_text}"
    return role_empty_answer(role, role_label)


def refine_with_llm_if_configured(
    state: dict[str, Any],
    intent: str,
    tool_calls: list[dict[str, Any]],
    fallback: str,
    skill: Any | None = None,
) -> str:
    base_url = os.getenv("AI_LLM_BASE_URL", "").rstrip("/")
    api_key = os.getenv("AI_LLM_API_KEY", "")
    model = os.getenv("AI_LLM_MODEL", "")
    if not base_url or not api_key or not model:
        return fallback

    system_prompt = (
        "你是智能AI房屋租赁系统的企业级租赁业务智能体。"
        "必须基于工具结果回答，不编造不存在的房源、用户或合同。"
        "输出中文，简洁、自然、像真实业务助手，不要像测试脚本或接口文档。"
        "优先围绕当前角色、当前选中业务和下一步动作来回答。"
        "不要机械复述规则，要结合当前角色、当前业务对象和最近对话连续回答。"
        f"\n当前角色表达风格：{role_style_hint(state.get('role'))}"
    )
    if skill:
        system_prompt += f"\n\n当前业务 Skill：{skill.name}\n{skill.prompt}"

    user_prompt = (
        f"用户问题：{state.get('message')}\n"
        f"识别意图：{INTENT_LABELS.get(intent, intent)}\n"
        f"当前角色：{state.get('roleLabel')}\n"
        f"最近对话：{compact_memory(state.get('memory') or [])}\n"
        f"当前选中业务：{state.get('selected')}\n"
        f"可用 LangChain 工具：{tool_specs(TOOL_REGISTRY, tool_label, tool_description)}\n"
        f"工具结果：{compact_tool_results(tool_calls)}\n"
        f"规则答案：{fallback}"
    )
    messages = [
        ("system", system_prompt),
        ("user", user_prompt),
    ]
    try:
        langchain_answer = refine_with_langchain(
            base_url=base_url,
            api_key=api_key,
            model=model,
            messages=messages,
            timeout=8,
        )
        if langchain_answer:
            return langchain_answer

        openai_messages = [
            {"role": role, "content": content}
            for role, content in messages
        ]
        response = httpx.post(
            f"{base_url}/chat/completions",
            headers={"Authorization": f"Bearer {api_key}"},
            json={"model": model, "messages": openai_messages, "temperature": 0.2},
            timeout=8,
        )
        response.raise_for_status()
        data = response.json()
        content = data.get("choices", [{}])[0].get("message", {}).get("content")
        return content.strip() if content else fallback
    except Exception:
        return fallback


def compact_tool_results(tool_calls: list[dict[str, Any]]) -> list[dict[str, Any]]:
    return [
        {
            "name": call.get("name"),
            "status": call.get("status"),
            "summary": call.get("resultSummary"),
            "output": call.get("output"),
        }
        for call in tool_calls
    ]


def first_output(tool_calls: list[dict[str, Any]], name: str) -> dict[str, Any]:
    for call in tool_calls:
        if call.get("name") == name:
            return call.get("output") or {}
    return {}


def collect_house_ids(state: dict[str, Any], tool_calls: list[dict[str, Any]]) -> list[int]:
    ids: list[int] = []
    selected = state.get("selected") or {}
    if selected.get("houseId") is not None:
        ids.append(int(selected["houseId"]))
    for call in tool_calls:
        output = call.get("output") or {}
        for item in output.get("matches") or []:
            house_id = as_int(item.get("houseId"))
            if house_id is not None:
                ids.append(house_id)
    return list(dict.fromkeys(ids))


def extract_suggestions(tool_calls: list[dict[str, Any]]) -> dict[str, Any] | None:
    for call in tool_calls:
        suggestions = (call.get("output") or {}).get("suggestions")
        if suggestions:
            return suggestions
    return None


def build_next_actions(intent: str, state: dict[str, Any], tool_calls: list[dict[str, Any]]) -> list[str]:
    if intent == "house_recommend":
        return ["选中房源", "发起预约", "提交意向", "打开业务沟通"]
    if intent == "transaction_draft":
        return ["检查表单字段", "提交事务", "同步业务会话"]
    if intent == "compliance_review":
        review = first_output(tool_calls, "review_house_compliance")
        if review.get("recommendation") == "approve":
            return ["复核字段", "通过并发布", "创建索引任务"]
        return ["查看驳回原因", "驳回房源", "通知户主补充"]
    if intent == "contract_risk":
        return ["核对合同字段", "补充条款", "提交确认或拒绝意见"]
    if intent == "index_advice":
        return ["创建索引任务", "处理待处理任务", "重新发起推荐"]
    if intent == "knowledge_answer":
        return ["查看命中内容", "转成业务建议", "补充知识库文档"]
    visible = state.get("visibleActions") or []
    return [item.get("label") for item in visible if item.get("label")][:4]


def update_memory(session_key: str, user_message: str, answer: str) -> bool:
    history = store.SESSION_MEMORY.setdefault(session_key, [])
    history.extend([
        {"role": "user", "content": user_message},
        {"role": "assistant", "content": answer},
    ])
    store.SESSION_MEMORY[session_key] = history[-12:]
    return True


def merge_memory(memory_history: list[dict[str, Any]], request_history: list[Any]) -> list[dict[str, str]]:
    merged: list[dict[str, str]] = []
    for item in memory_history or []:
        role = str(item.get("role") or "").strip()
        content = str(item.get("content") or "").strip()
        if role and content:
            merged.append({"role": role, "content": content})
    for item in request_history or []:
        role = str(getattr(item, "role", None) or (item.get("role") if isinstance(item, dict) else "")).strip()
        content = str(getattr(item, "content", None) or (item.get("content") if isinstance(item, dict) else "")).strip()
        if role and content:
            merged.append({"role": role, "content": content})
    deduped: list[dict[str, str]] = []
    seen: set[tuple[str, str]] = set()
    for item in merged:
        key = (item["role"], item["content"])
        if key in seen:
            continue
        seen.add(key)
        deduped.append(item)
    return deduped[-12:]


def compact_memory(history: list[dict[str, str]]) -> list[str]:
    return [f"{item.get('role')}: {item.get('content')}" for item in history[-6:]]
