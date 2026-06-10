from pydantic import BaseModel


class ChatRequest(BaseModel):
    userId: int | None = None
    role: str | None = None
    sessionId: str | None = None
    message: str
    context: dict | None = None


class RecommendRequest(BaseModel):
    userId: int | None = None
    role: str | None = None
    query: str
    city: str | None = None
    maxRent: int | None = None
