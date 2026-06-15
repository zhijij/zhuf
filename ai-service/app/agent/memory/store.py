import os
from typing import Any

from app import runtime_store


def memory_window_size() -> int:
    return int(os.getenv("MEMORY_WINDOW_SIZE", "12"))


def memory_ttl_seconds() -> int:
    return int(os.getenv("MEMORY_TTL_SECONDS", "86400"))


def _redis_client():
    url = os.getenv("REDIS_URL")
    try:
        import redis

        if url:
            return redis.Redis.from_url(url, decode_responses=True)
        host = os.getenv("REDIS_HOST")
        if not host:
            return None
        return redis.Redis(
            host=host,
            port=int(os.getenv("REDIS_PORT", "6379")),
            db=int(os.getenv("REDIS_DB", "0")),
            password=os.getenv("REDIS_PASSWORD") or None,
            decode_responses=True,
        )
    except Exception:
        return None


def load_memory(session_id: str) -> list[dict[str, str]]:
    key = f"tenant_agent:{session_id}"
    client = _redis_client()
    if client:
        try:
            import json

            raw = client.get(key)
            return json.loads(raw) if raw else []
        except Exception:
            return []
    return runtime_store.SESSION_MEMORY.get(key, [])


def save_memory(session_id: str, user_message: str, answer: str) -> bool:
    key = f"tenant_agent:{session_id}"
    history = load_memory(session_id)
    history.extend([
        {"role": "user", "content": user_message},
        {"role": "assistant", "content": answer},
    ])
    history = history[-memory_window_size():]
    client = _redis_client()
    if client:
        try:
            import json

            client.setex(key, memory_ttl_seconds(), json.dumps(history, ensure_ascii=False))
            return True
        except Exception:
            pass
    runtime_store.SESSION_MEMORY[key] = history
    return True


def compact_memory(history: list[dict[str, Any]]) -> list[str]:
    return [f"{item.get('role')}: {item.get('content')}" for item in history[-6:]]
