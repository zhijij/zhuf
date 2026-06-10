<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="租户用户ID" prop="userId">
        <el-input
          v-model="queryParams.userId"
          placeholder="请输入租户用户ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="意向城市" prop="city">
        <el-input
          v-model="queryParams.city"
          placeholder="请输入意向城市"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="最低预算" prop="minRent">
        <el-input
          v-model="queryParams.minRent"
          placeholder="请输入最低预算"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="最高预算" prop="maxRent">
        <el-input
          v-model="queryParams.maxRent"
          placeholder="请输入最高预算"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="通勤目标" prop="commuteTarget">
        <el-input
          v-model="queryParams.commuteTarget"
          placeholder="请输入通勤目标"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="期望通勤分钟" prop="commuteMinutes">
        <el-input
          v-model="queryParams.commuteMinutes"
          placeholder="请输入期望通勤分钟"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="来源:0用户填写,1AI提取" prop="source">
        <el-input
          v-model="queryParams.source"
          placeholder="请输入来源:0用户填写,1AI提取"
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
          v-hasPermi="['system:preference:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:preference:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:preference:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:preference:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="preferenceList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="偏好ID" align="center" prop="preferenceId" />
      <el-table-column label="租户用户ID" align="center" prop="userId" />
      <el-table-column label="意向城市" align="center" prop="city" />
      <el-table-column label="意向区域" align="center" prop="districts" />
      <el-table-column label="最低预算" align="center" prop="minRent" />
      <el-table-column label="最高预算" align="center" prop="maxRent" />
      <el-table-column label="户型偏好" align="center" prop="roomType" />
      <el-table-column label="通勤目标" align="center" prop="commuteTarget" />
      <el-table-column label="期望通勤分钟" align="center" prop="commuteMinutes" />
      <el-table-column label="必须标签" align="center" prop="requiredTags" />
      <el-table-column label="偏好标签" align="center" prop="preferredTags" />
      <el-table-column label="避雷标签" align="center" prop="avoidTags" />
      <el-table-column label="来源:0用户填写,1AI提取" align="center" prop="source" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:preference:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:preference:remove']">删除</el-button>
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

    <!-- 添加或修改租户租房偏好对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="preferenceRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="租户用户ID" prop="userId">
              <el-input v-model="form.userId" placeholder="请输入租户用户ID" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="意向城市" prop="city">
              <el-input v-model="form.city" placeholder="请输入意向城市" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="意向区域" prop="districts">
              <el-input v-model="form.districts" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="最低预算" prop="minRent">
              <el-input v-model="form.minRent" placeholder="请输入最低预算" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="最高预算" prop="maxRent">
              <el-input v-model="form.maxRent" placeholder="请输入最高预算" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="通勤目标" prop="commuteTarget">
              <el-input v-model="form.commuteTarget" placeholder="请输入通勤目标" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="期望通勤分钟" prop="commuteMinutes">
              <el-input v-model="form.commuteMinutes" placeholder="请输入期望通勤分钟" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="必须标签" prop="requiredTags">
              <el-input v-model="form.requiredTags" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="偏好标签" prop="preferredTags">
              <el-input v-model="form.preferredTags" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="避雷标签" prop="avoidTags">
              <el-input v-model="form.avoidTags" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="来源:0用户填写,1AI提取" prop="source">
              <el-input v-model="form.source" placeholder="请输入来源:0用户填写,1AI提取" />
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

<script setup name="Preference">
import { listPreference, getPreference, delPreference, addPreference, updatePreference } from "@/api/system/preference"

const { proxy } = getCurrentInstance()

const preferenceList = ref([])
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
    city: undefined,
    districts: undefined,
    minRent: undefined,
    maxRent: undefined,
    roomType: undefined,
    commuteTarget: undefined,
    commuteMinutes: undefined,
    requiredTags: undefined,
    preferredTags: undefined,
    avoidTags: undefined,
    source: undefined,
  },
  rules: {
    userId: [
      { required: true, message: "租户用户ID不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询租户租房偏好列表 */
function getList() {
  loading.value = true
  listPreference(queryParams.value).then(response => {
    preferenceList.value = response.rows
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
    preferenceId: null,
    userId: null,
    city: null,
    districts: null,
    minRent: null,
    maxRent: null,
    roomType: null,
    commuteTarget: null,
    commuteMinutes: null,
    requiredTags: null,
    preferredTags: null,
    avoidTags: null,
    source: null,
    createTime: null,
    updateTime: null
  }
  proxy.resetForm("preferenceRef")
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
  ids.value = selection.map(item => item.preferenceId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加租户租房偏好"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _preferenceId = row.preferenceId || ids.value
  getPreference(_preferenceId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改租户租房偏好"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["preferenceRef"].validate(valid => {
    if (valid) {
      if (form.value.preferenceId != null) {
        updatePreference(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addPreference(form.value).then(() => {
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
  const _preferenceIds = row.preferenceId || ids.value
  proxy.$modal.confirm('是否确认删除租户租房偏好编号为"' + _preferenceIds + '"的数据项？').then(function() {
    return delPreference(_preferenceIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/preference/export', {
    ...queryParams.value
  }, `preference_${new Date().getTime()}.xlsx`)
}

getList()
</script>
