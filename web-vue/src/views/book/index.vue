<template>
  <div class="book-page" :key="lang">
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>{{ t('menu.book') }}</span>
          <el-button type="primary" size="small" @click="uploadDialogVisible = true">{{ t('book.upload') }}</el-button>
        </div>
      </template>
      <el-tabs v-model="activeTab" @tab-change="loadData">
        <el-tab-pane :label="t('book.public')" name="public" />
        <el-tab-pane :label="t('book.private')" name="private" />
      </el-tabs>
      <el-form :inline="true" style="margin-bottom:16px">
        <el-form-item><el-input v-model="filters.keyword" :placeholder="t('book.searchPlaceholder')" clearable @keyup.enter="loadData" /></el-form-item>
        <el-form-item>
          <el-select v-model="filters.category" clearable :placeholder="t('book.category')" @change="loadData">
            <el-option :label="t('book.categoryNovel')" value="novel" /><el-option :label="t('book.categoryTextbook')" value="textbook" />
            <el-option :label="t('book.categoryReference')" value="reference" /><el-option :label="t('book.categoryComic')" value="comic" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="loadData">{{ t('common.search') }}</el-button></el-form-item>
      </el-form>
      <el-table :data="books" stripe v-if="books.length > 0">
        <el-table-column :label="t('book.title')">
          <template #default="{ row }">{{ row.title || '-' }}</template>
        </el-table-column>
        <el-table-column :label="t('book.author')" width="150">
          <template #default="{ row }">{{ row.author || '-' }}</template>
        </el-table-column>
        <el-table-column :label="t('book.format')" width="80">
          <template #default="{ row }">{{ row.fileFormat || '-' }}</template>
        </el-table-column>
        <el-table-column :label="t('book.category')" width="100">
          <template #default="{ row }">{{ row.category || '-' }}</template>
        </el-table-column>
        <el-table-column :label="t('file.uploadTime')" width="180">
          <template #default="{ row }">{{ row.createTime || '-' }}</template>
        </el-table-column>
        <el-table-column :label="t('common.delete')" width="200">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="readBook(row)">{{ t('book.read') }}</el-button>
            <el-button size="small" type="success" link @click="handleDownload(row.id)">{{ t('file.download') }}</el-button>
            <el-popconfirm :title="t('book.confirmDelete')" @confirm="handleDelete(row.id)">
              <template #reference><el-button size="small" type="danger" link>{{ t('common.delete') }}</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else :description="t('book.empty')" />
    </el-card>

    <el-dialog v-model="uploadDialogVisible" :title="t('book.uploadBook')" width="500px">
      <el-upload drag :auto-upload="false" :on-change="handleFileChange" accept=".epub,.pdf,.mobi,.txt">
        <el-icon :size="40"><UploadFilled /></el-icon>
        <div>{{ t('book.uploadHint') }}</div>
      </el-upload>
      <el-form label-width="80px" style="margin-top:16px">
        <el-form-item :label="t('book.title')"><el-input v-model="uploadForm.title" /></el-form-item>
        <el-form-item :label="t('book.author')"><el-input v-model="uploadForm.author" /></el-form-item>
        <el-form-item :label="t('book.category')">
          <el-select v-model="uploadForm.category">
            <el-option :label="t('book.categoryNovel')" value="novel" /><el-option :label="t('book.categoryTextbook')" value="textbook" />
            <el-option :label="t('book.categoryReference')" value="reference" /><el-option :label="t('book.categoryComic')" value="comic" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('book.visibility')">
          <el-radio-group v-model="uploadForm.visibility">
            <el-radio value="public">{{ t('book.public') }}</el-radio>
            <el-radio value="private">{{ t('book.private') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('book.tags')"><el-input v-model="uploadForm.tags" :placeholder="t('book.tagsPlaceholder')" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">{{ t('upload.startUpload') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getBookList, uploadBook, deleteBook } from '../../api'
import { useI18n } from '../../utils/i18n'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const { t, lang } = useI18n()
const router = useRouter()
const books = ref([])
const activeTab = ref('private')
const uploadDialogVisible = ref(false)
const uploading = ref(false)
const uploadFileRaw = ref(null)
const filters = reactive({ keyword: '', category: '' })
const uploadForm = reactive({ title: '', author: '', category: 'other', visibility: 'private', tags: '' })

onMounted(() => loadData())

async function loadData() {
  const params = { page: 1, size: 100, visibility: activeTab.value }
  if (filters.keyword) params.keyword = filters.keyword
  if (filters.category) params.category = filters.category
  const res = await getBookList(params)
  if (res.code === 200) books.value = res.data.records || []
}

function handleFileChange(file) { uploadFileRaw.value = file.raw }

async function handleUpload() {
  if (!uploadFileRaw.value) return
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', uploadFileRaw.value)
    Object.entries(uploadForm).forEach(([k, v]) => fd.append(k, v))
    await uploadBook(fd)
    ElMessage.success(t('common.success'))
    uploadDialogVisible.value = false
    loadData()
  } finally { uploading.value = false }
}

function readBook(book) { router.push(`/book/reader/${book.id}`) }
function handleDownload(id) { window.open(`/api/book/${id}/read`) }
async function handleDelete(id) { await deleteBook(id); ElMessage.success(t('common.success')); loadData() }
</script>
