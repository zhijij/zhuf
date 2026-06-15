<template>
  <div class="portal-chat-page">
    <header class="chat-page-head">
      <div class="chat-page-brand">
        <span>智能AI房屋租赁系统</span>
        <small>{{ pageTitle }}</small>
      </div>

      <div class="chat-page-actions">
        <el-button text icon="Back" @click="goBack">返回</el-button>
        <el-button text icon="Refresh" @click="refreshChat">刷新</el-button>
        <el-button type="primary" plain icon="Briefcase" @click="goWorkbench">业务台</el-button>
      </div>
    </header>

    <main class="chat-page-main">
      <BusinessChatPanel
        ref="panelRef"
        :biz-type="bizType"
        :biz-id="bizId"
        :session-id="sessionId"
        auto-open
      />
    </main>
  </div>
</template>

<script setup name="PortalChat">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import useUserStore from '@/store/modules/user'
import BusinessChatPanel from './components/BusinessChatPanel.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const panelRef = ref(null)

const bizType = computed(() => String(route.query.bizType || ''))
const bizId = computed(() => String(route.query.bizId || ''))
const sessionId = computed(() => String(route.query.sessionId || ''))
const pageTitle = computed(() => String(route.query.title || '消息中心'))

onMounted(prepareChat)

watch(
  () => route.fullPath,
  () => prepareChat()
)

async function prepareChat() {
  await nextTick()
  panelRef.value?.prepareSession()
}

function refreshChat() {
  panelRef.value?.prepareSession()
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
    return
  }
  goWorkbench()
}

function goWorkbench() {
  const roles = userStore.roles || []
  router.push(roles.includes('auditor') ? '/portal/auditor' : '/portal/index')
}
</script>

<style scoped>
.portal-chat-page {
  min-height: 100vh;
  color: #1f1f1f;
  background: #f7f9fc;
}

.chat-page-head {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 22px;
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1px solid rgba(60, 64, 67, 0.12);
  backdrop-filter: blur(10px);
}

.chat-page-brand span,
.chat-page-brand small {
  display: block;
}

.chat-page-brand span {
  font-size: 17px;
  font-weight: 700;
}

.chat-page-brand small {
  margin-top: 3px;
  color: #5f6368;
  font-size: 12px;
}

.chat-page-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chat-page-main {
  padding: 18px 22px 24px;
}

.chat-page-main :deep(.business-chat-panel) {
  min-height: calc(100vh - 110px);
}

.chat-page-main :deep(.message-list) {
  min-height: calc(100vh - 292px);
}

.chat-page-main :deep(.empty-room) {
  min-height: calc(100vh - 110px);
}

@media (max-width: 720px) {
  .chat-page-head {
    align-items: flex-start;
    flex-direction: column;
    padding: 12px 14px;
  }

  .chat-page-actions {
    width: 100%;
    justify-content: space-between;
  }

  .chat-page-main {
    padding: 12px;
  }
}
</style>
