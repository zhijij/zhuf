<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="真实姓名" prop="realName">
        <el-input
          v-model="queryParams.realName"
          placeholder="请输入真实姓名"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="证件号，可加密存储" prop="idCardNo">
        <el-input
          v-model="queryParams.idCardNo"
          placeholder="请输入证件号，可加密存储"
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
      <el-form-item label="收款账户，可加密存储" prop="bankAccount">
        <el-input
          v-model="queryParams.bankAccount"
          placeholder="请输入收款账户，可加密存储"
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
          v-hasPermi="['system:profile:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:profile:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:profile:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:profile:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="profileList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="户主用户ID，对应sys_user.user_id" align="center" prop="ownerId" />
      <el-table-column label="真实姓名" align="center" prop="realName" />
      <el-table-column label="证件号，可加密存储" align="center" prop="idCardNo" />
      <el-table-column label="联系电话" align="center" prop="contactPhone" />
      <el-table-column label="收款账户，可加密存储" align="center" prop="bankAccount" />
      <el-table-column label="认证状态:0未认证,1待审核,2已认证,3拒绝" align="center" prop="verifyStatus" />
      <el-table-column label="审核意见" align="center" prop="verifyReason" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:profile:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:profile:remove']">删除</el-button>
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

    <!-- 添加或修改户主资料对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="profileRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="form.realName" placeholder="请输入真实姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="证件号，可加密存储" prop="idCardNo">
              <el-input v-model="form.idCardNo" placeholder="请输入证件号，可加密存储" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="收款账户，可加密存储" prop="bankAccount">
              <el-input v-model="form.bankAccount" placeholder="请输入收款账户，可加密存储" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="审核意见" prop="verifyReason">
              <el-input v-model="form.verifyReason" type="textarea" placeholder="请输入内容" />
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

<script setup name="Profile">
import { listProfile, getProfile, delProfile, addProfile, updateProfile } from "@/api/system/profile"

const { proxy } = getCurrentInstance()

const profileList = ref([])
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
    realName: undefined,
    idCardNo: undefined,
    contactPhone: undefined,
    bankAccount: undefined,
    verifyStatus: undefined,
    verifyReason: undefined,
  },
  rules: {
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询户主资料列表 */
function getList() {
  loading.value = true
  listProfile(queryParams.value).then(response => {
    profileList.value = response.rows
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
    ownerId: null,
    realName: null,
    idCardNo: null,
    contactPhone: null,
    bankAccount: null,
    verifyStatus: null,
    verifyReason: null,
    createTime: null,
    updateTime: null
  }
  proxy.resetForm("profileRef")
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
  ids.value = selection.map(item => item.ownerId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加户主资料"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _ownerId = row.ownerId || ids.value
  getProfile(_ownerId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改户主资料"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["profileRef"].validate(valid => {
    if (valid) {
      if (form.value.ownerId != null) {
        updateProfile(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addProfile(form.value).then(() => {
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
  const _ownerIds = row.ownerId || ids.value
  proxy.$modal.confirm('是否确认删除户主资料编号为"' + _ownerIds + '"的数据项？').then(function() {
    return delProfile(_ownerIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/profile/export', {
    ...queryParams.value
  }, `profile_${new Date().getTime()}.xlsx`)
}

getList()
</script>
