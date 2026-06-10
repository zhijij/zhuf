<template>
  <div class="portal-home app-container">
    <section class="hero">
      <div>
        <p class="eyebrow">Smart Rental Portal</p>
        <h1>{{ pageTitle }}</h1>
        <p class="summary">
          这是统一业务门户。管理员继续使用 RuoYi 后台，用户、户主、中介登录后只看到各自可用的业务功能。
        </p>
        <div class="actions">
          <el-button
            v-for="item in entryButtons"
            :key="item.key"
            :type="item.key === activeKey ? 'primary' : 'default'"
            @click="switchRole(item.key)"
          >
            {{ item.label }}
          </el-button>
        </div>
      </div>
      <div class="stats">
        <article v-for="item in stats" :key="item.label" class="stat-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </article>
      </div>
    </section>

    <section class="panel-grid">
      <article v-for="panel in visiblePanels" :key="panel.key" class="role-panel">
        <h2>{{ panel.title }}</h2>
        <p>{{ panel.desc }}</p>
        <ul>
          <li v-for="item in panel.items" :key="item">{{ item }}</li>
        </ul>
      </article>
    </section>

    <section class="path-grid">
      <article class="path-card">
        <h2>角色定义</h2>
        <p>管理员：系统开发者 / 超级管理员</p>
        <p>用户：租户通用角色，负责找房、预约、收藏、AI 问答</p>
        <p>户主：负责房源发布、委托中介、查看合同与收益</p>
        <p>中介：负责受托房源、客户意向、预约处理、AI 文案辅助</p>
      </article>
      <article class="path-card">
        <h2>代码位置</h2>
        <p>后台前端：<code>src/views/system</code></p>
        <p>统一门户：<code>src/views/portal</code></p>
        <p>门户接口：<code>src/api/portal</code></p>
        <p>AI 服务：<code>ai-service</code></p>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import useUserStore from '@/store/modules/user'
import { getPortalHomeSummary } from '@/api/portal/home'

const userStore = useUserStore()

const activeRole = computed(() => {
  const roles = userStore.roles || []
  if (roles.includes('admin')) return 'admin'
  if (roles.includes('agent')) return 'agent'
  if (roles.includes('owner')) return 'owner'
  return 'user'
})

const activeKey = ref('user')
const stats = ref([
  { label: '房源', value: '--' },
  { label: '预约', value: '--' },
  { label: '合同', value: '--' }
])

const rolePanels = {
  user: [
    {
      key: 'user',
      title: '用户功能',
      desc: '面向租户的统一业务入口。',
      items: ['房源搜索', '房源详情', '收藏房源', '预约看房', 'AI 找房问答']
    }
  ],
  owner: [
    {
      key: 'owner',
      title: '户主功能',
      desc: '面向房东的房源经营入口。',
      items: ['房源发布', '委托关系', '预约查看', '合同查看', '收益分析']
    }
  ],
  agent: [
    {
      key: 'agent',
      title: '中介功能',
      desc: '面向中介的跟客与房源管理入口。',
      items: ['受托房源', '客户意向', '预约管理', 'AI 文案', '客户画像']
    }
  ],
  admin: [
    {
      key: 'admin',
      title: '管理员功能',
      desc: '管理员仍然是后台管理与开发配置入口。',
      items: ['用户管理', '角色管理', '菜单管理', '业务审核', '系统监控']
    }
  ]
}

const entryButtons = [
  { key: 'admin', label: '管理员' },
  { key: 'user', label: '用户' },
  { key: 'owner', label: '户主' },
  { key: 'agent', label: '中介' }
]

const pageTitle = computed(() => {
  return {
    admin: '管理员门户',
    user: '用户门户',
    owner: '户主门户',
    agent: '中介门户'
  }[activeKey.value]
})

const visiblePanels = computed(() => rolePanels[activeKey.value] || rolePanels.user)

function switchRole(key) {
  activeKey.value = key
}

onMounted(async () => {
  activeKey.value = activeRole.value

  try {
    const summary = await getPortalHomeSummary()
    const data = summary?.data || {}
    stats.value = [
      { label: '房源', value: data.houseCount ?? 0 },
      { label: '预约', value: data.appointmentCount ?? 0 },
      { label: '合同', value: data.contractCount ?? 0 }
    ]
  } catch (e) {
    // keep skeleton usable before backend data is ready
  }
})
</script>

<style scoped>
.portal-home {
  min-height: calc(100vh - 84px);
  background: #f4f7fb;
}

.hero {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 20px;
  padding: 24px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 12px 30px rgba(28, 48, 71, 0.08);
}

.eyebrow {
  margin: 0 0 12px;
  font-size: 12px;
  color: #4c78a8;
  text-transform: uppercase;
}

.hero h1 {
  margin: 0 0 12px;
  font-size: 30px;
  color: #1d3348;
}

.summary {
  margin: 0;
  line-height: 1.7;
  color: #546579;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 20px;
}

.stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.stat-card,
.path-card,
.role-panel {
  padding: 18px;
  background: #fff;
  border: 1px solid #dbe6f1;
  border-radius: 8px;
}

.stat-card {
  background: #f7fbff;
}

.stat-card span {
  display: block;
  margin-bottom: 10px;
  color: #648099;
  font-size: 13px;
}

.stat-card strong {
  font-size: 24px;
  color: #17324a;
}

.panel-grid,
.path-grid {
  display: grid;
  gap: 16px;
  margin-top: 20px;
}

.panel-grid {
  grid-template-columns: 1fr;
}

.path-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.path-card h2,
.role-panel h2 {
  margin: 0 0 10px;
  color: #17324a;
}

.path-card p,
.role-panel p {
  margin: 0 0 10px;
  line-height: 1.6;
  color: #587082;
}

.role-panel ul {
  margin: 0;
  padding-left: 18px;
  color: #36546d;
}

@media (max-width: 960px) {
  .hero,
  .path-grid,
  .stats {
    grid-template-columns: 1fr;
  }
}
</style>