<template>
  <div class="login-page">
    <div class="login-bg">
      <div class="bg-overlay"></div>
      <div class="bg-content">
        <h1 class="bg-title">神都洛阳</h1>
        <p class="bg-subtitle">十三朝古都 · 千年风华</p>
        <p class="bg-quote">若问古今兴废事</p>
        <p class="bg-quote">请君只看洛阳城</p>
      </div>
    </div>

    <div class="login-form-wrap">
      <div class="form-card">
        <div class="form-header">
          <svg width="40" height="40" viewBox="0 0 40 40">
            <rect width="40" height="40" rx="8" fill="var(--palace-red)"/>
            <text x="20" y="28" text-anchor="middle" font-size="24" fill="var(--gold)" font-family="serif" font-weight="bold">洛</text>
          </svg>
          <h2>{{ isRegister ? '注册' : '登录' }}</h2>
          <p class="form-desc">{{ isRegister ? '创建账号，开启洛阳之旅' : '欢迎回来，继续探索洛阳' }}</p>
        </div>

        <div class="form-body">
          <div class="input-group">
            <label>用户名</label>
            <input v-model="username" placeholder="请输入用户名" @keyup.enter="handleSubmit" />
          </div>
          <div class="input-group">
            <label>密码</label>
            <input v-model="password" type="password" placeholder="请输入密码" @keyup.enter="handleSubmit" />
          </div>

          <button class="submit-btn" :disabled="loading" @click="handleSubmit">
            {{ loading ? '处理中...' : (isRegister ? '注册' : '登录') }}
          </button>

          <div class="toggle-link">
            <span v-if="isRegister">已有账号？</span>
            <span v-else>还没有账号？</span>
            <a href="#" @click.prevent="isRegister = !isRegister">
              {{ isRegister ? '去登录' : '去注册' }}
            </a>
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
.login-page {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.login-bg {
  flex: 1;
  position: relative;
  background:
    radial-gradient(ellipse at 30% 20%, rgba(200, 164, 92, 0.15) 0%, transparent 60%),
    radial-gradient(ellipse at 70% 80%, rgba(122, 26, 46, 0.1) 0%, transparent 50%),
    linear-gradient(135deg, var(--palace-red-dark) 0%, var(--palace-red) 50%, #3A0A15 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;

  .bg-overlay {
    position: absolute;
    inset: 0;
    background:
      radial-gradient(2px 2px at 20% 30%, rgba(200, 164, 92, 0.3), transparent),
      radial-gradient(2px 2px at 40% 70%, rgba(200, 164, 92, 0.2), transparent),
      radial-gradient(2px 2px at 60% 20%, rgba(200, 164, 92, 0.25), transparent),
      radial-gradient(2px 2px at 80% 60%, rgba(200, 164, 92, 0.2), transparent);
    background-size: 200px 200px;
  }

  .bg-content {
    position: relative;
    z-index: 1;
    text-align: center;
    color: #fff;
  }

  .bg-title {
    font-family: var(--font-display);
    font-size: 72px;
    color: var(--gold);
    letter-spacing: 12px;
    margin-bottom: 12px;
    text-shadow: 0 4px 20px rgba(200, 164, 92, 0.3);
  }

  .bg-subtitle {
    font-family: var(--font-body);
    font-size: 16px;
    opacity: 0.7;
    letter-spacing: 4px;
    margin-bottom: 40px;
  }

  .bg-quote {
    font-family: var(--font-body);
    font-size: 14px;
    opacity: 0.5;
    letter-spacing: 3px;
    line-height: 2;
  }
}

.login-form-wrap {
  width: 460px;
  min-width: 460px;
  background: var(--ivory);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.form-card {
  width: 100%;
  max-width: 360px;
}

.form-header {
  text-align: center;
  margin-bottom: 36px;

  h2 {
    font-family: var(--font-body);
    font-size: 24px;
    color: var(--ink);
    margin: 12px 0 6px;
  }

  .form-desc {
    font-family: var(--font-body);
    font-size: 14px;
    color: var(--ink-light);
  }
}

.form-body {
  .input-group {
    margin-bottom: 20px;

    label {
      display: block;
      font-family: var(--font-body);
      font-size: 13px;
      font-weight: 600;
      color: var(--ink);
      margin-bottom: 6px;
    }

    input {
      width: 100%;
      padding: 12px 14px;
      border: 1px solid var(--gold-light);
      border-radius: 8px;
      font-family: var(--font-body);
      font-size: 14px;
      color: var(--ink);
      background: #fff;
      outline: none;
      transition: all 0.3s ease;

      &:focus {
        border-color: var(--gold);
        box-shadow: 0 0 0 3px rgba(200, 164, 92, 0.1);
      }

      &::placeholder {
        color: var(--ink-light);
        opacity: 0.5;
      }
    }
  }

  .submit-btn {
    width: 100%;
    padding: 13px;
    background: linear-gradient(135deg, var(--palace-red-light), var(--palace-red));
    color: #fff;
    border: none;
    border-radius: 8px;
    font-family: var(--font-body);
    font-size: 16px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s ease;
    letter-spacing: 4px;

    &:hover:not(:disabled) {
      background: var(--palace-red-dark);
      box-shadow: 0 4px 16px rgba(122, 26, 46, 0.3);
    }

    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
  }

  .toggle-link {
    text-align: center;
    margin-top: 20px;
    font-family: var(--font-body);
    font-size: 13px;
    color: var(--ink-light);

    a {
      color: var(--palace-red);
      margin-left: 4px;
      text-decoration: none;

      &:hover {
        text-decoration: underline;
      }
    }
  }
}

@media (max-width: 860px) {
  .login-bg { display: none; }
  .login-form-wrap { width: 100%; min-width: auto; }
}
</style>
