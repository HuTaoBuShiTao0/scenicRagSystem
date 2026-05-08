<template>
  <div class="user-management">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
            <path d="M14 2C7.373 2 2 7.373 2 14s5.373 12 12 12 12-5.373 12-12S20.627 2 14 2z" fill="var(--palace-red)"/>
            <path d="M14 6a4 4 0 100 8 4 4 0 000-8zm-6 12c0-2 4-3.5 6-3.5s6 1.5 6 3.5" stroke="var(--gold)" stroke-width="1.5" fill="none"/>
          </svg>
        </div>
        <div>
          <h1 class="page-title">用户管理</h1>
          <p class="page-desc">管理系统中的所有账号，可对普通用户进行升权和降权操作</p>
        </div>
      </div>
      <div class="header-stats">
        <div class="stat-item">
          <span class="stat-num">{{ users.length }}</span>
          <span class="stat-label">总用户</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <span class="stat-num">{{ adminCount }}</span>
          <span class="stat-label">管理员</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <span class="stat-num">{{ ownerCount }}</span>
          <span class="stat-label">超级管理员</span>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>正在加载用户数据...</p>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-state">
      <el-icon :size="48"><WarningFilled /></el-icon>
      <p>{{ error }}</p>
      <button class="retry-btn" @click="fetchUsers">重新加载</button>
    </div>

    <!-- 用户表格 -->
    <div v-else class="table-container">
      <div class="table-wrapper">
        <table class="user-table">
          <thead>
            <tr>
              <th class="col-id">ID</th>
              <th class="col-user">用户</th>
              <th class="col-username">用户名</th>
              <th class="col-role">角色</th>
              <th class="col-time">注册时间</th>
              <th class="col-actions">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in users" :key="user.id" class="user-row" :class="{ 'is-owner': user.role === 'OWNER' }">
              <td class="col-id">
                <span class="id-text">#{{ user.id }}</span>
              </td>
              <td class="col-user">
                <div class="user-cell">
                  <div class="user-avatar-sm">
                    <img v-if="user.avatar" :src="user.avatar" />
                    <span v-else>{{ user.nickname?.charAt(0) || '?' }}</span>
                  </div>
                  <span class="user-nickname">{{ user.nickname }}</span>
                </div>
              </td>
              <td class="col-username">
                <span class="username-text">@{{ user.username }}</span>
              </td>
              <td class="col-role">
                <span class="role-badge" :class="roleClass(user.role)">
                  {{ roleLabel(user.role) }}
                </span>
              </td>
              <td class="col-time">
                <span class="time-text">{{ formatTime(user.createdAt) }}</span>
              </td>
              <td class="col-actions">
                <template v-if="user.role === 'OWNER' || user.id === auth.userId">
                  <span class="no-action" :title="user.id === auth.userId ? '不能操作自己的账号' : ''">
                    {{ user.id === auth.userId ? '— 当前用户' : '—' }}
                  </span>
                </template>
                <template v-else-if="user.role === 'ADMIN'">
                  <button class="action-btn demote" @click="confirmDemote(user)" :disabled="actioningId === user.id">
                    <el-icon :size="14"><ArrowDown /></el-icon>
                    降级为用户
                  </button>
                </template>
                <template v-else>
                  <button class="action-btn promote" @click="confirmPromote(user)" :disabled="actioningId === user.id">
                    <el-icon :size="14"><ArrowUp /></el-icon>
                    升级为管理员
                  </button>
                </template>
              </td>
            </tr>

            <!-- 空状态 -->
            <tr v-if="users.length === 0">
              <td colspan="6" class="empty-cell">
                <div class="empty-state">
                  <el-icon :size="40"><UserFilled /></el-icon>
                  <p>暂无用户数据</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 确认对话框 -->
    <Teleport to="body">
      <Transition name="dialog-fade">
        <div v-if="dialog.visible" class="dialog-overlay" @click.self="closeDialog">
          <div class="dialog-card">
            <div class="dialog-header">
              <div class="dialog-icon" :class="dialog.type">
                <el-icon :size="24" v-if="dialog.type === 'promote'"><Top /></el-icon>
                <el-icon :size="24" v-else><Bottom /></el-icon>
              </div>
              <h3 class="dialog-title">{{ dialog.title }}</h3>
              <p class="dialog-desc">{{ dialog.description }}</p>
            </div>
            <div class="dialog-user-preview">
              <div class="preview-avatar">
                {{ dialog.user?.nickname?.charAt(0) || '?' }}
              </div>
              <div class="preview-info">
                <span class="preview-name">{{ dialog.user?.nickname }}</span>
                <span class="preview-username">@{{ dialog.user?.username }}</span>
                <span class="preview-role-tag" :class="roleClass(dialog.user?.role)">{{ roleLabel(dialog.user?.role) }}</span>
                <span class="preview-arrow">
                  <el-icon><ArrowRight /></el-icon>
                </span>
                <span class="preview-role-tag" :class="roleClass(dialog.targetRole)">{{ roleLabel(dialog.targetRole) }}</span>
              </div>
            </div>
            <div class="dialog-actions">
              <button class="dialog-btn cancel" @click="closeDialog" :disabled="dialog.loading">取消</button>
              <button class="dialog-btn confirm" :class="dialog.type" @click="handleRoleChange" :disabled="dialog.loading">
                <span v-if="dialog.loading" class="btn-loading"></span>
                {{ dialog.loading ? '处理中...' : '确认执行' }}
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
import { ElMessage } from 'element-plus'
import { WarningFilled, UserFilled, ArrowDown, ArrowUp, Top, Bottom, ArrowRight } from '@element-plus/icons-vue'
import { getUserList, updateUserRole } from '@/api'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const users = ref([])
const loading = ref(true)
const error = ref('')
const actioningId = ref(null)

const dialog = ref({
  visible: false,
  type: 'promote',
  title: '',
  description: '',
  user: null,
  targetRole: '',
  loading: false
})

const adminCount = computed(() => users.value.filter(u => u.role === 'ADMIN').length)
const ownerCount = computed(() => users.value.filter(u => u.role === 'OWNER').length)

function roleClass(role) {
  if (role === 'OWNER') return 'badge-owner'
  if (role === 'ADMIN') return 'badge-admin'
  return 'badge-user'
}

function roleLabel(role) {
  if (role === 'OWNER') return '超级管理员'
  if (role === 'ADMIN') return '管理员'
  return '普通用户'
}

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

async function fetchUsers() {
  loading.value = true
  error.value = ''
  try {
    const res = await getUserList()
    if (res.code === 200) {
      users.value = res.data || []
    } else {
      error.value = res.message || '获取用户列表失败'
    }
  } catch (e) {
    error.value = e.message || '网络错误，请稍后重试'
  } finally {
    loading.value = false
  }
}

function confirmPromote(user) {
  dialog.value = {
    visible: true,
    type: 'promote',
    title: '升级为管理员',
    description: '该用户将获得知识库管理和用户管理等管理员权限。',
    user,
    targetRole: 'ADMIN',
    loading: false
  }
}

function confirmDemote(user) {
  dialog.value = {
    visible: true,
    type: 'demote',
    title: '降级为普通用户',
    description: '该用户将失去所有管理员权限。',
    user,
    targetRole: 'USER',
    loading: false
  }
}

function closeDialog() {
  dialog.value.visible = false
  dialog.value.loading = false
}

async function handleRoleChange() {
  if (!dialog.value.user) return
  dialog.value.loading = true
  actioningId.value = dialog.value.user.id

  try {
    const res = await updateUserRole(dialog.value.user.id, dialog.value.targetRole)
    if (res.code === 200) {
      ElMessage.success(
        dialog.value.type === 'promote'
          ? `已将 ${dialog.value.user.nickname} 升级为管理员`
          : `已将 ${dialog.value.user.nickname} 降级为普通用户`
      )
      closeDialog()
      await fetchUsers()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    dialog.value.loading = false
    actioningId.value = null
  }
}

onMounted(fetchUsers)
</script>

<style scoped lang="scss">
.user-management {
  padding: 32px;
  max-width: 1100px;
  margin: 0 auto;
}

/* ===== 页面头部 ===== */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 32px;
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

  p {
    font-size: 14px;
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

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

.table-wrapper {
  overflow-x: auto;
}

/* ===== 用户表格 ===== */
.user-table {
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
      &.col-username { min-width: 120px; }
      &.col-role { width: 110px; }
      &.col-time { min-width: 140px; }
      &.col-actions { width: 160px; text-align: center; }
    }
  }

  tbody {
    tr {
      transition: background 0.2s;

      &:nth-child(even) {
        background: rgba(249, 247, 242, 0.5);
      }

      &:hover {
        background: rgba(200, 164, 92, 0.06);
      }

      &.is-owner {
        background: linear-gradient(90deg, rgba(200, 164, 92, 0.08), transparent);
      }
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

/* ID 列 */
.id-text {
  font-family: var(--font-body);
  font-size: 12px;
  color: var(--ink-light);
  font-weight: 500;
}

/* 用户列 */
.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;

  .user-avatar-sm {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    overflow: hidden;
    flex-shrink: 0;
    background: var(--gold);
    color: var(--palace-red);
    display: flex;
    align-items: center;
    justify-content: center;
    font-family: var(--font-display);
    font-size: 14px;
    font-weight: 600;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  .user-nickname {
    font-weight: 600;
    font-size: 14px;
    color: var(--ink);
  }
}

/* 用户名列 */
.username-text {
  font-size: 13px;
  color: var(--ink-light);
}

/* 时间列 */
.time-text {
  font-size: 13px;
  color: var(--ink-light);
}

/* ===== 角色徽章 ===== */
.role-badge {
  display: inline-flex;
  align-items: center;
  padding: 3px 12px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.5px;

  &.badge-owner {
    background: linear-gradient(135deg, var(--gold), #b8943e);
    color: #3A0A15;
    box-shadow: 0 2px 6px rgba(200, 164, 92, 0.3);
  }

  &.badge-admin {
    background: linear-gradient(135deg, var(--palace-red-light), var(--palace-red));
    color: #fff;
    box-shadow: 0 2px 6px rgba(122, 26, 46, 0.2);
  }

  &.badge-user {
    background: rgba(200, 164, 92, 0.12);
    color: var(--ink-light);
    border: 1px solid rgba(200, 164, 92, 0.2);
  }
}

/* ===== 操作按钮 ===== */
.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
  border: 1px solid transparent;
  letter-spacing: 0.5px;

  .el-icon {
    transition: transform 0.25s ease;
  }

  &:hover:not(:disabled) .el-icon {
    transform: translateY(-1px);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  &.promote {
    background: rgba(200, 164, 92, 0.1);
    color: #8a6d2b;
    border-color: rgba(200, 164, 92, 0.3);

    &:hover:not(:disabled) {
      background: linear-gradient(135deg, var(--gold), #b8943e);
      color: #3A0A15;
      border-color: var(--gold);
      box-shadow: 0 4px 12px rgba(200, 164, 92, 0.3);
      .el-icon { color: #3A0A15; }
    }
  }

  &.demote {
    background: rgba(122, 26, 46, 0.06);
    color: var(--palace-red);
    border-color: rgba(122, 26, 46, 0.15);

    &:hover:not(:disabled) {
      background: linear-gradient(135deg, var(--palace-red-light), var(--palace-red));
      color: #fff;
      border-color: var(--palace-red);
      box-shadow: 0 4px 12px rgba(122, 26, 46, 0.2);
      .el-icon { color: #fff; }
    }
  }
}

.no-action {
  color: var(--ink-light);
  font-size: 13px;
}

/* ===== 空状态 ===== */
.empty-cell {
  text-align: center;
  padding: 60px 20px !important;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--ink-light);

  .el-icon { opacity: 0.4; }
  p { font-size: 14px; }
}

/* ===== 对话框 ===== */
.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  backdrop-filter: blur(4px);
}

.dialog-card {
  width: 420px;
  max-width: 90vw;
  background: var(--ivory);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(200, 164, 92, 0.2);
}

.dialog-header {
  padding: 32px 32px 20px;
  text-align: center;

  .dialog-icon {
    width: 56px;
    height: 56px;
    border-radius: 50%;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 16px;

    &.promote {
      background: linear-gradient(135deg, var(--gold), #b8943e);
      color: #3A0A15;
      box-shadow: 0 4px 16px rgba(200, 164, 92, 0.3);
    }

    &.demote {
      background: linear-gradient(135deg, var(--palace-red-light), var(--palace-red));
      color: #fff;
      box-shadow: 0 4px 16px rgba(122, 26, 46, 0.25);
    }
  }

  .dialog-title {
    font-family: var(--font-body);
    font-size: 20px;
    font-weight: 700;
    color: var(--ink);
    margin: 0 0 8px;
  }

  .dialog-desc {
    font-size: 14px;
    color: var(--ink-light);
    margin: 0;
    line-height: 1.5;
  }
}

.dialog-user-preview {
  margin: 0 32px 24px;
  padding: 16px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid var(--gold-light);
  display: flex;
  align-items: center;
  gap: 14px;

  .preview-avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background: var(--gold);
    color: var(--palace-red);
    display: flex;
    align-items: center;
    justify-content: center;
    font-family: var(--font-display);
    font-size: 18px;
    font-weight: 600;
    flex-shrink: 0;
  }

  .preview-info {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;

    .preview-name {
      font-weight: 600;
      font-size: 14px;
      color: var(--ink);
    }

    .preview-username {
      font-size: 12px;
      color: var(--ink-light);
    }

    .preview-arrow {
      color: var(--ink-light);
      opacity: 0.5;
    }
  }
}

.dialog-actions {
  padding: 0 32px 32px;
  display: flex;
  gap: 12px;

  .dialog-btn {
    flex: 1;
    padding: 12px;
    border-radius: 10px;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.25s ease;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;

    &:disabled { opacity: 0.6; cursor: not-allowed; }

    &.cancel {
      background: #fff;
      color: var(--ink-light);
      border: 1px solid var(--gold-light);

      &:hover:not(:disabled) {
        border-color: var(--gold);
        color: var(--ink);
      }
    }

    &.confirm {
      border: none;
      color: #fff;

      &.promote {
        background: linear-gradient(135deg, var(--gold), #b8943e);

        &:hover:not(:disabled) {
          box-shadow: 0 4px 16px rgba(200, 164, 92, 0.4);
          transform: translateY(-1px);
        }
      }

      &.demote {
        background: linear-gradient(135deg, var(--palace-red-light), var(--palace-red));

        &:hover:not(:disabled) {
          box-shadow: 0 4px 16px rgba(122, 26, 46, 0.3);
          transform: translateY(-1px);
        }
      }
    }

    .btn-loading {
      width: 16px;
      height: 16px;
      border: 2px solid rgba(255, 255, 255, 0.3);
      border-top-color: #fff;
      border-radius: 50%;
      animation: spin 0.7s linear infinite;
    }
  }
}

/* ===== 对话框动画 ===== */
.dialog-fade-enter-active {
  transition: opacity 0.3s ease;

  .dialog-card {
    transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.3s ease;
  }
}

.dialog-fade-leave-active {
  transition: opacity 0.2s ease;

  .dialog-card {
    transition: transform 0.2s ease, opacity 0.2s ease;
  }
}

.dialog-fade-enter-from {
  opacity: 0;

  .dialog-card {
    transform: scale(0.92) translateY(12px);
    opacity: 0;
  }
}

.dialog-fade-leave-to {
  opacity: 0;

  .dialog-card {
    transform: scale(0.96) translateY(8px);
    opacity: 0;
  }
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .user-management { padding: 20px 16px; }

  .page-header {
    flex-direction: column;

    .header-stats {
      width: 100%;
      justify-content: center;
    }
  }

  .user-table {
    thead th {
      &.col-time, &.col-username { display: none; }
    }
    tbody td {
      &.col-time, &.col-username { display: none; }
    }
  }
}
</style>
