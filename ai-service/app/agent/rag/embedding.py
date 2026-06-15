from app.vector_store import create_embedding


def embed_query(text: str) -> list[float]:
    return create_embedding(text or "")
