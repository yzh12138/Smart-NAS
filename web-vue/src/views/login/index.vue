<template>
  <div class="login-container" :key="lang" @mousemove="handleMouseMove">
    <!-- 可爱浮动元素 -->
    <div class="bg-elements">
      <div v-for="(el, i) in cuteElements" :key="i" class="floating-element" :style="getElementStyle(i, el)">
        {{ el }}
      </div>
    </div>
    <div class="login-card" :style="cardStyle">
      <div class="login-header">
        <div class="cute-icons">
          <span class="bounce-icon" style="animation-delay:0s">🐱</span>
          <span class="bounce-icon" style="animation-delay:0.1s">🐶</span>
          <span class="bounce-icon" style="animation-delay:0.2s">🌸</span>
        </div>
        <h1>{{ t('app.title') }}</h1>
        <p>{{ t('login.subtitle') }}</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" class="login-form">
        <el-form-item prop="username">
          <el-input v-model="form.username" :placeholder="t('login.username')" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" :placeholder="t('login.password')" prefix-icon="Lock" size="large" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" class="login-btn" @click="handleLogin">
            {{ t('login.submit') }} 🐾
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-footer">
        <el-button text type="primary" @click="registerDialogVisible = true">{{ t('login.register') }}</el-button>
        <span style="color:#ccc;margin:0 8px">|</span>
        <span style="font-size:12px;color:#ccc">🐕 Made with love 🐱</span>
      </div>
    </div>

    <!-- 注册弹窗 -->
    <el-dialog v-model="registerDialogVisible" :title="t('login.registerTitle')" width="400px" append-to-body>
      <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-width="80px">
        <el-form-item :label="t('login.username')" prop="username">
          <el-input v-model="registerForm.username" />
        </el-form-item>
        <el-form-item :label="t('login.password')" prop="password">
          <el-input v-model="registerForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item :label="t('login.nickname')" prop="nickname">
          <el-input v-model="registerForm.nickname" />
        </el-form-item>
      </el-form>
      <el-alert :title="t('login.registerHint')" type="info" show-icon :closable="false" style="margin-bottom:16px" />
      <template #footer>
        <el-button @click="registerDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="registerLoading" @click="handleRegister">{{ t('login.register') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { useI18n } from '../../utils/i18n'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'

const router = useRouter()
const userStore = useUserStore()
const { t, lang } = useI18n()
const formRef = ref()
const loading = ref(false)
const mouseX = ref(0.5)
const mouseY = ref(0.5)

// 注册相关
const registerDialogVisible = ref(false)
const registerFormRef = ref()
const registerLoading = ref(false)
const registerForm = reactive({ username: '', password: '', nickname: '' })

const cuteElements = ['🐱', '🐶', '🐾', '🌸', '⭐', '🌙', '🐟', '🎨', '🎵', '💫', '🧸', '🎋', '🍭', '🐣', '🌈', '🌺', '🐱', '🐶', '🐾', '⭐']

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: () => t('login.usernameRequired'), trigger: 'blur' }],
  password: [{ required: true, message: () => t('login.passwordRequired'), trigger: 'blur' }]
}

const registerRules = {
  username: [{ required: true, message: () => t('login.usernameRequired'), trigger: 'blur' }],
  password: [{ required: true, message: () => t('login.passwordRequired'), trigger: 'blur' }],
  nickname: [{ required: true, message: () => t('login.nicknameRequired'), trigger: 'blur' }]
}

const cardStyle = computed(() => ({
  transform: `perspective(1000px) rotateY(${(mouseX.value - 0.5) * 8}deg) rotateX(${-(mouseY.value - 0.5) * 8}deg)`
}))

function getElementStyle(i, el) {
  const x = (mouseX.value - 0.5) * (3 + i * 1.5)
  const y = (mouseY.value - 0.5) * (3 + i * 1.5)
  return {
    left: `${(i * 5) % 100}%`, top: `${(i * 7 + 3) % 100}%`,
    transform: `translate(${x}px, ${y}px)`,
    fontSize: `${18 + (i % 4) * 10}px`, opacity: 0.2 + (i % 3) * 0.1
  }
}

function handleMouseMove(e) {
  mouseX.value = e.clientX / window.innerWidth
  mouseY.value = e.clientY / window.innerHeight
}

async function handleLogin() {
  try { await formRef.value.validate() } catch { return }
  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    ElMessage.success(t('login.loginSuccess'))
    router.push('/dashboard')
  } catch (e) {
    ElMessage.error(e.message || t('login.loginFailed'))
  } finally { loading.value = false }
}

async function handleRegister() {
  try { await registerFormRef.value.validate() } catch { return }
  registerLoading.value = true
  try {
    await request.post('/api/auth/register', {
      username: registerForm.username,
      password: registerForm.password,
      nickname: registerForm.nickname
    })
    ElMessage.success(t('login.registerSuccess'))
    registerDialogVisible.value = false
    registerForm.username = ''
    registerForm.password = ''
    registerForm.nickname = ''
  } catch (e) {
    ElMessage.error(e.response?.data?.message || t('login.registerFailed'))
  } finally { registerLoading.value = false }
}
</script>

<style scoped>
.login-container { width: 100vw; height: 100vh; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 50%, #a1c4fd 100%); position: relative; overflow: hidden; }
.bg-elements { position: absolute; width: 100%; height: 100%; pointer-events: none; }
.floating-element { position: absolute; transition: transform 0.15s ease-out; user-select: none; }
.login-card { width: 400px; max-width: 90vw; padding: 40px; background: rgba(255,255,255,0.95); border-radius: 20px; box-shadow: 0 20px 60px rgba(0,0,0,0.15); transition: transform 0.1s ease-out; z-index: 1; }
.login-header { text-align: center; margin-bottom: 30px; }
.cute-icons { font-size: 40px; margin-bottom: 8px; }
.bounce-icon { display: inline-block; animation: bounce 1.5s infinite; }
@keyframes bounce { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-10px); } }
.login-header h1 { font-size: 28px; color: #333; margin: 8px 0; }
.login-header p { color: #999; font-size: 14px; }
.login-btn { width: 100%; border-radius: 12px; }
.login-footer { text-align: center; margin-top: 16px; }
</style>
