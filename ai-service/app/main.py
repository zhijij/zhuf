from fastapi import FastAPI
from app.schemas import ChatRequest, RecommendRequest

app = FastAPI(title="Rental AI Service", version="0.1.0")


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/api/v1/agent/chat")
def chat(request: ChatRequest):
    return {
        "answer": f"已收到消息：{request.message}",
        "intent": "chat",
        "houseIds": [],
        "toolCalls": [],
        "memoryUpdated": False,
    }


@app.post("/api/v1/agent/recommend")
def recommend(request: RecommendRequest):
    return {
        "answer": f"已收到推荐请求：{request.query}",
        "intent": "house_recommend",
        "houseIds": [],
        "toolCalls": [],
        "memoryUpdated": False,
    }
