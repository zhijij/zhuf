from typing import Any


HOUSE_INDEX: dict[int, dict[str, Any]] = {}
KNOWLEDGE_INDEX: dict[str, dict[str, Any]] = {}
SESSION_MEMORY: dict[str, list[dict[str, str]]] = {}
DB_READY = False
