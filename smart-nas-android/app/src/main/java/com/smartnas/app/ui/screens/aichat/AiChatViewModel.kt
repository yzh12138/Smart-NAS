package com.smartnas.app.ui.screens.aichat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartnas.app.data.api.SmartNASApi
import com.smartnas.app.data.model.*
import com.smartnas.app.util.Resource
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

    private val _sendState = MutableStateFlow<Resource<ChatMessage>>(Resource.Loading)
    val sendState: StateFlow<Resource<ChatMessage>> = _sendState

    fun loadConversations() {
        viewModelScope.launch {
            try {
                val resp = api.getConversations()
                if (resp.isSuccessful && resp.body()?.code == 200) {
                    _conversations.value = resp.body()!!.data ?: emptyList()
                }
            } catch (_: Exception) {}
        }
    }

    fun createConversation(title: String? = null) {
        viewModelScope.launch {
            try {
                val body = title?.let { mapOf("title" to it) }
                api.createConversation(body)
                loadConversations()
            } catch (_: Exception) {}
        }
    }

    fun deleteConversation(id: Long) {
        viewModelScope.launch {
            try {
                api.deleteConversation(id)
                loadConversations()
            } catch (_: Exception) {}
        }
    }

    fun loadMessages(conversationId: Long) {
        viewModelScope.launch {
            try {
                val resp = api.getConversationMessages(conversationId)
                if (resp.isSuccessful && resp.body()?.code == 200) {
                    _messages.value = resp.body()!!.data ?: emptyList()
                }
            } catch (_: Exception) {}
        }
    }

    fun sendMessage(conversationId: Long, content: String) {
        viewModelScope.launch {
            _sendState.value = Resource.Loading
            try {
                val resp = api.sendChatMessage(conversationId, mapOf("content" to content))
                if (resp.isSuccessful && resp.body()?.code == 200) {
                    _sendState.value = Resource.Success(resp.body()!!.data!!)
                    loadMessages(conversationId)
                } else {
                    _sendState.value = Resource.Error(resp.body()?.message ?: "发送失败")
                }
            } catch (e: Exception) {
                _sendState.value = Resource.Error(e.message ?: "网络错误")
            }
        }
    }
}
