<template>
  <div class="tag-manage-page" :key="lang">
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>{{ t('tagManage.title') }}</span>
          <el-button type="primary" size="small" @click="showCreate">{{ t('common.create') }}</el-button>
        </div>
      </template>
      <el-table :data="paginatedTags" stripe v-if="tags.length > 0" v-loading="loading">
        <el-table-column prop="tagName" :label="t('tagManage.tagName')">
          <template #default="{ row }">
            <span style="color:#409eff;cursor:pointer;text-decoration:underline" @click="goToOverview(row)">{{ row.tagName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="tagCategory" :label="t('tagManage.category')" width="180">
          <template #default="{ row }">
            <template v-if="editingCategoryId === row.id">
              <el-select v-model="editingCategoryValue" size="small" style="width:140px" @change="saveCategory(row)" @blur="cancelEditCategory">
                <el-option :label="t('tagManage.cat_landscape')" value="landscape" />
                <el-option :label="t('tagManage.cat_scene')" value="scene" />
                <el-option :label="t('tagManage.cat_food')" value="food" />
                <el-option :label="t('tagManage.cat_people')" value="people" />
                <el-option :label="t('tagManage.cat_other')" value="other" />
              </el-select>
            </template>
            <template v-else>
              <span style="cursor:pointer" @click="startEditCategory(row)">{{ t('tagManage.cat_' + (row.tagCategory || 'other')) }}</span>
            </template>
          </template>
        </el-table-column>
        <el-table-column :label="t('tagManage.photoCount')" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.photoCount > 0 ? 'info' : 'success'">{{ row.photoCount || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('family.actions')" width="120">
          <template #default="{ row }">
            <el-tooltip :content="row.photoCount > 0 ? t('tagManage.deleteWithPhotos') : ''" :disabled="row.photoCount === 0">
              <el-popconfirm :title="row.photoCount > 0 ? t('tagManage.deleteWithPhotos') : t('tagManage.confirmDelete')" @confirm="handleDelete(row.id)">
                <template #reference>
                  <el-button size="small" type="danger" link :disabled="row.photoCount > 0">{{ t('common.delete') }}</el-button>
                </template>
              </el-popconfirm>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else :description="t('tagManage.empty')" />
      <el-pagination v-if="tags.length > pageSize" v-model:current-page="tagPage" :page-size="pageSize" :total="tags.length" layout="total, prev, pager, next" style="margin-top:16px;justify-content:flex-end" />
    </el-card>

    <el-dialog v-model="createVisible" :title="t('tagManage.createTag')" width="400px">
      <el-form label-width="80px">
        <el-form-item :label="t('tagManage.tagName')">
          <el-input v-model="form.tagName" />
        </el-form-item>
        <el-form-item :label="t('tagManage.category')">
          <el-select v-model="form.tagCategory" style="width:100%">
            <el-option :label="t('tagManage.cat_landscape')" value="landscape" />
            <el-option :label="t('tagManage.cat_scene')" value="scene" />
            <el-option :label="t('tagManage.cat_food')" value="food" />
            <el-option :label="t('tagManage.cat_people')" value="people" />
            <el-option :label="t('tagManage.cat_other')" value="other" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleCreate">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getTagList, createTag, updateTag, deleteTag } from '../../api'
import { useI18n } from '../../utils/i18n'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'

const router = useRouter()
const { t, lang } = useI18n()
const tags = ref([])
const loading = ref(false)
const tagPage = ref(1)
const pageSize = ref(20)
const createVisible = ref(false)
const form = ref({ tagName: '', tagCategory: 'other', tagColor: '#409eff' })
const editingCategoryId = ref(null)
const editingCategoryValue = ref('')

const paginatedTags = computed(() => {
  const start = (tagPage.value - 1) * pageSize.value
  return tags.value.slice(start, start + pageSize.value)
})

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  const res = await getTagList()
  if (res.code === 200) {
    const tagList = res.data || []
    await Promise.all(tagList.map(async (tag) => {
      try {
        const countRes = await request.get(`/api/tag/${tag.id}/photo-count`)
        tag.photoCount = countRes.code === 200 ? countRes.data : 0
      } catch { tag.photoCount = 0 }
    }))
    tags.value = tagList
  }
  loading.value = false
}

function showCreate() {
  form.value = { tagName: '', tagCategory: 'other', tagColor: '#409eff' }
  createVisible.value = true
}

async function handleCreate() {
  if (!form.value.tagName.trim()) return
  await createTag(form.value)
  ElMessage.success(t('common.success'))
  createVisible.value = false
  loadData()
}

function goToOverview(tag) {
  router.push({ path: '/photo/overview', query: { tagId: tag.id } })
}

function startEditCategory(row) {
  editingCategoryId.value = row.id
  editingCategoryValue.value = row.tagCategory || 'other'
}

function cancelEditCategory() {
  editingCategoryId.value = null
  editingCategoryValue.value = ''
}

async function saveCategory(row) {
  try {
    await updateTag(row.id, { tagCategory: editingCategoryValue.value })
    row.tagCategory = editingCategoryValue.value
    ElMessage.success(t('common.success'))
  } catch (e) {
    ElMessage.error(e.response?.data?.message || t('common.failed'))
  }
  editingCategoryId.value = null
}

async function handleDelete(id) {
  try {
    const res = await deleteTag(id)
    if (res.code === 200) {
      ElMessage.success(t('common.success'))
      loadData()
    } else {
      ElMessage.error(res.message || t('common.failed'))
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || t('common.failed'))
  }
}
</script>

<style scoped>
.tag-manage-page { max-width: 900px; margin: 0 auto; }
</style>
