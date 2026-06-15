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
            @click="changeWorkMode(item.value)"
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

            <section v-if="canUseTenantMap" v-loading="mapLoading" class="map-context-card">
              <div class="map-context-head">
                <div>
                  <h3>通勤与周边</h3>
                  <p>{{ mapContext?.summary || '按当前房源读取高德周边与通勤信息' }}</p>
                </div>
                <el-button plain icon="Refresh" @click="loadSelectedMapContext">刷新</el-button>
              </div>

              <div class="commute-bar">
                <el-input
                  v-model="commuteForm.destination"
                  clearable
                  placeholder="输入公司、学校或地铁站"
                  @keyup.enter="runCommuteEstimate"
                />
                <el-segmented v-model="commuteForm.mode" :options="commuteModeOptions" />
                <el-button type="primary" :disabled="!commuteForm.destination.trim()" @click="runCommuteEstimate">估算通勤</el-button>
              </div>

              <div v-if="mapContext?.route" class="route-summary">
                <strong>{{ routeSummary(mapContext.route) }}</strong>
                <span v-if="mapContext.route.distance">{{ formatDistance(mapContext.route.distance) }}</span>
                <span v-if="mapContext.route.walkingDistance">步行 {{ formatDistance(mapContext.route.walkingDistance) }}</span>
                <span v-if="mapContext.route.cost">约 {{ mapContext.route.cost }} 元</span>
              </div>

              <div v-if="nearbyGroups.length" class="nearby-grid">
                <article v-for="group in nearbyGroups" :key="group.label">
                  <div class="nearby-grid__head">
                    <strong>{{ group.label }}</strong>
                    <span>{{ group.fetched || (group.pois || []).length || 0 }} 条</span>
                  </div>
                  <ul>
                    <li v-for="poi in (group.pois || []).slice(0, 3)" :key="`${group.label}-${poi.name}-${poi.distance}`">
                      <span>{{ poi.name }}</span>
                      <em>{{ formatDistance(poi.distance) }}</em>
                    </li>
                  </ul>
                  <small v-if="group.truncated">已达单类 200 条上限</small>
                  <small v-else>搜索半径 {{ formatDistance(group.radius || 1500) }}</small>
                </article>
              </div>

              <p v-else-if="mapContext && !mapContext.configured" class="muted-tip">地图服务未配置，请在服务端设置 AMAP_API_KEY 后重试。</p>
              <p v-else class="muted-tip">暂无周边数据，可以刷新或完善房源地址/坐标后再试。</p>
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
                <el-button v-if="workMode === 'owner'" type="primary" icon="Plus" @click="openTransaction('ownerCreateHouse')">
                  新建房源
                </el-button>
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
                <el-button type="primary" icon="ChatLineRound" @click="contactHouseResponsibleFromSelected">联系负责人</el-button>
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

            <section v-if="workMode === 'owner' && selected.houseId && ['3', 3].includes(recordStatus(selected))" class="operation-card">
              <h3>当前房源审核</h3>
              <div class="panel-actions">
                <el-button plain @click="openTransaction('ownerSubmitAudit')">重新提交审核</el-button>
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

            <section
              v-if="workMode === 'tenant' && (selected.favoriteId || selected.appointmentId || selected.intentionId || selected.houseId)"
              class="operation-card"
            >
              <h3>当前房源记录</h3>
              <div class="panel-actions">
                <el-button v-if="selected.favoriteId" plain @click="cancelFavoriteFromSelected">取消收藏</el-button>
                <el-button v-if="selected.appointmentId" plain @click="cancelAppointmentFromSelected">取消预约</el-button>
                <el-button v-if="selected.intentionId" plain @click="abandonIntentionFromSelected">放弃意向</el-button>
                <el-button v-if="selected.houseId" plain @click="loadTenantHouseDetailFromSelected">查看房源详情</el-button>
              </div>
            </section>

            <section v-if="workMode === 'agent' && selected.houseId" class="operation-card">
              <h3>当前房源记录</h3>
              <div class="panel-actions">
                <el-button v-if="selected.houseId" plain @click="loadAgentHouseDetailFromSelected">查看房源详情</el-button>
              </div>
            </section>

          </template>

          <template v-else>
            <div class="empty-workspace">
              <el-empty :description="emptyDescription" :image-size="130" />
            </div>

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
      <section class="agent-workspace">
        <aside class="agent-conversation-panel">
          <div class="agent-conversation-head">
            <div>
              <span>会话记忆</span>
              <h2>AI 对话</h2>
            </div>
            <el-button circle plain :icon="Plus" :loading="conversationLoading" @click="startNewAiConversation" />
          </div>

          <div v-loading="conversationLoading" class="agent-conversation-list">
            <button
              v-for="item in aiConversations"
              :key="item.conversationId"
              class="conversation-item"
              :class="{ active: item.conversationId === activeConversationId }"
              @click="selectAiConversation(item.conversationId)"
            >
              <strong>{{ item.title || '新对话' }}</strong>
              <span>{{ item.lastMessage || '还没有消息' }}</span>
              <small>{{ conversationTime(item) }}</small>
              <el-button
                class="conversation-delete"
                text
                circle
                :icon="Delete"
                @click.stop="removeAiConversation(item)"
              />
            </button>
            <el-empty v-if="!conversationLoading && !aiConversations.length" description="暂无会话" :image-size="72" />
          </div>
        </aside>

        <aside class="agent-record-panel">
          <div class="agent-record-head">
            <div>
              <span>{{ aiAssistantProfile.badge }}</span>
              <h2>选择业务上下文</h2>
            </div>
            <em>{{ filteredRecords.length }} 条</em>
          </div>

          <div v-if="isAdmin" class="admin-mode-badge">
            合规审核
          </div>
          <div v-else class="role-tabs">
            <button
              v-for="item in roleModeOptions"
              :key="item.value"
              :class="{ active: workMode === item.value }"
              @click="changeWorkMode(item.value)"
            >
              {{ item.shortLabel }}
            </button>
          </div>

          <div class="agent-record-search">
            <el-input v-model="filters.keyword" clearable placeholder="搜索房源、城市、编号、用户ID">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-select v-model="filters.status" clearable placeholder="全部状态">
              <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-button plain icon="Refresh" @click="refreshMode">刷新</el-button>
          </div>

          <div v-loading="loading" class="records agent-records">
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

        <section class="agent-center" :class="{ 'has-chat': agentMessages.length > 1 }">
          <div class="agent-hero">
            <h1>{{ aiAssistantProfile.title }}</h1>
            <span class="agent-summary">{{ aiAssistantProfile.summary }}</span>
          </div>

          <section class="agent-context-card" :class="{ empty: !selected }">
            <template v-if="selected">
              <div class="agent-context-main">
                <span>当前上下文</span>
                <h2>{{ recordTitle(selected) }}</h2>
                <p>{{ recordSubtitle(selected) }}</p>
                <div class="detail-tags">
                  <el-tag :type="tagType(recordStatus(selected), selected)" effect="plain">{{ recordStatusLabel(selected) }}</el-tag>
                  <el-tag effect="plain">{{ recordTypeLabel(selected) }}</el-tag>
                  <el-tag v-if="selected.houseId" effect="plain">房源 {{ selected.houseId }}</el-tag>
                </div>
              </div>
              <div class="agent-context-actions">
                <strong>{{ priceText(selected) }}</strong>
                <el-button plain @click="openSelectedDetail">详情</el-button>
                <el-button type="primary" plain @click="agentInput = `请基于当前${recordTypeLabel(selected)}给出下一步建议`">问智能体</el-button>
              </div>
            </template>
            <template v-else>
              <div class="agent-context-main">
                <span>当前上下文</span>
                <h2>先从左侧选择房源或业务记录</h2>
                <p>选中后，智能体会自动带上该记录的房源、合同、预约或委托信息来回答。</p>
              </div>
            </template>
          </section>

          <div class="agent-thread">
            <div v-for="(item, index) in agentMessages" :key="index" :class="['agent-message', item.role]">
              <div class="message-content">{{ item.content }}</div>
              <div v-if="item.intentLabel || item.toolCalls?.length || item.collaboration" class="ai-trace">
                <span v-if="item.intentLabel">意图：{{ item.intentLabel }}</span>
                <span v-if="item.collaboration">主管协作：{{ collaborationSummary(item.collaboration) }}</span>
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
              :placeholder="selected ? `围绕「${recordTitle(selected)}」提问，Ctrl + Enter 发送` : aiAssistantProfile.placeholder"
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
      </section>
    </main>

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

    <AiConsoleDrawer
      v-model="aiConsoleOpen"
      v-model:knowledge-form="knowledgeForm"
      :capabilities="aiCapabilities"
      :workspace-summary="workspaceSummary"
      :capability-chips="aiCapabilityChips"
      :knowledge-source-text="knowledgeSourceText"
      :latest-task="latestAiTask"
      :house-document="aiHouseDocument"
      :knowledge-submitting="knowledgeSubmitting"
      :knowledge-seeding="knowledgeSeeding"
      :index-tasks="aiIndexTasks"
      :can-run-full-sync="canRunFullAiSync"
      :selected-house-id="selected?.houseId"
      @refresh="refreshAiConsole"
      @inspect-house-document="inspectAiHouseDocumentFromSelected"
      @create-house-index-task="createAiIndexTaskFromSelected"
      @create-full-index-task="createFullAiIndexTask"
      @process-pending-tasks="processPendingAiTasks"
      @seed-knowledge="seedKnowledgeBase"
      @submit-knowledge="submitKnowledgeDocument"
      @reset-knowledge="resetKnowledgeForm"
      @process-task="processAiTask"
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
          <el-form-item label="地图定位">
            <div class="owner-location-tool">
              <div class="owner-location-tool__actions">
                <el-button type="primary" icon="MapLocation" :loading="ownerLocationLoading" @click="openOwnerLocationPicker">
                  {{ hasOwnerLocation ? '重新选择地图位置' : '打开地图选点' }}
                </el-button>
                <el-button v-if="hasOwnerLocation" text icon="Position" @click="openOwnerLocationMap">高德地图查看</el-button>
              </div>
              <div class="owner-location-preview" :class="{ active: hasOwnerLocation }">
                <strong>{{ ownerLocationTitle }}</strong>
                <span>{{ ownerLocationText }}</span>
                <small v-if="hasOwnerLocation">经度 {{ ownerHouse.longitude }} / 纬度 {{ ownerHouse.latitude }}</small>
              </div>
            </div>
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
            <ImageUpload v-model="ownerHouse.imageUrls" action="/rental/owner/houses/images/upload" :limit="8" />
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
          <el-form-item v-if="transactionDialog.type === 'ownerEntrust'" label="委托中介" required>
            <el-select
              v-model="ownerEntrust.agentId"
              filterable
              clearable
              :loading="ownerAgentLoading"
              placeholder="选择可委托中介"
            >
              <el-option
                v-for="agent in ownerCandidateAgents"
                :key="agent.userId"
                :label="agentDisplayName(agent)"
                :value="agent.userId"
              >
                <div class="agent-option">
                  <strong>{{ agentDisplayName(agent) }}</strong>
                  <span>{{ agent.phonenumber || agent.userName || '暂无联系方式' }}</span>
                </div>
              </el-option>
            </el-select>
            <p v-if="!ownerAgentLoading && !ownerCandidateAgents.length" class="muted-tip">暂无可委托中介，可能已存在待确认或生效中的委托。</p>
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

    <el-dialog
      v-model="ownerLocationPicker.visible"
      title="地图确认房源位置"
      width="760px"
      append-to-body
      destroy-on-close
      class="owner-map-dialog"
      @opened="initOwnerLocationPicker"
      @closed="destroyOwnerLocationPicker"
    >
      <div class="owner-map-mode">
        <el-segmented v-model="ownerLocationPicker.provider" :options="ownerMapProviderOptions" @change="switchOwnerMapProvider" />
        <span>{{ ownerLocationPicker.provider === 'amap' ? '高德地图适合国内地址搜索' : '全球地图可直接点选经纬度' }}</span>
      </div>
      <div class="owner-map-search">
        <el-input v-model="ownerLocationPicker.keyword" clearable placeholder="输入小区、道路或门牌号" @keyup.enter="searchOwnerLocationOnMap" />
        <el-button type="primary" icon="Search" :loading="ownerLocationPicker.loading" @click="searchOwnerLocationOnMap">搜索定位</el-button>
        <el-button plain icon="Aim" :loading="ownerLocationPicker.loading" @click="locateOwnerPickerByCurrentPosition">当前位置</el-button>
      </div>
      <div class="owner-coordinate-inputs">
        <el-input v-model="ownerLocationPicker.longitude" placeholder="经度，例如 121.506377" />
        <el-input v-model="ownerLocationPicker.latitude" placeholder="纬度，例如 31.245105" />
        <el-button plain icon="Position" @click="applyOwnerManualCoordinate">应用坐标</el-button>
      </div>
      <div ref="ownerMapContainerRef" class="owner-map-container"></div>
      <div class="owner-map-preview">
        <strong>{{ ownerLocationPicker.address || ownerLocationLabel || ownerFullAddress() || '点击地图选择房源位置' }}</strong>
        <span v-if="ownerLocationPicker.longitude && ownerLocationPicker.latitude">
          经度 {{ ownerLocationPicker.longitude }} / 纬度 {{ ownerLocationPicker.latitude }}
        </span>
        <span v-else>可点击地图或拖动标记确认坐标。</span>
      </div>
      <template #footer>
        <el-button @click="ownerLocationPicker.visible = false">取消</el-button>
        <el-button type="primary" :disabled="!ownerLocationPicker.longitude || !ownerLocationPicker.latitude" @click="confirmOwnerLocationPicker">
          确认位置
        </el-button>
      </template>
    </el-dialog>

    <FloatingAiAssistant
      v-model:open="floatingAiOpen"
      v-model:input="floatingAiInput"
      :loading="floatingAiLoading"
      :messages="floatingAiMessages"
      :presets="floatingAiPresets"
      @open-tools="openBusinessTools"
      @open-messages="openMessagePanel"
      @send="sendFloatingAiMessage"
    />

    <el-drawer
      v-model="businessToolsOpen"
      direction="rtl"
      size="420px"
      append-to-body
      class="business-tools-drawer"
    >
      <div class="business-tools-panel">
        <header class="business-tools-hero">
          <div class="tool-hero-icon">
            <el-icon><Document /></el-icon>
          </div>
          <div>
            <span>{{ roleLabel }}</span>
            <h2>业务工具</h2>
            <p>常用动作、历史记录和智能体能力集中在这里。</p>
          </div>
        </header>

        <section v-if="businessAiCard" class="business-tool-card ai-card">
          <div class="tool-section-head">
            <div>
              <span>AI 辅助</span>
              <h3>{{ businessAiCard.title }}</h3>
            </div>
            <el-tag effect="plain" type="success">Smart Agent</el-tag>
          </div>
          <p>{{ businessAiCard.description }}</p>
          <div class="business-tool-actions">
            <el-input
              v-if="workMode === 'tenant'"
              v-model="aiRecommend.query"
              clearable
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

        <section class="business-tool-card compact">
          <div class="tool-section-head">
            <div>
              <span>业务与历史</span>
              <h3>快捷入口</h3>
            </div>
          </div>
          <div class="business-tool-grid">
            <button @click="runBusinessTool(() => switchPage('business'))">业务台</button>
            <button v-if="workMode === 'tenant'" @click="runBusinessTool(loadTenantFavoritesFromWorkspace)">我的收藏</button>
            <button v-if="workMode === 'tenant'" @click="runBusinessTool(loadTenantAppointmentsFromWorkspace)">我的预约</button>
            <button v-if="workMode === 'tenant'" @click="runBusinessTool(loadTenantIntentionsFromWorkspace)">我的意向</button>
            <button v-if="workMode === 'tenant'" @click="runBusinessTool(loadTenantContractsFromWorkspace)">我的合同</button>
            <button v-else-if="visibleWorkModes.includes('contract')" @click="runBusinessTool(openContractsShortcut)">我的合同</button>
            <button v-if="workMode === 'tenant'" @click="runBusinessTool(loadPortalPublicHouses)">公开房源</button>
            <button v-if="workMode === 'owner'" @click="runBusinessTool(() => openTransaction('ownerCreateHouse'))">新建房源</button>
            <button v-if="workMode === 'owner'" @click="runBusinessTool(loadOwnerHistory)">历史委托</button>
            <button v-if="workMode === 'agent'" @click="runBusinessTool(loadAgentCandidateWorkspace)">可承接房源</button>
            <button v-if="workMode === 'agent'" @click="runBusinessTool(loadAgentHistory)">我的受托房源</button>
            <button v-if="workMode === 'contract'" @click="runBusinessTool(refreshMode)">刷新合同</button>
            <button v-if="canOpenAiConsole" @click="runBusinessTool(openAiConsole)">AI 工作台</button>
            <button @click="runBusinessTool(refreshCurrent)">刷新</button>
            <button @click="runBusinessTool(openMessagePanel)">消息</button>
          </div>
        </section>

        <section v-if="aiAssistantProfile.capabilities?.length" class="business-tool-card compact">
          <div class="tool-section-head">
            <div>
              <span>当前智能体能力</span>
              <h3>可直接询问</h3>
            </div>
          </div>
          <div class="business-tool-tags">
            <button v-for="item in aiAssistantProfile.capabilities" :key="item" @click="agentInput = item; openFloatingAi()">
              {{ item }}
            </button>
          </div>
        </section>
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="PortalHome">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, Delete, Document, Plus, Promotion, Search } from '@element-plus/icons-vue'
import useUserStore from '@/store/modules/user'
import AiConsoleDrawer from './components/AiConsoleDrawer.vue'
import FloatingAiAssistant from './components/FloatingAiAssistant.vue'
import {
  aiAssistantProfiles,
  businessAiCards,
  floatingAiPresets,
  knowledgeSourceLabels,
  modeMeta,
  quickTags,
  roleConfig,
  transactionConfigs
} from './config'
import {
  formatMoney,
  formatPercent,
  formatValue,
  intentionLevelLabel,
  normalizeAiResponse,
  normalizeAiResponsePayload
} from './utils'
import {
  listTenantHouses,
  getTenantHouse,
  listTenantFavorites,
  favoriteTenantHouse,
  cancelTenantFavorite,
  contactTenantHouseResponsible,
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
  listOwnerCandidateAgents,
  listOwnerEntrusts,
  listOwnerEntrustApplications,
  entrustOwnerHouse,
  confirmOwnerEntrust,
  rejectOwnerEntrust
} from '@/api/portal/owner'
import {
  createAmapLngLat,
  createAmapPickerMap,
  geocodeAddressByAmapJs,
  getCurrentPositionByAmapJs,
  isAmapJsConfigured,
  reverseGeocodeByAmapJs
} from '@/utils/amap'
import { createGlobalLngLat, createGlobalPickerMap } from '@/utils/globalMap'
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
  listAiConversations,
  createAiConversation,
  getAiConversation,
  deleteAiConversation,
  updateAiConversationContext,
  summarizeAiConversation,
  sendAiConversationMessage,
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
import { listPortalHouses, getPortalHouseDetail, getPortalHouseMapContext } from '@/api/portal/house'
import { auditHouse, listAuditHouse } from '@/api/system/houseBusiness'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()
const pageMode = ref('business')
const loading = ref(false)
const records = ref([])
const selected = ref(null)
const detailOpen = ref(false)
const aiConsoleOpen = ref(false)
const filters = reactive({ keyword: '', status: '' })
const transactionDialog = reactive({ visible: false, type: '', submitting: false, aiLoading: false, row: null })
const tenantAppointment = reactive({ appointmentTime: '', remark: '' })
const tenantIntention = reactive({ intentionLevel: '2', note: '' })
const ownerHouse = reactive({
  title: '',
  city: '',
  district: '',
  address: '',
  longitude: null,
  latitude: null,
  rentAmount: null,
  area: null,
  operationMode: '0',
  imageUrls: '',
  description: ''
})
const ownerLocationLoading = ref(false)
const ownerLocationLabel = ref('')
const ownerMapContainerRef = ref(null)
const ownerLocationPicker = reactive({
  visible: false,
  loading: false,
  provider: 'amap',
  keyword: '',
  longitude: '',
  latitude: '',
  address: ''
})
let ownerPickerMap = null
let ownerPickerMarker = null
let ownerPickerMapProvider = ''
const ownerEntrust = reactive({ agentId: '', entrustScope: '发布,预约,带看,签约', commissionRate: 0.02 })
const ownerCandidateAgents = ref([])
const ownerAgentLoading = ref(false)
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
const mapLoading = ref(false)
const mapContext = ref(null)
const commuteForm = reactive({ destination: '', mode: 'transit' })
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
const aiConversations = ref([])
const activeConversationId = ref(null)
const conversationLoading = ref(false)
const floatingAiOpen = ref(false)
const businessToolsOpen = ref(false)
const floatingAiLoading = ref(false)
const floatingAiInput = ref('')
const floatingAiMessages = ref([
  { role: 'assistant', content: '我是右下角 AI 助手，可以随时协助找房推荐、合同摘要、房源文案和业务跟进。' }
])
const commuteModeOptions = [
  { label: '公交', value: 'transit' },
  { label: '驾车', value: 'driving' },
  { label: '步行', value: 'walking' }
]
let recordsRequestSeq = 0
let aiConsoleRequestSeq = 0
let mapContextRequestSeq = 0

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
const isSystemAdmin = computed(() => roles.value.includes('admin'))
const canManageAiIndex = computed(() => isSystemAdmin.value)
const canRunFullAiSync = computed(() => isSystemAdmin.value)
const canOpenAiConsole = computed(() => isSystemAdmin.value)
const canManageContractLifecycle = computed(() => roles.value.includes('owner') || roles.value.includes('agent'))
const canUseTenantMap = computed(() => workMode.value === 'tenant' && selected.value?.houseId && isTenantHouseContext(selected.value))
const amapJsReady = computed(() => isAmapJsConfigured())
const ownerMapProviderOptions = computed(() => [
  { label: '高德', value: 'amap', disabled: !amapJsReady.value },
  { label: '全球地图', value: 'global' }
])
const transactionMeta = computed(() => transactionConfigs[transactionDialog.type] || { title: '业务处理', submitText: '提交', form: 'none' })
const latestAiTask = computed(() => aiIndexTasks.value[0] || null)
const detailDrawerTitle = computed(() => selected.value ? `${recordTypeLabel(selected.value)}详情` : '详情')
const businessAiCard = computed(() => businessAiCards[workMode.value] || null)
const aiAssistantProfile = computed(() => aiAssistantProfiles[workMode.value] || aiAssistantProfiles.tenant)
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
  const sources = aiCapabilities.knowledgeSources || []
  if (!sources.length) return '合同、政策、FAQ、聊天记录、企业制度'
  return sources.map(item => knowledgeSourceLabels[item] || item).join('、')
})
const previewFields = computed(() => detailFields.value.slice(0, 6))
const nearbyGroups = computed(() => mapContext.value?.nearbyGroups || [])
const apiBaseUrl = import.meta.env.VITE_APP_BASE_API || ''
const hasOwnerLocation = computed(() => ownerHouse.longitude !== null && ownerHouse.longitude !== '' && ownerHouse.latitude !== null && ownerHouse.latitude !== '')
const ownerLocationTitle = computed(() => hasOwnerLocation.value ? '已确认房源地图位置' : '请在地图弹窗中选择房源位置')
const ownerLocationText = computed(() => {
  if (hasOwnerLocation.value) {
    return ownerLocationLabel.value || ownerFullAddress()
  }
  return '户主建档必须先打开地图选点，坐标会用于租客通勤、周边配套和 AI 分析。'
})
const detailImageUrls = computed(() => {
  const raw = selected.value?.imageUrls || selected.value?.images || selected.value?.imageList
  if (Array.isArray(raw)) {
    return raw.map(item => resolveDetailImageUrl(typeof item === 'string' ? item : item?.imageUrl || item?.url || item?.fileName)).filter(Boolean)
  }
  if (typeof raw === 'string' && raw.trim()) {
    return raw.split(',').map(item => resolveDetailImageUrl(item.trim())).filter(Boolean)
  }
  return []
})

function resolveDetailImageUrl(url) {
  if (!url) return ''
  if (/^([a-z][a-z\d+\-.]*:)?\/\//i.test(url)) return url
  if (apiBaseUrl && url.startsWith(apiBaseUrl)) return url
  return `${apiBaseUrl}${url.startsWith('/') ? url : `/${url}`}`
}
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
      { key: 'longitude', label: '经度' },
      { key: 'latitude', label: '纬度' },
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
      { key: 'address', label: '详细地址' },
      { key: 'longitude', label: '经度' },
      { key: 'latitude', label: '纬度' },
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

watch(filteredRecords, list => {
  if (!list.some(item => recordKey(item) === recordKey(selected.value))) {
    selected.value = list[0] || null
  }
})

watch(
  () => [workMode.value, selected.value?.houseId, selected.value?.appointmentId, selected.value?.intentionId, selected.value?.contractId],
  () => {
    if (canUseTenantMap.value) {
      loadSelectedMapContext({ silent: true })
    } else {
      resetMapContext()
    }
  }
)

watch(
  () => [activeConversationId.value, selected.value ? recordKey(selected.value) : ''],
  () => {
    syncActiveConversationContext()
  }
)

watch(
  () => [ownerHouse.city, ownerHouse.district, ownerHouse.address],
  () => {
    if (hasOwnerLocation.value) {
      resetOwnerLocation()
    }
  }
)

onMounted(() => {
  if (isAdmin.value) {
    pageMode.value = 'business'
  }
  const shouldOpenAiConsole = route.query?.ai === 'console' && canOpenAiConsole.value
  refreshMode()
  refreshAiOverview({ includeTasks: canManageAiIndex.value && !shouldOpenAiConsole })
  seedAgentWelcome()
  if (shouldOpenAiConsole) {
    nextTick(openAiConsole)
  }
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
  if (mode === 'agent') {
    loadAiConversationList()
  }
}

async function changeWorkMode(mode) {
  if (!visibleWorkModes.value.includes(mode)) return
  if (workMode.value === mode) {
    await refreshMode()
    return
  }
  workMode.value = mode
  await refreshMode()
  if (pageMode.value === 'agent') {
    await loadAiConversationList()
  }
}

async function showWorkMode(mode, loader) {
  if (!visibleWorkModes.value.includes(mode)) {
    ElMessage.warning('当前角色暂无该入口')
    return
  }
  if (workMode.value !== mode) {
    workMode.value = mode
    await nextTick()
  }
  if (loader) {
    await loader()
  } else {
    await refreshMode()
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
    await showWorkMode('admin')
    return
  }
  if (roles.value.includes('owner')) {
    await showWorkMode('owner')
    openTransaction('ownerCreateHouse')
    return
  }
  if (roles.value.includes('agent')) {
    await showWorkMode('agent', loadAgentCandidateWorkspace)
    return
  }
  await showWorkMode('tenant', loadTenantAppointmentsFromWorkspace)
}

async function openContractsShortcut() {
  pageMode.value = 'business'
  if (!visibleWorkModes.value.includes('contract')) {
    ElMessage.warning('当前角色暂无合同入口')
    return
  }
  await showWorkMode('contract')
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
}

async function refreshAiConsole() {
  const requestId = ++aiConsoleRequestSeq
  await refreshAiOverview({ includeTasks: true })
  if (requestId === aiConsoleRequestSeq && selected.value?.houseId) {
    await inspectAiHouseDocumentFromSelected({ silent: true })
  }
}

function openMessagePanel() {
  router.push('/portal/chat')
}

async function refreshCurrent() {
  await Promise.all([
    refreshMode(),
    refreshAiOverview({ includeTasks: canManageAiIndex.value })
  ])
}

async function refreshMode() {
  if (!visibleWorkModes.value.includes(workMode.value)) {
    workMode.value = visibleWorkModes.value[0]
  }
  loading.value = true
  const requestId = ++recordsRequestSeq
  const mode = workMode.value
  try {
    const loaders = {
      admin: loadAdminRecords,
      tenant: loadTenantRecords,
      owner: loadOwnerRecords,
      agent: loadAgentRecords,
      contract: loadContractRecords
    }
    const rows = await loaders[mode]()
    if (requestId !== recordsRequestSeq || mode !== workMode.value) return
    applyWorkspaceRows(rows)
  } finally {
    if (requestId === recordsRequestSeq) {
      loading.value = false
    }
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

function rowsOf(response) {
  if (Array.isArray(response)) return response
  return response?.rows || response?.data || []
}

function agentDisplayName(agent) {
  if (!agent) return '中介'
  return agent.nickName || agent.userName || `中介 #${agent.userId}`
}

function payloadOf(response) {
  return response?.data ?? response
}

function applyWorkspaceRows(rows) {
  records.value = Array.isArray(rows) ? rows : []
  selected.value = filteredRecords.value[0] || records.value[0] || null
}

async function loadWorkspaceRows(loader, mapper = item => item) {
  loading.value = true
  const requestId = ++recordsRequestSeq
  try {
    const response = await loader({})
    if (requestId !== recordsRequestSeq) return
    applyWorkspaceRows(rowsOf(response).map(mapper))
  } finally {
    if (requestId === recordsRequestSeq) {
      loading.value = false
    }
  }
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

function resetMapContext() {
  mapContextRequestSeq += 1
  mapContext.value = null
  mapLoading.value = false
  commuteForm.destination = ''
  commuteForm.mode = 'transit'
}

async function loadSelectedMapContext({ silent = false } = {}) {
  if (!canUseTenantMap.value) {
    resetMapContext()
    return
  }
  const houseId = selected.value.houseId
  const requestId = ++mapContextRequestSeq
  mapLoading.value = true
  try {
    const destination = commuteForm.destination.trim()
    const res = await getPortalHouseMapContext(houseId, {
      destination: destination || undefined,
      mode: commuteForm.mode
    })
    if (requestId !== mapContextRequestSeq || selected.value?.houseId !== houseId) return
    mapContext.value = {
      ...(res?.data || {}),
      destination,
      mode: commuteForm.mode
    }
    if (!silent && mapContext.value?.configured === false) {
      ElMessage.warning('地图服务未配置，请在服务端设置高德 Key')
    }
  } catch (error) {
    if (requestId === mapContextRequestSeq) {
      mapContext.value = { configured: false, summary: '地图服务暂不可用', nearbyGroups: [] }
    }
    if (!silent) {
      ElMessage.error('通勤与周边查询失败')
    }
  } finally {
    if (requestId === mapContextRequestSeq) {
      mapLoading.value = false
    }
  }
}

async function runCommuteEstimate() {
  if (!canUseTenantMap.value) {
    ElMessage.warning('请先选择租户可见房源')
    return
  }
  if (!commuteForm.destination.trim()) {
    ElMessage.warning('请输入目的地')
    return
  }
  await loadSelectedMapContext()
}

async function locateOwnerHouse({ silent = false } = {}) {
  if (!ownerHouse.city || !ownerHouse.district || !ownerHouse.address) {
    if (!silent) ElMessage.warning('请先填写城市、区域和详细地址')
    return false
  }
  if (!isAmapJsConfigured()) {
    if (!silent) ElMessage.warning('请先配置前端高德 JS API Key：VITE_AMAP_JS_API_KEY')
    return false
  }
  ownerLocationLoading.value = true
  try {
    const data = await geocodeAddressByAmapJs({
      city: ownerHouse.city,
      address: ownerFullAddress()
    })
    if (!data?.success || !data.longitude || !data.latitude) {
      if (!silent) ElMessage.warning('未解析到房源坐标，请补全门牌、小区或道路信息')
      return false
    }
    ownerHouse.longitude = String(data.longitude)
    ownerHouse.latitude = String(data.latitude)
    ownerLocationLabel.value = data.address || ownerFullAddress()
    if (data.district && !ownerHouse.district) ownerHouse.district = data.district
    if (!silent) ElMessage.success('已通过高德地图锁定房源坐标')
    return true
  } catch (error) {
    if (!silent) ElMessage.error('地图定位失败，请检查高德 JS API Key、域名白名单或地址信息')
    return false
  } finally {
    ownerLocationLoading.value = false
  }
}

async function locateOwnerHouseByCurrentPosition() {
  if (!isAmapJsConfigured()) {
    ElMessage.warning('请先配置前端高德 JS API Key：VITE_AMAP_JS_API_KEY')
    return false
  }
  ownerLocationLoading.value = true
  try {
    const position = await getCurrentPositionByAmapJs()
    const longitude = position.longitude
    const latitude = position.latitude
    let addressInfo = position
    try {
      addressInfo = await reverseGeocodeByAmapJs({ longitude, latitude, city: ownerHouse.city || position.city })
    } catch (error) {
      addressInfo = position
    }

    ownerHouse.longitude = String(longitude)
    ownerHouse.latitude = String(latitude)
    if (!ownerHouse.city && addressInfo.city) ownerHouse.city = addressInfo.city
    if (!ownerHouse.district && addressInfo.district) ownerHouse.district = addressInfo.district
    if (!ownerHouse.address && addressInfo.address) ownerHouse.address = addressInfo.address
    ownerLocationLabel.value = addressInfo.address || ownerFullAddress() || '当前位置'
    ElMessage.success('已根据浏览器当前位置锁定房源坐标')
    return true
  } catch (error) {
    ElMessage.error('当前位置获取失败，请允许浏览器定位并检查高德 JS API 配置')
    return false
  } finally {
    ownerLocationLoading.value = false
  }
}

function openOwnerLocationPicker() {
  ownerLocationPicker.provider = amapJsReady.value ? 'amap' : 'global'
  ownerLocationPicker.keyword = ownerFullAddress()
  ownerLocationPicker.longitude = hasOwnerLocation.value ? String(ownerHouse.longitude) : ''
  ownerLocationPicker.latitude = hasOwnerLocation.value ? String(ownerHouse.latitude) : ''
  ownerLocationPicker.address = ownerLocationLabel.value || ownerFullAddress()
  ownerLocationPicker.visible = true
}

async function initOwnerLocationPicker() {
  if (!ownerMapContainerRef.value) return
  ownerLocationPicker.loading = true
  try {
    await createOwnerPickerMap()
  } catch (error) {
    if (ownerLocationPicker.provider === 'amap') {
      ownerLocationPicker.provider = 'global'
      ElMessage.warning('高德地图不可用，已切换到全球地图手动选点')
      await createOwnerPickerMap()
    } else {
      ElMessage.error('全球地图加载失败，请手动输入经纬度后应用')
    }
  } finally {
    ownerLocationPicker.loading = false
  }
}

async function createOwnerPickerMap() {
  if (ownerLocationPicker.provider === 'amap' && !amapJsReady.value) {
    ownerLocationPicker.provider = 'global'
  }
  let longitude = ownerLocationPicker.longitude
  let latitude = ownerLocationPicker.latitude
  if (ownerLocationPicker.provider === 'amap' && (!longitude || !latitude) && ownerFullAddress()) {
    try {
      const data = await geocodeAddressByAmapJs({ city: ownerHouse.city, address: ownerFullAddress() })
      longitude = data.longitude
      latitude = data.latitude
      ownerLocationPicker.address = data.address || ownerFullAddress()
    } catch (error) {
      ownerLocationPicker.provider = 'global'
      ElMessage.warning('地址解析不到，已切换到全球地图，请直接点击或输入经纬度')
    }
  }

  if (ownerLocationPicker.provider === 'amap') {
    const picker = await createAmapPickerMap(ownerMapContainerRef.value, { longitude, latitude })
    ownerPickerMap = picker.map
    ownerPickerMarker = picker.marker
    ownerPickerMapProvider = 'amap'
    ownerPickerMap.on('click', event => updateOwnerPickerPosition(event.lnglat))
    ownerPickerMarker.on('dragend', event => updateOwnerPickerPosition(event.lnglat))
    if (longitude && latitude) {
      await updateOwnerPickerPosition(createAmapLngLat(longitude, latitude), { move: true })
    }
    return
  }

  const picker = await createGlobalPickerMap(ownerMapContainerRef.value, { longitude, latitude })
  ownerPickerMap = picker.map
  ownerPickerMarker = picker.marker
  ownerPickerMapProvider = 'global'
  ownerPickerMap.on('click', event => updateOwnerPickerPosition({ lng: event.latlng.lng, lat: event.latlng.lat }))
  ownerPickerMarker.on('dragend', event => {
    const latlng = event.target.getLatLng()
    updateOwnerPickerPosition({ lng: latlng.lng, lat: latlng.lat })
  })
  if (longitude && latitude) {
    await updateOwnerPickerPosition(createGlobalLngLat(longitude, latitude), { move: true })
  }
}

function destroyOwnerLocationPicker() {
  if (ownerPickerMap) {
    try {
      if (ownerPickerMapProvider === 'global' && typeof ownerPickerMap.remove === 'function') {
        ownerPickerMap.remove()
      } else if (typeof ownerPickerMap.destroy === 'function') {
        ownerPickerMap.destroy()
      } else if (typeof ownerPickerMap.remove === 'function') {
        ownerPickerMap.remove()
      }
    } catch (error) {
      console.warn('Owner map destroy failed:', error)
    }
  }
  ownerPickerMap = null
  ownerPickerMarker = null
  ownerPickerMapProvider = ''
  if (ownerMapContainerRef.value) {
    ownerMapContainerRef.value.replaceChildren()
    delete ownerMapContainerRef.value._leaflet_id
  }
}

async function switchOwnerMapProvider() {
  if (!ownerLocationPicker.visible || !ownerMapContainerRef.value) return
  if (ownerLocationPicker.provider === 'amap' && !amapJsReady.value) {
    ownerLocationPicker.provider = 'global'
    ElMessage.warning('请先配置前端高德 JS API Key：VITE_AMAP_JS_API_KEY')
    return
  }
  const requestedProvider = ownerLocationPicker.provider
  destroyOwnerLocationPicker()
  await nextTick()
  ownerLocationPicker.loading = true
  try {
    await createOwnerPickerMap()
  } catch (error) {
    console.error('Owner map switch failed:', error)
    destroyOwnerLocationPicker()
    if (requestedProvider === 'amap') {
      ownerLocationPicker.provider = 'global'
      try {
        await nextTick()
        await createOwnerPickerMap()
        ElMessage.warning('高德地图不可用，已切换到全球地图手动选点')
        return
      } catch (fallbackError) {
        console.error('Owner map fallback failed:', fallbackError)
      }
    }
    ElMessage.error('地图切换失败，请稍后重试或手动输入经纬度')
  } finally {
    ownerLocationPicker.loading = false
  }
}

async function updateOwnerPickerPosition(lnglat, options = {}) {
  if (!lnglat) return
  const longitude = String(lnglat.lng)
  const latitude = String(lnglat.lat)
  ownerLocationPicker.longitude = longitude
  ownerLocationPicker.latitude = latitude
  if (ownerLocationPicker.provider === 'global') {
    if (ownerPickerMarker) ownerPickerMarker.setLatLng([Number(latitude), Number(longitude)])
    if (ownerPickerMap && options.move) ownerPickerMap.setView([Number(latitude), Number(longitude)], Math.max(ownerPickerMap.getZoom(), 15))
    ownerLocationPicker.address = ownerLocationPicker.address || '已选择全球地图坐标'
    return
  }

  if (ownerPickerMarker) ownerPickerMarker.setPosition(lnglat)
  if (ownerPickerMap && options.move) ownerPickerMap.setCenter(lnglat)
  if (isAmapJsConfigured()) {
    try {
      const data = await reverseGeocodeByAmapJs({ longitude, latitude, city: ownerHouse.city })
      ownerLocationPicker.address = data.address || ownerLocationPicker.address
    } catch (error) {
      ownerLocationPicker.address = ownerLocationPicker.address || '已选择地图坐标'
    }
  }
}

async function applyOwnerManualCoordinate() {
  const longitude = Number(ownerLocationPicker.longitude)
  const latitude = Number(ownerLocationPicker.latitude)
  if (!Number.isFinite(longitude) || longitude < -180 || longitude > 180 || !Number.isFinite(latitude) || latitude < -90 || latitude > 90) {
    ElMessage.warning('请输入有效经纬度：经度 -180~180，纬度 -90~90')
    return
  }
  const lnglat = ownerLocationPicker.provider === 'global'
    ? createGlobalLngLat(longitude, latitude)
    : createAmapLngLat(longitude, latitude)
  await updateOwnerPickerPosition(lnglat || { lng: longitude, lat: latitude }, { move: true })
  ElMessage.success('已应用经纬度')
}

async function searchOwnerLocationOnMap() {
  if (ownerLocationPicker.provider === 'global') {
    ElMessage.info('全球地图模式请直接点击地图、拖动标记，或输入经纬度后应用')
    return
  }
  const keyword = ownerLocationPicker.keyword.trim()
  if (!keyword) {
    ElMessage.warning('请输入小区、道路或门牌号')
    return
  }
  ownerLocationPicker.loading = true
  try {
    const data = await geocodeAddressByAmapJs({ city: ownerHouse.city, address: keyword })
    if (!data?.longitude || !data?.latitude) {
      ElMessage.warning('未搜索到位置，已切换到全球地图，请直接点选')
      ownerLocationPicker.provider = 'global'
      destroyOwnerLocationPicker()
      await nextTick()
      await createOwnerPickerMap()
      return
    }
    ownerLocationPicker.address = data.address || keyword
    await updateOwnerPickerPosition(createAmapLngLat(data.longitude, data.latitude), { move: true })
    if (ownerPickerMap) ownerPickerMap.setZoom(16)
  } catch (error) {
    ElMessage.warning('搜索定位失败，已切换到全球地图，请直接点选')
    ownerLocationPicker.provider = 'global'
    destroyOwnerLocationPicker()
    await nextTick()
    await createOwnerPickerMap()
  } finally {
    ownerLocationPicker.loading = false
  }
}

async function locateOwnerPickerByCurrentPosition() {
  ownerLocationPicker.loading = true
  try {
    const position = ownerLocationPicker.provider === 'global'
      ? await getBrowserCurrentPosition()
      : await getCurrentPositionByAmapJs()
    ownerLocationPicker.address = position.address || ownerLocationPicker.address || '当前位置'
    const lnglat = ownerLocationPicker.provider === 'global'
      ? createGlobalLngLat(position.longitude, position.latitude)
      : createAmapLngLat(position.longitude, position.latitude)
    await updateOwnerPickerPosition(lnglat || { lng: Number(position.longitude), lat: Number(position.latitude) }, { move: true })
    if (ownerPickerMap && ownerLocationPicker.provider === 'amap') ownerPickerMap.setZoom(17)
  } catch (error) {
    ElMessage.error('当前位置获取失败，请允许浏览器定位或改用地图搜索')
  } finally {
    ownerLocationPicker.loading = false
  }
}

function getBrowserCurrentPosition() {
  if (!navigator.geolocation) {
    return Promise.reject(new Error('GEOLOCATION_UNSUPPORTED'))
  }
  return new Promise((resolve, reject) => {
    navigator.geolocation.getCurrentPosition(
      position => resolve({
        longitude: String(position.coords.longitude),
        latitude: String(position.coords.latitude),
        accuracy: position.coords.accuracy,
        address: ''
      }),
      reject,
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 30000 }
    )
  })
}

async function confirmOwnerLocationPicker() {
  ownerHouse.longitude = String(ownerLocationPicker.longitude)
  ownerHouse.latitude = String(ownerLocationPicker.latitude)
  ownerLocationLabel.value = ownerLocationPicker.address || ownerFullAddress()
  try {
    const data = await reverseGeocodeByAmapJs({
      longitude: ownerHouse.longitude,
      latitude: ownerHouse.latitude,
      city: ownerHouse.city
    })
    if (!ownerHouse.city && data.city) ownerHouse.city = data.city
    if (!ownerHouse.district && data.district) ownerHouse.district = data.district
    if (!ownerHouse.address && data.address) ownerHouse.address = data.address
    ownerLocationLabel.value = data.address || ownerLocationLabel.value
  } catch (error) {
    // Coordinates are still valid even if reverse geocoding is unavailable.
  }
  ownerLocationPicker.visible = false
  ElMessage.success('已确认房源地图位置')
}

function ownerFullAddress() {
  return [ownerHouse.city, ownerHouse.district, ownerHouse.address]
    .map(item => String(item || '').trim())
    .filter(Boolean)
    .join('')
}

function resetOwnerLocation() {
  ownerHouse.longitude = null
  ownerHouse.latitude = null
  ownerLocationLabel.value = ''
}

function resetOwnerHouseForm() {
  Object.assign(ownerHouse, {
    title: '',
    city: '',
    district: '',
    address: '',
    longitude: null,
    latitude: null,
    rentAmount: null,
    area: null,
    operationMode: '0',
    imageUrls: '',
    description: ''
  })
  ownerLocationLabel.value = ''
}

function openOwnerLocationMap() {
  if (!hasOwnerLocation.value) return
  const name = encodeURIComponent(ownerHouse.title || ownerLocationLabel.value || ownerFullAddress() || '房源位置')
  window.open(`https://uri.amap.com/marker?position=${ownerHouse.longitude},${ownerHouse.latitude}&name=${name}`, '_blank', 'noopener,noreferrer')
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

function formatDistance(value) {
  const meters = Number(value)
  if (!Number.isFinite(meters) || meters < 0) return '-'
  if (meters >= 1000) return `${(meters / 1000).toFixed(1)} km`
  return `${Math.round(meters)} m`
}

function routeSummary(route) {
  if (!route) return '暂无路线'
  if (route.summary) return route.summary
  const minutes = Math.ceil(Number(route.duration || 0) / 60)
  const modeMap = { transit: '公交', driving: '驾车', walking: '步行' }
  return `${modeMap[route.mode] || '通勤'}约 ${minutes || '-'} 分钟`
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
  router.push({
    path: '/portal/chat',
    query: {
      bizType: target.bizType || undefined,
      bizId: target.bizId || undefined,
      sessionId: target.sessionId || undefined,
      title: target.title || undefined
    }
  })
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
  } else if (type === 'ownerEntrust') {
    ownerEntrust.agentId = ''
    loadOwnerCandidateAgentsFor(row || selected.value)
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
    const shouldClose = await handler()
    if (shouldClose !== false) {
      transactionDialog.visible = false
    }
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

async function contactHouseResponsibleFromSelected() {
  if (!selected.value?.houseId) return ElMessage.warning('请选择房源')
  const res = await contactTenantHouseResponsible(selected.value.houseId)
  ElMessage.success('已打开房源咨询')
  openChatFromResponse(res, { title: '房源咨询' })
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
  await loadWorkspaceRows(listTenantFavorites, item => ({ ...item, _recordType: '我的收藏' }))
}

async function loadTenantAppointmentsFromWorkspace() {
  await loadWorkspaceRows(listTenantAppointments, item => ({ ...item, _recordType: '我的预约' }))
}

async function loadTenantIntentionsFromWorkspace() {
  await loadWorkspaceRows(listTenantIntentions, item => ({ ...item, _recordType: '我的意向' }))
}

async function loadTenantContractsFromWorkspace() {
  await loadWorkspaceRows(listTenantContracts, item => ({ ...item, _recordType: '我的合同' }))
}

async function loadTenantHouseDetailFromSelected() {
  if (!selected.value?.houseId) return ElMessage.warning('请选择房源')
  const res = await getTenantHouse(selected.value.houseId)
  selected.value = { ...(res?.data || {}), _recordType: '房源详情' }
}

async function createOwnerHouseFromForm() {
  if (!ownerHouse.title || !ownerHouse.city || !ownerHouse.district || !ownerHouse.address) {
    ElMessage.warning('请完整填写房源基础信息')
    return false
  }
  if (!hasOwnerLocation.value) {
    ElMessage.warning('请先在地图弹窗中选择并确认房源位置')
    openOwnerLocationPicker()
    return false
  }
  await createOwnerHouse(ownerHouse)
  ElMessage.success('房源已提交审核')
  resetOwnerHouseForm()
  await refreshMode()
  return true
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
  await loadWorkspaceRows(listOwnerEntrusts, item => ({ ...item, _recordType: '历史委托' }))
}

async function loadOwnerCandidateAgentsFor(row) {
  ownerCandidateAgents.value = []
  if (!row?.houseId) return
  ownerAgentLoading.value = true
  try {
    const res = await listOwnerCandidateAgents(row.houseId)
    ownerCandidateAgents.value = rowsOf(res)
  } finally {
    ownerAgentLoading.value = false
  }
}

async function inviteAgentFromSelected(row) {
  if (!row.houseId || !ownerEntrust.agentId) {
    return ElMessage.warning('请选择房源和委托中介')
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
  await loadWorkspaceRows(listAgentHouses, item => ({ ...item, _recordType: '受托房源' }))
}

async function loadAgentCandidateWorkspace() {
  await loadWorkspaceRows(listAgentCandidateHouses, item => ({ ...item, _recordType: '可申请房源' }))
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

async function refreshAiOverview({ includeTasks = false } = {}) {
  const tasks = [
    loadPortalSummary(),
    loadAiCapabilities()
  ]
  if (includeTasks) {
    tasks.push(loadAiIndexTasks())
  }
  await Promise.all(tasks)
}

async function loadPortalPublicHouses() {
  await loadWorkspaceRows(listPortalHouses, item => ({ ...item, _recordType: '公开房源' }))
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
  if (!canRunFullAiSync.value) return ElMessage.warning('只有管理员可以创建全量同步任务')
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
  await refreshAiOverview({ includeTasks: true })
  if (selected.value?.houseId) {
    await inspectAiHouseDocumentFromSelected({ silent: true })
  }
}

async function processPendingAiTasks() {
  const res = await processPendingAiIndexTasks({ limit: 5 })
  const processed = res?.data?.processed ?? 0
  ElMessage.success(`已处理 ${processed} 个索引任务`)
  await refreshAiOverview({ includeTasks: true })
  if (selected.value?.houseId) {
    await inspectAiHouseDocumentFromSelected({ silent: true })
  }
}

async function inspectAiHouseDocumentFromSelected(options = {}) {
  if (!selected.value?.houseId) return ElMessage.warning('请选择房源')
  const res = await inspectAiHouseDocument(selected.value.houseId)
  aiHouseDocument.value = res?.data || null
  if (!options.silent) {
    ElMessage.success('已读取房源文档状态')
  }
}

async function loadAiConversationList() {
  conversationLoading.value = true
  try {
    const res = await listAiConversations({ workMode: workMode.value })
    aiConversations.value = rowsOf(res)
    if (activeConversationId.value && !aiConversations.value.some(item => item.conversationId === activeConversationId.value)) {
      activeConversationId.value = null
      seedAgentWelcome()
    }
  } finally {
    conversationLoading.value = false
  }
}

async function startNewAiConversation() {
  const conversation = await ensureAiConversation({ forceNew: true })
  if (conversation?.conversationId) {
    await selectAiConversation(conversation.conversationId)
  }
}

async function ensureAiConversation(options = {}) {
  if (!options.forceNew && activeConversationId.value) {
    return aiConversations.value.find(item => item.conversationId === activeConversationId.value) || { conversationId: activeConversationId.value }
  }
  conversationLoading.value = true
  try {
    const res = await createAiConversation({
      title: selected.value ? recordTitle(selected.value) : '新对话',
      role: workMode.value,
      workMode: workMode.value,
      context: buildCurrentAiContext()
    })
    const conversation = payloadOf(res)
    activeConversationId.value = conversation?.conversationId || null
    seedAgentWelcome()
    await loadAiConversationList()
    return conversation
  } finally {
    conversationLoading.value = false
  }
}

async function selectAiConversation(conversationId) {
  if (!conversationId || conversationId === activeConversationId.value) return
  conversationLoading.value = true
  try {
    const res = await getAiConversation(conversationId)
    const conversation = payloadOf(res)
    activeConversationId.value = conversation?.conversationId || conversationId
    agentMessages.value = normalizeConversationMessages(conversation?.messages)
    if (!agentMessages.value.length) {
      seedAgentWelcome()
    }
  } finally {
    conversationLoading.value = false
  }
}

async function removeAiConversation(item) {
  if (!item?.conversationId) return
  try {
    await ElMessageBox.confirm('删除后该会话不会再出现在列表中，确定删除吗？', '删除会话', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteAiConversation(item.conversationId)
    if (activeConversationId.value === item.conversationId) {
      activeConversationId.value = null
      seedAgentWelcome()
    }
    await loadAiConversationList()
  } catch (error) {
    // 用户取消删除时保持当前会话。
  }
}

async function syncActiveConversationContext() {
  if (pageMode.value !== 'agent' || !activeConversationId.value || !selected.value) return
  try {
    await updateAiConversationContext(activeConversationId.value, buildCurrentAiContext())
  } catch (error) {
    // 上下文同步失败不打断用户继续切换记录或输入。
  }
}

function buildCurrentAiContext() {
  return {
    pageMode: pageMode.value,
    workMode: workMode.value,
    selected: selected.value ? buildAiRecordContext(selected.value) : null,
    filters: { ...filters },
    mapContext: mapContext.value ? { ...mapContext.value } : null,
    visibleActions: visibleActions.value.map(item => ({ key: item.key, label: item.label })),
    summary: { ...workspaceSummary }
  }
}

function normalizeConversationMessages(messages = []) {
  return messages
    .filter(item => ['user', 'assistant'].includes(item.role))
    .map(item => ({
      role: item.role,
      content: item.content || '',
      intent: item.intent,
      intentLabel: item.intentLabel,
      toolCalls: parseMaybeJson(item.toolCalls) || [],
      collaboration: parseMaybeJson(item.collaboration),
      contextSnapshot: parseMaybeJson(item.contextSnapshot)
    }))
}

function parseMaybeJson(value) {
  if (!value || typeof value !== 'string') return value
  try {
    return JSON.parse(value)
  } catch (error) {
    return value
  }
}

function conversationTime(item) {
  const value = item?.lastMessageTime || item?.updateTime || item?.createTime
  if (!value) return ''
  const date = new Date(String(value).replace(/-/g, '/'))
  if (Number.isNaN(date.getTime())) return ''
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  const hour = `${date.getHours()}`.padStart(2, '0')
  const minute = `${date.getMinutes()}`.padStart(2, '0')
  return `${month}-${day} ${hour}:${minute}`
}

async function sendAgentMessage() {
  const content = agentInput.value.trim()
  if (!content) return
  const conversation = await ensureAiConversation()
  if (!conversation?.conversationId) return
  const localUserMessage = { role: 'user', content }
  agentMessages.value.push(localUserMessage)
  agentInput.value = ''
  agentLoading.value = true
  try {
    const res = await sendAiConversationMessage(conversation.conversationId, buildAiRequest(content))
    const data = payloadOf(res)
    if (data?.conversation?.messages?.length) {
      agentMessages.value = normalizeConversationMessages(data.conversation.messages)
    } else if (data?.assistantMessage) {
      agentMessages.value.push(normalizeStoredAiMessage(data.assistantMessage, data.assistant))
    } else {
      agentMessages.value.push(normalizeAiMessage(data?.assistant || data))
    }
    summarizeAiConversation(conversation.conversationId).catch(() => {})
    await loadAiConversationList()
  } catch (error) {
    agentMessages.value.push({ role: 'assistant', content: '智能体服务暂不可用，请稍后再试。' })
  } finally {
    agentLoading.value = false
  }
}

function normalizeStoredAiMessage(message, assistantPayload) {
  const aiResult = assistantPayload ? normalizeAiResponse(assistantPayload) : null
  return {
    role: 'assistant',
    content: message?.content || aiResult?.answer || '智能体接口已收到请求。',
    intent: message?.intent || aiResult?.intent,
    intentLabel: message?.intentLabel || aiResult?.intentLabel,
    toolCalls: parseMaybeJson(message?.toolCalls) || aiResult?.toolCalls || [],
    collaboration: parseMaybeJson(message?.collaboration) || aiResult?.collaboration,
    suggestions: aiResult?.suggestions,
    nextActions: aiResult?.nextActions || []
  }
}


function openFloatingAi() {
  floatingAiOpen.value = true
}

function openBusinessTools() {
  businessToolsOpen.value = true
  if (pageMode.value !== 'business') {
    switchPage('business')
  }
}

async function runBusinessTool(handler) {
  switchPage('business')
  if (typeof handler === 'function') {
    await handler()
  }
  businessToolsOpen.value = false
}

async function sendFloatingAiMessage() {
  const content = floatingAiInput.value.trim()
  if (!content) return
  floatingAiMessages.value.push({ role: 'user', content })
  floatingAiInput.value = ''
  floatingAiLoading.value = true
  try {
    const res = await sendPortalAiChat(buildAiRequest(content))
    floatingAiMessages.value.push(normalizeAiMessage(res))
  } catch (error) {
    floatingAiMessages.value.push({ role: 'assistant', content: '智能体服务暂时没有返回结果，请稍后重试。当前业务页和消息沟通不受影响。' })
  } finally {
    floatingAiLoading.value = false
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
      mapContext: mapContext.value ? { ...mapContext.value } : null,
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
    longitude: item.longitude,
    latitude: item.latitude,
    location: item.longitude !== undefined && item.longitude !== null && item.longitude !== '' && item.latitude !== undefined && item.latitude !== null && item.latitude !== ''
      ? `${item.longitude},${item.latitude}`
      : undefined,
    ownerId: item.ownerId,
    agentId: item.agentId,
    tenantId: item.tenantId,
    appointmentTime: item.appointmentTime,
    startDate: item.startDate,
    endDate: item.endDate,
    description: item.description,
    facilities: item.facilities,
    tags: item.tags,
    mapContext: mapContext.value ? { ...mapContext.value } : null
  }
}

function normalizeAiMessage(response) {
  const aiResult = normalizeAiResponse(response)
  return {
    role: 'assistant',
    content: aiResult.answer || '智能体接口已收到请求，等待后端返回标准化结果。',
    intent: aiResult.intent,
    intentLabel: aiResult.intentLabel,
    toolCalls: aiResult.toolCalls || [],
    collaboration: aiResult.collaboration,
    suggestions: aiResult.suggestions,
    nextActions: aiResult.nextActions || []
  }
}

function collaborationSummary(collaboration) {
  const experts = collaboration?.experts || []
  if (!experts.length) return ''
  return experts.map(item => item.label || item.name).slice(0, 3).join(' / ')
}
</script>

<style scoped>
.portal-shell {
  --portal-ink: #1f1f1f;
  --portal-muted: #5f6368;
  --portal-line: rgba(60, 64, 67, 0.12);
  --portal-soft: #f0f4f9;
  --portal-surface: #ffffff;
  --portal-primary: #0b57d0;
  --portal-primary-strong: #e8f0fe;
  --portal-action: #137333;
  --portal-accent: #b06000;
  --portal-info: #80868b;
  min-height: calc(100vh - 84px);
  color: var(--portal-ink);
  background:
    radial-gradient(circle at 18% 12%, rgba(26, 115, 232, 0.12), transparent 28%),
    radial-gradient(circle at 84% 18%, rgba(147, 52, 230, 0.1), transparent 30%),
    #f8fafd;
}

.portal-shell button:focus,
.portal-shell button:focus-visible {
  outline: none;
}

.portal-shell :deep(.el-button:focus),
.portal-shell :deep(.el-button:focus-visible),
.portal-shell :deep(.el-segmented__item:focus),
.portal-shell :deep(.el-segmented__item:focus-visible) {
  outline: none;
  box-shadow: none;
}

.portal-shell.is-agent {
  background:
    radial-gradient(circle at 50% 8%, rgba(26, 115, 232, 0.14), transparent 32%),
    radial-gradient(circle at 82% 18%, rgba(147, 52, 230, 0.1), transparent 28%),
    #f8fafd;
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
  background: rgba(255, 255, 255, 0.86);
  border-bottom: 1px solid var(--portal-line);
  backdrop-filter: blur(18px);
}

.brand span {
  display: block;
  color: transparent;
  background: linear-gradient(90deg, #1a73e8, #9334e6 54%, #007b83);
  background-clip: text;
  font-size: 18px;
  font-weight: 700;
}

.brand small {
  color: var(--portal-muted);
}

.page-switch {
  display: grid;
  grid-template-columns: repeat(2, 72px);
  width: 144px;
  height: 30px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid var(--portal-line);
  border-radius: 999px;
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
  background: #e8f0fe;
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
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid var(--portal-line);
  border-radius: 999px;
  box-shadow: none;
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
  color: #0b57d0;
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
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid var(--portal-line);
  border-radius: 999px;
}

.role-tabs button {
  height: 26px;
  color: var(--portal-muted);
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: 999px;
  font-size: 13px;
}

.role-tabs button.active {
  color: var(--portal-primary);
  background: #e8f0fe;
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
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid var(--portal-line);
  border-radius: 999px;
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
  border-radius: 28px;
  box-shadow: 0 18px 54px rgba(60, 64, 67, 0.08);
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
  color: var(--portal-info);
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
  background: #ffffff;
  border: 1px solid var(--portal-line);
  border-radius: 22px;
}

.record-card:hover,
.record-card.active {
  background: #f8fbff;
  border-color: rgba(11, 87, 208, 0.24);
  box-shadow: inset 0 0 0 1px rgba(11, 87, 208, 0.08);
}

.record-card__top,
.record-card__bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.record-card__top strong {
  min-width: 0;
  overflow: hidden;
  color: var(--portal-ink);
  font-size: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  background: #f0f4f9;
  border-radius: 999px;
  font-style: normal;
  font-size: 12px;
}

.record-card__bottom span,
.record-card__bottom small {
  min-width: 0;
  overflow: hidden;
  color: var(--portal-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
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
  border-bottom: 1px solid var(--portal-line);
}

.detail-head h2 {
  overflow-wrap: anywhere;
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
  background: #f8fafd;
  border: 1px solid var(--portal-line);
  border-radius: 20px;
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
  background: var(--portal-surface);
  border: 1px solid var(--portal-line);
  border-radius: 24px;
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
  background: #f0f4f9;
  border-radius: 20px;
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
  background: #f8fafd;
  border: 1px solid var(--portal-line);
  border-radius: 18px;
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
.operation-card,
.map-context-card {
  padding: 18px;
  margin-top: 18px;
  background: #f8fafd;
  border: 1px solid var(--portal-line);
  border-radius: 24px;
}

.process-card h3,
.action-card h3,
.operation-card h3,
.map-context-card h3 {
  margin: 0 0 12px;
  color: var(--portal-ink);
}

.map-context-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.map-context-head > div {
  min-width: 0;
}

.map-context-head p {
  margin: 0;
  color: var(--portal-muted);
  line-height: 1.55;
}

.commute-bar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 188px auto;
  gap: 10px;
  align-items: center;
  margin-top: 14px;
}

.commute-bar :deep(.el-segmented) {
  width: 188px;
}

.route-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  padding: 12px 14px;
  margin-top: 14px;
  color: var(--portal-primary);
  background: #e8f0fe;
  border: 1px solid rgba(11, 87, 208, 0.16);
  border-radius: 18px;
}

.route-summary strong {
  margin-right: 4px;
}

.route-summary span {
  color: var(--portal-muted);
  font-size: 13px;
}

.nearby-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.nearby-grid article {
  min-width: 0;
  padding: 12px;
  background: #ffffff;
  border: 1px solid var(--portal-line);
  border-radius: 18px;
}

.nearby-grid__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}

.nearby-grid strong {
  color: var(--portal-ink);
  font-size: 14px;
}

.nearby-grid__head span,
.nearby-grid small {
  color: var(--portal-muted);
  font-size: 12px;
}

.nearby-grid ul {
  display: grid;
  gap: 8px;
  padding: 0;
  margin: 0;
  list-style: none;
}

.nearby-grid li {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
}

.nearby-grid span {
  min-width: 0;
  overflow: hidden;
  color: var(--portal-ink);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nearby-grid em {
  color: var(--portal-muted);
  font-size: 12px;
  font-style: normal;
}

.nearby-grid small {
  display: block;
  margin-top: 10px;
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

.action-card > div:first-child {
  min-width: 0;
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

.business-tools-drawer :deep(.el-drawer__header) {
  display: none;
}

.business-tools-drawer :deep(.el-drawer__body) {
  padding: 0;
  background: #f6f8fb;
}

.business-tools-panel {
  display: grid;
  gap: 14px;
  padding: 16px;
}

.business-tools-hero {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  padding: 16px;
  color: #fff;
  background: #1e3a5f;
  border-radius: 8px;
  box-shadow: 0 16px 34px rgba(30, 58, 95, 0.2);
}

.tool-hero-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 54px;
  height: 54px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 8px;
}

.tool-hero-icon .el-icon {
  font-size: 25px;
}

.business-tools-hero span,
.business-tools-hero h2,
.business-tools-hero p {
  display: block;
  margin: 0;
}

.business-tools-hero span {
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
}

.business-tools-hero h2 {
  margin-top: 4px;
  font-size: 20px;
}

.business-tools-hero p {
  margin-top: 6px;
  color: rgba(255, 255, 255, 0.78);
  font-size: 13px;
  line-height: 1.5;
}

.business-tool-card {
  padding: 15px;
  background: #ffffff;
  border: 1px solid #dfe5ea;
  border-radius: 8px;
  box-shadow: 0 10px 24px rgba(30, 42, 62, 0.06);
}

.business-tool-card.ai-card {
  background: #fbfdff;
}

.business-tool-card.compact {
  background: #ffffff;
}

.tool-section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.tool-section-head span {
  display: block;
  margin-bottom: 5px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.tool-section-head h3 {
  margin: 0;
  color: #121a31;
  font-size: 16px;
}

.business-tool-card p {
  margin: 8px 0 0;
  color: #5f6368;
  line-height: 1.6;
}

.business-tool-actions {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
  margin-top: 14px;
}

.business-tool-actions :deep(.el-input-number) {
  width: 100%;
}

.business-tool-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 9px;
}

.business-tool-grid button {
  min-height: 42px;
  padding: 0 10px;
  color: #1f2937;
  cursor: pointer;
  background: #f8fafd;
  border: 1px solid #dfe5ea;
  border-radius: 8px;
  font-size: 13px;
  transition: background 0.18s ease, border-color 0.18s ease, color 0.18s ease, transform 0.18s ease;
}

.business-tool-grid button:hover {
  color: #0b57d0;
  background: #eef4ff;
  border-color: #b7c8f8;
  transform: translateY(-1px);
}

.business-tool-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.business-tool-tags button {
  min-height: 34px;
  padding: 0 12px;
  color: #42526e;
  cursor: pointer;
  background: #f8fafd;
  border: 1px solid #dfe5ea;
  border-radius: 999px;
  font-size: 12px;
}

.business-tool-tags button:hover {
  color: #0b57d0;
  background: #eef4ff;
  border-color: #b7c8f8;
}

.ai-recommend-note {
  grid-column: 1 / -1;
  margin: 0;
  padding: 12px 14px;
  color: var(--portal-ink);
  background: #ffffff;
  border: 1px solid var(--portal-line);
  border-radius: 18px;
}

.panel-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

.compliance-card {
  background: #fce8e6;
}

.compliance-checks {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
}

.compliance-checks span {
  padding: 8px 10px;
  color: var(--portal-muted);
  background: #ffffff;
  border: 1px solid var(--portal-line);
  border-radius: 999px;
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

.table-shell {
  background: var(--portal-surface);
  border: 1px solid var(--portal-line);
  border-radius: 20px;
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
  background: #f8fafd;
  border: 1px solid var(--portal-line);
  border-radius: 18px;
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

.owner-location-tool {
  display: grid;
  gap: 10px;
  width: 100%;
}

.owner-location-tool__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.owner-location-preview {
  display: grid;
  gap: 4px;
  min-height: 76px;
  padding: 12px 14px;
  background: #f8fafd;
  border: 1px dashed var(--portal-line);
  border-radius: 8px;
}

.owner-location-preview.active {
  background: #eef5ff;
  border-color: rgba(11, 87, 208, 0.24);
}

.owner-location-preview strong {
  color: var(--portal-ink);
  font-size: 14px;
}

.owner-location-preview span,
.owner-location-preview small {
  color: var(--portal-muted);
  line-height: 1.5;
}

.owner-location-preview small {
  font-size: 12px;
}

.owner-map-mode {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
}

.owner-map-mode span {
  color: var(--portal-muted);
  font-size: 13px;
}

.owner-map-search {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 10px;
  margin-bottom: 12px;
}

.owner-coordinate-inputs {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) auto;
  gap: 10px;
  margin-bottom: 12px;
}

.owner-map-container {
  width: 100%;
  height: min(52vh, 420px);
  min-height: 320px;
  overflow: hidden;
  background: #f0f4f9;
  border: 1px solid var(--portal-line);
  border-radius: 8px;
}

.owner-map-preview {
  display: grid;
  gap: 4px;
  padding: 12px 14px;
  margin-top: 12px;
  background: #f8fafd;
  border: 1px solid var(--portal-line);
  border-radius: 8px;
}

.owner-map-preview strong {
  color: var(--portal-ink);
  line-height: 1.5;
}

.owner-map-preview span {
  color: var(--portal-muted);
  font-size: 13px;
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

.model-button {
  height: 34px;
  padding: 0 12px;
  color: var(--portal-primary);
  cursor: pointer;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid var(--portal-line);
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
}

.agent-workspace {
  display: grid;
  grid-template-columns: 280px 400px minmax(0, 1fr);
  gap: 18px;
  width: min(1480px, calc(100vw - 48px));
  min-height: calc(100vh - 112px);
  margin: 0 auto;
  padding: 24px 0 36px;
}

.agent-conversation-panel,
.agent-record-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 18px;
  background: var(--portal-surface);
  border: 1px solid var(--portal-line);
  border-radius: 28px;
  box-shadow: 0 18px 54px rgba(60, 64, 67, 0.08);
}

.agent-conversation-head,
.agent-record-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.agent-conversation-head span,
.agent-record-head span,
.agent-context-main span {
  display: block;
  margin-bottom: 5px;
  color: var(--portal-primary);
  font-size: 12px;
  font-weight: 700;
}

.agent-conversation-head h2,
.agent-record-head h2,
.agent-context-main h2 {
  margin: 0;
  color: var(--portal-ink);
  font-size: 18px;
}

.agent-conversation-list {
  display: grid;
  gap: 10px;
  min-height: 0;
  overflow-y: auto;
  padding-right: 2px;
}

.conversation-item {
  position: relative;
  display: grid;
  gap: 5px;
  width: 100%;
  min-height: 86px;
  padding: 12px 40px 12px 12px;
  color: var(--portal-ink);
  text-align: left;
  cursor: pointer;
  background: #ffffff;
  border: 1px solid var(--portal-line);
  border-radius: 18px;
}

.conversation-item.active {
  background: var(--portal-primary-strong);
  border-color: rgba(11, 87, 208, 0.28);
}

.conversation-item strong,
.conversation-item span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversation-item strong {
  font-size: 14px;
}

.conversation-item span,
.conversation-item small {
  color: var(--portal-muted);
  font-size: 12px;
}

.conversation-delete {
  position: absolute;
  top: 8px;
  right: 8px;
}

.agent-record-head em {
  color: var(--portal-muted);
  font-style: normal;
  font-size: 13px;
}

.agent-record-panel .role-tabs,
.agent-record-panel .admin-mode-badge {
  width: 100%;
  margin-bottom: 12px;
}

.agent-record-search {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 128px auto;
  gap: 8px;
  margin-bottom: 12px;
}

.agent-records {
  flex: 1;
  min-height: 0;
  max-height: none;
  padding-right: 2px;
}

.agent-center {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr) auto auto;
  align-items: stretch;
  min-width: 0;
  min-height: 0;
  padding: 8px 0 0;
}

.agent-center.has-chat {
  justify-content: stretch;
}

.agent-hero {
  padding: 24px 8px 6px;
}

.agent-hero h1 {
  margin: 0 0 12px;
  color: transparent;
  background: linear-gradient(90deg, #1a73e8, #9334e6 54%, #007b83);
  background-clip: text;
  font-size: 38px;
  font-weight: 700;
}

.agent-summary {
  display: block;
  width: min(760px, 100%);
  margin: 0 0 18px;
  color: var(--portal-muted);
  line-height: 1.75;
}

.agent-context-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 16px;
  align-items: center;
  padding: 16px;
  margin-bottom: 14px;
  background: var(--portal-surface);
  border: 1px solid var(--portal-line);
  border-radius: 24px;
  box-shadow: 0 18px 54px rgba(60, 64, 67, 0.08);
}

.agent-context-card.empty {
  grid-template-columns: 1fr;
}

.agent-context-main {
  min-width: 0;
}

.agent-context-main h2 {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.agent-context-main p {
  margin: 8px 0 10px;
  color: var(--portal-muted);
  line-height: 1.6;
}

.agent-context-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.agent-context-actions strong {
  margin-right: 4px;
  color: var(--portal-primary);
  white-space: nowrap;
}

.agent-thread {
  min-height: 260px;
  padding: 4px 6px 12px;
  margin-bottom: 14px;
  overflow-y: auto;
}

.agent-message {
  max-width: 76%;
  padding: 12px 14px;
  margin-bottom: 12px;
  border-radius: 22px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.agent-message.assistant {
  color: var(--portal-ink);
  background: #ffffff;
  border: 1px solid var(--portal-line);
}

.agent-message.user {
  margin-left: auto;
  color: #0b57d0;
  background: var(--portal-primary-strong);
}

.agent-input-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 42px;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 60px;
  padding: 8px 10px 8px 18px;
  background: var(--portal-surface);
  border: 1px solid var(--portal-line);
  border-radius: 30px;
  box-shadow: 0 18px 54px rgba(60, 64, 67, 0.14);
}

.agent-input-card :deep(.el-textarea__inner) {
  min-height: 34px !important;
  padding: 8px 0;
  border: 0;
  box-shadow: none;
}

.agent-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 14px;
}

.agent-actions button {
  height: 44px;
  padding: 0 18px;
  color: var(--portal-muted);
  cursor: pointer;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid var(--portal-line);
  border-radius: 22px;
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
  color: var(--portal-primary);
  background: #e8f0fe;
  border: 1px solid rgba(11, 87, 208, 0.16);
  border-radius: 999px;
  font-size: 12px;
  line-height: 1.4;
}

.ai-trace.compact span {
  font-size: 11px;
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
  .agent-workspace,
  .agent-record-search,
  .agent-context-card,
  .field-grid,
  .nearby-grid,
  .commute-bar,
  .owner-map-mode,
  .owner-map-search,
  .owner-coordinate-inputs,
  .process-steps,
  .inline-form,
  .inline-form.compact,
  .deal-form,
  .contract-extra-actions {
    grid-template-columns: 1fr;
  }

  .commute-bar :deep(.el-segmented) {
    width: 100%;
  }

  .business-page {
    width: min(100%, calc(100vw - 28px));
    padding-top: 16px;
  }

  .agent-workspace {
    width: min(100%, calc(100vw - 28px));
    min-height: auto;
    padding-top: 16px;
  }

  .detail-card,
  .result-list,
  .agent-conversation-panel,
  .agent-record-panel {
    min-height: auto;
  }

  .agent-message {
    max-width: 92%;
  }

  .records {
    max-height: none;
    min-height: auto;
  }

  .action-card,
  .detail-head {
    display: grid;
  }

  .detail-head__aside,
  .action-buttons,
  .agent-context-actions {
    justify-items: start;
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .agent-hero h1 {
    font-size: 26px;
    text-align: center;
  }

  .agent-center {
    min-height: auto;
    padding: 0;
  }

  .agent-conversation-panel,
  .agent-record-panel {
    padding: 14px;
  }

  .agent-conversation-list {
    max-height: 320px;
  }

  .agent-hero {
    padding: 8px 2px 0;
  }

  .agent-summary {
    text-align: center;
  }

  .agent-context-actions {
    flex-wrap: wrap;
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

  .business-search {
    margin-bottom: 12px;
  }

  .hot-tags {
    justify-content: flex-start;
    overflow-x: auto;
    padding-bottom: 4px;
  }

  .hot-tags span,
  .hot-tags button {
    flex: 0 0 auto;
  }

  .detail-card {
    padding: 18px;
  }

  .record-card__top,
  .record-card__bottom {
    align-items: flex-start;
    flex-direction: column;
    gap: 6px;
  }
}
</style>
