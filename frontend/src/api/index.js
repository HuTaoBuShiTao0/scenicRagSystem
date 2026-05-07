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
