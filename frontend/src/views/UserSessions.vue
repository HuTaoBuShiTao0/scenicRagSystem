<template>
  <div class="user-sessions">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
            <rect x="2" y="4" width="24" height="18" rx="4" fill="var(--palace-red)"/>
            <path d="M8 10h12M8 14h8M8 18h10" stroke="var(--gold)" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
        </div>
        <div>
          <h1 class="page-title">历史会话</h1>
          <p class="page-desc">共 {{ totalSessions }} 条对话记录</p>
        </div>
      </div>
      <button class="new-session-btn" @click="createNewSession">
        <el-icon :size="16"><Plus /></el-icon>
        新建会话
      </button>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <div class="search-input-wrap">
        <el-icon class="search-icon"><Search /></el-icon>
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索会话标题或对话内容..."
          @keyup.enter="doSearch"
        />
        <button v-if="keyword" class="search-clear" @click="clearSearch">
          <el-icon><Close /></el-icon>
        </button>
      </div>
      <button class="search-btn" @click="doSearch">
        <el-icon :size="16"><Search /></el-icon>
        搜索
      </button>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>正在加载会话列表...</p>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-state">
      <el-icon :size="48"><WarningFilled /></el-icon>
      <p>{{ error }}</p>
      <button class="retry-btn" @click="fetchSessions">重新加载</button>
    </div>

    <!-- 会话列表 -->
    <div v-else class="sessions-container">
      <!-- 空状态 -->
      <div v-if="sessions.length === 0" class="empty-state">
        <div class="empty-icon">
          <svg width="64" height="64" viewBox="0 0 64 64" fill="none">
            <rect x="8" y="12" width="48" height="36" rx="6" stroke="var(--gold-light)" stroke-width="2" fill="none"/>
            <path d="M16 24h32M16 32h24M16 40h16" stroke="var(--gold-light)" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </div>
        <p class="empty-title">{{ keyword ? '没有匹配的会话' : '暂无会话记录' }}</p>
        <p class="empty-desc">{{ keyword ? '试试其他关键词' : '开始一段新的对话吧' }}</p>
        <button v-if="!keyword" class="start-btn" @click="createNewSession">
          <el-icon :size="16"><ChatDotRound /></el-icon>
          开始对话
        </button>
        <button v-else class="start-btn" @click="clearSearch">
          <el-icon :size="16"><Refresh /></el-icon>
          清除搜索
        </button>
      </div>

      <!-- 会话卡片列表 -->
      <div v-else class="session-list">
        <div
          v-for="s in sessions"
          :key="s.id"
          class="session-card"
          @click="goToSession(s)"
        >
          <div class="card-left">
            <div class="card-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <rect x="3" y="4" width="18" height="14" rx="3" fill="var(--palace-red)" opacity="0.15"/>
                <path d="M7 9h10M7 13h7M7 17h4" stroke="var(--palace-red)" stroke-width="1.5" stroke-linecap="round"/>
              </svg>
            </div>
          </div>
          <div class="card-body">
            <div class="card-top">
              <span class="card-title">{{ s.title }}</span>
              <button class="card-delete" @click.stop="confirmDelete(s)" title="删除会话">
                <el-icon :size="14"><Delete /></el-icon>
              </button>
            </div>
            <div class="card-meta">
              <span class="meta-item">
                <el-icon :size="12"><ChatDotRound /></el-icon>
                {{ s.messageCount }} 条消息
              </span>
              <span class="meta-dot"></span>
              <span class="meta-item time">{{ formatTime(s.updatedAt) }}</span>
            </div>
          </div>
          <div class="card-arrow">
            <el-icon :size="16"><ArrowRight /></el-icon>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="totalPages > 1" class="pagination-bar">
        <div class="page-info">
          第 {{ currentPage + 1 }}/{{ totalPages }} 页，共 {{ totalSessions }} 条
        </div>
        <div class="page-controls">
          <button class="page-btn" :disabled="currentPage <= 0" @click="goPage(currentPage - 1)">
            <el-icon><ArrowLeft /></el-icon>
          </button>
          <template v-for="p in visiblePages" :key="p">
            <button
              v-if="p !== '...'"
              class="page-btn"
              :class="{ active: p === currentPage }"
              @click="goPage(p)"
            >{{ p + 1 }}</button>
            <span v-else class="page-ellipsis">...</span>
          </template>
          <button class="page-btn" :disabled="currentPage >= totalPages - 1" @click="goPage(currentPage + 1)">
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>
      </div>
    </div>

    <!-- 删除确认对话框 -->
    <Teleport to="body">
      <Transition name="dialog-fade">
        <div v-if="deleteDialog.visible" class="dialog-overlay" @click.self="deleteDialog.visible = false">
          <div class="dialog-card">
            <div class="dialog-icon-wrap">
              <el-icon :size="28"><WarningFilled /></el-icon>
            </div>
            <h3 class="dialog-title">确认删除</h3>
            <p class="dialog-desc">该操作将删除此会话及所有消息，不可恢复。</p>
            <div class="dialog-actions">
              <button class="dialog-btn cancel" @click="deleteDialog.visible = false" :disabled="deleteDialog.loading">取消</button>
              <button class="dialog-btn confirm" @click="handleDelete" :disabled="deleteDialog.loading">
                <span v-if="deleteDialog.loading" class="btn-loading"></span>
                {{ deleteDialog.loading ? '删除中...' : '确认删除' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Plus, ChatDotRound, Delete, WarningFilled,
  Search, Close, ArrowLeft, ArrowRight, Refresh
} from '@element-plus/icons-vue'
import { createSession, deleteSession, getSessionsPaged } from '@/api'

const router = useRouter()
const sessions = ref([])
const loading = ref(true)
const error = ref('')
const keyword = ref('')
const currentPage = ref(0)
const totalPages = ref(0)
const totalSessions = ref(0)

const deleteDialog = ref({
  visible: false,
  loading: false,
  sessionId: null
})

function formatTime(timeStr) {
  if (!timeStr) return '—'
  try {
    const d = new Date(timeStr)
    const now = new Date()
    const diff = now - d
    const minutes = Math.floor(diff / 60000)
    const hours = Math.floor(diff / 3600000)
    const days = Math.floor(diff / 86400000)

    if (minutes < 1) return '刚刚'
    if (minutes < 60) return `${minutes} 分钟前`
    if (hours < 24) return `${hours} 小时前`
    if (days < 7) return `${days} 天前`

    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${y}-${m}-${day}`
  } catch {
    return timeStr
  }
}

const visiblePages = computed(() => {
  const tp = totalPages.value
  const cp = currentPage.value
  if (tp <= 7) return Array.from({ length: tp }, (_, i) => i)

  const pages = []
  pages.push(0)
  if (cp > 3) pages.push('...')
  for (let i = Math.max(1, cp - 1); i <= Math.min(tp - 2, cp + 1); i++) {
    pages.push(i)
  }
  if (cp < tp - 4) pages.push('...')
  if (tp > 1) pages.push(tp - 1)
  return pages
})

async function fetchSessions() {
  loading.value = true
  error.value = ''
  try {
    const res = await getSessionsPaged({
      keyword: keyword.value,
      page: currentPage.value,
      size: 15
    })
    if (res.code === 200) {
      const data = res.data
      sessions.value = data.content || []
      currentPage.value = data.page || 0
      totalPages.value = data.totalPages || 0
      totalSessions.value = data.totalElements || 0
    } else {
      error.value = res.message || '获取会话列表失败'
    }
  } catch (e) {
    error.value = e.message || '网络错误，请稍后重试'
  } finally {
    loading.value = false
  }
}

function doSearch() {
  currentPage.value = 0
  fetchSessions()
}

function clearSearch() {
  keyword.value = ''
  currentPage.value = 0
  fetchSessions()
}

function goPage(page) {
  if (page < 0 || page >= totalPages.value) return
  currentPage.value = page
  fetchSessions()
}

async function createNewSession() {
  try {
    const res = await createSession()
    if (res.code === 200) {
      router.push(`/?session=${res.data.id}`)
    } else {
      ElMessage.error(res.message || '创建失败')
    }
  } catch (e) {
    ElMessage.error('创建会话失败')
  }
}

function goToSession(session) {
  router.push(`/?session=${session.id}`)
}

function confirmDelete(session) {
  deleteDialog.value = { visible: true, loading: false, sessionId: session.id }
}

async function handleDelete() {
  deleteDialog.value.loading = true
  try {
    await deleteSession(deleteDialog.value.sessionId)
    ElMessage.success('会话已删除')
    deleteDialog.value.visible = false
    await fetchSessions()
  } catch (e) {
    ElMessage.error('删除失败')
  } finally {
    deleteDialog.value.loading = false
  }
}

onMounted(fetchSessions)
</script>

<style scoped lang="scss">
.user-sessions {
  padding: 32px;
  max-width: 820px;
  margin: 0 auto;
}

/* ===== 页面头部 ===== */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  gap: 24px;

  .header-left {
    display: flex;
    align-items: flex-start;
    gap: 16px;

    .header-icon {
      width: 52px;
      height: 52px;
      background: linear-gradient(135deg, var(--palace-red), var(--palace-red-dark));
      border-radius: 14px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      box-shadow: 0 4px 12px rgba(122, 26, 46, 0.25);
    }

    .page-title {
      font-family: var(--font-body);
      font-size: 24px;
      font-weight: 700;
      color: var(--ink);
      margin: 0 0 4px;
      letter-spacing: 1px;
    }

    .page-desc {
      font-size: 13px;
      color: var(--ink-light);
      margin: 0;
    }
  }

  .new-session-btn {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 10px 20px;
    background: linear-gradient(135deg, var(--palace-red-light), var(--palace-red));
    color: #fff;
    border: none;
    border-radius: 10px;
    font-family: var(--font-body);
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s ease;
    letter-spacing: 1px;
    flex-shrink: 0;

    &:hover {
      background: var(--palace-red-dark);
      box-shadow: 0 4px 16px rgba(122, 26, 46, 0.3);
      transform: translateY(-1px);
    }
  }
}

/* ===== 搜索栏 ===== */
.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;

  .search-input-wrap {
    flex: 1;
    display: flex;
    align-items: center;
    background: #fff;
    border: 1px solid var(--gold-light);
    border-radius: 10px;
    padding: 0 14px;
    transition: all 0.3s ease;

    &:focus-within {
      border-color: var(--gold);
      box-shadow: 0 0 0 3px rgba(200, 164, 92, 0.1);
    }

    .search-icon { color: var(--ink-light); flex-shrink: 0; margin-right: 8px; }

    .search-input {
      flex: 1;
      height: 42px;
      border: none;
      outline: none;
      font-family: var(--font-body);
      font-size: 14px;
      color: var(--ink);
      background: transparent;

      &::placeholder { color: var(--ink-light); opacity: 0.6; }
    }

    .search-clear {
      background: none;
      border: none;
      color: var(--ink-light);
      cursor: pointer;
      padding: 4px;
      display: flex;
      align-items: center;
      transition: color 0.2s;
      &:hover { color: var(--ink); }
    }
  }

  .search-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 0 20px;
    background: linear-gradient(135deg, var(--palace-red-light), var(--palace-red));
    color: #fff;
    border: none;
    border-radius: 10px;
    font-family: var(--font-body);
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s ease;
    letter-spacing: 1px;

    &:hover {
      background: var(--palace-red-dark);
      box-shadow: 0 4px 12px rgba(122, 26, 46, 0.25);
    }
  }
}

/* ===== 加载/错误状态 ===== */
.loading-state, .error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  color: var(--ink-light);

  .loading-spinner {
    width: 36px;
    height: 36px;
    border: 3px solid var(--gold-light);
    border-top-color: var(--gold);
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
    margin-bottom: 16px;
  }

  p { font-size: 14px; }
  .el-icon { color: var(--palace-red-light); margin-bottom: 12px; }

  .retry-btn {
    padding: 8px 24px;
    background: var(--palace-red);
    color: #fff;
    border: none;
    border-radius: 8px;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.2s;
    margin-top: 12px;
    &:hover { background: var(--palace-red-dark); box-shadow: 0 4px 12px rgba(122, 26, 46, 0.25); }
  }
}

@keyframes spin { to { transform: rotate(360deg); } }

/* ===== 空状态 ===== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;

  .empty-icon { margin-bottom: 16px; opacity: 0.5; }

  .empty-title {
    font-family: var(--font-body);
    font-size: 18px;
    font-weight: 600;
    color: var(--ink-light);
    margin: 0 0 6px;
  }

  .empty-desc {
    font-size: 14px;
    color: var(--ink-light);
    margin: 0 0 24px;
    opacity: 0.7;
  }

  .start-btn {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 10px 24px;
    background: linear-gradient(135deg, var(--palace-red-light), var(--palace-red));
    color: #fff;
    border: none;
    border-radius: 10px;
    font-family: var(--font-body);
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s ease;
    &:hover { background: var(--palace-red-dark); box-shadow: 0 4px 16px rgba(122, 26, 46, 0.3); }
  }
}

/* ===== 会话卡片列表 ===== */
.session-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.session-card {
  display: flex;
  align-items: center;
  background: #fff;
  border: 1px solid var(--gold-light);
  border-radius: 14px;
  padding: 16px 18px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  gap: 14px;

  &:hover {
    border-color: var(--gold);
    box-shadow: 0 6px 24px rgba(200, 164, 92, 0.12);
    transform: translateX(4px);

    .card-arrow { opacity: 1; transform: translateX(0); }
  }

  .card-left {
    flex-shrink: 0;
    .card-icon {
      width: 40px; height: 40px;
      background: rgba(122, 26, 46, 0.06);
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }

  .card-body {
    flex: 1;
    min-width: 0;

    .card-top {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 6px;

      .card-title {
        font-family: var(--font-body);
        font-size: 14px;
        font-weight: 600;
        color: var(--ink);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        flex: 1;
      }

      .card-delete {
        width: 26px; height: 26px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: none;
        border: none;
        border-radius: 6px;
        color: var(--ink-light);
        cursor: pointer;
        transition: all 0.2s;
        flex-shrink: 0;
        margin-left: 8px;
        opacity: 0;

        .session-card:hover & { opacity: 1; }

        &:hover { background: rgba(122, 26, 46, 0.08); color: var(--palace-red); }
      }
    }

    .card-meta {
      display: flex;
      align-items: center;
      gap: 8px;

      .meta-item {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        font-size: 12px;
        color: var(--ink-light);
        .el-icon { opacity: 0.5; }
      }

      .meta-dot {
        width: 3px; height: 3px;
        border-radius: 50%;
        background: var(--gold-light);
        flex-shrink: 0;
      }
    }
  }

  .card-arrow {
    flex-shrink: 0;
    color: var(--gold);
    opacity: 0;
    transform: translateX(-4px);
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  }
}

/* ===== 分页 ===== */
.pagination-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 24px;
  margin-top: 8px;

  .page-info { font-size: 13px; color: var(--ink-light); }
  .page-controls { display: flex; align-items: center; gap: 4px; }

  .page-btn {
    width: 34px; height: 34px;
    display: flex; align-items: center; justify-content: center;
    border: 1px solid var(--gold-light);
    border-radius: 8px;
    background: #fff;
    color: var(--ink);
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;

    &:hover:not(:disabled) { border-color: var(--gold); color: var(--palace-red); }
    &.active { background: var(--palace-red); color: #fff; border-color: var(--palace-red); }
    &:disabled { opacity: 0.4; cursor: not-allowed; }
  }

  .page-ellipsis { padding: 0 4px; color: var(--ink-light); font-size: 13px; }
}

/* ===== 删除对话框 ===== */
.dialog-overlay {
  position: fixed; inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex; align-items: center; justify-content: center;
  z-index: 2000; backdrop-filter: blur(4px);
}

.dialog-card {
  width: 380px; max-width: 90vw;
  background: var(--ivory);
  border-radius: 20px;
  padding: 36px 32px 28px;
  text-align: center;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(200, 164, 92, 0.2);

  .dialog-icon-wrap {
    width: 56px; height: 56px;
    border-radius: 50%;
    background: rgba(122, 26, 46, 0.1);
    color: var(--palace-red);
    display: inline-flex; align-items: center; justify-content: center;
    margin-bottom: 16px;
  }

  .dialog-title {
    font-family: var(--font-body); font-size: 18px; font-weight: 700;
    color: var(--ink); margin: 0 0 8px;
  }

  .dialog-desc { font-size: 14px; color: var(--ink-light); margin: 0 0 28px; line-height: 1.5; }

  .dialog-actions {
    display: flex; gap: 12px;

    .dialog-btn {
      flex: 1; padding: 11px; border-radius: 10px;
      font-size: 14px; font-weight: 600;
      cursor: pointer; transition: all 0.25s ease;
      display: flex; align-items: center; justify-content: center; gap: 6px;

      &:disabled { opacity: 0.6; cursor: not-allowed; }

      &.cancel {
        background: #fff; color: var(--ink-light); border: 1px solid var(--gold-light);
        &:hover:not(:disabled) { border-color: var(--gold); color: var(--ink); }
      }

      &.confirm {
        background: linear-gradient(135deg, var(--palace-red-light), var(--palace-red));
        color: #fff; border: none;
        &:hover:not(:disabled) { box-shadow: 0 4px 16px rgba(122, 26, 46, 0.3); }
      }

      .btn-loading {
        width: 16px; height: 16px;
        border: 2px solid rgba(255, 255, 255, 0.3);
        border-top-color: #fff;
        border-radius: 50%;
        animation: spin 0.7s linear infinite;
      }
    }
  }
}

.dialog-fade-enter-active { transition: opacity 0.3s ease;
  .dialog-card { transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.3s ease; } }
.dialog-fade-leave-active { transition: opacity 0.2s ease;
  .dialog-card { transition: transform 0.2s ease, opacity 0.2s ease; } }
.dialog-fade-enter-from { opacity: 0;
  .dialog-card { transform: scale(0.92) translateY(12px); opacity: 0; } }
.dialog-fade-leave-to { opacity: 0;
  .dialog-card { transform: scale(0.96) translateY(8px); opacity: 0; } }

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .user-sessions { padding: 20px 16px; }

  .page-header {
    flex-direction: column;
    .new-session-btn { width: 100%; justify-content: center; }
  }

  .pagination-bar { flex-direction: column; gap: 12px; }

  .session-card {
    .card-delete { opacity: 1 !important; }
    .card-arrow { display: none; }
  }
}
</style>
