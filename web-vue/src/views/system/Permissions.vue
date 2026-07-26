<template>
  <div class="permissions-page" :key="lang">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ t('permManage.title') }}</span>
          <el-button type="primary" size="small" @click="showDialog()">{{ t('common.create') }}</el-button>
        </div>
      </template>
      <el-table :data="permTree" stripe row-key="id" default-expand-all>
        <el-table-column prop="permName" :label="t('permManage.permName')" />
        <el-table-column prop="permKey" :label="t('permManage.permKey')" />
        <el-table-column prop="permType" :label="t('permManage.type')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.permType === 1 ? '' : row.permType === 2 ? 'success' : 'warning'">
              {{ row.permType === 1 ? t('permManage.directory') : row.permType === 2 ? t('permManage.menu') : t('permManage.button') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" :label="t('permManage.routePath')" />
        <el-table-column prop="sortOrder" :label="t('permManage.sortOrder')" width="80" />
        <el-table-column :label="t('family.actions')" width="180">
          <template #default="{ row }">
            <el-button size="small" @click="showDialog(row)">{{ t('common.edit') }}</el-button>
            <el-popconfirm :title="t('common.confirm')" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button size="small" type="danger">{{ t('common.delete') }}</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? t('common.edit') : t('common.create')" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item :label="t('permManage.parentPerm')">
          <el-tree-select
            v-model="form.parentId"
            :data="permTree"
            :props="{ label: 'permName', value: 'id', children: 'children' }"
            check-strictly
            :placeholder="t('permManage.noParent')"
            clearable
          />
        </el-form-item>
        <el-form-item :label="t('permManage.permName')">
          <el-input v-model="form.permName" />
        </el-form-item>
        <el-form-item :label="t('permManage.permKey')">
          <el-input v-model="form.permKey" placeholder="e.g. system:user:list" />
        </el-form-item>
        <el-form-item :label="t('permManage.type')">
          <el-radio-group v-model="form.permType">
            <el-radio :value="1">{{ t('permManage.directory') }}</el-radio>
            <el-radio :value="2">{{ t('permManage.menu') }}</el-radio>
            <el-radio :value="3">{{ t('permManage.button') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('permManage.routePath')" v-if="form.permType !== 3">
          <el-input v-model="form.path" />
        </el-form-item>
        <el-form-item :label="t('permManage.componentPath')" v-if="form.permType === 2">
          <el-input v-model="form.component" placeholder="e.g. views/system/Users" />
        </el-form-item>
        <el-form-item :label="t('permManage.icon')" v-if="form.permType !== 3">
          <el-input v-model="form.icon" />
        </el-form-item>
        <el-form-item :label="t('permManage.sortOrder')">
          <el-input-number v-model="form.sortOrder" :min="0" />
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
import { getPermissionTree, createPermission, updatePermission, deletePermission } from '../../api'
import { useI18n } from '../../utils/i18n'
import { ElMessage } from 'element-plus'

const { t, lang } = useI18n()
const permTree = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const form = ref({ parentId: 0, permName: '', permKey: '', permType: 2, path: '', component: '', icon: '', sortOrder: 0 })

onMounted(() => loadPerms())

async function loadPerms() {
  const res = await getPermissionTree()
  if (res.code === 200) permTree.value = res.data
}

function showDialog(row) {
  if (row) {
    isEdit.value = true
    editId.value = row.id
    form.value = { ...row }
  } else {
    isEdit.value = false
    editId.value = null
    form.value = { parentId: 0, permName: '', permKey: '', permType: 2, path: '', component: '', icon: '', sortOrder: 0 }
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (isEdit.value) { await updatePermission(editId.value, form.value) }
  else { await createPermission(form.value) }
  ElMessage.success(t('common.success'))
  dialogVisible.value = false
  loadPerms()
}

async function handleDelete(id) {
  await deletePermission(id)
  ElMessage.success(t('common.success'))
  loadPerms()
}
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
