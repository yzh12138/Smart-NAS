<template>
  <div class="family-manage" :key="lang">
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>{{ t('family.manageTitle') }}</span>
          <el-button type="primary" size="small" @click="showCreate">{{ t('family.create') }}</el-button>
        </div>
      </template>
      <el-table :data="families" stripe>
        <el-table-column prop="familyName" :label="t('family.name')" />
        <el-table-column :label="t('family.desc')">
          <template #default="{ row }">{{ row.description || '-' }}</template>
        </el-table-column>
        <el-table-column :label="t('family.actions')" width="300">
          <template #default="{ row }">
            <el-button size="small" link @click="showMembers(row)">{{ t('family.members') }}</el-button>
            <el-button v-if="row.ownerId === currentUserId || isAdmin(row.id)" size="small" link @click="showInvite(row)">{{ t('family.inviteFriend') }}</el-button>
            <el-button v-if="row.ownerId === currentUserId || isAdmin(row.id)" size="small" link @click="showEdit(row)">{{ t('common.edit') }}</el-button>
            <el-popconfirm v-if="row.ownerId !== currentUserId" :title="t('family.confirmExit')" @confirm="handleExit(row.id)">
              <template #reference><el-button size="small" type="warning" link>{{ t('family.exit') }}</el-button></template>
            </el-popconfirm>
            <el-popconfirm v-if="row.ownerId === currentUserId || isAdmin(row.id)" :title="t('family.confirmDissolve')" @confirm="handleDissolve(row.id)">
              <template #reference><el-button size="small" type="danger" link>{{ t('family.dissolve') }}</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="createVisible" :title="editingFamily ? t('family.editFamily') : t('family.createFamily')" width="480px">
      <el-form label-width="80px">
        <el-form-item :label="t('family.name')"><el-input v-model="form.name" /></el-form-item>
        <el-form-item :label="t('family.desc')"><el-input v-model="form.description" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleSave">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="membersVisible" :title="t('family.memberManage')" width="600px">
      <el-tabs v-model="memberTab">
        <el-tab-pane :label="t('family.approved')" name="approved">
          <el-table :data="approvedMembers" stripe>
            <el-table-column prop="userId" :label="t('family.userId')" />
            <el-table-column prop="memberName" :label="t('family.memberName')" />
            <el-table-column prop="role" :label="t('family.role')" />
            <el-table-column :label="t('family.actions')" width="100">
              <template #default="{ row }">
                <el-button size="small" type="danger" link @click="handleRemove(row.userId)">{{ t('common.delete') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane :label="t('family.pending')" name="pending">
          <el-table :data="pendingMembers" stripe>
            <el-table-column prop="userId" :label="t('family.userId')" />
            <el-table-column :label="t('family.actions')" width="200">
              <template #default="{ row }">
                <el-button size="small" type="success" link @click="handleApprove(row.id)">{{ t('family.approve') }}</el-button>
                <el-button size="small" type="danger" link @click="handleReject(row.id)">{{ t('family.reject') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <el-dialog v-model="inviteVisible" :title="t('family.inviteTitle')" width="480px">
      <el-input v-model="friendSearch" :placeholder="t('family.searchFriend')" clearable style="margin-bottom:16px" />
      <div v-if="filteredFriends.length === 0" style="text-align:center;color:#999;padding:20px">{{ t('family.noFriends') }}</div>
      <div v-else style="max-height:300px;overflow-y:auto">
        <div v-for="friend in filteredFriends" :key="friend.userId" style="display:flex;justify-content:space-between;align-items:center;padding:10px;border-bottom:1px solid #f0f0f0">
          <div>
            <span>{{ friend.nickname || friend.username || ('用户' + friend.userId) }}</span>
          </div>
          <el-button size="small" type="primary" @click="handleInvite(friend.userId)">{{ t('family.sendInvite') }}</el-button>
        </div>
      </div>
      <template #footer>
        <el-button @click="inviteVisible = false">{{ t('common.cancel') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { getMyFamilies, createFamily, updateFamily, dissolveFamily, getFamilyMembers, getFamilyPending, approveMember, rejectMember, removeMember, getFriendList, inviteFamilyMember } from '../../api'
import { useI18n } from '../../utils/i18n'
import { ElMessage } from 'element-plus'

const { t, lang } = useI18n()
const families = ref([])
const createVisible = ref(false)
const editingFamily = ref(null)
const form = ref({ name: '', description: '' })
const membersVisible = ref(false)
const memberTab = ref('approved')
const currentFamilyId = ref(null)
const approvedMembers = ref([])
const pendingMembers = ref([])
const currentUserId = ref(null)
const familyRoles = ref({})
const inviteVisible = ref(false)
const inviteFamilyId = ref(null)
const friendList = ref([])
const friendSearch = ref('')

const filteredFriends = computed(() => {
  if (!friendSearch.value) return friendList.value
  const kw = friendSearch.value.toLowerCase()
  return friendList.value.filter(f => (f.nickname || f.username || '').toLowerCase().includes(kw) || String(f.userId).includes(kw))
})

onMounted(() => {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  currentUserId.value = userInfo.userId || null
  loadData()
})

function isAdmin(familyId) {
  return familyRoles.value[familyId] === 'admin'
}

async function loadData() {
  const res = await getMyFamilies()
  if (res.code === 200) {
    families.value = res.data
    for (const f of res.data) {
      const membersRes = await getFamilyMembers(f.id)
      if (membersRes.code === 200) {
        const myMember = membersRes.data.find(m => m.userId === currentUserId.value)
        if (myMember) familyRoles.value[f.id] = myMember.role
      }
    }
  }
}

function showCreate() { editingFamily.value = null; form.value = { name: '', description: '' }; createVisible.value = true }
function showEdit(family) { editingFamily.value = family; form.value = { name: family.familyName, description: family.description }; createVisible.value = true }

async function showInvite(family) {
  inviteFamilyId.value = family.id
  friendSearch.value = ''
  const res = await getFriendList()
  friendList.value = res.code === 200 ? res.data : []
  inviteVisible.value = true
}

async function handleInvite(friendId) {
  await inviteFamilyMember(inviteFamilyId.value, friendId)
  ElMessage.success(t('family.inviteSuccess'))
  friendList.value = friendList.value.filter(f => f.userId !== friendId)
}

async function handleSave() {
  if (editingFamily.value) {
    await updateFamily(editingFamily.value.id, form.value)
    ElMessage.success(t('common.success'))
  } else {
    await createFamily(form.value)
    ElMessage.success(t('common.success'))
  }
  createVisible.value = false; loadData()
}

async function handleDissolve(id) { await dissolveFamily(id); ElMessage.success(t('family.dissolved')); loadData() }

async function handleExit(familyId) {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  await removeMember(familyId, userInfo.userId || 1)
  ElMessage.success(t('family.exited'))
  loadData()
}

async function showMembers(family) {
  currentFamilyId.value = family.id
  const [approved, pending] = await Promise.all([getFamilyMembers(family.id), getFamilyPending(family.id)])
  approvedMembers.value = approved.code === 200 ? approved.data : []
  pendingMembers.value = pending.code === 200 ? pending.data : []
  membersVisible.value = true
}

async function handleApprove(memberId) { await approveMember(memberId); ElMessage.success(t('family.approved')); showMembers({ id: currentFamilyId.value }) }
async function handleReject(memberId) { await rejectMember(memberId); ElMessage.success(t('family.rejected')); showMembers({ id: currentFamilyId.value }) }
async function handleRemove(userId) { await removeMember(currentFamilyId.value, userId); ElMessage.success(t('family.removed')); showMembers({ id: currentFamilyId.value }) }
</script>
