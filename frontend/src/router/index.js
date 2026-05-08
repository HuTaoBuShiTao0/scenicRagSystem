import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginPage.vue'),
    meta: { title: '登录', guest: true }
  },
  {
    path: '/',
    name: 'Chat',
    component: () => import('@/views/Chat.vue'),
    meta: { title: '智能问答', auth: true }
  },
  {
    path: '/knowledge',
    name: 'Knowledge',
    component: () => import('@/views/Knowledge.vue'),
    meta: { title: '知识库管理', auth: true, admin: true }
  },
  {
    path: '/sessions',
    name: 'UserSessions',
    component: () => import('@/views/UserSessions.vue'),
    meta: { title: '历史会话', auth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/ProfilePage.vue'),
    meta: { title: '个人信息', auth: true }
  },
  {
    path: '/admin/users',
    name: 'UserManagement',
    component: () => import('@/views/UserManagement.vue'),
    meta: { title: '用户管理', auth: true, admin: true }
  },
  {
    path: '/admin/sessions',
    name: 'AdminSessions',
    component: () => import('@/views/AdminSessions.vue'),
    meta: { title: '会话管理', auth: true, admin: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title || '洛阳景区智能问答'

  const auth = useAuthStore()

  if (to.meta.auth && !auth.isLoggedIn) {
    return next('/login')
  }

  if (to.meta.admin && !auth.isAdmin) {
    return next('/')
  }

  if (to.meta.guest && auth.isLoggedIn) {
    return next('/')
  }

  next()
})

export default router
