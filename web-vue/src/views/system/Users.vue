<template>
  <div class="users-page" :key="lang">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ t('roleManage.userManagement') }}</span>
          <el-button type="primary" @click="showDialog()">{{ t('common.create') }}</el-button>
        </div>
      </template>
      <el-table :data="userList" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" :label="t('friend.username')" />
        <el-table-column prop="nickname" :label="t('friend.nickname')" />
        <el-table-column prop="status" :label="t('roleManage.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? t('roleManage.active') : t('roleManage.disabled') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="t('common.createTime')" width="180" />
        <el-table-column :label="t('family.actions')" width="180">
          <template #default="{ row }">
            <el-button size="small" @click="showDialog(row)">{{ t('common.edit') }}</el-button>
            <el-popconfirm :title="t('tagManage.confirmDelete')" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button size="small" type="danger">{{ t('common.delete') }}</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="page"
        :page-size="10"
        :total="total"
        layout="total, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @current-change="loadUsers"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? t('common.edit') : t('common.create')" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item :label="t('friend.username')">
          <el-input v-model="form.username" :disabled="isEdit" />
        </el-form-item>
        <el-form-item :label="t('profile.password')">
          <el-input v-model="form.password" type="password" show-password :placeholder="isEdit ? t('profile.passwordHint') : ''" />
        </el-form-item>
        <el-form-item :label="t('friend.nickname')">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item :label="t('roleManage.status')">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleSubmit">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getUserList, createUser, updateUser, deleteUser } from '../../api'
import { ElMessage } from 'element-plus'
import { useI18n } from '../../utils/i18n'

const { t, lang } = useI18n()

const userList = ref([])
const page = ref(1)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const form = ref({ username: '', password: '', nickname: '', status: 1 })

onMounted(() => loadUsers())

async function loadUsers() {
  const res = await getUserList({ page: page.value, size: 10 })
  if (res.code === 200) {
    userList.value = res.data.records || []
    total.value = res.data.total || 0
  }
}

function showDialog(row) {
  if (row) {
    isEdit.value = true
    editId.value = row.id
    form.value = { ...row, password: '' }
  } else {
    isEdit.value = false
    editId.value = null
    form.value = { username: '', password: '', nickname: '', status: 1 }
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (isEdit.value) {
    const data = { ...form.value }
    if (!data.password) delete data.password
    await updateUser(editId.value, data)
    ElMessage.success(t('common.success'))
  } else {
    await createUser(form.value)
    ElMessage.success(t('common.success'))
  }
  dialogVisible.value = false
  loadUsers()
}

async function handleDelete(id) {
  await deleteUser(id)
  ElMessage.success(t('common.success'))
  loadUsers()
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
