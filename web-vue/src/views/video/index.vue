<template>
  <div class="video-page" :key="lang">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ t('menu.video') }} <el-tag v-if="videos.length" size="small" type="info">{{ videos.length }}</el-tag></span>
          <div class="header-actions">
            <el-input v-model="searchKeyword" :placeholder="t('video.searchPlaceholder')" clearable prefix-icon="Search" style="width: 200px; margin-right: 12px" />
            <el-button type="danger" size="small" :disabled="selectedIds.length === 0" @click="handleBatchDelete">
              <el-icon><Delete /></el-icon> {{ t('video.batchDelete') }} ({{ selectedIds.length }})
            </el-button>
            <el-button type="success" size="small" :disabled="selectedIds.length === 0" @click="showBatchShareDialog">
              {{ t('family.shareToFamily') }} ({{ selectedIds.length }})
            </el-button>
            <el-button type="primary" size="small" @click="uploadDialogVisible = true">
              <el-icon><Upload /></el-icon> {{ t('video.uploadVideo') }}
            </el-button>
          </div>
        </div>
      </template>

      <div class="video-grid" v-if="filteredVideos.length > 0">
        <div v-for="video in filteredVideos" :key="video.id" class="video-item" :class="{ selected: selectedIds.includes(video.id) }" @click="playVideo(video)">
          <div class="video-checkbox" @click.stop="toggleSelect(video.id)">
            <el-checkbox :model-value="selectedIds.includes(video.id)" />
          </div>
          <img :src="`/api/photo/${video.id}/thumb`" class="video-thumb" />
          <div class="video-play-icon"><el-icon :size="32"><VideoPlay /></el-icon></div>
          <div class="video-info">
            <span class="video-name">{{ video.originalName }}</span>
            <span class="video-time">{{ formatDate(video.createTime) }}</span>
          </div>
          <div class="video-actions" @click.stop>
            <el-button size="small" circle type="success" @click="shareSingleVideo(video)">
              <el-icon><Share /></el-icon>
            </el-button>
            <el-button size="small" circle type="danger" @click="handleDelete(video)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
      <el-empty v-else :description="searchKeyword ? t('video.noSearchResult') : t('video.noVideos')">
        <el-button v-if="!searchKeyword" type="primary" @click="uploadDialogVisible = true">
          <el-icon><Upload /></el-icon> {{ t('video.uploadVideo') }}
        </el-button>
      </el-empty>
    </el-card>

    <!-- 播放器弹窗 -->
    <el-dialog v-model="playerVisible" width="70%" :title="currentVideo?.originalName" destroy-on-close>
      <video v-if="currentVideo" :src="`/api/photo/${currentVideo.id}/original`" controls autoplay class="video-player" />
    </el-dialog>

    <!-- 上传弹窗 -->
    <el-dialog v-model="uploadDialogVisible" :title="t('video.uploadVideo')" width="600px" destroy-on-close>
      <el-upload ref="uploadRef" class="upload-area" drag multiple :auto-upload="false" :on-change="handleFileChange" :file-list="uploadFileList" accept="video/*">
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">{{ t('video.dragHint') }} <em>{{ t('upload.clickUpload') }}</em></div>
        <template #tip><div class="el-upload__tip">{{ t('video.uploadTip') }}</div></template>
      </el-upload>

      <div v-if="uploadFileList.length > 0" class="upload-options">
        <el-form label-width="100px">
          <el-form-item :label="t('upload.addTags')">
            <el-select v-model="selectedTags" multiple filterable allow-create default-first-option :placeholder="t('upload.tagPlaceholder')" style="width: 100%">
              <el-option-group v-for="(tags, category) in groupedTags" :key="category" :label="categoryLabel(category)">
                <el-option v-for="tag in tags" :key="tag.id" :label="tag.tagName" :value="tag.id" />
              </el-option-group>
            </el-select>
          </el-form-item>
          <el-form-item :label="t('upload.newTags')">
            <el-input v-model="newTagInput" :placeholder="t('upload.newTagPlaceholder')" />
          </el-form-item>
        </el-form>
      </div>

      <div v-if="uploadProgress.length > 0" class="progress-area">
        <div v-for="(item, idx) in uploadProgress" :key="idx" class="progress-item">
          <span class="file-name">{{ item.name }}</span>
          <el-progress :percentage="item.progress" :status="item.status" style="flex:1;margin:0 12px" />
        </div>
      </div>

      <template #footer>
        <el-button @click="clearUploadFiles">{{ t('upload.clear') }}</el-button>
        <el-button type="primary" :loading="uploading" :disabled="uploadFileList.length === 0" @click="handleUpload">
          {{ t('upload.startUpload') }} ({{ uploadFileList.length }})
        </el-button>
      </template>
    </el-dialog>

    <!-- AI标签弹窗 -->
    <el-dialog v-model="aiTagDialogVisible" :title="t('upload.aiDialogTitle')" width="600px" :close-on-click-modal="false">
      <div v-if="aiTagLoading" class="ai-loading">
        <el-icon class="is-loading" :size="32"><Loading /></el-icon>
        <p>{{ t('upload.aiLoading') }}</p>
      </div>
      <div v-else>
        <el-alert v-if="aiResult.city || aiResult.province" type="info" :closable="false" show-icon style="margin-bottom:16px">
          {{ t('upload.aiLocation') }}{{ aiResult.province || '' }} {{ aiResult.city || '' }}
        </el-alert>
        <p style="margin-bottom:12px;color:#666">{{ t('upload.aiSelectHint') }}</p>
        <el-checkbox-group v-model="selectedAiTags">
          <div class="ai-tag-list">
            <el-checkbox v-for="tag in aiResult.tags" :key="tag" :value="tag" class="ai-tag-item">
              <el-tag size="small">{{ tag }}</el-tag>
            </el-checkbox>
          </div>
        </el-checkbox-group>
        <div style="margin-top:16px">
          <p style="margin-bottom:8px;color:#999;font-size:12px">{{ t('upload.aiExtraTags') }}</p>
          <el-input v-model="extraAiTags" :placeholder="t('upload.aiExtraPlaceholder')" />
        </div>
      </div>
      <template #footer>
        <el-button @click="skipAiTags">{{ t('upload.aiSkip') }}</el-button>
        <el-button type="primary" @click="confirmAiTagSelection" :disabled="aiTagLoading">{{ t('upload.aiConfirm') }} ({{ selectedAiTags.length + extraAiTagsCount }})</el-button>
      </template>
    </el-dialog>

    <!-- 共享到家庭弹窗 -->
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
import { ref, computed, onMounted } from 'vue'
import { getPhotoList, deletePhoto, uploadPhotos, getTagList, getAiSuggestedTags, confirmAiTags, getMyFamilies, batchShareToFamily } from '../../api'
import { useI18n } from '../../utils/i18n'
import { ElMessage, ElMessageBox } from 'element-plus'

const { t, lang } = useI18n()
const videos = ref([])
const searchKeyword = ref('')
const selectedIds = ref([])

// 播放器
const playerVisible = ref(false)
const currentVideo = ref(null)

// 上传相关
const uploadDialogVisible = ref(false)
const uploadRef = ref(null)
const uploadFileList = ref([])
const tagList = ref([])
const selectedTags = ref([])
const newTagInput = ref('')
const aiTag = ref(false)
const uploading = ref(false)
const uploadProgress = ref([])

// AI标签相关
const aiTagDialogVisible = ref(false)
const aiTagLoading = ref(false)
const aiResult = ref({ tags: [], city: null, province: null })
const selectedAiTags = ref([])
const extraAiTags = ref('')
const currentPhotoId = ref(null)

// 共享相关
const shareDialogVisible = ref(false)
const selectedFamilyId = ref(null)
const myFamilies = ref([])

const groupedTags = computed(() => {
  const groups = {}
  for (const tag of tagList.value) {
    const cat = tag.tagCategory || 'other'
    if (!groups[cat]) groups[cat] = []
    groups[cat].push(tag)
  }
  return groups
})

const extraAiTagsCount = computed(() => extraAiTags.value.split(/[,，]/).filter(x => x.trim()).length)

const categoryLabels = { province: '省/自治区', city: '城市', landscape: '风景', scene: '场景', food: '美食', people: '人物', other: '其他' }
function categoryLabel(cat) { return categoryLabels[cat] || cat }

const filteredVideos = computed(() => {
  if (!searchKeyword.value) return videos.value
  const kw = searchKeyword.value.toLowerCase()
  return videos.value.filter(v => v.originalName && v.originalName.toLowerCase().includes(kw))
})

onMounted(async () => {
  await loadVideos()
  const res = await getTagList()
  if (res.code === 200) tagList.value = res.data
  const familyRes = await getMyFamilies()
  if (familyRes.code === 200) myFamilies.value = familyRes.data
})

async function loadVideos() {
  const res = await getPhotoList({ page: 1, size: 1000, mediaType: 'video' })
  if (res.code === 200) {
    videos.value = res.data.records || []
    selectedIds.value = []
  }
}

function playVideo(video) { currentVideo.value = video; playerVisible.value = true }
function formatDate(d) { return d ? d.substring(0, 10) : '' }

function toggleSelect(id) {
  const idx = selectedIds.value.indexOf(id)
  if (idx > -1) selectedIds.value.splice(idx, 1)
  else selectedIds.value.push(id)
}

async function handleDelete(video) {
  try {
    await ElMessageBox.confirm(t('video.confirmDelete'), t('common.confirm'), { type: 'warning' })
    const res = await deletePhoto(video.id)
    if (res.code === 200) {
      ElMessage.success(t('common.success'))
      await loadVideos()
    } else { ElMessage.error(res.message || t('common.failed')) }
  } catch {}
}

async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(t('video.confirmBatchDelete', { count: selectedIds.value.length }), t('common.confirm'), { type: 'warning' })
    let successCount = 0
    for (const id of selectedIds.value) {
      const res = await deletePhoto(id)
      if (res.code === 200) successCount++
    }
    ElMessage.success(t('video.batchDeleteSuccess', { count: successCount }))
    await loadVideos()
  } catch {}
}

function handleFileChange(file, list) { uploadFileList.value = list }
function clearUploadFiles() { uploadFileList.value = []; uploadProgress.value = [] }

async function handleUpload() {
  if (uploadFileList.value.length === 0) return
  uploading.value = true
  uploadProgress.value = uploadFileList.value.map(f => ({ name: f.name, progress: 0, status: '' }))
  try {
    const formData = new FormData()
    uploadFileList.value.forEach(f => formData.append('files', f.raw))
    if (selectedTags.value.length > 0) formData.append('tagIds', selectedTags.value.join(','))
    if (newTagInput.value.trim()) formData.append('newTags', newTagInput.value.trim())
    formData.append('aiTag', false)
    const res = await uploadPhotos(formData)
    if (res.code === 200) {
      uploadProgress.value.forEach(p => { p.progress = 100; p.status = 'success' })
      ElMessage.success(t('upload.uploadSuccess', { count: res.data.length }))
      uploadFileList.value = []
      await loadVideos()
      uploadDialogVisible.value = false
    } else { throw new Error(res.message) }
  } catch (e) {
    uploadProgress.value.forEach(p => { p.status = 'exception' })
    ElMessage.error(t('upload.uploadFailed') + ': ' + (e.message || ''))
  } finally { uploading.value = false }
}

async function showAiTagDialog(photoId) {
  aiTagDialogVisible.value = true; aiTagLoading.value = true; selectedAiTags.value = []; extraAiTags.value = ''
  try {
    const res = await getAiSuggestedTags(photoId)
    if (res.code === 200) { aiResult.value = res.data; selectedAiTags.value = [...(res.data.tags || [])] }
  } catch { ElMessage.error(t('upload.aiFailed')); aiResult.value = { tags: [], city: null, province: null } }
  finally { aiTagLoading.value = false }
}

async function confirmAiTagSelection() {
  const allTags = [...selectedAiTags.value]
  extraAiTags.value.split(/[,，]/).forEach(x => { const v = x.trim(); if (v && !allTags.includes(v)) allTags.push(v) })
  try {
    await confirmAiTags(currentPhotoId.value, { tags: allTags, city: aiResult.value.city, province: aiResult.value.province })
    ElMessage.success(t('upload.tagsSaved')); aiTagDialogVisible.value = false
  } catch { ElMessage.error(t('common.failed')) }
}

function skipAiTags() { aiTagDialogVisible.value = false }

function showBatchShareDialog() {
  selectedFamilyId.value = null
  shareDialogVisible.value = true
}

async function confirmShare() {
  if (!selectedFamilyId.value) {
    ElMessage.warning(t('family.selectFamily'))
    return
  }
  await batchShareToFamily(selectedFamilyId.value, [...selectedIds.value])
  ElMessage.success(t('common.success'))
  shareDialogVisible.value = false
  selectedIds.value = []
}

function shareSingleVideo(video) {
  selectedIds.value = [video.id]
  showBatchShareDialog()
}
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-actions { display: flex; align-items: center; gap: 8px; }

.video-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }
.video-item { position: relative; cursor: pointer; border-radius: 8px; overflow: hidden; background: #000; transition: transform 0.2s, box-shadow 0.2s; }
.video-item:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
.video-item.selected { outline: 2px solid #409eff; outline-offset: 2px; }

.video-checkbox { position: absolute; top: 8px; left: 8px; z-index: 2; }

.video-thumb { width: 100%; height: 180px; object-fit: cover; display: block; }
.video-play-icon { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); background: rgba(0,0,0,0.5); border-radius: 50%; width: 56px; height: 56px; display: flex; align-items: center; justify-content: center; color: white; transition: background 0.2s; }
.video-item:hover .video-play-icon { background: rgba(64,158,255,0.8); }

.video-info { padding: 8px; background: #fff; }
.video-name { display: block; font-size: 13px; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.video-time { font-size: 12px; color: #999; }

.video-actions { position: absolute; top: 8px; right: 8px; z-index: 2; opacity: 0; transition: opacity 0.2s; }
.video-item:hover .video-actions { opacity: 1; }

.video-player { width: 100%; max-height: 60vh; }

.upload-area { width: 100%; }
.upload-area :deep(.el-upload-dragger) { width: 100%; padding: 30px 0; }
.upload-options { margin-top: 16px; }
.ai-tip { margin-left: 12px; font-size: 12px; color: #999; }
.progress-area { margin-top: 16px; }
.progress-item { display: flex; align-items: center; margin-bottom: 8px; }
.file-name { width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 13px; color: #666; }
.ai-loading { text-align: center; padding: 40px 0; color: #999; }
.ai-tag-list { display: flex; flex-wrap: wrap; gap: 12px; }
.ai-tag-item { margin-right: 0 !important; }
</style>
