<template>
  <div class="task-page" :key="lang">
    <el-card>
      <template #header>{{ t('task.title') }}</template>
      <el-tabs v-model="activeTab" @tab-change="loadData">
        <el-tab-pane :label="t('task.familyJoin')" name="familyJoin">
          <el-table :data="familyRequests" stripe v-if="familyRequests.length > 0">
            <el-table-column prop="username" :label="t('task.申请人')" width="120" />
            <el-table-column :label="t('task.family')" width="150">
              <template #default="{ row }">
                <span v-if="row.familyName">{{ row.familyName }}</span>
                <span v-else style="color:#999">-</span>
              </template>
            </el-table-column>
            <el-table-column :label="t('task.time')" width="180">
              <template #default="{ row }">{{ formatTime(row.create_time) }}</template>
            </el-table-column>
            <el-table-column :label="t('family.actions')" width="200">
              <template #default="{ row }">
                <el-button size="small" type="success" @click="handleApproveFamily(row)">{{ t('family.approve') }}</el-button>
                <el-button size="small" type="danger" @click="handleRejectFamily(row)">{{ t('family.reject') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else :description="t('task.noPending')" />
        </el-tab-pane>
        <el-tab-pane :label="t('task.friendRequest')" name="friendRequest">
          <el-table :data="friendRequests" stripe v-if="friendRequests.length > 0">
            <el-table-column prop="username" :label="t('task.申请人')" width="120" />
            <el-table-column prop="nickname" :label="t('task.nickname')" />
            <el-table-column :label="t('task.time')" width="180">
              <template #default="{ row }">{{ formatTime(row.create_time) }}</template>
            </el-table-column>
            <el-table-column :label="t('family.actions')" width="200">
              <template #default="{ row }">
                <el-button size="small" type="success" @click="handleApproveFriend(row.id)">{{ t('family.approve') }}</el-button>
                <el-button size="small" type="danger" @click="handleRejectFriend(row.id)">{{ t('family.reject') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else :description="t('task.noPending')" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMyFamilies, getFamilyPending, approveMember, rejectMember } from '../../api'
import { getPendingFriendRequests, acceptFriendRequest, rejectFriendRequest } from '../../api'
import { useI18n } from '../../utils/i18n'
import { ElMessage } from 'element-plus'

const { t, lang } = useI18n()
const activeTab = ref('familyJoin')
const familyRequests = ref([])
const friendRequests = ref([])

onMounted(() => loadData())

async function loadData() {
  await loadFamilyRequests()
  await loadFriendRequests()
}

async function loadFamilyRequests() {
  try {
    const familyRes = await getMyFamilies()
    if (familyRes.code !== 200) return
    const allPending = []
    for (const f of (familyRes.data || [])) {
      try {
        const pendingRes = await getFamilyPending(f.id)
        if (pendingRes.code === 200 && pendingRes.data) {
          for (const m of pendingRes.data) {
            allPending.push({ ...m, familyName: f.familyName, familyId: f.id })
          }
        }
      } catch {}
    }
    familyRequests.value = allPending
  } catch { familyRequests.value = [] }
}

async function loadFriendRequests() {
  try {
    const res = await getPendingFriendRequests()
    if (res.code === 200) friendRequests.value = res.data || []
  } catch { friendRequests.value = [] }
}

async function handleApproveFamily(row) {
  await approveMember(row.id)
  ElMessage.success(t('family.approved'))
  loadFamilyRequests()
}

async function handleRejectFamily(row) {
  await rejectMember(row.id)
  ElMessage.success(t('family.rejected'))
  loadFamilyRequests()
}

async function handleApproveFriend(id) {
  await acceptFriendRequest(id)
  ElMessage.success(t('family.approved'))
  loadFriendRequests()
}

async function handleRejectFriend(id) {
  await rejectFriendRequest(id)
  ElMessage.success(t('family.rejected'))
  loadFriendRequests()
}

function formatTime(t) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}
</script>

<style scoped>
.task-page { max-width: 900px; margin: 0 auto; }
</style>
