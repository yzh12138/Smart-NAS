<template>
  <div class="role-manage-page" :key="lang">
    <el-card style="margin-bottom:20px">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>{{ t('roleManage.userManagement') }}</span>
          <el-button type="primary" size="small" @click="showUserDialog()">{{ t('common.create') }}</el-button>
        </div>
      </template>
      <el-table :data="userList" stripe>
        <el-table-column prop="username" :label="t('friend.username')" width="120" />
        <el-table-column prop="nickname" :label="t('friend.nickname')" width="120" />
        <el-table-column :label="t('roleManage.role')" width="150">
          <template #default="{ row }">
            <el-tag v-for="role in row.roles" :key="role.id" size="small" style="margin-right:4px">{{ role.roleName }}</el-tag>
            <span v-if="!row.roles || row.roles.length === 0" style="color:#999">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('roleManage.status')" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? t('roleManage.active') : t('roleManage.disabled') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="t('friend.addTime')" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column :label="t('family.actions')" width="200">
          <template #default="{ row }">
            <el-button size="small" @click="showUserDialog(row)">{{ t('common.edit') }}</el-button>
            <el-button size="small" @click="showAssignRole(row)">{{ t('roleManage.assignRole') }}</el-button>
            <el-popconfirm :title="t('common.confirm')" @confirm="handleDeleteUser(row.id)">
              <template #reference><el-button size="small" type="danger">{{ t('common.delete') }}</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="userPage" :page-size="10" :total="userTotal"
        layout="total, prev, pager, next" style="margin-top:16px;justify-content:flex-end"
        @current-change="loadUsers"
      />
    </el-card>

    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>{{ t('roleManage.roleManagement') }}</span>
          <el-button type="primary" size="small" @click="showRoleDialog()">{{ t('common.create') }}</el-button>
        </div>
      </template>
      <el-table :data="roleList" stripe>
        <el-table-column prop="roleName" :label="t('roleManage.roleName')" />
        <el-table-column prop="roleKey" :label="t('roleManage.roleKey')" />
        <el-table-column :label="t('family.actions')" width="160">
          <template #default="{ row }">
            <el-button size="small" @click="showRoleDialog(row)">{{ t('common.edit') }}</el-button>
            <el-popconfirm :title="t('common.confirm')" @confirm="handleDeleteRole(row.id)">
              <template #reference><el-button size="small" type="danger">{{ t('common.delete') }}</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 用户编辑弹窗 -->
    <el-dialog v-model="userDialogVisible" :title="editingUser ? t('common.edit') : t('common.create')" width="480px">
      <el-form label-width="80px">
        <el-form-item :label="t('friend.username')">
          <el-input v-model="userForm.username" :disabled="!!editingUser" />
        </el-form-item>
        <el-form-item :label="t('profile.password')">
          <el-input v-model="userForm.password" type="password" show-password :placeholder="editingUser ? t('profile.passwordHint') : ''" />
        </el-form-item>
        <el-form-item :label="t('friend.nickname')">
          <el-input v-model="userForm.nickname" />
        </el-form-item>
        <el-form-item :label="t('roleManage.status')">
          <el-switch v-model="userForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleSaveUser">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- 角色编辑弹窗 -->
    <el-dialog v-model="roleDialogVisible" :title="editingRole ? t('common.edit') : t('common.create')" width="400px">
      <el-form label-width="80px">
        <el-form-item :label="t('roleManage.roleName')">
          <el-input v-model="roleForm.roleName" />
        </el-form-item>
        <el-form-item :label="t('roleManage.roleKey')">
          <el-input v-model="roleForm.roleKey" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleSaveRole">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色弹窗 -->
    <el-dialog v-model="assignRoleVisible" :title="t('roleManage.assignRole')" width="400px">
      <el-select v-model="selectedRoleId" :placeholder="t('roleManage.selectRole')" style="width:100%">
        <el-option v-for="r in roleList" :key="r.id" :label="r.roleName" :value="r.id" />
      </el-select>
      <template #footer>
        <el-button @click="assignRoleVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleAssignRole">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getUserList, createUser, updateUser, deleteUser, getRoleList, createRole, updateRole, deleteRole } from '../../api'
import { useI18n } from '../../utils/i18n'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'

const { t, lang } = useI18n()

// 用户管理
const userList = ref([])
const userPage = ref(1)
const userTotal = ref(0)
const userDialogVisible = ref(false)
const editingUser = ref(null)
const userForm = ref({ username: '', password: '', nickname: '', status: 1 })

// 角色管理
const roleList = ref([])
const roleDialogVisible = ref(false)
const editingRole = ref(null)
const roleForm = ref({ roleName: '', roleKey: '' })

// 分配角色
const assignRoleVisible = ref(false)
const assignUserId = ref(null)
const selectedRoleId = ref(null)

onMounted(() => { loadUsers(); loadRoles() })

async function loadUsers() {
  const res = await getUserList({ page: userPage.value, size: 10 })
  if (res.code === 200) {
    const records = res.data.records || []
    // 加载每个用户的角色
    for (const u of records) {
      try {
        const roleRes = await request.get(`/api/auth/info`, { headers: { 'X-User-Id': u.id } })
        u.roles = roleRes.code === 200 ? roleRes.data.roles || [] : []
      } catch { u.roles = [] }
    }
    userList.value = records
    userTotal.value = res.data.total || 0
  }
}

async function loadRoles() {
  const res = await getRoleList()
  if (res.code === 200) roleList.value = res.data || []
}

function showUserDialog(row) {
  if (row) {
    editingUser.value = row
    userForm.value = { username: row.username, password: '', nickname: row.nickname, status: row.status }
  } else {
    editingUser.value = null
    userForm.value = { username: '', password: '', nickname: '', status: 1 }
  }
  userDialogVisible.value = true
}

async function handleSaveUser() {
  if (editingUser.value) {
    const data = { ...userForm.value }
    if (!data.password) delete data.password
    await updateUser(editingUser.value.id, data)
  } else {
    await createUser(userForm.value)
  }
  ElMessage.success(t('common.success'))
  userDialogVisible.value = false
  loadUsers()
}

async function handleDeleteUser(id) {
  await deleteUser(id)
  ElMessage.success(t('common.success'))
  loadUsers()
}

function showRoleDialog(row) {
  if (row) {
    editingRole.value = row
    roleForm.value = { roleName: row.roleName, roleKey: row.roleKey }
  } else {
    editingRole.value = null
    roleForm.value = { roleName: '', roleKey: '' }
  }
  roleDialogVisible.value = true
}

async function handleSaveRole() {
  if (editingRole.value) {
    await updateRole(editingRole.value.id, roleForm.value)
  } else {
    await createRole(roleForm.value)
  }
  ElMessage.success(t('common.success'))
  roleDialogVisible.value = false
  loadRoles()
}

async function handleDeleteRole(id) {
  await deleteRole(id)
  ElMessage.success(t('common.success'))
  loadRoles()
}

function showAssignRole(row) {
  assignUserId.value = row.id
  selectedRoleId.value = row.roles && row.roles.length > 0 ? row.roles[0].id : null
  assignRoleVisible.value = true
}

async function handleAssignRole() {
  if (!selectedRoleId.value) return
  await request.post(`/api/system/user/${assignUserId.value}/role`, { roleId: selectedRoleId.value })
  ElMessage.success(t('common.success'))
  assignRoleVisible.value = false
  loadUsers()
}

function formatTime(t) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}
</script>

<style scoped>
.role-manage-page { max-width: 1000px; margin: 0 auto; }
</style>
