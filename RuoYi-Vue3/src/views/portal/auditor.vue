<template>
  <div class="auditor-shell">
    <section class="hero">
      <div>
        <p class="eyebrow">审核工作台</p>
        <h1>房源合规审核中心</h1>
        <p class="summary">
          面向审核员的独立工作台，聚焦待审房源、合规风险、索引状态和审核动作，不和租户、户主、中介共用首页。
        </p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" icon="Finished" @click="goAuditWorkspace">进入审核队列</el-button>
      </div>
    </section>

    <section class="metric-grid">
      <article class="metric-card">
        <span>公开房源</span>
        <strong>{{ summary.houseCount || 0 }}</strong>
        <p>已在门户公开展示的房源数量</p>
      </article>
      <article class="metric-card">
        <span>已索引房源</span>
        <strong>{{ summary.indexedHouseCount || 0 }}</strong>
        <p>已进入 pgvector 检索库的房源数量</p>
      </article>
      <article class="metric-card">
        <span>平均租金</span>
        <strong>{{ summary.avgRent || 0 }}</strong>
        <p>当前公开房源的平均租金</p>
      </article>
      <article class="metric-card">
        <span>AI 能力</span>
        <strong>{{ capabilityLabel }}</strong>
        <p>{{ capabilityDesc }}</p>
      </article>
    </section>

    <section class="panel-grid">
      <article class="panel wide">
        <div class="panel-head">
          <div>
            <p>工作范围</p>
            <h2>审核动作只属于审核员</h2>
          </div>
          <el-tag effect="plain" type="success">独立入口</el-tag>
        </div>
        <div class="flow">
          <div v-for="item in flowItems" :key="item.title">
            <span>{{ item.index }}</span>
            <strong>{{ item.title }}</strong>
            <p>{{ item.desc }}</p>
          </div>
        </div>
      </article>

      <article class="panel">
        <div class="panel-head">
          <div>
            <p>AI 审核能力</p>
            <h2>辅助而不代替</h2>
          </div>
        </div>
        <ul class="simple-list">
          <li>检查标题、地址、租金、面积、户主字段是否齐全</li>
          <li>生成审核意见草稿，保留人工通过和驳回</li>
          <li>参考 AI 审核建议，最终结论仍由审核员确认</li>
          <li>不自动发布房源，不替代人工审查结论</li>
        </ul>
      </article>

      <article class="panel">
        <div class="panel-head">
          <div>
            <p>RAG 状态</p>
            <h2>知识库与向量检索</h2>
          </div>
        </div>
        <div class="capability-list">
          <span>{{ aiCapabilities.vectorStore || 'PostgreSQL + pgvector' }}</span>
          <span>{{ aiCapabilities.embeddingMode || 'local-hash' }}</span>
          <span>{{ aiCapabilities.embeddingModel || 'embedding 未配置' }}</span>
        </div>
        <p class="rag-note">
          房源文本会切块后写入向量表，问答和推荐先做检索，再把结果交给大模型润色。
        </p>
      </article>
    </section>
  </div>
</template>

<script setup name="AuditorWorkbench">
import { computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import useUserStore from '@/store/modules/user'
import { getPortalHomeSummary } from '@/api/portal/home'
import { getAiCapabilities } from '@/api/portal/ai'

const router = useRouter()
const userStore = useUserStore()

const summary = reactive({
  houseCount: 0,
  avgRent: 0,
  pricedHouseCount: 0,
  indexedHouseCount: 0
})

const aiCapabilities = reactive({
  embeddingMode: '',
  embeddingModel: '',
  vectorStore: '',
  langchain: {}
})

const capabilityLabel = computed(() => aiCapabilities.embeddingMode === 'remote' ? '远程向量' : '本地向量')
const capabilityDesc = computed(() => aiCapabilities.vectorStore || '房源文本切块 + 向量检索 + LLM 润色')

const flowItems = [
  { index: '01', title: '接收待审房源', desc: '聚焦待审核队列，不与租户、户主、中介共享首页视图。' },
  { index: '02', title: '生成审核建议', desc: 'AI 读取房源字段后生成风险提示和审核意见草稿。' },
  { index: '03', title: '人工确认结论', desc: '通过、驳回、补充说明都由审核员手动提交。' },
  { index: '04', title: '维护检索索引', desc: '审核通过后的房源进入向量索引，服务推荐、问答和合同联动。' }
]

onMounted(async () => {
  if (!(userStore.roles || []).includes('auditor')) {
    router.replace('/portal/index')
    return
  }
  await Promise.all([loadSummary(), loadAiCapabilities()])
})

async function loadSummary() {
  const res = await getPortalHomeSummary()
  Object.assign(summary, res?.data || {})
}

async function loadAiCapabilities() {
  const res = await getAiCapabilities()
  Object.assign(aiCapabilities, res?.data || {})
}

function goAuditWorkspace() {
  router.push({ path: '/portal/index', query: { entry: 'audit' } })
}

</script>

<style scoped lang="scss">
.auditor-shell {
  min-height: calc(100vh - 84px);
  padding: 24px;
  color: var(--sr-text);
  background:
    radial-gradient(circle at 18% 12%, rgba(26, 115, 232, 0.12), transparent 28%),
    radial-gradient(circle at 84% 18%, rgba(147, 52, 230, 0.1), transparent 30%),
    #f8fafd;
}

.hero,
.panel,
.metric-card {
  background: var(--sr-surface);
  border: 1px solid var(--sr-line);
  border-radius: 28px;
  box-shadow: 0 18px 54px rgba(60, 64, 67, 0.08);
}

.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 28px 32px;
}

.eyebrow,
.panel-head p {
  margin: 0 0 8px;
  color: var(--sr-muted);
  font-size: 13px;
}

.hero h1,
.panel-head h2 {
  margin: 0;
  letter-spacing: 0;
}

.hero h1 {
  color: transparent;
  background: linear-gradient(90deg, #1a73e8, #9334e6 54%, #007b83);
  background-clip: text;
  font-size: 30px;
  font-weight: 700;
}

.summary {
  max-width: 760px;
  margin: 14px 0 0;
  color: var(--sr-muted);
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.metric-card {
  padding: 18px;
}

.metric-card span {
  color: var(--sr-muted);
  font-size: 13px;
}

.metric-card strong {
  display: block;
  margin: 8px 0;
  color: var(--sr-primary);
  font-size: 26px;
}

.metric-card p,
.simple-list li,
.flow p,
.rag-note {
  margin: 0;
  color: var(--sr-muted);
  line-height: 1.6;
}

.panel-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.panel {
  padding: 22px;
}

.panel.wide {
  grid-column: span 2;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.panel-head h2 {
  font-size: 20px;
  font-weight: 700;
}

.flow {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.flow div {
  padding: 14px;
  background: #f8fafd;
  border: 1px solid var(--sr-line);
  border-radius: 20px;
}

.flow span {
  color: var(--sr-warning);
  font-size: 12px;
  font-weight: 700;
}

.flow strong {
  display: block;
  margin: 8px 0;
  color: var(--sr-text-strong);
}

.simple-list {
  display: grid;
  gap: 10px;
  padding-left: 18px;
  margin: 0;
}

.capability-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.capability-list span {
  padding: 9px 12px;
  color: var(--sr-primary);
  background: #e8f0fe;
  border: 1px solid rgba(11, 87, 208, 0.16);
  border-radius: 999px;
  font-size: 13px;
}

.rag-note {
  margin-top: 16px;
}

@media (max-width: 1180px) {
  .metric-grid,
  .flow {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .auditor-shell {
    padding: 14px;
  }

  .hero {
    display: block;
    padding: 22px;
  }

  .hero-actions {
    justify-content: flex-start;
    margin-top: 18px;
  }

  .metric-grid,
  .panel-grid,
  .flow {
    grid-template-columns: 1fr;
  }

  .panel.wide {
    grid-column: span 1;
  }
}
</style>
