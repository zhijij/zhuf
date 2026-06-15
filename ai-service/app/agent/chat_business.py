import json
import os
from datetime import datetime
from typing import Any

from app.business_rules import knowledge_reference_lines, role_style_hint
from app.config import INTENT_LABELS
from app.langchain_runtime import refine_with_langchain
from app.tooling import call_tool


CHAT_BUSINESS_AGENT_NAMES = [
    "chat_context_agent",
    "business_stage_agent",
    "knowledge_risk_agent",
    "reply_agent",
    "action_agent",
]


SCENE_CONFIGS = {
    "appointment": {
        "label": "预约看房",
        "stage": "看房预约确认",
        "requiredFacts": ["看房时间", "房源状态", "入住时间", "通勤位置"],
        "actions": ["确认看房时间", "核实房源是否仍可租", "提醒看房重点", "约定到场方式"],
        "replyGoal": "确认可看房时间、到场人数和房源是否仍可租。",
        "replyTemplate": "您好，关于“{title}”的看房安排我这边继续跟进。方便确认一下可看房时间和到场人数吗？我同步核实房源状态后给您确认。",
    },
    "intention": {
        "label": "租赁意向",
        "stage": "意向跟进",
        "requiredFacts": ["预算", "入住时间", "付款押金", "客户意向"],
        "actions": ["确认意向等级", "补齐预算和入住时间", "判断是否推进签约", "记录下次沟通时间"],
        "replyGoal": "补齐预算、入住时间、付款周期和是否推进签约。",
        "replyTemplate": "您好，我已记录您对“{title}”的意向。为了判断是否推进签约，想再确认{missing}，确认后我会同步相关方并安排下一步。",
    },
    "contract": {
        "label": "合同协同",
        "stage": "合同确认",
        "requiredFacts": ["合同条款", "付款押金", "交付清单", "维修责任"],
        "actions": ["核对租金押金", "确认交付清单", "记录合同修改意见", "提交确认或拒绝意见"],
        "replyGoal": "围绕租金、押金、租期、交付清单和维修责任确认合同。",
        "replyTemplate": "您好，合同沟通我这边会重点核对租期、租金、押金、交付清单和维修责任。请您确认是否还有需要补充或调整的条款。",
    },
    "entrust": {
        "label": "委托跟进",
        "stage": "委托协同",
        "requiredFacts": ["委托范围", "客户反馈", "带看节奏", "合同条款"],
        "actions": ["同步客户反馈", "确认委托范围", "约定后续带看节奏", "记录成交风险"],
        "replyGoal": "同步客户反馈、确认委托范围和下一步带看节奏。",
        "replyTemplate": "您好，我这边已关注“{title}”的沟通进展。请同步客户意向、带看反馈和可能影响成交的风险点，便于判断下一步。",
    },
    "default": {
        "label": "业务沟通",
        "stage": "沟通推进",
        "requiredFacts": ["核心诉求", "下一步时间", "责任人"],
        "actions": ["确认对方核心诉求", "补齐缺失信息", "约定下一步时间", "记录沟通结论"],
        "replyGoal": "确认对方诉求、缺失信息和下一步时间。",
        "replyTemplate": "您好，我已整理“{title}”的沟通信息。方便补充{missing}吗？确认后我会继续推进业务。",
    },
}


FACT_KEYWORDS = {
    "预算": ["预算", "租金", "价格", "月租"],
    "入住时间": ["入住", "搬", "起租"],
    "看房时间": ["看房", "约看", "预约", "时间"],
    "房源状态": ["还在", "可租", "状态", "下架"],
    "通勤位置": ["通勤", "地铁", "公交", "上班", "公司"],
    "付款押金": ["押金", "付款", "月付", "季付"],
    "合同条款": ["合同", "条款", "违约", "租期"],
    "交付清单": ["交付", "钥匙", "清单", "水电"],
    "维修责任": ["维修", "责任", "损坏"],
    "家具家电": ["家具", "家电", "空调", "冰箱", "洗衣机"],
    "客户意向": ["意向", "满意", "考虑", "成交", "签"],
    "委托范围": ["委托", "范围", "中介", "代理"],
    "客户反馈": ["反馈", "客户", "租户"],
    "带看节奏": ["带看", "复看", "约看"],
    "核心诉求": ["想要", "希望", "需要", "关注"],
    "下一步时间": ["明天", "今天", "周", "下次", "时间"],
    "责任人": ["我来", "你来", "谁", "负责"],
}


def chat_business_agent_mode() -> dict[str, Any]:
    return {
        "role": "business_chat",
        "mode": "chat-business-multi-agent",
        "agents": CHAT_BUSINESS_AGENT_NAMES,
        "specialists": [
            "上下文智能体",
            "业务阶段智能体",
            "知识风险智能体",
            "回复智能体",
            "动作智能体",
        ],
    }


def run_chat_business_agent(state: dict[str, Any]) -> dict[str, Any]:
    checkpoints = ["chat_business_agent"]
    context_result = chat_context_agent(state)
    checkpoints.append("chat_context_agent")
    stage_result = business_stage_agent(state, context_result)
    checkpoints.append("business_stage_agent")
    knowledge_result = knowledge_risk_agent(state, context_result, stage_result)
    checkpoints.append("knowledge_risk_agent")
    reply_result = reply_agent(state, context_result, stage_result, knowledge_result)
    checkpoints.append("reply_agent")
    action_result = action_agent(state, context_result, stage_result, knowledge_result)
    checkpoints.append("action_agent")

    experts = [
        expert_result("chat_context_agent", "上下文智能体", context_result),
        expert_result("business_stage_agent", "业务阶段智能体", stage_result),
        expert_result("knowledge_risk_agent", "知识风险智能体", knowledge_result),
        expert_result("reply_agent", "回复智能体", reply_result),
        expert_result("action_agent", "动作智能体", action_result),
    ]
    tool_calls = [agent_tool_call(expert) for expert in experts]
    tool_calls.extend(knowledge_result.get("toolCalls") or [])

    mode = context_result.get("assistMode") or "reply"
    answer = choose_answer(mode, context_result, stage_result, reply_result, action_result, knowledge_result)
    return {
        "answer": answer,
        "intent": "chat_assist",
        "intentLabel": INTENT_LABELS.get("chat_assist", "会话协同"),
        "agentPlan": {
            "intent": "chat_assist",
            "mode": "chat-business-multi-agent",
            "agents": CHAT_BUSINESS_AGENT_NAMES,
            "reason": "business-chat-dedicated-agent",
        },
        "houseIds": [],
        "toolCalls": tool_calls,
        "collaboration": {
            "mode": "chat-business-multi-agent",
            "router": {
                "intent": "chat_assist",
                "assistMode": mode,
                "bizType": context_result.get("bizType"),
            },
            "coordinator": {
                "strategy": "sequential specialist synthesis",
                "agents": CHAT_BUSINESS_AGENT_NAMES,
            },
            "experts": experts,
            "synthesis": {
                "strategy": "reply/action/summary selected by assistMode",
                "answerSource": reply_result.get("source") or "structured",
            },
        },
        "suggestions": {"message": reply_result.get("message")} if reply_result.get("message") else None,
        "nextActions": action_result.get("actions") or [],
        "checkpoints": checkpoints,
    }


def chat_context_agent(state: dict[str, Any]) -> dict[str, Any]:
    context = state.get("context") or {}
    chat = context.get("chat") or {}
    selected = state.get("selected") or {}
    messages = normalize_messages(chat.get("messages") or [])
    text = "\n".join(item["content"] for item in messages)
    biz_type = str(chat.get("bizType") or selected.get("bizType") or "default")
    config = scene_config(biz_type)
    confirmed = [
        label
        for label, keywords in FACT_KEYWORDS.items()
        if any(keyword in text for keyword in keywords)
    ]
    missing = [item for item in config["requiredFacts"] if item not in confirmed]
    last_message = chat.get("lastMessage") or (messages[-1]["content"] if messages else "")
    summary = [
        f"会话：{chat.get('title') or selected.get('title') or '当前业务会话'}",
        f"业务：{config['label']}",
        f"消息数：{len(messages)}",
    ]
    if last_message:
        summary.append(f"最后消息：{str(last_message)[:80]}")
    if confirmed:
        summary.append("已提到：" + "、".join(confirmed[:6]))
    if missing:
        summary.append("待确认：" + "、".join(missing[:6]))
    return {
        "summary": "；".join(summary),
        "assistMode": chat.get("assistMode") or "reply",
        "title": chat.get("title") or selected.get("title") or "当前业务会话",
        "bizType": biz_type,
        "bizLabel": config["label"],
        "bizId": chat.get("bizId") or selected.get("bizId"),
        "messageCount": len(messages),
        "lastMessage": last_message,
        "confirmedFacts": confirmed,
        "missingFacts": missing,
        "recentMessages": messages,
    }


def business_stage_agent(
    state: dict[str, Any],
    chat_context: dict[str, Any],
) -> dict[str, Any]:
    config = scene_config(str(chat_context.get("bizType") or "default"))
    missing = chat_context.get("missingFacts") or []
    stage = config["stage"]
    priority = "先补齐关键沟通信息" if missing else "可以进入下一步业务动作"
    summary = f"{stage}：{priority}。"
    if missing:
        summary += " 建议优先确认：" + "、".join(missing[:4])
    return {
        "summary": summary,
        "stage": stage,
        "sceneLabel": config["label"],
        "replyGoal": config["replyGoal"],
        "priority": priority,
        "requiredFacts": config["requiredFacts"],
    }


def knowledge_risk_agent(
    state: dict[str, Any],
    chat_context: dict[str, Any],
    stage: dict[str, Any],
) -> dict[str, Any]:
    tool_state = {
        **state,
        "intent": "chat_assist",
        "message": build_knowledge_query(state, chat_context, stage),
    }
    knowledge_call = call_tool("search_knowledge_base", tool_state, sourceTypes=["enterprise", "chat", "faq"])
    output = knowledge_call.get("output") or {}
    references = knowledge_reference_lines(output)
    risks = infer_chat_risks(chat_context, stage)
    lines = []
    if references:
        lines.extend(references)
    if risks:
        lines.append("沟通风险：" + "、".join(risks[:4]))
    return {
        "summary": "；".join(lines) if lines else "当前没有额外知识库依据，按业务会话上下文推进。",
        "references": references,
        "risks": risks,
        "knowledge": output,
        "toolCalls": [knowledge_call],
    }


def reply_agent(
    state: dict[str, Any],
    chat_context: dict[str, Any],
    stage: dict[str, Any],
    knowledge: dict[str, Any],
) -> dict[str, Any]:
    draft = default_reply(chat_context, stage)
    llm_message = refine_reply_with_llm(state, chat_context, stage, knowledge, draft)
    message = llm_message or draft
    return {
        "summary": message,
        "message": message,
        "source": "llm" if llm_message else "structured-draft",
        "draft": draft,
    }


def action_agent(
    state: dict[str, Any],
    chat_context: dict[str, Any],
    stage: dict[str, Any],
    knowledge: dict[str, Any],
) -> dict[str, Any]:
    config = scene_config(str(chat_context.get("bizType") or "default"))
    missing = chat_context.get("missingFacts") or []
    actions = []
    if missing:
        actions.append("补充确认：" + "、".join(missing[:3]))
    actions.extend(config["actions"])
    risks = knowledge.get("risks") or []
    if risks:
        actions.append("记录风险：" + risks[0])
    unique_actions = list(dict.fromkeys(actions))[:5]
    return {
        "summary": "下一步建议：" + "、".join(unique_actions),
        "actions": unique_actions,
    }


def choose_answer(
    mode: str,
    chat_context: dict[str, Any],
    stage: dict[str, Any],
    reply: dict[str, Any],
    action: dict[str, Any],
    knowledge: dict[str, Any],
) -> str:
    if mode == "summary":
        parts = [chat_context.get("summary") or "", stage.get("summary") or ""]
        if knowledge.get("summary"):
            parts.append(knowledge["summary"])
        return "\n".join(item for item in parts if item)
    if mode == "next":
        lines = ["建议下一步这样推进："]
        lines.extend(f"{index}. {item}" for index, item in enumerate(action.get("actions") or [], start=1))
        if reply.get("message"):
            lines.append("可发送话术：" + reply["message"])
        return "\n".join(lines)
    return reply.get("message") or default_reply(chat_context, stage)


def normalize_messages(messages: list[Any]) -> list[dict[str, Any]]:
    normalized = []
    for item in messages[-12:]:
        if not isinstance(item, dict):
            continue
        content = str(item.get("content") or "").strip()
        if not content:
            continue
        normalized.append({
            "mine": bool(item.get("mine")),
            "messageType": item.get("messageType") or "text",
            "content": content,
            "createTime": item.get("createTime"),
            "senderId": item.get("senderId"),
        })
    return normalized


def scene_config(biz_type: str) -> dict[str, Any]:
    return SCENE_CONFIGS.get(biz_type, SCENE_CONFIGS["default"])


def default_reply(chat_context: dict[str, Any], stage: dict[str, Any]) -> str:
    config = scene_config(str(chat_context.get("bizType") or "default"))
    missing = "、".join((chat_context.get("missingFacts") or [])[:3]) or "下一步安排"
    return config["replyTemplate"].format(
        title=chat_context.get("title") or "当前业务",
        missing=missing,
    )


def refine_reply_with_llm(
    state: dict[str, Any],
    chat_context: dict[str, Any],
    stage: dict[str, Any],
    knowledge: dict[str, Any],
    draft: str,
) -> str | None:
    base_url = os.getenv("AI_LLM_BASE_URL", "").rstrip("/")
    api_key = os.getenv("AI_LLM_API_KEY", "")
    model = os.getenv("AI_LLM_MODEL", "")
    if not base_url or not api_key or not model:
        return None

    messages = [
        (
            "system",
            "你是智能AI房屋租赁系统里的沟通回复智能体。"
            "只基于给定会话、业务阶段和知识依据生成中文回复。"
            "回复要能直接复制给对方，不编造房源事实，不承诺未经确认的信息。"
            f"当前角色风格：{role_style_hint(state.get('role'))}",
        ),
        (
            "user",
            json.dumps(
                {
                    "userMessage": state.get("message"),
                    "chatContext": chat_context,
                    "businessStage": stage,
                    "knowledgeRisk": {
                        "references": knowledge.get("references") or [],
                        "risks": knowledge.get("risks") or [],
                    },
                    "draft": draft,
                },
                ensure_ascii=False,
            ),
        ),
    ]
    try:
        return refine_with_langchain(
            base_url=base_url,
            api_key=api_key,
            model=model,
            messages=messages,
            timeout=8,
        )
    except Exception:
        return None


def build_knowledge_query(
    state: dict[str, Any],
    chat_context: dict[str, Any],
    stage: dict[str, Any],
) -> str:
    return "；".join(
        item
        for item in [
            str(state.get("message") or ""),
            str(chat_context.get("bizLabel") or ""),
            str(stage.get("stage") or ""),
            " ".join(chat_context.get("missingFacts") or []),
            str(chat_context.get("lastMessage") or ""),
        ]
        if item
    )


def infer_chat_risks(
    chat_context: dict[str, Any],
    stage: dict[str, Any],
) -> list[str]:
    risks = []
    missing = chat_context.get("missingFacts") or []
    if missing:
        risks.append("关键字段未确认：" + "、".join(missing[:3]))
    last_message = str(chat_context.get("lastMessage") or "")
    risk_keywords = {
        "押金": "涉及押金时需要确认退还条件和扣除标准",
        "合同": "涉及合同时需要核对租期、金额、维修责任和违约责任",
        "转账": "涉及转账时避免脱离平台私下付款",
        "马上": "对方要求快速推进时仍需保留确认记录",
    }
    risks.extend(message for keyword, message in risk_keywords.items() if keyword in last_message)
    return list(dict.fromkeys(risks))[:5]


def expert_result(name: str, label: str, output: dict[str, Any]) -> dict[str, Any]:
    return {
        "name": name,
        "label": label,
        "status": "success",
        "summary": output.get("summary") or label,
        "output": output,
    }


def agent_tool_call(expert: dict[str, Any]) -> dict[str, Any]:
    return {
        "name": expert.get("name"),
        "label": expert.get("label"),
        "args": {},
        "status": expert.get("status") or "success",
        "resultSummary": expert.get("summary"),
        "output": expert.get("output") or {},
        "startedAt": datetime.now().isoformat(timespec="seconds"),
    }
