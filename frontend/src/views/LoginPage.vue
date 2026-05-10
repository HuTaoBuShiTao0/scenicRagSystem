<template>
  <div class="login-page">
    <!-- 全屏意境背景 -->
    <div class="bg-scene">
      <div class="mist-1"></div>
      <div class="mist-2"></div>
      <div class="mountains"></div>
      <!-- 飘落牡丹瓣 -->
      <div class="petals-rain" aria-hidden="true">
        <span v-for="i in 10" :key="i" class="petal" :style="petalStyle(i)"></span>
      </div>
    </div>

    <!-- 居中登录卡片 -->
    <div class="login-card">
      <!-- 左侧装饰 -->
      <div class="card-deco">
        <div class="deco-seal">{{ isRegister ? '注' : '登' }}</div>
        <div class="deco-text">
          <span class="deco-main">神都洛阳</span>
          <span class="deco-sub">十三朝古都</span>
        </div>
        <div class="deco-line"></div>
        <p class="deco-quote">若问古今兴废事</p>
        <p class="deco-quote">请君只看洛阳城</p>
      </div>

      <!-- 右侧表单 -->
      <div class="card-form">
        <div class="form-header">
          <h2>{{ isRegister ? '创建账号' : '欢迎回来' }}</h2>
          <p>{{ isRegister ? '开启您的洛阳探索之旅' : '继续探索千年古都之美' }}</p>
        </div>

        <div class="form-body">
          <div class="field" :class="{ filled: username }">
            <input v-model="username" type="text" autocomplete="username" @keyup.enter="handleSubmit" />
            <label>用户名</label>
            <div class="field-bar"></div>
          </div>

          <div class="field" :class="{ filled: password }">
            <input v-model="password" type="password" autocomplete="current-password" @keyup.enter="handleSubmit" />
            <label>密码</label>
            <div class="field-bar"></div>
          </div>

          <button class="submit-btn" :disabled="loading" @click="handleSubmit">
            <span v-if="!loading">{{ isRegister ? '注 册' : '登 录' }}</span>
            <span v-else class="btn-loading">
              <span class="btn-spinner"></span>
              处理中...
            </span>
          </button>

          <div class="toggle-area">
            <span>{{ isRegister ? '已有账号？' : '还没有账号？' }}</span>
            <button class="toggle-link" @click="isRegister = !isRegister">
              {{ isRegister ? '去登录' : '去注册' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const isRegister = ref(false)
const username = ref('')
const password = ref('')
const loading = ref(false)

function petalStyle(i) {
  const left = ((i * 37 + 13) % 100)
  const delay = (i * 0.7) % 8
  const size = 6 + (i % 4) * 3
  const duration = 8 + (i % 5) * 2
  return {
    left: `${left}%`,
    width: `${size}px`,
    height: `${size}px`,
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`,
    '--drift': `${((i * 23) % 60) - 30}px`
  }
}

async function handleSubmit() {
  if (!username.value || !password.value) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  if (password.value.length < 3) {
    ElMessage.warning('密码至少3位')
    return
  }
  loading.value = true
  try {
    const res = isRegister.value
      ? await auth.register(username.value, password.value)
      : await auth.login(username.value, password.value)
    if (res.code === 200) {
      ElMessage.success(isRegister.value ? '注册成功' : '登录成功')
      router.push('/')
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
// ========================================
// 神都洛阳 · 登录页 — 夜阑千灯
// ========================================

.login-page {
  position: relative;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #0d0508;
}

// ============ 全屏意境背景 ============
.bg-scene {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 50% 0%, rgba(200, 164, 92, 0.06) 0%, transparent 50%),
    radial-gradient(ellipse at 25% 70%, rgba(122, 26, 46, 0.12) 0%, transparent 50%),
    radial-gradient(ellipse at 75% 60%, rgba(122, 26, 46, 0.08) 0%, transparent 50%),
    linear-gradient(180deg, #1a0a0f 0%, #2d1219 25%, #3a1520 50%, #2a0e15 75%, #0d0508 100%);

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background:
      radial-gradient(1px 1px at 15% 20%, rgba(200,164,92,0.12), transparent),
      radial-gradient(1px 1px at 35% 50%, rgba(200,164,92,0.08), transparent),
      radial-gradient(1px 1px at 55% 15%, rgba(200,164,92,0.1), transparent),
      radial-gradient(1px 1px at 75% 40%, rgba(200,164,92,0.06), transparent),
      radial-gradient(1px 1px at 90% 70%, rgba(200,164,92,0.08), transparent),
      radial-gradient(1px 1px at 45% 85%, rgba(200,164,92,0.07), transparent);
    background-size: 160px 160px;
    pointer-events: none;
  }
}

// 雾霭
.mist-1, .mist-2 {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.mist-1 {
  background: radial-gradient(ellipse 80% 40% at 50% 80%, rgba(200,164,92,0.03) 0%, transparent 60%);
  animation: mistDrift 25s ease-in-out infinite;
}

.mist-2 {
  background: radial-gradient(ellipse 60% 30% at 30% 70%, rgba(200,164,92,0.02) 0%, transparent 50%);
  animation: mistDrift 20s ease-in-out infinite -10s;
}

@keyframes mistDrift {
  0%, 100% { transform: translateX(0) scale(1); }
  50% { transform: translateX(4%) scale(1.03); }
}

// 远山
.mountains {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 30%;
  pointer-events: none;
  background:
    radial-gradient(ellipse 60% 100% at 20% 100%, rgba(200,164,92,0.05) 0%, transparent 70%),
    radial-gradient(ellipse 50% 100% at 50% 100%, rgba(200,164,92,0.03) 0%, transparent 65%),
    radial-gradient(ellipse 40% 100% at 75% 100%, rgba(122,26,46,0.06) 0%, transparent 60%);
}

// 花瓣
.petals-rain {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.petal {
  position: absolute;
  top: -20px;
  border-radius: 50% 0 50% 0;
  background: linear-gradient(135deg, rgba(200,164,92,0.15), rgba(200,164,92,0.03));
  opacity: 0;
  animation: petalFall linear infinite;

  @keyframes petalFall {
    0% { transform: translateY(-20px) rotate(0deg); opacity: 0; }
    8% { opacity: 0.5; }
    90% { opacity: 0.2; }
    100% { transform: translateY(calc(100vh + 20px)) rotate(720deg) translateX(var(--drift, 0px)); opacity: 0; }
  }
}

// ============ 居中登录卡片 ============
.login-card {
  position: relative;
  z-index: 2;
  display: flex;
  background: rgba(250, 248, 243, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  overflow: hidden;
  box-shadow:
    0 20px 60px rgba(0,0,0,0.5),
    0 0 0 1px rgba(200, 164, 92, 0.1);
  animation: cardAppear 0.8s ease-out;
  width: 680px;
  max-width: 90vw;
}

@keyframes cardAppear {
  from {
    opacity: 0;
    transform: translateY(24px) scale(0.97);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

// ===== 左侧装饰区 =====
.card-deco {
  width: 220px;
  min-width: 220px;
  background: linear-gradient(135deg, var(--palace-red-dark) 0%, var(--palace-red) 50%, #5a0f1f 100%);
  padding: 40px 28px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;

  // 暗纹
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background:
      radial-gradient(ellipse at 30% 20%, rgba(200,164,92,0.08) 0%, transparent 50%),
      radial-gradient(ellipse at 70% 80%, rgba(200,164,92,0.05) 0%, transparent 40%);
    pointer-events: none;
  }
}

.deco-seal {
  width: 52px;
  height: 52px;
  border: 2px solid var(--gold);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-display);
  font-size: 24px;
  color: var(--gold);
  transform: rotate(-5deg);
  margin-bottom: 20px;
  position: relative;
  z-index: 1;
}

.deco-text {
  text-align: center;
  position: relative;
  z-index: 1;
  margin-bottom: 20px;

  .deco-main {
    display: block;
    font-family: var(--font-display);
    font-size: 26px;
    color: var(--gold);
    letter-spacing: 6px;
    line-height: 1.3;
  }

  .deco-sub {
    display: block;
    font-size: 11px;
    color: rgba(200,164,92,0.4);
    letter-spacing: 3px;
    margin-top: 4px;
  }
}

.deco-line {
  width: 40px;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--gold), transparent);
  opacity: 0.3;
  margin: 0 auto 20px;
  position: relative;
  z-index: 1;
}

.deco-quote {
  font-family: var(--font-body);
  font-size: 12px;
  color: rgba(200,164,92,0.35);
  letter-spacing: 2px;
  line-height: 2;
  text-align: center;
  position: relative;
  z-index: 1;
}

// ===== 右侧表单区 =====
.card-form {
  flex: 1;
  padding: 40px 36px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.form-header {
  margin-bottom: 28px;

  h2 {
    font-family: var(--font-body);
    font-size: 20px;
    font-weight: 600;
    color: var(--ink);
    margin-bottom: 4px;
    letter-spacing: 1px;
  }

  p {
    font-size: 13px;
    color: var(--ink-light);
    letter-spacing: 0.5px;
  }
}

// 浮动标签
.form-body {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.field {
  position: relative;
  padding-top: 6px;

  input {
    width: 100%;
    padding: 10px 0;
    border: none;
    background: transparent;
    font-family: var(--font-body);
    font-size: 15px;
    color: var(--ink);
    outline: none;
    position: relative;
    z-index: 1;

    &:-webkit-autofill {
      -webkit-box-shadow: 0 0 0 30px rgba(250,248,243,0.95) inset !important;
      -webkit-text-fill-color: var(--ink) !important;
      caret-color: var(--ink);
    }
  }

  label {
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
    font-size: 14px;
    color: var(--ink-light);
    transition: all 0.25s ease;
    pointer-events: none;
    opacity: 0.5;
  }

  .field-bar {
    height: 1px;
    background: rgba(200,164,92,0.2);
    transition: all 0.3s ease;
    position: relative;

    &::after {
      content: '';
      position: absolute;
      left: 0;
      bottom: 0;
      width: 0;
      height: 2px;
      background: linear-gradient(90deg, var(--palace-red), var(--gold));
      transition: width 0.3s ease;
    }
  }

  &:focus-within, &.filled {
    label {
      top: -4px;
      font-size: 11px;
      opacity: 0.8;
      color: var(--palace-red);
    }
    .field-bar::after { width: 100%; }
  }
}

// 按钮
.submit-btn {
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, var(--palace-red-light), var(--palace-red));
  color: #fff;
  border: none;
  border-radius: 8px;
  font-family: var(--font-body);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  letter-spacing: 6px;
  position: relative;
  overflow: hidden;
  margin-top: 4px;

  &::before {
    content: '';
    position: absolute;
    top: 0; left: -100%;
    width: 100%; height: 100%;
    background: linear-gradient(90deg, transparent, rgba(255,255,255,0.12), transparent);
    transition: left 0.5s ease;
  }

  &:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 6px 20px rgba(122, 26, 46, 0.25);

    &::before { left: 100%; }
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.btn-loading {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.btn-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

// 切换
.toggle-area {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-size: 13px;
  color: var(--ink-light);

  .toggle-link {
    background: none;
    border: none;
    color: var(--palace-red);
    cursor: pointer;
    font-family: var(--font-body);
    padding: 0;
    font-size: 13px;
    transition: all 0.2s;

    &:hover {
      text-decoration: underline;
      color: var(--palace-red-dark);
    }
  }
}

// ============ 响应式 ============
@media (max-width: 720px) {
  .login-card {
    flex-direction: column;
    width: 90vw;
    max-height: 90vh;
    overflow-y: auto;
  }

  .card-deco {
    width: 100%;
    min-width: auto;
    padding: 28px 20px;

    .deco-quote { display: none; }
    .deco-line { margin-bottom: 0; }
  }

  .card-form {
    padding: 28px 24px;
  }
}
</style>
