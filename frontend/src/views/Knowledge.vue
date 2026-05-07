<template>
  <div class="knowledge-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">知识库管理</h1>
      <div class="title-deco"></div>
    </div>

    <!-- 知识库标签页 -->
    <div class="tabs-wrapper">
      <div
        v-for="tab in knowledgeTabs"
        :key="tab.key"
        class="tab-item"
        :class="{ active: activeTab === tab.key }"
        @click="handleTabChange(tab.key)"
      >
        <el-icon :size="16">
          <Guide v-if="tab.key === 'scenic'" />
          <Pointer v-else-if="tab.key === 'attraction'" />
          <ForkSpoon v-else-if="tab.key === 'food'" />
          <House v-else-if="tab.key === 'hotel'" />
          <Present v-else-if="tab.key === 'product'" />
          <Ticket v-else-if="tab.key === 'ticket'" />
          <ChatLineSquare v-else-if="tab.key === 'fixed_qa'" />
          <Headset v-else-if="tab.key === 'customer_service'" />
          <Guide v-else />
        </el-icon>
        <span>{{ tab.label }}</span>
      </div>
    </div>

    <!-- 操作栏 -->
    <div class="action-bar">
      <div class="action-left">
        <button class="btn-seal" @click="handleAdd">
          <el-icon :size="16"><Plus /></el-icon>
          <span>添加数据</span>
        </button>
        <button class="btn-outline" @click="handleImport">
          <el-icon :size="16"><Upload /></el-icon>
          <span>批量导入</span>
        </button>
        <button class="btn-outline" @click="handleExport">
          <el-icon :size="16"><Download /></el-icon>
          <span>导出数据</span>
        </button>
      </div>
      <div class="action-right">
        <div class="search-wrapper">
          <el-icon class="search-icon"><Search /></el-icon>
          <input
            v-model="searchKeyword"
            class="search-input"
            placeholder="搜索关键词..."
            @input="handleSearch"
          />
        </div>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-wrapper">
      <div class="table-container">
        <table class="elegant-table">
          <thead>
            <tr>
              <th v-for="column in currentColumns" :key="column.prop" :style="{ minWidth: column.minWidth || 'auto', width: column.width || 'auto' }">
                {{ column.label }}
              </th>
              <th style="width: 160px">操作</th>
            </tr>
          </thead>
          <tbody v-if="filteredData.length > 0">
            <tr v-for="(row, idx) in pagedData" :key="row.id || idx">
              <td v-for="column in currentColumns" :key="column.prop">
                <template v-if="column.prop === 'image'">
                  <div class="table-image-cell">
                    <img :src="row.image" class="table-image" />
                  </div>
                </template>
                <template v-else-if="column.prop === 'tags'">
                  <div class="tag-group">
                    <span v-for="(tag, i) in (Array.isArray(row.tags) ? row.tags : [])" :key="i" class="tag-seal">{{ tag }}</span>
                  </div>
                </template>
                <template v-else>
                  <span class="cell-text">{{ row[column.prop] || '-' }}</span>
                </template>
              </td>
              <td>
                <div class="action-group">
                  <button class="btn-action edit" @click="handleEdit(row)">
                    <el-icon :size="14"><Edit /></el-icon>
                    <span>编辑</span>
                  </button>
                  <button class="btn-action delete" @click="handleDelete(row)">
                    <el-icon :size="14"><Delete /></el-icon>
                    <span>删除</span>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
          <tbody v-else>
            <tr>
              <td :colspan="currentColumns.length + 1" class="empty-cell">
                <div class="empty-state">
                  <el-icon :size="48"><FolderDelete /></el-icon>
                  <p>暂无数据</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div class="pagination-bar">
        <div class="pagination-info">共 {{ total }} 条</div>
        <div class="pagination-controls">
          <button class="page-btn" :disabled="currentPage <= 1" @click="handleCurrentChange(currentPage - 1)">
            <el-icon><ArrowLeft /></el-icon>
          </button>
          <template v-for="page in pageNumbers" :key="page">
            <button
              v-if="page !== '...'"
              class="page-btn"
              :class="{ active: page === currentPage }"
              @click="handleCurrentChange(page)"
            >
              {{ page }}
            </button>
            <span v-else class="page-ellipsis">...</span>
          </template>
          <button class="page-btn" :disabled="currentPage >= Math.ceil(total / pageSize)" @click="handleCurrentChange(currentPage + 1)">
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>
        <div class="page-size-control">
          <select v-model="pageSize" @change="handleSizeChange(pageSize)" class="page-size-select">
            <option :value="10">10条/页</option>
            <option :value="20">20条/页</option>
            <option :value="50">50条/页</option>
            <option :value="100">100条/页</option>
          </select>
        </div>
      </div>
    </div>

    <!-- 添加/编辑对话框 -->
    <div class="dialog-overlay" v-if="dialogVisible" @click.self="dialogVisible = false">
      <div class="dialog-panel">
        <div class="dialog-header">
          <h3>{{ dialogTitle }}</h3>
          <div class="dialog-deco"></div>
          <button class="dialog-close" @click="dialogVisible = false">
            <el-icon><Close /></el-icon>
          </button>
        </div>
        <div class="dialog-body">
          <div class="form-grid">
            <div
              v-for="field in currentFormFields"
              :key="field.prop"
              class="form-item"
            >
              <label class="form-label">
                {{ field.label }}
                <span v-if="field.required" class="required">*</span>
              </label>

              <!-- image -->
              <div v-if="field.type === 'image'" class="image-upload-area">
                <div v-if="formData[field.prop]" class="uploaded-preview">
                  <img :src="formData[field.prop]" class="uploaded-image" />
                  <button class="remove-image" @click="formData[field.prop] = ''">
                    <el-icon><Close /></el-icon>
                  </button>
                </div>
                <div v-else class="upload-placeholder" @click="triggerUpload(field.prop)">
                  <el-icon :size="28"><Plus /></el-icon>
                  <span>点击上传图片</span>
                </div>
                <input
                  type="file"
                  :ref="el => { if (el) uploadRefs[field.prop] = el }"
                  style="display:none"
                  accept="image/*"
                  @change="handleImageUpload($event, field.prop)"
                />
              </div>

              <!-- tags -->
              <div v-else-if="field.type === 'tags'" class="tag-selector">
                <div class="tag-list">
                  <span
                    v-for="option in field.options"
                    :key="option"
                    class="tag-option"
                    :class="{ selected: (formData[field.prop] || []).includes(option) }"
                    @click="toggleTag(field.prop, option)"
                  >
                    {{ option }}
                  </span>
                </div>
                <input
                  v-model="customTagInput[field.prop]"
                  class="tag-input"
                  placeholder="输入自定义标签后回车"
                  @keyup.enter="addCustomTag(field.prop)"
                />
              </div>

              <!-- select -->
              <div v-else-if="field.type === 'select'">
                <select v-model="formData[field.prop]" class="form-select">
                  <option value="" disabled>请选择</option>
                  <option v-for="opt in field.options" :key="opt" :value="opt">{{ opt }}</option>
                </select>
              </div>

              <!-- textarea -->
              <div v-else-if="field.type === 'textarea'">
                <textarea
                  v-model="formData[field.prop]"
                  class="form-textarea"
                  :placeholder="`请输入${field.label}`"
                  rows="3"
                ></textarea>
              </div>

              <!-- default input -->
              <div v-else>
                <input
                  v-model="formData[field.prop]"
                  class="form-input"
                  :placeholder="`请输入${field.label}`"
                />
              </div>
            </div>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="btn-outline" @click="dialogVisible = false">取消</button>
          <button class="btn-seal" @click="handleSubmit" :disabled="submitting">
            {{ submitting ? '提交中...' : '确定' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 导入对话框 -->
    <div class="dialog-overlay" v-if="importDialogVisible" @click.self="importDialogVisible = false">
      <div class="dialog-panel" style="max-width: 480px;">
        <div class="dialog-header">
          <h3>批量导入</h3>
          <div class="dialog-deco"></div>
          <button class="dialog-close" @click="importDialogVisible = false">
            <el-icon><Close /></el-icon>
          </button>
        </div>
        <div class="dialog-body">
          <div class="import-zone">
            <el-icon :size="40" style="color: var(--gold);"><UploadFilled /></el-icon>
            <p class="import-text">将文件拖到此处，或点击上传</p>
            <p class="import-hint">支持 .xlsx, .xls, .csv 格式文件</p>
            <button class="btn-outline" @click="triggerImport">选择文件</button>
            <input type="file" ref="importFileInput" style="display:none" accept=".xlsx,.xls,.csv" @change="handleImportFile" />
          </div>
        </div>
        <div class="dialog-footer">
          <button class="btn-outline" @click="importDialogVisible = false">取消</button>
          <button class="btn-seal" @click="handleImportConfirm">确定</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Edit, Delete, Search, Upload, Download,
  UploadFilled, Close, ArrowLeft, ArrowRight,
  Guide, Pointer, ForkSpoon, House, Present, Sunny, FolderDelete, Ticket,
  ChatLineSquare, Headset
} from '@element-plus/icons-vue'
import { getKnowledgeList, addKnowledge, updateKnowledge, deleteKnowledge } from '@/api'

const activeTab = ref('scenic')
const loading = ref(false)
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('添加数据')
const submitting = ref(false)
const importDialogVisible = ref(false)
const formData = ref({})
const customTagInput = ref({})
const uploadRefs = reactive({})
const importFileInput = ref(null)

// 知识库标签页
const knowledgeTabs = [
  { key: 'scenic', label: '景区知识库', icon: '🏯' },
  { key: 'attraction', label: '景点知识库', icon: '🎯' },
  { key: 'food', label: '美食知识库', icon: '🍜' },
  { key: 'hotel', label: '酒店知识库', icon: '🏨' },
  { key: 'product', label: '特产知识库', icon: '🎁' },
  { key: 'ticket', label: '门票知识库', icon: '🎟️' },
  { key: 'fixed_qa', label: '固定回答', icon: '💬' },
  { key: 'customer_service', label: '客服回答', icon: '📞' }
]

// 表格列配置
const tableColumns = {
  scenic: [
    { prop: 'image', label: '景区主图', width: 90 },
    { prop: 'name', label: '景区名称', minWidth: 150 },
    { prop: 'type', label: '景区类型', width: 110 },
    { prop: 'level', label: '景区级别', width: 90 },
    { prop: 'address', label: '景区地址', minWidth: 200 }
  ],
  attraction: [
    { prop: 'image', label: '景点图片', width: 90 },
    { prop: 'name', label: '景点名称', minWidth: 150 },
    { prop: 'tags', label: '特色标签', width: 150 },
    { prop: 'relatedScenic', label: '关联景区', width: 120 },
    { prop: 'address', label: '详细地址', minWidth: 200 }
  ],
  food: [
    { prop: 'image', label: '店铺主图', width: 90 },
    { prop: 'name', label: '店铺名称', minWidth: 150 },
    { prop: 'type', label: '店铺类型', width: 110 },
    { prop: 'relatedScenic', label: '归属景区', width: 120 },
    { prop: 'address', label: '详细地址', minWidth: 200 }
  ],
  hotel: [
    { prop: 'image', label: '酒店主图', width: 90 },
    { prop: 'name', label: '酒店名称', minWidth: 150 },
    { prop: 'level', label: '酒店等级', width: 90 },
    { prop: 'price', label: '价格区间', width: 110 },
    { prop: 'address', label: '详细地址', minWidth: 200 }
  ],
  product: [
    { prop: 'image', label: '产品图片', width: 90 },
    { prop: 'name', label: '产品名称', minWidth: 150 },
    { prop: 'category', label: '产品类别', width: 110 },
    { prop: 'price', label: '价格', width: 100 },
    { prop: 'description', label: '产品描述', minWidth: 200 }
  ],
  ticket: [
    { prop: 'image', label: '门票图片', width: 90 },
    { prop: 'name', label: '门票名称', minWidth: 150 },
    { prop: 'price', label: '价格', width: 100 },
    { prop: 'location', label: '所属地点', width: 120 },
    { prop: 'description', label: '门票描述', minWidth: 200 }
  ],
  fixed_qa: [
    { prop: 'question', label: '问题', minWidth: 250 },
    { prop: 'answer', label: '回答', minWidth: 300 }
  ],
  customer_service: [
    { prop: 'keywords', label: '关键词', width: 200 },
    { prop: 'response', label: '回答内容', minWidth: 300 }
  ]
}

// 表单字段配置
const formFields = {
  scenic: [
    { prop: 'image', label: '景区主图', type: 'image', required: true },
    { prop: 'name', label: '景区名称', type: 'input', required: true },
    { prop: 'type', label: '景区类型', type: 'select', options: ['自然景观', '人文景观', '历史遗迹', '主题公园'], required: true },
    { prop: 'level', label: '景区级别', type: 'select', options: ['5A', '4A', '3A', '2A', '1A'], required: true },
    { prop: 'address', label: '景区地址', type: 'input', required: true },
    { prop: 'longitude', label: '经度', type: 'input' },
    { prop: 'latitude', label: '纬度', type: 'input' },
    { prop: 'description', label: '景区介绍', type: 'textarea' }
  ],
  attraction: [
    { prop: 'image', label: '景点图片', type: 'image', required: true },
    { prop: 'name', label: '景点名称', type: 'input', required: true },
    { prop: 'tags', label: '特色标签', type: 'tags', options: ['必游', '网红打卡', '历史', '文化', '自然'], required: true },
    { prop: 'relatedScenic', label: '关联景区', type: 'input', required: true },
    { prop: 'address', label: '详细地址', type: 'input', required: true },
    { prop: 'longitude', label: '经度', type: 'input' },
    { prop: 'latitude', label: '纬度', type: 'input' },
    { prop: 'description', label: '景点介绍', type: 'textarea' }
  ],
  food: [
    { prop: 'image', label: '店铺主图', type: 'image', required: true },
    { prop: 'name', label: '店铺名称', type: 'input', required: true },
    { prop: 'type', label: '店铺类型', type: 'select', options: ['传统美食', '特色小吃', '餐厅', '小吃街'], required: true },
    { prop: 'relatedScenic', label: '归属景区', type: 'input' },
    { prop: 'address', label: '详细地址', type: 'input', required: true },
    { prop: 'longitude', label: '经度', type: 'input' },
    { prop: 'latitude', label: '纬度', type: 'input' },
    { prop: 'tags', label: '店铺标签', type: 'tags', options: ['老字号', '网红', '必吃', '特色'] }
  ],
  hotel: [
    { prop: 'image', label: '酒店主图', type: 'image', required: true },
    { prop: 'name', label: '酒店名称', type: 'input', required: true },
    { prop: 'level', label: '酒店等级', type: 'select', options: ['五星级', '四星级', '三星级', '二星级', '经济型'], required: true },
    { prop: 'price', label: '价格区间', type: 'input', required: true },
    { prop: 'address', label: '详细地址', type: 'input', required: true },
    { prop: 'phone', label: '联系电话', type: 'input' },
    { prop: 'facilities', label: '设施服务', type: 'textarea' }
  ],
  product: [
    { prop: 'image', label: '产品图片', type: 'image', required: true },
    { prop: 'name', label: '产品名称', type: 'input', required: true },
    { prop: 'category', label: '产品类别', type: 'select', options: ['工艺品', '纪念品', '文创产品', '传统手工艺'], required: true },
    { prop: 'price', label: '价格', type: 'input', required: true },
    { prop: 'description', label: '产品描述', type: 'textarea', required: true },
    { prop: 'origin', label: '产地', type: 'input' }
  ],
  ticket: [
    { prop: 'image', label: '门票图片', type: 'image', required: true },
    { prop: 'name', label: '门票名称', type: 'input', required: true },
    { prop: 'price', label: '价格', type: 'input', required: true },
    { prop: 'location', label: '所属地点', type: 'input' },
    { prop: 'description', label: '门票描述', type: 'textarea' },
    { prop: 'tags', label: '关键词标签', type: 'tags', options: ['热门', '特惠', '学生票', '套票'] }
  ],
  fixed_qa: [
    { prop: 'question', label: '问题', type: 'input', required: true },
    { prop: 'answer', label: '回答', type: 'textarea', required: true }
  ],
  customer_service: [
    { prop: 'keywords', label: '关键词', type: 'input' },
    { prop: 'response', label: '回答内容', type: 'textarea', required: true }
  ]
}

// 当前数据
const tableData = ref([])

// 计算属性
const currentColumns = computed(() => tableColumns[activeTab.value] || [])
const currentFormFields = computed(() => formFields[activeTab.value] || [])
const filteredData = computed(() => {
  if (!searchKeyword.value) return tableData.value
  const keyword = searchKeyword.value.toLowerCase()
  return tableData.value.filter(item =>
    Object.values(item).some(value =>
      String(value).toLowerCase().includes(keyword)
    )
  )
})

const pagedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredData.value.slice(start, start + pageSize.value)
})

const pageNumbers = computed(() => {
  const totalPages = Math.ceil(filteredData.value.length / pageSize.value) || 1
  const pages = []
  const delta = 2
  const left = Math.max(1, currentPage.value - delta)
  const right = Math.min(totalPages, currentPage.value + delta)

  if (left > 1) {
    pages.push(1)
    if (left > 2) pages.push('...')
  }
  for (let i = left; i <= right; i++) {
    pages.push(i)
  }
  if (right < totalPages) {
    if (right < totalPages - 1) pages.push('...')
    pages.push(totalPages)
  }
  return pages.length ? pages : [1]
})

// 切换标签页
const handleTabChange = (tab) => {
  activeTab.value = tab
  currentPage.value = 1
  loadData()
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await getKnowledgeList(activeTab.value)
    if (res.code === 200) {
      tableData.value = res.data || []
      total.value = tableData.value.length
    } else {
      ElMessage.error(res.message || '加载数据失败')
    }
  } catch (error) {
    ElMessage.error('加载数据失败: ' + (error.message || '网络错误'))
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
}

// 添加数据
const handleAdd = () => {
  dialogTitle.value = '添加数据'
  formData.value = {}
  customTagInput.value = {}
  dialogVisible.value = true
}

// 编辑数据
const handleEdit = (row) => {
  dialogTitle.value = '编辑数据'
  formData.value = { ...row }
  customTagInput.value = {}
  dialogVisible.value = true
}

// 删除数据
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除"${row.name || row.id}"这条数据吗？`,
      '提示',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await deleteKnowledge(activeTab.value, row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadData()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

// 提交表单
const handleSubmit = async () => {
  submitting.value = true
  try {
    const data = { ...formData.value }
    const isEdit = !!data.id

    // tags数组转逗号分隔字符串
    for (const key of Object.keys(data)) {
      if (Array.isArray(data[key])) {
        data[key] = data[key].join(',')
      }
    }

    let res
    if (isEdit) {
      res = await updateKnowledge(activeTab.value, data.id, data)
    } else {
      delete data.id
      res = await addKnowledge(activeTab.value, data)
    }

    if (res.code === 200) {
      ElMessage.success(isEdit ? '更新成功' : '添加成功')
      dialogVisible.value = false
      loadData()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    ElMessage.error('操作失败: ' + (error.message || ''))
  } finally {
    submitting.value = false
  }
}

// 批量导入
const handleImport = () => {
  importDialogVisible.value = true
}

const triggerImport = () => {
  importFileInput.value?.click()
}

const handleImportFile = (e) => {
  const file = e.target.files[0]
  if (file) {
    ElMessage.success(`已选择文件: ${file.name}`)
  }
}

const handleImportConfirm = () => {
  ElMessage.success('导入成功')
  importDialogVisible.value = false
  loadData()
}

// 导出数据
const handleExport = () => {
  ElMessage.success('导出成功')
}

// 图片上传
const triggerUpload = (prop) => {
  uploadRefs[prop]?.click()
}

const handleImageUpload = (event, prop) => {
  const file = event.target.files[0]
  if (!file) return

  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB!')
    return
  }

  // 模拟上传成功
  const reader = new FileReader()
  reader.onload = (e) => {
    formData.value[prop] = e.target.result
  }
  reader.readAsDataURL(file)
}

const handleImageSuccess = (response) => {
  formData.value.image = response.url
}

const beforeImageUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isImage) { ElMessage.error('只能上传图片文件!'); return false }
  if (!isLt2M) { ElMessage.error('图片大小不能超过 2MB!'); return false }
  return true
}

const beforeImportUpload = (file) => {
  const isExcel = file.type === 'application/vnd.ms-excel' ||
    file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' ||
    file.type === 'text/csv'
  if (!isExcel) { ElMessage.error('只能上传 Excel 或 CSV 文件!'); return false }
  return true
}

const handleImportSuccess = () => {
  ElMessage.success('导入成功')
  loadData()
}

// 标签选择
const toggleTag = (prop, tag) => {
  if (!formData.value[prop]) {
    formData.value[prop] = []
  }
  const tags = formData.value[prop]
  const idx = tags.indexOf(tag)
  if (idx > -1) {
    tags.splice(idx, 1)
  } else {
    tags.push(tag)
  }
}

const addCustomTag = (prop) => {
  const val = customTagInput.value[prop]
  if (!val || !val.trim()) return
  if (!formData.value[prop]) {
    formData.value[prop] = []
  }
  formData.value[prop].push(val.trim())
  customTagInput.value[prop] = ''
}

// 分页
const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
}

const handleCurrentChange = (val) => {
  currentPage.value = val
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.knowledge-page {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

// 页面标题
.page-header {
  margin-bottom: 24px;

  .page-title {
    font-family: var(--font-display);
    font-size: 32px;
    color: var(--palace-red);
    letter-spacing: 4px;
    margin-bottom: 8px;
  }

  .title-deco {
    height: 3px;
    width: 180px;
    background: linear-gradient(90deg, var(--gold), transparent);
    border-radius: 2px;
  }
}

// 标签页
.tabs-wrapper {
  display: flex;
  gap: 0;
  margin-bottom: 20px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 8px;
  padding: 4px;
  border: 1px solid var(--gold-light);
  overflow-x: auto;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-family: var(--font-body);
  font-size: 14px;
  color: var(--ink-light);
  transition: all 0.3s ease;
  white-space: nowrap;

  .el-icon {
    color: var(--gold-dark);
    transition: color 0.3s ease;
  }

  &:hover {
    color: var(--palace-red);
    background: rgba(200, 164, 92, 0.08);

    .el-icon { color: var(--gold); }
  }

  &.active {
    background: var(--palace-red);
    color: #fff;
    box-shadow: 0 2px 8px rgba(122, 26, 46, 0.2);

    .el-icon { color: var(--gold-light); }
  }
}

// 操作栏
.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 16px;
}

.action-left {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.btn-seal {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 20px;
  background: linear-gradient(135deg, var(--palace-red-light), var(--palace-red));
  color: #fff;
  border: none;
  border-radius: 6px;
  font-family: var(--font-body);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(122, 26, 46, 0.2);

  .el-icon { color: var(--gold-light); }

  &:hover {
    background: var(--palace-red-dark);
    box-shadow: 0 4px 16px rgba(122, 26, 46, 0.3);
    transform: translateY(-1px);
  }

  &:active {
    transform: translateY(0);
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.btn-outline {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  background: transparent;
  color: var(--palace-red);
  border: 1px solid var(--gold);
  border-radius: 6px;
  font-family: var(--font-body);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;

  .el-icon { color: var(--gold-dark); }

  &:hover {
    background: rgba(200, 164, 92, 0.08);
    border-color: var(--gold-dark);
    box-shadow: 0 2px 8px rgba(200, 164, 92, 0.15);
  }
}

.action-right {
  flex-shrink: 0;
}

.search-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid var(--gold-light);
  transition: all 0.3s ease;

  &:focus-within {
    border-color: var(--gold);
    box-shadow: 0 0 0 3px rgba(200, 164, 92, 0.1);
  }

  .search-icon {
    color: var(--gold-dark);
    font-size: 16px;
  }

  .search-input {
    border: none;
    outline: none;
    font-family: var(--font-body);
    font-size: 14px;
    color: var(--ink);
    width: 200px;
    background: transparent;

    &::placeholder {
      color: var(--ink-light);
      opacity: 0.6;
    }
  }
}

// 表格
.table-wrapper {
  background: #fff;
  border-radius: 12px;
  border: 1px solid var(--gold-light);
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.table-container {
  overflow-x: auto;
}

.elegant-table {
  width: 100%;
  border-collapse: collapse;
  font-family: var(--font-body);

  thead {
    th {
      background: var(--palace-red);
      color: #fff;
      font-weight: 600;
      font-size: 14px;
      padding: 14px 16px;
      text-align: left;
      white-space: nowrap;
    }
  }

  tbody {
    tr {
      transition: background 0.2s ease;

      &:nth-child(odd) {
        background: var(--ivory);
      }

      &:nth-child(even) {
        background: #fff;
      }

      &:hover {
        background: linear-gradient(135deg, rgba(200, 164, 92, 0.08), rgba(200, 164, 92, 0.03));

        td:first-child {
          box-shadow: inset 3px 0 0 var(--gold);
        }
      }
    }

    td {
      padding: 12px 16px;
      font-size: 13px;
      color: var(--ink);
      border-bottom: 1px solid rgba(200, 164, 92, 0.08);
      vertical-align: middle;
    }
  }
}

.table-image-cell {
  display: flex;
  align-items: center;
  justify-content: center;
}

.table-image {
  width: 56px;
  height: 56px;
  border-radius: 6px;
  object-fit: cover;
  border: 1px solid var(--gold-light);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
}

.cell-text {
  display: inline-block;
  max-width: 250px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-group {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.tag-seal {
  display: inline-block;
  padding: 2px 8px;
  border: 1px solid var(--gold);
  color: var(--palace-red);
  border-radius: 3px;
  font-family: var(--font-body);
  font-size: 11px;
  background: #fff;
  white-space: nowrap;
}

.action-group {
  display: flex;
  gap: 6px;
}

.btn-action {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  border-radius: 4px;
  font-family: var(--font-body);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;

  &.edit {
    background: transparent;
    color: var(--palace-red);
    border-color: var(--gold-light);

    &:hover {
      background: var(--palace-red);
      color: #fff;
      border-color: var(--palace-red);
    }
  }

  &.delete {
    background: transparent;
    color: #C05A5A;
    border-color: rgba(192, 90, 90, 0.3);

    &:hover {
      background: #C05A5A;
      color: #fff;
      border-color: #C05A5A;
    }
  }
}

.empty-cell {
  text-align: center;
  padding: 48px 16px !important;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--ink-light);

  .el-icon { opacity: 0.3; }
}

// 分页
.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-top: 1px solid var(--gold-light);
  flex-wrap: wrap;
  gap: 12px;
}

.pagination-info {
  font-family: var(--font-body);
  font-size: 13px;
  color: var(--ink-light);
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 4px;
}

.page-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid var(--gold-light);
  border-radius: 6px;
  background: #fff;
  color: var(--ink);
  font-family: var(--font-body);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;

  .el-icon { font-size: 12px; }

  &:hover:not(:disabled):not(.active) {
    border-color: var(--gold);
    color: var(--palace-red);
  }

  &.active {
    background: var(--palace-red);
    color: #fff;
    border-color: var(--palace-red);
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }
}

.page-ellipsis {
  padding: 0 4px;
  color: var(--ink-light);
}

.page-size-control {
  flex-shrink: 0;
}

.page-size-select {
  padding: 5px 10px;
  border: 1px solid var(--gold-light);
  border-radius: 6px;
  font-family: var(--font-body);
  font-size: 13px;
  color: var(--ink);
  background: #fff;
  cursor: pointer;
  outline: none;

  &:focus {
    border-color: var(--gold);
  }
}

// ============ 对话框 ============
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease-out;
}

.dialog-panel {
  background: #fff;
  border-radius: 16px;
  width: 90%;
  max-width: 640px;
  max-height: 85vh;
  overflow-y: auto;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  animation: slideUp 0.3s ease-out;
}

.dialog-header {
  position: relative;
  padding: 20px 24px 16px;
  border-bottom: 1px solid rgba(200, 164, 92, 0.15);

  h3 {
    font-family: var(--font-body);
    font-size: 18px;
    color: var(--ink);
    font-weight: 600;
  }

  .dialog-deco {
    height: 2px;
    width: 60px;
    background: var(--gold);
    border-radius: 1px;
    margin-top: 8px;
  }

  .dialog-close {
    position: absolute;
    top: 20px;
    right: 20px;
    width: 28px;
    height: 28px;
    border-radius: 6px;
    border: none;
    background: transparent;
    color: var(--ink-light);
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s;

    &:hover {
      background: rgba(0, 0, 0, 0.05);
      color: var(--ink);
    }
  }
}

.dialog-body {
  padding: 20px 24px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.form-item {
  &.full-width {
    grid-column: 1 / -1;
  }
}

.form-label {
  display: block;
  font-family: var(--font-body);
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
  margin-bottom: 6px;

  .required {
    color: #C05A5A;
    margin-left: 2px;
  }
}

.form-input, .form-select, .form-textarea {
  width: 100%;
  padding: 9px 12px;
  border: 1px solid rgba(0, 0, 0, 0.12);
  border-radius: 6px;
  font-family: var(--font-body);
  font-size: 14px;
  color: var(--ink);
  background: #fff;
  outline: none;
  transition: all 0.2s ease;

  &:focus {
    border-color: var(--gold);
    box-shadow: 0 0 0 3px rgba(200, 164, 92, 0.1);
  }

  &::placeholder {
    color: var(--ink-light);
    opacity: 0.5;
  }
}

.form-textarea {
  resize: vertical;
  min-height: 70px;
}

.form-select {
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%238a8a8a' stroke-width='2'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 10px center;
  padding-right: 30px;
}

// 图片上传
.image-upload-area {
  .upload-placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    height: 120px;
    border: 2px dashed var(--gold-light);
    border-radius: 8px;
    cursor: pointer;
    color: var(--ink-light);
    font-size: 13px;
    font-family: var(--font-body);
    transition: all 0.3s ease;

    &:hover {
      border-color: var(--gold);
      background: rgba(200, 164, 92, 0.04);
    }
  }

  .uploaded-preview {
    position: relative;
    display: inline-block;

    .uploaded-image {
      width: 120px;
      height: 120px;
      object-fit: cover;
      border-radius: 8px;
      border: 1px solid var(--gold-light);
    }

    .remove-image {
      position: absolute;
      top: -8px;
      right: -8px;
      width: 24px;
      height: 24px;
      border-radius: 50%;
      background: var(--palace-red);
      color: #fff;
      border: none;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);

      .el-icon { font-size: 14px; }

      &:hover {
        background: var(--palace-red-dark);
      }
    }
  }
}

// 标签选择器
.tag-selector {
  .tag-list {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-bottom: 8px;
  }

  .tag-option {
    padding: 4px 12px;
    border: 1px solid var(--gold-light);
    border-radius: 4px;
    font-size: 12px;
    font-family: var(--font-body);
    color: var(--ink-light);
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover {
      border-color: var(--gold);
    }

    &.selected {
      background: var(--palace-red);
      color: #fff;
      border-color: var(--palace-red);
    }
  }

  .tag-input {
    width: 100%;
    padding: 6px 10px;
    border: none;
    border-bottom: 1px solid var(--gold-light);
    font-family: var(--font-body);
    font-size: 12px;
    color: var(--ink);
    outline: none;
    background: transparent;

    &:focus {
      border-bottom-color: var(--gold);
    }

    &::placeholder {
      color: var(--ink-light);
      opacity: 0.5;
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 24px;
  border-top: 1px solid rgba(200, 164, 92, 0.1);
}

// 导入区域
.import-zone {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 40px 20px;
  border: 2px dashed var(--gold-light);
  border-radius: 12px;
  background: rgba(245, 240, 233, 0.3);
  text-align: center;

  .import-text {
    font-family: var(--font-body);
    font-size: 14px;
    color: var(--ink);
  }

  .import-hint {
    font-size: 12px;
    color: var(--ink-light);
  }
}

// 响应式
@media (max-width: 768px) {
  .knowledge-page {
    padding: 16px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .action-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .action-right {
    .search-input {
      width: 100%;
    }
  }

  .pagination-bar {
    flex-direction: column;
    align-items: center;
  }
}
</style>
