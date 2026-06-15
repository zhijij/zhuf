import os


VECTOR_DB_HOST = os.getenv("VECTOR_DB_HOST", "vector-db")
VECTOR_DB_PORT = int(os.getenv("VECTOR_DB_PORT", "5432"))
VECTOR_DB_NAME = os.getenv("VECTOR_DB_NAME", "smart_rental_ai")
VECTOR_DB_USER = os.getenv("VECTOR_DB_USER", "postgres")
VECTOR_DB_PASSWORD = os.getenv("VECTOR_DB_PASSWORD", "postgres")

EMBEDDING_DIM = int(os.getenv("VECTOR_DB_EMBEDDING_DIM", "64"))
REMOTE_EMBEDDING_DIM = int(os.getenv("AI_EMBEDDING_DIM", str(EMBEDDING_DIM)))
EMBEDDING_BASE_URL = os.getenv("AI_EMBEDDING_BASE_URL", "").rstrip("/")
EMBEDDING_API_KEY = os.getenv("AI_EMBEDDING_API_KEY", "")
EMBEDDING_MODEL = os.getenv("AI_EMBEDDING_MODEL", "")

KNOWLEDGE_SOURCE_TYPES = ["house", "contract", "policy", "faq", "chat", "enterprise"]

ROLE_LABELS = {
    "tenant": "租户",
    "owner": "户主",
    "agent": "中介",
    "contract": "合同协作",
    "admin": "管理员",
    "auditor": "房源审核员",
}

INTENT_LABELS = {
    "house_recommend": "房源推荐",
    "transaction_draft": "事务表单建议",
    "record_summary": "业务摘要",
    "compliance_review": "房源合规审查",
    "contract_risk": "合同风险审阅",
    "listing_copy": "房源文案",
    "followup_message": "沟通跟进",
    "index_advice": "索引与知识库",
    "knowledge_answer": "知识库问答",
    "context_answer": "业务问答",
}
