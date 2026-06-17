import json
from typing import Any, Callable


try:
    from langchain_core.tools import StructuredTool
    from langchain_openai import ChatOpenAI

    LANGCHAIN_IMPORT_ERROR = ""
except Exception as exc:  # pragma: no cover - depends on optional runtime packages.
    StructuredTool = None
    ChatOpenAI = None
    LANGCHAIN_IMPORT_ERROR = str(exc)


def langchain_available() -> bool:
    return StructuredTool is not None and ChatOpenAI is not None


def langchain_status() -> dict[str, Any]:
    return {
        "available": langchain_available(),
        "error": LANGCHAIN_IMPORT_ERROR,
    }


def build_langchain_tools(
    tool_registry: dict[str, Callable[..., dict[str, Any]]],
    label_getter: Callable[[str], str],
    description_getter: Callable[[str], str],
) -> list[Any]:
    if not langchain_available():
        return []

    tools = []
    for name in sorted(tool_registry.keys()):
        tools.append(
            StructuredTool.from_function(
                name=name,
                description=description_getter(name),
                func=_make_tool_adapter(name, tool_registry, label_getter),
            )
        )
    return tools


def tool_specs(
    tool_registry: dict[str, Callable[..., dict[str, Any]]],
    label_getter: Callable[[str], str],
    description_getter: Callable[[str], str],
) -> list[dict[str, str]]:
    return [
        {
            "name": name,
            "label": label_getter(name),
            "description": description_getter(name),
        }
        for name in sorted(tool_registry.keys())
    ]


def refine_with_langchain(
    *,
    base_url: str,
    api_key: str,
    model: str,
    messages: list[tuple[str, str]],
    timeout: int = 8,
) -> str | None:
    if not langchain_available():
        return None

    llm = ChatOpenAI(
        model=model,
        api_key=api_key,
        base_url=base_url,
        temperature=0.2,
        timeout=timeout,
    )
    response = llm.invoke(messages)
    content = getattr(response, "content", None)
    if isinstance(content, str):
        return content.strip() or None
    return str(content).strip() if content else None


def invoke_structured_json(
    *,
    base_url: str,
    api_key: str,
    model: str,
    messages: list[tuple[str, str]],
    timeout: int = 8,
) -> dict[str, Any] | None:
    if not langchain_available():
        raise RuntimeError(f"LangChain 不可用，无法调用模型输出结构化 JSON：{LANGCHAIN_IMPORT_ERROR}")

    llm = ChatOpenAI(
        model=model,
        api_key=api_key,
        base_url=base_url,
        temperature=0,
        timeout=timeout,
    )
    response = llm.invoke(messages)
    content = getattr(response, "content", None)
    if isinstance(content, list):
        content = "".join(
            item.get("text", "") if isinstance(item, dict) else str(item)
            for item in content
        )
    if not content:
        raise RuntimeError("模型没有返回结构化 JSON 内容")
    text = str(content).strip()
    if text.startswith("```"):
        text = text.strip("`")
        if text.startswith("json"):
            text = text[4:].strip()
    try:
        data = json.loads(text)
        if not isinstance(data, dict):
            raise RuntimeError(f"模型结构化 JSON 顶层不是对象：{text[:300]}")
        return data
    except json.JSONDecodeError:
        raise RuntimeError(f"模型返回不是有效 JSON：{text[:300]}")


def _make_tool_adapter(
    name: str,
    tool_registry: dict[str, Callable[..., dict[str, Any]]],
    label_getter: Callable[[str], str],
) -> Callable[[str], str]:
    def invoke(payload: str = "{}") -> str:
        """Invoke a smart rental business tool with JSON payload."""
        data = json.loads(payload or "{}")
        state = data.get("state") or {}
        args = data.get("args") or {}
        result = tool_registry[name](state, **args)
        return json.dumps(
            {
                "name": name,
                "label": label_getter(name),
                "output": result,
            },
            ensure_ascii=False,
        )

    return invoke
