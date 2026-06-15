<template>
  <div class="home-dashboard">
    <section class="overview">
      <div>
        <p class="eyebrow">运营后台</p>
        <h1>智能AI房屋租赁系统</h1>
        <p class="summary">
          面向租户、户主、中介和审核员的租赁业务工作台，覆盖房源审核、委托承接、预约意向、合同协作和 AI 知识库检索。
        </p>
      </div>
      <div class="overview-actions">
        <el-button type="primary" icon="Guide" @click="goPortal">进入业务工作台</el-button>
        <el-button v-if="isSuperAdmin" plain icon="Connection" @click="goAiOps">查看 AI 索引任务</el-button>
      </div>
    </section>

    <section class="metric-grid">
      <article v-for="item in metrics" :key="item.label" class="metric-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <p>{{ item.desc }}</p>
      </article>
    </section>

    <section class="workbench-grid">
      <article class="panel wide">
        <div class="panel-head">
          <div>
            <p>业务链路</p>
            <h2>从房源上架到合同确认</h2>
          </div>
          <el-tag effect="plain" type="success">生产流程</el-tag>
        </div>
        <div class="flow">
          <div v-for="step in flowSteps" :key="step.title">
            <span>{{ step.index }}</span>
            <strong>{{ step.title }}</strong>
            <p>{{ step.desc }}</p>
          </div>
        </div>
      </article>

      <article v-if="isSuperAdmin" class="panel">
        <div class="panel-head">
          <div>
            <p>角色入口</p>
            <h2>权限隔离</h2>
          </div>
        </div>
        <ul class="role-list">
          <li v-for="role in roles" :key="role.name">
            <span>{{ role.name }}</span>
            <p>{{ role.scope }}</p>
          </li>
        </ul>
      </article>

      <article class="panel">
        <div class="panel-head">
          <div>
            <p>AI 能力</p>
            <h2>智能体接入状态</h2>
          </div>
        </div>
        <div class="capability-list">
          <span>房源推荐与问答</span>
          <span>审核合规建议</span>
          <span>合同风险摘要</span>
          <span>pgvector 房源知识库</span>
        </div>
      </article>

      <article class="panel wide">
        <div class="panel-head">
          <div>
            <p>上线检查</p>
            <h2>正式环境必须确认</h2>
          </div>
          <el-tag effect="plain" type="warning">交付前</el-tag>
        </div>
        <div class="release-grid">
          <div v-for="item in releaseChecks" :key="item">
            <el-icon><Select /></el-icon>
            <span>{{ item }}</span>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup name="Index">
import useUserStore from '@/store/modules/user'

const router = useRouter()
const userStore = useUserStore()
const isSuperAdmin = computed(() => (userStore.roles || []).includes('admin'))

onMounted(() => {
  if (!isSuperAdmin.value) {
    if ((userStore.roles || []).includes('auditor')) {
      router.replace('/portal/auditor')
      return
    }
    router.replace('/portal/index')
  }
})

const metrics = [
  { label: '业务角色', value: '4 类', desc: '租户、户主、中介、审核员独立工作台' },
  { label: '核心流程', value: '7 条', desc: '房源、审核、委托、预约、意向、合同、聊天' },
  { label: 'AI 服务', value: '3 层', desc: '规则工具、LLM 润色、pgvector 检索' },
  { label: '部署形态', value: '容器化', desc: '前端、后端、AI、MySQL、Redis、pgvector' }
]

const flowSteps = [
  { index: '01', title: '户主发布', desc: '户主创建房源并提交审核，审核前可修改和重新提交。' },
  { index: '02', title: '合规审核', desc: '审核员检查房源信息，通过后发布，驳回时记录原因。' },
  { index: '03', title: '中介承接', desc: '中介申请或确认委托，进入预约带看和意向跟进。' },
  { index: '04', title: '租户成交', desc: '租户预约、收藏、提交意向，成交后进入合同协作。' }
]

const roles = [
  { name: '超级管理员', scope: '只进入后台管理，不参与业务审核动作。' },
  { name: '审核员', scope: '负责房源合规审查和 AI 索引维护。' },
  { name: '户主 / 中介', scope: '管理自己的房源、委托、预约和合同。' },
  { name: '租户', scope: '浏览公开房源，发起预约、意向和成交申请。' }
]

const releaseChecks = [
  '关闭开发账号快捷入口',
  '替换生产数据库和 Redis 强密码',
  '配置 HTTPS 域名和反向代理',
  '配置千问/百炼 API Key 与 embedding Key',
  '轮换云存储密钥并移出源码',
  '建立 MySQL、pgvector、上传文件备份策略'
]

function goPortal() {
  router.push('/portal/index')
}

function goAiOps() {
  router.push('/system/task')
}
</script>

<style scoped lang="scss">
.home-dashboard {
  min-height: calc(100vh - 84px);
  padding: 24px;
  color: var(--sr-text);
  background:
    radial-gradient(circle at 18% 12%, rgba(26, 115, 232, 0.12), transparent 28%),
    radial-gradient(circle at 84% 18%, rgba(147, 52, 230, 0.1), transparent 30%),
    #f8fafd;
}

.overview,
.panel,
.metric-card {
  background: var(--sr-surface);
  border: 1px solid var(--sr-line);
  border-radius: 28px;
  box-shadow: 0 18px 54px rgba(60, 64, 67, 0.08);
}

.overview {
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

.overview h1,
.panel-head h2 {
  margin: 0;
  font-weight: 700;
  letter-spacing: 0;
}

.overview h1 {
  color: transparent;
  background: linear-gradient(90deg, #1a73e8, #9334e6 54%, #007b83);
  background-clip: text;
  font-size: 30px;
}

.summary {
  max-width: 760px;
  margin: 14px 0 0;
  color: var(--sr-muted);
  line-height: 1.7;
}

.overview-actions {
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
.flow p,
.role-list p {
  margin: 0;
  color: var(--sr-muted);
  line-height: 1.55;
}

.workbench-grid {
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
}

.flow,
.release-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.flow div,
.release-grid div,
.role-list li {
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

.flow strong,
.role-list span {
  display: block;
  margin: 8px 0;
  color: var(--sr-text-strong);
}

.role-list {
  display: grid;
  gap: 10px;
  padding: 0;
  margin: 0;
  list-style: none;
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

.release-grid div {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--sr-text);
}

.release-grid .el-icon {
  color: var(--sr-primary);
}

@media (max-width: 1180px) {
  .metric-grid,
  .flow,
  .release-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .home-dashboard {
    padding: 14px;
  }

  .overview {
    display: block;
    padding: 22px;
  }

  .overview-actions {
    justify-content: flex-start;
    margin-top: 18px;
  }

  .metric-grid,
  .workbench-grid,
  .flow,
  .release-grid {
    grid-template-columns: 1fr;
  }

  .panel.wide {
    grid-column: span 1;
  }
}
</style>
