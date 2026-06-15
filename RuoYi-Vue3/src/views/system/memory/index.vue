<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="用户ID" prop="userId">
        <el-input
          v-model="queryParams.userId"
          placeholder="请输入用户ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="角色" prop="role">
        <el-input
          v-model="queryParams.role"
          placeholder="请输入角色"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="重要度" prop="importance">
        <el-input
          v-model="queryParams.importance"
          placeholder="请输入重要度"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="过期时间" prop="expiresAt">
        <el-date-picker clearable
          v-model="queryParams.expiresAt"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择过期时间">
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
          v-hasRole="['admin']"
          v-hasPermi="['system:memory:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasRole="['admin']"
          v-hasPermi="['system:memory:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasRole="['admin']"
          v-hasPermi="['system:memory:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasRole="['admin']"
          v-hasPermi="['system:memory:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="memoryList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="记忆ID" align="center" prop="memoryId" />
      <el-table-column label="用户ID" align="center" prop="userId" />
      <el-table-column label="角色" align="center" prop="role" />
      <el-table-column label="类型:preference/fact/summary" align="center" prop="memoryType" />
      <el-table-column label="记忆内容" align="center" prop="content" />
      <el-table-column label="重要度" align="center" prop="importance" />
      <el-table-column label="向量状态:0未索引,1已索引,2失败" align="center" prop="vectorStatus" />
      <el-table-column label="过期时间" align="center" prop="expiresAt" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.expiresAt, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasRole="['admin']" v-hasPermi="['system:memory:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasRole="['admin']" v-hasPermi="['system:memory:remove']">删除</el-button>
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

    <!-- 添加或修改AI用户记忆对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="memoryRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="用户ID" prop="userId">
              <el-input v-model="form.userId" placeholder="请输入用户ID" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="角色" prop="role">
              <el-input v-model="form.role" placeholder="请输入角色" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="记忆内容">
              <editor v-model="form.content" :min-height="192"/>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="重要度" prop="importance">
              <el-input v-model="form.importance" placeholder="请输入重要度" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="过期时间" prop="expiresAt">
              <el-date-picker clearable
                v-model="form.expiresAt"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择过期时间">
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

<script setup name="Memory">
import { listMemory, getMemory, delMemory, addMemory, updateMemory } from "@/api/system/memory"

const { proxy } = getCurrentInstance()

const memoryList = ref([])
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
    userId: undefined,
    role: undefined,
    memoryType: undefined,
    content: undefined,
    importance: undefined,
    vectorStatus: undefined,
    expiresAt: undefined,
  },
  rules: {
    userId: [
      { required: true, message: "用户ID不能为空", trigger: "blur" }
    ],
    role: [
      { required: true, message: "角色不能为空", trigger: "blur" }
    ],
    memoryType: [
      { required: true, message: "类型:preference/fact/summary不能为空", trigger: "change" }
    ],
    content: [
      { required: true, message: "记忆内容不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询AI用户记忆列表 */
function getList() {
  loading.value = true
  listMemory(queryParams.value).then(response => {
    memoryList.value = response.rows
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
    memoryId: null,
    userId: null,
    role: null,
    memoryType: null,
    content: null,
    importance: null,
    vectorStatus: null,
    expiresAt: null,
    createTime: null,
    updateTime: null
  }
  proxy.resetForm("memoryRef")
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
  ids.value = selection.map(item => item.memoryId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加AI用户记忆"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _memoryId = row.memoryId || ids.value
  getMemory(_memoryId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改AI用户记忆"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["memoryRef"].validate(valid => {
    if (valid) {
      if (form.value.memoryId != null) {
        updateMemory(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addMemory(form.value).then(() => {
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
  const _memoryIds = row.memoryId || ids.value
  proxy.$modal.confirm('是否确认删除AI用户记忆编号为"' + _memoryIds + '"的数据项？').then(function() {
    return delMemory(_memoryIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/memory/export', {
    ...queryParams.value
  }, `memory_${new Date().getTime()}.xlsx`)
}

getList()
</script>
