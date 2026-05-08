<template>
  <div class="admin-sessions">
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
          <h1 class="page-title">会话管理</h1>
          <p class="page-desc">查看所有用户的聊天会话记录，点击可查看对话详情</p>
        </div>
      </div>
      <div class="header-stats">
        <div class="stat-item">
          <span class="stat-num">{{ totalSessions }}</span>
          <span class="stat-label">总会话</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <span class="stat-num">{{ totalMessages }}</span>
          <span class="stat-label">总消息</span>
        </div>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <div class="search-input-wrap">
        <el-icon class="search-icon"><Search /></el-icon>
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索用户名、昵称或对话内容..."
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
    <div v-else class="table-container">
      <div class="table-wrapper">
        <table class="session-table">
          <thead>
            <tr>
              <th class="col-id">ID</th>
              <th class="col-user">用户</th>
              <th class="col-title">会话标题</th>
              <th class="col-count">消息数</th>
              <th class="col-time">最后活跃</th>
              <th class="col-action">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="s in sessions" :key="s.id" class="session-row">
              <td class="col-id">
                <span class="id-text">#{{ s.id }}</span>
              </td>
              <td class="col-user">
                <div class="user-cell">
                  <div class="user-avatar-xs">
                    {{ s.nickname?.charAt(0) || '?' }}
                  </div>
                  <div class="user-info">
                    <span class="user-nickname">{{ s.nickname }}</span>
                    <span class="user-username">@{{ s.username }}</span>
                  </div>
                </div>
              </td>
              <td class="col-title">
                <span class="title-text">{{ s.title }}</span>
              </td>
              <td class="col-count">
                <span class="count-badge">{{ s.messageCount }}</span>
              </td>
              <td class="col-time">
                <span class="time-text">{{ formatTime(s.updatedAt) }}</span>
              </td>
              <td class="col-action">
                <button class="view-btn" @click="openDetail(s)">
                  <el-icon :size="14"><View /></el-icon>
                  查看对话
                </button>
              </td>
            </tr>

            <!-- 空状态 -->
            <tr v-if="sessions.length === 0">
              <td colspan="6" class="empty-cell">
                <div class="empty-state">
                  <el-icon :size="40"><ChatLineSquare /></el-icon>
                  <p>{{ keyword ? '没有匹配的会话记录' : '暂无会话数据' }}</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div v-if="totalPages > 0" class="pagination-bar">
        <div class="page-info">
          共 {{ totalSessions }} 条会话，第 {{ currentPage + 1 }}/{{ totalPages }} 页
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

    <!-- 详情抽屉 -->
    <Transition name="drawer-fade">
      <div v-if="drawer.visible" class="drawer-overlay" @click.self="closeDetail">
        <div class="drawer-panel">
          <div class="drawer-header">
            <div class="drawer-header-left">
              <button class="drawer-back" @click="closeDetail">
                <el-icon :size="18"><ArrowRight /></el-icon>
              </button>
              <div>
                <h3 class="drawer-title">{{ drawer.session?.title }}</h3>
                <p class="drawer-meta">
                  {{ drawer.session?.nickname }} · {{ formatTime(drawer.session?.updatedAt) }}
                </p>
              </div>
            </div>
            <span class="drawer-badge">{{ drawer.messages.length }} 条消息</span>
          </div>

          <div class="drawer-body" ref="msgBodyRef">
            <div v-if="drawer.loading" class="detail-loading">
              <div class="loading-spinner"></div>
              <p>加载对话详情...</p>
            </div>

            <div v-else-if="drawer.messages.length === 0" class="detail-empty">
              <el-icon :size="36"><ChatDotRound /></el-icon>
              <p>该会话暂无消息</p>
            </div>

            <div v-else class="messages-list">
              <div
                v-for="(msg, idx) in drawer.messages"
                :key="msg.id || idx"
                class="msg-item"
                :class="msg.role"
              >
                <div class="msg-avatar">
                  <div v-if="msg.role === 'user'" class="user-avatar">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                      <circle cx="12" cy="8" r="4" fill="var(--palace-red)" opacity="0.8"/>
                      <path d="M4 20c2-4 6-5 8-5s6 1 8 5" stroke="var(--palace-red)" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                    </svg>
                  </div>
                  <div v-else class="ai-avatar">
                    <svg width="22" height="22" viewBox="0 0 40 40">
                      <rect width="40" height="40" rx="8" fill="var(--palace-red)"/>
                      <text x="20" y="27" text-anchor="middle" font-size="22" fill="var(--gold)" font-family="serif" font-weight="bold">洛</text>
                    </svg>
                  </div>
                </div>
                <div class="msg-bubble">
                  <div class="msg-sender">{{ msg.role === 'user' ? drawer.session?.nickname || '用户' : '洛阳助手' }}</div>
                  <div class="msg-content">{{ msg.content }}</div>
                </div>
              </div>
            </div>
          </div>

          <div class="drawer-footer">
            <span class="footer-hint">仅可查看，无法继续会话</span>
            <button class="close-btn" @click="closeDetail">
              <el-icon :size="14"><Close /></el-icon>
              关闭
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search, Close, WarningFilled, ChatLineSquare, ChatDotRound,
  View, ArrowLeft, ArrowRight, ArrowRight as ArrowRightIcon
} from '@element-plus/icons-vue'
import { getAdminSessionList, getAdminSessionMessages } from '@/api'

const sessions = ref([])
const loading = ref(true)
const error = ref('')
const keyword = ref('')
const currentPage = ref(0)
const totalPages = ref(0)
const totalSessions = ref(0)
const totalMessages = ref(0)

const drawer = ref({
  visible: false,
  loading: false,
  session: null,
  messages: []
})

const msgBodyRef = ref(null)

function formatTime(timeStr) {
  if (!timeStr) return '—'
  try {
    const d = new Date(timeStr)
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    const h = String(d.getHours()).padStart(2, '0')
    const min = String(d.getMinutes()).padStart(2, '0')
    return `${y}-${m}-${day} ${h}:${min}`
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
    const res = await getAdminSessionList({
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
      totalMessages.value = sessions.value.reduce((sum, s) => sum + (s.messageCount || 0), 0)
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

async function openDetail(session) {
  drawer.value = { visible: true, loading: true, session, messages: [] }
  try {
    const res = await getAdminSessionMessages(session.id)
    if (res.code === 200) {
      drawer.value.messages = res.data || []
    } else {
      ElMessage.error(res.message || '获取消息失败')
    }
  } catch (e) {
    ElMessage.error(e.message || '获取消息失败')
  } finally {
    drawer.value.loading = false
    await nextTick()
    if (msgBodyRef.value) {
      msgBodyRef.value.scrollTop = msgBodyRef.value.scrollHeight
    }
  }
}

function closeDetail() {
  drawer.value.visible = false
}

onMounted(fetchSessions)
</script>

<style scoped lang="scss">
.admin-sessions {
  padding: 32px;
  max-width: 1200px;
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

  .header-stats {
    display: flex;
    align-items: center;
    gap: 20px;
    background: #fff;
    padding: 12px 24px;
    border-radius: 12px;
    border: 1px solid var(--gold-light);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    flex-shrink: 0;

    .stat-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 2px;

      .stat-num {
        font-family: var(--font-body);
        font-size: 22px;
        font-weight: 700;
        color: var(--palace-red);
        line-height: 1.2;
      }

      .stat-label {
        font-size: 11px;
        color: var(--ink-light);
        white-space: nowrap;
      }
    }

    .stat-divider {
      width: 1px;
      height: 32px;
      background: var(--gold-light);
    }
  }
}

/* ===== 搜索栏 ===== */
.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;

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

    .search-icon {
      color: var(--ink-light);
      flex-shrink: 0;
      margin-right: 8px;
    }

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

/* ===== 加载状态 ===== */
.loading-state {
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
}

@keyframes spin { to { transform: rotate(360deg); } }

/* ===== 错误状态 ===== */
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  color: var(--ink-light);

  .el-icon { color: var(--palace-red-light); margin-bottom: 12px; }
  p { font-size: 14px; margin-bottom: 20px; }

  .retry-btn {
    padding: 8px 24px;
    background: var(--palace-red);
    color: #fff;
    border: none;
    border-radius: 8px;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      background: var(--palace-red-dark);
      box-shadow: 0 4px 12px rgba(122, 26, 46, 0.25);
    }
  }
}

/* ===== 表格容器 ===== */
.table-container {
  background: #fff;
  border-radius: 16px;
  border: 1px solid var(--gold-light);
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.table-wrapper { overflow-x: auto; }

/* ===== 会话表格 ===== */
.session-table {
  width: 100%;
  border-collapse: collapse;
  font-family: var(--font-body);

  thead {
    background: linear-gradient(135deg, var(--palace-red), var(--palace-red-dark));

    th {
      padding: 14px 16px;
      font-size: 12px;
      font-weight: 600;
      color: rgba(255, 255, 255, 0.85);
      text-transform: uppercase;
      letter-spacing: 1.5px;
      text-align: left;
      white-space: nowrap;

      &.col-id { width: 70px; }
      &.col-user { min-width: 180px; }
      &.col-title { min-width: 200px; }
      &.col-count { width: 80px; text-align: center; }
      &.col-time { min-width: 140px; }
      &.col-action { width: 120px; text-align: center; }
    }

    th.col-count { text-align: center; }
  }

  tbody {
    tr {
      transition: background 0.2s;

      &:nth-child(even) { background: rgba(249, 247, 242, 0.5); }
      &:hover { background: rgba(200, 164, 92, 0.06); }
    }

    td {
      padding: 14px 16px;
      font-size: 14px;
      color: var(--ink);
      border-bottom: 1px solid rgba(200, 164, 92, 0.1);
      vertical-align: middle;
    }
  }
}

.id-text {
  font-family: var(--font-body);
  font-size: 12px;
  color: var(--ink-light);
  font-weight: 500;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;

  .user-avatar-xs {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    flex-shrink: 0;
    background: var(--gold);
    color: var(--palace-red);
    display: flex;
    align-items: center;
    justify-content: center;
    font-family: var(--font-display);
    font-size: 14px;
    font-weight: 600;
  }

  .user-info {
    display: flex;
    flex-direction: column;
    gap: 1px;

    .user-nickname { font-weight: 600; font-size: 14px; color: var(--ink); }
    .user-username { font-size: 12px; color: var(--ink-light); }
  }
}

.title-text {
  font-size: 14px;
  color: var(--ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 280px;
  display: block;
}

.count-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 24px;
  padding: 0 8px;
  background: rgba(200, 164, 92, 0.12);
  color: #8a6d2b;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.time-text {
  font-size: 13px;
  color: var(--ink-light);
}

.view-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  background: rgba(200, 164, 92, 0.1);
  color: #8a6d2b;
  border: 1px solid rgba(200, 164, 92, 0.3);
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
  letter-spacing: 0.5px;

  &:hover {
    background: linear-gradient(135deg, var(--gold), #b8943e);
    color: #3A0A15;
    border-color: var(--gold);
    box-shadow: 0 4px 12px rgba(200, 164, 92, 0.3);
  }
}

/* ===== 空状态 ===== */
.empty-cell { text-align: center; padding: 60px 20px !important; }

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--ink-light);

  .el-icon { opacity: 0.4; }
  p { font-size: 14px; }
}

/* ===== 分页 ===== */
.pagination-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-top: 1px solid rgba(200, 164, 92, 0.1);

  .page-info {
    font-size: 13px;
    color: var(--ink-light);
  }

  .page-controls {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .page-btn {
    width: 34px;
    height: 34px;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 1px solid var(--gold-light);
    border-radius: 8px;
    background: #fff;
    color: var(--ink);
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;

    &:hover:not(:disabled) {
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
    font-size: 13px;
  }
}

/* ===== 详情抽屉 ===== */
.drawer-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  justify-content: flex-end;
  z-index: 2000;
  backdrop-filter: blur(3px);
}

.drawer-panel {
  width: 520px;
  max-width: 95vw;
  height: 100vh;
  background: var(--ivory);
  display: flex;
  flex-direction: column;
  box-shadow: -8px 0 32px rgba(0, 0, 0, 0.12);
}

.drawer-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 24px 24px 16px;
  border-bottom: 1px solid rgba(200, 164, 92, 0.15);
  flex-shrink: 0;

  .drawer-header-left {
    display: flex;
    align-items: flex-start;
    gap: 12px;

    .drawer-back {
      width: 36px;
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #fff;
      border: 1px solid var(--gold-light);
      border-radius: 10px;
      cursor: pointer;
      color: var(--ink);
      transition: all 0.2s;
      margin-top: 2px;

      &:hover {
        border-color: var(--gold);
        color: var(--palace-red);
      }

      .el-icon { transform: rotate(180deg); }
    }
  }

  .drawer-title {
    font-family: var(--font-body);
    font-size: 18px;
    font-weight: 700;
    color: var(--ink);
    margin: 0 0 4px;
  }

  .drawer-meta {
    font-size: 13px;
    color: var(--ink-light);
    margin: 0;
  }

  .drawer-badge {
    flex-shrink: 0;
    padding: 4px 12px;
    background: rgba(200, 164, 92, 0.12);
    color: #8a6d2b;
    border-radius: 10px;
    font-size: 12px;
    font-weight: 600;
    white-space: nowrap;
  }
}

.drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px;

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: var(--gold-light); border-radius: 2px; }
}

.detail-loading,
.detail-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: var(--ink-light);

  .loading-spinner {
    width: 30px;
    height: 30px;
    border: 3px solid var(--gold-light);
    border-top-color: var(--gold);
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
    margin-bottom: 12px;
  }

  p { font-size: 14px; }
}

/* ===== 消息列表 ===== */
.messages-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 8px 0;
}

.msg-item {
  display: flex;
  gap: 10px;
  max-width: 100%;

  &.user { flex-direction: row-reverse; }

  .msg-avatar {
    flex-shrink: 0;
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;

    .user-avatar {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      background: var(--gold-light);
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .ai-avatar {
      width: 36px;
      height: 36px;
      border-radius: 10px;
      background: var(--palace-red);
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }

  .msg-bubble {
    max-width: 380px;
    padding: 12px 16px;
    border-radius: 12px;
    font-size: 14px;
    line-height: 1.6;
  }

  &.user .msg-bubble {
    background: linear-gradient(135deg, var(--palace-red-light), var(--palace-red));
    color: #fff;
    border-bottom-right-radius: 4px;

    .msg-sender {
      font-size: 11px;
      font-weight: 600;
      opacity: 0.8;
      margin-bottom: 4px;
    }

    .msg-content { word-break: break-word; }
  }

  &.assistant .msg-bubble {
    background: #fff;
    color: var(--ink);
    border: 1px solid rgba(200, 164, 92, 0.15);
    border-bottom-left-radius: 4px;

    .msg-sender {
      font-size: 11px;
      font-weight: 600;
      color: var(--palace-red);
      margin-bottom: 4px;
    }

    .msg-content {
      word-break: break-word;
      white-space: pre-wrap;
    }
  }
}

.drawer-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-top: 1px solid rgba(200, 164, 92, 0.15);
  flex-shrink: 0;

  .footer-hint {
    font-size: 12px;
    color: var(--ink-light);
    font-style: italic;
  }

  .close-btn {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 8px 18px;
    background: #fff;
    color: var(--ink-light);
    border: 1px solid var(--gold-light);
    border-radius: 8px;
    font-size: 13px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      border-color: var(--gold);
      color: var(--ink);
    }
  }
}

/* ===== 抽屉动画 ===== */
.drawer-fade-enter-active { transition: opacity 0.3s ease;
  .drawer-panel { transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1); }
}
.drawer-fade-leave-active { transition: opacity 0.2s ease;
  .drawer-panel { transition: transform 0.2s ease; }
}
.drawer-fade-enter-from { opacity: 0;
  .drawer-panel { transform: translateX(100%); }
}
.drawer-fade-leave-to { opacity: 0;
  .drawer-panel { transform: translateX(100%); }
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .admin-sessions { padding: 20px 16px; }

  .page-header {
    flex-direction: column;
    .header-stats { width: 100%; justify-content: center; }
  }

  .session-table {
    thead th {
      &.col-time, &.col-title { display: none; }
    }
    tbody td {
      &.col-time, &.col-title { display: none; }
    }
  }

  .pagination-bar {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
