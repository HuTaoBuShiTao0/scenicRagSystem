import request from '@/utils/request'

// ============= 智能问答 =============

/**
 * 发送消息（支持SSE流式，兼容带/不带空格的 data: 格式）
 */
export function sendChatMessage(message, history = [], onMessage, onDone, onError, sessionId) {
  const controller = new AbortController()
  const token = localStorage.getItem('token')

  fetch('/api/chat/send', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...(token ? { 'Authorization': 'Bearer ' + token } : {}) },
    body: JSON.stringify({ message, history, sessionId }),
    signal: controller.signal
  }).then(async response => {
    if (!response.ok) throw new Error('Network error')

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) { onDone?.(); break }

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        const trimmed = line.trim()
        // 处理 data: 开头（可能有空格也可能没有）
        if (trimmed.startsWith('data:')) {
          let jsonStr = trimmed.slice(5).trim()
          if (jsonStr === '[DONE]') { onDone?.(); return }
          if (!jsonStr) continue
          try {
            const data = JSON.parse(jsonStr)
            onMessage?.(data)
          } catch (e) {
            // skip partial/invalid JSON
          }
        }
        // 处理 event: done 标记
        if (trimmed === 'event:done' || trimmed === 'event: complete') {
          // 等待下一条 data: 来完成
        }
      }
    }
  }).catch(err => {
    if (err.name !== 'AbortError') {
      onError?.(err)
    }
  })

  return controller
}

// ============= 知识库管理 =============

/**
 * 获取指定知识库数据列表
 */
export function getKnowledgeList(type) {
  return request.get(`/knowledge/${type}`)
}

/**
 * 添加知识库数据
 */
export function addKnowledge(type, data) {
  return request.post(`/knowledge/${type}`, data)
}

/**
 * 更新知识库数据
 */
export function updateKnowledge(type, id, data) {
  return request.put(`/knowledge/${type}/${id}`, data)
}

/**
 * 删除知识库数据
 */
export function deleteKnowledge(type, id) {
  return request.delete(`/knowledge/${type}/${id}`)
}

/**
 * 同步数据到向量库
 */
export function syncKnowledgeVector() {
  return request.post('/knowledge/sync-vector')
}

// ============= 会话管理 =============

export function getSessions() {
  return request.get('/sessions')
}

/**
 * 分页查询当前用户的会话（支持搜索）
 */
export function getSessionsPaged(params) {
  return request.get('/sessions/paged', { params })
}

export function createSession() {
  return request.post('/sessions')
}

export function deleteSession(id) {
  return request.delete(`/sessions/${id}`)
}

export function getSessionMessages(id) {
  return request.get(`/sessions/${id}/messages`)
}

// ============= 用户相关 =============

export function getUserProfile() {
  return request.get('/user/profile')
}

export function updateUserProfile(data) {
  return request.put('/user/profile', data)
}

// ============= 管理员：会话管理 =============

/**
 * 分页查询所有会话（管理员）
 */
export function getAdminSessionList(params) {
  return request.get('/admin/sessions', { params })
}

/**
 * 获取会话消息详情（管理员 - 只读）
 */
export function getAdminSessionMessages(sessionId) {
  return request.get(`/admin/sessions/${sessionId}/messages`)
}

// ============= 管理员：用户管理 =============

/**
 * 获取所有用户列表（管理员）
 */
export function getUserList() {
  return request.get('/admin/users')
}

/**
 * 更新用户角色（管理员）
 */
export function updateUserRole(id, role) {
  return request.put(`/admin/users/${id}/role`, { role })
}

// ============= 提示词管理 =============

/**
 * 获取所有提示词
 */
export function getPromptList() {
  return request.get('/admin/prompts')
}

/**
 * 根据ID获取提示词
 */
export function getPromptById(id) {
  return request.get(`/admin/prompts/${id}`)
}

/**
 * 根据类型获取提示词
 */
export function getPromptByType(type) {
  return request.get(`/admin/prompts/type/${type}`)
}

/**
 * 创建提示词
 */
export function createPrompt(data) {
  return request.post('/admin/prompts', data)
}

/**
 * 更新提示词
 */
export function updatePrompt(id, data) {
  return request.put(`/admin/prompts/${id}`, data)
}

/**
 * 快捷更新提示词内容（仅修改 content，清缓存）
 */
export function updatePromptContent(id, content) {
  return request.put(`/admin/prompts/${id}/content`, { content })
}

/**
 * 删除提示词
 */
export function deletePrompt(id) {
  return request.delete(`/admin/prompts/${id}`)
}

// ============= 天气查询 =============

/**
 * 获取今日天气（用于前端组件展示）
 */
export function getTodayWeather(location = '洛阳') {
  return request.get('/weather/today', { params: { location } })
}

// ============= 文件上传 =============

/**
 * 上传图片
 */
export function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
