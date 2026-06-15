<template>
  <el-drawer
    v-model="open"
    title="AI 工作台"
    size="900px"
    append-to-body
    destroy-on-close
    class="ai-console-drawer"
    @opened="$emit('refresh')"
  >
    <section class="ai-console-shell">
      <header class="ai-console-head">
        <div>
          <span>管理员工具</span>
          <h2>向量知识库与索引任务</h2>
          <p>AI 运维、房源索引和审核辅助放在这里，普通业务页面只保留当前角色的下一步动作。</p>
        </div>
        <div class="ai-console-status">
          <i :class="{ online: capabilitiesView.online }"></i>
          <strong>{{ capabilitiesView.online ? 'AI 在线' : 'AI 待检测' }}</strong>
        </div>
      </header>

      <div class="console-summary">
        <span>公开房源 {{ summaryView.houseCount || 0 }}</span>
        <span>已定价 {{ summaryView.pricedHouseCount || 0 }}</span>
        <span>平均租金 {{ summaryView.avgRent || 0 }}</span>
        <span>已索引 {{ summaryView.indexedHouseCount || 0 }}</span>
        <span>知识库 {{ capabilitiesView.indexedKnowledge || 0 }}</span>
        <span v-for="chip in capabilityChips" :key="chip">{{ chip }}</span>
      </div>

      <div class="console-actions">
        <el-button plain @click="$emit('refresh')">刷新状态</el-button>
        <el-button v-if="hasSelectedHouse" plain @click="$emit('inspect-house-document')">检查当前房源文档</el-button>
        <el-button v-if="hasSelectedHouse" type="primary" plain @click="$emit('create-house-index-task')">当前房源入库</el-button>
        <el-button v-if="canRunFullSync" type="warning" plain @click="$emit('create-full-index-task')">全量同步索引</el-button>
        <el-button type="success" plain @click="$emit('process-pending-tasks')">批量处理索引</el-button>
        <el-button :loading="knowledgeSeeding" type="primary" plain @click="$emit('seed-knowledge')">导入基础知识</el-button>
      </div>

      <div class="ai-status-grid">
        <article class="status-card">
          <strong>能力状态</strong>
          <p>{{ capabilitiesView.vectorStore || 'PostgreSQL + pgvector' }} / {{ capabilitiesView.embeddingMode || '未检测' }}</p>
          <p>Skill {{ capabilitiesView.skills.length }} 个，Tool {{ capabilitiesView.tools.length }} 个</p>
        </article>
        <article class="status-card">
          <strong>统一知识库</strong>
          <p>已入库 {{ capabilitiesView.indexedKnowledge || 0 }} 份知识文档</p>
          <p>{{ knowledgeSourceText }}</p>
        </article>
        <article v-if="latestTask" class="status-card">
          <strong>最新索引任务</strong>
          <p>#{{ latestTask.taskId }} / {{ taskStatusLabel(latestTask.status) }}</p>
          <p>{{ latestTask.errorMsg || '暂无任务说明' }}</p>
        </article>
        <article v-if="houseDocument" class="status-card">
          <strong>当前房源文档</strong>
          <p>{{ houseDocument.message || '暂无文档状态' }}</p>
          <p>索引状态：{{ aiIndexStatusLabel(houseDocument.aiIndexStatus, houseDocument.indexed) }}</p>
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
            <el-button :loading="knowledgeSubmitting" type="primary" @click="$emit('submit-knowledge')">写入知识库</el-button>
            <el-button plain @click="$emit('reset-knowledge')">清空</el-button>
          </div>
        </el-form>
      </section>

      <div class="task-table compact-shell">
        <el-table :data="indexTasks" size="small">
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
              <el-button link type="primary" :disabled="isTaskProcessing(row)" @click="$emit('process-task', row)">
                处理
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>
  </el-drawer>
</template>

<script setup>
import { computed } from 'vue'
import { aiIndexStatusLabel, taskStatusLabel } from '../utils'

const open = defineModel({ type: Boolean, default: false })
const knowledgeForm = defineModel('knowledgeForm', {
  type: Object,
  default: () => ({
    sourceType: 'faq',
    title: '',
    roles: ['tenant'],
    content: ''
  })
})

const emptyCapabilities = {
  online: false,
  model: '',
  embeddingMode: '',
  vectorStore: '',
  indexedKnowledge: 0,
  knowledgeSources: [],
  tools: [],
  skills: []
}

const props = defineProps({
  capabilities: {
    type: Object,
    default: () => ({
      online: false,
      model: '',
      embeddingMode: '',
      vectorStore: '',
      indexedKnowledge: 0,
      knowledgeSources: [],
      tools: [],
      skills: []
    })
  },
  workspaceSummary: { type: Object, default: () => ({}) },
  capabilityChips: { type: Array, default: () => [] },
  knowledgeSourceText: { type: String, default: '合同、政策、FAQ、聊天记录、企业制度' },
  latestTask: { type: Object, default: null },
  houseDocument: { type: Object, default: null },
  knowledgeSubmitting: { type: Boolean, default: false },
  knowledgeSeeding: { type: Boolean, default: false },
  indexTasks: { type: Array, default: () => [] },
  canRunFullSync: { type: Boolean, default: false },
  selectedHouseId: { type: [Number, String], default: '' }
})

defineEmits([
  'refresh',
  'inspect-house-document',
  'create-house-index-task',
  'create-full-index-task',
  'process-pending-tasks',
  'seed-knowledge',
  'submit-knowledge',
  'reset-knowledge',
  'process-task'
])

const capabilitiesView = computed(() => {
  const capabilities = props.capabilities || {}
  return {
    ...emptyCapabilities,
    ...capabilities,
    tools: Array.isArray(capabilities.tools) ? capabilities.tools : [],
    skills: Array.isArray(capabilities.skills) ? capabilities.skills : []
  }
})

const summaryView = computed(() => props.workspaceSummary || {})
const hasSelectedHouse = computed(() => Boolean(props.selectedHouseId))

function isTaskProcessing(row) {
  return ['1', '2'].includes(String(row?.status))
}
</script>

<style scoped>
.ai-console-shell {
  --portal-ink: #172033;
  --portal-muted: #667085;
  --portal-line: #dfe5ea;
  --portal-soft: #f6f8fb;
  --portal-action: #0f766e;

  min-height: 100%;
  padding: 24px;
  background: var(--portal-soft);
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

.ai-console-head span {
  display: block;
  margin-bottom: 8px;
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
}

.ai-console-head h2 {
  margin: 0;
  color: var(--portal-ink);
}

.ai-console-head p {
  margin: 10px 0 0;
  color: var(--portal-muted);
  line-height: 1.65;
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

.console-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.console-summary span {
  padding: 6px 10px;
  color: #4b5563;
  background: #fff;
  border: 1px solid var(--portal-line);
  border-radius: 999px;
  font-size: 12px;
}

.console-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.ai-status-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.status-card,
.task-table {
  background: #fff;
  border: 1px solid var(--portal-line);
  border-radius: 8px;
}

.status-card {
  padding: 14px;
}

.status-card strong {
  display: block;
  margin-bottom: 6px;
  color: var(--portal-ink);
}

.status-card p {
  margin: 0;
  color: var(--portal-muted);
  line-height: 1.6;
  font-size: 13px;
}

.knowledge-card {
  padding: 18px;
  margin-top: 14px;
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

.task-table {
  margin-top: 14px;
  overflow: hidden;
}

.compact-shell :deep(.el-table__cell) {
  padding: 8px 0;
}

@media (max-width: 900px) {
  .ai-console-shell {
    padding: 16px;
  }

  .ai-console-head {
    display: grid;
  }

  .ai-status-grid,
  .knowledge-form__row {
    grid-template-columns: 1fr;
  }
}
</style>
