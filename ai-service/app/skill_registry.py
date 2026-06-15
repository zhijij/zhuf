from dataclasses import dataclass
from pathlib import Path
from typing import Any


SKILL_DIR = Path(__file__).resolve().parent / "skills"


@dataclass(frozen=True)
class AgentSkill:
    key: str
    name: str
    description: str
    path: str
    prompt: str


INTENT_SKILL_MAP = {
    "compliance_review": "auditor_rules",
    "house_recommend": "tenant_recommendation",
    "followup_message": "agent_followup",
    "contract_risk": "contract_risk",
    "listing_copy": "owner_listing",
    "knowledge_answer": "policy_faq",
    "index_advice": "enterprise_knowledge",
    "record_summary": "chat_summary",
}


SKILL_FILES = {
    "auditor_rules": "auditor-rules.md",
    "tenant_recommendation": "tenant-recommendation.md",
    "agent_followup": "agent-followup.md",
    "contract_risk": "contract-risk.md",
    "owner_listing": "owner-listing.md",
    "policy_faq": "policy-faq.md",
    "enterprise_knowledge": "enterprise-knowledge.md",
    "chat_summary": "chat-summary.md",
}


def list_skills() -> list[AgentSkill]:
    return [load_skill(key) for key in SKILL_FILES if load_skill(key) is not None]


def load_skill(key: str) -> AgentSkill | None:
    filename = SKILL_FILES.get(key)
    if not filename:
        return None

    path = SKILL_DIR / filename
    if not path.exists():
        return None

    content = path.read_text(encoding="utf-8")
    meta, prompt = parse_skill(content)
    return AgentSkill(
        key=key,
        name=meta.get("name") or key,
        description=meta.get("description") or "",
        path=str(path),
        prompt=prompt.strip(),
    )


def select_skill(intent: str, state: dict[str, Any]) -> AgentSkill | None:
    skill_key = INTENT_SKILL_MAP.get(intent)
    role = state.get("role")
    if intent == "context_answer":
        role_map = {
            "tenant": "tenant_recommendation",
            "owner": "owner_listing",
            "agent": "agent_followup",
            "auditor": "auditor_rules",
            "admin": "auditor_rules",
            "contract": "contract_risk",
        }
        skill_key = role_map.get(role)
    if not skill_key and state.get("role") == "auditor":
        skill_key = "auditor_rules"
    return load_skill(skill_key) if skill_key else None


def skill_to_dict(skill: AgentSkill | None) -> dict[str, Any] | None:
    if not skill:
        return None
    return {
        "key": skill.key,
        "name": skill.name,
        "description": skill.description,
    }


def parse_skill(content: str) -> tuple[dict[str, str], str]:
    if not content.startswith("---"):
        return {}, content

    end = content.find("\n---", 3)
    if end == -1:
        return {}, content

    meta_block = content[3:end].strip()
    body = content[end + 4 :].strip()
    meta: dict[str, str] = {}
    for line in meta_block.splitlines():
        if ":" not in line:
            continue
        key, value = line.split(":", 1)
        meta[key.strip()] = value.strip().strip('"')
    return meta, body
