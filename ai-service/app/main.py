import os
import re
import math
import json
import hashlib
from datetime import datetime
from typing import Any, Callable

import httpx
import psycopg
from fastapi import FastAPI
from app.langchain_runtime import build_langchain_tools, langchain_status, refine_with_langchain, tool_specs
from app.schemas import ChatRequest, HouseIndexRequest, KnowledgeIndexRequest, RecommendRequest
from app.skill_registry import list_skills, select_skill, skill_to_dict

app = FastAPI(title="Rental AI Service", version="0.2.0")

HOUSE_INDEX: dict[int, dict[str, Any]] = {}
KNOWLEDGE_INDEX: dict[str, dict[str, Any]] = {}
SESSION_MEMORY: dict[str, list[dict[str, str]]] = {}
DB_READY = False

VECTOR_DB_HOST = os.getenv("VECTOR_DB_HOST", "vector-db")
VECTOR_DB_PORT = int(os.getenv("VECTOR_DB_PORT", "5432"))
VECTOR_DB_NAME = os.getenv("VECTOR_DB_NAME", "smart_rental_ai")
VECTOR_DB_USER = os.getenv("VECTOR_DB_USER", "postgres")
VECTOR_DB_PASSWORD = os.getenv("VECTOR_DB_PASSWORD", "postgres")
EMBEDDING_DIM = int(os.getenv("VECTOR_DB_EMBEDDING_DIM", "64"))
REMOTE_EMBEDDING_DIM = int(os.getenv("AI_EMBEDDING_DIM", str(EMBEDDING_DIM)))
EMBEDDING_BASE_URL = os.getenv("AI_EMBEDDING_BASE_URL", "").rstrip("/")
EMBEDDING_API_KEY = os.getenv("AI_EMBEDDING_API_KEY", "")
EMBEDDING_MODEL = os.getenv("AI_EMBEDDING_MODEL", "")

ROLE_LABELS = {
    "tenant": "租户",
    "owner": "户主",
    "agent": "中介",
    "contract": "合同协作",
    "admin": "管理员",
    "auditor": "房源审核员",
}

INTENT_LABELS = {
    "house_recommend": "房源推荐",
    "transaction_draft": "事务表单建议",
    "record_summary": "业务摘要",
    "compliance_review": "房源合规审查",
    "contract_risk": "合同风险审阅",
    "listing_copy": "房源文案",
    "followup_message": "沟通跟进",
    "index_advice": "索引与知识库",
    "knowledge_answer": "知识库问答",
    "context_answer": "业务问答",
}


@app.get("/health")
def health():
    ensure_vector_store()
    langchain_tools = build_langchain_tools(TOOL_REGISTRY, tool_label, tool_description)
    return {
        "status": "ok",
        "indexedHouses": count_indexed_houses(),
        "indexedKnowledge": count_indexed_knowledge(),
        "agent": "ready",
        "vectorDbReady": DB_READY,
        "embeddingMode": "remote" if embedding_api_enabled() else "local-hash",
        "langchain": langchain_status(),
        "langchainToolCount": len(langchain_tools),
        "skills": [skill_to_dict(skill) for skill in list_skills()],
        "tools": tool_specs(TOOL_REGISTRY, tool_label, tool_description),
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
        "vectorDbReady": DB_READY,
        "embeddingMode": "remote" if embedding_api_enabled() else "local-hash",
        "embeddingModel": EMBEDDING_MODEL if embedding_api_enabled() else "local-hash",
        "vectorStore": "PostgreSQL + pgvector",
        "knowledgeSources": ["house", "contract", "policy", "faq", "chat", "enterprise"],
        "langchain": langchain_status(),
        "langchainToolCount": len(langchain_tools),
        "skills": [skill_to_dict(skill) for skill in list_skills()],
        "tools": tool_specs(TOOL_REGISTRY, tool_label, tool_description),
        "mcpReady": True,
        "mcpPlan": "已提供 MCP 工具/资源清单，后续可独立成 MCP Server 对外注册。",
    }


@app.post("/api/v1/agent/chat")
def chat(request: ChatRequest):
    state = build_agent_state(request)
    intent = detect_intent(request.message, state)
    skill = select_skill(intent, state)
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
        "roles": request.roles or [],
        "isAdmin": bool(request.isAdmin),
        "context": request.context or {},
        "selected": (request.context or {}).get("selected") or {},
        "sessionKey": f"user:{request.userId or 'anonymous'}",
        "transactionType": None,
        "transactionTitle": None,
        "recommend": {"city": request.city, "maxRent": request.maxRent},
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
    skill = select_skill("house_recommend", state)
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
            HOUSE_INDEX.pop(int(house_id), None)
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
        HOUSE_INDEX[int(house_id)] = {
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
        KNOWLEDGE_INDEX.pop(key, None)
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
    KNOWLEDGE_INDEX[key] = {
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
        KNOWLEDGE_INDEX[key] = {
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
    memory_history = SESSION_MEMORY.get(f"session:{session_key}", [])
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


TOOL_REGISTRY: dict[str, Callable[..., dict[str, Any]]] = {}


def tool(name: str):
    def decorator(func: Callable[..., dict[str, Any]]):
        TOOL_REGISTRY[name] = func
        return func
    return decorator


def call_tool(name: str, state: dict[str, Any], **kwargs) -> dict[str, Any]:
    started = datetime.now().isoformat(timespec="seconds")
    try:
        output = TOOL_REGISTRY[name](state, **kwargs)
        return {
            "name": name,
            "label": tool_label(name),
            "args": kwargs,
            "status": "success",
            "resultSummary": output.get("summary", "工具已完成"),
            "output": output,
            "startedAt": started,
        }
    except Exception as exc:
        return {
            "name": name,
            "label": tool_label(name),
            "args": kwargs,
            "status": "error",
            "resultSummary": str(exc)[:300],
            "output": {},
            "startedAt": started,
        }


def tool_label(name: str) -> str:
    labels = {
        "search_public_houses": "检索房源索引",
        "summarize_business_record": "读取当前业务",
        "draft_transaction_form": "生成事务表单",
        "review_house_compliance": "审查房源合规",
        "explain_contract_risk": "审阅合同风险",
        "draft_listing_copy": "生成房源文案",
        "draft_followup_message": "生成沟通建议",
        "summarize_index_state": "检查索引状态",
        "search_knowledge_base": "检索统一知识库",
    }
    return labels.get(name, name)


def tool_description(name: str) -> str:
    descriptions = {
        "search_public_houses": "Use pgvector and business filters to find public rental houses.",
        "summarize_business_record": "Read and summarize the currently selected business record.",
        "draft_transaction_form": "Generate structured suggestions for a modal transaction form.",
        "review_house_compliance": "Review house listing compliance and produce audit suggestions.",
        "explain_contract_risk": "Inspect contract fields and summarize signing risks.",
        "draft_listing_copy": "Draft compliant listing copy from the selected house.",
        "draft_followup_message": "Draft role-specific chat follow-up text.",
        "summarize_index_state": "Inspect vector index and knowledge-base state.",
        "search_knowledge_base": "Search policies, FAQ, contracts, chat records, enterprise rules and indexed houses.",
    }
    return descriptions.get(name, tool_label(name))


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
        candidates = list(HOUSE_INDEX.values())
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
    indexed = HOUSE_INDEX.get(int(house_id)) if house_id is not None and str(house_id).isdigit() else None
    if indexed:
        summary = f"当前房源 #{house_id} 已进入 AI 检索索引，共 {len(indexed.get('chunks') or [])} 个文本块。"
    else:
        summary = f"当前向量库中共有 {count_indexed_houses()} 套房源、{count_indexed_knowledge()} 条通用知识。若业务页显示未索引，请先创建并处理索引任务。"
    return {
        "summary": summary,
        "indexedHouseCount": count_indexed_houses(),
        "indexedKnowledgeCount": count_indexed_knowledge(),
        "selectedIndexed": bool(indexed),
        "vectorDbReady": DB_READY,
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
    history = SESSION_MEMORY.setdefault(session_key, [])
    history.extend([
        {"role": "user", "content": user_message},
        {"role": "assistant", "content": answer},
    ])
    SESSION_MEMORY[session_key] = history[-12:]
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


def score_house(house: dict[str, Any], query: str, city: str | None, max_rent: int | None) -> int:
    content = " ".join(
        str(house.get(key) or "")
        for key in ["title", "city", "district", "community", "address", "tags", "description", "content"]
    ).lower()
    score = 0
    for token in tokenize(query):
        if token and token in content:
            score += 3
    if city and city in str(house.get("city") or ""):
        score += 8
    rent = as_int(house.get("rentAmount"))
    if max_rent and rent:
        score += 8 if rent <= max_rent else -6
    vector_score = house.get("_vectorScore")
    if vector_score is not None:
        score += int(max(float(vector_score), 0) * 20)
    if house.get("houseId"):
        score += 1
    return max(score, 0)


def house_brief(house: dict[str, Any], score: int) -> dict[str, Any]:
    return {
        "houseId": house.get("houseId"),
        "title": house.get("title") or f"房源 {house.get('houseId')}",
        "city": house.get("city"),
        "district": house.get("district"),
        "rentAmount": house.get("rentAmount"),
        "area": house.get("area"),
        "score": score,
    }


def tokenize(text: str) -> list[str]:
    raw = re.split(r"[\s,，。；;、/]+", (text or "").lower())
    tokens = [item for item in raw if len(item) >= 2]
    for keyword in ["地铁", "整租", "两居", "一居", "近", "预算", "通勤", "押金"]:
        if keyword in text:
            tokens.append(keyword)
    return tokens


def extract_budget(text: str) -> int | None:
    matches = re.findall(r"(\d{3,6})\s*(?:元|块|以内|以下|预算)?", text or "")
    if not matches:
        return None
    values = [int(item) for item in matches]
    return max(values)


def extract_city(text: str) -> str | None:
    for city in ["北京", "上海", "广州", "深圳", "杭州", "南京", "成都", "武汉", "西安", "天津", "重庆", "苏州"]:
        if city in (text or ""):
            return city
    return None


def build_house_content(document: dict[str, Any]) -> str:
    labels = [
        ("标题", "title"),
        ("城市", "city"),
        ("区域", "district"),
        ("小区", "community"),
        ("地址", "address"),
        ("租金", "rentAmount"),
        ("押金", "depositAmount"),
        ("面积", "area"),
        ("朝向", "orientation"),
        ("出租方式", "rentType"),
        ("装修", "decoration"),
        ("设施", "facilities"),
        ("标签", "tags"),
        ("描述", "description"),
    ]
    lines = []
    for label, key in labels:
        value = document.get(key)
        if value not in (None, ""):
            lines.append(f"{label}：{value}")
    return "\n".join(lines)


def build_knowledge_content(source_type: str, document: dict[str, Any]) -> str:
    labels = [
        ("标题", "title"),
        ("问题", "question"),
        ("答案", "answer"),
        ("适用角色", "roles"),
        ("业务类型", "bizType"),
        ("摘要", "summary"),
        ("正文", "content"),
        ("条款", "clauses"),
        ("风险点", "risks"),
        ("处理流程", "process"),
    ]
    lines = [f"知识类型：{source_type_label(source_type)}"]
    for label, key in labels:
        value = document.get(key)
        if value not in (None, ""):
            lines.append(f"{label}：{value}")
    return "\n".join(lines)


def normalize_source_type(source_type: str) -> str:
    mapping = {
        "house": "house",
        "contract": "contract",
        "policy": "policy",
        "faq": "faq",
        "chat": "chat",
        "enterprise": "enterprise",
        "制度": "enterprise",
        "政策": "policy",
        "合同": "contract",
        "聊天": "chat",
    }
    return mapping.get(str(source_type or "").lower(), "faq")


def source_type_label(source_type: str | None) -> str:
    labels = {
        "house": "房源",
        "contract": "合同",
        "policy": "政策",
        "faq": "FAQ",
        "chat": "聊天记录",
        "enterprise": "企业制度",
    }
    return labels.get(source_type or "", source_type or "知识")


def knowledge_match_line(item: dict[str, Any]) -> str:
    title = item.get("title") or item.get("question") or "未命名知识"
    source_label = source_type_label(item.get("sourceType"))
    content = str(item.get("summary") or item.get("answer") or item.get("content") or "").strip()
    snippet = content.replace("\n", " ")[:90]
    if snippet:
        return f"- {title}（{source_label}）：{snippet}"
    return f"- {title}（{source_label}）"


def knowledge_reference_lines(knowledge: dict[str, Any]) -> list[str]:
    matches = knowledge.get("matches") or []
    if not matches:
        return []
    titles = []
    for item in matches[:2]:
        title = item.get("title") or item.get("question")
        if title:
            titles.append(f"{title}（{source_type_label(item.get('sourceType'))}）")
    return ["参考知识库：" + "、".join(titles)] if titles else []


def role_style_hint(role: str | None) -> str:
    hints = {
        "tenant": "像租户找房顾问，关心预算、通勤、入住时间、看房问题和合同风险，少讲后台术语。",
        "owner": "像户主房源经营助理，帮他补齐发布资料、审核缺口、委托中介和合同确认事项。",
        "agent": "像中介成交推进助理，关注客户意向、跟进节奏、预约安排、话术和转化动作。",
        "auditor": "像房源合规审核助理，只围绕字段完整性、风险词、证据和审核意见，不替人最终审批。",
        "admin": "像平台后台助手，强调权限、配置和系统状态；超级管理员不参与普通租赁业务。",
        "contract": "像合同风控助理，聚焦租期、押金、付款周期、维修责任、交付清单和违约责任。",
    }
    return hints.get(role or "", "像真实租赁业务助手，基于当前记录给出下一步动作。")


def role_context_intro(role: str | None, role_label: str) -> str:
    if role == "tenant":
        return "我先按租户决策顺序看：预算、通勤、入住和合同风险。"
    if role == "owner":
        return "我先按户主经营顺序看：资料完整度、审核缺口、委托和签约风险。"
    if role == "agent":
        return "我先按中介推进顺序看：客户意向、约看节奏、跟进话术和成交动作。"
    if role in ["auditor", "admin"]:
        return "我先按审核员视角看：字段完整性、风险证据和可写入的审核意见。"
    if role == "contract":
        return "我先按合同风控视角看：金额、租期、押金、责任和违约约定。"
    return f"我会站在{role_label}视角继续判断。"


def role_empty_answer(role: str | None, role_label: str) -> str:
    if role == "tenant":
        return "你可以直接说预算、城市、通勤或想看的房型。我会按租户视角给你筛房源、列看房问题，再提醒合同风险。"
    if role == "owner":
        return "你可以选中自己的房源，或者说要新建/补充哪套房源。我会帮你整理发布资料、审核缺口、委托话术和签约检查项。"
    if role == "agent":
        return "你可以选中委托、预约或意向记录。我会帮你判断下一步跟进动作，并生成能直接发给客户或户主的话术。"
    if role in ["auditor", "admin"]:
        return "请选中待审核房源，或导入政策/FAQ/合同模板。我会按审核规则给出风险点和审核意见草稿。"
    if role == "contract":
        return "请选中合同记录。我会帮你检查租期、押金、付款周期、交付清单、维修责任和违约条款。"
    return f"我会按{role_label}的业务视角继续协助你。你可以告诉我具体目标，我会结合当前记录直接给建议。"


def stable_source_id(source_type: str, document: dict[str, Any]) -> str:
    raw = json.dumps({"sourceType": source_type, "document": document}, ensure_ascii=False, sort_keys=True)
    return hashlib.sha1(raw.encode("utf-8")).hexdigest()[:16]


def seed_static_knowledge(query: str, state: dict[str, Any]) -> list[dict[str, Any]]:
    role = state.get("role")
    seeds = default_knowledge_documents()
    scored = []
    text = query or ""
    for item in seeds:
        score = 0
        content = item["title"] + item["content"]
        for token in tokenize(text):
            if token in content:
                score += 2
        if role == "tenant" and item["sourceType"] == "faq":
            score += 2
        if role == "agent" and item["sourceId"] == "agent-followup-sop":
            score += 2
        if role in ["owner", "auditor"] and item["sourceType"] == "policy":
            score += 2
        if score > 0:
            scored.append((score, item))
    scored.sort(key=lambda item: item[0], reverse=True)
    return [
        {
            **item,
            "score": score,
        }
        for score, item in scored[:5]
    ]


def default_knowledge_documents() -> list[dict[str, Any]]:
    return [
        {
            "sourceType": "policy",
            "sourceId": "rental-audit-baseline",
            "title": "房源合规审核基础规则",
            "roles": ["auditor", "owner"],
            "content": "房源发布前需要核对标题、地址、租金、面积、户主信息、图片和描述一致性；疑似虚假、隔断群租、诱导私下转账、夸张承诺的内容应驳回或要求补证。",
        },
        {
            "sourceType": "contract",
            "sourceId": "contract-risk-baseline",
            "title": "租赁合同风险检查清单",
            "roles": ["tenant", "owner", "agent"],
            "content": "合同确认前应核对租期、租金、押金、付款周期、维修责任、交付清单、提前退租、违约责任和双方身份信息。AI 只能提示风险，最终合同确认由业务用户提交。",
        },
        {
            "sourceType": "faq",
            "sourceId": "tenant-appointment-faq",
            "title": "租户预约看房常见问题",
            "roles": ["tenant", "agent"],
            "content": "预约前建议确认房源是否仍可租、看房时间、通勤、噪音、采光、家具家电、押金退还规则和付款方式。看房后应记录满意点、顾虑和下一步意向。",
        },
        {
            "sourceType": "enterprise",
            "sourceId": "agent-followup-sop",
            "title": "中介跟进 SOP",
            "roles": ["agent"],
            "content": "中介跟进应记录预算、入住时间、付款周期、看房反馈、客户等级和下一次沟通时间。高意向客户优先安排复看或签约；无效意向必须记录明确原因。",
        },
        {
            "sourceType": "chat",
            "sourceId": "chat-record-usage",
            "title": "聊天记录使用规则",
            "roles": ["tenant", "owner", "agent", "auditor"],
            "content": "聊天记录可用于总结上下文、生成沟通话术和提取业务证据线索，但不能替代合同、审核结论或数据库中的真实业务状态。",
        },
    ]


def build_audit_reason(
    selected: dict[str, Any],
    recommendation: str,
    risks: list[str],
    warnings: list[str],
) -> str:
    title = selected.get("title") or f"房源 {selected.get('houseId') or ''}".strip()
    if recommendation == "approve":
        points = ["标题、地址、租金、面积、户主信息满足发布审核要求"]
        if warnings:
            points.append("建议发布后继续完善：" + "；".join(warnings[:2]))
        return f"{title}经合规审查可通过发布。" + "；".join(points) + "。"
    reason = "；".join(risks[:6]) if risks else "当前房源关键信息不足，需户主补充后重新提交"
    return f"{title}暂不建议发布，原因：{reason}。请户主修正后重新提交审核。"


def chunk_text(text: str, size: int = 500) -> list[str]:
    text = text.strip()
    if not text:
        return []
    return [text[index:index + size] for index in range(0, len(text), size)]


def clean_dict(data: dict[str, Any]) -> dict[str, Any]:
    return {key: value for key, value in data.items() if value not in (None, "")}


def as_int(value: Any) -> int | None:
    if value is None or str(value).strip() == "":
        return None
    try:
        return int(float(str(value)))
    except ValueError:
        return None


def same_id(left: Any, right: Any) -> bool:
    return str(left or "") == str(right or "")


def ensure_vector_store() -> None:
    global DB_READY
    if DB_READY:
        return
    try:
        with db_connection() as conn:
            with conn.cursor() as cur:
                cur.execute("CREATE EXTENSION IF NOT EXISTS vector")
                cur.execute(
                    f"""
                    CREATE TABLE IF NOT EXISTS ai_house_chunks (
                        id BIGSERIAL PRIMARY KEY,
                        house_id BIGINT NOT NULL,
                        chunk_index INT NOT NULL,
                        title TEXT,
                        city VARCHAR(64),
                        district VARCHAR(64),
                        rent_amount BIGINT,
                        area DOUBLE PRECISION,
                        payload JSONB NOT NULL,
                        content TEXT NOT NULL,
                        embedding vector({EMBEDDING_DIM}) NOT NULL,
                        created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                        updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                        UNIQUE (house_id, chunk_index)
                    )
                    """
                )
                cur.execute(
                    f"""
                    CREATE TABLE IF NOT EXISTS ai_knowledge_chunks (
                        id BIGSERIAL PRIMARY KEY,
                        source_type VARCHAR(32) NOT NULL,
                        source_id VARCHAR(128) NOT NULL,
                        chunk_index INT NOT NULL,
                        title TEXT,
                        roles TEXT,
                        payload JSONB NOT NULL,
                        content TEXT NOT NULL,
                        embedding vector({EMBEDDING_DIM}) NOT NULL,
                        created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                        updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                        UNIQUE (source_type, source_id, chunk_index)
                    )
                    """
                )
            conn.commit()
        DB_READY = True
    except Exception:
        DB_READY = False


def db_connection():
    return psycopg.connect(
        host=VECTOR_DB_HOST,
        port=VECTOR_DB_PORT,
        dbname=VECTOR_DB_NAME,
        user=VECTOR_DB_USER,
        password=VECTOR_DB_PASSWORD,
        autocommit=False,
    )


def embedding_api_enabled() -> bool:
    return bool(EMBEDDING_BASE_URL and EMBEDDING_API_KEY and EMBEDDING_MODEL)


def create_embedding(text: str) -> list[float]:
    if embedding_api_enabled():
        remote = create_remote_embedding(text)
        if remote:
            return normalize_vector(remote, REMOTE_EMBEDDING_DIM)
    return create_local_embedding(text, EMBEDDING_DIM)


def create_remote_embedding(text: str) -> list[float] | None:
    try:
        response = httpx.post(
            f"{EMBEDDING_BASE_URL}/embeddings",
            headers={"Authorization": f"Bearer {EMBEDDING_API_KEY}"},
            json={"model": EMBEDDING_MODEL, "input": text},
            timeout=12,
        )
        response.raise_for_status()
        data = response.json()
        embedding = data.get("data", [{}])[0].get("embedding")
        return embedding if isinstance(embedding, list) else None
    except Exception:
        return None


def create_local_embedding(text: str, dim: int) -> list[float]:
    vector = [0.0] * dim
    for token in tokenize(text) or [text]:
        digest = hashlib.sha256(token.encode("utf-8")).digest()
        for index in range(0, len(digest), 2):
            bucket = digest[index] % dim
            sign = 1.0 if digest[index + 1] % 2 == 0 else -1.0
            vector[bucket] += sign
    return normalize_vector(vector, dim)


def normalize_vector(vector: list[float], dim: int) -> list[float]:
    padded = [float(value) for value in vector[:dim]]
    if len(padded) < dim:
        padded.extend([0.0] * (dim - len(padded)))
    norm = math.sqrt(sum(value * value for value in padded))
    if norm <= 0:
        return [0.0] * dim
    return [round(value / norm, 8) for value in padded]


def vector_literal(vector: list[float]) -> str:
    return "[" + ",".join(f"{value:.8f}" for value in vector) + "]"


def upsert_house_vectors(house_id: int, document: dict[str, Any], chunks: list[str]) -> int:
    if not DB_READY:
        return 0
    written = 0
    try:
        with db_connection() as conn:
            with conn.cursor() as cur:
                cur.execute("DELETE FROM ai_house_chunks WHERE house_id = %s", (house_id,))
                for chunk_index, chunk in enumerate(chunks):
                    vector = create_embedding(chunk)
                    cur.execute(
                        f"""
                        INSERT INTO ai_house_chunks (
                            house_id, chunk_index, title, city, district, rent_amount, area, payload, content, embedding, updated_at
                        ) VALUES (
                            %s, %s, %s, %s, %s, %s, %s, %s::jsonb, %s, %s::vector, NOW()
                        )
                        """,
                        (
                            house_id,
                            chunk_index,
                            document.get("title"),
                            document.get("city"),
                            document.get("district"),
                            as_int(document.get("rentAmount")),
                            float(document.get("area")) if document.get("area") not in (None, "") else None,
                            json.dumps(document, ensure_ascii=False),
                            chunk,
                            vector_literal(vector),
                        ),
                    )
                    written += 1
            conn.commit()
    except Exception:
        return 0
    return written


def upsert_knowledge_vectors(source_type: str, source_id: str, document: dict[str, Any], chunks: list[str]) -> int:
    if not DB_READY:
        return 0
    written = 0
    try:
        with db_connection() as conn:
            with conn.cursor() as cur:
                cur.execute(
                    "DELETE FROM ai_knowledge_chunks WHERE source_type = %s AND source_id = %s",
                    (source_type, source_id),
                )
                for chunk_index, chunk in enumerate(chunks):
                    vector = create_embedding(chunk)
                    cur.execute(
                        f"""
                        INSERT INTO ai_knowledge_chunks (
                            source_type, source_id, chunk_index, title, roles, payload, content, embedding, updated_at
                        ) VALUES (
                            %s, %s, %s, %s, %s, %s::jsonb, %s, %s::vector, NOW()
                        )
                        """,
                        (
                            source_type,
                            source_id,
                            chunk_index,
                            document.get("title") or document.get("question"),
                            ",".join(document.get("roles") or []) if isinstance(document.get("roles"), list) else document.get("roles"),
                            json.dumps({**document, "sourceType": source_type, "sourceId": source_id}, ensure_ascii=False),
                            chunk,
                            vector_literal(vector),
                        ),
                    )
                    written += 1
            conn.commit()
    except Exception:
        return 0
    return written


def delete_house_vectors(house_id: int) -> None:
    if not DB_READY:
        return
    try:
        with db_connection() as conn:
            with conn.cursor() as cur:
                cur.execute("DELETE FROM ai_house_chunks WHERE house_id = %s", (house_id,))
            conn.commit()
    except Exception:
        return


def delete_knowledge_vectors(source_type: str, source_id: str) -> None:
    if not DB_READY:
        return
    try:
        with db_connection() as conn:
            with conn.cursor() as cur:
                cur.execute(
                    "DELETE FROM ai_knowledge_chunks WHERE source_type = %s AND source_id = %s",
                    (source_type, source_id),
                )
            conn.commit()
    except Exception:
        return


def count_indexed_houses() -> int:
    if not DB_READY:
        return len(HOUSE_INDEX)
    try:
        with db_connection() as conn:
            with conn.cursor() as cur:
                cur.execute("SELECT COUNT(DISTINCT house_id) FROM ai_house_chunks")
                result = cur.fetchone()
                return int(result[0] or 0)
    except Exception:
        return len(HOUSE_INDEX)


def count_indexed_knowledge() -> int:
    if not DB_READY:
        return len(KNOWLEDGE_INDEX)
    try:
        with db_connection() as conn:
            with conn.cursor() as cur:
                cur.execute("SELECT COUNT(DISTINCT source_type || ':' || source_id) FROM ai_knowledge_chunks")
                result = cur.fetchone()
                return int(result[0] or 0)
    except Exception:
        return len(KNOWLEDGE_INDEX)


def search_vector_houses(query: str, city: str | None, max_rent: int | None) -> list[dict[str, Any]]:
    if not DB_READY or not query.strip():
        return []
    vector = create_embedding(query)
    try:
        with db_connection() as conn:
            with conn.cursor() as cur:
                sql = f"""
                    SELECT DISTINCT ON (house_id)
                        house_id,
                        title,
                        city,
                        district,
                        rent_amount,
                        area,
                        payload,
                        1 - (embedding <=> %s::vector) AS similarity
                    FROM ai_house_chunks
                    WHERE (%s IS NULL OR city = %s)
                      AND (%s IS NULL OR rent_amount IS NULL OR rent_amount <= %s)
                    ORDER BY house_id, embedding <=> %s::vector
                    LIMIT 12
                """
                cur.execute(
                    sql,
                    (
                        vector_literal(vector),
                        city,
                        city,
                        max_rent,
                        max_rent,
                        vector_literal(vector),
                    ),
                )
                rows = cur.fetchall()
    except Exception:
        return []

    results: list[dict[str, Any]] = []
    for row in rows:
        payload = row[6] or {}
        house = payload if isinstance(payload, dict) else {}
        house = {
            **house,
            "houseId": row[0],
            "title": row[1] or house.get("title"),
            "city": row[2] or house.get("city"),
            "district": row[3] or house.get("district"),
            "rentAmount": row[4] if row[4] is not None else house.get("rentAmount"),
            "area": row[5] if row[5] is not None else house.get("area"),
            "_vectorScore": float(row[7] or 0),
        }
        results.append(house)
    return results


def search_vector_knowledge(query: str, role: str | None = None) -> list[dict[str, Any]]:
    if not DB_READY or not query.strip():
        return []
    vector = create_embedding(query)
    try:
        with db_connection() as conn:
            with conn.cursor() as cur:
                cur.execute(
                    f"""
                    SELECT DISTINCT ON (source_type, source_id)
                        source_type,
                        source_id,
                        title,
                        roles,
                        payload,
                        content,
                        1 - (embedding <=> %s::vector) AS similarity
                    FROM ai_knowledge_chunks
                    ORDER BY source_type, source_id, embedding <=> %s::vector
                    LIMIT 12
                    """,
                    (vector_literal(vector), vector_literal(vector)),
                )
                rows = cur.fetchall()
    except Exception:
        return []

    results: list[dict[str, Any]] = []
    for row in rows:
        payload = row[4] or {}
        data = payload if isinstance(payload, dict) else {}
        role_text = str(row[3] or "")
        score = float(row[6] or 0)
        if role and (not role_text or role in role_text):
            score += 0.05
        results.append({
            **data,
            "sourceType": row[0],
            "sourceId": row[1],
            "title": row[2] or data.get("title") or data.get("question") or f"{source_type_label(row[0])} {row[1]}",
            "roles": row[3] or data.get("roles"),
            "content": row[5] or data.get("content"),
            "score": round(score, 4),
        })
    results.sort(key=lambda item: item.get("score") or 0, reverse=True)
    return results[:6]
