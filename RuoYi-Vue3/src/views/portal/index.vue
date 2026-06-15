<template>
  <div class="portal-shell" :class="{ 'is-agent': pageMode === 'agent' }">
    <header class="portal-header">
      <div class="brand">
        <span>智能AI房屋租赁系统</span>
        <small>{{ roleLabel }}</small>
      </div>

      <div class="page-switch" role="tablist" aria-label="首页模式切换">
        <button :class="{ active: pageMode === 'agent' }" @click="switchPage('agent')">AI助手</button>
        <button :class="{ active: pageMode === 'business' }" @click="switchPage('business')">业务台</button>
      </div>

      <div class="header-actions">
        <el-button v-if="canOpenAiConsole" text icon="Promotion" @click="openAiConsole">AI 工作台</el-button>
        <el-button text icon="ChatLineRound" @click="openMessagePanel">消息</el-button>
        <el-button text icon="Refresh" @click="refreshCurrent">刷新</el-button>
        <el-dropdown trigger="click" popper-class="portal-user-menu" @command="handleUserCommand">
          <button class="user-entry">
            <el-avatar :size="30" :src="userStore.avatar">
              {{ userInitial }}
            </el-avatar>
            <span>{{ displayUserName }}</span>
            <el-icon><ArrowDown /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile" icon="User">个人中心</el-dropdown-item>
              <el-dropdown-item command="password" icon="Lock">修改密码</el-dropdown-item>
              <el-dropdown-item command="message" icon="ChatLineRound">消息中心</el-dropdown-item>
              <el-dropdown-item command="agent" icon="Promotion">AI助手</el-dropdown-item>
              <el-dropdown-item command="business" icon="Briefcase">业务台</el-dropdown-item>
              <el-dropdown-item command="primaryAction" icon="Operation">{{ primaryActionLabel }}</el-dropdown-item>
              <el-dropdown-item v-if="visibleWorkModes.includes('contract')" command="contracts" icon="Document">我的合同</el-dropdown-item>
              <el-dropdown-item v-if="canOpenAiConsole" command="aiConsole" icon="Aim">AI 工作台</el-dropdown-item>
              <el-dropdown-item divided command="logout" icon="SwitchButton">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <main v-if="pageMode === 'business'" class="business-page">
      <section class="business-search">
        <div v-if="isAdmin" class="admin-mode-badge">
          合规审核
        </div>
        <div v-else class="role-tabs">
          <button
            v-for="item in roleModeOptions"
            :key="item.value"
            :class="{ active: workMode === item.value }"
            @click="workMode = item.value"
          >
            {{ item.shortLabel }}
          </button>
        </div>
        <el-input v-model="filters.keyword" clearable placeholder="搜索房源、城市、编号、用户ID">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="filters.status" clearable placeholder="全部状态">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" @click="refreshMode">搜索</el-button>
      </section>

      <section class="hot-tags">
        <span>快捷筛选：</span>
        <button v-for="tag in quickTags" :key="tag" @click="filters.keyword = tag">{{ tag }}</button>
      </section>

      <section class="business-workspace">
        <aside class="result-list">
          <div class="list-head">
            <div>
              <p>{{ currentRoleConfig.eyebrow }}</p>
              <h1>{{ currentRoleConfig.title }}</h1>
            </div>
            <span>{{ filteredRecords.length }} 条</span>
          </div>

          <div v-loading="loading" class="records">
            <button
              v-for="item in filteredRecords"
              :key="recordKey(item)"
              class="record-card"
              :class="{ active: recordKey(item) === recordKey(selected) }"
              @click="selectRecord(item)"
            >
              <div class="record-card__top">
                <strong>{{ recordTitle(item) }}</strong>
                <span>{{ priceText(item) }}</span>
              </div>
              <div class="record-tags">
                <em>{{ recordTypeLabel(item) }}</em>
                <em>{{ recordStatusLabel(item) }}</em>
                <em v-if="item.city">{{ item.city }}</em>
                <em v-if="item.district">{{ item.district }}</em>
              </div>
              <div class="record-card__bottom">
                <span>{{ ownerAgentText(item) }}</span>
                <small>{{ recordIdText(item) }}</small>
              </div>
            </button>
            <el-empty v-if="!loading && !filteredRecords.length" :description="emptyDescription" :image-size="90" />
          </div>
        </aside>

        <section class="detail-card">
          <template v-if="selected">
            <header class="detail-head">
              <div>
                <h2>{{ recordTitle(selected) }}</h2>
                <p>{{ recordSubtitle(selected) }}</p>
                <div class="detail-tags">
                  <el-tag :type="tagType(recordStatus(selected), selected)" effect="plain">{{ recordStatusLabel(selected) }}</el-tag>
                  <el-tag effect="plain">{{ recordTypeLabel(selected) }}</el-tag>
                </div>
              </div>
              <div class="detail-head__aside">
                <strong>{{ priceText(selected) }}</strong>
                <el-button plain @click="openSelectedDetail">查看完整详情</el-button>
              </div>
            </header>

            <section class="field-grid">
              <article v-for="field in previewFields" :key="field.key">
                <span>{{ field.label }}</span>
                <strong>{{ field.formatter ? field.formatter(selected[field.key], selected) : formatValue(selected[field.key]) }}</strong>
              </article>
            </section>

            <section class="process-card">
              <h3>业务流程</h3>
              <div class="process-steps">
                <div v-for="step in processSteps" :key="step.label" :class="{ done: step.done }">
                  <span></span>
                  <p>{{ step.label }}</p>
                </div>
              </div>
            </section>

            <section class="action-card">
              <div>
                <h3>{{ currentRoleConfig.actionTitle }}</h3>
                <p>{{ currentRoleConfig.actionHint }}</p>
              </div>
              <div class="action-buttons">
                <el-button
                  v-for="action in visibleActions"
                  :key="action.key"
                  :type="action.type || 'default'"
                  :icon="action.icon"
                  :disabled="action.disabled"
                  @click="action.handler(selected)"
                >
                  {{ action.label }}
                </el-button>
                <el-button v-if="chatTarget" type="primary" icon="ChatLineRound" @click="openChat(chatTarget)">
                  立即沟通
                </el-button>
              </div>
            </section>

            <section v-if="workMode === 'tenant' && isTenantHouseContext(selected)" class="operation-card">
              <h3>租户事务</h3>
              <div class="panel-actions">
                <el-button type="primary" @click="openTransaction('tenantAppointment')">预约看房</el-button>
                <el-button plain @click="openTransaction('tenantIntention')">提交意向</el-button>
                <el-button plain @click="openTransaction('tenantDeal')">成交申请</el-button>
              </div>
            </section>

            <section v-if="workMode === 'owner' && isOwnerHouseContext(selected)" class="operation-card">
              <h3>委托中介</h3>
              <div class="panel-actions">
                <el-button type="success" @click="openTransaction('ownerEntrust')">发起委托</el-button>
              </div>
            </section>

            <section v-if="workMode === 'owner'" class="operation-card">
              <h3>新建房源</h3>
              <div class="panel-actions">
                <el-button type="primary" @click="openTransaction('ownerCreateHouse')">新建房源</el-button>
                <el-button
                  v-if="selected.houseId && ['3', 3].includes(recordStatus(selected))"
                  plain
                  @click="openTransaction('ownerSubmitAudit')"
                >
                  重新提交审核
                </el-button>
                <el-button plain @click="loadOwnerHistory">查看历史委托</el-button>
              </div>
            </section>

            <section v-if="workMode === 'agent' && isAgentCandidateHouse(selected)" class="operation-card">
              <h3>申请承接</h3>
              <div class="panel-actions">
                <el-button type="success" @click="openTransaction('agentApplyEntrust')">申请承接</el-button>
              </div>
            </section>

            <section v-if="workMode === 'agent' && selected.appointmentId" class="operation-card">
              <h3>预约处理</h3>
              <div class="inline-form compact">
                <el-button type="success" @click="openTransaction('agentConfirmAppointment')">确认预约</el-button>
                <el-button type="warning" @click="openTransaction('agentCompleteAppointment')">完成看房</el-button>
                <el-button type="danger" @click="openTransaction('agentRejectAppointment')">拒绝预约</el-button>
              </div>
            </section>

            <section v-if="workMode === 'agent' && selected.intentionId" class="operation-card">
              <h3>意向跟进</h3>
              <div class="panel-actions">
                <el-button type="primary" @click="openTransaction('agentFollowIntention')">保存跟进</el-button>
                <el-button type="danger" @click="openTransaction('agentInvalidIntention')">标记无效</el-button>
                <el-button type="success" @click="openTransaction('agentDealIntention')">意向转成交</el-button>
              </div>
            </section>

            <section v-if="workMode === 'agent' && isAgentEntrustedHouse(selected)" class="operation-card">
              <h3>成交信息</h3>
              <div class="panel-actions">
                <el-button type="success" @click="openTransaction('agentDealHouse')">确认成交</el-button>
              </div>
            </section>

            <section v-if="workMode === 'contract'" class="operation-card">
              <h3>合同意见</h3>
              <div class="contract-extra-actions">
                <el-button v-if="canManageContractLifecycle" type="success" @click="openTransaction('contractActivate', selected)">合同生效</el-button>
                <el-button type="warning" @click="openTransaction('contractVoid', selected)">作废合同</el-button>
                <el-button v-if="canManageContractLifecycle" type="danger" @click="openTransaction('contractTerminate', selected)">终止合同</el-button>
              </div>
              <div class="panel-actions">
                <el-button plain @click="loadContractDetailFromSelected">查看合同详情</el-button>
                <el-button v-if="selected.contractId" plain @click="openContractChatFromSelected">打开合同沟通</el-button>
              </div>
              <div v-if="contractConfirms.length" class="table-shell compact-shell">
                <el-table :data="contractConfirms" size="small">
                  <el-table-column prop="userRole" label="角色" min-width="100" />
                  <el-table-column prop="confirmStatus" label="确认状态" min-width="110" />
                  <el-table-column prop="confirmOpinion" label="处理意见" min-width="180" show-overflow-tooltip />
                  <el-table-column prop="confirmTime" label="确认时间" min-width="160" />
                </el-table>
              </div>
            </section>

            <section v-if="workMode === 'admin'" class="operation-card compliance-card">
              <h3>合规审查要点</h3>
              <div class="compliance-checks">
                <span :class="{ done: Boolean(selected.title) }">标题完整</span>
                <span :class="{ done: Boolean(selected.city && selected.district && selected.address) }">地址清晰</span>
                <span :class="{ done: Number(selected.rentAmount) > 0 }">租金有效</span>
                <span :class="{ done: Number(selected.area) > 0 }">面积有效</span>
                <span :class="{ done: Boolean(selected.ownerId) }">户主明确</span>
              </div>
              <div class="panel-actions">
                <el-button type="success" :disabled="!canAuditSelectedHouse" @click="openTransaction('adminApproveHouse')">通过并发布</el-button>
                <el-button type="danger" :disabled="!canAuditSelectedHouse" @click="openTransaction('adminRejectHouse')">驳回房源</el-button>
                <el-button plain @click="refreshMode">刷新审核队列</el-button>
              </div>
              <p v-if="!canAuditSelectedHouse" class="muted-tip">当前房源不是待审核状态，只能查看不能重复审核。</p>
            </section>

            <section v-if="workMode === 'tenant'" class="operation-card">
              <h3>历史记录</h3>
              <div class="panel-actions">
                <el-button plain @click="loadTenantFavoritesFromWorkspace">我的收藏</el-button>
                <el-button plain @click="loadTenantAppointmentsFromWorkspace">我的预约</el-button>
                <el-button plain @click="loadTenantIntentionsFromWorkspace">我的意向</el-button>
                <el-button plain @click="loadTenantContractsFromWorkspace">我的合同</el-button>
                <el-button v-if="selected.favoriteId" plain @click="cancelFavoriteFromSelected">取消收藏</el-button>
                <el-button v-if="selected.appointmentId" plain @click="cancelAppointmentFromSelected">取消预约</el-button>
                <el-button v-if="selected.intentionId" plain @click="abandonIntentionFromSelected">放弃意向</el-button>
                <el-button v-if="selected.houseId" plain @click="loadTenantHouseDetailFromSelected">查看房源详情</el-button>
              </div>
            </section>

            <section v-if="workMode === 'agent'" class="operation-card">
              <h3>业务视图</h3>
              <div class="panel-actions">
                <el-button plain @click="loadAgentHistory">我的受托房源</el-button>
                <el-button v-if="selected.houseId" plain @click="loadAgentHouseDetailFromSelected">查看房源详情</el-button>
              </div>
            </section>

            <section v-if="businessAiCard" class="business-ai-card">
              <div>
                <span>AI 辅助</span>
                <h3>{{ businessAiCard.title }}</h3>
                <p>{{ businessAiCard.description }}</p>
              </div>
              <div class="business-ai-actions">
                <el-input
                  v-if="workMode === 'tenant'"
                  v-model="aiRecommend.query"
                  clearable
                  class="ai-inline-input"
                  placeholder="预算、通勤、户型偏好"
                  @keyup.enter="runBusinessAiAction"
                />
                <el-input-number
                  v-if="workMode === 'tenant'"
                  v-model="aiRecommend.maxRent"
                  :min="0"
                  controls-position="right"
                  placeholder="预算上限"
                />
                <el-button type="primary" :loading="floatingAiLoading" @click="runBusinessAiAction">
                  {{ businessAiCard.actionLabel }}
                </el-button>
                <el-button plain @click="openFloatingAi">打开助手</el-button>
              </div>
              <p v-if="workMode === 'tenant' && aiRecommendAnswer" class="ai-recommend-note">{{ aiRecommendAnswer }}</p>
            </section>
          </template>

          <template v-else>
            <div class="empty-workspace">
              <el-empty :description="emptyDescription" :image-size="130" />
            </div>

            <section v-if="workMode === 'owner'" class="operation-card">
              <h3>新建房源</h3>
              <div class="panel-actions">
                <el-button type="primary" @click="openTransaction('ownerCreateHouse')">新建房源</el-button>
              </div>
            </section>

            <section v-if="workMode === 'tenant'" class="operation-card">
              <h3>找房入口</h3>
              <div class="panel-actions">
                <el-button type="primary" @click="refreshMode">刷新推荐房源</el-button>
                <el-button plain @click="loadPortalPublicHouses">查看公开房源</el-button>
                <el-button plain @click="loadTenantAppointmentsFromWorkspace">我的预约</el-button>
                <el-button plain @click="loadTenantContractsFromWorkspace">我的合同</el-button>
              </div>
            </section>

            <section v-if="workMode === 'agent'" class="operation-card">
              <h3>中介工作入口</h3>
              <div class="panel-actions">
                <el-button type="primary" @click="loadAgentCandidateWorkspace">查看可承接房源</el-button>
                <el-button plain @click="loadAgentHistory">我的受托房源</el-button>
                <el-button plain @click="refreshMode">刷新业务队列</el-button>
              </div>
            </section>

            <section v-if="workMode === 'contract'" class="operation-card">
              <h3>合同入口</h3>
              <div class="panel-actions">
                <el-button type="primary" @click="refreshMode">刷新我的合同</el-button>
                <el-button plain @click="openMessagePanel">打开消息中心</el-button>
              </div>
            </section>

            <section v-if="workMode === 'admin'" class="operation-card">
              <h3>合规审核入口</h3>
              <div class="panel-actions">
                <el-button type="primary" @click="refreshMode">刷新待审核房源</el-button>
              </div>
            </section>
          </template>
        </section>
      </section>
    </main>

    <main v-else class="agent-home">
      <div class="agent-topline">
        <button class="model-button">{{ aiAssistantProfile.badge }}</button>
      </div>

      <section class="agent-center" :class="{ 'has-chat': agentMessages.length > 1 }">
        <div v-if="agentMessages.length <= 1" class="agent-hero">
          <p>{{ aiAssistantProfile.eyebrow }}</p>
          <h1>{{ aiAssistantProfile.title }}</h1>
          <span class="agent-summary">{{ aiAssistantProfile.summary }}</span>
          <div class="agent-capabilities">
            <span v-for="item in aiAssistantProfile.capabilities" :key="item">{{ item }}</span>
          </div>
        </div>

        <div v-else class="agent-thread">
          <div v-for="(item, index) in agentMessages" :key="index" :class="['agent-message', item.role]">
            <div class="message-content">{{ item.content }}</div>
            <div v-if="item.intentLabel || item.toolCalls?.length" class="ai-trace">
              <span v-if="item.intentLabel">意图：{{ item.intentLabel }}</span>
              <span v-for="tool in item.toolCalls" :key="`${index}-${tool.name}`">
                {{ tool.label || tool.name }} · {{ tool.status === 'success' ? '完成' : '异常' }}
              </span>
            </div>
          </div>
        </div>

        <div class="agent-input-card">
          <el-input
            v-model="agentInput"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 5 }"
            resize="none"
            :placeholder="aiAssistantProfile.placeholder"
            @keydown.ctrl.enter.prevent="sendAgentMessage"
          />
          <el-button circle icon="Promotion" type="primary" :loading="agentLoading" @click="sendAgentMessage" />
        </div>

        <div class="agent-actions">
          <button v-for="item in agentPresets" :key="item" @click="agentInput = item">
            {{ item }}
          </button>
        </div>
      </section>
    </main>

    <el-drawer v-model="messageOpen" title="消息" size="860px" append-to-body destroy-on-close @opened="prepareMessagePanel">
      <BusinessChatPanel ref="messagePanelRef" auto-open />
    </el-drawer>

    <el-drawer
      v-model="detailOpen"
      :title="detailDrawerTitle"
      size="760px"
      append-to-body
      destroy-on-close
      class="record-detail-drawer"
    >
      <template v-if="selected">
        <section class="drawer-detail-shell">
          <header class="detail-head drawer-head">
            <div>
              <h2>{{ recordTitle(selected) }}</h2>
              <p>{{ recordSubtitle(selected) }}</p>
              <div class="detail-tags">
                <el-tag :type="tagType(recordStatus(selected), selected)" effect="plain">{{ recordStatusLabel(selected) }}</el-tag>
                <el-tag effect="plain">{{ recordTypeLabel(selected) }}</el-tag>
              </div>
            </div>
            <strong>{{ priceText(selected) }}</strong>
          </header>

          <section v-if="detailImageUrls.length" class="detail-gallery">
            <el-image
              v-for="(image, index) in detailImageUrls"
              :key="`${image}-${index}`"
              :src="image"
              :preview-src-list="detailImageUrls"
              fit="cover"
            />
          </section>

          <section class="field-grid detail-grid">
            <article v-for="field in detailFields" :key="field.key">
              <span>{{ field.label }}</span>
              <strong>{{ field.formatter ? field.formatter(selected[field.key], selected) : formatValue(selected[field.key]) }}</strong>
            </article>
          </section>

          <section class="process-card">
            <h3>业务流程</h3>
            <div class="process-steps">
              <div v-for="step in processSteps" :key="step.label" :class="{ done: step.done }">
                <span></span>
                <p>{{ step.label }}</p>
              </div>
            </div>
          </section>

          <section v-if="selected.description || selected.remark || selected.auditReason" class="operation-card">
            <h3>补充说明</h3>
            <div class="drawer-note-list">
              <article v-if="selected.description">
                <span>房源描述</span>
                <p>{{ selected.description }}</p>
              </article>
              <article v-if="selected.remark">
                <span>业务备注</span>
                <p>{{ selected.remark }}</p>
              </article>
              <article v-if="selected.auditReason">
                <span>审核意见</span>
                <p>{{ selected.auditReason }}</p>
              </article>
            </div>
          </section>

          <section v-if="selected.contractId && contractConfirms.length" class="table-shell">
            <el-table :data="contractConfirms" size="small">
              <el-table-column prop="userRole" label="角色" min-width="100" />
              <el-table-column prop="confirmStatus" label="确认状态" min-width="110" />
              <el-table-column prop="confirmOpinion" label="处理意见" min-width="180" show-overflow-tooltip />
              <el-table-column prop="confirmTime" label="确认时间" min-width="160" />
            </el-table>
          </section>
        </section>
      </template>
    </el-drawer>

    <el-drawer
      v-model="aiConsoleOpen"
      title="AI 工作台"
      size="900px"
      append-to-body
      destroy-on-close
      class="ai-console-drawer"
      @opened="refreshAiConsole"
    >
      <section class="ai-console-shell">
        <header class="ai-console-head">
          <div>
            <span>审核员工具</span>
            <h2>向量知识库与索引任务</h2>
            <p>AI 运维、房源索引和审核辅助放在这里，普通业务页面只保留当前角色的下一步动作。</p>
          </div>
          <div class="ai-console-status">
            <i :class="{ online: aiCapabilities.online }"></i>
            <strong>{{ aiCapabilities.online ? 'AI 在线' : 'AI 待检测' }}</strong>
          </div>
        </header>

        <div class="summary-strip console-summary">
          <span>公开房源 {{ workspaceSummary.houseCount || 0 }}</span>
          <span>已定价 {{ workspaceSummary.pricedHouseCount || 0 }}</span>
          <span>平均租金 {{ workspaceSummary.avgRent || 0 }}</span>
          <span>已索引 {{ workspaceSummary.indexedHouseCount || 0 }}</span>
          <span>知识库 {{ aiCapabilities.indexedKnowledge || 0 }}</span>
          <span v-for="chip in aiCapabilityChips" :key="chip">{{ chip }}</span>
        </div>

        <div class="console-actions">
          <el-button plain @click="refreshAiConsole">刷新状态</el-button>
          <el-button v-if="selected?.houseId" plain @click="inspectAiHouseDocumentFromSelected">检查当前房源文档</el-button>
          <el-button v-if="selected?.houseId" type="primary" plain @click="createAiIndexTaskFromSelected">当前房源入库</el-button>
          <el-button v-if="canRunFullAiSync" type="warning" plain @click="createFullAiIndexTask">全量同步索引</el-button>
          <el-button type="success" plain @click="processPendingAiTasks">批量处理索引</el-button>
          <el-button :loading="knowledgeSeeding" type="primary" plain @click="seedKnowledgeBase">导入基础知识</el-button>
        </div>

        <div class="mini-grid ai-status-grid">
          <article class="mini-card">
            <strong>能力状态</strong>
            <p>{{ aiCapabilities.vectorStore || 'PostgreSQL + pgvector' }} / {{ aiCapabilities.embeddingMode || '未检测' }}</p>
            <p>Skill {{ aiCapabilities.skills.length }} 个，Tool {{ aiCapabilities.tools.length }} 个</p>
          </article>
          <article class="mini-card">
            <strong>统一知识库</strong>
            <p>已入库 {{ aiCapabilities.indexedKnowledge || 0 }} 份知识文档</p>
            <p>{{ knowledgeSourceText }}</p>
          </article>
          <article v-if="latestAiTask" class="mini-card">
            <strong>最新索引任务</strong>
            <p>#{{ latestAiTask.taskId }} / {{ taskStatusLabel(latestAiTask.status) }}</p>
            <p>{{ latestAiTask.errorMsg || '暂无任务说明' }}</p>
          </article>
          <article v-if="aiHouseDocument" class="mini-card">
            <strong>当前房源文档</strong>
            <p>{{ aiHouseDocument.message || '暂无文档状态' }}</p>
            <p>索引状态：{{ aiIndexStatusLabel(aiHouseDocument.aiIndexStatus, aiHouseDocument.indexed) }}</p>
          </article>
        </div>

        <section class="knowledge-card">
          <div class="knowledge-card__head">
            <div>
              <strong>知识入库</strong>
              <p>导入合同、政策、FAQ、聊天摘要或企业制度，AI 会在推荐、问答和审核辅助时检索这些内容。</p>
            </div>
            <el-tag effect="plain">RAG</el-tag>
          </div>
          <el-form label-position="top" class="knowledge-form">
            <div class="knowledge-form__row">
              <el-form-item label="来源类型">
                <el-select v-model="knowledgeForm.sourceType">
                  <el-option label="FAQ" value="faq" />
                  <el-option label="政策" value="policy" />
                  <el-option label="合同模板" value="contract" />
                  <el-option label="聊天摘要" value="chat" />
                  <el-option label="企业制度" value="enterprise" />
                </el-select>
              </el-form-item>
              <el-form-item label="适用角色">
                <el-select v-model="knowledgeForm.roles" multiple collapse-tags collapse-tags-tooltip>
                  <el-option label="租户" value="tenant" />
                  <el-option label="户主" value="owner" />
                  <el-option label="中介" value="agent" />
                  <el-option label="审核员" value="auditor" />
                </el-select>
              </el-form-item>
            </div>
            <el-form-item label="标题">
              <el-input v-model="knowledgeForm.title" maxlength="80" show-word-limit placeholder="例如：押金退还规则 FAQ" />
            </el-form-item>
            <el-form-item label="正文">
              <el-input
                v-model="knowledgeForm.content"
                type="textarea"
                :rows="5"
                resize="none"
                maxlength="4000"
                show-word-limit
                placeholder="粘贴政策、合同条款、FAQ 答案、聊天摘要或企业制度正文"
              />
            </el-form-item>
            <div class="knowledge-actions">
              <el-button :loading="knowledgeSubmitting" type="primary" @click="submitKnowledgeDocument">写入知识库</el-button>
              <el-button plain @click="resetKnowledgeForm">清空</el-button>
            </div>
          </el-form>
        </section>

        <div class="table-shell compact-shell">
          <el-table :data="aiIndexTasks" size="small">
            <el-table-column prop="taskId" label="任务ID" width="96" />
            <el-table-column prop="sourceId" label="房源ID" width="96" />
            <el-table-column prop="action" label="动作" width="110" />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">{{ taskStatusLabel(row.status) }}</template>
            </el-table-column>
            <el-table-column prop="errorMsg" label="任务说明" min-width="220" show-overflow-tooltip />
            <el-table-column prop="createTime" label="创建时间" min-width="160" />
            <el-table-column label="操作" width="96" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" :disabled="['1', '2'].includes(String(row.status))" @click="processAiTask(row)">
                  处理
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </section>
    </el-drawer>

    <BusinessChatDrawer
      v-model="chatOpen"
      :biz-type="chatContext.bizType"
      :biz-id="chatContext.bizId"
      :session-id="chatContext.sessionId"
      :title="chatContext.title"
    />

    <el-dialog
      v-model="transactionDialog.visible"
      :title="transactionMeta.title"
      width="640px"
      append-to-body
      destroy-on-close
      class="transaction-dialog"
    >
      <el-form label-position="top" class="transaction-form">
        <div v-if="transactionMeta.form === 'none'" class="transaction-summary">
          <strong>{{ recordTitle(transactionDialog.row || selected) }}</strong>
          <p>{{ recordSubtitle(transactionDialog.row || selected) || '确认后将写入业务记录，并同步到相关沟通会话。' }}</p>
        </div>

        <template v-if="transactionDialog.type === 'ownerCreateHouse'">
          <el-form-item label="房源标题" required>
            <el-input v-model="ownerHouse.title" placeholder="例如：近地铁两居室整租" />
          </el-form-item>
          <div class="form-grid two">
            <el-form-item label="城市" required>
              <el-input v-model="ownerHouse.city" placeholder="城市" />
            </el-form-item>
            <el-form-item label="区域" required>
              <el-input v-model="ownerHouse.district" placeholder="区域" />
            </el-form-item>
          </div>
          <el-form-item label="详细地址" required>
            <el-input v-model="ownerHouse.address" placeholder="详细地址" />
          </el-form-item>
          <div class="form-grid two">
            <el-form-item label="月租">
              <el-input-number v-model="ownerHouse.rentAmount" :min="0" controls-position="right" />
            </el-form-item>
            <el-form-item label="面积">
              <el-input-number v-model="ownerHouse.area" :min="0" controls-position="right" />
            </el-form-item>
          </div>
          <el-form-item label="房源图片">
            <ImageUpload v-model="ownerHouse.imageUrls" :limit="8" />
          </el-form-item>
          <el-form-item label="房源描述">
            <el-input v-model="ownerHouse.description" type="textarea" :rows="3" resize="none" placeholder="交通、装修、配套、看房时间等" />
          </el-form-item>
        </template>

        <template v-if="transactionDialog.type === 'tenantAppointment'">
          <el-form-item label="预约时间" required>
            <el-date-picker v-model="tenantAppointment.appointmentTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择预约时间" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="tenantAppointment.remark" type="textarea" :rows="3" resize="none" placeholder="看房人数、期望沟通点等" />
          </el-form-item>
        </template>

        <template v-if="transactionDialog.type === 'tenantIntention'">
          <el-form-item label="意向等级" required>
            <el-select v-model="tenantIntention.intentionLevel" placeholder="选择意向等级">
              <el-option label="高" value="3" />
              <el-option label="中" value="2" />
              <el-option label="低" value="1" />
            </el-select>
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="tenantIntention.note" type="textarea" :rows="3" resize="none" placeholder="预算、入住时间、关注点" />
          </el-form-item>
        </template>

        <template v-if="transactionMeta.form === 'deal'">
          <div class="form-grid two">
            <el-form-item v-if="transactionDialog.type === 'agentDealHouse'" label="租户用户ID" required>
              <el-input v-model="dealForm.tenantId" placeholder="租户用户ID" />
            </el-form-item>
            <el-form-item label="付款周期">
              <el-select v-model="dealForm.paymentCycle" placeholder="付款周期">
                <el-option label="月付" value="月付" />
                <el-option label="季付" value="季付" />
                <el-option label="半年付" value="半年付" />
              </el-select>
            </el-form-item>
          </div>
          <div class="form-grid two">
            <el-form-item label="合同开始" required>
              <el-date-picker v-model="dealForm.startDate" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" />
            </el-form-item>
            <el-form-item label="合同结束" required>
              <el-date-picker v-model="dealForm.endDate" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" />
            </el-form-item>
          </div>
          <div class="form-grid two">
            <el-form-item label="月租" required>
              <el-input-number v-model="dealForm.rentAmount" :min="0" controls-position="right" />
            </el-form-item>
            <el-form-item label="押金">
              <el-input-number v-model="dealForm.depositAmount" :min="0" controls-position="right" />
            </el-form-item>
          </div>
          <el-form-item label="合同补充条款">
            <el-input v-model="dealForm.contractContent" type="textarea" :rows="3" resize="none" placeholder="补充条款、交付约定、备注" />
          </el-form-item>
        </template>

        <template v-if="transactionDialog.type === 'ownerEntrust' || transactionDialog.type === 'agentApplyEntrust'">
          <el-form-item v-if="transactionDialog.type === 'ownerEntrust'" label="中介用户ID" required>
            <el-input v-model="ownerEntrust.agentId" placeholder="中介用户ID" />
          </el-form-item>
          <el-form-item label="服务范围" required>
            <el-input
              v-model="entrustScopeModel"
              type="textarea"
              :rows="3"
              resize="none"
              placeholder="发布、预约、带看、签约等"
            />
          </el-form-item>
          <el-form-item label="佣金比例">
            <el-input-number v-model="entrustCommissionModel" :min="0" :step="0.01" controls-position="right" />
          </el-form-item>
        </template>

        <template v-if="transactionDialog.type === 'agentFollowIntention'">
          <el-form-item label="意向等级" required>
            <el-select v-model="agentFollow.intentionLevel" placeholder="选择意向等级">
              <el-option label="高" value="3" />
              <el-option label="中" value="2" />
              <el-option label="低" value="1" />
            </el-select>
          </el-form-item>
          <el-form-item label="跟进备注">
            <el-input v-model="agentFollow.note" type="textarea" :rows="3" resize="none" placeholder="沟通结果、下一步安排" />
          </el-form-item>
        </template>

        <template v-if="transactionMeta.form === 'reason'">
          <el-form-item :label="transactionMeta.reasonLabel || '处理原因'">
            <el-input v-model="actionReason" type="textarea" :rows="3" resize="none" placeholder="填写原因或处理备注" />
          </el-form-item>
        </template>

        <template v-if="transactionMeta.form === 'audit'">
          <div class="audit-summary">
            <span>审核对象</span>
            <strong>{{ recordTitle(transactionDialog.row || selected) }}</strong>
            <p>{{ recordSubtitle(transactionDialog.row || selected) }}</p>
          </div>
          <el-form-item label="审核意见">
            <el-input
              v-model="auditForm.auditReason"
              type="textarea"
              :rows="4"
              resize="none"
              placeholder="记录合规性判断、驳回原因或补充说明"
            />
          </el-form-item>
        </template>

        <template v-if="transactionMeta.form === 'opinion'">
          <el-form-item label="合同意见">
            <el-input v-model="contractOpinion" type="textarea" :rows="3" resize="none" placeholder="确认或拒绝前填写处理意见" />
          </el-form-item>
        </template>
      </el-form>

      <template #footer>
        <el-button :loading="transactionDialog.aiLoading" @click="askAiForTransaction">AI 辅助</el-button>
        <el-button @click="transactionDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="transactionDialog.submitting" @click="submitTransaction">
          {{ transactionMeta.submitText }}
        </el-button>
      </template>
    </el-dialog>

    <div class="ai-quick-rail" :class="{ 'is-open': floatingAiOpen }">
      <button class="rail-button" title="业务工具" @click="switchPage('business')">
        <el-icon><Briefcase /></el-icon>
      </button>
      <button class="rail-button" title="消息中心" @click="openMessagePanel">
        <el-icon><Headset /></el-icon>
      </button>
      <button class="rail-button ai-entry" title="AI 助手" @click="openFloatingAi">
        <el-icon><Promotion /></el-icon>
      </button>
      <button class="rail-button" title="使用指南">
        <el-icon><Document /></el-icon>
      </button>
    </div>

    <section
      v-if="floatingAiOpen"
      class="ai-float-modal"
      :style="{ left: `${floatingAiPosition.x}px`, top: `${floatingAiPosition.y}px` }"
    >
      <header class="ai-float-head" @pointerdown="startFloatingAiDrag">
        <div>
          <strong>Smart Agent</strong>
          <span>业务智能体 · 可拖动</span>
        </div>
        <div class="ai-float-actions">
          <button title="归位" @click.stop="resetFloatingAiPosition">
            <el-icon><Aim /></el-icon>
          </button>
          <button title="收起" @click.stop="floatingAiOpen = false">
            <el-icon><Close /></el-icon>
          </button>
        </div>
      </header>

      <div ref="floatingAiListRef" class="ai-float-body">
        <div
          v-for="(item, index) in floatingAiMessages"
          :key="index"
          :class="['ai-float-message', item.role]"
        >
          <div class="message-content">{{ item.content }}</div>
          <div v-if="item.intentLabel || item.toolCalls?.length" class="ai-trace compact">
            <span v-if="item.intentLabel">{{ item.intentLabel }}</span>
            <span v-for="tool in item.toolCalls" :key="`${index}-${tool.name}`">{{ tool.label || tool.name }}</span>
          </div>
        </div>
      </div>

      <div class="ai-float-presets">
        <button v-for="item in floatingAiPresets" :key="item" @click="floatingAiInput = item">
          {{ item }}
        </button>
      </div>

      <footer class="ai-float-composer">
        <el-input
          v-model="floatingAiInput"
          type="textarea"
          :autosize="{ minRows: 1, maxRows: 4 }"
          resize="none"
          placeholder="问 AI 助手，Ctrl + Enter 发送"
          @keydown.ctrl.enter.prevent="sendFloatingAiMessage"
        />
        <el-button circle type="primary" icon="Promotion" :loading="floatingAiLoading" @click="sendFloatingAiMessage" />
      </footer>
    </section>
  </div>
</template>

<script setup name="PortalHome">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Aim, ArrowDown, Briefcase, Close, Document, Headset, Promotion, Search } from '@element-plus/icons-vue'
import useUserStore from '@/store/modules/user'
import BusinessChatDrawer from './components/BusinessChatDrawer.vue'
import BusinessChatPanel from './components/BusinessChatPanel.vue'
import {
  listTenantHouses,
  getTenantHouse,
  listTenantFavorites,
  favoriteTenantHouse,
  cancelTenantFavorite,
  listTenantAppointments,
  createTenantAppointment,
  cancelTenantAppointment,
  listTenantIntentions,
  createTenantIntention,
  abandonTenantIntention,
  applyTenantDeal,
  listTenantContracts
} from '@/api/portal/tenant'
import {
  listOwnerHouses,
  getOwnerHouse,
  createOwnerHouse,
  submitOwnerHouseAudit,
  listOwnerEntrusts,
  listOwnerEntrustApplications,
  entrustOwnerHouse,
  confirmOwnerEntrust,
  rejectOwnerEntrust
} from '@/api/portal/owner'
import {
  listAgentEntrusts,
  listAgentCandidateHouses,
  applyAgentEntrust,
  confirmAgentEntrust,
  rejectAgentEntrust,
  listAgentHouses,
  getAgentHouse,
  listAgentAppointments,
  listAgentIntentions,
  confirmAgentAppointment,
  rejectAgentAppointment,
  completeAgentAppointment,
  followAgentIntention,
  invalidAgentIntention,
  dealAgentIntention,
  dealAgentHouse
} from '@/api/portal/agent'
import {
  listMyContracts,
  getContractDetail,
  submitContractSign,
  confirmTenantContract,
  confirmOwnerContract,
  confirmAgentContract,
  rejectContract,
  activateContract,
  voidContract,
  terminateContract,
  openContractChat
} from '@/api/portal/contractBusiness'
import {
  sendPortalAiChat,
  recommendRentalHouses,
  getAiCapabilities,
  createAiIndexTask,
  listAiIndexTasks,
  processAiIndexTask,
  processPendingAiIndexTasks,
  inspectAiHouseDocument,
  indexKnowledgeDocument,
  seedKnowledgeDocuments
} from '@/api/portal/ai'
import { getPortalHomeSummary } from '@/api/portal/home'
import { listPortalHouses, getPortalHouseDetail } from '@/api/portal/house'
import { auditHouse, listAuditHouse } from '@/api/system/houseBusiness'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()
const pageMode = ref('business')
const loading = ref(false)
const records = ref([])
const selected = ref(null)
const detailOpen = ref(false)
const chatOpen = ref(false)
const messageOpen = ref(false)
const aiConsoleOpen = ref(false)
const messagePanelRef = ref(null)
const filters = reactive({ keyword: '', status: '' })
const chatContext = reactive({ bizType: '', bizId: '', sessionId: '', title: '' })
const transactionDialog = reactive({ visible: false, type: '', submitting: false, aiLoading: false, row: null })
const tenantAppointment = reactive({ appointmentTime: '', remark: '' })
const tenantIntention = reactive({ intentionLevel: '2', note: '' })
const ownerHouse = reactive({
  title: '',
  city: '',
  district: '',
  address: '',
  rentAmount: null,
  area: null,
  operationMode: '0',
  imageUrls: '',
  description: ''
})
const ownerEntrust = reactive({ agentId: '', entrustScope: '发布,预约,带看,签约', commissionRate: 0.02 })
const agentApply = reactive({ entrustScope: '发布,预约,带看,签约', commissionRate: 0.02 })
const contractOpinion = ref('')
const actionReason = ref('')
const dealForm = reactive({
  tenantId: '',
  startDate: '',
  endDate: '',
  rentAmount: null,
  depositAmount: null,
  paymentCycle: '月付',
  contractContent: ''
})
const agentFollow = reactive({ intentionLevel: '2', note: '' })
const auditForm = reactive({ auditReason: '' })
const workspaceSummary = reactive({ houseCount: 0, avgRent: 0, pricedHouseCount: 0, indexedHouseCount: 0 })
const aiIndexTasks = ref([])
const aiRecommend = reactive({ query: '', city: '北京', maxRent: null })
const aiRecommendAnswer = ref('')
const aiHouseDocument = ref(null)
const knowledgeSubmitting = ref(false)
const knowledgeSeeding = ref(false)
const knowledgeForm = reactive({
  sourceType: 'faq',
  title: '',
  roles: ['tenant'],
  content: ''
})
const aiCapabilities = reactive({
  online: false,
  model: '',
  embeddingMode: '',
  vectorStore: '',
  indexedKnowledge: 0,
  knowledgeSources: [],
  tools: [],
  skills: []
})
const agentInput = ref('')
const agentLoading = ref(false)
const agentMessages = ref([])
const floatingAiOpen = ref(false)
const floatingAiLoading = ref(false)
const floatingAiInput = ref('')
const floatingAiListRef = ref(null)
const floatingAiPosition = reactive({ x: 0, y: 0 })
const floatingAiDrag = reactive({ active: false, offsetX: 0, offsetY: 0 })
const floatingAiMessages = ref([
  { role: 'assistant', content: '我是右下角 AI 助手，可以随时协助找房推荐、合同摘要、房源文案和业务跟进。' }
])

const transactionConfigs = {
  ownerCreateHouse: { title: '新建房源', submitText: '提交审核' },
  ownerSubmitAudit: { title: '重新提交审核', submitText: '重新提交', form: 'none' },
  tenantAppointment: { title: '预约看房', submitText: '提交预约' },
  tenantIntention: { title: '提交租赁意向', submitText: '提交意向' },
  tenantDeal: { title: '成交申请', submitText: '提交申请', form: 'deal' },
  ownerEntrust: { title: '发起委托', submitText: '发送邀请' },
  agentApplyEntrust: { title: '申请承接', submitText: '提交申请' },
  agentConfirmAppointment: { title: '确认预约', submitText: '确认预约', form: 'none' },
  agentCompleteAppointment: { title: '完成看房', submitText: '完成看房', form: 'none' },
  agentRejectAppointment: { title: '拒绝预约', submitText: '拒绝预约', form: 'reason', reasonLabel: '拒绝原因' },
  agentFollowIntention: { title: '跟进意向', submitText: '保存跟进' },
  agentInvalidIntention: { title: '标记意向无效', submitText: '标记无效', form: 'reason', reasonLabel: '无效原因' },
  agentDealIntention: { title: '意向转成交', submitText: '确认成交', form: 'deal' },
  agentDealHouse: { title: '确认房源成交', submitText: '确认成交', form: 'deal' },
  contractSubmitSign: { title: '提交合同签署', submitText: '提交签署', form: 'none' },
  contractTenantConfirm: { title: '租户确认合同', submitText: '确认合同', form: 'opinion' },
  contractOwnerConfirm: { title: '户主确认合同', submitText: '确认合同', form: 'opinion' },
  contractAgentConfirm: { title: '中介确认合同', submitText: '确认合同', form: 'opinion' },
  contractReject: { title: '拒绝合同', submitText: '拒绝合同', form: 'opinion' },
  contractActivate: { title: '合同生效', submitText: '确认生效', form: 'none' },
  contractVoid: { title: '作废合同', submitText: '作废合同', form: 'reason', reasonLabel: '作废原因' },
  contractTerminate: { title: '终止合同', submitText: '终止合同', form: 'reason', reasonLabel: '终止原因' },
  contractOpinion: { title: '合同确认', submitText: '提交', form: 'opinion' },
  adminApproveHouse: { title: '房源合规审核', submitText: '通过并发布', form: 'audit' },
  adminRejectHouse: { title: '驳回房源', submitText: '确认驳回', form: 'audit' }
}

const roleConfig = {
  admin: { title: '房源合规审核', eyebrow: '审核队列', actionTitle: '审核动作', actionHint: '审核员只处理房源合规性：通过后发布，驳回后记录原因。' },
  tenant: { title: '租户找房', eyebrow: '推荐房源', actionTitle: '求租动作', actionHint: '收藏、预约、提交意向都在这里完成。' },
  owner: { title: '户主委托', eyebrow: '我的房源', actionTitle: '委托动作', actionHint: '处理中介申请，或指定中介发起委托。' },
  agent: { title: '中介拓客', eyebrow: '可承接房源', actionTitle: '承接动作', actionHint: '响应户主委托，也可以主动申请承接。' },
  contract: { title: '合同协作', eyebrow: '合同队列', actionTitle: '合同动作', actionHint: '推进三方确认，拒绝时请填写处理意见。' }
}

const modeMeta = {
  admin: { label: '合规审核', shortLabel: '审核' },
  tenant: { label: '租户找房', shortLabel: '找房' },
  owner: { label: '户主房源', shortLabel: '房源' },
  agent: { label: '中介业务', shortLabel: '业务' },
  contract: { label: '合同协作', shortLabel: '合同' }
}

const quickTags = ['整租', '可沟通', '待处理', '合同', '中介申请', '推荐房源']
const floatingAiPresets = ['总结当前业务', '生成房源文案', '梳理合同风险']

const roles = computed(() => userStore.roles || [])
const isAdmin = computed(() => roles.value.includes('auditor'))
const workMode = ref(defaultWorkMode())
const displayUserName = computed(() => userStore.nickName || userStore.name || '用户')
const userInitial = computed(() => displayUserName.value.slice(0, 1).toUpperCase())
const roleLabel = computed(() => {
  if (isAdmin.value) return '审核员 · 房源合规审核'
  const labels = roleModesFromRoles().map(value => modeMeta[value]?.label).filter(Boolean)
  return labels.length ? labels.join(' / ') : friendlyRoleText.value
})
const friendlyRoleText = computed(() => {
  const labels = {
    user: '租户',
    tenant: '租户',
    owner: '户主',
    agent: '中介',
    auditor: '审核员',
    admin: '超级管理员'
  }
  const mapped = roles.value.map(role => labels[role] || role).filter(Boolean)
  return mapped.length ? mapped.join(' / ') : '未识别角色'
})
const primaryActionLabel = computed(() => {
  if (isAdmin.value) return '审核队列'
  if (roles.value.includes('owner')) return '新建房源'
  if (roles.value.includes('agent')) return '可承接房源'
  return '我的预约'
})
const currentRoleConfig = computed(() => roleConfig[workMode.value] || roleConfig.tenant)
const contractConfirms = computed(() => selected.value?.confirms || [])
const roleModeOptions = computed(() => visibleWorkModes.value.map(value => ({ value, ...modeMeta[value] })))
const canAuditSelectedHouse = computed(() => workMode.value === 'admin' && selected.value?.houseId && String(selected.value.status) === '1')
const canManageAiIndex = computed(() => roles.value.some(role => ['owner', 'agent', 'auditor'].includes(role)))
const canRunFullAiSync = computed(() => roles.value.includes('auditor'))
const canOpenAiConsole = computed(() => roles.value.includes('auditor'))
const canManageContractLifecycle = computed(() => roles.value.includes('owner') || roles.value.includes('agent'))
const transactionMeta = computed(() => transactionConfigs[transactionDialog.type] || { title: '业务处理', submitText: '提交', form: 'none' })
const latestAiTask = computed(() => aiIndexTasks.value[0] || null)
const detailDrawerTitle = computed(() => selected.value ? `${recordTypeLabel(selected.value)}详情` : '详情')
const businessAiCard = computed(() => {
  const cards = {
    admin: {
      title: 'AI 辅助合规审查',
      description: '结合房源字段生成审核要点，结果只写入审核意见，最终通过或驳回仍由审核员确认。',
      actionLabel: '生成审核意见'
    },
    tenant: {
      title: 'AI 推荐房源',
      description: '按预算、位置和当前房源上下文生成推荐摘要，房源详情仍从业务接口读取。',
      actionLabel: '生成推荐'
    },
    owner: {
      title: 'AI 房源文案',
      description: '根据当前房源生成发布描述或委托沟通话术，创建和提交仍通过业务表单完成。',
      actionLabel: '生成文案'
    },
    agent: {
      title: 'AI 跟进建议',
      description: '根据当前委托、预约或意向生成下一步跟进话术，不自动变更业务状态。',
      actionLabel: '生成建议'
    },
    contract: {
      title: 'AI 合同风险提示',
      description: '提取合同风险点和确认意见草稿，签署、拒绝、生效仍走合同接口。',
      actionLabel: '梳理风险'
    }
  }
  return cards[workMode.value] || null
})
const aiAssistantProfile = computed(() => {
  const profiles = {
    tenant: {
      badge: '租户 AI 助手',
      eyebrow: '找房协作',
      title: '租户智能找房助手',
      summary: '围绕预算、通勤、户型和合同风险提供建议，帮你更快做出看房和意向决策。',
      placeholder: '例如：预算 4500，想找通勤方便的两居，顺便看看当前房源值不值得约看',
      presets: ['按预算推荐房源', '分析当前房源是否值得预约', '梳理合同风险'],
      capabilities: ['预算匹配', '看房建议', '合同风险', '沟通问题清单']
    },
    owner: {
      badge: '户主 AI 助手',
      eyebrow: '发布协作',
      title: '户主房源经营助手',
      summary: '围绕房源发布、委托中介、审核补充和合同协作，帮你把业务动作做得更完整。',
      placeholder: '例如：帮我润色当前房源文案，或者告诉我这套房源还缺哪些审核信息',
      presets: ['润色当前房源文案', '检查当前房源审核缺口', '生成委托沟通话术'],
      capabilities: ['房源文案', '审核补充', '委托沟通', '合同确认']
    },
    agent: {
      badge: '中介 AI 助手',
      eyebrow: '成交推进',
      title: '中介业务跟进助手',
      summary: '围绕可承接房源、客户预约、意向推进和签约沟通，给你更贴近业务的下一步建议。',
      placeholder: '例如：帮我给当前客户生成跟进话术，或者判断这条意向下一步该怎么推进',
      presets: ['生成当前业务跟进话术', '总结当前客户下一步动作', '梳理合同签约风险'],
      capabilities: ['跟进话术', '预约推进', '意向转化', '签约提醒']
    },
    contract: {
      badge: '合同 AI 助手',
      eyebrow: '签约协作',
      title: '合同确认与风险助手',
      summary: '聚焦租期、押金、付款周期、交付清单和违约责任，帮你在确认前看清主要风险。',
      placeholder: '例如：帮我看这份合同还缺什么，或者当前合同应该提醒双方注意哪些点',
      presets: ['梳理当前合同风险', '生成合同确认意见', '总结当前合同下一步动作'],
      capabilities: ['风险提炼', '确认意见', '交付检查', '违约提醒']
    },
    admin: {
      badge: '审核 AI 助手',
      eyebrow: '合规辅助',
      title: '审核意见辅助助手',
      summary: '只为审核员服务，聚焦房源字段完整性、风险词、审核意见草稿和索引任务提醒。',
      placeholder: '例如：请结合当前房源生成审核意见，并指出必须补充的字段',
      presets: ['生成当前房源审核意见', '检查当前房源硬性问题', '说明索引状态与处理建议'],
      capabilities: ['字段审查', '风险提示', '审核意见', '索引提醒']
    }
  }
  return profiles[workMode.value] || profiles.tenant
})
const agentPresets = computed(() => aiAssistantProfile.value.presets)
const aiCapabilityChips = computed(() => {
  const chips = []
  if (aiCapabilities.embeddingMode) {
    chips.push(aiCapabilities.embeddingMode === 'remote' ? '远程向量' : '本地向量')
  }
  if (aiCapabilities.vectorStore) {
    chips.push(aiCapabilities.vectorStore)
  }
  if (aiCapabilities.model) {
    chips.push(aiCapabilities.model)
  }
  return chips
})
const knowledgeSourceText = computed(() => {
  const labels = {
    house: '房源',
    contract: '合同',
    policy: '政策',
    faq: 'FAQ',
    chat: '聊天记录',
    enterprise: '企业制度'
  }
  const sources = aiCapabilities.knowledgeSources || []
  if (!sources.length) return '合同、政策、FAQ、聊天记录、企业制度'
  return sources.map(item => labels[item] || item).join('、')
})
const previewFields = computed(() => detailFields.value.slice(0, 6))
const detailImageUrls = computed(() => {
  const raw = selected.value?.imageUrls || selected.value?.images || selected.value?.imageList
  if (Array.isArray(raw)) {
    return raw.map(item => typeof item === 'string' ? item : item?.imageUrl).filter(Boolean)
  }
  if (typeof raw === 'string' && raw.trim()) {
    return raw.split(',').map(item => item.trim()).filter(Boolean)
  }
  return []
})
const entrustScopeModel = computed({
  get: () => transactionDialog.type === 'agentApplyEntrust' ? agentApply.entrustScope : ownerEntrust.entrustScope,
  set: value => {
    if (transactionDialog.type === 'agentApplyEntrust') {
      agentApply.entrustScope = value
    } else {
      ownerEntrust.entrustScope = value
    }
  }
})
const entrustCommissionModel = computed({
  get: () => transactionDialog.type === 'agentApplyEntrust' ? agentApply.commissionRate : ownerEntrust.commissionRate,
  set: value => {
    if (transactionDialog.type === 'agentApplyEntrust') {
      agentApply.commissionRate = value
    } else {
      ownerEntrust.commissionRate = value
    }
  }
})
const emptyDescription = computed(() => {
  const map = {
    admin: '暂无待审核房源',
    tenant: '暂无推荐房源，可以刷新或查看公开房源',
    owner: '还没有房源，先创建一套房源',
    agent: '暂无业务记录，可以查看可承接房源',
    contract: '暂无合同记录'
  }
  return map[workMode.value] || '暂无数据'
})

const visibleWorkModes = computed(() => {
  if (isAdmin.value) return ['admin']
  return roleModesFromRoles()
})

const statusOptions = computed(() => {
  if (workMode.value === 'admin') {
    return [{ value: '1', label: '待合规审核' }]
  }
  const seen = new Map()
  records.value.forEach(item => {
    const status = recordStatus(item)
    seen.set(String(status), recordStatusLabel(item))
  })
  return Array.from(seen.entries()).map(([value, label]) => ({ value, label }))
})

const filteredRecords = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  return records.value.filter(item => {
    if (filters.status && String(recordStatus(item)) !== String(filters.status)) return false
    if (!keyword) return true
    return searchableValues(item).some(value => String(value || '').toLowerCase().includes(keyword))
  })
})

const detailFields = computed(() => {
  if (!selected.value) return []
  if (workMode.value === 'admin' && selected.value.houseId) {
    return [
      { key: 'houseId', label: '房源ID' },
      { key: 'ownerId', label: '户主ID' },
      { key: 'city', label: '城市' },
      { key: 'district', label: '区域' },
      { key: 'address', label: '详细地址' },
      { key: 'rentAmount', label: '租金', formatter: formatMoney },
      { key: 'area', label: '面积', formatter: value => value ? `${value} 平米` : '-' },
      { key: 'auditStatus', label: '审核状态', formatter: auditStatusLabel },
      { key: 'auditReason', label: '审核意见' },
      { key: 'createTime', label: '提交时间' }
    ]
  }
  if (selected.value.appointmentId) {
    return [
      { key: 'appointmentId', label: '预约ID' },
      { key: 'houseId', label: '房源ID' },
      { key: 'tenantId', label: '租户ID' },
      { key: 'ownerId', label: '户主ID' },
      { key: 'agentId', label: '中介ID' },
      { key: 'appointmentTime', label: '预约时间' }
    ]
  }
  if (selected.value.intentionId) {
    return [
      { key: 'intentionId', label: '意向ID' },
      { key: 'houseId', label: '房源ID' },
      { key: 'tenantId', label: '租户ID' },
      { key: 'ownerId', label: '户主ID' },
      { key: 'agentId', label: '中介ID' },
      { key: 'intentionLevel', label: '意向等级', formatter: intentionLevelLabel }
    ]
  }
  if (selected.value.houseId && !selected.value.entrustId && !selected.value.contractId && !selected.value.appointmentId && !selected.value.intentionId) {
    return [
      { key: 'houseId', label: '房源ID' },
      { key: 'city', label: '城市' },
      { key: 'district', label: '区域' },
      { key: 'rentAmount', label: '租金', formatter: formatMoney },
      { key: 'area', label: '面积', formatter: value => value ? `${value} 平米` : '-' },
      { key: 'ownerId', label: '户主ID' },
      { key: 'agentId', label: '中介ID' }
    ]
  }
  if (selected.value.entrustId) {
    return [
      { key: 'entrustId', label: '委托ID' },
      { key: 'houseId', label: '房源ID' },
      { key: 'ownerId', label: '户主ID' },
      { key: 'agentId', label: '中介ID' },
      { key: 'entrustScope', label: '委托范围' },
      { key: 'commissionRate', label: '佣金比例', formatter: formatPercent }
    ]
  }
  return [
    { key: 'contractId', label: '合同ID' },
    { key: 'houseId', label: '房源ID' },
    { key: 'tenantId', label: '租户ID' },
    { key: 'ownerId', label: '户主ID' },
    { key: 'agentId', label: '中介ID' },
    { key: 'rentAmount', label: '租金', formatter: formatMoney }
  ]
})

const processSteps = computed(() => {
  const item = selected.value || {}
  if (workMode.value === 'admin') {
    return [
      { label: '户主提交', done: Boolean(item.houseId) },
      { label: '待合规审核', done: String(item.status) === '1' || String(item.auditStatus) === '1' },
      { label: '审核结论', done: ['2', '3'].includes(String(item.auditStatus)) || ['2', '3'].includes(String(item.status)) },
      { label: '发布或驳回', done: ['2', '3'].includes(String(item.status)) }
    ]
  }
  if (workMode.value === 'tenant') {
    return [
      { label: '浏览房源', done: Boolean(item.houseId) },
      { label: '收藏或预约', done: Boolean(item.favoriteId || item.appointmentId || item.intentionId) },
      { label: '业务沟通', done: Boolean(chatTargetFor(item)) },
      { label: '合同签署', done: Boolean(item.contractId) }
    ]
  }
  if (workMode.value === 'owner') {
    return [
      { label: '房源建档', done: Boolean(item.houseId) },
      { label: '委托中介', done: Boolean(item.entrustId || item.agentId) },
      { label: '确认协作', done: isConfirmedStatus(recordStatus(item)) },
      { label: '推进签约', done: Boolean(item.contractId) }
    ]
  }
  if (workMode.value === 'agent') {
    return [
      { label: '发现房源', done: Boolean(item.houseId) },
      { label: '申请或接收委托', done: Boolean(item.entrustId) },
      { label: '跟进客户', done: Boolean(item.appointmentId || item.intentionId) },
      { label: '促成合同', done: Boolean(item.contractId) }
    ]
  }
  return [
    { label: '合同创建', done: Boolean(item.contractId) },
    { label: '提交签署', done: Number(recordStatus(item)) >= 1 },
    { label: '三方确认', done: isConfirmedStatus(recordStatus(item)) },
    { label: '归档生效', done: Number(recordStatus(item)) >= 4 }
  ]
})

const visibleActions = computed(() => {
  if (!selected.value) return []
  if (workMode.value === 'admin') {
    return [
      { key: 'approve', label: '通过发布', type: 'success', icon: 'Select', handler: row => openTransaction('adminApproveHouse', row) },
      { key: 'reject', label: '驳回', type: 'danger', icon: 'Close', handler: row => openTransaction('adminRejectHouse', row) }
    ].map(action => ({
      ...action,
      disabled: !canAuditSelectedHouse.value
    }))
  }
  if (workMode.value === 'tenant') {
    return isTenantHouseContext(selected.value)
      ? [{ key: 'favorite', label: '收藏', type: 'primary', icon: 'Star', handler: favoriteSelectedHouse }]
      : []
  }
  if (workMode.value === 'owner' && selected.value.entrustId) {
    return [
      { key: 'confirm', label: '确认中介', type: 'success', icon: 'Select', handler: confirmOwnerApplication },
      { key: 'reject', label: '拒绝', type: 'danger', icon: 'Close', handler: rejectOwnerApplication }
    ]
  }
  if (workMode.value === 'agent' && selected.value.entrustId) {
    return [
      { key: 'confirm', label: '接受委托', type: 'success', icon: 'Select', handler: confirmAgentInvite },
      { key: 'reject', label: '拒绝', type: 'danger', icon: 'Close', handler: rejectAgentInvite }
    ]
  }
  if (workMode.value === 'contract') {
    const actions = []
    if (roles.value.includes('owner') || roles.value.includes('agent')) {
      actions.push({ key: 'submit', label: '提交签署', type: 'primary', icon: 'Upload', handler: row => openTransaction('contractSubmitSign', row) })
    }
    if (roles.value.includes('tenant') || roles.value.includes('user')) {
      actions.push({ key: 'tenant', label: '租户确认', handler: row => openTransaction('contractTenantConfirm', row) })
    }
    if (roles.value.includes('owner')) {
      actions.push({ key: 'owner', label: '户主确认', handler: row => openTransaction('contractOwnerConfirm', row) })
    }
    if (roles.value.includes('agent')) {
      actions.push({ key: 'agent', label: '中介确认', handler: row => openTransaction('contractAgentConfirm', row) })
    }
    actions.push({ key: 'reject', label: '拒绝合同', type: 'danger', icon: 'Close', handler: row => openTransaction('contractReject', row) })
    return actions
  }
  return []
})

const chatTarget = computed(() => chatTargetFor(selected.value))

watch(workMode, refreshMode)
watch(filteredRecords, list => {
  if (!list.some(item => recordKey(item) === recordKey(selected.value))) {
    selected.value = list[0] || null
  }
})

onMounted(() => {
  if (isAdmin.value) {
    pageMode.value = 'business'
  }
  refreshMode()
  resetFloatingAiPosition()
  loadPortalSummary()
  loadAiCapabilities()
  if (canManageAiIndex.value) {
    loadAiIndexTasks()
  }
  seedAgentWelcome()
  if (route.query?.ai === 'console' && canOpenAiConsole.value) {
    nextTick(openAiConsole)
  }
})

onBeforeUnmount(() => {
  stopFloatingAiDrag()
})

function defaultWorkMode() {
  if (roles.value.includes('auditor')) return 'admin'
  if (roles.value.includes('agent')) return 'agent'
  if (roles.value.includes('owner')) return 'owner'
  return 'tenant'
}

function roleModesFromRoles() {
  const modes = []
  if (roles.value.includes('tenant') || roles.value.includes('user')) modes.push('tenant')
  if (roles.value.includes('owner')) modes.push('owner')
  if (roles.value.includes('agent')) modes.push('agent')
  if (modes.length || roles.value.some(role => ['tenant', 'owner', 'agent', 'user'].includes(role))) {
    modes.push('contract')
  }
  return modes.length ? Array.from(new Set(modes)) : ['tenant', 'contract']
}

function switchPage(mode) {
  pageMode.value = mode
  if (mode === 'agent' && agentMessages.value.length <= 1) {
    seedAgentWelcome()
  }
}

async function handleUserCommand(command) {
  if (command === 'profile') {
    router.push('/user/profile')
    return
  }
  if (command === 'password') {
    router.push({ name: 'Profile', params: { activeTab: 'resetPwd' } })
    return
  }
  if (command === 'message') {
    openMessagePanel()
    return
  }
  if (command === 'agent' || command === 'business') {
    switchPage(command)
    return
  }
  if (command === 'primaryAction') {
    await runPrimaryUserAction()
    return
  }
  if (command === 'contracts') {
    await openContractsShortcut()
    return
  }
  if (command === 'aiConsole') {
    await openAiConsole()
    return
  }
  if (command === 'logout') {
    await logoutFromPortal()
  }
}

async function runPrimaryUserAction() {
  pageMode.value = 'business'
  if (isAdmin.value) {
    workMode.value = 'admin'
    await refreshMode()
    return
  }
  if (roles.value.includes('owner')) {
    workMode.value = 'owner'
    openTransaction('ownerCreateHouse')
    return
  }
  if (roles.value.includes('agent')) {
    workMode.value = 'agent'
    await loadAgentCandidateWorkspace()
    return
  }
  workMode.value = 'tenant'
  await loadTenantAppointmentsFromWorkspace()
}

async function openContractsShortcut() {
  pageMode.value = 'business'
  if (!visibleWorkModes.value.includes('contract')) {
    ElMessage.warning('当前角色暂无合同入口')
    return
  }
  workMode.value = 'contract'
  await loadContractRecordsToWorkspace()
}

async function logoutFromPortal() {
  try {
    await ElMessageBox.confirm('确定注销并退出系统吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await userStore.logOut()
    location.href = '/index'
  } catch (error) {
    // 用户取消退出时不打扰当前业务操作。
  }
}

async function openAiConsole() {
  if (!canOpenAiConsole.value) return
  aiConsoleOpen.value = true
  await refreshAiConsole()
}

async function refreshAiConsole() {
  await loadPortalSummary()
  await loadAiCapabilities()
  await loadAiIndexTasks()
  if (selected.value?.houseId) {
    await inspectAiHouseDocumentFromSelected()
  }
}

function openMessagePanel() {
  messageOpen.value = true
}

function prepareMessagePanel() {
  messagePanelRef.value?.prepareSession()
}

async function refreshCurrent() {
  if (pageMode.value === 'business') {
    await refreshMode()
    await loadPortalSummary()
    await loadAiCapabilities()
    if (canManageAiIndex.value) {
      await loadAiIndexTasks()
    }
  }
}

async function refreshMode() {
  if (!visibleWorkModes.value.includes(workMode.value)) {
    workMode.value = visibleWorkModes.value[0]
    return
  }
  loading.value = true
  try {
    const loaders = {
      admin: loadAdminRecords,
      tenant: loadTenantRecords,
      owner: loadOwnerRecords,
      agent: loadAgentRecords,
      contract: loadContractRecords
    }
    records.value = await loaders[workMode.value]()
    selected.value = filteredRecords.value[0] || records.value[0] || null
  } finally {
    loading.value = false
  }
}

async function loadTenantRecords() {
  const res = await listTenantHouses({})
  return rowsOf(res).map(item => ({ ...item, _recordType: '可租房源' }))
}

async function loadAdminRecords() {
  const res = await listAuditHouse({})
  return rowsOf(res)
    .map(item => ({ ...item, _recordType: '合规审核' }))
    .sort((left, right) => {
      const leftPending = String(left.status) === '1' ? 0 : 1
      const rightPending = String(right.status) === '1' ? 0 : 1
      return leftPending - rightPending
    })
}

async function loadOwnerRecords() {
  const [houses, applications, entrusts] = await Promise.all([
    listOwnerHouses({}),
    listOwnerEntrustApplications({}),
    listOwnerEntrusts({})
  ])
  return [
    ...rowsOf(applications).map(item => ({ ...item, _recordType: '中介申请' })),
    ...rowsOf(entrusts).map(item => ({ ...item, _recordType: '历史委托' })),
    ...rowsOf(houses).map(item => ({ ...item, _recordType: '我的房源' }))
  ]
}

async function loadAgentRecords() {
  const [invites, candidates, houses, appointments, intentions] = await Promise.all([
    listAgentEntrusts({}),
    listAgentCandidateHouses({}),
    listAgentHouses({}),
    listAgentAppointments({}),
    listAgentIntentions({})
  ])
  return [
    ...rowsOf(invites).map(item => ({ ...item, _recordType: '委托邀请' })),
    ...rowsOf(houses).map(item => ({ ...item, _recordType: '受托房源' })),
    ...rowsOf(appointments).map(item => ({ ...item, _recordType: '看房预约' })),
    ...rowsOf(intentions).map(item => ({ ...item, _recordType: '租户意向' })),
    ...rowsOf(candidates).map(item => ({ ...item, _recordType: '可申请房源' }))
  ]
}

async function loadContractRecords() {
  const res = await listMyContracts({})
  return rowsOf(res).map(item => ({ ...item, _recordType: '租赁合同' }))
}

async function loadContractRecordsToWorkspace() {
  const rows = await loadContractRecords()
  records.value = rows
  selected.value = rows[0] || null
}

function rowsOf(response) {
  return response?.rows || response?.data || []
}

function selectRecord(item) {
  selected.value = item
}

async function openSelectedDetail() {
  if (!selected.value) return
  await hydrateSelectedDetail()
  detailOpen.value = true
}

async function hydrateSelectedDetail() {
  if (!selected.value) return
  if (selected.value.contractId) {
    const res = await getContractDetail(selected.value.contractId)
    selected.value = { ...(res?.data || {}), _recordType: '合同详情' }
    return
  }
  if (!selected.value.houseId) return
  if (workMode.value === 'owner' || selected.value._recordType?.includes('我的房源')) {
    const res = await getOwnerHouse(selected.value.houseId)
    selected.value = { ...(res?.data || {}), _recordType: '我的房源详情' }
    return
  }
  if (workMode.value === 'agent' || selected.value._recordType?.includes('受托') || selected.value._recordType?.includes('可申请')) {
    const res = await getAgentHouse(selected.value.houseId)
    selected.value = { ...(res?.data || {}), _recordType: '受托房源详情' }
    return
  }
  if (workMode.value === 'tenant') {
    const res = await getTenantHouse(selected.value.houseId)
    selected.value = { ...(res?.data || {}), _recordType: '房源详情' }
    return
  }
  const res = await getPortalHouseDetail(selected.value.houseId)
  selected.value = { ...(res?.data || {}), _recordType: '门户房源详情' }
}

function recordKey(item) {
  if (!item) return ''
  return `${item._recordType || workMode.value}-${item.entrustId || item.contractId || item.appointmentId || item.intentionId || item.houseId}`
}

function recordTypeLabel(item) {
  if (item?._recordType) return item._recordType
  if (item?.entrustId) return '委托'
  if (item?.contractId) return '合同'
  return '房源'
}

function recordTitle(item) {
  if (!item) return '-'
  return item.title || item.contractNo || `房源 ${item.houseId || '-'}`
}

function recordSubtitle(item) {
  if (!item) return ''
  const parts = []
  if (workMode.value === 'admin') parts.push(auditStatusLabel(item.auditStatus))
  if (item.city) parts.push(item.city)
  if (item.district) parts.push(item.district)
  if (item.rentAmount) parts.push(`${item.rentAmount} 元/月`)
  if (item.ownerId) parts.push(`户主 ${item.ownerId}`)
  if (item.agentId) parts.push(`中介 ${item.agentId}`)
  return parts.join(' / ') || recordIdText(item)
}

function recordIdText(item) {
  return `编号 ${item.entrustId || item.contractId || item.houseId || '-'}`
}

function priceText(item) {
  return item?.rentAmount ? `${item.rentAmount} 元/月` : '-'
}

function ownerAgentText(item) {
  if (item?.agentId) return `中介 ${item.agentId}`
  if (item?.ownerId) return `户主 ${item.ownerId}`
  return item?.city || '智能AI房屋租赁系统'
}

function recordStatus(item) {
  if (!item) return ''
  if (workMode.value === 'admin') return item.auditStatus ?? item.status ?? ''
  return item.status ?? item.auditStatus ?? item.contractStatus ?? ''
}

function recordStatusLabel(item) {
  if (!item) return '未设置'
  return statusLabel(recordStatus(item), item)
}

function auditStatusLabel(status) {
  const map = {
    0: '未提交',
    1: '待合规审核',
    2: '审核通过',
    3: '审核驳回'
  }
  return map[String(status)] || status || '未设置'
}

function statusLabel(status, item = selected.value) {
  const type = item?._recordType || ''
  const normalized = String(status ?? '')
  if (workMode.value === 'admin' || type.includes('合规审核')) return auditStatusLabel(status)
  const houseMap = {
    0: '草稿',
    1: '待审核',
    2: '已发布',
    3: '审核未通过',
    4: '已出租',
    5: '已下架'
  }
  const entrustMap = {
    0: '待确认',
    1: '生效中',
    2: '已拒绝',
    3: '已终止',
    4: '已过期'
  }
  const appointmentMap = {
    0: '待确认',
    1: '已确认',
    2: '已完成',
    3: '已取消',
    4: '已拒绝'
  }
  const intentionMap = {
    0: '跟进中',
    1: '已成交',
    2: '无效',
    3: '放弃'
  }
  const contractMap = {
    0: '草稿',
    1: '待确认/待签',
    2: '生效',
    3: '终止',
    4: '作废'
  }
  const commonMap = {
    pending: '待处理',
    active: '进行中',
    confirmed: '已确认',
    rejected: '已拒绝',
    finished: '已完成',
    draft: '草稿'
  }
  if (item?.contractId || type.includes('合同')) return contractMap[normalized] || commonMap[normalized] || status || '未设置'
  if (item?.appointmentId || type.includes('预约')) return appointmentMap[normalized] || commonMap[normalized] || status || '未设置'
  if (item?.intentionId || type.includes('意向')) return intentionMap[normalized] || commonMap[normalized] || status || '未设置'
  if (item?.entrustId || type.includes('委托') || type.includes('申请')) return entrustMap[normalized] || commonMap[normalized] || status || '未设置'
  if (item?.houseId || type.includes('房源') || type.includes('收藏')) return houseMap[normalized] || commonMap[normalized] || status || '未设置'
  const map = {
    ...commonMap,
    0: '待处理',
    1: '进行中',
    2: '已确认',
    3: '已拒绝',
    4: '已完成',
    5: '已下架'
  }
  return map[status] || status || '未设置'
}

function tagType(status, item = selected.value) {
  const type = item?._recordType || ''
  const normalized = String(status ?? '')
  if (item?.contractId || type.includes('合同')) {
    if (['0', '1'].includes(normalized)) return 'warning'
    if (['3', '4'].includes(normalized)) return 'danger'
    if (normalized === '2') return 'success'
  }
  if (item?.appointmentId || type.includes('预约')) {
    if (normalized === '0') return 'warning'
    if (['3', '4'].includes(normalized)) return 'danger'
    if (['1', '2'].includes(normalized)) return 'success'
  }
  if (item?.intentionId || type.includes('意向')) {
    if (normalized === '0') return 'warning'
    if (['2', '3'].includes(normalized)) return 'danger'
    if (normalized === '1') return 'success'
  }
  if (item?.entrustId || type.includes('委托') || type.includes('申请')) {
    if (normalized === '0') return 'warning'
    if (['2', '3', '4'].includes(normalized)) return 'danger'
    if (normalized === '1') return 'success'
  }
  if (item?.houseId || type.includes('房源') || type.includes('收藏')) {
    if (['0', '1'].includes(normalized)) return 'warning'
    if (['3', '5'].includes(normalized)) return 'danger'
    if (['2', '4'].includes(normalized)) return 'success'
  }
  if (isPendingStatus(status)) return 'warning'
  if (isRejectedStatus(status)) return 'danger'
  if (isConfirmedStatus(status)) return 'success'
  return 'info'
}

function isPendingStatus(status) {
  return ['0', 0, 'pending', 'draft'].includes(status)
}

function isRejectedStatus(status) {
  return ['3', 3, 'rejected'].includes(status)
}

function isConfirmedStatus(status) {
  return ['2', 2, '4', 4, 'confirmed', 'finished'].includes(status)
}

function searchableValues(item) {
  return [
    item.houseId,
    item.entrustId,
    item.contractId,
    item.appointmentId,
    item.intentionId,
    item.title,
    item.contractNo,
    item.city,
    item.district,
    item.ownerId,
    item.agentId,
    item.tenantId,
    item._recordType,
    recordStatusLabel(item)
  ]
}

function formatValue(value) {
  return value === undefined || value === null || value === '' ? '-' : value
}

function formatMoney(value) {
  return value === undefined || value === null || value === '' ? '-' : `${value} 元/月`
}

function formatPercent(value) {
  if (value === undefined || value === null || value === '') return '-'
  const number = Number(value)
  if (Number.isNaN(number)) return value
  return `${(number * 100).toFixed(2)}%`
}

function intentionLevelLabel(value) {
  const map = { 1: '低', 2: '中', 3: '高' }
  return map[String(value)] || value || '-'
}

function taskStatusLabel(status) {
  const map = {
    0: '待处理',
    1: '处理中',
    2: '已完成',
    3: '失败',
    pending: '待处理',
    running: '处理中',
    success: '已完成',
    failed: '失败'
  }
  return map[status] || status || '未设置'
}

function aiIndexStatusLabel(status, indexed = false) {
  if (indexed || ['1', 1].includes(status)) return '已索引'
  if (['2', 2].includes(status)) return '索引失败'
  return '未索引'
}

function chatTargetFor(item) {
  if (!item) return null
  if (item.entrustId) return { bizType: 'entrust', bizId: item.entrustId, title: '委托沟通' }
  if (item.contractId) return { bizType: 'contract', bizId: item.contractId, title: '合同沟通' }
  if (item.appointmentId) return { bizType: 'appointment', bizId: item.appointmentId, title: '预约沟通' }
  if (item.intentionId) return { bizType: 'intention', bizId: item.intentionId, title: '意向沟通' }
  return null
}

function isTenantHouseContext(item) {
  if (!item?.houseId) return false
  return !item.appointmentId && !item.intentionId && !item.contractId
}

function isOwnerHouseContext(item) {
  if (!item?.houseId) return false
  return !item.entrustId && !item.appointmentId && !item.intentionId && !item.contractId
}

function isAgentCandidateHouse(item) {
  return item?.houseId && !item.entrustId && item?._recordType === '可申请房源'
}

function isAgentEntrustedHouse(item) {
  return item?.houseId && !item.appointmentId && !item.intentionId && !item.contractId
    && ['受托房源', '受托房源详情'].includes(item?._recordType)
}

function openChat(target) {
  chatContext.bizType = target.bizType
  chatContext.bizId = target.bizId
  chatContext.sessionId = target.sessionId || ''
  chatContext.title = target.title
  chatOpen.value = true
}

function chatSessionFromResponse(response) {
  return response?.chatSession || response?.data?.chatSession || null
}

function openChatFromResponse(response, fallback = {}) {
  const session = chatSessionFromResponse(response)
  if (!session?.sessionId && !fallback.bizType) return
  openChat({
    bizType: session?.bizType || fallback.bizType,
    bizId: session?.bizId || fallback.bizId,
    sessionId: session?.sessionId || '',
    title: fallback.title || session?.title || '业务沟通'
  })
}

function openTransaction(type, row = selected.value) {
  transactionDialog.type = type
  transactionDialog.row = row || selected.value || null
  if (type === 'adminApproveHouse') {
    auditForm.auditReason = '房源信息完整，租金、地址、户主信息符合发布要求。'
  } else if (type === 'adminRejectHouse') {
    auditForm.auditReason = ''
  }
  transactionDialog.visible = true
}

async function submitTransaction() {
  transactionDialog.submitting = true
  try {
    const row = transactionDialog.row || selected.value
    const handlers = {
      ownerCreateHouse: createOwnerHouseFromForm,
      ownerSubmitAudit: submitOwnerAuditFromSelected,
      tenantAppointment: createAppointmentFromSelected,
      tenantIntention: createIntentionFromSelected,
      tenantDeal: applyTenantDealFromSelected,
      ownerEntrust: () => inviteAgentFromSelected(row),
      agentApplyEntrust: applyEntrustFromSelected,
      agentConfirmAppointment: confirmAppointmentFromSelected,
      agentCompleteAppointment: completeAppointmentFromSelected,
      agentRejectAppointment: rejectAppointmentFromSelected,
      agentFollowIntention: followIntentionFromSelected,
      agentInvalidIntention: invalidIntentionFromSelected,
      agentDealIntention: dealIntentionFromSelected,
      agentDealHouse: dealHouseFromSelected,
      contractSubmitSign: () => submitSign(row),
      contractTenantConfirm: () => tenantConfirm(row),
      contractOwnerConfirm: () => ownerConfirm(row),
      contractAgentConfirm: () => agentConfirm(row),
      contractReject: () => rejectCurrentContract(row),
      contractActivate: () => activateCurrentContract(row),
      contractVoid: () => voidCurrentContract(row),
      contractTerminate: () => terminateCurrentContract(row),
      adminApproveHouse: () => auditAdminHouse(row, '2'),
      adminRejectHouse: () => auditAdminHouse(row, '3')
    }
    const handler = handlers[transactionDialog.type]
    if (!handler) return ElMessage.warning('当前事务暂未配置提交动作')
    await handler()
    transactionDialog.visible = false
  } finally {
    transactionDialog.submitting = false
  }
}

async function askAiForTransaction() {
  if (!transactionDialog.type) return
  transactionDialog.aiLoading = true
  try {
    const prompt = `请基于当前业务，给出“${transactionMeta.value.title}”的可直接填写建议。`
    const res = await sendPortalAiChat({
      ...buildAiRequest(prompt),
      transactionType: transactionDialog.type,
      transactionTitle: transactionMeta.value.title
    })
    const aiResult = normalizeAiResponse(res)
    applyAiSuggestion(aiResult)
    ElMessage.success('AI 建议已写入当前事务表单')
  } finally {
    transactionDialog.aiLoading = false
  }
}

function applyAiSuggestion(result) {
  const aiResult = typeof result === 'string' ? { answer: result, suggestions: null } : (result || {})
  const suggestions = aiResult.suggestions || {}
  const text = String(aiResult.answer || '').trim()
  if (Object.keys(suggestions).length) {
    applyStructuredSuggestion(suggestions)
    return
  }
  if (!text) return
  if (transactionMeta.value.form === 'opinion') {
    contractOpinion.value = text
    return
  }
  if (transactionMeta.value.form === 'reason') {
    actionReason.value = text
    return
  }
  if (transactionDialog.type === 'tenantAppointment') {
    tenantAppointment.remark = text
    return
  }
  if (transactionDialog.type === 'tenantIntention') {
    tenantIntention.note = text
    return
  }
  if (transactionDialog.type === 'agentFollowIntention') {
    agentFollow.note = text
    return
  }
  if (transactionMeta.value.form === 'deal') {
    dealForm.contractContent = text
    return
  }
  if (transactionDialog.type === 'ownerEntrust' || transactionDialog.type === 'agentApplyEntrust') {
    entrustScopeModel.value = text
  }
}

function applyStructuredSuggestion(suggestions) {
  if (suggestions.title !== undefined) ownerHouse.title = suggestions.title
  if (suggestions.city !== undefined) ownerHouse.city = suggestions.city
  if (suggestions.district !== undefined) ownerHouse.district = suggestions.district
  if (suggestions.address !== undefined) ownerHouse.address = suggestions.address
  if (suggestions.rentAmount !== undefined) {
    ownerHouse.rentAmount = suggestions.rentAmount
    dealForm.rentAmount = suggestions.rentAmount
  }
  if (suggestions.area !== undefined) ownerHouse.area = suggestions.area
  if (suggestions.remark !== undefined) tenantAppointment.remark = suggestions.remark
  if (suggestions.note !== undefined) {
    tenantIntention.note = suggestions.note
    agentFollow.note = suggestions.note
  }
  if (suggestions.intentionLevel !== undefined) {
    tenantIntention.intentionLevel = String(suggestions.intentionLevel)
    agentFollow.intentionLevel = String(suggestions.intentionLevel)
  }
  if (suggestions.depositAmount !== undefined) dealForm.depositAmount = suggestions.depositAmount
  if (suggestions.paymentCycle !== undefined) dealForm.paymentCycle = suggestions.paymentCycle
  if (suggestions.contractContent !== undefined) dealForm.contractContent = suggestions.contractContent
  if (suggestions.entrustScope !== undefined) entrustScopeModel.value = suggestions.entrustScope
  if (suggestions.commissionRate !== undefined) entrustCommissionModel.value = suggestions.commissionRate
  if (suggestions.reason !== undefined) actionReason.value = suggestions.reason
  if (suggestions.auditReason !== undefined) auditForm.auditReason = suggestions.auditReason
  if (suggestions.opinion !== undefined) contractOpinion.value = suggestions.opinion
  if (suggestions.description !== undefined && transactionDialog.type === 'ownerCreateHouse') {
    aiRecommendAnswer.value = suggestions.description
  }
}

async function favoriteSelectedHouse(row) {
  if (!row.houseId) return ElMessage.warning('当前记录不是可收藏房源')
  await favoriteTenantHouse(row.houseId)
  ElMessage.success('已收藏')
  await refreshMode()
}

async function cancelFavoriteFromSelected() {
  if (!selected.value?.houseId) return ElMessage.warning('请选择收藏房源')
  await cancelTenantFavorite(selected.value.houseId)
  ElMessage.success('已取消收藏')
  await refreshMode()
}

async function createAppointmentFromSelected() {
  if (!selected.value?.houseId || !tenantAppointment.appointmentTime) {
    return ElMessage.warning('请选择房源并填写预约时间')
  }
  const res = await createTenantAppointment({
    houseId: selected.value.houseId,
    appointmentTime: tenantAppointment.appointmentTime,
    remark: tenantAppointment.remark
  })
  ElMessage.success('预约已提交')
  openChatFromResponse(res, { bizType: 'appointment', bizId: res?.appointment?.appointmentId, title: '预约沟通' })
  await refreshMode()
}

async function createIntentionFromSelected() {
  if (!selected.value?.houseId) return ElMessage.warning('请选择房源')
  const res = await createTenantIntention({
    houseId: selected.value.houseId,
    intentionLevel: tenantIntention.intentionLevel,
    note: tenantIntention.note
  })
  ElMessage.success('意向已提交')
  openChatFromResponse(res, { bizType: 'intention', bizId: res?.intention?.intentionId, title: '意向沟通' })
  await refreshMode()
}

async function abandonIntentionFromSelected() {
  if (!selected.value?.intentionId) return ElMessage.warning('请选择意向')
  const res = await abandonTenantIntention(selected.value.intentionId)
  ElMessage.success('已放弃意向')
  openChatFromResponse(res, { bizType: 'intention', bizId: selected.value.intentionId, title: '意向沟通' })
  await refreshMode()
}

async function applyTenantDealFromSelected() {
  if (!selected.value?.houseId) return ElMessage.warning('请选择房源')
  if (!dealForm.startDate || !dealForm.endDate || !dealForm.rentAmount) {
    return ElMessage.warning('请填写合同时间和租金')
  }
  const res = await applyTenantDeal(selected.value.houseId, buildDealPayload())
  ElMessage.success('成交申请已提交')
  openChatFromResponse(res, { bizType: 'contract', bizId: res?.contract?.contractId, title: '合同沟通' })
  await refreshMode()
}

async function cancelAppointmentFromSelected() {
  if (!selected.value?.appointmentId) return ElMessage.warning('请选择预约')
  const res = await cancelTenantAppointment(selected.value.appointmentId, { reason: actionReason.value })
  ElMessage.success('预约已取消')
  openChatFromResponse(res, { bizType: 'appointment', bizId: selected.value.appointmentId, title: '预约沟通' })
  await refreshMode()
}

async function loadTenantFavoritesFromWorkspace() {
  const res = await listTenantFavorites({})
  records.value = rowsOf(res).map(item => ({ ...item, _recordType: '我的收藏' }))
  selected.value = records.value[0] || null
}

async function loadTenantAppointmentsFromWorkspace() {
  const res = await listTenantAppointments({})
  records.value = rowsOf(res).map(item => ({ ...item, _recordType: '我的预约' }))
  selected.value = records.value[0] || null
}

async function loadTenantIntentionsFromWorkspace() {
  const res = await listTenantIntentions({})
  records.value = rowsOf(res).map(item => ({ ...item, _recordType: '我的意向' }))
  selected.value = records.value[0] || null
}

async function loadTenantContractsFromWorkspace() {
  const res = await listTenantContracts({})
  records.value = rowsOf(res).map(item => ({ ...item, _recordType: '我的合同' }))
  selected.value = records.value[0] || null
}

async function loadTenantHouseDetailFromSelected() {
  if (!selected.value?.houseId) return ElMessage.warning('请选择房源')
  const res = await getTenantHouse(selected.value.houseId)
  selected.value = { ...(res?.data || {}), _recordType: '房源详情' }
}

async function createOwnerHouseFromForm() {
  if (!ownerHouse.title || !ownerHouse.city || !ownerHouse.district || !ownerHouse.address) {
    return ElMessage.warning('请完整填写房源基础信息')
  }
  await createOwnerHouse(ownerHouse)
  ElMessage.success('房源已提交审核')
  Object.assign(ownerHouse, {
    title: '',
    city: '',
    district: '',
    address: '',
    rentAmount: null,
    area: null,
    operationMode: '0',
    imageUrls: '',
    description: ''
  })
  await refreshMode()
}

async function auditAdminHouse(row, auditStatus) {
  if (!row?.houseId) return ElMessage.warning('请选择房源')
  if (String(row.status) !== '1') return ElMessage.warning('只有待审核房源可以处理')
  if (auditStatus === '3' && !auditForm.auditReason.trim()) {
    return ElMessage.warning('驳回房源必须填写审核意见')
  }
  await auditHouse(row.houseId, {
    auditStatus,
    auditReason: auditForm.auditReason
  })
  ElMessage.success(auditStatus === '2' ? '房源已通过并发布' : '房源已驳回')
  await refreshMode()
}

async function submitOwnerAuditFromSelected() {
  if (!selected.value?.houseId) return ElMessage.warning('请选择房源')
  await submitOwnerHouseAudit(selected.value.houseId)
  ElMessage.success('已重新提交审核')
  await refreshMode()
}

async function loadOwnerHistory() {
  const res = await listOwnerEntrusts({})
  records.value = rowsOf(res).map(item => ({ ...item, _recordType: '历史委托' }))
  selected.value = records.value[0] || null
}

async function inviteAgentFromSelected(row) {
  if (!row.houseId || !ownerEntrust.agentId) {
    return ElMessage.warning('请选择房源并填写中介用户ID')
  }
  const res = await entrustOwnerHouse(row.houseId, ownerEntrust)
  ElMessage.success('委托邀请已发送')
  openChatFromResponse(res, { bizType: 'entrust', bizId: res?.entrust?.entrustId, title: '委托沟通' })
  await refreshMode()
}

async function confirmOwnerApplication(row) {
  const res = await confirmOwnerEntrust(row.entrustId)
  ElMessage.success('已确认中介申请')
  openChatFromResponse(res, { bizType: 'entrust', bizId: row.entrustId, title: '委托沟通' })
  await refreshMode()
}

async function rejectOwnerApplication(row) {
  const res = await rejectOwnerEntrust(row.entrustId)
  ElMessage.success('已拒绝中介申请')
  openChatFromResponse(res, { bizType: 'entrust', bizId: row.entrustId, title: '委托沟通' })
  await refreshMode()
}

async function applyEntrustFromSelected() {
  if (!selected.value?.houseId) return ElMessage.warning('请选择可申请房源')
  const res = await applyAgentEntrust(selected.value.houseId, agentApply)
  ElMessage.success('承接申请已提交')
  openChatFromResponse(res, { bizType: 'entrust', bizId: res?.entrust?.entrustId, title: '委托沟通' })
  await refreshMode()
}

async function loadAgentHistory() {
  const res = await listAgentHouses({})
  records.value = rowsOf(res).map(item => ({ ...item, _recordType: '受托房源' }))
  selected.value = records.value[0] || null
}

async function loadAgentCandidateWorkspace() {
  const res = await listAgentCandidateHouses({})
  records.value = rowsOf(res).map(item => ({ ...item, _recordType: '可申请房源' }))
  selected.value = records.value[0] || null
}

async function loadAgentHouseDetailFromSelected() {
  if (!selected.value?.houseId) return ElMessage.warning('请选择房源')
  const res = await getAgentHouse(selected.value.houseId)
  selected.value = { ...(res?.data || {}), _recordType: '受托房源详情' }
}

async function confirmAgentInvite(row) {
  const res = await confirmAgentEntrust(row.entrustId)
  ElMessage.success('已接受委托')
  openChatFromResponse(res, { bizType: 'entrust', bizId: row.entrustId, title: '委托沟通' })
  await refreshMode()
}

async function rejectAgentInvite(row) {
  const res = await rejectAgentEntrust(row.entrustId)
  ElMessage.success('已拒绝委托')
  openChatFromResponse(res, { bizType: 'entrust', bizId: row.entrustId, title: '委托沟通' })
  await refreshMode()
}

async function confirmAppointmentFromSelected() {
  if (!selected.value?.appointmentId) return ElMessage.warning('请选择预约')
  const res = await confirmAgentAppointment(selected.value.appointmentId)
  ElMessage.success('预约已确认')
  openChatFromResponse(res, { bizType: 'appointment', bizId: selected.value.appointmentId, title: '预约沟通' })
  await refreshMode()
}

async function rejectAppointmentFromSelected() {
  if (!selected.value?.appointmentId) return ElMessage.warning('请选择预约')
  const res = await rejectAgentAppointment(selected.value.appointmentId, { reason: actionReason.value })
  ElMessage.success('预约已拒绝')
  openChatFromResponse(res, { bizType: 'appointment', bizId: selected.value.appointmentId, title: '预约沟通' })
  await refreshMode()
}

async function completeAppointmentFromSelected() {
  if (!selected.value?.appointmentId) return ElMessage.warning('请选择预约')
  const res = await completeAgentAppointment(selected.value.appointmentId)
  ElMessage.success('看房已完成')
  openChatFromResponse(res, { bizType: 'appointment', bizId: selected.value.appointmentId, title: '预约沟通' })
  await refreshMode()
}

async function followIntentionFromSelected() {
  if (!selected.value?.intentionId) return ElMessage.warning('请选择意向')
  const res = await followAgentIntention(selected.value.intentionId, agentFollow)
  ElMessage.success('意向已跟进')
  openChatFromResponse(res, { bizType: 'intention', bizId: selected.value.intentionId, title: '意向沟通' })
  await refreshMode()
}

async function invalidIntentionFromSelected() {
  if (!selected.value?.intentionId) return ElMessage.warning('请选择意向')
  const res = await invalidAgentIntention(selected.value.intentionId, { reason: actionReason.value })
  ElMessage.success('意向已标记无效')
  openChatFromResponse(res, { bizType: 'intention', bizId: selected.value.intentionId, title: '意向沟通' })
  await refreshMode()
}

async function dealIntentionFromSelected() {
  if (!selected.value?.intentionId) return ElMessage.warning('请选择意向')
  const res = await dealAgentIntention(selected.value.intentionId, buildDealPayload())
  ElMessage.success('意向已转成交')
  openChatFromResponse(res, { bizType: 'contract', bizId: res?.contract?.contractId, title: '合同沟通' })
  await refreshMode()
}

async function dealHouseFromSelected() {
  if (!selected.value?.houseId) return ElMessage.warning('请选择房源')
  if (!dealForm.tenantId) return ElMessage.warning('请填写租户用户ID')
  const res = await dealAgentHouse(selected.value.houseId, buildDealPayload())
  ElMessage.success('成交已确认')
  openChatFromResponse(res, { bizType: 'contract', bizId: res?.contract?.contractId, title: '合同沟通' })
  await refreshMode()
}

async function submitSign(row) {
  const res = await submitContractSign(row.contractId)
  ElMessage.success('已提交签署')
  openChatFromResponse(res, { bizType: 'contract', bizId: row.contractId, title: '合同沟通' })
  await refreshMode()
}

async function tenantConfirm(row) {
  const res = await confirmTenantContract(row.contractId, { opinion: contractOpinion.value })
  ElMessage.success('租户已确认')
  openChatFromResponse(res, { bizType: 'contract', bizId: row.contractId, title: '合同沟通' })
  await refreshMode()
}

async function ownerConfirm(row) {
  const res = await confirmOwnerContract(row.contractId, { opinion: contractOpinion.value })
  ElMessage.success('户主已确认')
  openChatFromResponse(res, { bizType: 'contract', bizId: row.contractId, title: '合同沟通' })
  await refreshMode()
}

async function agentConfirm(row) {
  const res = await confirmAgentContract(row.contractId, { opinion: contractOpinion.value })
  ElMessage.success('中介已确认')
  openChatFromResponse(res, { bizType: 'contract', bizId: row.contractId, title: '合同沟通' })
  await refreshMode()
}

async function rejectCurrentContract(row) {
  const res = await rejectContract(row.contractId, { opinion: contractOpinion.value })
  ElMessage.success('合同已拒绝')
  openChatFromResponse(res, { bizType: 'contract', bizId: row.contractId, title: '合同沟通' })
  await refreshMode()
}

async function activateCurrentContract(row) {
  const res = await activateContract(row.contractId)
  ElMessage.success('合同已生效')
  openChatFromResponse(res, { bizType: 'contract', bizId: row.contractId, title: '合同沟通' })
  await refreshMode()
}

async function voidCurrentContract(row) {
  const res = await voidContract(row.contractId, { reason: actionReason.value })
  ElMessage.success('合同已作废')
  openChatFromResponse(res, { bizType: 'contract', bizId: row.contractId, title: '合同沟通' })
  await refreshMode()
}

async function terminateCurrentContract(row) {
  const res = await terminateContract(row.contractId, { reason: actionReason.value })
  ElMessage.success('合同已终止')
  openChatFromResponse(res, { bizType: 'contract', bizId: row.contractId, title: '合同沟通' })
  await refreshMode()
}

async function loadContractDetailFromSelected() {
  if (!selected.value?.contractId) return ElMessage.warning('请选择合同')
  const res = await getContractDetail(selected.value.contractId)
  selected.value = { ...(res?.data || {}), _recordType: '合同详情' }
}

async function openContractChatFromSelected() {
  if (!selected.value?.contractId) return ElMessage.warning('请选择合同')
  const res = await openContractChat(selected.value.contractId)
  const session = res?.data || null
  openChat({
    bizType: 'contract',
    bizId: selected.value.contractId,
    sessionId: session?.sessionId || '',
    title: '合同沟通'
  })
}

async function loadPortalSummary() {
  const res = await getPortalHomeSummary()
  const data = res?.data || {}
  workspaceSummary.houseCount = data.houseCount || 0
  workspaceSummary.avgRent = data.avgRent || 0
  workspaceSummary.pricedHouseCount = data.pricedHouseCount || 0
  workspaceSummary.indexedHouseCount = data.indexedHouseCount || 0
}

async function loadPortalPublicHouses() {
  const res = await listPortalHouses({})
  records.value = rowsOf(res).map(item => ({ ...item, _recordType: '公开房源' }))
  selected.value = records.value[0] || null
}

async function runAiRecommendation() {
  if (!aiRecommend.query.trim()) return ElMessage.warning('请输入推荐需求')
  const aiRequest = buildAiRequest(aiRecommend.query)
  const res = await recommendRentalHouses({
    ...aiRecommend,
    userId: userStore.id,
    role: workMode.value,
    context: aiRequest.context
  })
  const aiResult = normalizeAiResponse(res)
  aiRecommendAnswer.value = aiResult.answer || '推荐结果已生成'
  ElMessage.success('已生成推荐结果')
}

async function loadAiCapabilities() {
  try {
    const res = await getAiCapabilities()
    const data = normalizeAiResponsePayload(res)
    aiCapabilities.online = true
    aiCapabilities.model = data.embeddingModel || data.model || data.langchain?.model || ''
    aiCapabilities.embeddingMode = data.embeddingMode || ''
    aiCapabilities.vectorStore = data.vectorStore || ''
    aiCapabilities.indexedKnowledge = Number(data.indexedKnowledge || 0)
    aiCapabilities.knowledgeSources = Array.isArray(data.knowledgeSources) ? data.knowledgeSources : []
    aiCapabilities.tools = Array.isArray(data.tools) ? data.tools : []
    aiCapabilities.skills = Array.isArray(data.skills) ? data.skills : []
    if (Number.isFinite(Number(data.indexedHouses))) {
      workspaceSummary.indexedHouseCount = Number(data.indexedHouses)
    }
  } catch (error) {
    aiCapabilities.online = false
  }
}

function resetKnowledgeForm() {
  knowledgeForm.sourceType = 'faq'
  knowledgeForm.title = ''
  knowledgeForm.roles = ['tenant']
  knowledgeForm.content = ''
}

async function submitKnowledgeDocument() {
  if (!knowledgeForm.title.trim()) return ElMessage.warning('请填写知识标题')
  if (!knowledgeForm.content.trim()) return ElMessage.warning('请填写知识正文')
  knowledgeSubmitting.value = true
  try {
    await indexKnowledgeDocument({
      sourceType: knowledgeForm.sourceType,
      action: 'upsert',
      document: {
        title: knowledgeForm.title.trim(),
        roles: knowledgeForm.roles,
        content: knowledgeForm.content.trim()
      }
    })
    ElMessage.success('知识文档已写入统一知识库')
    resetKnowledgeForm()
    await loadAiCapabilities()
  } finally {
    knowledgeSubmitting.value = false
  }
}

async function seedKnowledgeBase() {
  knowledgeSeeding.value = true
  try {
    const res = await seedKnowledgeDocuments()
    const data = normalizeAiResponsePayload(res)
    ElMessage.success(data?.message || '基础知识已写入统一知识库')
    await loadAiCapabilities()
  } finally {
    knowledgeSeeding.value = false
  }
}

async function createAiIndexTaskFromSelected() {
  if (!selected.value?.houseId) return ElMessage.warning('请选择需要入库的房源')
  const payload = {
    houseId: selected.value.houseId,
    action: 'upsert'
  }
  await createAiIndexTask(payload)
  ElMessage.success('当前房源索引任务已创建')
  await loadAiIndexTasks()
  await inspectAiHouseDocumentFromSelected()
}

async function createFullAiIndexTask() {
  if (!canRunFullAiSync.value) return ElMessage.warning('只有审核员可以创建全量同步任务')
  await createAiIndexTask({ action: 'sync' })
  ElMessage.success('全量同步索引任务已创建')
  await loadAiIndexTasks()
}

async function loadAiIndexTasks() {
  const res = await listAiIndexTasks({})
  aiIndexTasks.value = rowsOf(res)
}

async function processAiTask(row) {
  if (!row?.taskId) return ElMessage.warning('请选择索引任务')
  await processAiIndexTask(row.taskId)
  ElMessage.success('索引任务已处理')
  await loadAiIndexTasks()
  await loadPortalSummary()
  await loadAiCapabilities()
  if (selected.value?.houseId) {
    await inspectAiHouseDocumentFromSelected()
  }
}

async function processPendingAiTasks() {
  const res = await processPendingAiIndexTasks({ limit: 5 })
  const processed = res?.data?.processed ?? 0
  ElMessage.success(`已处理 ${processed} 个索引任务`)
  await loadAiIndexTasks()
  await loadPortalSummary()
  await loadAiCapabilities()
  if (selected.value?.houseId) {
    await inspectAiHouseDocumentFromSelected()
  }
}

async function inspectAiHouseDocumentFromSelected() {
  if (!selected.value?.houseId) return ElMessage.warning('请选择房源')
  const res = await inspectAiHouseDocument(selected.value.houseId)
  aiHouseDocument.value = res?.data || null
  ElMessage.success('已读取房源文档状态')
}

async function sendAgentMessage() {
  const content = agentInput.value.trim()
  if (!content) return
  agentMessages.value.push({ role: 'user', content })
  agentInput.value = ''
  agentLoading.value = true
  try {
    const res = await sendPortalAiChat(buildAiRequest(content))
    agentMessages.value.push(normalizeAiMessage(res))
  } catch (error) {
    agentMessages.value.push({ role: 'assistant', content: '智能体服务暂时没有返回结果，请稍后重试或切换到业务页继续处理当前流程。' })
  } finally {
    agentLoading.value = false
  }
}

function openFloatingAi() {
  floatingAiOpen.value = true
  nextTick(scrollFloatingAiToBottom)
}

function resetFloatingAiPosition() {
  if (typeof window === 'undefined') return
  const width = Math.min(420, window.innerWidth - 32)
  const height = Math.min(620, window.innerHeight - 120)
  floatingAiPosition.x = Math.max(16, window.innerWidth - width - 92)
  floatingAiPosition.y = Math.max(84, window.innerHeight - height - 28)
}

function startFloatingAiDrag(event) {
  if (event.button !== undefined && event.button !== 0) return
  floatingAiDrag.active = true
  floatingAiDrag.offsetX = event.clientX - floatingAiPosition.x
  floatingAiDrag.offsetY = event.clientY - floatingAiPosition.y
  window.addEventListener('pointermove', moveFloatingAi)
  window.addEventListener('pointerup', stopFloatingAiDrag)
}

function moveFloatingAi(event) {
  if (!floatingAiDrag.active || typeof window === 'undefined') return
  const modalWidth = Math.min(420, window.innerWidth - 32)
  const modalHeight = Math.min(620, window.innerHeight - 120)
  const nextX = event.clientX - floatingAiDrag.offsetX
  const nextY = event.clientY - floatingAiDrag.offsetY
  floatingAiPosition.x = clamp(nextX, 12, window.innerWidth - modalWidth - 12)
  floatingAiPosition.y = clamp(nextY, 72, window.innerHeight - modalHeight - 12)
}

function stopFloatingAiDrag() {
  floatingAiDrag.active = false
  if (typeof window === 'undefined') return
  window.removeEventListener('pointermove', moveFloatingAi)
  window.removeEventListener('pointerup', stopFloatingAiDrag)
}

async function sendFloatingAiMessage() {
  const content = floatingAiInput.value.trim()
  if (!content) return
  floatingAiMessages.value.push({ role: 'user', content })
  floatingAiInput.value = ''
  floatingAiLoading.value = true
  await nextTick(scrollFloatingAiToBottom)
  try {
    const res = await sendPortalAiChat(buildAiRequest(content))
    floatingAiMessages.value.push(normalizeAiMessage(res))
  } catch (error) {
    floatingAiMessages.value.push({ role: 'assistant', content: '智能体服务暂时没有返回结果，请稍后重试。当前业务页和消息沟通不受影响。' })
  } finally {
    floatingAiLoading.value = false
    await nextTick(scrollFloatingAiToBottom)
  }
}

async function runBusinessAiAction() {
  if (workMode.value === 'tenant') {
    return runAiRecommendation()
  }
  const prompts = {
    admin: '请结合当前房源生成审核意见，指出硬性问题与补充建议。',
    owner: '请基于当前房源生成更适合发布的房源标题和描述。',
    agent: '请结合当前业务生成下一步跟进建议和沟通话术。',
    contract: '请梳理当前合同的主要风险点，并给出确认前检查项。'
  }
  floatingAiInput.value = prompts[workMode.value] || '请结合当前业务给出下一步建议。'
  openFloatingAi()
  await sendFloatingAiMessage()
}

function scrollFloatingAiToBottom() {
  if (floatingAiListRef.value) {
    floatingAiListRef.value.scrollTop = floatingAiListRef.value.scrollHeight
  }
}

function buildDealPayload() {
  return {
    tenantId: dealForm.tenantId ? Number(dealForm.tenantId) : undefined,
    startDate: dealForm.startDate,
    endDate: dealForm.endDate,
    rentAmount: dealForm.rentAmount,
    depositAmount: dealForm.depositAmount,
    paymentCycle: dealForm.paymentCycle,
    contractContent: dealForm.contractContent
  }
}

function buildAiRequest(message) {
  const memorySource = pageMode.value === 'agent' ? agentMessages.value : floatingAiMessages.value
  const history = memorySource.slice(-6).map(item => ({
    role: item.role,
    content: item.content
  }))
  return {
    message,
    userId: userStore.id,
    username: userStore.name,
    role: workMode.value,
    roles: roles.value,
    sessionId: `${userStore.id || 'anonymous'}-${pageMode.value}-${workMode.value}`,
    context: {
      pageMode: pageMode.value,
      workMode: workMode.value,
      selected: selected.value ? buildAiRecordContext(selected.value) : null,
      filters: { ...filters },
      visibleActions: visibleActions.value.map(item => ({ key: item.key, label: item.label })),
      summary: { ...workspaceSummary },
      history
    }
  }
}

function buildAgentWelcomeMessage() {
  return {
    role: 'assistant',
    content: `${aiAssistantProfile.value.summary} 你可以直接问我当前业务下一步该怎么做，我会尽量结合已选中的记录来回答。`
  }
}

function seedAgentWelcome() {
  agentMessages.value = [buildAgentWelcomeMessage()]
}

function buildAiRecordContext(item) {
  return {
    recordType: item._recordType,
    houseId: item.houseId,
    entrustId: item.entrustId,
    appointmentId: item.appointmentId,
    intentionId: item.intentionId,
    contractId: item.contractId,
    title: recordTitle(item),
    subtitle: recordSubtitle(item),
    status: recordStatus(item),
    statusLabel: recordStatusLabel(item),
    auditStatus: item.auditStatus,
    auditReason: item.auditReason,
    rentAmount: item.rentAmount,
    area: item.area,
    city: item.city,
    district: item.district,
    community: item.community,
    address: item.address,
    ownerId: item.ownerId,
    agentId: item.agentId,
    tenantId: item.tenantId,
    appointmentTime: item.appointmentTime,
    startDate: item.startDate,
    endDate: item.endDate,
    description: item.description,
    facilities: item.facilities,
    tags: item.tags
  }
}

function normalizeAiResponse(response) {
  const data = normalizeAiResponsePayload(response)
  if (typeof data === 'string') {
    return { answer: data, toolCalls: [], suggestions: null }
  }
  return {
    answer: data?.answer || data?.msg || '智能体接口已收到请求。',
    intent: data?.intent || '',
    intentLabel: data?.intentLabel || '',
    toolCalls: data?.toolCalls || [],
    suggestions: data?.suggestions || null,
    nextActions: data?.nextActions || []
  }
}

function normalizeAiResponsePayload(response) {
  const data = response?.data && typeof response.data === 'object' ? response.data : response
  if (data?.data && typeof data.data === 'object') {
    return data.data
  }
  return data || {}
}

function normalizeAiMessage(response) {
  const aiResult = normalizeAiResponse(response)
  return {
    role: 'assistant',
    content: aiResult.answer || '智能体接口已收到请求，等待后端返回标准化结果。',
    intent: aiResult.intent,
    intentLabel: aiResult.intentLabel,
    toolCalls: aiResult.toolCalls || [],
    suggestions: aiResult.suggestions,
    nextActions: aiResult.nextActions || []
  }
}

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max)
}
</script>

<style scoped>
.portal-shell {
  --portal-ink: #172033;
  --portal-muted: #667085;
  --portal-line: #dfe5ea;
  --portal-soft: #f6f8fb;
  --portal-surface: #ffffff;
  --portal-primary: #2563eb;
  --portal-primary-strong: #1e3a5f;
  --portal-action: #0f766e;
  --portal-accent: #b45309;
  --portal-info: #475467;
  min-height: calc(100vh - 84px);
  color: var(--portal-ink);
  background: #f5f7f8;
}

.portal-shell.is-agent {
  background:
    linear-gradient(180deg, rgba(30, 58, 95, 0.06), rgba(245, 247, 248, 0) 320px),
    #f5f7f8;
}

.portal-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  height: 64px;
  padding: 0 32px;
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1px solid var(--portal-line);
  backdrop-filter: blur(10px);
}

.brand span {
  display: block;
  color: var(--portal-ink);
  font-size: 18px;
  font-weight: 800;
}

.brand small {
  color: var(--portal-muted);
}

.page-switch {
  display: grid;
  grid-template-columns: repeat(2, 72px);
  width: 144px;
  height: 30px;
  background: #eef3f4;
  border: 1px solid var(--portal-line);
  border-radius: 8px;
  overflow: hidden;
}

.page-switch button {
  width: 72px;
  height: 30px;
  color: var(--portal-muted);
  cursor: pointer;
  background: transparent;
  border: 0;
  font-size: 13px;
}

.page-switch button.active {
  color: var(--portal-primary);
  background: #fff;
  box-shadow: 0 1px 8px rgba(37, 99, 235, 0.16);
}

.header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.user-entry {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 38px;
  max-width: 190px;
  padding: 0 10px 0 6px;
  color: var(--portal-ink);
  cursor: pointer;
  background: #fff;
  border: 1px solid var(--portal-line);
  border-radius: 999px;
  box-shadow: 0 10px 24px rgba(23, 32, 51, 0.06);
}

.user-entry span {
  max-width: 92px;
  overflow: hidden;
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-entry .el-icon {
  color: var(--portal-muted);
  font-size: 13px;
}

.business-page {
  width: min(1480px, calc(100vw - 48px));
  margin: 0 auto;
  padding: 24px 0 36px;
}

.business-search {
  display: grid;
  grid-template-columns: 130px minmax(260px, 1fr) 140px 120px;
  gap: 10px;
  width: min(920px, 100%);
  margin: 0 auto 16px;
}

.role-select {
  width: 130px;
}

.admin-mode-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  color: #fff;
  background: var(--portal-primary-strong);
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
}

.role-tabs {
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: minmax(58px, 1fr);
  align-items: center;
  height: 32px;
  padding: 2px;
  background: #eef3f4;
  border: 1px solid var(--portal-line);
  border-radius: 8px;
}

.role-tabs button {
  height: 26px;
  color: #667085;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: 6px;
  font-size: 13px;
}

.role-tabs button.active {
  color: var(--portal-primary);
  background: #fff;
  box-shadow: 0 1px 5px rgba(15, 23, 42, 0.12);
}

.hot-tags {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 24px;
  color: var(--portal-muted);
}

.hot-tags button {
  padding: 5px 10px;
  color: var(--portal-primary);
  cursor: pointer;
  background: #fff;
  border: 1px solid var(--portal-line);
  border-radius: 6px;
}

.business-workspace {
  display: grid;
  grid-template-columns: 460px minmax(0, 1fr);
  gap: 18px;
}

.result-list,
.detail-card {
  background: var(--portal-surface);
  border: 1px solid var(--portal-line);
  border-radius: 8px;
}

.result-list {
  min-height: 680px;
  overflow: hidden;
}

.list-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 18px 24px 12px;
}

.list-head p,
.list-head h1 {
  margin: 0;
}

.list-head p {
  color: var(--portal-muted);
  font-size: 13px;
}

.list-head h1 {
  margin-top: 4px;
  color: var(--portal-ink);
  font-size: 22px;
}

.list-head span {
  color: #8a96a8;
  font-size: 13px;
}

.records {
  max-height: calc(100vh - 250px);
  min-height: 600px;
  padding: 0 14px 18px;
  overflow-y: auto;
}

.record-card {
  display: block;
  width: 100%;
  padding: 18px;
  margin-bottom: 12px;
  text-align: left;
  cursor: pointer;
  background: #fff;
  border: 1px solid var(--portal-line);
  border-radius: 8px;
}

.record-card:hover,
.record-card.active {
  border-color: var(--portal-primary);
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.12);
}

.record-card__top,
.record-card__bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.record-card__top strong {
  color: var(--portal-ink);
  font-size: 18px;
}

.record-card__top span {
  flex: 0 0 auto;
  color: var(--portal-accent);
  font-size: 18px;
  font-weight: 700;
}

.record-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 14px 0;
}

.record-tags em {
  padding: 4px 8px;
  color: var(--portal-muted);
  background: var(--portal-soft);
  border-radius: 5px;
  font-style: normal;
  font-size: 12px;
}

.record-card__bottom span,
.record-card__bottom small {
  color: var(--portal-muted);
}

.detail-card {
  min-height: 680px;
  padding: 30px;
}

.empty-workspace {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 260px;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding-bottom: 22px;
  border-bottom: 1px solid #edf1f3;
}

.detail-head h2 {
  margin: 0 0 10px;
  color: var(--portal-ink);
  font-size: 28px;
}

.detail-head p {
  margin: 0 0 12px;
  color: var(--portal-muted);
}

.detail-head > strong,
.detail-head__aside > strong {
  color: var(--portal-accent);
  font-size: 26px;
}

.detail-head__aside {
  display: grid;
  justify-items: end;
  gap: 10px;
  flex: 0 0 auto;
}

.detail-tags {
  display: flex;
  gap: 8px;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin: 22px 0;
}

.field-grid article {
  padding: 14px;
  background: var(--portal-soft);
  border: 1px solid var(--portal-line);
  border-radius: 8px;
}

.field-grid span,
.field-grid strong {
  display: block;
}

.field-grid span {
  margin-bottom: 8px;
  color: var(--portal-muted);
  font-size: 12px;
}

.field-grid strong {
  color: var(--portal-ink);
  overflow-wrap: anywhere;
}

.record-detail-drawer :deep(.el-drawer__body) {
  padding: 0;
  background: var(--portal-soft);
}

.drawer-detail-shell {
  padding: 24px;
}

.drawer-head {
  padding: 22px;
  background: #fff;
  border: 1px solid var(--portal-line);
  border-radius: 8px;
}

.detail-gallery {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 16px;
}

.detail-gallery :deep(.el-image) {
  width: 100%;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  background: #e8edf3;
  border-radius: 8px;
}

.detail-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.drawer-note-list {
  display: grid;
  gap: 12px;
}

.drawer-note-list article {
  padding: 12px;
  background: #fff;
  border: 1px solid var(--portal-line);
  border-radius: 8px;
}

.drawer-note-list span {
  display: block;
  margin-bottom: 6px;
  color: var(--portal-muted);
  font-size: 12px;
}

.drawer-note-list p {
  margin: 0;
  color: var(--portal-ink);
  line-height: 1.7;
}

.process-card,
.action-card,
.operation-card {
  padding: 18px;
  margin-top: 18px;
  background: var(--portal-soft);
  border: 1px solid var(--portal-line);
  border-radius: 8px;
}

.process-card h3,
.action-card h3,
.operation-card h3 {
  margin: 0 0 12px;
  color: var(--portal-ink);
}

.process-steps {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.process-steps div {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--portal-muted);
}

.process-steps span {
  width: 10px;
  height: 10px;
  background: #d5dde5;
  border-radius: 50%;
}

.process-steps .done {
  color: var(--portal-action);
}

.process-steps .done span {
  background: var(--portal-action);
}

.process-steps p {
  margin: 0;
}

.action-card {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.action-card p {
  margin: 0;
  color: var(--portal-muted);
}

.action-buttons {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.business-ai-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 520px);
  gap: 18px;
  align-items: center;
  padding: 20px;
  margin-top: 18px;
  background: linear-gradient(135deg, #f7fbff, #eff6ff 48%, #f9fafb);
  border: 1px solid #d7e3f7;
  border-radius: 8px;
}

.business-ai-card > div:first-child span,
.ai-console-head span {
  display: block;
  margin-bottom: 8px;
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
}

.business-ai-card h3,
.ai-console-head h2 {
  margin: 0;
  color: var(--portal-ink);
}

.business-ai-card p,
.ai-console-head p {
  margin: 10px 0 0;
  color: var(--portal-muted);
  line-height: 1.65;
}

.business-ai-actions {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 140px auto auto;
  gap: 10px;
  align-items: center;
}

.ai-inline-input,
.business-ai-actions :deep(.el-input-number) {
  width: 100%;
}

.ai-recommend-note {
  grid-column: 1 / -1;
  margin: 0;
  padding: 12px 14px;
  color: #334155;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #dbe7f7;
  border-radius: 8px;
}

.ai-console-shell {
  padding: 24px;
  background: var(--portal-soft);
  min-height: 100%;
}

.ai-console-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 22px;
  background: #fff;
  border: 1px solid var(--portal-line);
  border-radius: 8px;
}

.ai-console-status {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid var(--portal-line);
  border-radius: 999px;
  white-space: nowrap;
}

.ai-console-status i {
  width: 10px;
  height: 10px;
  background: #f59e0b;
  border-radius: 50%;
}

.ai-console-status i.online {
  background: #16a34a;
}

.console-summary,
.console-actions {
  margin-top: 16px;
}

.console-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.panel-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

.summary-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

.summary-strip span {
  padding: 6px 10px;
  color: #4b5563;
  background: #fff;
  border: 1px solid var(--portal-line);
  border-radius: 999px;
  font-size: 12px;
}

.compliance-card {
  background: #f8fafc;
}

.compliance-checks {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
}

.compliance-checks span {
  padding: 8px 10px;
  color: var(--portal-muted);
  background: #fff;
  border: 1px solid var(--portal-line);
  border-radius: 6px;
  text-align: center;
  font-size: 12px;
}

.compliance-checks span.done {
  color: var(--portal-action);
  border-color: rgba(15, 118, 110, 0.34);
}

.muted-tip {
  margin: 12px 0 0;
  color: var(--portal-muted);
  font-size: 13px;
}

.audit-summary {
  padding: 14px;
  margin-bottom: 12px;
  background: var(--portal-soft);
  border: 1px solid var(--portal-line);
  border-radius: 8px;
}

.audit-summary span,
.audit-summary strong,
.audit-summary p {
  display: block;
}

.audit-summary span {
  margin-bottom: 4px;
  color: var(--portal-muted);
  font-size: 12px;
}

.audit-summary strong {
  color: var(--portal-ink);
  font-size: 16px;
}

.audit-summary p {
  margin: 6px 0 0;
  color: var(--portal-muted);
}

.mini-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.mini-card,
.table-shell {
  background: #fff;
  border: 1px solid var(--portal-line);
  border-radius: 8px;
}

.mini-card {
  padding: 14px;
}

.mini-card strong {
  display: block;
  margin-bottom: 6px;
  color: var(--portal-ink);
}

.mini-card p {
  margin: 0;
  color: var(--portal-muted);
  line-height: 1.6;
  font-size: 13px;
}

.table-shell {
  margin-top: 14px;
  overflow: hidden;
}

.compact-shell :deep(.el-table__cell) {
  padding: 8px 0;
}

.inline-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) 160px auto;
  gap: 10px;
  align-items: center;
}

.inline-form.compact {
  grid-template-columns: minmax(220px, 1fr) repeat(3, auto);
}

.deal-form,
.contract-extra-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  align-items: center;
}

.deal-form :deep(.el-textarea),
.deal-form > .el-button,
.contract-extra-actions {
  grid-column: 1 / -1;
}

.contract-extra-actions {
  grid-template-columns: minmax(220px, 1fr) repeat(3, auto);
  margin-top: 12px;
}

.transaction-dialog :deep(.el-dialog__body) {
  padding-top: 8px;
}

.transaction-form {
  display: grid;
  gap: 2px;
}

.transaction-summary {
  padding: 14px;
  margin-bottom: 10px;
  background: var(--portal-soft);
  border: 1px solid var(--portal-line);
  border-radius: 8px;
}

.transaction-summary strong {
  display: block;
  color: var(--portal-ink);
  font-size: 15px;
}

.transaction-summary p {
  margin: 6px 0 0;
  color: var(--portal-muted);
  line-height: 1.5;
}

.form-grid {
  display: grid;
  gap: 12px;
}

.form-grid.two {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.transaction-form :deep(.el-select),
.transaction-form :deep(.el-date-editor),
.transaction-form :deep(.el-input-number) {
  width: 100%;
}

.inline-form :deep(.el-date-editor),
.inline-form :deep(.el-input-number),
.inline-form :deep(.el-select),
.deal-form :deep(.el-date-editor),
.deal-form :deep(.el-input-number),
.deal-form :deep(.el-select),
.contract-extra-actions :deep(.el-input) {
  width: 100%;
}

.agent-home {
  min-height: calc(100vh - 148px);
  background: transparent;
}

.agent-topline {
  display: flex;
  justify-content: flex-start;
  padding: 18px 32px;
}

.model-button {
  height: 34px;
  padding: 0 12px;
  color: #1e3a5f;
  cursor: pointer;
  background: #fff;
  border: 1px solid var(--portal-line);
  border-radius: 8px;
  font-size: 14px;
  font-weight: 700;
}

.agent-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 250px);
  padding: 24px;
}

.agent-center.has-chat {
  justify-content: flex-end;
}

.agent-hero h1 {
  margin: 0 0 22px;
  color: var(--portal-ink);
  font-size: 34px;
  font-weight: 700;
}

.agent-hero p {
  margin: 0 0 10px;
  color: var(--portal-primary);
  text-align: center;
  font-size: 13px;
  font-weight: 700;
}

.agent-summary {
  display: block;
  width: min(760px, 100%);
  margin: 0 auto 22px;
  color: #4b5563;
  text-align: center;
  line-height: 1.75;
}

.agent-capabilities {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  width: min(640px, 100%);
  margin: 0 auto 28px;
}

.agent-capabilities span {
  padding: 10px 12px;
  color: #344054;
  text-align: center;
  background: #fff;
  border: 1px solid var(--portal-line);
  border-radius: 8px;
  font-size: 13px;
}

.agent-thread {
  width: min(880px, 100%);
  max-height: calc(100vh - 330px);
  margin-bottom: 24px;
  overflow-y: auto;
}

.agent-message {
  max-width: 76%;
  padding: 12px 14px;
  margin-bottom: 12px;
  border-radius: 8px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.agent-message.assistant {
  color: var(--portal-ink);
  background: #fff;
  border: 1px solid var(--portal-line);
}

.agent-message.user {
  margin-left: auto;
  color: #fff;
  background: var(--portal-primary);
}

.agent-input-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 42px;
  align-items: center;
  gap: 10px;
  width: min(860px, 100%);
  min-height: 60px;
  padding: 8px 10px 8px 18px;
  background: #fff;
  border: 1px solid var(--portal-line);
  border-radius: 24px;
  box-shadow: 0 18px 42px rgba(31, 41, 55, 0.12);
}

.agent-input-card :deep(.el-textarea__inner) {
  min-height: 34px !important;
  padding: 8px 0;
  border: 0;
  box-shadow: none;
}

.agent-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.agent-actions button {
  height: 44px;
  padding: 0 18px;
  color: var(--portal-muted);
  cursor: pointer;
  background: #fff;
  border: 1px solid var(--portal-line);
  border-radius: 22px;
}

.ai-quick-rail {
  position: fixed;
  right: 20px;
  bottom: 76px;
  z-index: 2500;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rail-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  color: var(--portal-primary);
  cursor: pointer;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid var(--portal-line);
  border-radius: 8px;
  box-shadow: 0 12px 28px rgba(36, 42, 66, 0.12);
  backdrop-filter: blur(8px);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.rail-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 34px rgba(36, 42, 66, 0.16);
}

.rail-button .el-icon {
  font-size: 22px;
}

.rail-button.ai-entry {
  color: #fff;
  background: var(--portal-primary);
  border: 0;
  box-shadow: 0 18px 38px rgba(37, 99, 235, 0.28);
}

.ai-quick-rail.is-open .ai-entry {
  transform: scale(1.04);
}

.ai-float-modal {
  position: fixed;
  z-index: 3000;
  display: flex;
  flex-direction: column;
  width: min(420px, calc(100vw - 32px));
  height: min(620px, calc(100vh - 120px));
  overflow: hidden;
  background: #fff;
  border: 1px solid var(--portal-line);
  border-radius: 8px;
  box-shadow: 0 28px 70px rgba(26, 31, 54, 0.28);
}

.ai-float-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 16px 14px;
  color: #fff;
  cursor: move;
  user-select: none;
  background: #1e3a5f;
}

.ai-float-head strong,
.ai-float-head span {
  display: block;
}

.ai-float-head strong {
  font-size: 16px;
}

.ai-float-head span {
  margin-top: 4px;
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
}

.ai-float-actions {
  display: flex;
  gap: 8px;
}

.ai-float-actions button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  color: #fff;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 9px;
}

.ai-float-body {
  flex: 1;
  min-height: 0;
  padding: 18px;
  overflow-y: auto;
  background: var(--portal-soft);
}

.ai-float-message {
  max-width: 84%;
  padding: 10px 12px;
  margin-bottom: 12px;
  border-radius: 8px;
  line-height: 1.65;
  white-space: pre-wrap;
}

.ai-float-message.assistant {
  color: var(--portal-ink);
  background: #fff;
  border: 1px solid var(--portal-line);
}

.ai-float-message.user {
  margin-left: auto;
  color: #fff;
  background: var(--portal-primary);
}

.ai-float-presets {
  display: flex;
  gap: 8px;
  padding: 10px 14px 0;
  overflow-x: auto;
  background: #fff;
}

.ai-float-presets button {
  flex: 0 0 auto;
  height: 30px;
  padding: 0 10px;
  color: #1e3a5f;
  cursor: pointer;
  background: #eef4ff;
  border: 1px solid #c7d7fe;
  border-radius: 15px;
  font-size: 12px;
}

.ai-float-composer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 42px;
  gap: 10px;
  align-items: center;
  padding: 12px 14px 14px;
  background: #fff;
}

.ai-float-composer :deep(.el-textarea__inner) {
  min-height: 40px !important;
  padding: 10px 12px;
  background: var(--portal-soft);
  border: 1px solid var(--portal-line);
  border-radius: 8px;
  box-shadow: none;
}

.message-content {
  white-space: pre-wrap;
}

.ai-trace {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.ai-trace span {
  padding: 3px 7px;
  color: #1e3a5f;
  background: #eef4ff;
  border: 1px solid #c7d7fe;
  border-radius: 999px;
  font-size: 12px;
  line-height: 1.4;
}

.ai-trace.compact span {
  font-size: 11px;
}

.knowledge-card {
  padding: 18px;
  background: #fff;
  border: 1px solid var(--portal-line);
  border-radius: 8px;
}

.knowledge-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
}

.knowledge-card__head strong {
  color: var(--portal-ink);
  font-size: 16px;
}

.knowledge-card__head p {
  max-width: 640px;
  margin: 6px 0 0;
  color: var(--portal-muted);
  line-height: 1.6;
}

.knowledge-form__row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.knowledge-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

@media (max-width: 1100px) {
  .portal-header {
    grid-template-columns: 1fr;
    height: auto;
    gap: 12px;
    padding: 14px 20px;
  }

  .header-actions,
  .page-switch {
    justify-self: start;
  }

  .header-actions {
    flex-wrap: wrap;
  }

  .business-search,
  .business-workspace,
  .field-grid,
  .process-steps,
  .agent-capabilities,
  .inline-form,
  .inline-form.compact,
  .mini-grid,
  .knowledge-form__row,
  .deal-form,
  .contract-extra-actions {
    grid-template-columns: 1fr;
  }

  .business-page {
    width: min(100%, calc(100vw - 28px));
    padding-top: 16px;
  }

  .detail-card,
  .result-list {
    min-height: auto;
  }

  .agent-message,
  .ai-float-message {
    max-width: 92%;
  }

  .records {
    max-height: none;
    min-height: auto;
  }
}

@media (max-width: 720px) {
  .agent-hero h1 {
    font-size: 26px;
    text-align: center;
  }

  .agent-center {
    min-height: calc(100vh - 220px);
    padding: 18px 14px;
  }

  .agent-actions {
    flex-direction: column;
    width: 100%;
  }

  .agent-actions button,
  .agent-input-card {
    width: 100%;
  }

  .user-entry {
    width: 42px;
    padding: 0 5px;
  }

  .user-entry span,
  .user-entry .el-icon {
    display: none;
  }

  .ai-quick-rail {
    right: 14px;
    bottom: 64px;
  }
}
</style>
