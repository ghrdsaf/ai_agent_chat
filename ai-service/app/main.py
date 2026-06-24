from fastapi import FastAPI

app = FastAPI(title="ai-agent-chat-ai-service")


@app.get("/healthz")
def health():
    return {"status": "ok", "service": "ai-service"}


@app.post("/v1/reply/suggest")
def suggest_reply(payload: dict):
    return {
        "replyText": "stub reply",
        "riskLevel": "L2",
        "confidence": 0.5,
        "evidence": [],
        "action": "REVIEW",
        "input": payload,
    }

