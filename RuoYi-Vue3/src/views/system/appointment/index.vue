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
      <el-form-item label="预约时间" prop="appointmentTime">
        <el-date-picker clearable
          v-model="queryParams.appointmentTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择预约时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="联系人" prop="contactName">
        <el-input
          v-model="queryParams.contactName"
          placeholder="请输入联系人"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="联系电话" prop="contactPhone">
        <el-input
          v-model="queryParams.contactPhone"
          placeholder="请输入联系电话"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="来源:0手动,1AI助手" prop="source">
        <el-input
          v-model="queryParams.source"
          placeholder="请输入来源:0手动,1AI助手"
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
          v-hasPermi="['system:appointment:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:appointment:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:appointment:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:appointment:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="appointmentList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="预约ID" align="center" prop="appointmentId" />
      <el-table-column label="房源ID" align="center" prop="houseId" />
      <el-table-column label="租户用户ID" align="center" prop="tenantId" />
      <el-table-column label="户主用户ID" align="center" prop="ownerId" />
      <el-table-column label="中介用户ID，自主出租时为空" align="center" prop="agentId" />
      <el-table-column label="预约时间" align="center" prop="appointmentTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.appointmentTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="联系人" align="center" prop="contactName" />
      <el-table-column label="联系电话" align="center" prop="contactPhone" />
      <el-table-column label="留言" align="center" prop="message" />
      <el-table-column label="状态:0待确认,1已确认,2已完成,3已取消,4已拒绝" align="center" prop="status" />
      <el-table-column label="取消/拒绝原因" align="center" prop="cancelReason" />
      <el-table-column label="来源:0手动,1AI助手" align="center" prop="source" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:appointment:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:appointment:remove']">删除</el-button>
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

    <!-- 添加或修改看房预约对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="appointmentRef" :model="form" :rules="rules" label-width="100px">
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
            <el-form-item label="预约时间" prop="appointmentTime">
              <el-date-picker clearable
                v-model="form.appointmentTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择预约时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="联系人" prop="contactName">
              <el-input v-model="form.contactName" placeholder="请输入联系人" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="留言" prop="message">
              <el-input v-model="form.message" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="取消/拒绝原因" prop="cancelReason">
              <el-input v-model="form.cancelReason" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="来源:0手动,1AI助手" prop="source">
              <el-input v-model="form.source" placeholder="请输入来源:0手动,1AI助手" />
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

<script setup name="Appointment">
import { listAppointment, getAppointment, delAppointment, addAppointment, updateAppointment } from "@/api/system/appointment"

const { proxy } = getCurrentInstance()

const appointmentList = ref([])
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
    appointmentTime: undefined,
    contactName: undefined,
    contactPhone: undefined,
    message: undefined,
    status: undefined,
    cancelReason: undefined,
    source: undefined,
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
    appointmentTime: [
      { required: true, message: "预约时间不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询看房预约列表 */
function getList() {
  loading.value = true
  listAppointment(queryParams.value).then(response => {
    appointmentList.value = response.rows
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
    appointmentId: null,
    houseId: null,
    tenantId: null,
    ownerId: null,
    agentId: null,
    appointmentTime: null,
    contactName: null,
    contactPhone: null,
    message: null,
    status: null,
    cancelReason: null,
    source: null,
    createTime: null,
    updateTime: null
  }
  proxy.resetForm("appointmentRef")
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
  ids.value = selection.map(item => item.appointmentId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加看房预约"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _appointmentId = row.appointmentId || ids.value
  getAppointment(_appointmentId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改看房预约"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["appointmentRef"].validate(valid => {
    if (valid) {
      if (form.value.appointmentId != null) {
        updateAppointment(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addAppointment(form.value).then(() => {
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
  const _appointmentIds = row.appointmentId || ids.value
  proxy.$modal.confirm('是否确认删除看房预约编号为"' + _appointmentIds + '"的数据项？').then(function() {
    return delAppointment(_appointmentIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/appointment/export', {
    ...queryParams.value
  }, `appointment_${new Date().getTime()}.xlsx`)
}

getList()
</script>
