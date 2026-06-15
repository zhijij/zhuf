import os
from typing import Any

from app.agent.rag.embedding import embed_query
from app.agent.rag.schema import MILVUS_COLLECTION, build_prompt_context, normalize_hit
from app.vector_store import search_vector_knowledge


def milvus_enabled() -> bool:
    return bool(os.getenv("MILVUS_URI") or os.getenv("MILVUS_HOST"))


def search_milvus(
    query: str,
    role: str | None = None,
    limit: int = 6,
    source_types: list[str] | None = None,
) -> list[dict[str, Any]]:
    if not milvus_enabled() or not query.strip():
        return []
    try:
        from pymilvus import MilvusClient

        uri = os.getenv("MILVUS_URI") or f"http://{os.getenv('MILVUS_HOST', 'milvus')}:{os.getenv('MILVUS_PORT', '19530')}"
        client = MilvusClient(uri=uri, token=os.getenv("MILVUS_TOKEN") or None)
        result = client.search(
            collection_name=os.getenv("MILVUS_COLLECTION", MILVUS_COLLECTION),
            data=[embed_query(query)],
            limit=limit,
            output_fields=["sourceType", "sourceId", "title", "content", "payload", "roles"],
        )
        rows = result[0] if result else []
        hits = []
        source_types = [item for item in (source_types or []) if item]
        for row in rows:
            entity = row.get("entity") if isinstance(row, dict) else {}
            if source_types and (entity or {}).get("sourceType") not in source_types:
                continue
            if role:
                roles = str((entity or {}).get("roles") or "")
                if roles and role not in roles:
                    continue
            hits.append(normalize_hit({**(entity or {}), "score": row.get("distance", 0)}))
        return hits
    except Exception:
        return []


def rag_prefetch(
    query: str,
    role: str | None = None,
    source_types: list[str] | None = None,
) -> dict[str, Any]:
    hits = search_milvus(query, role, source_types=source_types)
    source = "milvus"
    if not hits:
        hits = [
            normalize_hit(item)
            for item in search_vector_knowledge(query, role, source_types=source_types)
        ]
        source = "pgvector"
    return {
        "source": source,
        "hits": hits[:6],
        "prompt": build_prompt_context(hits),
        "sourceTypes": source_types or [],
    }
