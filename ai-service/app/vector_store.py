import hashlib
import json
import math
import os
from typing import Any

import httpx
import psycopg

from app import runtime_store as store
from app.business_rules import as_int, source_type_label, tokenize
from app.config import (
    EMBEDDING_API_KEY,
    EMBEDDING_BASE_URL,
    EMBEDDING_DIM,
    EMBEDDING_MODEL,
    REMOTE_EMBEDDING_DIM,
    VECTOR_DB_HOST,
    VECTOR_DB_NAME,
    VECTOR_DB_PASSWORD,
    VECTOR_DB_PORT,
    VECTOR_DB_USER,
)


DEFAULT_KNOWLEDGE_MIN_SCORE = float(os.getenv("AI_KNOWLEDGE_MIN_SCORE", "0.12"))


def ensure_vector_store() -> None:
    if store.DB_READY:
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
        store.DB_READY = True
    except Exception:
        store.DB_READY = False


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
    if not store.DB_READY:
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
    if not store.DB_READY:
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
    if not store.DB_READY:
        return
    try:
        with db_connection() as conn:
            with conn.cursor() as cur:
                cur.execute("DELETE FROM ai_house_chunks WHERE house_id = %s", (house_id,))
            conn.commit()
    except Exception:
        return


def delete_knowledge_vectors(source_type: str, source_id: str) -> None:
    if not store.DB_READY:
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
    if not store.DB_READY:
        return len(store.HOUSE_INDEX)
    try:
        with db_connection() as conn:
            with conn.cursor() as cur:
                cur.execute("SELECT COUNT(DISTINCT house_id) FROM ai_house_chunks")
                result = cur.fetchone()
                return int(result[0] or 0)
    except Exception:
        return len(store.HOUSE_INDEX)


def count_indexed_knowledge() -> int:
    if not store.DB_READY:
        return len(store.KNOWLEDGE_INDEX)
    try:
        with db_connection() as conn:
            with conn.cursor() as cur:
                cur.execute("SELECT COUNT(DISTINCT source_type || ':' || source_id) FROM ai_knowledge_chunks")
                result = cur.fetchone()
                return int(result[0] or 0)
    except Exception:
        return len(store.KNOWLEDGE_INDEX)


def search_vector_houses(query: str, city: str | None, max_rent: int | None) -> list[dict[str, Any]]:
    if not store.DB_READY or not query.strip():
        return []
    vector = create_embedding(query)
    city_like = f"%{city}%" if city else None
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
                    WHERE (%s IS NULL OR city ILIKE %s)
                      AND (%s IS NULL OR rent_amount IS NULL OR rent_amount <= %s)
                    ORDER BY house_id, embedding <=> %s::vector
                    LIMIT 12
                """
                cur.execute(
                    sql,
                    (
                        vector_literal(vector),
                        city_like,
                        city_like,
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


def search_vector_knowledge(
    query: str,
    role: str | None = None,
    source_types: list[str] | None = None,
    min_score: float | None = None,
) -> list[dict[str, Any]]:
    if not store.DB_READY or not query.strip():
        return []
    vector = create_embedding(query)
    source_types = [item for item in (source_types or []) if item]
    threshold = DEFAULT_KNOWLEDGE_MIN_SCORE if min_score is None else float(min_score)
    try:
        with db_connection() as conn:
            with conn.cursor() as cur:
                if source_types:
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
                    WHERE source_type = ANY(%s)
                    ORDER BY source_type, source_id, embedding <=> %s::vector
                    LIMIT 18
                    """,
                        (vector_literal(vector), source_types, vector_literal(vector)),
                    )
                else:
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
                    LIMIT 18
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
        if threshold > 0 and score < threshold:
            continue
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
