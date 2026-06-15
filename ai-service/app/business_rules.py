import hashlib
import json
import re
from typing import Any


def score_house(house: dict[str, Any], query: str, city: str | None, max_rent: int | None) -> int:
    normalized_query = normalize_house_query(query)
    content = " ".join(
        str(house.get(key) or "")
        for key in ["title", "city", "district", "community", "address", "tags", "description", "content"]
    ).lower()
    score = 0
    for token in tokenize(normalized_query):
        if token and token in content:
            score += 3
    house_city = str(house.get("city") or "")
    if city and (city in house_city or city.removesuffix("市") in house_city):
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


def normalize_house_query(text: str) -> str:
    normalized = text or ""
    for word in ["有没有", "有房源吗", "有房吗", "房源", "推荐", "找房", "可租", "吗", "？", "?"]:
        normalized = normalized.replace(word, "")
    return normalized.strip()


def extract_budget(text: str) -> int | None:
    matches = re.findall(r"(\d{3,6})\s*(?:元|块|以内|以下|预算)?", text or "")
    if not matches:
        return None
    values = [int(item) for item in matches]
    return max(values)


def extract_city(text: str) -> str | None:
    aliases = {
        "北京": "北京",
        "上海": "上海",
        "广州": "广州",
        "深圳": "深圳",
        "杭州": "杭州",
        "南京": "南京",
        "成都": "成都",
        "武汉": "武汉",
        "西安": "西安",
        "天津": "天津",
        "重庆": "重庆",
        "苏州": "苏州",
        "延安": "延安市",
        "延安市": "延安市",
    }
    for alias, city in aliases.items():
        if alias in (text or ""):
            return city
    return None


def build_house_content(document: dict[str, Any]) -> str:
    labels = [
        ("标题", "title"),
        ("城市", "city"),
        ("区域", "district"),
        ("小区", "community"),
        ("地址", "address"),
        ("经度", "longitude"),
        ("纬度", "latitude"),
        ("坐标", "location"),
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
