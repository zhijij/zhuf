from typing import Any


def checkpoint_config(session_id: str) -> dict[str, Any]:
    return {"configurable": {"thread_id": session_id or "anonymous"}}


def get_checkpointer():
    try:
        from langgraph.checkpoint.memory import MemorySaver

        return MemorySaver()
    except Exception:
        return None
