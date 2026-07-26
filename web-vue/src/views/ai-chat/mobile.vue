<template>
  <div class="mobile-ai-chat" :key="lang">
    <header class="chat-header">
      <span>{{ t('aiChat.newChat') }}</span>
    </header>
    <div class="chat-messages" ref="messagesRef">
      <div v-for="msg in messages" :key="msg.id" :class="['message', msg.role]">
        <div class="message-content">{{ msg.content }}</div>
      </div>
      <div v-if="messages.length === 0" class="chat-empty">{{ t('aiChat.emptyHint') }}</div>
    </div>
    <div class="chat-input">
      <el-input v-model="inputText" :placeholder="t('aiChat.inputPlaceholder')" @keyup.enter="send" :disabled="!currentConv" />
      <el-button @click="send" :disabled="!currentConv || !inputText" type="primary" circle><el-icon><Promotion /></el-icon></el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { getConversations, createConversation, getConversationMessages, sendChatMessage } from '../../api'
import { useI18n } from '../../utils/i18n'

const { t, lang } = useI18n()
const messages = ref([])
const inputText = ref('')
const messagesRef = ref(null)
const currentConv = ref(null)
const sending = ref(false)

onMounted(async () => {
  const res = await getConversations()
  if (res.code === 200 && res.data.length > 0) {
    currentConv.value = res.data[0]
    const msgRes = await getConversationMessages(res.data[0].id)
    if (msgRes.code === 200) messages.value = msgRes.data
    scrollToBottom()
  } else {
    const convRes = await createConversation({ title: t('aiChat.mobileChatTitle') })
    if (convRes.code === 200) currentConv.value = convRes.data
  }
})

async function send() {
  if (!inputText.value.trim() || !currentConv.value || sending.value) return
  sending.value = true
  const text = inputText.value
  inputText.value = ''
  messages.value.push({ role: 'user', content: text, id: Date.now() })
  scrollToBottom()
  try {
    const res = await sendChatMessage(currentConv.value.id, { content: text })
    if (res.code === 200) { messages.value.push(res.data); scrollToBottom() }
  } finally { sending.value = false }
}

function scrollToBottom() { nextTick(() => { if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight }) }
</script>

<style scoped>
.mobile-ai-chat { display: flex; flex-direction: column; height: calc(100vh - 100px); }
.chat-header { padding: 12px 16px; font-weight: bold; border-bottom: 1px solid #eee; }
.chat-messages { flex: 1; overflow-y: auto; padding: 16px; }
.chat-empty { text-align: center; color: #999; padding: 60px 0; }
.message { margin-bottom: 12px; }
.message.user .message-content { background: #409eff; color: white; margin-left: 20%; border-radius: 12px 12px 0 12px; }
.message.assistant .message-content { background: #f0f0f0; color: #333; margin-right: 20%; border-radius: 12px 12px 12px 0; }
.message-content { padding: 10px 14px; display: inline-block; max-width: 100%; white-space: pre-wrap; word-break: break-word; font-size: 14px; }
.chat-input { padding: 12px 16px; border-top: 1px solid #eee; display: flex; gap: 8px; }
</style>
