from typing import Any

from app.agent.rag.retriever import rag_prefetch


def vector_search(query: str, role: str | None = None) -> dict[str, Any]:
    return rag_prefetch(query, role)
