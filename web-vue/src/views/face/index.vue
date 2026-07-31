<template>
  <div class="face-page" :key="lang">
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>{{ t('face.title') }}</span>
          <el-button type="primary" size="small" @click="showCreateDialog">{{ t('face.addPerson') }}</el-button>
        </div>
      </template>

      <!-- 人物聚类网格 -->
      <div v-if="clusters.length > 0" class="cluster-grid">
        <div v-for="cluster in clusters" :key="cluster.id" class="cluster-card" @click="openCluster(cluster)">
          <div class="cluster-avatar">
            <img v-if="cluster.coverPhotoId" :src="`/api/photo/${cluster.coverPhotoId}/thumb`" />
            <div v-else class="avatar-placeholder">{{ (cluster.clusterName || '?')[0] }}</div>
          </div>
          <div class="cluster-info">
            <div class="cluster-name">{{ cluster.clusterName || t('face.unnamed') }}</div>
            <div class="cluster-count">{{ cluster.photoCount || 0 }} {{ t('face.photos') }}</div>
          </div>
          <div class="cluster-actions" @click.stop>
            <el-dropdown trigger="click">
              <el-icon style="cursor:pointer;color:#999"><MoreFilled /></el-icon>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="renameCluster(cluster)">{{ t('common.edit') }}</el-dropdown-item>
                  <el-dropdown-item @click="deleteCluster(cluster)" divided>{{ t('common.delete') }}</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </div>
      <el-empty v-else :description="t('face.noClusters')" />
    </el-card>

    <!-- 人物照片详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="currentCluster?.clusterName || t('face.unnamed')" width="80%" top="5vh">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
        <span style="color:#666">{{ clusterPhotos.length }} {{ t('face.photos') }}</span>
        <el-button type="primary" size="small" @click="showAddPhotos">{{ t('face.addPhotos') }}</el-button>
      </div>
      <div v-if="clusterPhotos.length > 0" class="photo-grid">
        <div v-for="photo in clusterPhotos" :key="photo.id" class="photo-item">
          <img :src="`/api/photo/${photo.id}/thumb`" @click="previewPhoto(photo)" />
          <div class="photo-actions">
            <el-popconfirm :title="t('face.confirmRemove')" @confirm="removePhoto(photo.id)">
              <template #reference>
                <el-button size="small" type="danger" circle><el-icon><Close /></el-icon></el-button>
              </template>
            </el-popconfirm>
          </div>
        </div>
      </div>
      <el-empty v-else :description="t('face.noPhotos')" />
    </el-dialog>

    <!-- 添加照片弹窗 -->
    <el-dialog v-model="addPhotosVisible" :title="t('face.addPhotos')" width="80%" top="5vh">
      <div style="margin-bottom:12px">
        <el-input v-model="photoSearchKeyword" :placeholder="t('face.searchPhotos')" clearable @input="debounceLoadAvailablePhotos" style="width:300px" />
      </div>
      <div v-if="availablePhotos.length > 0" class="photo-grid" style="max-height:400px;overflow-y:auto">
        <div v-for="photo in availablePhotos" :key="photo.id" class="photo-item" :class="{ selected: selectedPhotoIds.includes(photo.id) }" @click="togglePhotoSelection(photo.id)">
          <img :src="`/api/photo/${photo.id}/thumb`" />
          <div v-if="selectedPhotoIds.includes(photo.id)" class="select-badge"><el-icon><Check /></el-icon></div>
        </div>
      </div>
      <el-empty v-else :description="t('face.noAvailablePhotos')" />
      <template #footer>
        <span style="color:#999;margin-right:auto">{{ t('face.selectedCount', { count: selectedPhotoIds.length }) }}</span>
        <el-button @click="addPhotosVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :disabled="selectedPhotoIds.length === 0" @click="handleAddPhotos">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- 重命名弹窗 -->
    <el-dialog v-model="renameVisible" :title="t('face.rename')" width="400px">
      <el-input v-model="renameName" :placeholder="t('face.namePlaceholder')" @keyup.enter="handleRename" />
      <template #footer>
        <el-button @click="renameVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleRename">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- 新建人物弹窗 -->
    <el-dialog v-model="createVisible" :title="t('face.addPerson')" width="400px">
      <el-input v-model="createName" :placeholder="t('face.namePlaceholder')" @keyup.enter="handleCreate" />
      <template #footer>
        <el-button @click="createVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleCreate">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- 照片预览 -->
    <el-dialog v-model="previewVisible" width="80%" top="5vh" :show-close="true">
      <img v-if="previewPhotoUrl" :src="previewPhotoUrl" style="max-width:100%;max-height:80vh;object-fit:contain" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getFaceClusters, getFaceClusterPhotos, createFaceCluster, renameFaceCluster, deleteFaceCluster, removePhotoFromCluster, movePhotoToCluster, getPhotoList } from '../../api'
import { useI18n } from '../../utils/i18n'
import { ElMessage, ElMessageBox } from 'element-plus'

const { t, lang } = useI18n()
const clusters = ref([])
const detailVisible = ref(false)
const currentCluster = ref(null)
const clusterPhotos = ref([])
const renameVisible = ref(false)
const renameName = ref('')
const renameClusterId = ref(null)
const createVisible = ref(false)
const createName = ref('')
const previewVisible = ref(false)
const previewPhotoUrl = ref('')
const addPhotosVisible = ref(false)
const availablePhotos = ref([])
const selectedPhotoIds = ref([])
const photoSearchKeyword = ref('')
let searchTimer = null

onMounted(() => loadClusters())

async function loadClusters() {
  const res = await getFaceClusters()
  if (res.code === 200) clusters.value = res.data || []
}

async function openCluster(cluster) {
  currentCluster.value = cluster
  const res = await getFaceClusterPhotos(cluster.id)
  if (res.code === 200) clusterPhotos.value = res.data || []
  detailVisible.value = true
}

function renameCluster(cluster) {
  renameClusterId.value = cluster.id
  renameName.value = cluster.clusterName || ''
  renameVisible.value = true
}

async function handleRename() {
  if (!renameName.value.trim()) return
  try {
    const res = await renameFaceCluster(renameClusterId.value, { name: renameName.value.trim() })
    if (res.code === 200) {
      ElMessage.success(t('common.success'))
      renameVisible.value = false
      loadClusters()
      if (currentCluster.value?.id === renameClusterId.value) {
        currentCluster.value.clusterName = renameName.value.trim()
      }
    } else {
      ElMessage.error(res.message || t('common.failed'))
    }
  } catch {
    ElMessage.error(t('common.failed'))
  }
}

async function deleteCluster(cluster) {
  try {
    await ElMessageBox.confirm(t('face.confirmDelete'), t('common.confirm'), { type: 'warning' })
    await deleteFaceCluster(cluster.id)
    ElMessage.success(t('common.success'))
    loadClusters()
  } catch {}
}

async function removePhoto(photoId) {
  try {
    const res = await removePhotoFromCluster(currentCluster.value.id, photoId)
    if (res.code === 200) {
      ElMessage.success(t('common.success'))
      clusterPhotos.value = clusterPhotos.value.filter(p => p.id !== photoId)
      const c = clusters.value.find(c => c.id === currentCluster.value.id)
      if (c) c.photoCount = Math.max(0, c.photoCount - 1)
    } else {
      ElMessage.error(res.message || t('common.failed'))
    }
  } catch {
    ElMessage.error(t('common.failed'))
  }
}

function showCreateDialog() {
  createName.value = ''
  createVisible.value = true
}

async function handleCreate() {
  if (!createName.value.trim()) return
  try {
    const res = await createFaceCluster({ name: createName.value.trim() })
    if (res.code === 200) {
      ElMessage.success(t('common.success'))
      createVisible.value = false
      loadClusters()
    } else {
      ElMessage.error(res.message || t('common.failed'))
    }
  } catch {
    ElMessage.error(t('common.failed'))
  }
}

function previewPhoto(photo) {
  previewPhotoUrl.value = `/api/photo/${photo.id}/original`
  previewVisible.value = true
}

async function showAddPhotos() {
  selectedPhotoIds.value = []
  photoSearchKeyword.value = ''
  addPhotosVisible.value = true
  await loadAvailablePhotos()
}

async function loadAvailablePhotos() {
  const params = { page: 1, size: 100, mediaType: 'image' }
  if (photoSearchKeyword.value.trim()) params.keyword = photoSearchKeyword.value.trim()
  const res = await getPhotoList(params)
  if (res.code === 200) availablePhotos.value = res.data.records || []
}

function debounceLoadAvailablePhotos() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => loadAvailablePhotos(), 300)
}

function togglePhotoSelection(photoId) {
  const idx = selectedPhotoIds.value.indexOf(photoId)
  if (idx >= 0) selectedPhotoIds.value.splice(idx, 1)
  else selectedPhotoIds.value.push(photoId)
}

async function handleAddPhotos() {
  if (selectedPhotoIds.value.length === 0) return
  try {
    let successCount = 0
    for (const photoId of selectedPhotoIds.value) {
      const res = await movePhotoToCluster(currentCluster.value.id, photoId)
      if (res.code === 200) successCount++
    }
    ElMessage.success(t('face.addPhotosSuccess', { count: successCount }))
    addPhotosVisible.value = false
    // 刷新聚类照片
    const res = await getFaceClusterPhotos(currentCluster.value.id)
    if (res.code === 200) clusterPhotos.value = res.data || []
    // 刷新聚类列表（更新封面和计数）
    loadClusters()
  } catch {
    ElMessage.error(t('common.failed'))
  }
}
</script>

<style scoped>
.face-page { max-width: 1200px; margin: 0 auto; }
.cluster-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 16px; }
.cluster-card {
  display: flex; flex-direction: column; align-items: center; padding: 16px;
  border: 1px solid #e8e8e8; border-radius: 12px; cursor: pointer; position: relative; transition: all 0.2s;
}
.cluster-card:hover { border-color: #409eff; box-shadow: 0 2px 12px rgba(64,158,255,0.15); }
.cluster-avatar { width: 80px; height: 80px; border-radius: 50%; overflow: hidden; margin-bottom: 8px; }
.cluster-avatar img { width: 100%; height: 100%; object-fit: cover; }
.avatar-placeholder {
  width: 100%; height: 100%; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex; align-items: center; justify-content: center; color: white; font-size: 28px; font-weight: bold;
}
.cluster-info { text-align: center; }
.cluster-name { font-weight: 500; margin-bottom: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 120px; }
.cluster-count { font-size: 12px; color: #999; }
.cluster-actions { position: absolute; top: 8px; right: 8px; }
.photo-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 8px; }
.photo-item { position: relative; border-radius: 8px; overflow: hidden; aspect-ratio: 1; }
.photo-item img { width: 100%; height: 100%; object-fit: cover; cursor: pointer; }
.photo-actions { position: absolute; top: 4px; right: 4px; opacity: 0; transition: opacity 0.2s; }
.photo-item:hover .photo-actions { opacity: 1; }
.photo-item.selected { outline: 3px solid #409eff; border-radius: 8px; }
.select-badge {
  position: absolute; bottom: 4px; right: 4px; width: 24px; height: 24px;
  background: #409eff; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white;
}
</style>
