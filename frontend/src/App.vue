<template>
  <div class="app-shell" v-if="auth.isLoggedIn">
    <aside class="sidebar">
      <div class="sidebar-brand">
        <svg width="32" height="32" viewBox="0 0 32 32">
          <rect width="32" height="32" rx="6" fill="var(--gold)"/>
          <text x="16" y="23" text-anchor="middle" font-size="20" fill="var(--palace-red)" font-family="serif" font-weight="bold">洛</text>
        </svg>
        <span class="brand-text">神都洛阳</span>
      </div>

      <nav class="sidebar-nav">
        <div class="nav-item" :class="{ active: route.path === '/' }" @click="router.push('/')">
          <el-icon :size="20"><ChatDotRound /></el-icon>
          <span>智能问答</span>
        </div>
        <div v-if="auth.isAdmin" class="nav-item" :class="{ active: route.path === '/knowledge' }" @click="router.push('/knowledge')">
          <el-icon :size="20"><Collection /></el-icon>
          <span>知识库管理</span>
        </div>
        <div class="nav-item" :class="{ active: route.path === '/sessions' }" @click="router.push('/sessions')">
          <el-icon :size="20"><ChatLineSquare /></el-icon>
          <span>历史会话</span>
        </div>
        <div v-if="auth.isAdmin" class="nav-item" :class="{ active: route.path === '/admin/users' }" @click="router.push('/admin/users')">
          <el-icon :size="20"><User /></el-icon>
          <span>用户管理</span>
        </div>
        <div v-if="auth.isAdmin" class="nav-item" :class="{ active: route.path === '/admin/sessions' }" @click="router.push('/admin/sessions')">
          <el-icon :size="20"><ChatLineSquare /></el-icon>
          <span>会话管理</span>
        </div>
      </nav>

      <div class="sidebar-footer">
        <div class="user-info" @click="router.push('/profile')">
          <div class="user-avatar">
            <img v-if="auth.avatar" :src="auth.avatar" />
            <span v-else>{{ auth.nickname?.charAt(0) }}</span>
          </div>
          <div class="user-meta">
            <div class="user-name">{{ auth.nickname }}</div>
            <div class="user-role">{{ auth.roleName }}</div>
          </div>
        </div>
        <div class="gold-divider"></div>
        <div class="footer-text">十三朝古都</div>
        <div class="logout-btn" @click="handleLogout">退出登录</div>
      </div>
    </aside>

    <main class="main-content">
      <router-view />
    </main>
  </div>

  <router-view v-else />
</template>

<script setup>
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Collection, User, ChatLineSquare } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

function handleLogout() {
  auth.logout()
  ElMessage.success('已退出')
  router.push('/login')
}
</script>

<style scoped lang="scss">
.app-shell { display: flex; height: 100vh; overflow: hidden; }
.sidebar { width: 220px; min-width: 220px; background: linear-gradient(180deg, var(--palace-red-dark) 0%, var(--palace-red) 40%, var(--palace-red-dark) 100%); display: flex; flex-direction: column; position: relative; overflow: hidden;
  &::before { content: ''; position: absolute; inset: 0; background: radial-gradient(ellipse at 30% 20%, rgba(200,164,92,0.08) 0%, transparent 60%), radial-gradient(ellipse at 70% 80%, rgba(200,164,92,0.05) 0%, transparent 50%); pointer-events: none; }
}
.sidebar-brand { display: flex; align-items: center; gap: 10px; padding: 24px 20px 20px; border-bottom: 1px solid rgba(200,164,92,0.2); position: relative; z-index: 1;
  .brand-text { font-family: var(--font-display); font-size: 22px; color: var(--gold); letter-spacing: 4px; }
}
.sidebar-nav { flex: 1; padding: 16px 12px; display: flex; flex-direction: column; gap: 6px; position: relative; z-index: 1; }
.nav-item { display: flex; align-items: center; gap: 12px; padding: 12px 16px; border-radius: var(--radius-sm); cursor: pointer; color: rgba(255,255,255,0.8); font-family: var(--font-body); font-size: 15px; transition: all 0.3s ease; position: relative;
  .el-icon { color: var(--gold-light); transition: all 0.3s ease; }
  &:hover { background: rgba(200,164,92,0.15); color: #fff; box-shadow: 0 0 12px rgba(200,164,92,0.15); .el-icon { color: var(--gold); } }
  &.active { background: rgba(200,164,92,0.2); color: var(--gold); box-shadow: 0 0 16px rgba(200,164,92,0.2); .el-icon { color: var(--gold); } &::before { content: ''; position: absolute; left: 0; top: 50%; transform: translateY(-50%); width: 3px; height: 20px; background: var(--gold); border-radius: 0 2px 2px 0; } }
}
.sidebar-footer { padding: 16px 20px; position: relative; z-index: 1; }
.user-info { display: flex; align-items: center; gap: 10px; padding: 8px; border-radius: 8px; cursor: pointer; transition: background 0.2s; margin-bottom: 8px;
  &:hover { background: rgba(200,164,92,0.12); }
  .user-avatar { width: 36px; height: 36px; border-radius: 50%; overflow: hidden; flex-shrink: 0; background: var(--gold); color: var(--palace-red); display: flex; align-items: center; justify-content: center; font-family: var(--font-display); font-size: 18px;
    img { width: 100%; height: 100%; object-fit: cover; } }
  .user-meta {
    .user-name { font-size: 14px; color: #fff; font-family: var(--font-body); }
    .user-role { font-size: 11px; color: rgba(200,164,92,0.6); } }
}
.gold-divider { height: 1px; background: linear-gradient(90deg, transparent, var(--gold), transparent); margin-bottom: 10px; opacity: 0.3; }
.footer-text { font-family: var(--font-display); font-size: 14px; color: var(--gold); letter-spacing: 2px; margin-bottom: 8px; }
.logout-btn { font-size: 12px; color: rgba(255,255,255,0.4); cursor: pointer; transition: color 0.2s; &:hover { color: rgba(255,255,255,0.8); } }
.main-content { flex: 1; overflow-y: auto; background: var(--ivory); position: relative; }
</style>
