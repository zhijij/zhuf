<template>
  <div class="ai-quick-rail" :class="{ 'is-open': open }">
    <button class="rail-button" title="业务工具" @click="$emit('open-business')">
      <el-icon><Briefcase /></el-icon>
    </button>
    <button class="rail-button" title="消息中心" @click="$emit('open-messages')">
      <el-icon><Headset /></el-icon>
    </button>
    <button class="rail-button ai-entry" title="AI 助手" @click="openAssistant">
      <el-icon><Promotion /></el-icon>
    </button>
    <button class="rail-button" title="使用指南">
      <el-icon><Document /></el-icon>
    </button>
  </div>

  <section
    v-if="open"
    class="ai-float-modal"
    :style="{ left: `${position.x}px`, top: `${position.y}px` }"
  >
    <header class="ai-float-head" @pointerdown="startDrag">
      <div>
        <strong>Smart Agent</strong>
        <span>业务智能体 · 可拖动</span>
      </div>
      <div class="ai-float-actions">
        <button title="归位" @click.stop="resetPosition">
          <el-icon><Aim /></el-icon>
        </button>
        <button title="收起" @click.stop="open = false">
          <el-icon><Close /></el-icon>
        </button>
      </div>
    </header>

    <div ref="messageListRef" class="ai-float-body">
      <div
        v-for="(item, index) in messages"
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
      <button v-for="item in presets" :key="item" @click="input = item">
        {{ item }}
      </button>
    </div>

    <footer class="ai-float-composer">
      <el-input
        v-model="input"
        type="textarea"
        :autosize="{ minRows: 1, maxRows: 4 }"
        resize="none"
        placeholder="问 AI 助手，Ctrl + Enter 发送"
        @keydown.ctrl.enter.prevent="$emit('send')"
      />
      <el-button circle type="primary" icon="Promotion" :loading="loading" @click="$emit('send')" />
    </footer>
  </section>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { Aim, Briefcase, Close, Document, Headset, Promotion } from '@element-plus/icons-vue'

const open = defineModel('open', { type: Boolean, default: false })
const input = defineModel('input', { type: String, default: '' })

const props = defineProps({
  loading: { type: Boolean, default: false },
  messages: { type: Array, default: () => [] },
  presets: { type: Array, default: () => [] }
})

defineEmits(['open-business', 'open-messages', 'send'])

const messageListRef = ref(null)
const position = reactive({ x: 0, y: 0 })
const drag = reactive({ active: false, offsetX: 0, offsetY: 0 })

watch(
  () => [open.value, props.messages.length],
  () => nextTick(scrollToBottom)
)

onMounted(resetPosition)

onBeforeUnmount(stopDrag)

function openAssistant() {
  open.value = true
  nextTick(scrollToBottom)
}

function resetPosition() {
  if (typeof window === 'undefined') return
  const width = Math.min(420, window.innerWidth - 32)
  const height = Math.min(620, window.innerHeight - 120)
  position.x = Math.max(16, window.innerWidth - width - 92)
  position.y = Math.max(84, window.innerHeight - height - 28)
}

function startDrag(event) {
  if (event.button !== undefined && event.button !== 0) return
  drag.active = true
  drag.offsetX = event.clientX - position.x
  drag.offsetY = event.clientY - position.y
  window.addEventListener('pointermove', moveDrag)
  window.addEventListener('pointerup', stopDrag)
}

function moveDrag(event) {
  if (!drag.active || typeof window === 'undefined') return
  const modalWidth = Math.min(420, window.innerWidth - 32)
  const modalHeight = Math.min(620, window.innerHeight - 120)
  const nextX = event.clientX - drag.offsetX
  const nextY = event.clientY - drag.offsetY
  position.x = clamp(nextX, 12, window.innerWidth - modalWidth - 12)
  position.y = clamp(nextY, 72, window.innerHeight - modalHeight - 12)
}

function stopDrag() {
  drag.active = false
  if (typeof window === 'undefined') return
  window.removeEventListener('pointermove', moveDrag)
  window.removeEventListener('pointerup', stopDrag)
}

function scrollToBottom() {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max)
}
</script>

<style scoped>
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

@media (max-width: 1100px) {
  .ai-float-message {
    max-width: 92%;
  }
}

@media (max-width: 720px) {
  .ai-quick-rail {
    right: 14px;
    bottom: 64px;
  }
}
</style>
