<template>
  <div class="roles-page" :key="lang">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ t('roleManage.roleManagement') }}</span>
          <el-button type="primary" @click="showDialog()">{{ t('common.create') }}</el-button>
        </div>
      </template>
      <el-table :data="roleList" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="roleName" :label="t('roleManage.roleName')" />
        <el-table-column prop="roleKey" :label="t('roleManage.roleKey')" />
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
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? t('common.edit') : t('common.create')" width="480px">
      <el-form :model="form" label-width="100px">
        <el-form-item :label="t('roleManage.roleName')">
          <el-input v-model="form.roleName" />
        </el-form-item>
        <el-form-item :label="t('roleManage.roleKey')">
          <el-input v-model="form.roleKey" :disabled="isEdit" />
        </el-form-item>
        <el-form-item :label="t('permManage.sortOrder')">
          <el-input-number v-model="form.sortOrder" :min="0" />
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
import { getRoleList, createRole, updateRole, deleteRole } from '../../api'
import { ElMessage } from 'element-plus'
import { useI18n } from '../../utils/i18n'

const { t, lang } = useI18n()

const roleList = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const form = ref({ roleName: '', roleKey: '', sortOrder: 0, status: 1 })

onMounted(() => loadRoles())

async function loadRoles() {
  const res = await getRoleList()
  if (res.code === 200) roleList.value = res.data
}

function showDialog(row) {
  if (row) {
    isEdit.value = true
    editId.value = row.id
    form.value = { ...row }
  } else {
    isEdit.value = false
    editId.value = null
    form.value = { roleName: '', roleKey: '', sortOrder: 0, status: 1 }
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (isEdit.value) {
    await updateRole(editId.value, form.value)
    ElMessage.success(t('common.success'))
  } else {
    await createRole(form.value)
    ElMessage.success(t('common.success'))
  }
  dialogVisible.value = false
  loadRoles()
}

async function handleDelete(id) {
  await deleteRole(id)
  ElMessage.success(t('common.success'))
  loadRoles()
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
