import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const nickname = computed(() => user.value?.nickname || user.value?.username || '')
  const avatar = computed(() => user.value?.avatar || '')
  const userId = computed(() => user.value?.id)

  function setAuth(t, u) {
    token.value = t
    user.value = u
    localStorage.setItem('token', t)
    localStorage.setItem('user', JSON.stringify(u))
  }

  function updateUser(u) {
    user.value = u
    localStorage.setItem('user', JSON.stringify(u))
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  async function login(username, password) {
    const res = await request.post('/auth/login', { username, password })
    if (res.code === 200) {
      setAuth(res.data.token, res.data.user)
    }
    return res
  }

  async function register(username, password) {
    const res = await request.post('/auth/register', { username, password })
    if (res.code === 200) {
      setAuth(res.data.token, res.data.user)
    }
    return res
  }

  async function fetchProfile() {
    const res = await request.get('/user/profile')
    if (res.code === 200) {
      updateUser(res.data)
    }
    return res
  }

  async function updateProfile(data) {
    const res = await request.put('/user/profile', data)
    if (res.code === 200) {
      updateUser(res.data)
    }
    return res
  }

  return { token, user, isLoggedIn, isAdmin, nickname, avatar, userId,
           setAuth, updateUser, logout, login, register, fetchProfile, updateProfile }
})
