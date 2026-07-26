<template>
  <div v-if="!fullyClosed" class="floating-assistant" :class="{ minimized: closed }" :style="assistantStyle" @mousedown="startDrag">
    <!-- 柴犬头像 -->
    <div class="shiba-avatar" @click.stop="toggleChat" @mouseenter="showTip = true" @mouseleave="showTip = false">
      <span class="shiba-emoji">{{ currentEmoji }}</span>
      <!-- 关闭按钮 -->
      <div class="close-btn" @click.stop="closeAssistant" v-if="showTip && !closed">✕</div>
    </div>
    <!-- 卖萌提示 -->
    <div v-if="showTip && !chatVisible && !closed" class="shiba-tip">{{ tipText }}</div>
    <!-- AI对话小弹窗 -->
    <div v-if="chatVisible" class="chat-popup" @mousedown.stop>
      <div class="chat-header">
        <span>🐕 {{ t('aiChat.assistantTitle') }}</span>
        <div style="display:flex;align-items:center;gap:8px">
          <el-button text size="small" @click="startNewChat">+ {{ t('aiChat.newChat') }}</el-button>
          <el-select v-model="selectedModelId" size="small" style="width:120px" @change="handleModelChange">
            <el-option v-for="m in aiModels" :key="m.id" :label="m.modelName" :value="m.id" />
          </el-select>
          <el-button text size="small" @click="chatVisible = false">✕</el-button>
        </div>
      </div>
      <div class="chat-messages" ref="chatMessagesRef">
        <div v-for="(msg, idx) in chatMessages" :key="idx" :class="['chat-msg', msg.role]">
          {{ msg.content }}
        </div>
        <div v-if="chatLoading" class="chat-msg assistant">{{ t('aiChat.thinking') }}</div>
      </div>
      <div class="chat-input">
        <input v-model="chatInput" @keyup.enter="sendChat" :placeholder="t('aiChat.assistantPlaceholder')" />
        <button @click="sendChat" :disabled="!chatInput.trim()">{{ t('aiChat.send') }}</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { sendChatMessage, createConversation, getConversations, getConversationMessages, getAiModelList } from '../api'
import { useI18n } from '../utils/i18n'
import { ElSelect, ElOption, ElButton } from 'element-plus'

const { t } = useI18n()

const closed = ref(false)
const fullyClosed = ref(false)
const chatVisible = ref(false)
const chatInput = ref('')
const chatMessages = ref([])
const chatLoading = ref(false)
const chatMessagesRef = ref(null)
const showTip = ref(false)

// 模型选择
const aiModels = ref([])
const selectedModelId = ref(null)

// 拖拽相关
const posX = ref(window.innerWidth - 100)
const posY = ref(window.innerHeight - 100)
const isDragging = ref(false)
const dragOffsetX = ref(0)
const dragOffsetY = ref(0)
const hasDragged = ref(false)

// 柴犬表情和卖萌语录
const emojis = ['🐕', '🐶', '🦴', '🐾', '😌', '😴', '🥰', '😋']
const tips = ['汪！有什么可以帮你的？', '今天也要开心哦~', '摸摸头~', '我在这里等你~', '汪汪！', '想聊点什么？']
const currentEmoji = ref('🐕')
const tipText = ref(tips[0])
const tipIndex = ref(0)

let convId = null

const assistantStyle = computed(() => ({
  left: posX.value + 'px',
  top: posY.value + 'px'
}))

// 定时换表情和语录
let emojiTimer = null
onMounted(() => {
  emojiTimer = setInterval(() => {
    tipIndex.value = (tipIndex.value + 1) % tips.length
    tipText.value = tips[tipIndex.value]
    currentEmoji.value = emojis[Math.floor(Math.random() * emojis.length)]
  }, 8000)
  loadModels()
})

onUnmounted(() => { if (emojiTimer) clearInterval(emojiTimer) })

function startDrag(e) {
  isDragging.value = true
  hasDragged.value = false
  dragOffsetX.value = e.clientX - posX.value
  dragOffsetY.value = e.clientY - posY.value
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
}

function onDrag(e) {
  if (!isDragging.value) return
  hasDragged.value = true
  posX.value = e.clientX - dragOffsetX.value
  posY.value = e.clientY - dragOffsetY.value
}

function stopDrag() {
  isDragging.value = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}

function closeAssistant() {
  closed.value = true
  chatVisible.value = false
  // 移动到右侧半隐藏
  posX.value = window.innerWidth - 30
  posY.value = window.innerHeight / 2
}

function reopenAssistant() {
  closed.value = false
  posX.value = window.innerWidth - 100
  posY.value = window.innerHeight - 100
}

async function loadModels() {
  try {
    const res = await getAiModelList()
    if (res.code === 200) {
      aiModels.value = (res.data || []).filter(m => m.status === 1)
      const defaultModel = aiModels.value.find(m => m.isDefault === 1)
      if (defaultModel) selectedModelId.value = defaultModel.id
    }
  } catch {}
}

async function handleModelChange(modelId) {
  // 模型切换会在下次发消息时生效
}

async function startNewChat() {
  try {
    const convRes = await createConversation({ title: t('aiChat.assistantChatTitle') })
    if (convRes.code === 200 && convRes.data) {
      convId = convRes.data.id
      chatMessages.value = []
    }
  } catch {}
}

async function toggleChat() {
  if (hasDragged.value) return
  // 如果是关闭状态，先恢复
  if (closed.value) {
    closed.value = false
    posX.value = window.innerWidth - 100
    posY.value = window.innerHeight - 100
    return
  }
  chatVisible.value = !chatVisible.value
  if (chatVisible.value && !convId) {
    try {
      const res = await getConversations()
      if (res.code === 200 && res.data && res.data.length > 0) {
        convId = res.data[0].id
        // 加载历史消息
        const msgRes = await getConversationMessages(convId)
        if (msgRes.code === 200 && msgRes.data) chatMessages.value = msgRes.data
      } else {
        const convRes = await createConversation({ title: t('aiChat.assistantChatTitle') })
        if (convRes.code === 200 && convRes.data) convId = convRes.data.id
      }
    } catch {}
  }
}

async function sendChat() {
  if (!chatInput.value.trim() || chatLoading.value) return
  const text = chatInput.value
  chatInput.value = ''
  chatMessages.value.push({ role: 'user', content: text, id: Date.now() })
  chatLoading.value = true
  try {
    if (!convId) {
      const convRes = await createConversation({ title: t('aiChat.assistantChatTitle') })
      if (convRes.code === 200 && convRes.data) convId = convRes.data.id
      else { chatLoading.value = false; return }
    }
    const res = await sendChatMessage(convId, { content: text })
    if (res.code === 200 && res.data) chatMessages.value.push({ role: 'assistant', content: res.data.content, id: Date.now() + 1 })
    else chatMessages.value.push({ role: 'assistant', content: t('aiChat.assistantError'), id: Date.now() + 1 })
  } catch {
    chatMessages.value.push({ role: 'assistant', content: t('aiChat.assistantErrorRetry'), id: Date.now() + 1 })
  }
  chatLoading.value = false
  if (chatMessagesRef.value) chatMessagesRef.value.scrollTop = chatMessagesRef.value.scrollHeight
}
</script>

<style scoped>
.floating-assistant {
  position: fixed;
  z-index: 10000;
  cursor: grab;
  user-select: none;
  transition: opacity 0.3s;
}
.floating-assistant.minimized { opacity: 0.6; }
.floating-assistant.minimized:hover { opacity: 1; }
.floating-assistant:active { cursor: grabbing; }
.shiba-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffecd2, #fcb69f);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 20px rgba(0,0,0,0.15);
  cursor: pointer;
  transition: transform 0.2s;
  position: relative;
}
.floating-assistant.minimized .shiba-avatar { width: 40px; height: 40px; }
.floating-assistant.minimized .shiba-emoji { font-size: 20px; }
.shiba-avatar:hover { transform: scale(1.1); }
.shiba-emoji { font-size: 28px; }
.close-btn {
  position: absolute;
  top: -4px;
  right: -4px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #f56c6c;
  color: white;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.shiba-tip {
  position: absolute;
  bottom: 64px;
  right: 0;
  background: white;
  padding: 8px 12px;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.1);
  font-size: 13px;
  white-space: nowrap;
  color: #333;
}
.shiba-tip::after {
  content: '';
  position: absolute;
  bottom: -6px;
  right: 20px;
  border-left: 6px solid transparent;
  border-right: 6px solid transparent;
  border-top: 6px solid white;
}
.chat-popup {
  position: absolute;
  bottom: 64px;
  right: 0;
  width: 320px;
  height: 400px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 8px 40px rgba(0,0,0,0.2);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.chat-header {
  padding: 12px 16px;
  background: linear-gradient(135deg, #ffecd2, #fcb69f);
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}
.chat-msg {
  margin-bottom: 8px;
  padding: 8px 12px;
  border-radius: 12px;
  font-size: 13px;
  max-width: 85%;
  line-height: 1.4;
}
.chat-msg.user {
  background: #409eff;
  color: white;
  margin-left: auto;
  border-radius: 12px 12px 0 12px;
}
.chat-msg.assistant {
  background: #f0f0f0;
  color: #333;
  border-radius: 12px 12px 12px 0;
}
.chat-input {
  padding: 8px 12px;
  border-top: 1px solid #eee;
  display: flex;
  gap: 8px;
}
.chat-input input {
  flex: 1;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 6px 10px;
  font-size: 13px;
  outline: none;
}
.chat-input input:focus { border-color: #409eff; }
.chat-input button {
  background: #409eff;
  color: white;
  border: none;
  border-radius: 8px;
  padding: 6px 12px;
  cursor: pointer;
  font-size: 13px;
}
.chat-input button:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
