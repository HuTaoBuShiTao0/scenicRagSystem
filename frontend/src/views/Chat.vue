<template>
  <div class="chat-container">
    <!-- 顶部操作栏（有消息时显示） -->
    <div v-if="messages.length > 0" class="chat-topbar">
      <button class="topbar-btn" @click="router.push('/sessions')">
        <el-icon :size="14"><ChatLineSquare /></el-icon>
        <span>历史记录</span>
      </button>
      <span class="topbar-title">神都洛阳</span>
      <div class="topbar-right">
        <div class="weather-mini" v-if="weatherNow" @click="sendMessage(weatherQueryTip)">
          <span class="wm-temp">{{ weatherNow.tempMax }}°</span>
          <span class="wm-text">{{ weatherNow.textDay }}</span>
          <span class="wm-dot"></span>
        </div>
        <button class="topbar-btn primary" @click="handleNewSession">
          <el-icon :size="14"><Plus /></el-icon>
          <span>新建对话</span>
        </button>
      </div>
    </div>
    <!-- 欢迎页天气（无消息时显示） -->
    <div v-if="messages.length === 0 && weatherNow" class="welcome-weather">
      <div class="ww-inner" @click="sendMessage(weatherQueryTip)">
        <span class="ww-temp">{{ weatherNow.tempMax }}°</span>
        <div class="ww-info">
          <span class="ww-city">洛阳</span>
          <span class="ww-desc">{{ weatherNow.textDay }}</span>
        </div>
        <span class="ww-wind">{{ weatherNow.windDirDay }}{{ weatherNow.windScaleDay }}级</span>
      </div>
    </div>

    <!-- 可滚动的中间区域：欢迎页 + 消息 -->
    <div class="scroll-area" ref="scrollAreaRef">
      <!-- 欢迎首页（始终存在，有消息时变为紧凑模式） -->
      <div class="welcome-section" :class="{ 'welcome-compact': messages.length > 0 }">
        <div class="petals" aria-hidden="true">
          <span v-for="i in 8" :key="i" class="petal" :style="petalStyle(i)"></span>
        </div>
        <div class="welcome-inner">
          <div class="welcome-header">
            <h1 class="welcome-title">神都洛阳</h1>
            <p class="welcome-subtitle">AI 智能问答助手 · 探索千年古都之美</p>
          </div>
          <div class="welcome-actions">
            <div class="quick-actions">
              <div v-for="action in quickActions" :key="action.id" class="action-card" @click="handleQuickAction(action)">
                <div class="card-icon-wrap">
                  <el-icon :size="28">
                    <ChatDotRound v-if="action.id === 1" /><ForkSpoon v-else-if="action.id === 2" />
                    <House v-else-if="action.id === 3" /><Present v-else-if="action.id === 4" />
                    <Sunny v-else-if="action.id === 5" /><Service v-else />
                  </el-icon>
                </div>
                <div class="card-info">
                  <div class="card-title">{{ action.title }}</div>
                  <div class="card-desc">{{ action.desc }}</div>
                </div>
              </div>
            </div>
            <div class="hot-section">
              <div class="hot-label">热问</div>
              <div class="hot-list">
                <div v-for="question in hotQuestions" :key="question.id" class="hot-item" @click="sendMessage(question.text)">
                  <span class="hot-text">{{ question.text }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 消息列表 -->
      <div v-if="messages.length > 0" class="messages-section" ref="messagesRef">
        <div class="messages-divider">
          <span class="divider-line"></span>
          <span class="divider-text">对话记录</span>
          <span class="divider-line"></span>
        </div>
        <div
          v-for="(message, index) in messages"
          :key="index"
          :class="['message-item', message.role]"
        >
          <div class="message-avatar">
            <div v-if="message.role === 'user'" class="user-avatar">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="8" r="4" fill="#fff" opacity="0.9"/>
                <path d="M4 20c2-4 6-5 8-5s6 1 8 5" stroke="#fff" stroke-width="2" fill="none" stroke-linecap="round"/>
              </svg>
            </div>
            <div v-else class="ai-avatar">
              <svg width="24" height="24" viewBox="0 0 40 40">
                <rect width="40" height="40" rx="8" fill="var(--palace-red)"/>
                <text x="20" y="27" text-anchor="middle" font-size="22" fill="var(--gold)" font-family="serif" font-weight="bold">洛</text>
              </svg>
            </div>
          </div>
          <div class="message-content">
            <div class="message-sender">
              {{ message.role === 'user' ? '您' : '洛阳助手' }}
            </div>
            <div class="message-text" :class="message.role">
              <template v-if="message.content">
                <span v-html="renderMessage(message.content)"></span>
              </template>
              <div v-else-if="isTyping && message.role === 'assistant'" class="typing-dots">
                <span></span><span></span><span></span>
              </div>
            </div>

            <!-- 智能卡片（多条，水平滚动） -->
            <div v-if="message.cards?.length" class="cards-carousel">
              <div
                v-for="(card, ci) in message.cards"
                :key="ci"
                class="carousel-item"
              >
                <!-- 景点讲解卡片 -->
                <div v-if="card.type === 'attraction'" class="info-card">
                  <div class="card-badge">🏛️</div>
                  <h4 class="card-name">{{ card.data.name }}</h4>
                  <div class="card-tags" v-if="card.data.tags?.length">
                    <span v-for="tag in card.data.tags" :key="tag" class="tag-seal">{{ tag }}</span>
                  </div>
                  <p class="card-desc">{{ card.data.briefIntro || card.data.description }}</p>
                  <div class="card-meta">
                    <span v-if="card.data.address">📍 {{ card.data.address }}</span>
                  </div>
                </div>

                <!-- 美食导购卡片 -->
                <div v-else-if="card.type === 'food'" class="info-card">
                  <div class="card-badge">🍜</div>
                  <h4 class="card-name">{{ card.data.name }}</h4>
                  <div class="card-tags" v-if="card.data.tags?.length">
                    <span v-for="tag in card.data.tags" :key="tag" class="tag-seal">{{ tag }}</span>
                  </div>
                  <div class="card-meta">
                    <span v-if="card.data.address">📍 {{ card.data.address }}</span>
                  </div>
                </div>

                <!-- 酒店导购卡片 -->
                <div v-else-if="card.type === 'hotel'" class="info-card">
                  <div class="card-badge">🏨</div>
                  <h4 class="card-name">{{ card.data.name }}</h4>
                  <div class="card-tags" v-if="card.data.level">
                    <span class="tag-seal">{{ card.data.level }}</span>
                  </div>
                  <div class="card-meta">
                    <span v-if="card.data.price">💰 {{ card.data.price }}</span>
                    <span v-if="card.data.address">📍 {{ card.data.address }}</span>
                  </div>
                </div>

                <!-- 特产文创卡片 -->
                <div v-else-if="card.type === 'product'" class="info-card">
                  <div class="card-badge">🎁</div>
                  <h4 class="card-name">{{ card.data.name }}</h4>
                  <div class="card-tags" v-if="card.data.category">
                    <span class="tag-seal">{{ card.data.category }}</span>
                  </div>
                  <p class="card-desc">{{ card.data.description }}</p>
                  <div class="card-meta">
                    <span v-if="card.data.price">💰 {{ card.data.price }}</span>
                  </div>
                </div>

                <!-- 天气查询卡片 -->
                <div v-else-if="card.type === 'weather'" class="info-card weather">
                  <div class="card-badge">🌤️</div>
                  <h4 class="card-name">{{ card.data.location }} 天气</h4>
                  <div class="weather-row">
                    <span class="wh-temp">{{ card.data.tempMax }}°</span>
                    <span class="wh-text">{{ card.data.textDay }}</span>
                  </div>
                  <div class="weather-grid">
                    <span>💧 {{ card.data.humidity }}%</span>
                    <span>🌬️ {{ card.data.windDirDay }}{{ card.data.windScaleDay }}级</span>
                  </div>
                </div>

                <!-- 门票购买卡片 -->
                <div v-else-if="card.type === 'ticket'" class="info-card">
                  <div class="card-badge">🎫</div>
                  <h4 class="card-name">{{ card.data.name }}</h4>
                  <div class="card-tags" v-if="card.data.tags?.length">
                    <span v-for="tag in card.data.tags" :key="tag" class="tag-seal">{{ tag }}</span>
                  </div>
                  <div class="card-meta">
                    <span v-if="card.data.price">💰 {{ card.data.price }}</span>
                    <span v-if="card.data.location">📍 {{ card.data.location }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="chat-input">
      <div class="input-container">
        <div class="input-row">
          <input
            v-model="inputMessage"
            class="message-input"
            placeholder="输入您的问题..."
            :disabled="isTyping"
            @keyup.enter="handleSend"
          />
          <button
            class="send-btn"
            :disabled="isTyping || !inputMessage.trim()"
            @click="handleSend"
          >
            发送
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ChatDotRound, ForkSpoon, House, Present, Sunny, Service, Plus, ChatLineSquare
} from '@element-plus/icons-vue'
import { sendChatMessage, createSession, getSessionMessages, getTodayWeather } from '@/api'

const router = useRouter()
const route = useRoute()
const inputMessage = ref('')
const messages = ref([])
const isTyping = ref(false)
const scrollAreaRef = ref(null)
const messagesRef = ref(null)
const currentSessionId = ref(null)
const weatherNow = ref(null)
const weatherLoaded = ref(false)
const weatherQueryTip = computed(() => {
  return weatherNow.value ? `洛阳今天天气怎么样` : ''
})

// 快捷操作
const quickActions = [
  { id: 1, icon: '🏯', title: '景点推荐', desc: '探索洛阳著名景点', query: '推荐几个洛阳必去的景点' },
  { id: 2, icon: '🍜', title: '美食指南', desc: '品尝洛阳特色美食', query: '洛阳有什么特色美食推荐' },
  { id: 3, icon: '🏨', title: '酒店住宿', desc: '舒适住宿选择', query: '推荐洛阳的酒店' },
  { id: 4, icon: '🎁', title: '文创产品', desc: '特色文创纪念品', query: '洛阳有什么文创产品推荐' },
  { id: 5, icon: '🌤️', title: '天气查询', desc: '实时天气信息', query: '洛阳今天天气怎么样' },
  { id: 6, icon: '💬', title: '客服咨询', desc: '在线客服帮助', query: '联系客服' }
]

// 热门问题
const hotQuestions = [
  { id: 1, text: '龙门石窟有什么景点？' },
  { id: 2, text: '洛阳有哪些特色美食推荐？' },
  { id: 3, text: '洛阳牡丹花会什么时候？' },
  { id: 4, text: '老君山的门票多少钱？' }
]

// 创建新会话
async function handleNewSession() {
  try {
    const res = await createSession()
    if (res.code === 200) {
      currentSessionId.value = res.data.id
      messages.value = []
    }
  } catch (e) { ElMessage.error('创建会话失败') }
}

// 加载指定会话的消息
async function loadSessionMessages(sessionId) {
  currentSessionId.value = sessionId
  try {
    const res = await getSessionMessages(sessionId)
    if (res.code === 200) {
      messages.value = (res.data || []).map(m => ({
        role: m.role,
        content: m.content,
        cards: m.cards || null,
        card: m.card || null
      }))
    }
  } catch (e) { /* ignore */ }
}

// 处理快捷操作
const handleQuickAction = (action) => {
  sendMessage(action.query)
}

// 发送消息
const sendMessage = async (text) => {
  const message = text || inputMessage.value.trim()
  if (!message || isTyping.value) return

  const history = messages.value.slice(-10).map(m => ({
    role: m.role === 'user' ? 'user' : 'assistant',
    content: m.content
  }))

  messages.value.push({ role: 'user', content: message })
  inputMessage.value = ''
  isTyping.value = true

  await nextTick()
  scrollToBottom()

  // 创建AI消息占位，通过数组索引读写保证Vue响应式
  messages.value.push({ role: 'assistant', content: '', cards: null, _full: '' })
  const msgIdx = messages.value.length - 1

  let hasCard = false
  let typingTimer = null

  // 打字机: 从 _full 逐步复制到 content（操作 messages.value[msgIdx] 保证响应式）
  const startTyping = () => {
    if (typingTimer) return
    typingTimer = setInterval(() => {
      const m = messages.value[msgIdx]
      if (!m) { clearInterval(typingTimer); typingTimer = null; return }
      const full = m._full || ''
      const shown = m.content || ''
      if (shown.length < full.length) {
        const step = Math.min(full.length - shown.length, 3)
        m.content = full.substring(0, shown.length + step)
        scrollToBottom()
      } else {
        clearInterval(typingTimer)
        typingTimer = null
        isTyping.value = false
        scrollToBottom()
      }
    }, 40)
  }

  try {
    await sendChatMessage(
      message,
      history,
      (data) => {
        // 同步 sessionId（新建会话时后端返回）
        if (data.sessionId) {
          currentSessionId.value = data.sessionId
        }
        const m = messages.value[msgIdx]
        if (!m) return
        if (data.content) {
          m._full += data.content
          if (!typingTimer) startTyping()
        }
        if (data.cards && !hasCard) {
          m.cards = data.cards
          hasCard = true
        }
      },
      () => {
        const m = messages.value[msgIdx]
        if (!typingTimer && m) {
          m.content = m._full || m.content
          isTyping.value = false
          scrollToBottom()
        }
        // 消息发送完成
      },
      (err) => {
        const m = messages.value[msgIdx]
        if (typingTimer) { clearInterval(typingTimer); typingTimer = null }
        if (m) m.content = m._full || m.content || '抱歉，AI服务暂时不可用，请稍后再试。'
        ElMessage.error('请求失败: ' + (err.message || '网络错误'))
        isTyping.value = false
      },
      currentSessionId.value
    )
  } catch (e) {
    const m = messages.value[msgIdx]
    if (typingTimer) { clearInterval(typingTimer); typingTimer = null }
    if (m) m.content = m._full || m.content || '抱歉，请求失败，请重试。'
    ElMessage.error('发送失败')
    isTyping.value = false
  }
}

// 处理发送
const handleSend = () => {
  sendMessage()
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (scrollAreaRef.value) {
      scrollAreaRef.value.scrollTop = scrollAreaRef.value.scrollHeight
    }
  })
}

// 渲染消息
import { marked } from 'marked'

// 配置 marked 选项
marked.setOptions({
  breaks: true,      // 将 \n 转换为 <br>
  gfm: true,         // 启用 GitHub 风格 Markdown
})

const renderMessage = (content) => {
  if (!content) return ''
  // 将内容渲染为 HTML，清除潜在的 XSS 风险
  const html = marked.parse(content)
  // 移除可能的代码块包裹标记
  return html
}

// 花瓣样式
const petalStyle = (i) => {
  const colors = ['#C8A45C', '#E8D5A3', '#D4B87C', '#F0E0C0', '#C8A45C', '#E8D5A3', '#D4B87C', '#F0E0C0']
  const leftPositions = [5, 18, 35, 55, 72, 85, 45, 65]
  const durations = [8, 10, 12, 9, 11, 8.5, 13, 10.5]
  const delays = [0, 0.5, 1.5, 0.3, 2, 1, 0.8, 2.5]
  const sizes = [12, 10, 14, 11, 9, 13, 10, 12]

  return {
    left: `${leftPositions[i-1]}%`,
    width: `${sizes[i-1]}px`,
    height: `${sizes[i-1]}px`,
    background: colors[i-1],
    animationDuration: `${durations[i-1]}s`,
    animationDelay: `${delays[i-1]}s`
  }
}

// 监听消息变化
watch(messages, () => {
  scrollToBottom()
}, { deep: true })

// 加载今日天气
async function loadWeather() {
  try {
    const res = await getTodayWeather('洛阳')
    if (res.code === 200 && res.data) {
      weatherNow.value = res.data
    }
  } catch (e) {
    // 静默失败，不影响页面
  } finally {
    weatherLoaded.value = true
  }
}

onMounted(async () => {
  loadWeather()
  // 从 URL 参数加载指定会话
  const sessionId = route.query.session
  if (sessionId) {
    await loadSessionMessages(Number(sessionId))
  }
})
</script>

<style scoped lang="scss">
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  position: relative;
  background: var(--ivory);
}

// ============ 顶部操作栏 ============
.chat-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px;
  background: #fff;
  border-bottom: 1px solid var(--gold-light);
  flex-shrink: 0;
  z-index: 10;

  .topbar-title {
    font-family: var(--font-body);
    font-size: 14px;
    font-weight: 600;
    color: var(--ink);
    letter-spacing: 1px;
  }

  .topbar-btn {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 6px 14px;
    border-radius: 8px;
    font-family: var(--font-body);
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
    border: 1px solid var(--gold-light);
    background: #fff;
    color: var(--ink-light);

    &:hover {
      border-color: var(--gold);
      color: var(--palace-red);
      box-shadow: 0 2px 8px rgba(200, 164, 92, 0.12);
    }

    &.primary {
      background: linear-gradient(135deg, var(--palace-red-light), var(--palace-red));
      color: #fff;
      border-color: transparent;

      &:hover {
        box-shadow: 0 4px 12px rgba(122, 26, 46, 0.25);
      }
    }
  }
}

// 顶部栏右侧（天气小笺 + 操作按钮）
.topbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

// 天气小笺 — 顶栏紧凑版
.weather-mini {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px 4px 10px;
  border-radius: 20px;
  background: linear-gradient(135deg, var(--silk-gold) 0%, rgba(200,164,92,0.08) 100%);
  border: 1px solid rgba(200,164,92,0.2);
  cursor: pointer;
  transition: all 0.3s ease;
  user-select: none;

  &:hover {
    border-color: var(--gold);
    box-shadow: 0 2px 8px rgba(200,164,92,0.15);
  }

  .wm-temp {
    font-size: 15px;
    font-weight: 700;
    color: var(--palace-red);
    letter-spacing: -0.5px;
  }

  .wm-text {
    font-size: 12px;
    color: var(--ink-light);
  }

  .wm-dot {
    width: 4px;
    height: 4px;
    border-radius: 50%;
    background: var(--gold);
    opacity: 0.4;
  }
}

// 欢迎页天气 — 独立展示版
.welcome-weather {
  padding: 10px 20px 0;

  .ww-inner {
    display: inline-flex;
    align-items: center;
    gap: 10px;
    padding: 6px 16px 6px 14px;
    border-radius: 20px;
    background: linear-gradient(135deg, rgba(200,164,92,0.08) 0%, rgba(200,164,92,0.03) 100%);
    border: 1px solid rgba(200,164,92,0.12);
    cursor: pointer;
    transition: all 0.3s ease;

    &:hover {
      border-color: var(--gold);
      background: linear-gradient(135deg, rgba(200,164,92,0.12) 0%, rgba(200,164,92,0.05) 100%);
    }
  }

  .ww-temp {
    font-size: 20px;
    font-weight: 700;
    color: var(--palace-red);
    letter-spacing: -1px;
  }

  .ww-info {
    display: flex;
    flex-direction: column;
    line-height: 1.3;

    .ww-city {
      font-size: 12px;
      font-weight: 500;
      color: var(--ink);
    }

    .ww-desc {
      font-size: 11px;
      color: var(--ink-light);
    }
  }

  .ww-wind {
    font-size: 11px;
    color: var(--ink-light);
    opacity: 0.6;
  }
}

// ============ 可滚动区域 ============
.scroll-area {
  flex: 1;
  overflow-y: auto;

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: var(--gold-light); border-radius: 2px; }
}

// ============ 欢迎首页 ============
.welcome-section {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  position: relative;
  overflow: hidden;
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);

  // 紧凑模式（已有消息时）
  &.welcome-compact {
    padding: 24px 24px 16px;

    .welcome-title {
      font-size: 28px;
      letter-spacing: 4px;
      margin-bottom: 4px;
    }

    .welcome-subtitle {
      font-size: 13px;
      margin-bottom: 0;
    }

    .petals { opacity: 0.2; }
  }
}

.welcome-inner {
  max-width: 900px;
  width: 100%;
  position: relative;
  z-index: 1;
}

.welcome-header {
  text-align: center;
  margin-bottom: 40px;

  .welcome-title {
    font-family: var(--font-display);
    font-size: 56px;
    color: var(--palace-red);
    letter-spacing: 8px;
    margin-bottom: 12px;
    text-shadow: 2px 2px 4px rgba(122, 26, 46, 0.1);
    animation: fadeIn 0.8s ease-out;
  }

  .welcome-subtitle {
    font-family: var(--font-body);
    font-size: 16px;
    color: var(--ink-light);
    letter-spacing: 2px;
    animation: fadeIn 0.8s ease-out 0.2s both;
  }
}

// 快捷操作卡片
.quick-actions {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 36px;
}

.action-card {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(8px);
  border: 1px solid var(--gold-light);
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  gap: 14px;
  position: relative;
  overflow: hidden;

  // 拐角装饰
  &::before {
    content: '';
    position: absolute;
    top: 6px;
    left: 6px;
    width: 12px;
    height: 12px;
    border-top: 2px solid var(--gold);
    border-left: 2px solid var(--gold);
    opacity: 0.4;
    transition: opacity 0.3s ease;
  }

  &::after {
    content: '';
    position: absolute;
    bottom: 6px;
    right: 6px;
    width: 12px;
    height: 12px;
    border-bottom: 2px solid var(--gold);
    border-right: 2px solid var(--gold);
    opacity: 0.4;
    transition: opacity 0.3s ease;
  }

  &:hover {
    transform: translateY(-6px);
    border-color: var(--gold);
    box-shadow: 0 8px 28px rgba(200, 164, 92, 0.25);

    &::before, &::after {
      opacity: 1;
    }
  }

  .card-icon-wrap {
    width: 48px;
    height: 48px;
    border-radius: 10px;
    background: linear-gradient(135deg, var(--palace-red-light), var(--palace-red));
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    .el-icon {
      color: var(--gold-light);
    }
  }

  .card-info {
    flex: 1;
    min-width: 0;
  }

  .card-title {
    font-family: var(--font-body);
    font-size: 16px;
    font-weight: 600;
    color: var(--ink);
    margin-bottom: 4px;
  }

  .card-desc {
    font-size: 13px;
    color: var(--ink-light);
  }
}

// 热门问题
.hot-section {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;

  .hot-label {
    font-family: var(--font-display);
    font-size: 18px;
    color: var(--gold-dark);
    writing-mode: vertical-lr;
    letter-spacing: 2px;
    padding: 4px 2px;
    background: rgba(200, 164, 92, 0.1);
    border-radius: 4px;
  }

  .hot-list {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
  }

  .hot-item {
    padding: 8px 18px;
    border: 1px solid var(--gold);
    border-radius: 20px;
    cursor: pointer;
    transition: all 0.3s ease;
    background: rgba(255, 255, 255, 0.8);

    .hot-text {
      font-family: var(--font-body);
      font-size: 13px;
      color: var(--ink);
      white-space: nowrap;
    }

    &:hover {
      background: var(--palace-red);
      border-color: var(--palace-red);

      .hot-text {
        color: #fff;
      }
    }
  }
}

// 飘落花瓣
.petals {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.petal {
  position: absolute;
  top: -20px;
  border-radius: 50%;
  opacity: 0.5;
  animation: petalFall linear infinite;

  @keyframes petalFall {
    0% {
      transform: translateY(-20px) rotate(0deg) scale(1);
      opacity: 0.6;
    }
    100% {
      transform: translateY(calc(100vh + 20px)) rotate(720deg) scale(0.5);
      opacity: 0;
    }
  }
}

// ============ 对话区域 ============
// ============ 消息区域 ============
.messages-section {
  max-width: 860px;
  margin: 0 auto;
  padding: 8px 24px 24px;
}

.messages-divider {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;

  .divider-line {
    flex: 1;
    height: 1px;
    background: linear-gradient(90deg, transparent, var(--gold-light), transparent);
  }

  .divider-text {
    font-size: 11px;
    color: var(--ink-light);
    letter-spacing: 2px;
    white-space: nowrap;
    opacity: 0.6;
  }
}

.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  animation: fadeIn 0.4s ease-out;

  &.user {
    flex-direction: row-reverse;

    .message-content {
      align-items: flex-end;
    }
  }
}

.message-avatar {
  flex-shrink: 0;

  .user-avatar,
  .ai-avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .user-avatar {
    background: var(--gold);
    box-shadow: 0 2px 8px rgba(200, 164, 92, 0.3);
  }

  .ai-avatar {
    background: var(--palace-red);
    box-shadow: 0 2px 8px rgba(122, 26, 46, 0.3);
    overflow: hidden;
  }
}

.message-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-width: 70%;
}

.message-sender {
  font-size: 12px;
  color: var(--ink-light);
  font-weight: 500;
  padding: 0 4px;
}

.message-text {
  padding: 14px 18px;
  border-radius: 16px;
  line-height: 1.8;
  font-size: 15px;
  word-wrap: break-word;

  &.user {
    background: linear-gradient(135deg, var(--palace-red), var(--palace-red-dark));
    color: #fff;
    border-radius: 16px 16px 4px 16px;
  }

  &.assistant {
    background: var(--silk-gold);
    color: var(--ink);
    border-left: 3px solid var(--gold);
    border-radius: 16px 16px 16px 4px;
  }
}

// Markdown 渲染样式（助手消息内）
.message-text.assistant {
  // 段落
  p { margin-bottom: 8px; &:last-child { margin-bottom: 0; } }

  // 加粗
  strong { color: var(--palace-red); font-weight: 600; }

  // 列表
  ul, ol {
    padding-left: 20px;
    margin: 6px 0;
    li { margin-bottom: 4px; }
  }

  // 行内代码
  code {
    background: rgba(200,164,92,0.12);
    color: var(--palace-red);
    padding: 1px 6px;
    border-radius: 4px;
    font-size: 13px;
    font-family: 'Courier New', monospace;
  }

  // 标题
  h1, h2, h3, h4 {
    font-family: var(--font-display);
    color: var(--palace-red);
    margin: 10px 0 6px;
    letter-spacing: 1px;
  }
  h1 { font-size: 20px; }
  h2 { font-size: 18px; }
  h3 { font-size: 16px; }

  // 链接
  a {
    color: var(--palace-red);
    text-decoration: underline;
    text-underline-offset: 2px;
    text-decoration-color: var(--gold);
  }

  // 分隔线
  hr {
    border: none;
    height: 1px;
    background: linear-gradient(90deg, transparent, var(--gold), transparent);
    margin: 12px 0;
    opacity: 0.3;
  }

  // 引用
  blockquote {
    border-left: 3px solid var(--gold);
    padding: 6px 12px;
    margin: 8px 0;
    background: rgba(200,164,92,0.06);
    border-radius: 0 6px 6px 0;
    color: var(--ink-light);
    font-style: italic;
  }

  // 代码块
  pre {
    background: rgba(0,0,0,0.04);
    border: 1px solid rgba(200,164,92,0.15);
    border-radius: 8px;
    padding: 12px 14px;
    margin: 8px 0;
    overflow-x: auto;

    code {
      background: transparent;
      padding: 0;
      font-size: 13px;
      color: var(--ink);
    }
  }

  // 表格
  table {
    border-collapse: collapse;
    width: 100%;
    margin: 8px 0;
    font-size: 14px;

    th, td {
      border: 1px solid var(--gold-light);
      padding: 6px 10px;
      text-align: left;
    }

    th {
      background: var(--palace-red);
      color: #fff;
      font-weight: 500;
    }

    tr:nth-child(even) td {
      background: rgba(245,240,233,0.5);
    }
  }
}

// 消息卡片 - 水平滚动列表
.cards-carousel {
  display: grid;
  grid-auto-columns: 220px;
  grid-auto-flow: column;
  gap: 10px;
  overflow-x: auto;
  overflow-y: hidden;
  padding: 8px 0 12px;
  scroll-snap-type: x mandatory;
  -webkit-overflow-scrolling: touch;

  &::-webkit-scrollbar { height: 6px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb {
    background: var(--gold);
    border-radius: 3px;
  }
}

.carousel-item {
  display: flex;
  scroll-snap-align: start;
  min-width: 0;
}

.info-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--gold-light);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  padding: 14px 14px 16px;
  position: relative;
  min-height: 120px;
  display: flex;
  flex-direction: column;
  gap: 6px;

  &::before {
    content: '';
    position: absolute;
    top: 0; left: 0; right: 0;
    height: 3px;
    background: linear-gradient(90deg, var(--palace-red), var(--gold), var(--palace-red));
    opacity: 0.6;
  }

  // 图标角标
  .card-badge {
    font-size: 18px;
    line-height: 1;
    margin-bottom: 2px;
  }

  .card-name {
    font-family: var(--font-body);
    font-size: 15px;
    font-weight: 600;
    color: var(--ink);
    line-height: 1.3;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .card-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
  }

  .tag-seal {
    display: inline-block;
    padding: 1px 8px;
    border: 1px solid var(--gold);
    color: var(--palace-red);
    border-radius: 3px;
    font-size: 11px;
    background: #fff;
  }

  .card-desc {
    font-size: 12px;
    color: var(--ink-light);
    line-height: 1.5;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .card-meta {
    display: flex;
    flex-direction: column;
    gap: 4px;
    font-size: 11px;
    color: var(--ink-light);
    padding-top: 8px;
    margin-top: auto;
    border-top: 1px solid rgba(200, 164, 92, 0.15);
  }
}

// 天气卡片 - 紧凑版
.info-card.weather {
  background: linear-gradient(135deg, var(--silk-gold) 0%, #fff 100%);

  .weather-row {
    display: flex;
    align-items: baseline;
    gap: 6px;
    margin-bottom: 6px;

    .wh-temp {
      font-size: 22px;
      font-weight: 700;
      color: var(--palace-red);
      line-height: 1;
    }

    .wh-text {
      font-size: 13px;
      color: var(--ink-light);
    }
  }

  .weather-grid {
    display: flex;
    flex-wrap: wrap;
    gap: 6px 10px;
    font-size: 12px;
    color: var(--ink-light);
  }
}

// 正在输入
.typing-indicator {
  display: flex;
  gap: 5px;
  padding: 14px 18px;
  background: var(--silk-gold);
  border-radius: 16px 16px 16px 4px;
  border-left: 3px solid var(--gold);

  span, .typing-dots span {
    width: 8px;
    height: 8px;
    background: var(--gold);
    border-radius: 50%;
    animation: typingBounce 1.4s ease-in-out infinite;

    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }
}

.typing-dots {
  display: flex;
  gap: 5px;
  align-items: center;

  span {
    width: 8px;
    height: 8px;
    background: var(--gold);
    border-radius: 50%;
    animation: typingBounce 1.4s ease-in-out infinite;

    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }
}

@keyframes typingBounce {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-8px); }
}

// ============ 输入区域 ============
.chat-input {
  padding: 16px 24px;
  background: rgba(255, 255, 255, 0.95);
  border-top: 1px solid var(--gold-light);
}

.input-container {
  max-width: 860px;
  margin: 0 auto;
}

.input-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.message-input {
  flex: 1;
  padding: 14px 20px;
  border: 1px solid var(--gold-light);
  border-radius: 10px;
  font-family: var(--font-body);
  font-size: 15px;
  color: var(--ink);
  background: #fff;
  outline: none;
  transition: all 0.3s ease;

  &:focus {
    border-color: var(--gold);
    box-shadow: 0 0 0 3px rgba(200, 164, 92, 0.12);
  }

  &::placeholder {
    color: var(--ink-light);
    opacity: 0.6;
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.send-btn {
  padding: 14px 28px;
  background: var(--palace-red);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-family: var(--font-body);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  letter-spacing: 4px;
  white-space: nowrap;

  &:hover:not(:disabled) {
    background: var(--palace-red-dark);
    box-shadow: 0 4px 12px rgba(122, 26, 46, 0.3);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  &:active:not(:disabled) {
    transform: scale(0.97);
  }
}

// ============ 响应式 ============
@media (max-width: 768px) {
  .quick-actions {
    grid-template-columns: repeat(2, 1fr);
  }

  .welcome-header .welcome-title {
    font-size: 40px;
  }

  .message-content {
    max-width: 85%;
  }

  .chat-input {
    padding: 12px 16px;
  }
}

@media (max-width: 480px) {
  .quick-actions {
    grid-template-columns: 1fr;
  }

  .welcome-header .welcome-title {
    font-size: 32px;
  }
}
</style>
