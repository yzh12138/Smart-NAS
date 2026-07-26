<template>
  <div class="city-manage-page" :key="lang">
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>{{ t('cityManage.title') }}</span>
          <el-button type="primary" size="small" @click="showCreate">{{ t('common.create') }}</el-button>
        </div>
      </template>
      <el-table :data="cityStats" stripe v-if="cityStats.length > 0">
        <el-table-column prop="name" :label="t('cityManage.city')" />
        <el-table-column prop="description" :label="t('cityManage.description')" show-overflow-tooltip />
        <el-table-column :label="t('cityManage.photoCount')" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.photoCount > 0 ? 'info' : 'success'">{{ row.photoCount || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('family.actions')" width="200">
          <template #default="{ row }">
            <el-button size="small" @click="viewCity(row.name)">{{ t('cityManage.view') }}</el-button>
            <el-button size="small" type="primary" @click="showEdit(row)">{{ t('common.edit') }}</el-button>
            <el-popconfirm :title="row.photoCount > 0 ? t('cityManage.deleteWithPhotos') : t('cityManage.confirmDelete')" @confirm="handleDelete(row)">
              <template #reference>
                <el-button size="small" type="danger" :disabled="!row.id || row.photoCount > 0">{{ t('common.delete') }}</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else :description="t('cityManage.empty')" />
    </el-card>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? t('cityManage.editCity') : t('cityManage.createCity')" width="400px">
      <el-form label-width="80px">
        <el-form-item :label="t('cityManage.city')">
          <el-input v-model="form.name" :placeholder="t('cityManage.namePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('cityManage.description')">
          <el-input v-model="form.description" type="textarea" :rows="3" :placeholder="t('cityManage.descPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- View Photos Dialog -->
    <el-dialog v-model="photosVisible" :title="t('cityManage.cityPhotos', { city: currentCity })" width="80%">
      <div class="photo-grid" v-if="cityPhotos.length > 0">
        <div v-for="photo in cityPhotos" :key="photo.id" class="photo-item">
          <img :src="`/api/photo/${photo.id}/thumb`" class="photo-thumb" />
          <div class="photo-name">{{ photo.originalName }}</div>
        </div>
      </div>
      <el-empty v-else :description="t('memory.noPhotos')" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getCityPhotoStats, getPhotosByCity, getCityList, createCity, updateCity, deleteCity } from '../../api'
import { useI18n } from '../../utils/i18n'
import { ElMessage } from 'element-plus'

const { t, lang } = useI18n()
const cityStats = ref([])
const photosVisible = ref(false)
const currentCity = ref('')
const cityPhotos = ref([])

const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const form = ref({ name: '', description: '' })
const submitting = ref(false)

onMounted(() => loadData())

async function loadData() {
  const [cityRes, photoRes] = await Promise.all([getCityList(), getCityPhotoStats()])
  const cities = cityRes.code === 200 ? cityRes.data || [] : []
  const photoStats = photoRes.code === 200 ? photoRes.data || [] : []
  const photoCountMap = {}
  for (const ps of photoStats) photoCountMap[ps.city] = ps.count

  const merged = []
  const seen = new Set()
  for (const c of cities) {
    merged.push({ ...c, photoCount: photoCountMap[c.name] || 0 })
    seen.add(c.name)
  }
  for (const ps of photoStats) {
    if (!seen.has(ps.city)) {
      merged.push({ name: ps.city, photoCount: ps.count })
    }
  }
  cityStats.value = merged
}

function showCreate() {
  isEdit.value = false
  editingId.value = null
  form.value = { name: '', description: '' }
  dialogVisible.value = true
}

function showEdit(row) {
  isEdit.value = !!row.id
  editingId.value = row.id || null
  form.value = { name: row.name, description: row.description || '' }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.value.name.trim()) return
  submitting.value = true
  try {
    if (isEdit.value) {
      const res = await updateCity(editingId.value, form.value)
      if (res.code === 200) {
        ElMessage.success(t('common.success'))
      } else {
        ElMessage.error(res.message || t('common.failed'))
        submitting.value = false
        return
      }
    } else {
      const res = await createCity(form.value)
      if (res.code === 200) {
        ElMessage.success(t('common.success'))
      } else {
        ElMessage.error(res.message || t('common.failed'))
        submitting.value = false
        return
      }
    }
    dialogVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || t('common.failed'))
  }
  submitting.value = false
}

async function handleDelete(row) {
  try {
    const res = await deleteCity(row.id)
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

async function viewCity(city) {
  currentCity.value = city
  const res = await getPhotosByCity(city)
  if (res.code === 200) cityPhotos.value = res.data || []
  photosVisible.value = true
}
</script>

<style scoped>
.city-manage-page { max-width: 900px; margin: 0 auto; }
.photo-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.photo-item { text-align: center; }
.photo-thumb { width: 100%; height: 120px; object-fit: cover; border-radius: 6px; }
.photo-name { font-size: 12px; color: #666; margin-top: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
