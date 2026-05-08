<template>
  <div class="profile-page">
    <div class="profile-card">
      <div class="profile-header">
        <div class="avatar-section">
          <div class="avatar-wrap">
            <img v-if="auth.avatar" :src="auth.avatar" class="avatar-img" />
            <div v-else class="avatar-placeholder">{{ auth.nickname.charAt(0) }}</div>
          </div>
          <h2 class="nickname">{{ auth.nickname }}</h2>
          <p class="username">@{{ auth.user?.username }}</p>
          <span class="role-badge" :class="auth.isAdmin ? (auth.user?.role === 'OWNER' ? 'owner' : 'admin') : 'user'">
            {{ auth.roleName }}
          </span>
        </div>
      </div>

      <div class="profile-body">
        <div class="form-group">
          <label>昵称</label>
          <input v-model="form.nickname" placeholder="输入昵称" />
        </div>
        <div class="form-group">
          <label>头像链接</label>
          <input v-model="form.avatar" placeholder="输入图片URL" />
          <div v-if="form.avatar" class="avatar-preview">
            <img :src="form.avatar" />
          </div>
        </div>

        <div class="form-actions">
          <button class="btn-primary" :disabled="saving" @click="handleSave">
            {{ saving ? '保存中...' : '保存修改' }}
          </button>
          <button class="btn-secondary" @click="router.back()">返回</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const saving = ref(false)

const form = reactive({
  nickname: '',
  avatar: ''
})

onMounted(() => {
  form.nickname = auth.nickname || ''
  form.avatar = auth.avatar || ''
})

async function handleSave() {
  saving.value = true
  try {
    const res = await auth.updateProfile({ nickname: form.nickname, avatar: form.avatar })
    if (res.code === 200) {
      ElMessage.success('保存成功')
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped lang="scss">
.profile-page {
  padding: 40px 24px;
  max-width: 600px;
  margin: 0 auto;
  display: flex;
  justify-content: center;
  padding-top: 60px;
}

.profile-card {
  width: 100%;
  background: #fff;
  border-radius: 16px;
  border: 1px solid var(--gold-light);
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.profile-header {
  background: linear-gradient(135deg, var(--palace-red), var(--palace-red-dark));
  padding: 40px;
  text-align: center;
}

.avatar-section {
  .avatar-wrap {
    width: 80px;
    height: 80px;
    border-radius: 50%;
    margin: 0 auto 16px;
    overflow: hidden;
    border: 3px solid var(--gold);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);

    .avatar-img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .avatar-placeholder {
      width: 100%;
      height: 100%;
      background: var(--gold);
      color: var(--palace-red);
      display: flex;
      align-items: center;
      justify-content: center;
      font-family: var(--font-display);
      font-size: 32px;
    }
  }

  .nickname {
    font-family: var(--font-body);
    font-size: 22px;
    color: #fff;
    margin-bottom: 4px;
  }

  .username {
    font-size: 13px;
    color: rgba(255, 255, 255, 0.6);
    margin-bottom: 12px;
  }

  .role-badge {
    display: inline-block;
    padding: 3px 14px;
    border-radius: 12px;
    font-size: 12px;
    font-family: var(--font-body);

    &.owner {
      background: linear-gradient(135deg, var(--gold), #b8943e);
      color: #3A0A15;
      box-shadow: 0 0 12px rgba(200, 164, 92, 0.4);
    }

    &.admin {
      background: var(--gold);
      color: var(--palace-red);
    }

    &.user {
      background: rgba(255, 255, 255, 0.2);
      color: #fff;
    }
  }
}

.profile-body {
  padding: 32px;
}

.form-group {
  margin-bottom: 24px;

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
    padding: 11px 14px;
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
  }

  .avatar-preview {
    margin-top: 10px;
    width: 80px;
    height: 80px;
    border-radius: 8px;
    overflow: hidden;
    border: 1px solid var(--gold-light);

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }
}

.form-actions {
  display: flex;
  gap: 12px;

  .btn-primary {
    flex: 1;
    padding: 12px;
    background: linear-gradient(135deg, var(--palace-red-light), var(--palace-red));
    color: #fff;
    border: none;
    border-radius: 8px;
    font-family: var(--font-body);
    font-size: 15px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s ease;
    letter-spacing: 2px;

    &:hover:not(:disabled) {
      background: var(--palace-red-dark);
      box-shadow: 0 4px 12px rgba(122, 26, 46, 0.2);
    }

    &:disabled { opacity: 0.6; cursor: not-allowed; }
  }

  .btn-secondary {
    padding: 12px 24px;
    background: transparent;
    color: var(--ink-light);
    border: 1px solid var(--gold-light);
    border-radius: 8px;
    font-family: var(--font-body);
    font-size: 15px;
    cursor: pointer;
    transition: all 0.3s ease;

    &:hover {
      border-color: var(--gold);
      color: var(--ink);
    }
  }
}
</style>
