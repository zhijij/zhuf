# RuoYi 智能租赁系统开发落地指南

> 适用项目：`F:\JAVA_project\home_agent\RuoYi-Vue`，当前版本 `RuoYi v3.9.2`

## 1. 先说结论

在 RuoYi 里开发这套智能租赁系统，建议分成两层：

- 第一层：用 MySQL 建业务表，再用 RuoYi 代码生成器生成基础 CRUD。
- 第二层：复杂业务不要依赖生成器，手写在 `service`、`controller`、`mapper.xml`、AI 服务里。

适合代码生成器的内容：

- 房源管理
- 房源图片管理
- 户主资料管理
- 房源委托关系管理
- 收藏管理
- 预约管理
- 租赁意向管理
- 合同管理
- AI 会话/消息/Memory/知识库/向量任务管理

不适合代码生成器的内容：

- 房源审核流转
- 委托中介审批流
- 看房预约冲突校验
- 合同生成与风险分析
- Java 调 Python AI 服务
- Milvus 向量同步
- RAG、Memory、Multi-Agent、MCP、Function Calling

## 2. RuoYi 项目里各层放哪里

你当前项目的典型目录是：

- 后端启动类与 controller：`RuoYi-Vue/ruoyi-admin`
- 业务实体、mapper、service：`RuoYi-Vue/ruoyi-system`
- 代码生成器：`RuoYi-Vue/ruoyi-generator`
- 前端 Vue2：`RuoYi-Vue/ruoyi-ui`

对你这个项目，建议这样放：

- 简单业务实体、Mapper、Service：放在 `ruoyi-system`
- 页面接口 Controller：放在 `ruoyi-admin/src/main/java/com/ruoyi/web/controller`
- AI 相关 Controller：先放 `ruoyi-admin/src/main/java/com/ruoyi/web/controller/ai`
- AI 业务 Service：先放 `ruoyi-system/src/main/java/com/ruoyi/system/service`
- Python AI 服务：单独新建目录，例如 `F:\JAVA_project\home_agent\ai-service`

推荐的 Java 包结构：

```text
com.ruoyi.system.domain.rental
com.ruoyi.system.mapper.rental
com.ruoyi.system.service.rental
com.ruoyi.system.service.rental.impl

com.ruoyi.web.controller.rental
com.ruoyi.web.controller.ai
```

如果你暂时不想细分子包，也可以先让生成器按 RuoYi 默认生成到：

- `com.ruoyi.system.domain`
- `com.ruoyi.system.mapper`
- `com.ruoyi.system.service`
- `com.ruoyi.web.controller.system`

后面稳定后再整理包结构。

## 3. 建表应该怎么做

### 3.1 正确顺序

建议你按这个顺序做：

1. 先在 MySQL 里执行 SQL 建表。
2. 启动 RuoYi 后端和前端。
3. 登录系统，进入“系统工具 -> 代码生成”。
4. 从数据库导入刚创建的表。
5. 配置每张表的生成信息和字段信息。
6. 预览代码。
7. 生成代码到项目。
8. 对生成代码进行二次改造。

### 3.2 建表位置

建议把你的租赁系统 SQL 单独放一个文件，方便反复执行和版本管理。

推荐新建：

[sql/smart_rental.sql](F:/JAVA_project/home_agent/RuoYi-Vue/sql)

里面先放你现在已经设计好的这些表：

- `rental_house`
- `rental_house_image`
- `rental_owner_profile`
- `rental_house_entrust`
- `rental_tenant_preference`
- `rental_house_favorite`
- `rental_appointment`
- `rental_intention`
- `rental_contract`
- `ai_chat_session`
- `ai_chat_message`
- `ai_user_memory`
- `ai_knowledge_doc`
- `ai_knowledge_chunk`
- `ai_vector_index_task`
- `ai_tool_audit_log`

### 3.3 建表规范

为了让 RuoYi 生成器更顺手，表字段建议遵守这些规则：

- 主键统一 `xxx_id BIGINT AUTO_INCREMENT`
- 逻辑删除统一 `del_flag CHAR(1) DEFAULT '0'`
- 创建人 `create_by`
- 创建时间 `create_time`
- 更新人 `update_by`
- 更新时间 `update_time`
- 备注 `remark`

原因：

- RuoYi 对这些字段有默认识别逻辑。
- 生成器会自动把这些字段作为基础字段处理。
- 后续审计、分页、列表展示会省很多事。

## 4. RuoYi 代码生成器怎么用

### 4.1 先启动项目

后端：

```powershell
cd F:\JAVA_project\home_agent\RuoYi-Vue
mvn clean package
```

运行后端通常是启动 `ruoyi-admin`。

前端：

```powershell
cd F:\JAVA_project\home_agent\RuoYi-Vue\ruoyi-ui
npm install
npm run dev
```

然后登录若依系统后台。

### 4.2 导入表

进入：

```text
系统工具 -> 代码生成
```

操作步骤：

1. 点击“导入”。
2. 选择数据库里你刚建好的 `rental_*` 和 `ai_*` 表。
3. 导入后进入每张表的“编辑”。

### 4.3 编辑生成信息

每张表都要重点看这几个配置：

- `表名称`
- `表描述`
- `实体类名称`
- `生成包路径`
- `生成模块名`
- `生成业务名`
- `生成功能名`
- `上级菜单`

建议这样配：

- 生成包路径：`com.ruoyi.system`
- 生成模块名：`system`
- 业务名：例如 `house`、`appointment`、`contract`
- 功能名：例如“房源管理”“预约管理”

因为你当前 `generator.yml` 默认就是：

- `packageName: com.ruoyi.system`

所以先顺着默认配置来，最稳。

### 4.4 编辑字段信息

这是最重要的一步。

你要逐列检查：

- 是否插入
- 是否编辑
- 是否列表显示
- 是否查询
- 查询方式
- 表单类型
- 字典类型

几个常见建议：

- `status`、`audit_status`、`operation_mode` 这种字段：用字典管理
- `create_time`：一般列表显示，可查询，查询方式选范围
- `remark`：表单类型用文本域
- `description`、`contract_content`：表单类型用文本域
- `rent_amount`、`deposit_amount`：列表显示，支持范围查询
- `del_flag`：不在页面里直接编辑
- `create_by/create_time/update_by/update_time`：通常不让前端编辑

### 4.5 字典要先建

像这些字段建议先去“系统管理 -> 字典管理”建字典：

- 房源状态 `rental_house_status`
- 审核状态 `rental_audit_status`
- 运营方式 `rental_operation_mode`
- 预约状态 `rental_appointment_status`
- 委托状态 `rental_entrust_status`
- 合同状态 `rental_contract_status`

这样代码生成器生成页面时，下拉框会更完整。

### 4.6 生成代码

生成方式建议分两种：

第一种：先“预览代码”

- 看看实体、mapper、service、controller、vue 页面是否符合预期。

第二种：再“生成代码”

- 初期建议先下载 zip 看生成结果。
- 确认没问题后再生成到自定义路径或手工合并。

原因：

- 若依生成器适合打基础，但不是所有字段和交互都一次完美。
- 先预览能避免直接把不满意的代码落进项目。

## 5. 建议先生成哪些表

不要一次把所有表都生成，容易乱。

建议第一批只生成这 6 张：

1. `rental_house`
2. `rental_house_image`
3. `rental_owner_profile`
4. `rental_house_entrust`
5. `rental_appointment`
6. `rental_contract`

第二批再生成：

1. `rental_house_favorite`
2. `rental_tenant_preference`
3. `rental_intention`
4. `ai_chat_session`
5. `ai_chat_message`
6. `ai_user_memory`

第三批再生成后台支撑表：

1. `ai_knowledge_doc`
2. `ai_knowledge_chunk`
3. `ai_vector_index_task`
4. `ai_tool_audit_log`

## 6. 复杂业务在哪开发

这部分最关键。你后面真正的系统价值，大多都不在“生成代码”里，而在手写业务里。

### 6.1 应该手写在 Java 的业务

放在：

- `ruoyi-system/service`
- `ruoyi-system/service/impl`
- `ruoyi-admin/controller`
- `ruoyi-system/mapper` + `mapper xml`

这些复杂业务要手写：

- 房源审核流转
- 户主委托中介审批
- 房源上下架规则
- 看房预约时间冲突校验
- 同一租户防重复预约
- 合同生成逻辑
- 合同状态流转
- AI 工具内部接口
- AI 会话落库
- 向量索引任务状态回写

例子：

- `HouseServiceImpl`：房源发布、审核、上下架、同步索引任务
- `EntrustServiceImpl`：户主委托中介、确认委托、终止委托
- `AppointmentServiceImpl`：预约创建、预约冲突校验、通知逻辑
- `ContractServiceImpl`：合同草稿、签约状态流转
- `AiInternalServiceImpl`：供 Python AI 调用的内部工具接口

### 6.2 应该开发在 Python 的业务

放在单独 AI 服务里，例如：

```text
F:\JAVA_project\home_agent\ai-service
```

这些内容不要写进 RuoYi Java 主系统：

- LangChain / LangGraph
- Multi-Agent 编排
- RAG 检索
- Milvus 读写
- Memory 提取
- Prompt 模板
- MCP Client / MCP Server
- Function Calling 执行器

原因：

- Python 生态更适合 AI 编排。
- Java 更适合稳定业务系统和权限事务。
- 这样分层后，后面替换模型、改 RAG、换向量库都更轻松。

### 6.3 Java 和 Python 的边界

建议坚持一条原则：

- Java 管业务事实
- Python 管智能推理

也就是：

- 房源、预约、合同、权限、审核：Java 决定
- 推荐、问答、总结、检索、文案、风险提示：Python 负责

Python 不能直接写业务表。

正确方式是：

1. Python 调 Java 内部接口。
2. Java 校验权限和业务规则。
3. Java 写 MySQL。
4. Java 返回结果给 Python。

## 7. 第一阶段最推荐的开发顺序

你现在最适合这样推进：

1. 先把 SQL 落下来，建表。
2. 用 RuoYi 生成第一批表的 CRUD。
3. 跑通房源、户主、委托、预约、合同基础页面。
4. 手写房源审核、委托确认、预约校验。
5. 再接 Python AI 服务。
6. 再接 Milvus 和 RAG。

这个顺序的好处是：

- 你先拥有一个完整可用的租赁业务骨架。
- 后面 AI 接入时，不会一上来就和数据库、权限、页面一起打架。

## 8. 你现在最该做的事

如果你马上开始落地，我建议当前第一步就是：

1. 把建表 SQL 整理成一个完整文件。
2. 先在数据库执行。
3. 先导入 `rental_house`、`rental_owner_profile`、`rental_house_entrust`、`rental_appointment` 这 4 张表到代码生成器。
4. 生成第一版代码。

如果你愿意，我下一步可以直接继续帮你做两件具体的事：

- 把完整建表 SQL 文件真正落到 [RuoYi-Vue/sql](/abs/path placeholder) 目录里
- 再给你一份“每张表在 RuoYi 代码生成器里字段怎么勾选”的详细配置清单
