# ai-service

FastAPI + LangChain AI service for the rental system.

## 当前架构

```text
前端门户
  -> RuoYi 后端 /rental/ai/chat
  -> ai-service /api/v1/agent/chat
  -> intent 识别
  -> Skill 选择
  -> Tool 编排执行
  -> LangChain ChatOpenAI 可选润色
  -> 返回 answer / skill / toolCalls / suggestions / nextActions
```

未配置 `AI_LLM_API_KEY` 时，服务仍使用本地规则和工具链，业务功能可用。

## 租户多智能体协作

租户智能体采用主管和并行混合模式，接口仍复用 `/api/v1/agent/chat` 与 `/api/v1/agent/recommend`：

```text
router
  -> 基于用户问题、角色上下文和 RAG 命中内容判断意图、槽位和路由
coordinator
  -> 主管智能体生成工具计划和专家协作计划
collaboration
  -> 并行调用房源检索、地图生活、风险分析、知识政策、业务上下文专家
synthesis
  -> 汇总专家结果、工具结果和上下文，统一生成最终回复
```

响应会额外返回 `collaboration` 字段，包含 `mode`、`router`、`coordinator`、`experts` 和 `synthesis`，前端可用于展示主管智能体协作轨迹。

## Skill

Skill 是业务规则说明书，放在 `app/skills`：

- `auditor-rules.md`：审核员合规审查规则
- `tenant-recommendation.md`：租户推荐规则
- `agent-followup.md`：中介跟进规则
- `contract-risk.md`：合同风险规则
- `owner-listing.md`：户主房源经营、新建房源、委托和发布文案规则
- `policy-faq.md`：政策、FAQ、合同模板、聊天记录和企业制度问答规则
- `enterprise-knowledge.md`：知识库、Tool、MCP 编排规则
- `chat-summary.md`：业务聊天摘要和下一步动作规则

新增 Skill：

1. 在 `app/skills` 新建 `xxx.md`。
2. 在 `app/skill_registry.py` 的 `SKILL_FILES` 注册文件。
3. 在 `INTENT_SKILL_MAP` 把 intent 映射到 Skill。

## Tool

Tool 是可执行业务函数，集中在 `app/main.py`：

```python
@tool("review_house_compliance")
def review_house_compliance(state, **kwargs):
    return {
        "summary": "审查完成",
        "suggestions": {"auditStatus": "2"}
    }
```

注册后会自动进入：

- `/health` 的 `tools`
- `/api/v1/agent/capabilities`
- LangChain `StructuredTool` 适配层

当前已落地的核心 Tool：

- `search_public_houses`：检索房源向量索引，用于租户推荐和中介匹配。
- `search_knowledge_base`：检索统一知识库，用于政策、FAQ、合同、聊天记录、企业制度问答。
- `review_house_compliance`：辅助审核员检查房源合规。
- `draft_transaction_form`：给前端模态框生成可写入字段建议。
- `draft_listing_copy`：给户主生成真实克制的房源文案。
- `draft_followup_message`：给中介、户主、租户生成可复制的话术。
- `explain_contract_risk`：提取合同风险点。

## RAG 统一知识库

项目使用 `PostgreSQL + pgvector` 做向量库。知识库不只存房源，也存：

- `house`：房源文档
- `contract`：合同模板、条款、风险清单
- `policy`：平台政策、审核规则、业务流程
- `faq`：常见问题
- `chat`：聊天记录摘要
- `enterprise`：企业制度、SOP、运营规范

写入接口：

```http
POST /api/v1/index/knowledge
```

请求示例：

```json
{
  "sourceType": "policy",
  "action": "upsert",
  "document": {
    "title": "房源审核规则",
    "roles": ["auditor", "owner"],
    "content": "房源发布前必须核对标题、地址、租金、面积、户主信息和图片真实性。"
  }
}
```

前端审核员 AI 工作台已经接入该能力；普通租户、户主、中介只能检索和使用，不允许写入平台级知识。

## MCP

当前提供 MCP 兼容清单：

```http
GET /api/v1/mcp/manifest
```

清单暴露：

- `resources`：Skill、统一知识库来源。
- `tools`：当前可执行业务 Tool。
- `notes`：当前边界和后续拆成独立 MCP Server 的说明。

现阶段 Tool 仍由 `ai-service` 本地执行。后续如果要接 IDE、智能体平台或外部工作流，可以把 `TOOL_REGISTRY` 和 `mcp_manifest` 抽成标准 MCP Server。

## LangChain

`app/langchain_runtime.py` 负责：

- 把本地 Tool 注册表包装成 LangChain `StructuredTool`。
- 使用 `langchain-openai` 的 `ChatOpenAI` 调用 OpenAI-compatible 大模型。
- LangChain 不可用或未配置 Key 时自动回退到本地规则答案。

千问/百炼示例：

```text
AI_LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
AI_LLM_API_KEY=你的百炼API Key
AI_LLM_MODEL=qwen-plus
```

## 能力检查

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:8000/health
Invoke-WebRequest -UseBasicParsing http://localhost:8000/api/v1/agent/capabilities
```

关键字段：

- `langchain.available`：LangChain 是否可用。
- `langchainToolCount`：已包装的 LangChain Tool 数量。
- `skills`：已加载业务 Skill。
- `tools`：已注册业务 Tool。
- `indexedKnowledge`：已入库知识文档数量。
- `knowledgeSources`：统一知识库支持的来源类型。
