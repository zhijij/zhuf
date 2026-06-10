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
      <el-form-item label="意向等级:1低,2中,3高" prop="intentionLevel">
        <el-input
          v-model="queryParams.intentionLevel"
          placeholder="请输入意向等级:1低,2中,3高"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="预算" prop="budgetAmount">
        <el-input
          v-model="queryParams.budgetAmount"
          placeholder="请输入预算"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="期望入住时间" prop="expectedMoveIn">
        <el-date-picker clearable
          v-model="queryParams.expectedMoveIn"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择期望入住时间">
        </el-date-picker>
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
          v-hasPermi="['system:intention:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:intention:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:intention:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:intention:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="intentionList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="意向ID" align="center" prop="intentionId" />
      <el-table-column label="房源ID" align="center" prop="houseId" />
      <el-table-column label="租户用户ID" align="center" prop="tenantId" />
      <el-table-column label="户主用户ID" align="center" prop="ownerId" />
      <el-table-column label="中介用户ID，自主出租时为空" align="center" prop="agentId" />
      <el-table-column label="意向等级:1低,2中,3高" align="center" prop="intentionLevel" />
      <el-table-column label="状态:0跟进中,1已成交,2无效,3放弃" align="center" prop="status" />
      <el-table-column label="预算" align="center" prop="budgetAmount" />
      <el-table-column label="期望入住时间" align="center" prop="expectedMoveIn" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.expectedMoveIn, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="note" />
      <el-table-column label="AI意向摘要" align="center" prop="aiSummary" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:intention:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:intention:remove']">删除</el-button>
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

    <!-- 添加或修改租赁意向对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="intentionRef" :model="form" :rules="rules" label-width="100px">
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
            <el-form-item label="意向等级:1低,2中,3高" prop="intentionLevel">
              <el-input v-model="form.intentionLevel" placeholder="请输入意向等级:1低,2中,3高" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="预算" prop="budgetAmount">
              <el-input v-model="form.budgetAmount" placeholder="请输入预算" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="期望入住时间" prop="expectedMoveIn">
              <el-date-picker clearable
                v-model="form.expectedMoveIn"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择期望入住时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="note">
              <el-input v-model="form.note" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="AI意向摘要" prop="aiSummary">
              <el-input v-model="form.aiSummary" type="textarea" placeholder="请输入内容" />
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

<script setup name="Intention">
import { listIntention, getIntention, delIntention, addIntention, updateIntention } from "@/api/system/intention"

const { proxy } = getCurrentInstance()

const intentionList = ref([])
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
    intentionLevel: undefined,
    status: undefined,
    budgetAmount: undefined,
    expectedMoveIn: undefined,
    note: undefined,
    aiSummary: undefined,
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
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询租赁意向列表 */
function getList() {
  loading.value = true
  listIntention(queryParams.value).then(response => {
    intentionList.value = response.rows
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
    intentionId: null,
    houseId: null,
    tenantId: null,
    ownerId: null,
    agentId: null,
    intentionLevel: null,
    status: null,
    budgetAmount: null,
    expectedMoveIn: null,
    note: null,
    aiSummary: null,
    createTime: null,
    updateTime: null
  }
  proxy.resetForm("intentionRef")
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
  ids.value = selection.map(item => item.intentionId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加租赁意向"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _intentionId = row.intentionId || ids.value
  getIntention(_intentionId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改租赁意向"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["intentionRef"].validate(valid => {
    if (valid) {
      if (form.value.intentionId != null) {
        updateIntention(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addIntention(form.value).then(() => {
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
  const _intentionIds = row.intentionId || ids.value
  proxy.$modal.confirm('是否确认删除租赁意向编号为"' + _intentionIds + '"的数据项？').then(function() {
    return delIntention(_intentionIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/intention/export', {
    ...queryParams.value
  }, `intention_${new Date().getTime()}.xlsx`)
}

getList()
</script>
