<template>
  <div class="overview-page" :key="lang">
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <div style="display:flex;align-items:center;gap:12px">
            <span>{{ t('menu.photoOverview') }}</span>
            <el-tag v-if="selectedIds.length > 0" type="primary">{{ selectedIds.length }} {{ t('overview.selected') }}</el-tag>
          </div>
          <div style="display:flex;gap:12px;align-items:center">
            <!-- <el-button type="primary" size="small" @click="router.push('/photo/upload')">{{ t('upload.clickUpload') }}</el-button> -->
            <template v-if="photoTab === 'my'">
              <el-select v-model="selectedTag" :placeholder="t('overview.filterTag')" clearable style="width:180px">
                <el-option-group v-if="recentTags.length > 0" :label="t('overview.recentTags')">
                  <el-option v-for="tag in recentTags" :key="'r-'+tag.id" :label="tag.tagName" :value="tag.id" />
                </el-option-group>
                <el-option v-for="tag in otherTags" :key="tag.id" :label="tag.tagName" :value="tag.id" />
              </el-select>
              <el-date-picker v-model="dateRange" type="daterange" :start-placeholder="t('overview.startDate')" :end-placeholder="t('overview.endDate')" style="width:280px" @change="loadData" />
              <el-input v-model="searchKeyword" :placeholder="t('overview.searchPlaceholder')" clearable style="width:200px" @keyup.enter="loadData" />
              <el-button type="primary" size="small" @click="loadData">{{ t('common.search') }}</el-button>
            <el-button v-if="!selectMode" type="warning" size="small" @click="handleBatchScanAll" :loading="scanning">{{ scanning ? t('review.scanning') : t('review.batchScanAll') }}</el-button>
            <el-button v-if="selectMode && selectedIds.length > 0" type="warning" size="small" @click="handleBatchScanSelected" :loading="scanning">{{ scanning ? t('review.scanning') : t('review.batchScanSelected') }} ({{ selectedIds.length }})</el-button>
            </template>
            <template v-if="selectedIds.length > 0 && photoTab === 'my'">
              <el-popconfirm :title="t('recycle.confirmDelete')" @confirm="batchDelete">
                <template #reference><el-button type="danger" size="small">{{ t('recycle.delete') }} ({{ selectedIds.length }})</el-button></template>
              </el-popconfirm>
            </template>
            <template v-if="selectedIds.length > 0 && photoTab === 'shared'">
              <el-button type="success" size="small" @click="showBatchShareDialog">{{ t('family.shareToFamily') }} ({{ selectedIds.length }})</el-button>
              <el-popconfirm :title="t('overview.confirmUnshare')" @confirm="batchUnshare">
                <template #reference><el-button type="warning" size="small">{{ t('overview.unshare') }} ({{ selectedIds.length }})</el-button></template>
              </el-popconfirm>
            </template>
            <el-button v-if="selectedIds.length > 0" size="small" @click="toggleSelectMode">{{ t('overview.cancelSelect') }}</el-button>
            <el-button v-else size="small" @click="toggleSelectMode">{{ t('overview.selectMode') }}</el-button>
          </div>
        </div>
      </template>
      <el-tabs v-model="photoTab" @tab-change="handleTabChange">
        <el-tab-pane :label="t('overview.myPhotos')" name="my" />
        <el-tab-pane :label="t('overview.sharedPhotos')" name="shared" />
      </el-tabs>
      <div class="photo-grid" v-if="photos.length > 0">
        <div v-for="photo in photos" :key="photo.id" class="photo-item" :class="{ selected: selectedIds.includes(photo.id) }" @click="handlePhotoClick(photo, $event)">
          <div v-if="selectMode" class="select-check">
            <el-checkbox :model-value="selectedIds.includes(photo.id)" @click.stop @change="togglePhotoSelect(photo.id)" />
          </div>
          <img :src="`/api/photo/${photo.id}/thumb`" class="photo-thumb" />
          <div class="photo-overlay">
            <span class="photo-name">{{ photo.originalName }}</span>
            <span class="photo-city" v-if="photo.city">{{ photo.city }}</span>
          </div>
        </div>
      </div>
      <el-empty v-else :description="t('memory.noPhotos')" />
      <div class="pagination-bar" v-if="photoTab === 'my' && total > pageSize">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :page-sizes="[20, 40, 80]" :total="total" layout="total, sizes, prev, pager, next" @current-change="loadData" @size-change="loadData" />
      </div>
    </el-card>

    <!-- 照片详情弹窗 -->
    <el-dialog v-model="detailVisible" width="90%" :title="t('memory.detail')" destroy-on-close>
      <div v-if="currentPhoto" style="display:flex;gap:20px;min-height:500px">
        <!-- 左侧：图片 -->
        <div style="flex:1;display:flex;flex-direction:column;align-items:center;min-width:0">
          <div class="ratio-controls">
            <el-button-group size="small">
              <el-button :type="ratio === 'auto' ? 'primary' : ''" @click="ratio = 'auto'">Auto</el-button>
              <el-button :type="ratio === '16:9' ? 'primary' : ''" @click="ratio = '16:9'">16:9</el-button>
              <el-button :type="ratio === '4:3' ? 'primary' : ''" @click="ratio = '4:3'">4:3</el-button>
              <el-button :type="ratio === '1:1' ? 'primary' : ''" @click="ratio = '1:1'">1:1</el-button>
            </el-button-group>
          </div>
          <div class="image-container" :style="containerStyle">
            <img :src="`/api/photo/${currentPhoto.id}/original`" class="detail-image" />
          </div>
          <div class="detail-info">
            <p><strong>{{ currentPhoto.originalName || '' }}</strong></p>
            <p v-if="currentPhoto.city || currentPhoto.province">{{ currentPhoto.province || '' }} {{ currentPhoto.city || '' }}</p>
            <div v-if="photoTags.length > 0" class="detail-tags">
              <el-tag v-for="tag in photoTags" :key="tag.id" size="small" :color="tag.tagColor || '#409eff'" effect="dark" style="margin-right:6px;margin-bottom:4px;cursor:pointer" @click="handleTagClick(tag)">{{ tag.tagName }}</el-tag>
            </div>
            <div class="detail-actions">
              <el-button size="small" link @click="openFullscreen">{{ t('overview.fullscreen') }}</el-button>
              <el-button size="small" type="success" link @click="downloadPhoto">{{ t('file.download') }}</el-button>
              <el-button size="small" type="primary" link @click="showShareDialog">{{ t('family.shareToFamily') }}</el-button>
              <el-popconfirm :title="t('recycle.confirmDelete')" @confirm="handleDelete">
                <template #reference><el-button size="small" type="danger">{{ t('recycle.delete') }}</el-button></template>
              </el-popconfirm>
            </div>
            <!-- 猜你喜欢 -->
            <div v-if="recommendedPhotos.length > 0" class="recommended-section">
              <h4 class="recommended-title">{{ t('overview.recommended') }}</h4>
              <div class="recommended-list">
                <div v-for="rp in recommendedPhotos" :key="rp.id" class="recommended-item" @click="viewRecommendedPhoto(rp)">
                  <img :src="`/api/photo/${rp.id}/thumb`" class="recommended-thumb" />
                  <div class="recommended-info">
                    <span class="recommended-name">{{ rp.originalName }}</span>
                    <span class="recommended-count">{{ rp.clickCount }} {{ t('overview.clicks') }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <!-- 右侧：留言 -->
        <div style="width:320px;display:flex;flex-direction:column;border-left:1px solid #eee;padding-left:20px">
          <h4 style="margin:0 0 12px">{{ t('overview.comments') }} ({{ comments.length }})</h4>
          <div class="comment-list" style="flex:1;overflow-y:auto;margin-bottom:12px">
            <div v-for="c in comments" :key="c.id" class="comment-item">
              <div class="comment-header">
                <span class="comment-user">{{ c.nickname || c.username }}</span>
                <span class="comment-time">{{ formatTime(c.create_time) }}</span>
              </div>
              <div class="comment-content">{{ c.content }}</div>
            </div>
            <el-empty v-if="comments.length === 0" :description="t('overview.noComments')" :image-size="60" />
          </div>
          <div class="comment-input">
            <el-input v-model="newComment" :placeholder="t('overview.commentPlaceholder')" @keyup.enter="submitComment" />
            <el-button type="primary" size="small" @click="submitComment" :disabled="!newComment.trim()" style="margin-top:8px;width:100%">{{ t('overview.commentSubmit') }}</el-button>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 全屏预览 -->
    <div v-if="fullscreenVisible" class="fullscreen-overlay" @click.self="closeFullscreen" @keydown="handleKeydown" tabindex="0" ref="fullscreenRef">
      <div class="fullscreen-toolbar">
        <span>{{ fullscreenIndex + 1 }} / {{ fullscreenPhotos.length }}</span>
        <el-button size="small" text style="color:white" @click="closeFullscreen">{{ t('common.cancel') }}</el-button>
      </div>
      <div class="fullscreen-image-wrap" @wheel.prevent="handleWheel">
        <img :src="`/api/photo/${fullscreenPhotos[fullscreenIndex]?.id}/original`" class="fullscreen-image" :style="{ transform: `scale(${fullscreenZoom})` }" />
      </div>
      <div class="fullscreen-nav" v-if="fullscreenPhotos.length > 1">
        <el-button circle @click="prevPhoto" :disabled="fullscreenIndex === 0"><el-icon><ArrowLeft /></el-icon></el-button>
        <el-button circle @click="nextPhoto" :disabled="fullscreenIndex === fullscreenPhotos.length - 1"><el-icon><ArrowRight /></el-icon></el-button>
      </div>
    </div>

    <el-dialog v-model="shareDialogVisible" :title="t('family.shareToFamily')" width="400px">
      <el-select v-model="selectedFamilyId" :placeholder="t('family.selectFamily')" style="width:100%">
        <el-option v-for="f in myFamilies" :key="f.id" :label="f.familyName" :value="f.id" />
      </el-select>
      <template #footer>
        <el-button @click="shareDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="confirmShare">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPhotoList, searchPhotos, getTagList, deletePhoto, getMyFamilies, shareToFamily, getPhotoDetail, getSharedPhotos, batchShareToFamily, batchAiScan, getPhotoComments, addPhotoComment, batchUnshareFromFamily, trackPhotoClick, getRecommendedPhotos } from '../../api'
import { useI18n } from '../../utils/i18n'
import { ElMessage, ElMessageBox } from 'element-plus'

const { t, lang } = useI18n()
const router = useRouter()
const photos = ref([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const selectedTag = ref(null)
const searchKeyword = ref('')
const tagList = ref([])
const detailVisible = ref(false)
const currentPhoto = ref(null)
const selectMode = ref(false)
const selectedIds = ref([])
const ratio = ref('auto')
const photoTags = ref([])
const photoTab = ref('my')
const myFamilies = ref([])
const shareDialogVisible = ref(false)
const selectedFamilyId = ref(null)
const scanning = ref(false)
const recentTagIds = ref(JSON.parse(localStorage.getItem('recentTagIds') || '[]'))
const dateRange = ref(null)

// 留言相关
const comments = ref([])
const newComment = ref('')

// 猜你喜欢
const recommendedPhotos = ref([])

// 全屏预览
const fullscreenVisible = ref(false)
const fullscreenPhotos = ref([])
const fullscreenIndex = ref(0)
const fullscreenZoom = ref(1)
const fullscreenRef = ref(null)

const recentTags = computed(() => {
  if (recentTagIds.value.length === 0) return []
  return recentTagIds.value
    .map(id => tagList.value.find(t => t.id === id))
    .filter(Boolean)
})

const otherTags = computed(() => {
  if (recentTagIds.value.length === 0) return tagList.value
  return tagList.value.filter(t => !recentTagIds.value.includes(t.id))
})

const containerStyle = computed(() => {
  switch (ratio.value) {
    case '16:9': return { width: '60%', aspectRatio: '16/9' }
    case '4:3': return { width: '50%', aspectRatio: '4/3' }
    case '1:1': return { width: '40%', aspectRatio: '1/1' }
    default: return { maxWidth: '100%', maxHeight: '60vh' }
  }
})

onMounted(async () => {
  const route = useRoute()
  if (route.query.city) {
    searchKeyword.value = route.query.city
  }
  if (route.query.tagId) {
    selectedTag.value = Number(route.query.tagId)
  }
  const tagRes = await getTagList()
  if (tagRes.code === 200) tagList.value = tagRes.data
  const familyRes = await getMyFamilies()
  if (familyRes.code === 200) myFamilies.value = familyRes.data
  loadData()
})

onBeforeUnmount(() => { document.removeEventListener('keydown', handleKeydown) })

function handleTabChange() {
  selectedIds.value = []
  selectMode.value = false
  loadData()
}

async function loadData() {
  if (selectedTag.value) {
    recentTagIds.value = [selectedTag.value, ...recentTagIds.value.filter(id => id !== selectedTag.value)].slice(0, 5)
    localStorage.setItem('recentTagIds', JSON.stringify(recentTagIds.value))
  }
  if (photoTab.value === 'shared') {
    const res = await getSharedPhotos()
    if (res.code === 200) { photos.value = res.data || []; total.value = res.data.length }
  } else if (searchKeyword.value) {
    const res = await searchPhotos(searchKeyword.value)
    if (res.code === 200) { photos.value = res.data || []; total.value = res.data.length }
  } else {
    const params = { page: page.value, size: pageSize.value, tagId: selectedTag.value }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0].toISOString().split('T')[0]
      params.endDate = dateRange.value[1].toISOString().split('T')[0]
    }
    const res = await getPhotoList(params)
    if (res.code === 200) { photos.value = res.data.records || []; total.value = res.data.total || 0 }
  }
}

function handlePhotoClick(photo, event) {
  if (selectMode.value) {
    const idx = selectedIds.value.indexOf(photo.id)
    if (idx >= 0) selectedIds.value.splice(idx, 1)
    else selectedIds.value.push(photo.id)
  } else {
    showDetail(photo)
  }
}

function togglePhotoSelect(id) {
  const idx = selectedIds.value.indexOf(id)
  if (idx >= 0) selectedIds.value.splice(idx, 1)
  else selectedIds.value.push(id)
}

function toggleSelectMode() {
  selectMode.value = !selectMode.value
  if (!selectMode.value) selectedIds.value = []
}

function showDetail(photo) {
  currentPhoto.value = photo
  detailVisible.value = true
  photoTags.value = []
  comments.value = []
  newComment.value = ''
  ratio.value = 'auto'
  recommendedPhotos.value = []
  loadPhotoTags(photo.id)
  loadComments(photo.id)
  trackPhotoClick(photo.id)
  loadRecommended()
}

async function loadPhotoTags(photoId) {
  const res = await getPhotoDetail(photoId)
  if (res.code === 200 && res.data?.tags) photoTags.value = res.data.tags
}

async function loadComments(photoId) {
  const res = await getPhotoComments(photoId)
  if (res.code === 200) comments.value = res.data || []
}

async function loadRecommended() {
  const res = await getRecommendedPhotos()
  if (res.code === 200) recommendedPhotos.value = res.data || []
}

function viewRecommendedPhoto(photo) {
  detailVisible.value = false
  nextTick(() => showDetail(photo))
}

async function submitComment() {
  if (!newComment.value.trim() || !currentPhoto.value) return
  await addPhotoComment(currentPhoto.value.id, { content: newComment.value.trim() })
  newComment.value = ''
  loadComments(currentPhoto.value.id)
}

function formatTime(t) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}

function downloadPhoto() { if (currentPhoto.value) window.open(`/api/photo/${currentPhoto.value.id}/original`) }

async function handleDelete() {
  if (currentPhoto.value) { await deletePhoto(currentPhoto.value.id); ElMessage.success(t('recycle.delete')); detailVisible.value = false; loadData() }
}

async function batchDelete() {
  for (const id of selectedIds.value) { await deletePhoto(id) }
  ElMessage.success(`${t('recycle.delete')} ${selectedIds.value.length}`)
  selectedIds.value = []; selectMode.value = false; loadData()
}

function showShareDialog() {
  selectedFamilyId.value = null
  shareDialogVisible.value = true
}

function showBatchShareDialog() {
  selectedFamilyId.value = null
  shareDialogVisible.value = true
}

async function confirmShare() {
  if (!selectedFamilyId.value) {
    ElMessage.warning(t('family.selectFamily'))
    return
  }
  const photoIds = selectMode.value ? [...selectedIds.value] : [currentPhoto.value.id]
  await batchShareToFamily(selectedFamilyId.value, photoIds)
  ElMessage.success(t('common.success'))
  shareDialogVisible.value = false
}

async function batchUnshare() {
  await batchUnshareFromFamily([...selectedIds.value])
  ElMessage.success(t('common.success'))
  selectedIds.value = []
  selectMode.value = false
  loadData()
}

async function handleBatchScanAll() {
  scanning.value = true
  try {
    const res = await batchAiScan(null)
    if (res.code === 200) {
      ElMessage.success(t('review.scanComplete', { total: res.data.total, success: res.data.success, failed: res.data.failed }))
    }
  } catch (e) {
    ElMessage.error(t('review.scanFailed'))
  }
  scanning.value = false
}

async function handleBatchScanSelected() {
  scanning.value = true
  try {
    const res = await batchAiScan([...selectedIds.value])
    if (res.code === 200) {
      ElMessage.success(t('review.scanComplete', { total: res.data.total, success: res.data.success, failed: res.data.failed }))
      selectedIds.value = []
      selectMode.value = false
    }
  } catch (e) {
    ElMessage.error(t('review.scanFailed'))
  }
  scanning.value = false
}

// 标签点击导航
async function handleTagClick(tag) {
  try {
    await ElMessageBox.confirm(
      t('overview.confirmTagNav', { tag: tag.tagName }),
      t('overview.tagNavTitle'),
      { confirmButtonText: t('common.confirm'), cancelButtonText: t('common.cancel'), type: 'info' }
    )
    detailVisible.value = false
    selectedTag.value = tag.id
    photoTab.value = 'my'
    loadData()
  } catch {}
}

// 全屏预览
function openFullscreen() {
  fullscreenPhotos.value = photos.value.length > 0 ? photos.value : [currentPhoto.value]
  fullscreenIndex.value = fullscreenPhotos.value.findIndex(p => p.id === currentPhoto.value.id)
  if (fullscreenIndex.value < 0) fullscreenIndex.value = 0
  fullscreenZoom.value = 1
  fullscreenVisible.value = true
  nextTick(() => {
    if (fullscreenRef.value) fullscreenRef.value.focus()
    document.addEventListener('keydown', handleKeydown)
  })
}

function closeFullscreen() {
  fullscreenVisible.value = false
  document.removeEventListener('keydown', handleKeydown)
}

function handleWheel(e) {
  if (e.deltaY < 0) {
    fullscreenZoom.value = Math.min(fullscreenZoom.value + 0.1, 5)
  } else {
    fullscreenZoom.value = Math.max(fullscreenZoom.value - 0.1, 0.5)
  }
}

function handleKeydown(e) {
  if (!fullscreenVisible.value) return
  if (e.key === 'ArrowLeft') {
    if (fullscreenZoom.value <= 1 && fullscreenIndex.value > 0) {
      fullscreenIndex.value--
      fullscreenZoom.value = 1
    }
  } else if (e.key === 'ArrowRight') {
    if (fullscreenZoom.value <= 1 && fullscreenIndex.value < fullscreenPhotos.value.length - 1) {
      fullscreenIndex.value++
      fullscreenZoom.value = 1
    }
  } else if (e.key === 'Escape') {
    closeFullscreen()
  }
}

function prevPhoto() {
  if (fullscreenIndex.value > 0) { fullscreenIndex.value--; fullscreenZoom.value = 1 }
}
function nextPhoto() {
  if (fullscreenIndex.value < fullscreenPhotos.value.length - 1) { fullscreenIndex.value++; fullscreenZoom.value = 1 }
}
</script>

<style scoped>
.photo-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 8px; }
.photo-item { position: relative; cursor: pointer; border-radius: 6px; overflow: hidden; aspect-ratio: 1; border: 2px solid transparent; transition: border-color 0.2s; }
.photo-item.selected { border-color: #409eff; }
.select-check { position: absolute; top: 6px; left: 6px; z-index: 2; background: rgba(0,0,0,0.5); border-radius: 4px; padding: 2px; }
.photo-thumb { width: 100%; height: 100%; object-fit: cover; display: block; }
.photo-overlay { position: absolute; bottom: 0; left: 0; right: 0; background: linear-gradient(transparent, rgba(0,0,0,0.7)); padding: 6px 8px; color: white; font-size: 11px; }
.photo-name { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.photo-city { color: #ffd166; }
.pagination-bar { display: flex; justify-content: center; margin-top: 16px; }
.ratio-controls { margin-bottom: 12px; text-align: center; }
.image-container { display: flex; justify-content: center; align-items: center; overflow: hidden; background: #000; }
.detail-image { max-width: 100%; max-height: 100%; object-fit: contain; }
.detail-content { display: flex; flex-direction: column; align-items: center; }
.detail-info { width: 100%; margin-top: 12px; font-size: 14px; line-height: 1.8; text-align: center; }
.detail-tags { margin: 8px 0; display: flex; justify-content: center; flex-wrap: wrap; }
.detail-actions { display: flex; gap: 12px; justify-content: center; margin-top: 12px; }

/* 猜你喜欢 */
.recommended-section { margin-top: 16px; text-align: left; border-top: 1px solid #eee; padding-top: 12px; }
.recommended-title { margin: 0 0 10px; font-size: 14px; color: #333; }
.recommended-list { display: flex; gap: 8px; overflow-x: auto; padding-bottom: 8px; }
.recommended-item { min-width: 100px; cursor: pointer; border-radius: 6px; overflow: hidden; border: 1px solid #eee; transition: border-color 0.2s; flex-shrink: 0; }
.recommended-item:hover { border-color: #409eff; }
.recommended-thumb { width: 100px; height: 80px; object-fit: cover; display: block; }
.recommended-info { padding: 4px 6px; }
.recommended-name { display: block; font-size: 11px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.recommended-count { font-size: 10px; color: #999; }

/* 留言样式 */
.comment-item { padding: 10px 0; border-bottom: 1px solid #f0f0f0; }
.comment-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.comment-user { font-weight: 500; font-size: 13px; }
.comment-time { font-size: 11px; color: #999; }
.comment-content { font-size: 13px; color: #333; line-height: 1.5; }
.comment-input { border-top: 1px solid #eee; padding-top: 12px; }

/* 全屏预览 */
.fullscreen-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.95); z-index: 9999; display: flex; flex-direction: column; align-items: center; justify-content: center; outline: none; }
.fullscreen-toolbar { position: absolute; top: 16px; left: 0; right: 0; display: flex; justify-content: space-between; align-items: center; padding: 0 24px; z-index: 10; color: white; font-size: 14px; }
.fullscreen-image-wrap { display: flex; align-items: center; justify-content: center; width: 100%; height: 100%; overflow: hidden; }
.fullscreen-image { max-width: 90vw; max-height: 90vh; object-fit: contain; transition: transform 0.2s ease; cursor: grab; }
.fullscreen-nav { position: absolute; bottom: 24px; display: flex; gap: 16px; z-index: 10; }
</style>
