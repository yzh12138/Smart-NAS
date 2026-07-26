<template>
  <div class="settings-page" :key="lang">
    <el-card>
      <template #header>{{ t('settings.title') }}</template>
      <el-form label-width="120px" label-position="left">
        <!-- 显示设置 -->
        <el-divider content-position="left">{{ t('settings.display') }}</el-divider>
        <el-form-item :label="t('settings.theme')">
          <el-radio-group v-model="settings.theme" @change="applyTheme">
            <el-radio-button value="light">{{ t('settings.themeLight') }}</el-radio-button>
            <el-radio-button value="dark">{{ t('settings.themeDark') }}</el-radio-button>
            <el-radio-button value="auto">{{ t('settings.themeAuto') }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('settings.language')">
          <el-select v-model="settings.language" @change="applyLanguage" style="width:200px">
            <el-option :label="t('settings.langZh')" value="zh" />
            <el-option :label="t('settings.langEn')" value="en" />
          </el-select>
        </el-form-item>

        <!-- 回收站设置 -->
        <el-divider content-position="left">{{ t('recycle.title') }}</el-divider>
        <el-form-item :label="t('recycle.retentionDays')">
          <el-input-number v-model="recycleDays" :min="1" :max="60" />
          <span style="margin-left:8px;color:#999">{{ t('common.days') }}</span>
          <el-button type="primary" size="small" style="margin-left:12px" @click="saveRecycleDays">{{ t('common.confirm') }}</el-button>
        </el-form-item>

        <!-- 存储信息（管理员可编辑） -->
        <template v-if="isAdmin">
        <el-divider content-position="left">{{ t('settings.storage') }}</el-divider>
        <el-form-item :label="t('settings.photoPath')"><el-input v-model="storagePaths.photos" /></el-form-item>
        <el-form-item :label="t('settings.videoPath')"><el-input v-model="storagePaths.videos" /></el-form-item>
        <el-form-item :label="t('settings.thumbPath')"><el-input v-model="storagePaths.thumbnails" /></el-form-item>
        <el-form-item><el-button type="primary" size="small" @click="saveStoragePaths">{{ t('common.confirm') }}</el-button></el-form-item>
        </template>

        <!-- 重复文件检测 -->
        <template v-if="isAdmin">
        <el-divider content-position="left">{{ t('settings.duplicateCheck') }}</el-divider>
        <el-form-item>
          <el-button @click="checkDuplicates" :loading="checkingDup">{{ t('settings.checkDuplicate') }}</el-button>
        </el-form-item>
        <div v-if="duplicates.length > 0">
          <el-alert :title="t('settings.duplicateFound', { count: duplicates.length })" type="warning" show-icon style="margin-bottom:12px" />
          <div v-for="(group, idx) in duplicates" :key="idx" class="dup-group">
            <p style="font-weight:bold">{{ t('settings.hash') }}: {{ group.hash.substring(0, 16) }}... ({{ group.count }} {{ t('settings.files') }})</p>
            <div v-for="photo in group.photos" :key="photo.id" class="dup-item">
              <img :src="`/api/photo/${photo.id}/thumb`" style="width:40px;height:40px;object-fit:cover;border-radius:4px" />
              <span>{{ photo.originalName }}</span>
            </div>
          </div>
          <el-button type="danger" size="small" style="margin-top:12px" @click="cleanAllDuplicates">{{ t('settings.cleanAll') }}</el-button>
        </div>
        </template>

        <!-- AI 服务 -->
        <template v-if="isAdmin">
        <el-divider content-position="left">{{ t('settings.aiService') }}</el-divider>
        <el-form-item :label="t('settings.ollamaStatus')">
          <el-tag :type="ollamaStatus === 'running' ? 'success' : 'danger'">
            {{ ollamaStatus === 'running' ? t('settings.ollamaRunning') : t('settings.ollamaStopped') }}
          </el-tag>
          <el-button size="small" style="margin-left:12px" @click="checkOllama">{{ t('settings.check') }}</el-button>
        </el-form-item>
        <el-form-item :label="t('settings.aiModel')">
          <div style="width:100%">
            <div v-if="aiModels.length > 0" style="margin-bottom:8px">
              <el-tag v-for="m in aiModels" :key="m.id" :type="m.isDefault === 1 ? 'success' : 'info'" style="margin-right:8px;margin-bottom:4px">
                {{ m.modelName }} ({{ m.modelId }})
                <el-button v-if="m.isDefault !== 1" text size="small" @click="setDefaultModel(m.id)">{{ t('settings.setDefault') }}</el-button>
              </el-tag>
            </div>
            <div style="display:flex;gap:8px;flex-wrap:wrap">
              <el-button size="small" type="primary" @click="showAddModel">{{ t('settings.addModel') }}</el-button>
              <el-button size="small" @click="addMiMoModel">小米 MiMo</el-button>
              <el-button size="small" @click="addQuickModel('deepseek', 'DeepSeek', 'deepseek-chat', 'https://api.deepseek.com/v1')">DeepSeek</el-button>
              <el-button size="small" @click="addQuickModel('qwen', '通义千问', 'qwen-plus', 'https://dashscope.aliyuncs.com/compatible-mode/v1')">通义千问</el-button>
              <el-button size="small" @click="addQuickModel('zhipu', '智谱GLM', 'glm-4', 'https://open.bigmodel.cn/api/paas/v4')">智谱GLM</el-button>
              <el-button size="small" @click="addQuickModel('moonshot', '月之暗面', 'moonshot-v1-8k', 'https://api.moonshot.cn/v1')">Kimi</el-button>
              <el-button size="small" @click="addQuickModel('gemini', 'Google Gemini', 'gemini-pro', 'https://generativelanguage.googleapis.com/v1beta')">Gemini</el-button>
            </div>
          </div>
        </el-form-item>
        <el-form-item :label="t('settings.globalPrompt')">
          <el-input v-model="globalPrompt" type="textarea" :rows="4" :placeholder="t('settings.globalPromptPlaceholder')" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="small" @click="saveGlobalPrompt">{{ t('common.confirm') }}</el-button>
        </el-form-item>
        </template>

        <!-- 手机备份 -->
        <el-divider content-position="left">{{ t('backup.title') }}</el-divider>
        <div class="backup-section">
          <div class="backup-info">
            <div class="backup-row">
              <span class="backup-label">{{ t('backup.address') }}:</span>
              <el-tag type="info" size="small" style="font-family:monospace">{{ webdavUrl }}</el-tag>
              <el-button size="small" text @click="copyUrl">{{ t('backup.copy') }}</el-button>
            </div>
            <div class="backup-row">
              <span class="backup-label">{{ t('backup.username') }}:</span>
              <span>{{ currentUser }}</span>
            </div>
            <div class="backup-row">
              <span class="backup-label">{{ t('backup.password') }}:</span>
              <span>{{ t('backup.sameAsLogin') }}</span>
            </div>
          </div>
          <div class="backup-qr">
            <div ref="qrCodeRef" style="width:120px;height:120px;border:1px solid #eee;border-radius:8px;display:flex;align-items:center;justify-content:center"></div>
            <p style="font-size:11px;color:#999;margin-top:4px">{{ t('backup.scanToConnect') }}</p>
          </div>
        </div>
        <el-collapse style="margin-top:12px">
          <el-collapse-item :title="t('backup.iosGuide')">
            <ol style="margin:0;padding-left:20px;font-size:13px;color:#666;line-height:1.8">
              <li>{{ t('backup.iosStep1') }}</li>
              <li>{{ t('backup.iosStep2') }}</li>
              <li>{{ t('backup.iosStep3') }}</li>
              <li>{{ t('backup.iosStep4') }}</li>
            </ol>
          </el-collapse-item>
          <el-collapse-item :title="t('backup.androidGuide')">
            <ol style="margin:0;padding-left:20px;font-size:13px;color:#666;line-height:1.8">
              <li>{{ t('backup.androidStep1') }}</li>
              <li>{{ t('backup.androidStep2') }}</li>
              <li>{{ t('backup.androidStep3') }}</li>
            </ol>
          </el-collapse-item>
          <el-collapse-item :title="t('backup.windowsGuide')">
            <ol style="margin:0;padding-left:20px;font-size:13px;color:#666;line-height:1.8">
              <li>{{ t('backup.windowsStep1') }}</li>
              <li>{{ t('backup.windowsStep2') }}</li>
              <li>{{ t('backup.windowsStep3') }}</li>
            </ol>
          </el-collapse-item>
        </el-collapse>
      </el-form>
    </el-card>

    <!-- 添加模型弹窗 -->
    <el-dialog v-model="modelDialogVisible" :title="t('settings.addModel')" width="500px">
      <el-form label-width="100px">
        <el-form-item :label="t('settings.modelName')">
          <el-input v-model="modelForm.modelName" :placeholder="t('settings.modelNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('settings.modelId')">
          <el-input v-model="modelForm.modelId" :placeholder="t('settings.modelIdPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('settings.modelType')">
          <el-select v-model="modelForm.modelType" style="width:100%">
            <el-option label="Ollama (本地)" value="ollama" />
            <el-option label="OpenAI" value="openai" />
            <el-option label="Azure OpenAI" value="azure" />
            <el-option label="Google Gemini" value="gemini" />
            <el-option label="Anthropic Claude" value="claude" />
            <el-option label="DeepSeek" value="deepseek" />
            <el-option label="通义千问 Qwen" value="qwen" />
            <el-option label="智谱 GLM" value="zhipu" />
            <el-option label="月之暗面 Moonshot" value="moonshot" />
            <el-option label="百川 Baichuan" value="baichuan" />
            <el-option label="MiniMax" value="minimax" />
            <el-option label="百度文心 ERNIE" value="wenxin" />
            <el-option label="讯飞星火 Spark" value="spark" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('settings.apiUrl')">
          <el-input v-model="modelForm.apiUrl" :placeholder="apiUrlPlaceholder" />
        </el-form-item>
        <el-form-item v-if="modelForm.modelType === 'openai'" label="API Key">
          <el-input v-model="mimoApiKey" :placeholder="t('settings.mimoApiKeyPlaceholder')" show-password />
        </el-form-item>
        <el-form-item :label="t('settings.modelPrompt')">
          <el-input v-model="modelForm.promptTemplate" type="textarea" :rows="3" :placeholder="t('settings.modelPromptPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modelDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleAddModel">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick, watch } from 'vue'
import { useI18n } from '../../utils/i18n'
import { getDuplicateList, cleanDuplicates, getGlobalPrompt, updateGlobalPrompt, getAiModelList, createAiModel, updateAiModel, setDefaultAiModel, updateRecycleDays as apiUpdateRecycleDays, getUserInfo } from '../../api'
import { useUserStore } from '../../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import QRCode from 'qrcodejs2'

const { t, setLang, lang } = useI18n()
const userStore = useUserStore()
const isAdmin = computed(() => {
  const roles = userStore.userInfo?.roles
  return roles && roles.some(r => r.roleKey === 'admin')
})

const settings = reactive({ theme: localStorage.getItem('theme') || 'light', language: localStorage.getItem('language') || 'zh' })
const recycleDays = ref(parseInt(localStorage.getItem('recycleDays') || '30'))
const storagePaths = reactive({ photos: 'D:\\test\\photos', videos: 'D:\\test\\videos', thumbnails: 'D:\\test\\thumbnails' })
const ollamaStatus = ref('unknown')
const duplicates = ref([])
const checkingDup = ref(false)
const globalPrompt = ref('')
const aiModels = ref([])
const modelDialogVisible = ref(false)
const modelForm = ref({ modelName: '', modelId: '', modelType: 'ollama', apiUrl: 'http://localhost:11434', promptTemplate: '' })
const mimoApiKey = ref(null)

const apiUrlPlaceholder = computed(() => {
  const placeholders = {
    ollama: 'http://localhost:11434',
    openai: 'https://api.openai.com/v1',
    azure: 'https://your-resource.openai.azure.com',
    gemini: 'https://generativelanguage.googleapis.com/v1beta',
    claude: 'https://api.anthropic.com/v1',
    deepseek: 'https://api.deepseek.com/v1',
    qwen: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
    zhipu: 'https://open.bigmodel.cn/api/paas/v4',
    moonshot: 'https://api.moonshot.cn/v1',
    baichuan: 'https://api.baichuan-ai.com/v1',
    minimax: 'https://api.minimax.chat/v1',
    wenxin: 'https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop',
    spark: 'https://spark-api-open.xf-yun.com/v1'
  }
  return placeholders[modelForm.value.modelType] || t('settings.apiUrlPlaceholder')
})

// 备份相关
const currentUser = ref('admin')
const qrCodeRef = ref(null)
const webdavUrl = computed(() => {
  const host = window.location.hostname
  return `http://${host}:8081/webdav/`
})

onMounted(() => {
  applyTheme(); loadGlobalPrompt(); loadAiModels(); userStore.getUserInfo(); loadUserInfo()
  nextTick(() => generateQR())
})

watch(webdavUrl, () => nextTick(() => generateQR()))

function generateQR() {
  if (!qrCodeRef.value) return
  qrCodeRef.value.innerHTML = ''
  try {
    new QRCode(qrCodeRef.value, {
      text: webdavUrl.value,
      width: 120,
      height: 120,
      colorDark: '#000000',
      colorLight: '#ffffff',
      correctLevel: QRCode.CorrectLevel.M
    })
  } catch {}
}

function applyTheme() {
  localStorage.setItem('theme', settings.theme)
  const root = document.documentElement
  root.setAttribute('data-theme', settings.theme === 'auto' ? (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light') : settings.theme)
}

function applyLanguage() { setLang(settings.language) }

async function loadUserInfo() {
  try {
    const res = await getUserInfo()
    if (res.code === 200 && res.data) {
      currentUser.value = res.data.username || 'admin'
    }
  } catch {}
}

function copyUrl() {
  navigator.clipboard.writeText(webdavUrl.value).then(() => {
    ElMessage.success(t('backup.copied'))
  }).catch(() => {
    ElMessage.error(t('backup.copyFailed'))
  })
}

async function saveRecycleDays() {
  localStorage.setItem('recycleDays', String(recycleDays.value))
  await apiUpdateRecycleDays(recycleDays.value)
  ElMessage.success(t('common.success'))
}

function saveStoragePaths() { ElMessage.success(t('settings.storagePathUpdated')) }

async function checkOllama() {
  try {
    const res = await fetch('http://localhost:11434/api/tags')
    if (res.ok) {
      const data = await res.json()
      const models = (data.models || []).map(m => m.name).join(', ')
      ollamaStatus.value = 'running'
      ElMessageBox.alert(t('settings.ollamaRunningDetail', { models: models || '' }), t('settings.ollamaStatusTitle'), { type: 'success' })
    } else {
      ollamaStatus.value = 'stopped'
      ElMessage.error(t('settings.ollamaResponseError'))
    }
  } catch {
    ollamaStatus.value = 'stopped'
    ElMessage.error(t('settings.ollamaNotConnected'))
  }
}

async function checkDuplicates() {
  checkingDup.value = true
  try {
    const res = await getDuplicateList()
    if (res.code === 200) {
      duplicates.value = res.data || []
      if (duplicates.value.length === 0) {
        ElMessage.success(t('settings.noDuplicates'))
      }
    }
  } catch (e) {
    ElMessage.error(t('settings.duplicateCheckFailed'))
  } finally { checkingDup.value = false }
}

async function cleanAllDuplicates() {
  const keepIds = duplicates.value.flatMap(g => [g.photos[0].id])
  await cleanDuplicates({ keepIds })
  ElMessage.success(t('settings.duplicatesCleaned'))
  duplicates.value = []
}

async function loadGlobalPrompt() {
  const res = await getGlobalPrompt()
  if (res.code === 200) globalPrompt.value = res.data.prompt || ''
}

async function saveGlobalPrompt() {
  await updateGlobalPrompt({ prompt: globalPrompt.value })
  ElMessage.success(t('settings.globalPromptUpdated'))
}

async function loadAiModels() {
  const res = await getAiModelList()
  if (res.code === 200) aiModels.value = res.data || []
}

function showAddModel() {
  modelForm.value = { modelName: '', modelId: '', modelType: 'ollama', apiUrl: 'http://localhost:11434', promptTemplate: '' }
  mimoApiKey.value = null
  modelDialogVisible.value = true
}

function addQuickModel(type, name, modelId, apiUrl) {
  modelForm.value = {
    modelName: name,
    modelId: modelId,
    modelType: 'openai',
    apiUrl: apiUrl,
    promptTemplate: ''
  }
  mimoApiKey.value = ''
  modelDialogVisible.value = true
}

async function handleAddModel() {
  if (!modelForm.value.modelName || !modelForm.value.modelId) {
    ElMessage.warning(t('settings.modelNameRequired'))
    return
  }
  // 如果有 API Key（如 MiMo），一并保存
  if (mimoApiKey.value !== null && mimoApiKey.value !== '') {
    modelForm.value.apiKey = mimoApiKey.value
  }
  await createAiModel(modelForm.value)
  ElMessage.success(t('common.success'))
  modelDialogVisible.value = false
  mimoApiKey.value = null
  loadAiModels()
}

async function setDefaultModel(id) {
  await setDefaultAiModel(id)
  ElMessage.success(t('common.success'))
  loadAiModels()
}

function addMiMoModel() {
  modelForm.value = {
    modelName: 'MiMo',
    modelId: 'MiMo',
    modelType: 'openai',
    apiUrl: 'https://api.mimo.xiaomi.com/v1',
    promptTemplate: ''
  }
  mimoApiKey.value = ''
  modelDialogVisible.value = true
}
</script>

<style scoped>
.settings-page { max-width: 700px; margin: 0 auto; padding: 0 12px; }
.settings-page :deep(.el-divider) { margin: 24px 0 16px; }
.settings-page :deep(.el-form-item) { margin-bottom: 20px; }
.settings-page :deep(.el-form) { label-width: auto !important; }
.dup-group { margin-bottom: 16px; padding: 12px; background: #fdf6ec; border-radius: 8px; }
.dup-item { display: flex; align-items: center; gap: 8px; padding: 4px 0; }
.backup-section { display: flex; gap: 24px; align-items: flex-start; }
.backup-info { flex: 1; }
.backup-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; font-size: 14px; }
.backup-label { color: #666; min-width: 70px; }
.backup-qr { text-align: center; flex-shrink: 0; }
@media (max-width: 768px) {
  .settings-page :deep(.el-form-item__label) { float: none; display: block; text-align: left; margin-bottom: 4px; }
  .settings-page :deep(.el-form-item__content) { margin-left: 0 !important; }
  .backup-section { flex-direction: column; align-items: center; }
}
</style>
