from typing import Any

from pydantic import BaseModel, ConfigDict


class ChatHistoryMessage(BaseModel):
    role: str
    content: str


class ChatRequest(BaseModel):
    model_config = ConfigDict(extra="allow")

    userId: int | None = None
    username: str | None = None
    role: str | None = None
    roles: list[str] | None = None
    isAdmin: bool | None = None
    sessionId: str | None = None
    message: str
    history: list[ChatHistoryMessage] | None = None
    context: dict | None = None
    transactionType: str | None = None
    transactionTitle: str | None = None


class RecommendRequest(BaseModel):
    model_config = ConfigDict(extra="allow")

    userId: int | None = None
    username: str | None = None
    role: str | None = None
    roles: list[str] | None = None
    isAdmin: bool | None = None
    query: str
    city: str | None = None
    maxRent: int | None = None
    context: dict | None = None


class HouseIndexRequest(BaseModel):
    model_config = ConfigDict(extra="allow")

    taskId: int | None = None
    houseId: int | None = None
    action: str = "upsert"
    document: dict[str, Any] | None = None


class KnowledgeIndexRequest(BaseModel):
    model_config = ConfigDict(extra="allow")

    taskId: int | None = None
    sourceType: str = "faq"
    sourceId: str | None = None
    action: str = "upsert"
    document: dict[str, Any] | None = None
