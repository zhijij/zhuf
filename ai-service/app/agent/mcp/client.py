import json
import os
from typing import Any


async def load_mcp_tools() -> list[Any]:
    raw = os.getenv("MCP_SERVERS_JSON", "").strip()
    if not raw:
        return []
    try:
        from langchain_mcp_adapters.client import MultiServerMCPClient

        config = json.loads(raw)
        client = MultiServerMCPClient(config)
        return await client.get_tools()
    except Exception:
        return []
