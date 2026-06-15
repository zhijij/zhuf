<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="文档ID" prop="docId">
        <el-input
          v-model="queryParams.docId"
          placeholder="请输入文档ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="分片序号" prop="chunkNo">
        <el-input
          v-model="queryParams.chunkNo"
          placeholder="请输入分片序号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="Token数量" prop="tokenCount">
        <el-input
          v-model="queryParams.tokenCount"
          placeholder="请输入Token数量"
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
          v-hasRole="['admin']"
          v-hasPermi="['system:chunk:add']"
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
          v-hasPermi="['system:chunk:edit']"
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
          v-hasPermi="['system:chunk:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasRole="['admin']"
          v-hasPermi="['system:chunk:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="chunkList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="分片ID" align="center" prop="chunkId" />
      <el-table-column label="文档ID" align="center" prop="docId" />
      <el-table-column label="分片序号" align="center" prop="chunkNo" />
      <el-table-column label="分片内容" align="center" prop="content" />
      <el-table-column label="Token数量" align="center" prop="tokenCount" />
      <el-table-column label="向量状态" align="center" prop="vectorStatus" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasRole="['admin']" v-hasPermi="['system:chunk:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasRole="['admin']" v-hasPermi="['system:chunk:remove']">删除</el-button>
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

    <!-- 添加或修改AI知识库分片对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="chunkRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="文档ID" prop="docId">
              <el-input v-model="form.docId" placeholder="请输入文档ID" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="分片序号" prop="chunkNo">
              <el-input v-model="form.chunkNo" placeholder="请输入分片序号" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="分片内容">
              <editor v-model="form.content" :min-height="192"/>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="Token数量" prop="tokenCount">
              <el-input v-model="form.tokenCount" placeholder="请输入Token数量" />
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

<script setup name="Chunk">
import { listChunk, getChunk, delChunk, addChunk, updateChunk } from "@/api/system/chunk"

const { proxy } = getCurrentInstance()

const chunkList = ref([])
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
    docId: undefined,
    chunkNo: undefined,
    content: undefined,
    tokenCount: undefined,
    vectorStatus: undefined,
  },
  rules: {
    docId: [
      { required: true, message: "文档ID不能为空", trigger: "blur" }
    ],
    chunkNo: [
      { required: true, message: "分片序号不能为空", trigger: "blur" }
    ],
    content: [
      { required: true, message: "分片内容不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询AI知识库分片列表 */
function getList() {
  loading.value = true
  listChunk(queryParams.value).then(response => {
    chunkList.value = response.rows
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
    chunkId: null,
    docId: null,
    chunkNo: null,
    content: null,
    tokenCount: null,
    vectorStatus: null,
    createTime: null
  }
  proxy.resetForm("chunkRef")
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
  ids.value = selection.map(item => item.chunkId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加AI知识库分片"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _chunkId = row.chunkId || ids.value
  getChunk(_chunkId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改AI知识库分片"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["chunkRef"].validate(valid => {
    if (valid) {
      if (form.value.chunkId != null) {
        updateChunk(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addChunk(form.value).then(() => {
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
  const _chunkIds = row.chunkId || ids.value
  proxy.$modal.confirm('是否确认删除AI知识库分片编号为"' + _chunkIds + '"的数据项？').then(function() {
    return delChunk(_chunkIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/chunk/export', {
    ...queryParams.value
  }, `chunk_${new Date().getTime()}.xlsx`)
}

getList()
</script>
