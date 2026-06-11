# 房源控制层业务说明

## 目标范围

本文档说明 `RuoYi-Vue/ruoyi-admin/src/main/java/com/ruoyi/web/controller/system/RentalHouseController.java` 及其依赖的房源 Service 生命周期设计。

当前保留 RuoYi 生成的 CRUD 接口，用于后台管理兜底；新增业务接口用于真实业务流程，所有状态判断集中在 `IRentalHouseService` / `RentalHouseServiceImpl`。

## 状态定义

房源业务状态 `status`：

| 值 | 枚举 | 含义 | 公开列表/RAG |
| --- | --- | --- | --- |
| 0 | DRAFT | 草稿 | 不可见 |
| 1 | PENDING_AUDIT | 待审核 | 不可见 |
| 2 | PUBLISHED | 已发布/上架 | 可见 |
| 3 | REJECTED | 审核未通过 | 不可见 |
| 4 | RENTED | 已出租/成交 | 不可见，仅相关方可查 |
| 5 | OFF_SHELF | 已下架 | 不可见，仅相关方可查 |

审核状态 `audit_status`：

| 值 | 枚举 | 含义 |
| --- | --- | --- |
| 0 | NOT_SUBMITTED | 未提交 |
| 1 | PENDING | 待审 |
| 2 | PASSED | 审核通过 |
| 3 | REJECTED | 审核拒绝 |

运营方式 `operation_mode`：

| 值 | 枚举 | 含义 |
| --- | --- | --- |
| 0 | OWNER_SELF | 房东本人出租 |
| 1 | AGENT_ENTRUST | 委托中介代理 |

## 业务接口

### 公开查询

`GET /system/house/public-list`

返回租户可见房源。房源必须满足：

- `status=2`
- `audit_status=2`
- 房东自营，或委托中介且委托关系 `status=1`

用于主页列表，也可作为 RAG 检索的数据口径。

`GET /system/house/detail/{houseId}`

详情可见性：

- 租户可见房源：所有已登录角色可见。
- 已成交/下架/审核中/驳回房源：仅房东、中介、成交合同租户、后台管理员/超级管理员可见。
- 收藏用户可以在收藏列表看到记录，但不能因此查看非法、未审核或已下架详情。

### 用户提交与修改

`POST /system/house/submit`

用户提交房源，默认进入：

- `status=1`
- `audit_status=1`
- `ai_index_status=0`

`PUT /system/house/{houseId}/before-approval`

用户只允许在审核通过前修改，即 `status=0` 或 `status=3`。审核通过后直接修改会被拒绝，后续建议新增“房源变更审核”业务，而不是直接覆盖已发布信息。

`POST /system/house/{houseId}/resubmit`

驳回后用户修改完成，再次提交审核。

### 管理员审核

`POST /system/house/{houseId}/audit`

只允许普通管理员审核，超级管理员会被拒绝。管理员审核只负责房源合法性，类似视频发布审核，不代表委托关系成立。请求体：

```json
{
  "auditStatus": "2",
  "auditReason": "资料真实，允许上架"
}
```

合法性审核通过：

- `status=2`
- `audit_status=2`
- TODO 创建 AI 向量 upsert 任务，让 RAG 可检索。

审核拒绝：

- `status=3`
- `audit_status=3`
- 用户可修改后再次提交。

权限和审计：

- 审核接口使用按钮权限 `system:house:audit`，应只分配给普通管理员角色。
- 超级管理员即使拥有全部权限，业务层仍会拒绝执行审核，只保留监督和查看职责。
- 审核接口使用 `@Log(title = "房源合法性审核")`，成功和失败都会进入 RuoYi 操作日志。
- 权限种子 SQL 已写入 `RuoYi-Vue/sql/smart_rental_schema.sql`，会为房源菜单补充“房源合法性审核”按钮权限。

### 委托中介

`POST /system/house/{houseId}/entrust`

房东向中介发起委托，创建 `rental_house_entrust` 记录。委托是房东与中介的双向选择，不替代管理员合法性审核。

- `operation_mode=1`
- `agent_id=中介用户ID`

中介确认后委托关系 `status=1`，若房源已经合法性审核通过，则租户端可见；中介拒绝后委托关系 `status=2`，房源恢复为房东自营，不写成审核拒绝。

### 用户取消/下架

`POST /system/house/{houseId}/cancel`

支持房东取消申请或下架房源。房源进入：

- `status=5`
- `ai_index_status=0`

TODO：创建 AI 向量 delete 任务，确保主页和 RAG 都不可检索。

### 成交交付

`POST /system/house/{houseId}/deal`

房东、中介或后台确认成交。当前会创建一条合同记录，并将房源设为：

- `status=4`
- `ai_index_status=0`

成交后房源不再出现在公开列表或 RAG 中，但房东、中介、成交租户、后台可查看详情。

TODO：合同层继续实现合同编号规则、电子签章、租期合法性、租金押金校验、交付验收单、支付确认。

## 状态设计说明

本次没有引入完整状态模式类族，而是使用轻量枚举 `RentalHouseStatus` 承载状态流转判断，例如：

- `canSubmitAudit()`
- `canEditBeforeApproval()`
- `canBeAudited()`
- `canEntrust()`
- `canCancel()`
- `canCompleteDeal()`

这样能先把业务规则集中起来，避免 controller/service 到处写魔法字符串。后续如果每个状态动作需要复杂副作用，如通知、合同、支付、风控、AI 索引任务，可以平滑升级为完整状态处理器。

## 后续层 TODO 清单

租户闭环：

- `/rental/tenant/favorites/{houseId}`：收藏/取消收藏租户可见房源。
- `/rental/tenant/appointments`：创建看房预约，创建后返回 appointment 聊天会话。
- `/rental/tenant/intentions`：创建租赁意向，创建后返回 intention 聊天会话。
- `/rental/tenant/houses/{houseId}/deal`：租户发起成交申请，创建待确认合同并返回 contract 聊天会话。

中介闭环：

- `/rental/agent/houses`：查看受托房源。
- `/rental/agent/appointments/*`：确认、拒绝、完成预约，复用 appointment 聊天。
- `/rental/agent/intentions/*`：跟进、无效、成交意向，复用 intention/contract 聊天。
- `/rental/agent/houses/{houseId}/deal`：中介直接确认成交，创建合同并返回 contract 聊天。

合同确认展示：

- 新增 `rental_contract_confirm`：保存租户、房东、中介各自确认状态。
- `/rental/contract/{contractId}`：合同详情，聚合合同主表和确认列表。
- `/rental/contract/{contractId}/tenant-confirm`：租户确认合同。
- `/rental/contract/{contractId}/owner-confirm`：房东确认合同。
- `/rental/contract/{contractId}/agent-confirm`：中介确认合同。
- `/rental/contract/{contractId}/reject`：参与方拒绝合同，合同作废。
- `/rental/contract/{contractId}/activate`：全部参与方确认后合同生效。
- `/rental/contract/{contractId}/void`：签署前作废。
- `/rental/contract/{contractId}/terminate`：生效后终止。
- `/rental/contract/{contractId}/chat`：打开合同聊天。

支付说明：

- 当前系统不维护支付表、不接入线上支付。
- 微信、支付宝、银行卡、现金等均作为合同参与方线下交互。
- 系统只负责合同确认、状态展示、聊天协同和后续交付对接。

AI 索引层：

- `createHouseIndexUpsertTask(houseId)`：审核通过后创建向量索引更新任务。
- `createHouseIndexDeleteTask(houseId)`：成交或下架后创建向量索引删除任务。

合同层：

- `generateContractNo(houseId, tenantId)`：统一合同编号。
- `validateContractPeriod(startDate, endDate)`：校验租期。
- `activateContractAfterSign(contractId)`：电子签完成后合同生效。

支付/交付层：

- `confirmDepositPaid(contractId)`：确认押金。
- `confirmRentPaid(contractId)`：确认首期租金。
- `completeHouseDelivery(contractId)`：完成交付验收。

通知层：

- `notifyAdminHousePendingAudit(houseId)`：提醒管理员审核。
- `notifyOwnerAuditResult(houseId)`：通知房东审核结果。
- `notifyAgentEntrustCreated(entrustId)`：通知中介处理委托。

权限与审计层：

- “普通管理员审核、超级管理员只监督”已经落到按钮权限 `system:house:audit`、后端超级管理员拦截和 RuoYi 操作日志。
