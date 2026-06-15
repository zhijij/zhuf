import os
import pickle
from collections import defaultdict
from typing import Any


def checkpoint_config(session_id: str) -> dict[str, Any]:
    return {"configurable": {"thread_id": session_id or "anonymous"}}


_STATUS: dict[str, Any] = {
    "backend": "memory",
    "persistent": False,
    "message": "LangGraph MemorySaver",
}


def checkpointer_status() -> dict[str, Any]:
    return dict(_STATUS)


def get_checkpointer():
    try:
        from langgraph.checkpoint.memory import MemorySaver

        backend = os.getenv("LANGGRAPH_CHECKPOINT_BACKEND", "redis").strip().lower()
        if backend in {"redis", "auto"}:
            client = _redis_client()
            if client:
                _STATUS.update({
                    "backend": "redis",
                    "persistent": True,
                    "message": "LangGraph checkpoints persisted in Redis",
                })
                return RedisBackedMemorySaver(client)
            if backend == "redis":
                _STATUS.update({
                    "backend": "memory",
                    "persistent": False,
                    "message": "Redis unavailable, fell back to MemorySaver",
                })

        _STATUS.update({
            "backend": "memory",
            "persistent": False,
            "message": "LangGraph MemorySaver",
        })
        return MemorySaver()
    except Exception as exc:
        _STATUS.update({
            "backend": "disabled",
            "persistent": False,
            "message": f"Checkpointer unavailable: {str(exc)[:120]}",
        })
        return None


try:
    from langgraph.checkpoint.memory import MemorySaver as _MemorySaverBase
except Exception:  # pragma: no cover - handled by get_checkpointer fallback.
    _MemorySaverBase = object


class RedisBackedMemorySaver(_MemorySaverBase):
    """Persist LangGraph MemorySaver state into Redis for restart recovery."""

    def __init__(self, redis_client, key: str | None = None):
        super().__init__()
        self._redis = redis_client
        self._key = key or os.getenv("LANGGRAPH_CHECKPOINT_REDIS_KEY", "langgraph:tenant_agent:checkpoints")
        self._ttl = _int_env("LANGGRAPH_CHECKPOINT_TTL_SECONDS")
        self._restore()

    def put(self, config, checkpoint, metadata, new_versions):
        result = super().put(config, checkpoint, metadata, new_versions)
        self._persist()
        return result

    def put_writes(self, config, writes, task_id, task_path=""):
        super().put_writes(config, writes, task_id, task_path)
        self._persist()

    def delete_thread(self, thread_id: str) -> None:
        super().delete_thread(thread_id)
        self._persist()

    def _restore(self) -> None:
        try:
            raw = self._redis.get(self._key)
            if not raw:
                return
            data = pickle.loads(raw)
            self.storage = _restore_storage(data.get("storage") or {})
            self.writes = defaultdict(dict, data.get("writes") or {})
            self.blobs = defaultdict(None, data.get("blobs") or {})
        except Exception:
            pass

    def _persist(self) -> None:
        try:
            payload = pickle.dumps({
                "storage": _plain_storage(self.storage),
                "writes": dict(self.writes),
                "blobs": dict(self.blobs),
            })
            if self._ttl:
                self._redis.setex(self._key, self._ttl, payload)
            else:
                self._redis.set(self._key, payload)
        except Exception:
            pass


def _redis_client():
    url = os.getenv("REDIS_URL")
    try:
        import redis

        if url:
            client = redis.Redis.from_url(url, decode_responses=False)
        else:
            host = os.getenv("REDIS_HOST")
            if not host:
                return None
            client = redis.Redis(
                host=host,
                port=int(os.getenv("REDIS_PORT", "6379")),
                db=int(os.getenv("REDIS_DB", "0")),
                password=os.getenv("REDIS_PASSWORD") or None,
                decode_responses=False,
            )
        client.ping()
        return client
    except Exception:
        return None


def _plain_storage(storage) -> dict[str, dict[str, dict[str, Any]]]:
    return {
        thread_id: {
            checkpoint_ns: dict(checkpoints)
            for checkpoint_ns, checkpoints in namespaces.items()
        }
        for thread_id, namespaces in storage.items()
    }


def _restore_storage(data: dict[str, dict[str, dict[str, Any]]]):
    storage = defaultdict(lambda: defaultdict(dict))
    for thread_id, namespaces in data.items():
        for checkpoint_ns, checkpoints in namespaces.items():
            storage[thread_id][checkpoint_ns].update(checkpoints)
    return storage


def _int_env(name: str) -> int | None:
    raw = os.getenv(name, "").strip()
    if not raw:
        return None
    try:
        value = int(raw)
        return value if value > 0 else None
    except ValueError:
        return None
