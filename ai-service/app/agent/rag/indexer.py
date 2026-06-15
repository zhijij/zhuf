from typing import Any

from app.agent.rag.embedding import embed_query
from app.agent.rag.retriever import milvus_enabled
from app.agent.rag.schema import MILVUS_COLLECTION


def ensure_collection() -> dict[str, Any]:
    if not milvus_enabled():
        return {"ready": False, "message": "Milvus 未配置，跳过集合检查"}
    try:
        from pymilvus import MilvusClient

        client = MilvusClient()
        return {"ready": client.has_collection(MILVUS_COLLECTION), "collection": MILVUS_COLLECTION}
    except Exception as exc:
        return {"ready": False, "message": str(exc)[:160]}


def index_document(document: dict[str, Any]) -> dict[str, Any]:
    if not milvus_enabled():
        return {"indexed": False, "message": "Milvus 未配置"}
    content = str(document.get("content") or document.get("summary") or "")
    return {
        "indexed": False,
        "embeddingPreview": embed_query(content)[:3] if content else [],
        "message": "Milvus 入库由部署环境启用后执行",
    }
