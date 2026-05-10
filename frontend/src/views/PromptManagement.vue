<template>
  <div class="prompt-management">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
            <rect x="2" y="4" width="24" height="20" rx="3" fill="var(--palace-red)" />
            <rect x="5" y="8" width="18" height="2" rx="1" fill="var(--gold-light)" opacity="0.6" />
            <rect x="5" y="13" width="14" height="2" rx="1" fill="var(--gold-light)" opacity="0.4" />
            <rect x="5" y="18" width="10" height="2" rx="1" fill="var(--gold-light)" opacity="0.4" />
          </svg>
        </div>
        <div>
          <h1 class="page-title">提示词管理</h1>
          <p class="page-desc">管理系统中的 AI 提示词，修改后实时生效</p>
        </div>
      </div>
      <div class="header-actions">
        <button class="btn-create" @click="handleCreate">
          <el-icon :size="16"><Plus /></el-icon>
          <span>新建提示词</span>
        </button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-scroll">
        <div class="scroll-inner"></div>
      </div>
      <p>正在加载提示词列表...</p>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-state">
      <el-icon :size="48"><WarningFilled /></el-icon>
      <p>{{ error }}</p>
      <button class="retry-btn" @click="fetchPrompts">重新加载</button>
    </div>

    <!-- 主体内容 -->
    <div v-else class="main-layout">
      <!-- 左侧：提示词列表（卷宗索引） -->
      <aside class="prompt-index">
        <div class="index-header">
          <span class="index-title">敕令卷宗</span>
          <span class="index-count">{{ prompts.length }} 条</span>
        </div>
        <div class="index-search">
          <el-icon class="search-icon"><Search /></el-icon>
          <input
            v-model="searchQuery"
            class="search-input"
            placeholder="搜索提示词..."
          />
        </div>
        <div class="prompt-list">
          <div
            v-for="prompt in filteredPrompts"
            :key="prompt.id"
            class="prompt-card"
            :class="{ active: selectedPrompt?.id === prompt.id }"
            @click="selectPrompt(prompt)"
          >
            <div class="card-header">
              <span class="card-type">{{ formatType(prompt.type) }}</span>
              <span
                class="status-dot"
                :class="{ online: prompt.enabled, offline: !prompt.enabled }"
                :title="prompt.enabled ? '已启用' : '已禁用'"
              ></span>
            </div>
            <div class="card-name">{{ prompt.name }}</div>
            <div class="card-meta">
              <span class="card-version">v{{ prompt.version }}</span>
              <span class="card-date">{{ formatDate(prompt.updatedAt) }}</span>
            </div>
          </div>
          <div v-if="filteredPrompts.length === 0" class="empty-list">
            <el-icon :size="32"><Document /></el-icon>
            <p>暂未找到匹配的提示词</p>
          </div>
        </div>
      </aside>

      <!-- 右侧：编辑面板（谕旨书案） -->
      <main class="editor-panel" v-if="selectedPrompt">
        <div class="editor-scroll">
          <!-- 谕旨抬头 -->
          <div class="edict-header">
            <div class="edict-seal">{{ formatTypeShort(selectedPrompt.type) }}</div>
            <div class="edict-title-group">
              <h2 class="edict-title">{{ selectedPrompt.name }}</h2>
              <span class="edict-subtitle">{{ selectedPrompt.type }}</span>
            </div>
            <div class="edict-actions">
              <button
                class="btn-status"
                :class="{ enabled: selectedPrompt.enabled }"
                @click="toggleEnabled"
              >
                <el-icon :size="14"><VideoPause v-if="selectedPrompt.enabled" /><VideoPlay v-else /></el-icon>
                <span>{{ selectedPrompt.enabled ? '已启用' : '已禁用' }}</span>
              </button>
            </div>
          </div>

          <!-- 提示词内容编辑区（奏折/谕旨正文） -->
          <div class="edict-body">
            <div class="body-label">
              <span class="label-icon">敕</span>
              <span>谕旨正文</span>
              <span class="label-hint">支持 {rag_context} 和 {user_message} 作为占位符</span>
            </div>
            <div class="scroll-container">
              <div class="scroll-top-deco"></div>
              <textarea
                v-model="editContent"
                class="edict-textarea"
                placeholder="在此输入提示词内容..."
                spellcheck="false"
              ></textarea>
              <div class="scroll-bottom-deco"></div>
            </div>
            <div class="content-stats">
              <span>字数：{{ editContent?.length || 0 }}</span>
              <span>版本：v{{ selectedPrompt.version }}</span>
              <span>最后修改：{{ formatDate(selectedPrompt.updatedAt) }}</span>
            </div>
          </div>

          <!-- 备注信息 -->
          <div class="edict-remark">
            <div class="remark-label">
              <span class="label-icon">注</span>
              <span>备注说明</span>
            </div>
            <input
              v-model="editRemark"
              class="remark-input"
              placeholder="添加备注说明..."
            />
          </div>

          <!-- 操作按钮 -->
          <div class="edict-footer">
            <div class="footer-left">
              <button class="btn-save" @click="handleSave" :disabled="saving">
                <el-icon :size="16" v-if="!saving"><Check /></el-icon>
                <el-icon :size="16" v-else class="loading-icon"><Loading /></el-icon>
                <span>{{ saving ? '保存中...' : '保存生效' }}</span>
              </button>
              <span v-if="saveSuccess" class="save-success">
                <el-icon :size="14"><CircleCheck /></el-icon>
                已保存，配置已实时生效
              </span>
            </div>
            <button class="btn-delete" @click="handleDelete">
              <el-icon :size="14"><Delete /></el-icon>
              <span>删除</span>
            </button>
          </div>
        </div>
      </main>

      <!-- 未选择提示词时显示占位 -->
      <main v-else class="editor-panel editor-empty">
        <div class="empty-state">
          <div class="empty-icon">
            <svg width="64" height="64" viewBox="0 0 64 64" fill="none">
              <rect x="8" y="12" width="48" height="40" rx="4" fill="var(--gold-light)" opacity="0.3" />
              <rect x="14" y="20" width="36" height="3" rx="1.5" fill="var(--gold-light)" opacity="0.5" />
              <rect x="14" y="28" width="28" height="3" rx="1.5" fill="var(--gold-light)" opacity="0.3" />
              <rect x="14" y="36" width="20" height="3" rx="1.5" fill="var(--gold-light)" opacity="0.3" />
            </svg>
          </div>
          <p class="empty-title">请从左侧选择一条提示词进行编辑</p>
          <p class="empty-desc">或点击「新建提示词」创建新的敕令</p>
        </div>
      </main>
    </div>

    <!-- 新建/编辑提示词对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新建提示词' : '编辑提示词'"
      width="500px"
      :close-on-click-modal="false"
      class="prompt-dialog"
    >
      <el-form :model="dialogForm" label-position="top" class="dialog-form">
        <el-form-item label="提示词类型" required>
          <el-input v-model="dialogForm.type" placeholder="如：INTENT_RECOGNITION, REPLY_FOOD" />
        </el-form-item>
        <el-form-item label="提示词名称" required>
          <el-input v-model="dialogForm.name" placeholder="如：意图识别提示词、美食导购提示词" />
        </el-form-item>
        <el-form-item label="备注说明">
          <el-input v-model="dialogForm.remark" placeholder="可选，说明此提示词的用途" />
        </el-form-item>
      </el-form>
      <template #footer>
        <button class="dialog-btn cancel" @click="dialogVisible = false">取消</button>
        <button class="dialog-btn confirm" @click="confirmDialog">确认{{ dialogMode === 'create' ? '创建' : '保存' }}</button>
      </template>
    </el-dialog>

    <!-- 删除确认对话框 -->
    <el-dialog
      v-model="deleteDialogVisible"
      title="删除提示词"
      width="420px"
      class="prompt-dialog delete-dialog"
    >
      <div class="delete-warning">
        <el-icon :size="40" color="var(--palace-red)"><WarningFilled /></el-icon>
        <p>确定要删除提示词「<strong>{{ selectedPrompt?.name }}</strong>」吗？</p>
        <p class="delete-hint">删除后系统将使用内置默认提示词作为备用。此操作不可撤销。</p>
      </div>
      <template #footer>
        <button class="dialog-btn cancel" @click="deleteDialogVisible = false">取消</button>
        <button class="dialog-btn danger" @click="confirmDelete" :disabled="deleting">
          {{ deleting ? '删除中...' : '确认删除' }}
        </button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getPromptList, createPrompt, updatePrompt,
  updatePromptContent, deletePrompt, getPromptByType
} from '@/api'

// === 数据 ===
const prompts = ref([])
const loading = ref(true)
const error = ref('')
const searchQuery = ref('')

const selectedPrompt = ref(null)
const editContent = ref('')
const editRemark = ref('')
const saving = ref(false)
const saveSuccess = ref(false)

const dialogVisible = ref(false)
const dialogMode = ref('create')
const dialogForm = ref({ type: '', name: '', remark: '' })

const deleteDialogVisible = ref(false)
const deleting = ref(false)

// === 计算属性 ===
const filteredPrompts = computed(() => {
  if (!searchQuery.value) return prompts.value
  const q = searchQuery.value.toLowerCase()
  return prompts.value.filter(p =>
    p.type.toLowerCase().includes(q) ||
    p.name.toLowerCase().includes(q)
  )
})

// === 生命周期 ===
onMounted(() => {
  fetchPrompts()
})

// === 方法 ===
async function fetchPrompts() {
  loading.value = true
  error.value = ''
  try {
    const res = await getPromptList()
    prompts.value = res.data || []
  } catch (e) {
    error.value = e.message || '加载提示词列表失败'
  } finally {
    loading.value = false
  }
}

function selectPrompt(prompt) {
  saveSuccess.value = false
  selectedPrompt.value = prompt
  editContent.value = prompt.content || ''
  editRemark.value = prompt.remark || ''
}

async function toggleEnabled() {
  if (!selectedPrompt.value) return
  const prompt = selectedPrompt.value
  try {
    const res = await updatePrompt(prompt.id, { enabled: !prompt.enabled })
    Object.assign(prompt, res.data)
    ElMessage.success(prompt.enabled ? '提示词已启用' : '提示词已禁用')
    saveSuccess.value = true
  } catch (e) {
    ElMessage.error('操作失败：' + (e.message || '未知错误'))
  }
}

async function handleSave() {
  if (!selectedPrompt.value) return
  saving.value = true
  saveSuccess.value = false
  try {
    const data = { content: editContent.value, remark: editRemark.value }
    const res = await updatePrompt(selectedPrompt.value.id, data)
    Object.assign(selectedPrompt.value, res.data)
    // 更新列表中的数据
    const idx = prompts.value.findIndex(p => p.id === selectedPrompt.value.id)
    if (idx >= 0) prompts.value[idx] = { ...selectedPrompt.value }
    saveSuccess.value = true
    ElMessage.success('提示词已保存，配置已实时生效')
  } catch (e) {
    ElMessage.error('保存失败：' + (e.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

function handleCreate() {
  dialogMode.value = 'create'
  dialogForm.value = { type: '', name: '', remark: '' }
  dialogVisible.value = true
}

function confirmDialog() {
  const { type, name } = dialogForm.value
  if (!type || !name) {
    ElMessage.warning('请填写提示词类型和名称')
    return
  }
  if (dialogMode.value === 'create') {
    createPrompt({ type, name, remark: dialogForm.value.remark, content: '', enabled: true })
      .then(res => {
        prompts.value.push(res.data)
        dialogVisible.value = false
        selectPrompt(res.data)
        ElMessage.success('提示词创建成功')
      })
      .catch(e => ElMessage.error('创建失败：' + (e.message || '未知错误')))
  }
}

function handleDelete() {
  if (!selectedPrompt.value) return
  deleteDialogVisible.value = true
}

async function confirmDelete() {
  if (!selectedPrompt.value) return
  deleting.value = true
  try {
    await deletePrompt(selectedPrompt.value.id)
    const deletedId = selectedPrompt.value.id
    prompts.value = prompts.value.filter(p => p.id !== deletedId)
    selectedPrompt.value = filteredPrompts.value[0] || null
    if (selectedPrompt.value) {
      editContent.value = selectedPrompt.value.content || ''
      editRemark.value = selectedPrompt.value.remark || ''
    } else {
      editContent.value = ''
      editRemark.value = ''
    }
    deleteDialogVisible.value = false
    ElMessage.success('提示词已删除')
  } catch (e) {
    ElMessage.error('删除失败：' + (e.message || '未知错误'))
  } finally {
    deleting.value = false
  }
}

// === 工具方法 ===
function formatType(type) {
  const map = {
    'INTENT_RECOGNITION': '意图识别',
    'REPLY_ATTRACTION': '景点讲解',
    'REPLY_FOOD': '美食导购',
    'REPLY_HOTEL': '酒店导购',
    'REPLY_PRODUCT': '特产文创',
    'REPLY_TICKET': '门票购买',
    'REPLY_CUSTOMER_SERVICE': '客服问答',
    'REPLY_WEATHER': '天气查询',
  }
  return map[type] || type
}

function formatTypeShort(type) {
  const map = {
    'INTENT_RECOGNITION': '意',
    'REPLY_ATTRACTION': '景',
    'REPLY_FOOD': '食',
    'REPLY_HOTEL': '宿',
    'REPLY_PRODUCT': '物',
    'REPLY_TICKET': '票',
    'REPLY_CUSTOMER_SERVICE': '客',
    'REPLY_WEATHER': '天',
  }
  return map[type] || '敕'
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hour = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hour}:${min}`
}
</script>

<style scoped lang="scss">
// ========================================
// 提示词管理 - 谕旨敕令风格
// ========================================

.prompt-management {
  padding: var(--space-lg);
  height: 100%;
  display: flex;
  flex-direction: column;
  animation: fadeIn 0.5s ease-out;
}

// === 页面头部 ===
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: var(--space-lg);
  flex-shrink: 0;

  .header-left {
    display: flex;
    align-items: center;
    gap: 14px;
  }

  .header-icon {
    width: 48px;
    height: 48px;
    border-radius: var(--radius-md);
    background: linear-gradient(135deg, var(--palace-red), var(--palace-red-dark));
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: var(--shadow-gold);
  }

  .page-title {
    font-family: var(--font-display);
    font-size: 26px;
    color: var(--palace-red);
    letter-spacing: 3px;
    line-height: 1.2;
  }

  .page-desc {
    font-size: 13px;
    color: var(--ink-light);
    margin-top: 2px;
    letter-spacing: 0.5px;
  }

  .btn-create {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 10px 20px;
    background: linear-gradient(135deg, var(--palace-red-light), var(--palace-red));
    color: #fff;
    border: none;
    border-radius: var(--radius-sm);
    font-family: var(--font-body);
    font-size: 14px;
    cursor: pointer;
    transition: all 0.3s ease;
    box-shadow: 0 2px 8px rgba(122, 26, 46, 0.25);

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 16px rgba(122, 26, 46, 0.35);
    }
  }
}

// === 加载状态 ===
.loading-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: var(--ink-light);

  .loading-scroll {
    width: 40px;
    height: 40px;
    border: 3px solid var(--gold-light);
    border-top-color: var(--gold);
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }

  p { font-size: 14px; }
}

.error-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--ink-light);

  p { font-size: 14px; }

  .retry-btn {
    padding: 8px 24px;
    background: var(--palace-red);
    color: #fff;
    border: none;
    border-radius: var(--radius-sm);
    cursor: pointer;
    font-family: var(--font-body);
    transition: all 0.2s;

    &:hover { background: var(--palace-red-dark); }
  }
}

// === 主体双栏布局 ===
.main-layout {
  flex: 1;
  display: flex;
  gap: var(--space-lg);
  min-height: 0;
  overflow: hidden;
}

// === 左侧：提示词索引（卷宗） ===
.prompt-index {
  width: 280px;
  min-width: 280px;
  background: #fff;
  border-radius: var(--radius-lg);
  border: 1px solid var(--gold-light);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

.index-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(200, 164, 92, 0.15);
  background: linear-gradient(135deg, rgba(122, 26, 46, 0.03), rgba(200, 164, 92, 0.05));

  .index-title {
    font-family: var(--font-display);
    font-size: 16px;
    color: var(--palace-red);
    letter-spacing: 2px;
  }

  .index-count {
    font-size: 12px;
    color: var(--ink-light);
    background: var(--ivory);
    padding: 2px 10px;
    border-radius: 10px;
  }
}

.index-search {
  position: relative;
  padding: 12px 16px;

  .search-icon {
    position: absolute;
    left: 26px;
    top: 50%;
    transform: translateY(-50%);
    color: var(--ink-light);
    font-size: 14px;
  }

  .search-input {
    width: 100%;
    padding: 8px 12px 8px 34px;
    border: 1px solid rgba(200, 164, 92, 0.2);
    border-radius: var(--radius-sm);
    background: var(--ivory);
    font-family: var(--font-body);
    font-size: 13px;
    outline: none;
    transition: all 0.2s;

    &:focus {
      border-color: var(--gold);
      background: #fff;
      box-shadow: 0 0 0 3px rgba(200, 164, 92, 0.1);
    }

    &::placeholder { color: #bbb; }
  }
}

.prompt-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 12px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.prompt-card {
  padding: 12px 14px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.25s ease;
  border: 1px solid transparent;
  position: relative;

  &:hover {
    background: rgba(200, 164, 92, 0.08);
    border-color: rgba(200, 164, 92, 0.15);
  }

  &.active {
    background: linear-gradient(135deg, rgba(122, 26, 46, 0.06), rgba(200, 164, 92, 0.08));
    border-color: var(--gold);
    box-shadow: 0 0 0 1px rgba(200, 164, 92, 0.2);

    &::before {
      content: '';
      position: absolute;
      left: -12px;
      top: 50%;
      transform: translateY(-50%);
      width: 3px;
      height: 24px;
      background: var(--gold);
      border-radius: 0 2px 2px 0;
    }
  }

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 4px;
  }

  .card-type {
    font-size: 13px;
    font-weight: 600;
    color: var(--palace-red);
    letter-spacing: 0.5px;
  }

  .status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    flex-shrink: 0;

    &.online {
      background: #52c41a;
      box-shadow: 0 0 4px rgba(82, 196, 26, 0.5);
    }

    &.offline {
      background: #d9d9d9;
    }
  }

  .card-name {
    font-size: 12px;
    color: var(--ink-light);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    margin-bottom: 6px;
  }

  .card-meta {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .card-version {
    font-size: 11px;
    color: var(--gold-dark);
    background: rgba(200, 164, 92, 0.1);
    padding: 1px 6px;
    border-radius: 3px;
  }

  .card-date {
    font-size: 11px;
    color: #aaa;
  }
}

.empty-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--ink-light);
  padding: 40px 20px;

  p { font-size: 13px; }
}

// === 右侧：编辑面板（谕旨书案） ===
.editor-panel {
  flex: 1;
  background: #fff;
  border-radius: var(--radius-lg);
  border: 1px solid var(--gold-light);
  overflow-y: auto;
  box-shadow: var(--shadow-sm);

  &.editor-empty {
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

.editor-scroll {
  padding: var(--space-lg);
  max-width: 900px;
}

// 空状态
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: var(--ink-light);

  .empty-icon { opacity: 0.5; }

  .empty-title {
    font-size: 16px;
    color: var(--ink);
    font-family: var(--font-display);
    letter-spacing: 1px;
  }

  .empty-desc { font-size: 13px; }
}

// 谕旨抬头
.edict-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-bottom: 16px;
  border-bottom: 2px solid;
  border-image: linear-gradient(90deg, transparent, var(--gold), transparent) 1;
  margin-bottom: 20px;
}

.edict-seal {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--palace-red), var(--palace-red-dark));
  color: var(--gold);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-display);
  font-size: 18px;
  box-shadow: 0 2px 8px rgba(122, 26, 46, 0.2);
  flex-shrink: 0;
}

.edict-title-group {
  flex: 1;
}

.edict-title {
  font-family: var(--font-display);
  font-size: 20px;
  color: var(--palace-red);
  letter-spacing: 2px;
}

.edict-subtitle {
  font-size: 12px;
  color: var(--gold-dark);
  background: rgba(200, 164, 92, 0.1);
  padding: 1px 8px;
  border-radius: 3px;
  margin-top: 2px;
  display: inline-block;
}

.edict-actions {
  flex-shrink: 0;
}

.btn-status {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border: 1px solid #d9d9d9;
  border-radius: var(--radius-sm);
  background: #fff;
  color: var(--ink-light);
  cursor: pointer;
  font-family: var(--font-body);
  font-size: 13px;
  transition: all 0.2s;

  &:hover {
    border-color: var(--gold);
  }

  &.enabled {
    border-color: #52c41a;
    color: #52c41a;
    background: rgba(82, 196, 26, 0.05);

    &:hover {
      background: rgba(82, 196, 26, 0.1);
    }
  }
}

// 谕旨正文
.edict-body {
  margin-bottom: var(--space-md);
}

.body-label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;

  .label-icon {
    width: 22px;
    height: 22px;
    border-radius: 4px;
    background: var(--palace-red);
    color: var(--gold);
    display: flex;
    align-items: center;
    justify-content: center;
    font-family: var(--font-display);
    font-size: 12px;
  }

  span { font-size: 14px; font-weight: 600; color: var(--ink); }

  .label-hint {
    font-size: 11px;
    font-weight: 400;
    color: var(--gold-dark);
    margin-left: auto;
  }
}

.scroll-container {
  position: relative;
  border-radius: var(--radius-md);
  border: 1px solid var(--gold-light);
  background: linear-gradient(180deg,
    rgba(245, 240, 232, 0.3) 0%,
    rgba(245, 240, 232, 0.6) 50%,
    rgba(245, 240, 232, 0.3) 100%
  );
  overflow: hidden;
}

.scroll-top-deco {
  height: 4px;
  background: linear-gradient(90deg, transparent, var(--gold), transparent);
  opacity: 0.4;
}

.scroll-bottom-deco {
  height: 4px;
  background: linear-gradient(90deg, transparent, var(--gold), transparent);
  opacity: 0.4;
}

.edict-textarea {
  width: 100%;
  min-height: 320px;
  padding: 20px;
  border: none;
  background: transparent;
  font-family: 'Courier New', 'Noto Serif SC', monospace;
  font-size: 14px;
  line-height: 1.8;
  color: var(--ink);
  resize: vertical;
  outline: none;

  &::placeholder {
    color: #ccc;
    font-family: var(--font-body);
  }

  &:focus {
    background: rgba(255, 255, 255, 0.5);
  }
}

.content-stats {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-top: 8px;
  font-size: 12px;
  color: var(--ink-light);

  span {
    display: flex;
    align-items: center;
    gap: 4px;
  }
}

// 备注
.edict-remark {
  margin-bottom: var(--space-lg);
}

.remark-label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;

  .label-icon {
    width: 22px;
    height: 22px;
    border-radius: 4px;
    background: var(--gold);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-family: var(--font-display);
    font-size: 12px;
  }

  span { font-size: 14px; font-weight: 500; color: var(--ink); }
}

.remark-input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid var(--gold-light);
  border-radius: var(--radius-sm);
  background: var(--ivory);
  font-family: var(--font-body);
  font-size: 13px;
  outline: none;
  transition: all 0.2s;

  &:focus {
    border-color: var(--gold);
    background: #fff;
    box-shadow: 0 0 0 3px rgba(200, 164, 92, 0.1);
  }

  &::placeholder { color: #bbb; }
}

// 操作按钮区
.edict-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: var(--space-md);
  border-top: 1px solid rgba(200, 164, 92, 0.12);
}

.footer-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.btn-save {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 28px;
  background: linear-gradient(135deg, var(--palace-red-light), var(--palace-red));
  color: #fff;
  border: none;
  border-radius: var(--radius-sm);
  font-family: var(--font-body);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(122, 26, 46, 0.25);

  &:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 4px 16px rgba(122, 26, 46, 0.35);
  }

  &:disabled {
    opacity: 0.7;
    cursor: not-allowed;
  }

  .loading-icon { animation: spin 1s linear infinite; }
}

.save-success {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #52c41a;
  animation: fadeIn 0.3s ease-out;
}

.btn-delete {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 16px;
  border: 1px solid #e8d8d8;
  border-radius: var(--radius-sm);
  background: #fff;
  color: #999;
  cursor: pointer;
  font-family: var(--font-body);
  font-size: 13px;
  transition: all 0.2s;

  &:hover {
    border-color: var(--palace-red);
    color: var(--palace-red);
    background: rgba(122, 26, 46, 0.03);
  }
}

// === 对话框 ===
:deep(.prompt-dialog) {
  .el-dialog__header {
    border-bottom: 1px solid var(--gold-light);
    padding: 18px 24px;

    .el-dialog__title {
      font-family: var(--font-display);
      font-size: 18px;
      color: var(--palace-red);
      letter-spacing: 2px;
    }
  }

  .el-dialog__body {
    padding: var(--space-lg);
  }

  .el-dialog__footer {
    padding: 12px 24px 18px;
    border-top: 1px solid rgba(0,0,0,0.04);
  }
}

.dialog-form {
  :deep(.el-form-item__label) {
    font-family: var(--font-body);
    font-weight: 500;
    color: var(--ink);
    padding-bottom: 4px;
  }

  :deep(.el-input__wrapper) {
    border-radius: var(--radius-sm);
  }
}

.dialog-btn {
  padding: 8px 24px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--gold-light);
  font-family: var(--font-body);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;

  &.cancel {
    background: #fff;
    color: var(--ink-light);
    margin-right: 8px;

    &:hover {
      border-color: var(--gold);
      color: var(--ink);
    }
  }

  &.confirm {
    background: linear-gradient(135deg, var(--palace-red-light), var(--palace-red));
    color: #fff;
    border-color: transparent;

    &:hover {
      box-shadow: var(--shadow-gold);
    }
  }

  &.danger {
    background: var(--palace-red);
    color: #fff;
    border-color: transparent;

    &:hover {
      background: var(--palace-red-dark);
    }

    &:disabled {
      opacity: 0.7;
      cursor: not-allowed;
    }
  }
}

// 删除警告
.delete-warning {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  text-align: center;
  padding: 8px 0;

  p { font-size: 15px; color: var(--ink); }

  .delete-hint {
    font-size: 13px;
    color: var(--ink-light);
  }
}

// === 动画 ===
@keyframes spin {
  to { transform: rotate(360deg); }
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
