<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
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
      <el-form-item label="运营方式:0户主自主出租,1委托中介" prop="operationMode">
        <el-input
          v-model="queryParams.operationMode"
          placeholder="请输入运营方式:0户主自主出租,1委托中介"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="房源标题" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入房源标题"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="城市" prop="city">
        <el-input
          v-model="queryParams.city"
          placeholder="请输入城市"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="区域" prop="district">
        <el-input
          v-model="queryParams.district"
          placeholder="请输入区域"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="街道" prop="street">
        <el-input
          v-model="queryParams.street"
          placeholder="请输入街道"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="小区" prop="community">
        <el-input
          v-model="queryParams.community"
          placeholder="请输入小区"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="详细地址" prop="address">
        <el-input
          v-model="queryParams.address"
          placeholder="请输入详细地址"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="经度" prop="longitude">
        <el-input
          v-model="queryParams.longitude"
          placeholder="请输入经度"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="纬度" prop="latitude">
        <el-input
          v-model="queryParams.latitude"
          placeholder="请输入纬度"
          clearable
          @keyup.enter="handleQuery"
        />
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
      <el-form-item label="面积" prop="area">
        <el-input
          v-model="queryParams.area"
          placeholder="请输入面积"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="室" prop="roomCount">
        <el-input
          v-model="queryParams.roomCount"
          placeholder="请输入室"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="厅" prop="hallCount">
        <el-input
          v-model="queryParams.hallCount"
          placeholder="请输入厅"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="卫" prop="toiletCount">
        <el-input
          v-model="queryParams.toiletCount"
          placeholder="请输入卫"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="所在楼层" prop="floorNo">
        <el-input
          v-model="queryParams.floorNo"
          placeholder="请输入所在楼层"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="总楼层" prop="totalFloor">
        <el-input
          v-model="queryParams.totalFloor"
          placeholder="请输入总楼层"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="朝向" prop="orientation">
        <el-input
          v-model="queryParams.orientation"
          placeholder="请输入朝向"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="装修" prop="decoration">
        <el-input
          v-model="queryParams.decoration"
          placeholder="请输入装修"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="浏览次数" prop="viewCount">
        <el-input
          v-model="queryParams.viewCount"
          placeholder="请输入浏览次数"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="收藏次数" prop="favoriteCount">
        <el-input
          v-model="queryParams.favoriteCount"
          placeholder="请输入收藏次数"
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
          v-hasPermi="['system:house:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:house:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:house:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:house:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="houseList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="房源ID" align="center" prop="houseId" />
      <el-table-column label="户主用户ID" align="center" prop="ownerId" />
      <el-table-column label="中介用户ID，自主出租时为空" align="center" prop="agentId" />
      <el-table-column label="运营方式:0户主自主出租,1委托中介" align="center" prop="operationMode" />
      <el-table-column label="房源标题" align="center" prop="title" />
      <el-table-column label="城市" align="center" prop="city" />
      <el-table-column label="区域" align="center" prop="district" />
      <el-table-column label="街道" align="center" prop="street" />
      <el-table-column label="小区" align="center" prop="community" />
      <el-table-column label="详细地址" align="center" prop="address" />
      <el-table-column label="经度" align="center" prop="longitude" />
      <el-table-column label="纬度" align="center" prop="latitude" />
      <el-table-column label="月租金" align="center" prop="rentAmount" />
      <el-table-column label="押金" align="center" prop="depositAmount" />
      <el-table-column label="面积" align="center" prop="area" />
      <el-table-column label="室" align="center" prop="roomCount" />
      <el-table-column label="厅" align="center" prop="hallCount" />
      <el-table-column label="卫" align="center" prop="toiletCount" />
      <el-table-column label="所在楼层" align="center" prop="floorNo" />
      <el-table-column label="总楼层" align="center" prop="totalFloor" />
      <el-table-column label="朝向" align="center" prop="orientation" />
      <el-table-column label="出租方式:0整租,1合租" align="center" prop="rentType" />
      <el-table-column label="装修" align="center" prop="decoration" />
      <el-table-column label="配套设施JSON或逗号分隔" align="center" prop="facilities" />
      <el-table-column label="标签" align="center" prop="tags" />
      <el-table-column label="房源描述" align="center" prop="description" />
      <el-table-column label="状态:0草稿,1待审核,2已发布,3驳回,4已出租,5下架" align="center" prop="status" />
      <el-table-column label="审核状态:0未提交,1待审,2通过,3拒绝" align="center" prop="auditStatus" />
      <el-table-column label="审核意见" align="center" prop="auditReason" />
      <el-table-column label="浏览次数" align="center" prop="viewCount" />
      <el-table-column label="收藏次数" align="center" prop="favoriteCount" />
      <el-table-column label="向量索引状态:0未索引,1已索引,2失败" align="center" prop="aiIndexStatus" />
      <el-table-column label="${comment}" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:house:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:house:remove']">删除</el-button>
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

    <!-- 添加或修改租赁房源对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="houseRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
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
            <el-form-item label="运营方式:0户主自主出租,1委托中介" prop="operationMode">
              <el-input v-model="form.operationMode" placeholder="请输入运营方式:0户主自主出租,1委托中介" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="房源标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入房源标题" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="城市" prop="city">
              <el-input v-model="form.city" placeholder="请输入城市" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="区域" prop="district">
              <el-input v-model="form.district" placeholder="请输入区域" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="街道" prop="street">
              <el-input v-model="form.street" placeholder="请输入街道" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="小区" prop="community">
              <el-input v-model="form.community" placeholder="请输入小区" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="详细地址" prop="address">
              <el-input v-model="form.address" placeholder="请输入详细地址" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="经度" prop="longitude">
              <el-input v-model="form.longitude" placeholder="请输入经度" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="纬度" prop="latitude">
              <el-input v-model="form.latitude" placeholder="请输入纬度" />
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
            <el-form-item label="面积" prop="area">
              <el-input v-model="form.area" placeholder="请输入面积" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="室" prop="roomCount">
              <el-input v-model="form.roomCount" placeholder="请输入室" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="厅" prop="hallCount">
              <el-input v-model="form.hallCount" placeholder="请输入厅" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="卫" prop="toiletCount">
              <el-input v-model="form.toiletCount" placeholder="请输入卫" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="所在楼层" prop="floorNo">
              <el-input v-model="form.floorNo" placeholder="请输入所在楼层" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="总楼层" prop="totalFloor">
              <el-input v-model="form.totalFloor" placeholder="请输入总楼层" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="朝向" prop="orientation">
              <el-input v-model="form.orientation" placeholder="请输入朝向" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="装修" prop="decoration">
              <el-input v-model="form.decoration" placeholder="请输入装修" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="配套设施JSON或逗号分隔" prop="facilities">
              <el-input v-model="form.facilities" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="标签" prop="tags">
              <el-input v-model="form.tags" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="房源描述" prop="description">
              <el-input v-model="form.description" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="审核意见" prop="auditReason">
              <el-input v-model="form.auditReason" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="浏览次数" prop="viewCount">
              <el-input v-model="form.viewCount" placeholder="请输入浏览次数" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="收藏次数" prop="favoriteCount">
              <el-input v-model="form.favoriteCount" placeholder="请输入收藏次数" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="删除标志" prop="delFlag">
              <el-input v-model="form.delFlag" placeholder="请输入删除标志" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="${comment}" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
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

<script setup name="House">
import { listHouse, getHouse, delHouse, addHouse, updateHouse } from "@/api/system/house"

const { proxy } = getCurrentInstance()

const houseList = ref([])
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
    ownerId: undefined,
    agentId: undefined,
    operationMode: undefined,
    title: undefined,
    city: undefined,
    district: undefined,
    street: undefined,
    community: undefined,
    address: undefined,
    longitude: undefined,
    latitude: undefined,
    rentAmount: undefined,
    depositAmount: undefined,
    area: undefined,
    roomCount: undefined,
    hallCount: undefined,
    toiletCount: undefined,
    floorNo: undefined,
    totalFloor: undefined,
    orientation: undefined,
    rentType: undefined,
    decoration: undefined,
    facilities: undefined,
    tags: undefined,
    description: undefined,
    status: undefined,
    auditStatus: undefined,
    auditReason: undefined,
    viewCount: undefined,
    favoriteCount: undefined,
    aiIndexStatus: undefined,
  },
  rules: {
    ownerId: [
      { required: true, message: "户主用户ID不能为空", trigger: "blur" }
    ],
    title: [
      { required: true, message: "房源标题不能为空", trigger: "blur" }
    ],
    city: [
      { required: true, message: "城市不能为空", trigger: "blur" }
    ],
    district: [
      { required: true, message: "区域不能为空", trigger: "blur" }
    ],
    rentAmount: [
      { required: true, message: "月租金不能为空", trigger: "blur" }
    ],
    status: [
      { required: true, message: "状态:0草稿,1待审核,2已发布,3驳回,4已出租,5下架不能为空", trigger: "change" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询租赁房源列表 */
function getList() {
  loading.value = true
  listHouse(queryParams.value).then(response => {
    houseList.value = response.rows
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
    houseId: null,
    ownerId: null,
    agentId: null,
    operationMode: null,
    title: null,
    city: null,
    district: null,
    street: null,
    community: null,
    address: null,
    longitude: null,
    latitude: null,
    rentAmount: null,
    depositAmount: null,
    area: null,
    roomCount: null,
    hallCount: null,
    toiletCount: null,
    floorNo: null,
    totalFloor: null,
    orientation: null,
    rentType: null,
    decoration: null,
    facilities: null,
    tags: null,
    description: null,
    status: null,
    auditStatus: null,
    auditReason: null,
    viewCount: null,
    favoriteCount: null,
    aiIndexStatus: null,
    delFlag: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("houseRef")
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
  ids.value = selection.map(item => item.houseId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加租赁房源"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _houseId = row.houseId || ids.value
  getHouse(_houseId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改租赁房源"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["houseRef"].validate(valid => {
    if (valid) {
      if (form.value.houseId != null) {
        updateHouse(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addHouse(form.value).then(() => {
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
  const _houseIds = row.houseId || ids.value
  proxy.$modal.confirm('是否确认删除租赁房源编号为"' + _houseIds + '"的数据项？').then(function() {
    return delHouse(_houseIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/house/export', {
    ...queryParams.value
  }, `house_${new Date().getTime()}.xlsx`)
}

getList()
</script>
