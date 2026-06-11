<template>
  <div class="app-container rental-test">
    <el-row :gutter="12" class="toolbar">
      <el-col :span="16">
        <el-space wrap>
          <el-button type="primary" icon="Refresh" @click="refreshActive">刷新当前页签</el-button>
          <el-button icon="ChatLineRound" @click="loadChatSessions">刷新会话</el-button>
          <el-tag type="info">当前测试房源ID：{{ selected.houseId || '-' }}</el-tag>
          <el-tag type="info">当前合同ID：{{ selected.contractId || '-' }}</el-tag>
        </el-space>
      </el-col>
      <el-col :span="8">
        <el-input v-model="quick.houseId" placeholder="输入房源ID后可在各页签复用">
          <template #append>
            <el-button @click="selected.houseId = quick.houseId">设为当前</el-button>
          </template>
        </el-input>
      </el-col>
    </el-row>

    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane label="租户闭环" name="tenant">
        <section class="section">
          <el-form :model="tenantQuery" inline>
            <el-form-item label="标题">
              <el-input v-model="tenantQuery.title" clearable placeholder="房源标题" />
            </el-form-item>
            <el-form-item label="城市">
              <el-input v-model="tenantQuery.city" clearable placeholder="城市" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="Search" @click="loadTenantHouses">查询公开房源</el-button>
            </el-form-item>
          </el-form>

          <el-table v-loading="loading.tenantHouses" :data="tenantHouses" border>
            <el-table-column prop="houseId" label="ID" width="80" />
            <el-table-column prop="title" label="标题" min-width="180" />
            <el-table-column prop="city" label="城市" width="100" />
            <el-table-column prop="district" label="区域" width="120" />
            <el-table-column prop="rentAmount" label="租金" width="100" />
            <el-table-column prop="operationMode" label="运营" width="90">
              <template #default="{ row }">{{ row.operationMode === '1' ? '委托' : '房东自营' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="340" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="selectHouse(row.houseId)">选中</el-button>
                <el-button link type="primary" @click="showHouse(row.houseId)">详情</el-button>
                <el-button link type="primary" @click="favoriteHouse(row.houseId)">收藏</el-button>
                <el-button link type="primary" @click="tenantAppointment.houseId = row.houseId">预约</el-button>
                <el-button link type="primary" @click="tenantIntention.houseId = row.houseId">意向</el-button>
                <el-button link type="success" @click="tenantDeal.houseId = row.houseId">成交</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section class="grid-3 section">
          <el-card shadow="never">
            <template #header>预约看房</template>
            <el-form :model="tenantAppointment" label-width="82px">
              <el-form-item label="房源ID"><el-input v-model="tenantAppointment.houseId" /></el-form-item>
              <el-form-item label="预约时间"><el-date-picker v-model="tenantAppointment.appointmentTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item>
              <el-form-item label="备注"><el-input v-model="tenantAppointment.remark" type="textarea" :rows="2" /></el-form-item>
              <el-button type="primary" @click="createAppointment">提交预约</el-button>
            </el-form>
          </el-card>

          <el-card shadow="never">
            <template #header>租房意向</template>
            <el-form :model="tenantIntention" label-width="82px">
              <el-form-item label="房源ID"><el-input v-model="tenantIntention.houseId" /></el-form-item>
              <el-form-item label="意向级别"><el-select v-model="tenantIntention.intentionLevel"><el-option label="高" value="3" /><el-option label="中" value="2" /><el-option label="低" value="1" /></el-select></el-form-item>
              <el-form-item label="备注"><el-input v-model="tenantIntention.note" type="textarea" :rows="2" /></el-form-item>
              <el-button type="primary" @click="createIntention">提交意向</el-button>
            </el-form>
          </el-card>

          <el-card shadow="never">
            <template #header>发起成交</template>
            <el-form :model="tenantDeal" label-width="82px">
              <el-form-item label="房源ID"><el-input v-model="tenantDeal.houseId" /></el-form-item>
              <el-form-item label="起租日"><el-date-picker v-model="tenantDeal.startDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
              <el-form-item label="结束日"><el-date-picker v-model="tenantDeal.endDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
              <el-form-item label="租金"><el-input-number v-model="tenantDeal.rentAmount" :min="0" /></el-form-item>
              <el-button type="success" @click="applyDeal">生成待确认合同</el-button>
            </el-form>
          </el-card>
        </section>

        <section class="section">
          <el-space wrap>
            <el-button @click="loadTenantFavorites">收藏列表</el-button>
            <el-button @click="loadTenantAppointments">预约列表</el-button>
            <el-button @click="loadTenantIntentions">意向列表</el-button>
            <el-button @click="loadTenantContracts">合同列表</el-button>
          </el-space>
          <el-table :data="tenantRecords" border class="mt12">
            <el-table-column v-for="col in tenantRecordColumns" :key="col" :prop="col" :label="col" min-width="120" />
          </el-table>
        </section>
      </el-tab-pane>

      <el-tab-pane label="房东闭环" name="owner">
        <section class="grid-2 section">
          <el-card shadow="never">
            <template #header>提交房源</template>
            <el-form :model="ownerHouse" label-width="88px">
              <el-form-item label="标题"><el-input v-model="ownerHouse.title" /></el-form-item>
              <el-form-item label="城市"><el-input v-model="ownerHouse.city" /></el-form-item>
              <el-form-item label="区域"><el-input v-model="ownerHouse.district" /></el-form-item>
              <el-form-item label="地址"><el-input v-model="ownerHouse.address" /></el-form-item>
              <el-form-item label="租金"><el-input-number v-model="ownerHouse.rentAmount" :min="0" /></el-form-item>
              <el-form-item label="面积"><el-input-number v-model="ownerHouse.area" :min="0" /></el-form-item>
              <el-button type="primary" @click="createOwnerHouseRecord">提交房源</el-button>
            </el-form>
          </el-card>
          <el-card shadow="never">
            <template #header>委托中介</template>
            <el-form :model="ownerEntrust" label-width="88px">
              <el-form-item label="房源ID"><el-input v-model="ownerEntrust.houseId" /></el-form-item>
              <el-form-item label="中介ID"><el-input v-model="ownerEntrust.agentId" /></el-form-item>
              <el-form-item label="备注"><el-input v-model="ownerEntrust.remark" type="textarea" :rows="3" /></el-form-item>
              <el-space>
                <el-button type="primary" @click="submitOwnerAudit">重新提交审核</el-button>
                <el-button type="success" @click="createEntrust">申请委托</el-button>
              </el-space>
            </el-form>
          </el-card>
        </section>
        <section class="section">
          <el-space wrap>
            <el-button type="primary" @click="loadOwnerHouses">我的房源</el-button>
            <el-button @click="loadOwnerEntrusts">我的委托</el-button>
          </el-space>
          <el-table :data="ownerRecords" border class="mt12">
            <el-table-column prop="houseId" label="房源ID" width="90" />
            <el-table-column prop="entrustId" label="委托ID" width="90" />
            <el-table-column prop="title" label="标题" min-width="160" />
            <el-table-column prop="agentId" label="中介ID" width="100" />
            <el-table-column prop="status" label="状态" width="100" />
            <el-table-column prop="auditStatus" label="审核" width="100" />
          </el-table>
        </section>
      </el-tab-pane>

      <el-tab-pane label="中介闭环" name="agent">
        <section class="section">
          <el-space wrap>
            <el-button type="primary" @click="loadAgentEntrusts">受托申请</el-button>
            <el-button @click="loadAgentHouses">受托房源</el-button>
            <el-button @click="loadAgentAppointments">预约处理</el-button>
            <el-button @click="loadAgentIntentions">意向跟进</el-button>
          </el-space>
          <el-table :data="agentRecords" border class="mt12">
            <el-table-column prop="entrustId" label="委托ID" width="90" />
            <el-table-column prop="houseId" label="房源ID" width="90" />
            <el-table-column prop="appointmentId" label="预约ID" width="90" />
            <el-table-column prop="intentionId" label="意向ID" width="90" />
            <el-table-column prop="tenantId" label="租户ID" width="90" />
            <el-table-column prop="title" label="标题" min-width="160" />
            <el-table-column prop="status" label="状态" width="100" />
            <el-table-column label="操作" width="330" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.entrustId" link type="primary" @click="confirmEntrust(row.entrustId)">确认委托</el-button>
                <el-button v-if="row.entrustId" link type="danger" @click="rejectEntrust(row.entrustId)">拒绝委托</el-button>
                <el-button v-if="row.appointmentId" link type="primary" @click="confirmAppointment(row.appointmentId)">确认预约</el-button>
                <el-button v-if="row.appointmentId" link type="success" @click="completeAppointment(row.appointmentId)">完成预约</el-button>
                <el-button v-if="row.intentionId" link type="primary" @click="followIntention(row.intentionId)">跟进</el-button>
                <el-button v-if="row.intentionId" link type="success" @click="dealIntention(row)">成交</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>
      </el-tab-pane>

      <el-tab-pane label="合同与聊天" name="contract">
        <section class="grid-2 section">
          <el-card shadow="never">
            <template #header>合同确认</template>
            <el-form :model="contractAction" label-width="88px">
              <el-form-item label="合同ID"><el-input v-model="contractAction.contractId" /></el-form-item>
              <el-form-item label="意见"><el-input v-model="contractAction.opinion" type="textarea" :rows="2" /></el-form-item>
              <el-space wrap>
                <el-button @click="loadMyContracts">我的合同</el-button>
                <el-button @click="showContract">详情</el-button>
                <el-button type="primary" @click="tenantConfirm">租户确认</el-button>
                <el-button type="primary" @click="ownerConfirm">房东确认</el-button>
                <el-button type="primary" @click="agentConfirm">中介确认</el-button>
                <el-button type="danger" @click="rejectCurrentContract">拒绝</el-button>
              </el-space>
            </el-form>
          </el-card>
          <el-card shadow="never">
            <template #header>业务聊天</template>
            <el-form :model="chatAction" label-width="88px">
              <el-form-item label="会话ID"><el-input v-model="chatAction.sessionId" /></el-form-item>
              <el-form-item label="消息"><el-input v-model="chatAction.content" type="textarea" :rows="2" /></el-form-item>
              <el-space wrap>
                <el-button @click="loadChatSessions">会话列表</el-button>
                <el-button @click="loadChatMessages">消息列表</el-button>
                <el-button type="primary" @click="sendMessage">发送消息</el-button>
              </el-space>
            </el-form>
          </el-card>
        </section>
        <section class="section">
          <el-table :data="contractRecords" border>
            <el-table-column v-for="col in contractRecordColumns" :key="col" :prop="col" :label="col" min-width="130" />
          </el-table>
        </section>
      </el-tab-pane>

      <el-tab-pane label="AI/RAG 预留" name="ai">
        <section class="grid-2 section">
          <el-card shadow="never">
            <template #header>AI 找房对话</template>
            <el-form :model="aiChat" label-width="88px">
              <el-form-item label="问题"><el-input v-model="aiChat.message" type="textarea" :rows="3" /></el-form-item>
              <el-button type="primary" @click="sendAiChat">调用 /rental/ai/chat</el-button>
            </el-form>
          </el-card>
          <el-card shadow="never">
            <template #header>AI/RAG 模块预留</template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="房源索引">审核通过 upsert、下架/成交 delete</el-descriptions-item>
              <el-descriptions-item label="RAG 检索">仅检索公开可租房源</el-descriptions-item>
              <el-descriptions-item label="推荐">租户偏好、预算、通勤、标签匹配</el-descriptions-item>
              <el-descriptions-item label="合同风控">合同条款摘要、风险提示、三方确认展示</el-descriptions-item>
              <el-descriptions-item label="聊天助手">复用合同/预约/意向会话上下文</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </section>
      </el-tab-pane>
    </el-tabs>

    <el-drawer v-model="detailOpen" title="接口返回详情" size="44%">
      <pre class="json-view">{{ detailJson }}</pre>
    </el-drawer>
  </div>
</template>

<script setup name="RentalBusinessTest">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  listTenantHouses,
  getTenantHouse,
  listTenantFavorites,
  favoriteTenantHouse,
  listTenantAppointments,
  createTenantAppointment,
  listTenantIntentions,
  createTenantIntention,
  applyTenantDeal,
  listTenantContracts
} from '@/api/portal/tenant'
import {
  listOwnerHouses,
  createOwnerHouse,
  submitOwnerHouseAudit,
  entrustOwnerHouse,
  listOwnerEntrusts
} from '@/api/portal/owner'
import {
  listAgentEntrusts,
  confirmAgentEntrust,
  rejectAgentEntrust,
  listAgentHouses,
  listAgentAppointments,
  confirmAgentAppointment,
  completeAgentAppointment,
  listAgentIntentions,
  followAgentIntention,
  dealAgentIntention
} from '@/api/portal/agent'
import {
  listMyContracts,
  getContractDetail,
  confirmTenantContract,
  confirmOwnerContract,
  confirmAgentContract,
  rejectContract
} from '@/api/portal/contractBusiness'
import { listChatSessions, listChatMessages, sendChatMessage } from '@/api/portal/chat'
import { sendPortalAiChat } from '@/api/portal/ai'

const activeTab = ref('tenant')
const detailOpen = ref(false)
const detailData = ref({})
const selected = reactive({ houseId: '', contractId: '' })
const quick = reactive({ houseId: '' })
const loading = reactive({ tenantHouses: false })

const tenantQuery = reactive({ title: '', city: '' })
const tenantHouses = ref([])
const tenantRecords = ref([])
const tenantRecordColumns = ref([])
const tenantAppointment = reactive({ houseId: '', appointmentTime: '', remark: '' })
const tenantIntention = reactive({ houseId: '', intentionLevel: '2', note: '' })
const tenantDeal = reactive({ houseId: '', startDate: '', endDate: '', rentAmount: undefined, depositAmount: undefined, paymentCycle: 'monthly' })

const ownerHouse = reactive({ title: '', city: '', district: '', address: '', rentAmount: 0, area: 0, operationMode: '0' })
const ownerEntrust = reactive({ houseId: '', agentId: '', remark: '' })
const ownerRecords = ref([])

const agentRecords = ref([])
const contractRecords = ref([])
const contractRecordColumns = ref([])
const contractAction = reactive({ contractId: '', opinion: '' })
const chatAction = reactive({ sessionId: '', content: '' })
const aiChat = reactive({ message: '' })

const detailJson = computed(() => JSON.stringify(detailData.value, null, 2))

function rowsOf(response) {
  return response?.rows || response?.data || []
}

function showResult(response, message = '操作成功') {
  detailData.value = response
  detailOpen.value = true
  ElMessage.success(message)
}

function selectHouse(houseId) {
  selected.houseId = houseId
  tenantAppointment.houseId = houseId
  tenantIntention.houseId = houseId
  tenantDeal.houseId = houseId
  ownerEntrust.houseId = houseId
}

async function loadTenantHouses() {
  loading.tenantHouses = true
  try {
    const res = await listTenantHouses(tenantQuery)
    tenantHouses.value = rowsOf(res)
  } finally {
    loading.tenantHouses = false
  }
}

async function showHouse(houseId) {
  const res = await getTenantHouse(houseId)
  showResult(res, '已读取房源详情')
}

async function favoriteHouse(houseId) {
  showResult(await favoriteTenantHouse(houseId), '已收藏房源')
}

async function createAppointment() {
  const res = await createTenantAppointment(tenantAppointment)
  showResult(res, '预约已提交')
}

async function createIntention() {
  const res = await createTenantIntention(tenantIntention)
  showResult(res, '意向已提交')
}

async function applyDeal() {
  const res = await applyTenantDeal(tenantDeal.houseId, tenantDeal)
  selected.contractId = res?.contract?.contractId || res?.data?.contract?.contractId || selected.contractId
  contractAction.contractId = selected.contractId
  showResult(res, '成交申请已提交')
}

async function loadTenantFavorites() {
  const res = await listTenantFavorites({})
  tenantRecords.value = rowsOf(res)
  tenantRecordColumns.value = ['favoriteId', 'houseId', 'userId', 'createTime']
}

async function loadTenantAppointments() {
  const res = await listTenantAppointments({})
  tenantRecords.value = rowsOf(res)
  tenantRecordColumns.value = ['appointmentId', 'houseId', 'tenantId', 'agentId', 'appointmentTime', 'status']
}

async function loadTenantIntentions() {
  const res = await listTenantIntentions({})
  tenantRecords.value = rowsOf(res)
  tenantRecordColumns.value = ['intentionId', 'houseId', 'tenantId', 'agentId', 'intentionLevel', 'status']
}

async function loadTenantContracts() {
  const res = await listTenantContracts({})
  tenantRecords.value = rowsOf(res)
  tenantRecordColumns.value = ['contractId', 'houseId', 'tenantId', 'ownerId', 'agentId', 'status']
}

async function createOwnerHouseRecord() {
  showResult(await createOwnerHouse(ownerHouse), '房源已提交审核')
}

async function submitOwnerAudit() {
  showResult(await submitOwnerHouseAudit(ownerEntrust.houseId), '已重新提交审核')
}

async function createEntrust() {
  showResult(await entrustOwnerHouse(ownerEntrust.houseId, ownerEntrust), '委托申请已提交')
}

async function loadOwnerHouses() {
  const res = await listOwnerHouses({})
  ownerRecords.value = rowsOf(res)
}

async function loadOwnerEntrusts() {
  const res = await listOwnerEntrusts({})
  ownerRecords.value = rowsOf(res)
}

async function loadAgentEntrusts() {
  const res = await listAgentEntrusts({})
  agentRecords.value = rowsOf(res)
}

async function loadAgentHouses() {
  const res = await listAgentHouses({})
  agentRecords.value = rowsOf(res)
}

async function loadAgentAppointments() {
  const res = await listAgentAppointments({})
  agentRecords.value = rowsOf(res)
}

async function loadAgentIntentions() {
  const res = await listAgentIntentions({})
  agentRecords.value = rowsOf(res)
}

async function confirmEntrust(entrustId) {
  showResult(await confirmAgentEntrust(entrustId), '委托已确认')
}

async function rejectEntrust(entrustId) {
  showResult(await rejectAgentEntrust(entrustId), '委托已拒绝')
}

async function confirmAppointment(appointmentId) {
  showResult(await confirmAgentAppointment(appointmentId), '预约已确认')
}

async function completeAppointment(appointmentId) {
  showResult(await completeAgentAppointment(appointmentId), '预约已完成')
}

async function followIntention(intentionId) {
  showResult(await followAgentIntention(intentionId, { intentionLevel: '3', note: '测试跟进' }), '意向已跟进')
}

async function dealIntention(row) {
  const data = { tenantId: row.tenantId, rentAmount: row.rentAmount, paymentCycle: 'monthly' }
  showResult(await dealAgentIntention(row.intentionId, data), '意向已转成交')
}

async function loadMyContracts() {
  const res = await listMyContracts({})
  contractRecords.value = rowsOf(res)
  contractRecordColumns.value = ['contractId', 'houseId', 'tenantId', 'ownerId', 'agentId', 'status']
}

async function showContract() {
  const res = await getContractDetail(contractAction.contractId)
  contractRecords.value = res?.data?.confirms || []
  contractRecordColumns.value = ['confirmId', 'contractId', 'userId', 'userRole', 'confirmStatus', 'confirmOpinion', 'confirmTime']
  showResult(res, '已读取合同详情')
}

async function tenantConfirm() {
  showResult(await confirmTenantContract(contractAction.contractId, { opinion: contractAction.opinion }), '租户已确认')
}

async function ownerConfirm() {
  showResult(await confirmOwnerContract(contractAction.contractId, { opinion: contractAction.opinion }), '房东已确认')
}

async function agentConfirm() {
  showResult(await confirmAgentContract(contractAction.contractId, { opinion: contractAction.opinion }), '中介已确认')
}

async function rejectCurrentContract() {
  showResult(await rejectContract(contractAction.contractId, { opinion: contractAction.opinion }), '合同已拒绝')
}

async function loadChatSessions() {
  const res = await listChatSessions()
  contractRecords.value = rowsOf(res)
  contractRecordColumns.value = ['sessionId', 'bizType', 'bizId', 'title', 'lastMessageTime']
}

async function loadChatMessages() {
  const res = await listChatMessages(chatAction.sessionId)
  contractRecords.value = rowsOf(res)
  contractRecordColumns.value = ['messageId', 'sessionId', 'senderId', 'messageType', 'content', 'createTime']
}

async function sendMessage() {
  showResult(await sendChatMessage({ sessionId: chatAction.sessionId, content: chatAction.content, messageType: 'text' }), '消息已发送')
}

async function sendAiChat() {
  showResult(await sendPortalAiChat({ message: aiChat.message }), 'AI 请求已发送')
}

function refreshActive() {
  const actions = {
    tenant: loadTenantHouses,
    owner: loadOwnerHouses,
    agent: loadAgentEntrusts,
    contract: loadMyContracts,
    ai: () => ElMessage.info('AI/RAG 模块已预留，等待后端索引任务与检索接口接入')
  }
  actions[activeTab.value]?.()
}

loadTenantHouses()
</script>

<style scoped>
.rental-test {
  background: #f5f7fa;
}

.toolbar {
  margin-bottom: 12px;
}

.section {
  margin-bottom: 16px;
}

.grid-2,
.grid-3 {
  display: grid;
  gap: 12px;
}

.grid-2 {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.grid-3 {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.mt12 {
  margin-top: 12px;
}

.json-view {
  min-height: 420px;
  padding: 12px;
  overflow: auto;
  color: #23384d;
  background: #f8fafc;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  white-space: pre-wrap;
}

@media (max-width: 1180px) {
  .grid-2,
  .grid-3 {
    grid-template-columns: 1fr;
  }
}
</style>
