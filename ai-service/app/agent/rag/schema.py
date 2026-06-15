from typing import Any


MILVUS_COLLECTION = "smart_rental_knowledge"
VECTOR_FIELD = "embedding"


def normalize_hit(hit: dict[str, Any]) -> dict[str, Any]:
    return {
        "sourceType": hit.get("sourceType") or hit.get("source_type") or "knowledge",
        "sourceId": str(hit.get("sourceId") or hit.get("source_id") or ""),
        "title": hit.get("title") or hit.get("question") or "未命名知识",
        "content": hit.get("content") or hit.get("summary") or hit.get("answer") or "",
        "score": float(hit.get("score") or hit.get("distance") or 0),
        "payload": hit.get("payload") or {},
    }


def build_prompt_context(hits: list[dict[str, Any]]) -> str:
    if not hits:
        return "RAG 未命中可靠知识。"
    lines = []
    for item in hits[:5]:
        content = str(item.get("content") or "").replace("\n", " ")[:180]
        lines.append(f"- {item.get('title')}：{content}")
    return "\n".join(lines)
