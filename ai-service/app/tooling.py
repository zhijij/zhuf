from datetime import datetime
from typing import Any, Callable


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
        "summarize_chat_context": "读取会话上下文",
        "summarize_index_state": "检查索引状态",
        "search_knowledge_base": "检索统一知识库",
        "amap_search_around": "查询坐标周边",
        "amap_house_context": "查询通勤与周边",
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
        "summarize_chat_context": "Summarize recent business chat context and missing follow-up facts.",
        "summarize_index_state": "Inspect vector index and knowledge-base state.",
        "search_knowledge_base": "Search policies, FAQ, contracts, chat records, enterprise rules and indexed houses.",
        "amap_search_around": "Fetch nearby POIs around a coordinate through the Java business boundary.",
        "amap_house_context": "Fetch tenant-visible AMap commute and nearby context for the selected house.",
    }
    return descriptions.get(name, tool_label(name))
