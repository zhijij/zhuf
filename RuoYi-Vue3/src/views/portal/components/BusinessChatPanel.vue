<template>
  <div class="business-chat-panel">
    <aside class="chat-sidebar">
      <div class="chat-search">
        <el-input v-model="sessionKeyword" clearable placeholder="搜索会话">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>

      <div class="chat-filters">
        <el-button :type="filterType === 'all' ? 'primary' : 'default'" size="small" plain @click="filterType = 'all'">全部</el-button>
        <el-button :type="filterType === 'unread' ? 'primary' : 'default'" size="small" plain @click="filterType = 'unread'">未读</el-button>
        <el-button :type="filterType === 'business' ? 'primary' : 'default'" size="small" plain @click="filterType = 'business'">业务</el-button>
      </div>

      <div v-loading="sessionsLoading" class="session-list">
        <button
          v-for="session in filteredSessions"
          :key="session.sessionId"
          class="session-item"
          :class="{ active: sameId(session.sessionId, activeSessionId) }"
          @click="selectSession(session)"
        >
          <div class="session-avatar">{{ sessionAvatar(session) }}</div>
          <div class="session-main">
            <div class="session-title">
              <strong>{{ sessionTitle(session) }}</strong>
              <span>{{ formatTime(session.lastMessageTime) }}</span>
            </div>
            <p>{{ session.lastMessage || '暂无消息' }}</p>
          </div>
          <el-badge v-if="sessionUnread(session)" :value="sessionUnread(session)" />
        </button>

        <el-empty v-if="!sessionsLoading && !filteredSessions.length" description="暂无会话" :image-size="80" />
      </div>
    </aside>

    <section class="chat-room">
      <template v-if="activeSessionId">
        <header class="chat-room-head">
          <div>
            <strong>{{ activeSessionTitle }}</strong>
            <span>{{ activeBizLabel }}</span>
          </div>
          <div class="room-actions">
            <el-button icon="Refresh" link type="primary" @click="loadMessages">刷新</el-button>
            <el-button icon="Promotion" plain type="primary" :loading="aiLoading" @click="generateAiReply">AI 生成回复</el-button>
          </div>
        </header>

        <div ref="messageListRef" v-loading="messagesLoading" class="message-list">
          <div
            v-for="message in messages"
            :key="message.messageId || message.createTime"
            class="message-row"
            :class="{ mine: sameId(message.senderId, userId), system: message.messageType === 'system' }"
          >
            <div class="message-bubble">
              <p>{{ message.content }}</p>
              <span>{{ message.createTime || '' }}</span>
            </div>
          </div>
          <el-empty v-if="!messagesLoading && !messages.length" description="暂无消息" :image-size="100" />
        </div>

        <footer class="chat-composer">
          <el-input
            v-model="draft"
            type="textarea"
            :rows="3"
            resize="none"
            placeholder="输入消息，Ctrl + Enter 发送"
            @keydown.ctrl.enter.prevent="submitMessage"
          />
          <el-button type="primary" :disabled="!draft.trim()" @click="submitMessage">发送</el-button>
        </footer>
      </template>

      <div v-else class="empty-room">
        <el-empty description="选择左侧会话开始沟通" :image-size="120" />
      </div>
    </section>

    <aside class="agent-panel">
      <header class="agent-head">
        <div>
          <strong>智能体建议</strong>
          <span>{{ activeSessionId ? '基于当前会话生成' : '选择会话后启用' }}</span>
        </div>
        <el-button icon="Refresh" text :loading="aiLoading" :disabled="!activeSessionId" @click="generateAiSummary">分析</el-button>
      </header>

      <div v-if="activeSessionId" class="agent-body">
        <div class="agent-shortcuts">
          <button
            v-for="item in aiShortcuts"
            :key="item.key"
            :class="{ active: aiMode === item.key }"
            :disabled="aiLoading"
            @click="runAiShortcut(item)"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </button>
        </div>

        <div class="agent-question">
          <el-input
            v-model="aiInput"
            type="textarea"
            resize="none"
            :rows="3"
            placeholder="让智能体帮你判断怎么回复、怎么推进业务"
            @keydown.ctrl.enter.prevent="askAi"
          />
          <el-button type="primary" icon="Promotion" :loading="aiLoading" :disabled="!aiInput.trim()" @click="askAi">
            询问
          </el-button>
        </div>

        <div class="agent-result">
          <template v-if="aiAnswer">
            <div class="agent-answer">{{ aiAnswer }}</div>
            <div v-if="aiMeta.intentLabel || aiMeta.toolCalls.length" class="agent-trace">
              <span v-if="aiMeta.intentLabel">{{ aiMeta.intentLabel }}</span>
              <span v-if="aiMeta.collaboration?.experts?.length">协作：{{ collaborationSummary(aiMeta.collaboration) }}</span>
              <span v-for="tool in aiMeta.toolCalls" :key="tool.name">{{ tool.label || tool.name }}</span>
            </div>
            <div v-if="aiMeta.collaboration?.experts?.length" class="agent-experts">
              <button
                v-for="expert in aiMeta.collaboration.experts"
                :key="expert.name"
                type="button"
                @click="aiInput = expert.summary || expert.label"
              >
                <strong>{{ expert.label || expert.name }}</strong>
                <span>{{ expert.summary }}</span>
              </button>
            </div>
            <div v-if="aiMeta.nextActions.length" class="next-actions">
              <strong>下一步</strong>
              <button v-for="item in aiMeta.nextActions" :key="item" @click="aiInput = `请围绕“${item}”生成沟通话术`">
                {{ item }}
              </button>
            </div>
            <div class="agent-result-actions">
              <el-button plain type="primary" icon="EditPen" @click="useAiAnswer">填入输入框</el-button>
              <el-button plain icon="CopyDocument" @click="copyAiAnswer">复制</el-button>
            </div>
          </template>
          <el-empty v-else description="智能体还没有生成建议" :image-size="82" />
        </div>
      </div>

      <div v-else class="agent-empty">
        <el-empty description="请选择一个业务会话" :image-size="96" />
      </div>
    </aside>
  </div>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatLineRound, DataAnalysis, MagicStick, Promotion, Search } from '@element-plus/icons-vue'
import useUserStore from '@/store/modules/user'
import { sendPortalAiChat } from '@/api/portal/ai'
import { listChatMessages, listChatSessions, openChatSession, sendChatMessage } from '@/api/portal/chat'

const props = defineProps({
  bizType: {
    type: String,
    default: ''
  },
  bizId: {
    type: [Number, String],
    default: ''
  },
  sessionId: {
    type: [Number, String],
    default: ''
  },
  autoOpen: {
    type: Boolean,
    default: false
  }
})

const userStore = useUserStore()
const userId = computed(() => userStore.id)
const sessions = ref([])
const messages = ref([])
const activeSessionId = ref('')
const activeSession = ref(null)
const draft = ref('')
const sessionKeyword = ref('')
const filterType = ref('all')
const sessionsLoading = ref(false)
const messagesLoading = ref(false)
const messageListRef = ref(null)
const aiLoading = ref(false)
const aiInput = ref('')
const aiMode = ref('reply')
const aiAnswer = ref('')
const aiMeta = ref({
  intent: '',
  intentLabel: '',
  toolCalls: [],
  collaboration: null,
  suggestions: null,
  nextActions: []
})

const bizNameMap = {
  entrust: '委托沟通',
  appointment: '预约沟通',
  intention: '意向沟通',
  contract: '合同沟通'
}

const bizContextLabelMap = {
  entrust: '委托跟进',
  appointment: '预约看房',
  intention: '租赁意向',
  contract: '合同协同'
}

const aiShortcuts = [
  { key: 'reply', label: '生成回复', icon: MagicStick, prompt: '请基于当前会话生成一段可以直接发给对方的回复，语气专业、自然、不过度承诺。' },
  { key: 'summary', label: '总结会话', icon: DataAnalysis, prompt: '请总结当前沟通进展、双方已确认信息、仍缺失的信息和风险点。' },
  { key: 'next', label: '下一步动作', icon: ChatLineRound, prompt: '请判断当前业务下一步应该怎么推进，并给出可执行动作和对应沟通话术。' }
]

const filteredSessions = computed(() => {
  const keyword = sessionKeyword.value.trim().toLowerCase()
  return sessions.value.filter(session => {
    if (filterType.value === 'unread' && !sessionUnread(session)) return false
    if (filterType.value === 'business' && !session.bizType) return false
    if (!keyword) return true
    return [session.title, session.bizType, session.bizId, session.lastMessage]
      .some(value => String(value || '').toLowerCase().includes(keyword))
  })
})

const activeSessionTitle = computed(() => activeSession.value ? sessionTitle(activeSession.value) : '业务沟通')
const activeBizLabel = computed(() => {
  const session = activeSession.value || {}
  const label = bizDisplayName(session.bizType)
  return session.bizId ? `${label} #${session.bizId}` : label
})

watch(
  () => [props.bizType, props.bizId, props.sessionId, props.autoOpen],
  () => {
    if (props.autoOpen) {
      prepareSession()
    }
  }
)

async function prepareSession() {
  await loadSessions()

  if (props.sessionId) {
    activeSessionId.value = props.sessionId
    activeSession.value = sessions.value.find(item => sameId(item.sessionId, props.sessionId)) || null
    await loadMessages()
    return
  }

  if (props.bizType && props.bizId) {
    messagesLoading.value = true
    try {
      const res = await openChatSession({ bizType: props.bizType, bizId: props.bizId })
      activeSession.value = res?.data || null
      activeSessionId.value = activeSession.value?.sessionId || ''
      await loadSessions()
      await loadMessages()
    } finally {
      messagesLoading.value = false
    }
    return
  }

  activeSession.value = sessions.value[0] || null
  activeSessionId.value = activeSession.value?.sessionId || ''
  await loadMessages()
}

async function loadSessions() {
  sessionsLoading.value = true
  try {
    const res = await listChatSessions()
    sessions.value = res?.rows || res?.data || []
    if (activeSessionId.value) {
      activeSession.value = sessions.value.find(item => sameId(item.sessionId, activeSessionId.value)) || activeSession.value
    }
  } finally {
    sessionsLoading.value = false
  }
}

async function selectSession(session) {
  activeSessionId.value = session.sessionId
  activeSession.value = session
  resetAiState()
  await loadMessages()
}

async function loadMessages() {
  if (!activeSessionId.value) {
    messages.value = []
    return
  }
  messagesLoading.value = true
  try {
    const res = await listChatMessages(activeSessionId.value)
    messages.value = res?.rows || res?.data || []
    await loadSessions()
    await nextTick()
    scrollToBottom()
  } finally {
    messagesLoading.value = false
  }
}

async function submitMessage() {
  const content = draft.value.trim()
  if (!content || !activeSessionId.value) return
  await sendChatMessage({ sessionId: activeSessionId.value, content, messageType: 'text' })
  draft.value = ''
  ElMessage.success('消息已发送')
  await loadMessages()
}

async function generateAiReply() {
  await runAiShortcut(aiShortcuts[0])
}

async function generateAiSummary() {
  await runAiShortcut(aiShortcuts[1])
}

async function runAiShortcut(shortcut) {
  aiMode.value = shortcut.key
  aiInput.value = shortcut.prompt
  await askAi()
}

async function askAi() {
  const prompt = aiInput.value.trim()
  if (!prompt || !activeSessionId.value) return
  aiLoading.value = true
  try {
    const res = await sendPortalAiChat(buildChatAiRequest(prompt))
    applyAiResponse(res)
  } finally {
    aiLoading.value = false
  }
}

function buildChatAiRequest(message) {
  const session = activeSession.value || {}
  const history = messages.value.slice(-10).map(item => ({
    role: sameId(item.senderId, userId.value) ? 'user' : (item.messageType === 'system' ? 'system' : 'assistant'),
    content: item.content || ''
  })).filter(item => item.content)
  return {
    message,
    userId: userStore.id,
    username: userStore.name,
    role: inferWorkRole(),
    roles: userStore.roles || [],
    sessionId: `business-chat-${activeSessionId.value}`,
    context: {
      pageMode: 'chat',
      workMode: inferWorkRole(),
      selected: {
        recordType: bizContextLabel(session.bizType),
        title: sessionTitle(session),
        statusLabel: session.status,
        bizType: session.bizType,
        bizId: session.bizId,
        sessionId: session.sessionId,
        lastMessage: session.lastMessage
      },
      chat: {
        assistMode: aiMode.value,
        sessionId: session.sessionId,
        title: sessionTitle(session),
        bizType: session.bizType,
        bizId: session.bizId,
        lastMessage: session.lastMessage,
        messages: messages.value.slice(-10).map(item => ({
          senderId: item.senderId,
          mine: sameId(item.senderId, userId.value),
          messageType: item.messageType,
          content: item.content,
          createTime: item.createTime
        }))
      },
      visibleActions: [
        { key: 'reply', label: '发送回复' },
        { key: 'follow', label: '记录跟进' },
        { key: 'appointment', label: '确认预约' },
        { key: 'contract', label: '核对合同' }
      ],
      history
    },
    history
  }
}

function applyAiResponse(response) {
  const data = normalizeAiPayload(response)
  aiAnswer.value = data.answer || '智能体已处理，但没有返回可展示内容。'
  aiMeta.value = {
    intent: data.intent || '',
    intentLabel: data.intentLabel || '',
    toolCalls: data.toolCalls || [],
    collaboration: data.collaboration || null,
    suggestions: data.suggestions || null,
    nextActions: data.nextActions || []
  }
}

function normalizeAiPayload(response) {
  const payload = response?.data?.data || response?.data || response
  return payload && typeof payload === 'object' ? payload : { answer: String(payload || '') }
}

function useAiAnswer() {
  if (!aiAnswer.value) return
  draft.value = aiAnswer.value.trim()
  ElMessage.success('已填入输入框，请确认后发送')
}

async function copyAiAnswer() {
  if (!aiAnswer.value) return
  await navigator.clipboard?.writeText(aiAnswer.value)
  ElMessage.success('已复制智能体建议')
}

function resetAiState() {
  aiInput.value = ''
  aiAnswer.value = ''
  aiMeta.value = {
    intent: '',
    intentLabel: '',
    toolCalls: [],
    collaboration: null,
    suggestions: null,
    nextActions: []
  }
}

function collaborationSummary(collaboration) {
  const experts = collaboration?.experts || []
  if (!experts.length) return ''
  return experts.map(item => item.label || item.name).slice(0, 3).join(' / ')
}

function inferWorkRole() {
  const roles = userStore.roles || []
  if (roles.includes('agent')) return 'agent'
  if (roles.includes('owner')) return 'owner'
  if (roles.includes('auditor')) return 'auditor'
  return 'tenant'
}

function sessionUnread(session) {
  const member = (session.members || []).find(item => sameId(item.userId, userId.value))
  return member?.unreadCount || 0
}

function sessionTitle(session) {
  return session.title || bizDisplayName(session.bizType) || `会话 #${session.sessionId}`
}

function bizDisplayName(bizType) {
  if (String(bizType || '').startsWith('house:')) return '房源咨询'
  return bizNameMap[bizType] || '业务沟通'
}

function bizContextLabel(bizType) {
  if (String(bizType || '').startsWith('house:')) return '房源咨询'
  return bizContextLabelMap[bizType] || '业务沟通'
}

function sessionAvatar(session) {
  return (sessionTitle(session) || '会').slice(0, 1)
}

function formatTime(value) {
  return value ? String(value).slice(5, 16) : ''
}

function sameId(left, right) {
  return String(left || '') === String(right || '')
}

function scrollToBottom() {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

defineExpose({
  prepareSession,
  loadSessions,
  loadMessages
})
</script>

<style scoped>
.business-chat-panel {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr) 340px;
  min-height: 620px;
  background: #fff;
  border: 1px solid #dfe5ea;
  border-radius: 8px;
  overflow: hidden;
}

.chat-sidebar {
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: #f6f8fb;
  border-right: 1px solid #dfe5ea;
}

.chat-search {
  padding: 12px 14px;
}

.chat-filters {
  display: flex;
  gap: 8px;
  padding: 0 14px 12px;
}

.session-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.session-item {
  position: relative;
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 10px;
  width: 100%;
  padding: 14px;
  text-align: left;
  cursor: pointer;
  background: #fff;
  border: 0;
  border-top: 1px solid #f0f3f5;
}

.session-item:hover,
.session-item.active {
  background: #eef4ff;
}

.session-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  color: #fff;
  background: #1e3a5f;
  border-radius: 8px;
  font-weight: 700;
}

.session-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.session-title strong {
  overflow: hidden;
  color: #121a31;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-title span,
.session-main p {
  color: #8a96a8;
  font-size: 12px;
}

.session-main p {
  margin: 6px 0 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-main {
  min-width: 0;
}

.session-item :deep(.el-badge) {
  position: absolute;
  top: 12px;
  left: 46px;
}

.chat-room {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}

.chat-room-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 15px 18px;
  border-bottom: 1px solid #edf1f5;
}

.room-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chat-room-head strong,
.chat-room-head span {
  display: block;
}

.chat-room-head strong {
  color: #121a31;
  font-size: 16px;
}

.chat-room-head span {
  margin-top: 4px;
  color: #8a96a8;
  font-size: 12px;
}

.message-list {
  flex: 1;
  min-height: 360px;
  padding: 18px;
  overflow-y: auto;
  background: #f6f8fb;
}

.message-row {
  display: flex;
  margin-bottom: 12px;
}

.message-row.mine {
  justify-content: flex-end;
}

.message-row.system {
  justify-content: center;
}

.message-bubble {
  max-width: 72%;
  padding: 10px 12px;
  color: #121a31;
  background: #fff;
  border: 1px solid #dfe5ea;
  border-radius: 8px;
  overflow-wrap: anywhere;
}

.message-row.mine .message-bubble {
  color: #fff;
  background: #2563eb;
  border-color: #2563eb;
}

.message-row.system .message-bubble {
  max-width: 88%;
  color: #42526e;
  text-align: center;
  background: #eef7f5;
  border-color: #cce5df;
}

.message-bubble p {
  margin: 0;
  line-height: 1.55;
  white-space: pre-wrap;
}

.message-bubble span {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  opacity: 0.7;
}

.chat-composer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 88px;
  gap: 10px;
  padding: 14px;
  border-top: 1px solid #edf1f5;
}

.empty-room {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 620px;
}

.agent-panel {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  background: #fbfcfe;
  border-left: 1px solid #dfe5ea;
}

.agent-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 15px 16px;
  border-bottom: 1px solid #edf1f5;
}

.agent-head strong,
.agent-head span {
  display: block;
}

.agent-head strong {
  color: #121a31;
  font-size: 15px;
}

.agent-head span {
  margin-top: 4px;
  color: #8a96a8;
  font-size: 12px;
}

.agent-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  padding: 14px;
  overflow-y: auto;
}

.agent-shortcuts {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 12px;
}

.agent-shortcuts button,
.next-actions button {
  cursor: pointer;
  border-radius: 8px;
  transition: background 0.18s ease, border-color 0.18s ease;
}

.agent-shortcuts button {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
  height: 64px;
  flex-direction: column;
  gap: 5px;
  color: #1e3a5f;
  background: #eef4ff;
  border: 1px solid #c7d7fe;
  font-size: 12px;
}

.agent-shortcuts button.active,
.agent-shortcuts button:hover {
  color: #fff;
  background: #2563eb;
  border-color: #2563eb;
}

.agent-question {
  display: grid;
  gap: 10px;
  margin-bottom: 14px;
}

.agent-result {
  flex: 1;
  min-height: 180px;
}

.agent-answer {
  padding: 12px;
  color: #121a31;
  line-height: 1.65;
  white-space: pre-wrap;
  background: #fff;
  border: 1px solid #dfe5ea;
  border-radius: 8px;
}

.agent-trace {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.agent-trace span {
  padding: 3px 7px;
  color: #1e3a5f;
  background: #eef4ff;
  border: 1px solid #c7d7fe;
  border-radius: 999px;
  font-size: 12px;
}

.next-actions {
  margin-top: 12px;
}

.agent-experts {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}

.agent-experts button {
  padding: 9px 10px;
  color: #42526e;
  text-align: left;
  cursor: pointer;
  background: #fff;
  border: 1px solid #dfe5ea;
  border-radius: 8px;
}

.agent-experts button:hover {
  border-color: #b7c8f8;
}

.agent-experts strong,
.agent-experts span {
  display: block;
}

.agent-experts strong {
  color: #1e3a5f;
  font-size: 12px;
}

.agent-experts span {
  display: -webkit-box;
  margin-top: 4px;
  overflow: hidden;
  font-size: 12px;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.next-actions strong {
  display: block;
  margin-bottom: 8px;
  color: #42526e;
  font-size: 12px;
}

.next-actions button {
  display: block;
  width: 100%;
  margin-bottom: 7px;
  padding: 8px 10px;
  color: #42526e;
  text-align: left;
  background: #fff;
  border: 1px solid #dfe5ea;
}

.next-actions button:hover {
  color: #2563eb;
  border-color: #b7c8f8;
}

.agent-result-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-top: 12px;
}

.agent-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
  min-height: 280px;
}

@media (max-width: 1180px) {
  .business-chat-panel {
    grid-template-columns: 1fr;
    min-height: 0;
  }

  .chat-sidebar {
    max-height: 280px;
    border-right: 0;
    border-bottom: 1px solid #dfe5ea;
  }

  .agent-panel {
    border-left: 0;
    border-top: 1px solid #dfe5ea;
  }

  .message-list {
    min-height: 320px;
  }
}

@media (max-width: 640px) {
  .chat-room-head,
  .chat-composer {
    grid-template-columns: 1fr;
  }

  .chat-room-head {
    display: grid;
    gap: 8px;
  }

  .room-actions,
  .agent-result-actions {
    display: grid;
    grid-template-columns: 1fr;
  }

  .message-bubble {
    max-width: 88%;
  }

  .chat-composer .el-button,
  .room-actions .el-button,
  .agent-result-actions .el-button {
    width: 100%;
  }
}
</style>
