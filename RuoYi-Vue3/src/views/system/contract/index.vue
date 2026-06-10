<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="房源ID" prop="houseId">
        <el-input
          v-model="queryParams.houseId"
          placeholder="请输入房源ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="租户用户ID" prop="tenantId">
        <el-input
          v-model="queryParams.tenantId"
          placeholder="请输入租户用户ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="户主用户ID" prop="ownerId">
        <el-input
          v-model="queryParams.ownerId"
          placeholder="请输入户主用户ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="中介用户ID，自主出租时为空" prop="agentId">
        <el-input
          v-model="queryParams.agentId"
          placeholder="请输入中介用户ID，自主出租时为空"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="合同编号" prop="contractNo">
        <el-input
          v-model="queryParams.contractNo"
          placeholder="请输入合同编号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="租期开始" prop="startDate">
        <el-date-picker clearable
          v-model="queryParams.startDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择租期开始">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="租期结束" prop="endDate">
        <el-date-picker clearable
          v-model="queryParams.endDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择租期结束">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="月租金" prop="rentAmount">
        <el-input
          v-model="queryParams.rentAmount"
          placeholder="请输入月租金"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="押金" prop="depositAmount">
        <el-input
          v-model="queryParams.depositAmount"
          placeholder="请输入押金"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="付款周期" prop="paymentCycle">
        <el-input
          v-model="queryParams.paymentCycle"
          placeholder="请输入付款周期"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['system:contract:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:contract:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:contract:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:contract:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="contractList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="合同ID" align="center" prop="contractId" />
      <el-table-column label="房源ID" align="center" prop="houseId" />
      <el-table-column label="租户用户ID" align="center" prop="tenantId" />
      <el-table-column label="户主用户ID" align="center" prop="ownerId" />
      <el-table-column label="中介用户ID，自主出租时为空" align="center" prop="agentId" />
      <el-table-column label="合同编号" align="center" prop="contractNo" />
      <el-table-column label="租期开始" align="center" prop="startDate" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.startDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="租期结束" align="center" prop="endDate" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.endDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="月租金" align="center" prop="rentAmount" />
      <el-table-column label="押金" align="center" prop="depositAmount" />
      <el-table-column label="付款周期" align="center" prop="paymentCycle" />
      <el-table-column label="合同内容" align="center" prop="contractContent" />
      <el-table-column label="AI风险摘要" align="center" prop="aiRiskSummary" />
      <el-table-column label="状态:0草稿,1待签,2生效,3终止,4作废" align="center" prop="status" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:contract:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:contract:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改租赁合同对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="contractRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="房源ID" prop="houseId">
              <el-input v-model="form.houseId" placeholder="请输入房源ID" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="租户用户ID" prop="tenantId">
              <el-input v-model="form.tenantId" placeholder="请输入租户用户ID" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="户主用户ID" prop="ownerId">
              <el-input v-model="form.ownerId" placeholder="请输入户主用户ID" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="中介用户ID，自主出租时为空" prop="agentId">
              <el-input v-model="form.agentId" placeholder="请输入中介用户ID，自主出租时为空" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="合同编号" prop="contractNo">
              <el-input v-model="form.contractNo" placeholder="请输入合同编号" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="租期开始" prop="startDate">
              <el-date-picker clearable
                v-model="form.startDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择租期开始">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="租期结束" prop="endDate">
              <el-date-picker clearable
                v-model="form.endDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择租期结束">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="月租金" prop="rentAmount">
              <el-input v-model="form.rentAmount" placeholder="请输入月租金" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="押金" prop="depositAmount">
              <el-input v-model="form.depositAmount" placeholder="请输入押金" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="付款周期" prop="paymentCycle">
              <el-input v-model="form.paymentCycle" placeholder="请输入付款周期" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="合同内容">
              <editor v-model="form.contractContent" :min-height="192"/>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="AI风险摘要" prop="aiRiskSummary">
              <el-input v-model="form.aiRiskSummary" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Contract">
import { listContract, getContract, delContract, addContract, updateContract } from "@/api/system/contract"

const { proxy } = getCurrentInstance()

const contractList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    houseId: undefined,
    tenantId: undefined,
    ownerId: undefined,
    agentId: undefined,
    contractNo: undefined,
    startDate: undefined,
    endDate: undefined,
    rentAmount: undefined,
    depositAmount: undefined,
    paymentCycle: undefined,
    contractContent: undefined,
    aiRiskSummary: undefined,
    status: undefined,
  },
  rules: {
    houseId: [
      { required: true, message: "房源ID不能为空", trigger: "blur" }
    ],
    tenantId: [
      { required: true, message: "租户用户ID不能为空", trigger: "blur" }
    ],
    ownerId: [
      { required: true, message: "户主用户ID不能为空", trigger: "blur" }
    ],
    contractNo: [
      { required: true, message: "合同编号不能为空", trigger: "blur" }
    ],
    startDate: [
      { required: true, message: "租期开始不能为空", trigger: "blur" }
    ],
    endDate: [
      { required: true, message: "租期结束不能为空", trigger: "blur" }
    ],
    rentAmount: [
      { required: true, message: "月租金不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询租赁合同列表 */
function getList() {
  loading.value = true
  listContract(queryParams.value).then(response => {
    contractList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  form.value = {
    contractId: null,
    houseId: null,
    tenantId: null,
    ownerId: null,
    agentId: null,
    contractNo: null,
    startDate: null,
    endDate: null,
    rentAmount: null,
    depositAmount: null,
    paymentCycle: null,
    contractContent: null,
    aiRiskSummary: null,
    status: null,
    createTime: null,
    updateTime: null
  }
  proxy.resetForm("contractRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.contractId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加租赁合同"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _contractId = row.contractId || ids.value
  getContract(_contractId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改租赁合同"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["contractRef"].validate(valid => {
    if (valid) {
      if (form.value.contractId != null) {
        updateContract(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addContract(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _contractIds = row.contractId || ids.value
  proxy.$modal.confirm('是否确认删除租赁合同编号为"' + _contractIds + '"的数据项？').then(function() {
    return delContract(_contractIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/contract/export', {
    ...queryParams.value
  }, `contract_${new Date().getTime()}.xlsx`)
}

getList()
</script>
