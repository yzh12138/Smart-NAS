<template>
  <div class="file-page" :key="lang">
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>{{ t('menu.fileStorage') }}</span>
          <el-button type="primary" size="small" @click="uploadDialogVisible = true">{{ t('common.create') }}</el-button>
        </div>
      </template>
      <el-form :inline="true" style="margin-bottom:16px">
        <el-form-item>
          <el-select v-model="filterCategory" :placeholder="t('file.allCategories')" clearable style="width:200px" @change="loadData">
            <el-option :label="t('file.categoryInstaller')" value="installer" />
            <el-option :label="t('file.categoryArchive')" value="archive" />
            <el-option :label="t('file.categoryDocument')" value="document" />
            <el-option :label="t('file.categoryOther')" value="other" />
          </el-select>
        </el-form-item>
      </el-form>
      <el-table :data="files" stripe v-if="files.length > 0">
        <el-table-column prop="fileName" :label="t('file.fileName')" />
        <el-table-column :label="t('file.category')" width="120">
          <template #default="{ row }">{{ t('file.category' + row.category.charAt(0).toUpperCase() + row.category.slice(1)) }}</template>
        </el-table-column>
        <el-table-column prop="fileSize" :label="t('file.size')" width="120">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" :label="t('file.uploadTime')" width="180" />
        <el-table-column :label="t('common.delete')" width="160">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="handleDownload(row.id)">{{ t('file.download') }}</el-button>
            <el-popconfirm :title="t('file.confirmDelete')" @confirm="handleDelete(row.id)">
              <template #reference><el-button size="small" type="danger" link>{{ t('common.delete') }}</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else :description="t('file.empty')" />
      <el-pagination v-if="total > 0" v-model:current-page="page" :page-size="20" :total="total" layout="total, prev, pager, next" style="margin-top:16px;justify-content:flex-end" @current-change="loadData" />
    </el-card>

    <el-dialog v-model="uploadDialogVisible" :title="t('file.uploadFile')" width="500px">
      <el-upload drag :auto-upload="false" :on-change="handleFileChange" accept="*">
        <el-icon :size="40"><UploadFilled /></el-icon>
        <div>{{ t('upload.clickUpload') }}</div>
      </el-upload>
      <el-form label-width="80px" style="margin-top:16px">
        <el-form-item :label="t('file.category')">
          <el-select v-model="uploadForm.category" style="width:100%">
            <el-option :label="t('file.categoryInstaller')" value="installer" />
            <el-option :label="t('file.categoryArchive')" value="archive" />
            <el-option :label="t('file.categoryDocument')" value="document" />
            <el-option :label="t('file.categoryOther')" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('file.desc')"><el-input v-model="uploadForm.description" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">{{ t('upload.startUpload') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getFileList, uploadFile, deleteFile } from '../../api'
import { useI18n } from '../../utils/i18n'
import { ElMessage } from 'element-plus'

const { t, lang } = useI18n()
const files = ref([])
const page = ref(1)
const total = ref(0)
const filterCategory = ref('')
const uploadDialogVisible = ref(false)
const uploading = ref(false)
const uploadFileRaw = ref(null)
const uploadForm = ref({ category: 'other', description: '' })

onMounted(() => loadData())

async function loadData() {
  const params = { page: page.value, size: 20 }
  if (filterCategory.value) params.category = filterCategory.value
  const res = await getFileList(params)
  if (res.code === 200) { files.value = res.data.records || []; total.value = res.data.total || 0 }
}

function handleFileChange(file) { uploadFileRaw.value = file.raw }

async function handleUpload() {
  if (!uploadFileRaw.value) return
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', uploadFileRaw.value)
    formData.append('category', uploadForm.value.category)
    formData.append('description', uploadForm.value.description)
    await uploadFile(formData)
    ElMessage.success(t('common.success'))
    uploadDialogVisible.value = false
    loadData()
  } finally { uploading.value = false }
}

function handleDownload(id) { window.open(`/api/file/${id}/download`) }
async function handleDelete(id) { await deleteFile(id); ElMessage.success(t('common.success')); loadData() }
function formatSize(bytes) { if (!bytes) return '-'; if (bytes < 1024) return bytes + 'B'; if (bytes < 1048576) return (bytes/1024).toFixed(1) + 'KB'; return (bytes/1048576).toFixed(1) + 'MB' }
</script>
