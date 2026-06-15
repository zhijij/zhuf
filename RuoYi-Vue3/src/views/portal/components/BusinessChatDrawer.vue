<template>
  <el-drawer
    v-model="visible"
    :title="title || '业务沟通'"
    :size="drawerSize"
    append-to-body
    destroy-on-close
    @opened="prepareSession"
  >
    <BusinessChatPanel
      ref="panelRef"
      :biz-type="bizType"
      :biz-id="bizId"
      :session-id="sessionId"
      :auto-open="visible"
    />
  </el-drawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import BusinessChatPanel from './BusinessChatPanel.vue'

const visible = defineModel({ type: Boolean, default: false })

defineProps({
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
  title: {
    type: String,
    default: '业务沟通'
  }
})

const panelRef = ref(null)
const drawerSize = computed(() => {
  if (typeof window === 'undefined') return '780px'
  return window.innerWidth <= 900 ? '100%' : '780px'
})

function prepareSession() {
  panelRef.value?.prepareSession()
}

defineExpose({
  prepareSession
})
</script>
