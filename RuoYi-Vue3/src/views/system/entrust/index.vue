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
      <el-form-item label="户主用户ID" prop="ownerId">
        <el-input
          v-model="queryParams.ownerId"
          placeholder="请输入户主用户ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="中介用户ID" prop="agentId">
        <el-input
          v-model="queryParams.agentId"
          placeholder="请输入中介用户ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="佣金比例" prop="commissionRate">
        <el-input
          v-model="queryParams.commissionRate"
          placeholder="请输入佣金比例"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="委托开始日期" prop="startDate">
        <el-date-picker clearable
          v-model="queryParams.startDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择委托开始日期">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="委托结束日期" prop="endDate">
        <el-date-picker clearable
          v-model="queryParams.endDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择委托结束日期">
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
          v-hasPermi="['system:entrust:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:entrust:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:entrust:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:entrust:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="entrustList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="委托ID" align="center" prop="entrustId" />
      <el-table-column label="房源ID" align="center" prop="houseId" />
      <el-table-column label="户主用户ID" align="center" prop="ownerId" />
      <el-table-column label="中介用户ID" align="center" prop="agentId" />
      <el-table-column label="委托范围:发布,预约,带看,签约等" align="center" prop="entrustScope" />
      <el-table-column label="佣金比例" align="center" prop="commissionRate" />
      <el-table-column label="委托开始日期" align="center" prop="startDate" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.startDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="委托结束日期" align="center" prop="endDate" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.endDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态:0待确认,1生效中,2已拒绝,3已终止,4已过期" align="center" prop="status" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:entrust:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:entrust:remove']">删除</el-button>
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

    <!-- 添加或修改房源委托关系对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="entrustRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="房源ID" prop="houseId">
              <el-input v-model="form.houseId" placeholder="请输入房源ID" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="户主用户ID" prop="ownerId">
              <el-input v-model="form.ownerId" placeholder="请输入户主用户ID" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="中介用户ID" prop="agentId">
              <el-input v-model="form.agentId" placeholder="请输入中介用户ID" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="委托范围:发布,预约,带看,签约等" prop="entrustScope">
              <el-input v-model="form.entrustScope" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="佣金比例" prop="commissionRate">
              <el-input v-model="form.commissionRate" placeholder="请输入佣金比例" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="委托开始日期" prop="startDate">
              <el-date-picker clearable
                v-model="form.startDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择委托开始日期">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="委托结束日期" prop="endDate">
              <el-date-picker clearable
                v-model="form.endDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择委托结束日期">
              </el-date-picker>
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

<script setup name="Entrust">
import { listEntrust, getEntrust, delEntrust, addEntrust, updateEntrust } from "@/api/system/entrust"

const { proxy } = getCurrentInstance()

const entrustList = ref([])
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
    ownerId: undefined,
    agentId: undefined,
    entrustScope: undefined,
    commissionRate: undefined,
    startDate: undefined,
    endDate: undefined,
    status: undefined,
  },
  rules: {
    houseId: [
      { required: true, message: "房源ID不能为空", trigger: "blur" }
    ],
    ownerId: [
      { required: true, message: "户主用户ID不能为空", trigger: "blur" }
    ],
    agentId: [
      { required: true, message: "中介用户ID不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询房源委托关系列表 */
function getList() {
  loading.value = true
  listEntrust(queryParams.value).then(response => {
    entrustList.value = response.rows
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
    entrustId: null,
    houseId: null,
    ownerId: null,
    agentId: null,
    entrustScope: null,
    commissionRate: null,
    startDate: null,
    endDate: null,
    status: null,
    createTime: null,
    updateTime: null
  }
  proxy.resetForm("entrustRef")
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
  ids.value = selection.map(item => item.entrustId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加房源委托关系"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _entrustId = row.entrustId || ids.value
  getEntrust(_entrustId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改房源委托关系"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["entrustRef"].validate(valid => {
    if (valid) {
      if (form.value.entrustId != null) {
        updateEntrust(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addEntrust(form.value).then(() => {
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
  const _entrustIds = row.entrustId || ids.value
  proxy.$modal.confirm('是否确认删除房源委托关系编号为"' + _entrustIds + '"的数据项？').then(function() {
    return delEntrust(_entrustIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/entrust/export', {
    ...queryParams.value
  }, `entrust_${new Date().getTime()}.xlsx`)
}

getList()
</script>
