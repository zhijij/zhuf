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
            <span>{{ activeSession?.bizType ? bizNameMap[activeSession.bizType] : '业务沟通' }}</span>
          </div>
          <el-button icon="Refresh" link type="primary" @click="loadMessages">刷新</el-button>
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
  </div>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import useUserStore from '@/store/modules/user'
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

const bizNameMap = {
  entrust: '委托沟通',
  appointment: '预约沟通',
  intention: '意向沟通',
  contract: '合同沟通'
}

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

function sessionUnread(session) {
  const member = (session.members || []).find(item => sameId(item.userId, userId.value))
  return member?.unreadCount || 0
}

function sessionTitle(session) {
  return session.title || bizNameMap[session.bizType] || `会话 #${session.sessionId}`
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
  grid-template-columns: 340px minmax(0, 1fr);
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
  padding: 15px 18px;
  border-bottom: 1px solid #edf1f5;
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

@media (max-width: 900px) {
  .business-chat-panel {
    grid-template-columns: 1fr;
  }

  .chat-sidebar {
    max-height: 280px;
    border-right: 0;
    border-bottom: 1px solid #dfe5ea;
  }
}
</style>
