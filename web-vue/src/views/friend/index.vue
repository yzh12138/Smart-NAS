<template>
  <div class="friend-page" :key="lang">
    <el-row :gutter="20">
      <el-col :span="16">
        <el-card>
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>{{ t('friend.myFriends') }}</span>
              <el-button type="primary" size="small" @click="searchDialogVisible = true">{{ t('friend.addFriend') }}</el-button>
            </div>
          </template>
          <el-table :data="friends" stripe v-if="friends.length > 0">
            <el-table-column :label="t('friend.username')" width="120">
              <template #default="{ row }">{{ row.username }}</template>
            </el-table-column>
            <el-table-column :label="t('friend.nickname')">
              <template #default="{ row }">{{ row.nickname || '-' }}</template>
            </el-table-column>
            <el-table-column :label="t('friend.addTime')" width="180">
              <template #default="{ row }">{{ formatTime(row.create_time) }}</template>
            </el-table-column>
            <el-table-column :label="t('family.actions')" width="120">
              <template #default="{ row }">
                <el-popconfirm :title="t('friend.confirmRemove')" @confirm="handleRemoveFriend(row.friend_id === currentUserId ? row.user_id : row.friend_id)">
                  <template #reference><el-button size="small" type="danger">{{ t('friend.remove') }}</el-button></template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else :description="t('friend.noFriends')" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>{{ t('friend.pendingRequests') }} ({{ pendingRequests.length }})</template>
          <div v-for="req in pendingRequests" :key="req.id" class="pending-item">
            <div>
              <strong>{{ req.username }}</strong>
              <span style="color:#999;font-size:12px;margin-left:8px">{{ req.nickname }}</span>
            </div>
            <div style="margin-top:8px">
              <el-button size="small" type="success" @click="handleAccept(req.id)">{{ t('family.approve') }}</el-button>
              <el-button size="small" type="danger" @click="handleReject(req.id)">{{ t('family.reject') }}</el-button>
            </div>
          </div>
          <el-empty v-if="pendingRequests.length === 0" :description="t('friend.noPending')" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索好友弹窗 -->
    <el-dialog v-model="searchDialogVisible" :title="t('friend.searchFriend')" width="500px">
      <el-input v-model="searchKeyword" :placeholder="t('friend.searchPlaceholder')" clearable @keyup.enter="handleSearch" style="margin-bottom:16px">
        <template #append>
          <el-button @click="handleSearch">{{ t('common.search') }}</el-button>
        </template>
      </el-input>
      <el-table :data="searchResults" stripe v-if="searchResults.length > 0">
        <el-table-column prop="username" :label="t('friend.username')" />
        <el-table-column prop="nickname" :label="t('friend.nickname')" />
        <el-table-column :label="t('family.actions')" width="120">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleSendRequest(row.id)">{{ t('friend.sendRequest') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getFriendList, getPendingFriendRequests, acceptFriendRequest, rejectFriendRequest, removeFriend, sendFriendRequest, searchUsers } from '../../api'
import { useI18n } from '../../utils/i18n'
import { ElMessage } from 'element-plus'

const { t, lang } = useI18n()
const friends = ref([])
const pendingRequests = ref([])
const currentUserId = ref(null)
const searchDialogVisible = ref(false)
const searchKeyword = ref('')
const searchResults = ref([])

onMounted(async () => {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  currentUserId.value = userInfo.userId || null
  loadData()
})

async function loadData() {
  const [friendsRes, pendingRes] = await Promise.all([getFriendList(), getPendingFriendRequests()])
  if (friendsRes.code === 200) friends.value = friendsRes.data || []
  if (pendingRes.code === 200) pendingRequests.value = pendingRes.data || []
}

async function handleSearch() {
  if (!searchKeyword.value.trim()) return
  const res = await searchUsers(searchKeyword.value.trim())
  if (res.code === 200) searchResults.value = res.data || []
}

async function handleSendRequest(friendId) {
  await sendFriendRequest(friendId)
  ElMessage.success(t('friend.requestSent'))
  searchDialogVisible.value = false
  searchKeyword.value = ''
  searchResults.value = []
}

async function handleAccept(id) {
  await acceptFriendRequest(id)
  ElMessage.success(t('common.success'))
  loadData()
}

async function handleReject(id) {
  await rejectFriendRequest(id)
  ElMessage.success(t('common.success'))
  loadData()
}

async function handleRemoveFriend(friendId) {
  await removeFriend(friendId)
  ElMessage.success(t('common.success'))
  loadData()
}

function formatTime(t) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}
</script>

<style scoped>
.pending-item { padding: 12px; border-bottom: 1px solid #f0f0f0; }
.pending-item:last-child { border-bottom: none; }
</style>
