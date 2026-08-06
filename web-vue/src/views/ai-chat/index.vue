<template>
  <div class="ai-chat-page" :key="lang">
    <el-row :gutter="0" style="height:calc(100vh - 100px)">
      <el-col :span="6" style="border-right:1px solid #e8e8e8;overflow-y:auto">
        <div style="padding:12px;display:flex;gap:8px">
          <el-button type="primary" size="small" style="flex:1" @click="createConv">{{ t('aiChat.newChat') }}</el-button>
          <el-button size="small" @click="promptDialogVisible = true">{{ t('aiChat.prompts') }}</el-button>
        </div>
        <div v-for="conv in conversations" :key="conv.id" class="conv-item" :class="{ active: currentConv?.id === conv.id }" @click="selectConv(conv)">
          <template v-if="editingConvId === conv.id">
            <el-input v-model="editingConvTitle" size="small" @keyup.enter="saveConvTitle(conv)" @blur="saveConvTitle(conv)" @click.stop autofocus style="flex:1" />
          </template>
          <template v-else>
            <span class="conv-title" @dblclick.stop="startEditConv(conv)">{{ conv.title }}</span>
          </template>
          <el-icon class="conv-delete" @click.stop="deleteConv(conv.id)"><Delete /></el-icon>
        </div>
      </el-col>
      <el-col :span="18" style="display:flex;flex-direction:column;overflow:hidden">
        <div class="prompt-bar">
          <div style="display:flex;align-items:center;gap:8px;margin-bottom:6px">
            <span style="font-size:12px;color:#999">{{ t('aiChat.prompt') }}:</span>
            <el-tag v-if="currentPromptName" size="small" closable @close="clearPromptSelection">{{ currentPromptName }}</el-tag>
            <el-button v-if="currentConv" size="small" text @click="openMemoryDialog">{{ t('aiChat.memory') }}</el-button>
            <el-select v-model="selectedModelId" size="small" style="width:160px;margin-left:auto" @change="handleModelChange">
              <el-option v-for="m in aiModels" :key="m.id" :label="m.modelName" :value="m.id" />
            </el-select>
          </div>
          <el-input v-model="myPrompt" type="textarea" :rows="2" :placeholder="t('aiChat.promptPlaceholder')" @blur="savePrompt" />
        </div>
        <div class="chat-messages" ref="messagesRef">
          <div v-for="msg in messages" :key="msg.id" :class="['message', msg.role]">
            <div class="message-content">{{ msg.content }}</div>
          </div>
          <div v-if="messages.length === 0" class="chat-empty">{{ t('aiChat.emptyHint') }}</div>
        </div>
        <div class="chat-input">
          <div v-if="pendingImage" class="pending-image">
            <img :src="pendingImagePreview" class="pending-img-thumb" />
            <el-icon class="pending-img-remove" @click="removePendingImage"><Close /></el-icon>
          </div>
          <div class="input-row">
            <el-upload :auto-upload="false" :show-file-list="false" accept="image/*" :on-change="handleImageSelect">
              <el-button circle><el-icon><Picture /></el-icon></el-button>
            </el-upload>
            <el-input v-model="inputText" :placeholder="t('aiChat.inputPlaceholder')" @keyup.enter="send" style="flex:1" />
            <el-button @click="send" :disabled="!inputText && !pendingImage">{{ t('aiChat.send') }}</el-button>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 提示词管理弹窗 -->
    <el-dialog v-model="promptDialogVisible" :title="t('aiChat.promptManage')" width="600px">
      <div style="margin-bottom:12px;display:flex;gap:8px">
        <el-input v-model="newPromptName" :placeholder="t('aiChat.promptName')" style="width:200px" />
        <el-button type="primary" size="small" @click="handleCreatePrompt">{{ t('common.create') }}</el-button>
      </div>
      <el-table :data="promptList" stripe max-height="400">
        <el-table-column :label="t('aiChat.promptName')" prop="name" />
        <el-table-column :label="t('aiChat.promptContent')" min-width="200">
          <template #default="{ row }">
            <span style="color:#666;font-size:12px;display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;max-width:300px">{{ row.content || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('aiChat.promptDefault')" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.isDefault === 1" size="small" type="success">{{ t('aiChat.default') }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('family.actions')" width="200">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="selectPrompt(row)">{{ t('aiChat.use') }}</el-button>
            <el-button size="small" link @click="setDefaultPrompt(row.id)">{{ t('aiChat.setDefault') }}</el-button>
            <el-button size="small" link @click="editPrompt(row)">{{ t('common.edit') }}</el-button>
            <el-popconfirm :title="t('aiChat.confirmDeletePrompt')" @confirm="handleDeletePrompt(row.id)">
              <template #reference><el-button size="small" type="danger" link>{{ t('common.delete') }}</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 编辑提示词弹窗 -->
    <el-dialog v-model="editPromptDialogVisible" :title="t('aiChat.editPrompt')" width="600px">
      <el-form label-width="80px">
        <el-form-item :label="t('aiChat.promptName')">
          <el-input v-model="editingPrompt.name" />
        </el-form-item>
        <el-form-item :label="t('aiChat.promptContent')">
          <el-input v-model="editingPrompt.content" type="textarea" :rows="6" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editPromptDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleUpdatePrompt">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- 记忆编辑弹窗 -->
    <el-dialog v-model="memoryDialogVisible" :title="t('aiChat.memoryEdit')" width="600px">
      <p style="color:#999;font-size:12px;margin-bottom:12px">{{ t('aiChat.memoryHint') }}</p>
      <el-input v-model="conversationMemory" type="textarea" :rows="8" :placeholder="t('aiChat.memoryPlaceholder')" />
      <template #footer>
        <el-button @click="memoryDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="saveMemory">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { getConversations, createConversation, getConversationMessages, sendChatMessage, deleteConversation, getUserAiPrompt, updateUserAiPrompt, uploadChatImage, getPromptList, createPrompt, updatePrompt, deletePrompt, setDefaultPrompt as apiSetDefaultPrompt, getAiModelList } from '../../api'
import { useI18n } from '../../utils/i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

const { t, lang } = useI18n()
const conversations = ref([])
const currentConv = ref(null)
const messages = ref([])
const inputText = ref('')
const messagesRef = ref(null)
const sending = ref(false)
const myPrompt = ref('')
const pendingImage = ref(null)
const pendingImagePreview = ref('')
const pendingImagePath = ref('')

// 提示词管理
const promptDialogVisible = ref(false)
const promptList = ref([])
const newPromptName = ref('')
const currentPromptName = ref('')
const editPromptDialogVisible = ref(false)
const editingPrompt = ref({ id: null, name: '', content: '' })

// 记忆编辑
const memoryDialogVisible = ref(false)
const conversationMemory = ref('')

// 模型选择
const aiModels = ref([])
const selectedModelId = ref(null)

// 对话重命名
const editingConvId = ref(null)
const editingConvTitle = ref('')

onMounted(async () => {
  await loadConversations()
  await loadPrompt()
  await loadPromptList()
  await loadAiModels()
})

async function loadConversations() {
  const res = await getConversations()
  if (res.code === 200) conversations.value = res.data
}

async function loadPrompt() {
  const res = await getUserAiPrompt()
  if (res.code === 200) myPrompt.value = res.data.aiPrompt || ''
}

async function savePrompt() {
  // 保存到用户全局提示词
  await updateUserAiPrompt({ aiPrompt: myPrompt.value })
  // 如果有当前对话，也更新对话的提示词
  if (currentConv.value) {
    await request.put(`/api/ai-chat/conversation/${currentConv.value.id}`, { systemPrompt: myPrompt.value })
    currentConv.value.systemPrompt = myPrompt.value
  }
}

async function loadPromptList() {
  const res = await getPromptList()
  if (res.code === 200) {
    promptList.value = res.data || []
    // 只有当用户没有个人提示词时，才显示默认提示词名称
    const defaultPrompt = promptList.value.find(p => p.isDefault === 1)
    if (defaultPrompt && !myPrompt.value) {
      currentPromptName.value = defaultPrompt.name
      myPrompt.value = defaultPrompt.content || ''
    }
  }
}

function selectPrompt(prompt) {
  currentPromptName.value = prompt.name
  myPrompt.value = prompt.content || ''
  savePrompt()
  promptDialogVisible.value = false
  ElMessage.success(t('aiChat.promptSelected'))
}

function clearPromptSelection() {
  currentPromptName.value = ''
}

async function handleCreatePrompt() {
  if (!newPromptName.value.trim()) {
    ElMessage.warning(t('aiChat.promptNameRequired'))
    return
  }
  const res = await createPrompt({ name: newPromptName.value, content: myPrompt.value })
  if (res.code === 200) {
    ElMessage.success(t('common.success'))
    newPromptName.value = ''
    loadPromptList()
  }
}

function editPrompt(prompt) {
  editingPrompt.value = { id: prompt.id, name: prompt.name, content: prompt.content }
  editPromptDialogVisible.value = true
}

async function handleUpdatePrompt() {
  await updatePrompt(editingPrompt.value.id, { name: editingPrompt.value.name, content: editingPrompt.value.content })
  ElMessage.success(t('common.success'))
  editPromptDialogVisible.value = false
  loadPromptList()
}

async function handleDeletePrompt(id) {
  await deletePrompt(id)
  ElMessage.success(t('common.success'))
  loadPromptList()
}

async function setDefaultPrompt(id) {
  await apiSetDefaultPrompt(id)
  ElMessage.success(t('common.success'))
  loadPromptList()
}

function handleImageSelect(file) {
  pendingImage.value = file.raw
  pendingImagePreview.value = URL.createObjectURL(file.raw)
}

function removePendingImage() {
  pendingImage.value = null
  pendingImagePreview.value = ''
  pendingImagePath.value = ''
}

async function uploadImageIfNeeded() {
  if (!pendingImage.value) return null
  const formData = new FormData()
  formData.append('file', pendingImage.value)
  const res = await uploadChatImage(formData)
  if (res.code === 200) return res.data
  return null
}

async function createConv() {
  const res = await createConversation({ title: t('aiChat.newChat') })
  if (res.code === 200) { conversations.value.unshift(res.data); selectConv(res.data) }
}

async function selectConv(conv) {
  currentConv.value = conv
  // 加载该对话的提示词
  if (conv.systemPrompt) {
    myPrompt.value = conv.systemPrompt
    // 从 promptList 中查找匹配的提示词名称
    const matched = promptList.value.find(p => p.content === conv.systemPrompt)
    currentPromptName.value = matched ? matched.name : ''
  } else {
    myPrompt.value = ''
    currentPromptName.value = ''
  }
  // 同步模型选择器
  if (conv.modelConfigId) {
    selectedModelId.value = conv.modelConfigId
  }
  const res = await getConversationMessages(conv.id)
  if (res.code === 200) messages.value = res.data
  await nextTick()
  scrollToBottom()
}

async function send() {
  if ((!inputText.value.trim() && !pendingImage.value) || sending.value) return
  sending.value = true
  const text = inputText.value
  inputText.value = ''

  // 发送前先保存提示词（防止blur未触发）
  await savePrompt()

  // 自动创建对话
  if (!currentConv.value) {
    const convRes = await createConversation({ title: text.substring(0, 20) || t('aiChat.newChat') })
    if (convRes.code === 200) {
      conversations.value.unshift(convRes.data)
      currentConv.value = convRes.data
    } else {
      sending.value = false
      return
    }
  }

  const imagePath = pendingImage.value ? await uploadImageIfNeeded() : null
  removePendingImage()

  messages.value.push({ role: 'user', content: text || t('aiChat.imagePlaceholder'), id: Date.now() })
  scrollToBottom()

  try {
    const res = await sendChatMessage(currentConv.value.id, { content: text, imagePath: imagePath || '' })
    if (res.code === 200 && res.data) {
      messages.value.push(res.data)
      scrollToBottom()
    } else {
      messages.value.push({ role: 'assistant', content: res.message || t('aiChat.noResponse'), id: Date.now() + 1 })
    }
  } catch (e) {
    messages.value.push({ role: 'assistant', content: t('aiChat.sendFailed') + (e.message || t('aiChat.networkError')), id: Date.now() + 1 })
  } finally { sending.value = false }
}

function startEditConv(conv) {
  editingConvId.value = conv.id
  editingConvTitle.value = conv.title
}

async function saveConvTitle(conv) {
  if (editingConvTitle.value.trim() && editingConvTitle.value !== conv.title) {
    await request.put(`/api/ai-chat/conversation/${conv.id}`, { title: editingConvTitle.value.trim() })
    conv.title = editingConvTitle.value.trim()
  }
  editingConvId.value = null
}

async function deleteConv(id) {
  try {
    await ElMessageBox.confirm(t('aiChat.confirmDelete'), t('common.confirm'), { type: 'warning' })
    await deleteConversation(id)
    conversations.value = conversations.value.filter(c => c.id !== id)
    if (currentConv.value?.id === id) { currentConv.value = null; messages.value = [] }
  } catch {}
}

function scrollToBottom() {
  nextTick(() => { if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight })
}

// 记忆编辑
function openMemoryDialog() {
  conversationMemory.value = currentConv.value?.systemPrompt || ''
  memoryDialogVisible.value = true
}

async function saveMemory() {
  if (currentConv.value) {
    currentConv.value.systemPrompt = conversationMemory.value
    await request.put(`/api/ai-chat/conversation/${currentConv.value.id}`, { systemPrompt: conversationMemory.value })
    myPrompt.value = conversationMemory.value
  }
  memoryDialogVisible.value = false
  ElMessage.success(t('common.success'))
}

// 模型选择
async function loadAiModels() {
  const res = await getAiModelList()
  if (res.code === 200) {
    aiModels.value = (res.data || []).filter(m => m.status === 1)
    const defaultModel = aiModels.value.find(m => m.isDefault === 1)
    if (defaultModel) selectedModelId.value = defaultModel.id
  }
}

async function handleModelChange(modelId) {
  if (!currentConv.value) return
  await request.put(`/api/ai-chat/conversation/${currentConv.value.id}/model`, { modelConfigId: modelId })
}
</script>

<style scoped>
.prompt-bar { padding: 8px 12px; border-bottom: 1px solid #e8e8e8; background: #fafafa; }
.conv-item { padding: 12px 16px; cursor: pointer; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #f0f0f0; }
.conv-item:hover, .conv-item.active { background: #ecf5ff; }
.conv-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; }
.conv-delete { color: #999; margin-left: 8px; }
.chat-messages { flex: 1; min-height: 0; overflow-y: auto; padding: 20px; }
.chat-empty { text-align: center; color: #999; padding: 60px 0; }
.message { margin-bottom: 16px; }
.message.user .message-content { background: #409eff; color: white; margin-left: 20%; border-radius: 12px 12px 0 12px; }
.message.assistant .message-content { background: #f0f0f0; color: #333; margin-right: 20%; border-radius: 12px 12px 12px 0; }
.message-content { padding: 12px 16px; display: inline-block; max-width: 100%; white-space: pre-wrap; word-break: break-word; }
.chat-input { padding: 12px 16px; border-top: 1px solid #e8e8e8; }
.input-row { display: flex; align-items: center; gap: 8px; }
.pending-image { position: relative; display: inline-block; margin-bottom: 8px; }
.pending-img-thumb { width: 80px; height: 80px; object-fit: cover; border-radius: 8px; border: 1px solid #ddd; }
.pending-img-remove { position: absolute; top: -6px; right: -6px; background: #f56c6c; color: white; border-radius: 50%; width: 18px; height: 18px; display: flex; align-items: center; justify-content: center; cursor: pointer; font-size: 12px; }
</style>
