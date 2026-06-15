# Docker 部署说明

本项目已容器化，包含：

- `RuoYi-Vue`：Spring Boot 后端，端口 `8080`
- `RuoYi-Vue3`：Vue 3 前端，由 Nginx 提供静态服务，端口 `80`
- `ai-service`：FastAPI AI 服务，端口 `8000`
- MySQL 8.4：宿主机端口 `3307`
- Redis 7：宿主机端口 `6379`

## 快速启动

```powershell
cd C:\Users\zj\testRepository
Copy-Item .env.example .env
docker compose up -d --build
```

后续如果只是启动已有容器：

```powershell
docker compose up -d
```

查看状态：

```powershell
docker compose ps
```

停止容器：

```powershell
docker compose down
```

## 访问地址

- 前端：http://localhost/
- 后端：http://localhost:8080/
- AI 健康检查：http://localhost:8000/health

## AI 智能体逻辑

当前 AI 服务已经按 `Tool + Skill + LangChain` 的方式组织，核心流程是：

1. 前端把当前角色、页面模式、选中的业务记录、可执行动作、门户摘要一起发给后端。
2. 后端 `/rental/ai/chat` 补充登录用户、角色和权限后转发给 `ai-service`。
3. `ai-service` 识别意图，例如房源推荐、事务表单建议、业务摘要、合同风险、沟通话术、索引检查。
4. `Skill` 层选择业务规则说明书，例如审核员规则、租户推荐规则、中介跟进规则、合同风险规则。
5. `Tool` 层执行真实业务工具，例如房源索引检索、当前业务摘要、事务表单生成、合同风险审阅、房源文案生成。
6. 如配置了 OpenAI-compatible 大模型，LangChain `ChatOpenAI` 会基于 Skill 和 Tool 结果进行中文润色；未配置时直接返回本地规则答案。
7. 前端展示 AI 答案、意图标签、使用的 Skill、工具执行轨迹；事务弹窗会优先读取结构化 `suggestions` 并自动写入表单。

当前已经沉淀的 Skill 文件位于 `ai-service/app/skills`：

- `auditor-rules.md`：审核员房源合规审核。
- `tenant-recommendation.md`：租户找房推荐。
- `agent-followup.md`：中介跟进和沟通话术。
- `contract-risk.md`：合同风险审阅。

能力清单接口：

```powershell
(Invoke-WebRequest http://localhost:8000/api/v1/agent/capabilities -UseBasicParsing).Content
```

返回中重点看：

- `langchain.available`
- `langchainToolCount`
- `skills`
- `tools`

未配置大模型时，AI 服务会使用本地规则工具链，推荐、摘要、事务建议仍可用。需要接入 OpenAI-compatible 大模型时，在 `.env` 中配置：

```text
AI_LLM_BASE_URL=https://api.openai.com/v1
AI_LLM_API_KEY=sk-...
AI_LLM_MODEL=gpt-4.1-mini
```

如果使用其他兼容服务，把 `AI_LLM_BASE_URL`、`AI_LLM_API_KEY`、`AI_LLM_MODEL` 换成对应供应商配置即可。当前房源索引会优先写入 `PostgreSQL + pgvector`，只有向量库不可用时才临时回退到内存索引。

### 接入千问

当前 `ai-service` 已按 OpenAI-compatible 协议实现，无需改业务代码，直接配置 `.env` 即可。

阿里云百炼官方兼容说明：

- Chat 兼容文档：[如何通过 OpenAI 接口调用千问模型](https://help.aliyun.com/zh/model-studio/compatibility-of-openai-with-dashscope)
- 模型列表：[选择模型](https://help.aliyun.com/zh/model-studio/models)
- 向量模型列表：[向量与重排序](https://help.aliyun.com/zh/model-studio/embedding-rerank-model/)

如果你使用北京地域，推荐先这样配：

```text
AI_LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
AI_LLM_API_KEY=你的百炼API Key
AI_LLM_MODEL=qwen-plus
```

配置完成后重启 AI 服务：

```powershell
docker compose up -d --build ai-service
```

这个配置会让 AI 对话、摘要、建议、规则结果润色都走千问，但知识库检索如果没配 embedding，仍然会走本地哈希向量。

## pgvector 知识库

当前项目已经升级为 `PostgreSQL + pgvector` 路线：

- 新增 `vector-db` 容器，独立于业务 MySQL，仅用于 AI 检索。
- `ai-service` 会把房源文档切片后写入 `ai_house_chunks` 表，并保存向量。
- 查询时优先走 pgvector 相似度检索，再叠加城市、预算和关键词规则排序。
- 如果 `vector-db` 暂时不可用，AI 服务会回退到内存索引，不影响基础业务联调。

默认配置在 `.env.example` 中：

```text
VECTOR_DB_NAME=smart_rental_ai
VECTOR_DB_USER=postgres
VECTOR_DB_PASSWORD=postgres
VECTOR_DB_PORT=5433
VECTOR_DB_EMBEDDING_DIM=64
```

### Embedding 配置

如果不配置 embedding 服务，AI 服务会使用本地哈希向量，适合原型联调和毕业设计演示。  
如果要接入真正语义检索，在 `.env` 中补上 OpenAI-compatible embedding 配置：

```text
AI_EMBEDDING_BASE_URL=https://api.openai.com/v1
AI_EMBEDDING_API_KEY=sk-...
AI_EMBEDDING_MODEL=text-embedding-3-small
```

只配置大模型聊天、不配置 embedding，也可以正常聊天，但知识库召回仍会走本地哈希向量。

如果接入千问文本向量，推荐配置为：

```text
AI_EMBEDDING_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
AI_EMBEDDING_API_KEY=你的百炼API Key
AI_EMBEDDING_MODEL=text-embedding-v4
AI_EMBEDDING_DIM=1024
VECTOR_DB_EMBEDDING_DIM=1024
```

说明：

- 阿里云百炼当前推荐纯文本 RAG 使用 `text-embedding-v4`。
- `text-embedding-v4` 支持多种维度，通用场景推荐 `1024` 维。
- `VECTOR_DB_EMBEDDING_DIM` 必须和 `AI_EMBEDDING_DIM` 一致，否则会影响向量写入和检索效果。
- 如果你之前已经用 `64` 维本地哈希向量写过数据，切换到 `1024` 维后建议重建向量库。

重建方式：

```powershell
docker compose down
docker volume rm testrepository_vector-db-data
docker compose up -d --build vector-db ai-service
```

随后重新触发房源索引写入，再验证：

```powershell
(Invoke-WebRequest http://localhost:8000/health -UseBasicParsing).Content
```

正常情况下应看到：

- `vectorDbReady: true`
- `embeddingMode: "remote"`

## 开发账号

开发环境登录页会显示开发账号快捷按钮；生产环境默认关闭。也可以手动输入：

| 角色 | 账号 | 密码 |
| --- | --- | --- |
| 超级后台 | `admin` | `admin123` |
| 审核员 | `auditor_test` | `admin123` |
| 租户 | `tenant_test` | `admin123` |
| 户主 | `owner_test` | `admin123` |
| 中介 | `agent_test` | `admin123` |

生产上线前必须修改或禁用这些开发账号，并保持 `RuoYi-Vue3/.env.production` 中：

```text
VITE_DEV_LOGIN = 'false'
```

## 在另一台电脑上使用

另一台电脑只需要安装并启动：

- Git
- Docker Desktop

不需要单独安装 Java、Maven、Node.js、Python、MySQL 或 Redis。

### 1. 克隆项目

```powershell
git clone https://github.com/ha123g/testRepository.git
cd testRepository
```

### 2. 准备环境变量

```powershell
Copy-Item .env.example .env
```

如需修改数据库或 Redis 密码，编辑 `.env` 文件即可。

### 3. 启动项目

```powershell
docker compose up -d --build
```

首次启动会下载基础镜像并构建后端、前端和 AI 服务，耗时会比较久。构建完成后再启动通常只需要：

```powershell
docker compose up -d
```

## Docker 代理设置

如果拉取镜像时出现 `TLS handshake timeout`、`unexpected EOF`、`failed to resolve reference` 等错误，通常是 Docker Desktop 没有正确走代理。

在 Docker Desktop 中配置：

1. 打开 Docker Desktop。
2. 进入 `Settings` -> `Resources` -> `Proxies`。
3. 选择 `Manual configuration`。
4. 按你的代理端口填写，例如：

```text
HTTP Proxy:  http://127.0.0.1:7890
HTTPS Proxy: http://127.0.0.1:7890
Bypass:      localhost,127.0.0.1,::1
```

5. `Containers proxy` 建议选择 `Same as host proxy`。
6. 点击 `Apply & Restart`。

如果使用 Clash、V2RayN 等代理软件，请确认已经开启：

- 系统代理
- 允许局域网
- HTTP 或 Mixed 代理端口

## 端口说明

| 服务 | 宿主机端口 | 容器端口 |
| --- | ---: | ---: |
| 前端 | `80` | `80` |
| 后端 | `8080` | `8080` |
| AI 服务 | `8000` | `8000` |
| MySQL | `3307` | `3306` |
| Redis | `6379` | `6379` |

MySQL 映射到宿主机 `3307`，是为了避免和本机已有 MySQL 的 `3306` 冲突。后端容器内部仍然通过 `mysql:3306` 访问数据库。

如果使用 Navicat、DataGrip 等工具连接 Docker MySQL：

```text
Host: localhost
Port: 3307
User: ruoyi
Password: feng123
Database: ry-vue
```

## 数据库初始化

MySQL 首次启动时会按顺序执行：

1. `RuoYi-Vue/sql/ry_20260417.sql`
2. `RuoYi-Vue/sql/quartz.sql`
3. `RuoYi-Vue/sql/smart_rental_schema.sql`
4. `RuoYi-Vue/sql/smart_rental_business_chat.sql`

这些初始化脚本只会在 `mysql-data` 数据卷为空时自动执行。

如果需要完全重置数据库：

```powershell
docker compose down -v
docker compose up -d --build
```

注意：`docker compose down -v` 会删除数据库数据卷，执行前请确认不需要保留当前数据。

## 生产上线步骤

### 1. 准备生产配置

复制 `.env.example` 为 `.env` 后，必须替换默认密码和外部服务密钥：

- `MYSQL_ROOT_PASSWORD`、`MYSQL_PASSWORD`、`REDIS_PASSWORD`、`VECTOR_DB_PASSWORD` 使用生产强密码。
- `AI_LLM_BASE_URL`、`AI_LLM_API_KEY`、`AI_LLM_MODEL` 配置千问/百炼或其他 OpenAI-compatible 模型。
- 如需真实语义 RAG，配置 `AI_EMBEDDING_BASE_URL`、`AI_EMBEDDING_API_KEY`、`AI_EMBEDDING_MODEL`、`AI_EMBEDDING_DIM`。
- 如果 embedding 使用 `1024` 维，`VECTOR_DB_EMBEDDING_DIM` 必须同步设为 `1024`，并在首次建库前完成配置。
- 文件存储密钥只放在生产环境变量或服务器密钥管理中，不提交到 Git。

### 2. 构建和启动

```powershell
docker compose build
docker compose up -d
docker compose ps
```

### 3. 健康检查

```powershell
Invoke-WebRequest -UseBasicParsing -Uri http://localhost/ -TimeoutSec 10
Invoke-WebRequest -UseBasicParsing -Uri http://localhost:8080/captchaImage -TimeoutSec 10
Invoke-WebRequest -UseBasicParsing -Uri http://localhost:8000/health -TimeoutSec 10
```

AI 健康检查中如果看到 `embeddingMode: "local-hash"`，表示还没有接入远程 embedding；能用于联调，但生产语义检索建议配置为远程 embedding。

### 4. 首次数据确认

- 确认普通审核员账号存在，并且角色是 `auditor`。
- 确认超级管理员只进入后台管理，不参与房源审核业务。
- 使用户主账号创建房源并提交审核，使用审核员账号通过或驳回，再验证租户和中介侧可见数据。
- 如果旧 MySQL 数据卷已经存在，新增 SQL 初始化脚本不会自动重复执行，需要手动补齐新增账号和字段。
- 切换真实 embedding 维度后，需要重建 pgvector 数据卷并重新触发房源索引。

### 5. 对外发布

- 配置域名、HTTPS 证书和反向代理。
- 外网只暴露前端入口和必要 API，MySQL、Redis、pgvector 不直接暴露公网。
- 修改或禁用 `admin123` 开发账号，生产保持 `VITE_DEV_LOGIN = 'false'`。
- 建立 MySQL、pgvector、上传文件卷的定期备份和恢复演练。
- 对日志、磁盘、容器健康状态设置监控告警。

## 上线前需要准备

- 域名和 HTTPS 证书。
- 生产 MySQL、Redis、pgvector 的强密码。
- 千问/百炼 API Key：用于 `AI_LLM_API_KEY`，如需语义检索还要配置 `AI_EMBEDDING_API_KEY`。
- 文件存储配置：如果使用腾讯云 COS，需要准备新的 SecretId、SecretKey、Region、Bucket，并不要提交到 Git。
- 开发账号处理策略：禁用、改密或删除 `admin123` 开发账号。
- 生产端口规划：前端反向代理到后端 `/prod-api`，后端只暴露必要端口。
- 数据备份策略：MySQL 业务库、pgvector 向量库、上传文件目录都要备份。

## 容器网络

后端在容器内部通过 Docker 服务名访问依赖：

- MySQL：`mysql:3306`
- Redis：`redis:6379`
- AI 服务：`http://ai-service:8000`

前端请求 `/prod-api`，由 Nginx 代理到后端容器。
