package com.smartnas.app.ui.screens.aichat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartnas.app.data.api.SmartNASApi
import com.smartnas.app.data.model.ChatMessage
import com.smartnas.app.data.model.Conversation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val api: SmartNASApi
) : ViewModel() {

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending

    fun loadConversations() {
        viewModelScope.launch {
            try {
                val response = api.getConversations()
                if (response.isSuccessful && response.body()?.code == 0) {
                    _conversations.value = response.body()?.data ?: emptyList()
                }
            } catch (_: Exception) {}
        }
    }

    fun createConversation(title: String = "新对话", onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val response = api.createConversation(mapOf("title" to title))
                if (response.isSuccessful && response.body()?.code == 0) {
                    val conv = response.body()!!.data!!
                    loadConversations()
                    onCreated(conv.id)
                }
            } catch (_: Exception) {}
        }
    }

    fun loadMessages(conversationId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api.getConversationMessages(conversationId)
                if (response.isSuccessful && response.body()?.code == 0) {
                    _messages.value = response.body()?.data ?: emptyList()
                }
            } catch (_: Exception) {}
            _isLoading.value = false
        }
    }

    fun sendMessage(conversationId: Long, content: String, imageUrl: String? = null) {
        viewModelScope.launch {
            _isSending.value = true
            try {
                val body = mutableMapOf<String, Any?>("content" to content)
                if (imageUrl != null) body["imageUrl"] = imageUrl

                val response = api.sendChatMessage(conversationId, body)
                if (response.isSuccessful && response.body()?.code == 0) {
                    val reply = response.body()!!.data
                    if (reply != null) {
                        _messages.value = _messages.value + listOf(
                            ChatMessage(role = "user", content = content, imageUrl = imageUrl),
                            reply
                        )
                    }
                }
            } catch (_: Exception) {}
            _isSending.value = false
        }
    }

    fun deleteConversation(id: Long) {
        viewModelScope.launch {
            try {
                api.deleteConversation(id)
