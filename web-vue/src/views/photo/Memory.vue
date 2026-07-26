<template>
  <div class="memory-page" :key="lang">
    <el-row :gutter="16">
      <el-col :xs="24" :sm="24" :md="16">
        <el-card class="map-card">
          <template #header>
            <div class="map-header">
              <span>{{ t('memory.photoMap') }}</span>
              <el-input
                v-model="searchKeyword"
                :placeholder="t('memory.searchPlaceholder')"
                prefix-icon="Search"
                clearable
                style="width: 240px"
                @keyup.enter="handleSearch"
                @clear="loadAllPhotos"
              />
            </div>
          </template>
          <div ref="mapRef" class="map-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :md="8">
        <el-card class="photo-list-card">
          <template #header>
            <div class="list-header">
              <span>{{ selectedCity ? t('memory.cityPhotos', { city: selectedCity }) : t('memory.allPhotos') }}</span>
              <el-tag size="small" type="info">{{ t('memory.photoCount', { count: photos.length }) }}</el-tag>
            </div>
          </template>
          <div class="photo-grid" v-if="photos.length > 0">
            <div
              v-for="photo in photos"
              :key="photo.id"
              class="photo-item"
              @click="showDetail(photo)"
              @mouseenter="showTooltip(photo, $event)"
              @mouseleave="hideTooltip"
            >
              <div class="photo-thumb-wrap">
                <img
                  v-if="photo.mediaType === 'video'"
                  :src="getThumbUrl(photo)"
                  class="photo-thumb"
                  @error="handleImgError"
                />
                <el-image
                  v-else
                  :src="getThumbUrl(photo)"
                  fit="cover"
                  class="photo-thumb"
                >
                  <template #error>
                    <div class="thumb-fallback">
                      <el-icon :size="24"><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
                <div v-if="photo.mediaType === 'video'" class="video-play-icon">
                  <el-icon :size="32"><VideoPlay /></el-icon>
                </div>
              </div>
              <div class="photo-meta">
                <span class="photo-city" v-if="photo.city">
                  <el-icon><Location /></el-icon>
                  {{ photo.city }}
                </span>
                <span class="photo-time">{{ formatDate(photo.shootTime || photo.createTime) }}</span>
              </div>
            </div>
          </div>
          <el-empty v-else :description="t('memory.noPhotos')" />
          <div class="pagination-bar" v-if="photoTotal > photoPageSize">
            <el-pagination
              v-model:current-page="photoPage"
              v-model:page-size="photoPageSize"
              :page-sizes="[10, 20, 40]"
              :total="photoTotal"
              layout="total, sizes, prev, pager, next"
              small
              @current-change="handlePageChange"
              @size-change="handleSizeChange"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 照片详情弹窗 -->
    <el-dialog v-model="detailVisible" width="80%" :title="t('memory.detail')" destroy-on-close>
      <div class="detail-content" v-if="currentPhoto">
        <el-image v-if="currentPhoto.mediaType !== 'video'" :src="getOriginalUrl(currentPhoto)" fit="contain" class="detail-image">
          <template #error><div class="detail-fallback">{{ t('memory.loadFailed') }}</div></template>
        </el-image>
        <video v-else :src="getOriginalUrl(currentPhoto)" controls class="detail-video" />
        <div class="detail-info">
          <div class="detail-name-row">
            <el-input v-model="editingName" size="large" v-if="isEditingName" @keyup.enter="saveName" />
            <h3 v-else style="margin:0">{{ currentPhoto.originalName }}</h3>
            <el-button size="small" @click="startEditName">{{ isEditingName ? t('common.confirm') : t('memory.editName') }}</el-button>
          </div>
          <p v-if="currentPhoto.city || currentPhoto.province"><strong>{{ t('memory.location') }}：</strong>{{ currentPhoto.province || '' }} {{ currentPhoto.city || '' }}</p>
          <p v-if="currentPhoto.shootTime"><strong>{{ t('memory.shootTime') }}：</strong>{{ currentPhoto.shootTime }}</p>
          <p><strong>{{ t('memory.uploadTime') }}：</strong>{{ currentPhoto.createTime }}</p>
          <div v-if="photoTags.length > 0" class="detail-tags">
            <el-tag v-for="tag in photoTags" :key="tag.id" size="small" :color="tag.tagColor || '#409eff'" effect="dark" style="margin-right:6px;margin-bottom:4px">{{ tag.tagName }}</el-tag>
          </div>
          <div class="detail-actions">
            <el-button size="small" type="success" @click="downloadPhoto">{{ t('file.download') }}</el-button>
            <el-select v-model="selectedFamilyId" :placeholder="t('family.shareToFamily')" style="width:200px" size="small">
              <el-option v-for="f in myFamilies" :key="f.id" :label="f.familyName" :value="f.id" />
            </el-select>
            <el-button size="small" type="primary" @click="handleShare" :disabled="!selectedFamilyId">{{ t('family.share') }}</el-button>
            <el-popconfirm :title="t('recycle.confirmDelete')" @confirm="handleDelete">
              <template #reference><el-button size="small" type="danger">{{ t('recycle.delete') }}</el-button></template>
            </el-popconfirm>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 视频播放弹窗 -->
    <el-dialog v-model="videoVisible" width="70%" :title="t('memory.videoPlay')" destroy-on-close>
      <video v-if="currentVideo" :src="getOriginalUrl(currentVideo)" controls autoplay class="detail-video" />
      <template #footer v-if="currentVideo">
        <el-button type="success" @click="downloadPhoto(currentVideo)">{{ t('file.download') }}</el-button>
      </template>
    </el-dialog>

    <!-- 图片悬浮弹窗 -->
    <div v-if="tooltipPhoto" class="photo-tooltip" :style="{ left: tooltipX + 'px', top: tooltipY + 'px' }">
      <img :src="`/api/photo/${tooltipPhoto.id}/thumb`" class="tooltip-img" />
      <div class="tooltip-info">
        <p><strong>{{ tooltipPhoto.originalName }}</strong></p>
        <p v-if="tooltipPhoto.city || tooltipPhoto.province">📍 {{ tooltipPhoto.province || '' }} {{ tooltipPhoto.city || '' }}</p>
        <p v-if="tooltipPhoto.shootTime">🕐 {{ tooltipPhoto.shootTime }}</p>
        <p v-if="tooltipPhoto.width">{{ tooltipPhoto.width }} x {{ tooltipPhoto.height }}</p>
        <p v-if="tooltipPhoto.mimeType">📷 {{ tooltipPhoto.mimeType }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getCityPhotoStats, getPhotosByCity, getPhotoList, searchPhotos, deletePhoto, updatePhotoName, getMyFamilies, shareToFamily, getPhotoDetail } from '../../api'
import { useI18n } from '../../utils/i18n'
import { ElMessage } from 'element-plus'

const { t, lang } = useI18n()

const mapRef = ref()
const cityStats = ref([])
const photos = ref([])
const selectedCity = ref('')
const searchKeyword = ref('')
const detailVisible = ref(false)
const currentPhoto = ref(null)
const videoVisible = ref(false)
const currentVideo = ref(null)
const isEditingName = ref(false)
const editingName = ref('')
const selectedFamilyId = ref(null)
const myFamilies = ref([])
const photoPage = ref(1)
const photoPageSize = ref(10)
const photoTotal = ref(0)
const tooltipPhoto = ref(null)
const tooltipX = ref(0)
const tooltipY = ref(0)
const photoTags = ref([])
let mapChart = null

onMounted(async () => {
  await loadCityStats()
  await loadAllPhotos()
  await nextTick()
  initMap()
})

async function loadCityStats() {
  const res = await getCityPhotoStats()
  if (res.code === 200) cityStats.value = res.data
}

async function loadAllPhotos() {
  const res = await getPhotoList({ page: photoPage.value, size: photoPageSize.value })
  if (res.code === 200) { photos.value = res.data.records || []; photoTotal.value = res.data.total || 0 }
}

async function loadPhotosByCity(city) {
  const res = await getPhotosByCity(city)
  if (res.code === 200) { photos.value = res.data || []; photoTotal.value = res.data.length }
}

async function initMap() {
  try {
    const response = await fetch('/map/china.json')
    const geoJson = await response.json()
    echarts.registerMap('china', geoJson)

    mapChart = echarts.init(mapRef.value)

    const cityData = cityStats.value.map(item => ({
      name: item.city,
      value: item.count
    }))

    mapChart.setOption({
      tooltip: {
        trigger: 'item',
        formatter: (params) => {
          const count = params.value || 0
          return `${params.name}<br/>${t('memory.photoCountLabel')}: ${count}`
        }
      },
      visualMap: {
        min: 0,
        max: Math.max(...cityData.map(d => d.value), 1),
        text: ['多', '少'],
        realtime: false,
        calculable: true,
        inRange: { color: ['#e0f3f8', '#0077b6'] }
      },
      series: [{
        name: '照片分布',
        type: 'map',
        map: 'china',
        roam: true,
        label: { show: false },
        emphasis: {
          label: { show: true, color: '#333' },
          itemStyle: { areaColor: '#ffd166' }
        },
        data: cityData
      }]
    })

    mapChart.on('click', (params) => {
      if (params.componentType === 'series') {
        if (params.value && params.value > 0) {
          selectedCity.value = params.name
          photoPage.value = 1
          loadPhotosByCity(params.name)
        } else {
          selectedCity.value = ''
          photoPage.value = 1
          loadAllPhotos()
        }
      } else {
        // 点击地图空白区域（非省份），显示全部照片
        selectedCity.value = ''
        photoPage.value = 1
        loadAllPhotos()
      }
    })
  } catch (e) {
    console.error('地图加载失败:', e)
  }
}

async function handleSearch() {
  if (!searchKeyword.value || searchKeyword.value.trim() === '') {
    loadAllPhotos()
    return
  }
  const res = await searchPhotos(searchKeyword.value.trim())
  if (res.code === 200) photos.value = res.data
}

function getThumbUrl(photo) {
  return `/api/photo/${photo.id}/thumb`
}

function getOriginalUrl(photo) {
  return `/api/photo/${photo.id}/original`
}

function showDetail(photo) {
  if (photo.mediaType === 'video') {
    currentVideo.value = photo
    videoVisible.value = true
  } else {
    currentPhoto.value = photo
    isEditingName.value = false
    editingName.value = photo.originalName
    detailVisible.value = true
    photoTags.value = []
    loadFamilies()
    loadPhotoTags(photo.id)
  }
}

async function loadPhotoTags(photoId) {
  const res = await getPhotoDetail(photoId)
  if (res.code === 200 && res.data?.tags) photoTags.value = res.data.tags
}

async function loadFamilies() {
  const res = await getMyFamilies()
  if (res.code === 200) myFamilies.value = res.data
}

function startEditName() {
  if (isEditingName.value) {
    saveName()
  } else {
    isEditingName.value = true
  }
}

async function saveName() {
  if (editingName.value.trim() && currentPhoto.value) {
    await updatePhotoName(currentPhoto.value.id, editingName.value.trim())
    currentPhoto.value.originalName = editingName.value.trim()
    isEditingName.value = false
    ElMessage.success(t('memory.nameUpdated'))
  }
}

async function handleDelete() {
  if (currentPhoto.value) {
    await deletePhoto(currentPhoto.value.id)
    ElMessage.success(t('recycle.delete'))
    detailVisible.value = false
    loadAllPhotos()
  }
}

async function handleShare() {
  if (currentPhoto.value && selectedFamilyId.value) {
    await shareToFamily(selectedFamilyId.value, currentPhoto.value.id)
    ElMessage.success(t('common.success'))
  }
}

function downloadPhoto(photo) {
  const p = photo || currentPhoto.value
  if (p) window.open(`/api/photo/${p.id}/original`)
}

function handlePageChange(page) {
  photoPage.value = page
  if (selectedCity.value) loadPhotosByCity(selectedCity.value)
  else loadAllPhotos()
}

function handleSizeChange(size) {
  photoPageSize.value = size
  photoPage.value = 1
  if (selectedCity.value) loadPhotosByCity(selectedCity.value)
  else loadAllPhotos()
}

function showTooltip(photo, event) {
  tooltipPhoto.value = photo
  tooltipX.value = event.clientX + 16
  tooltipY.value = event.clientY - 150
}

function hideTooltip() { tooltipPhoto.value = null }

function handleImgError(e) {
  e.target.style.display = 'none'
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return dateStr.substring(0, 10)
}
</script>

<style scoped>
.map-card {
  height: calc(100vh - 140px);
}
.map-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.map-container {
  width: 100%;
  height: 600px;
}
.photo-list-card {
  height: calc(100vh - 140px);
  overflow-y: auto;
}
.photo-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}
@media (max-width: 768px) {
  .photo-grid { grid-template-columns: repeat(3, 1fr); gap: 6px; }
  .map-container { height: 250px; }
  .map-header { flex-direction: column; gap: 8px; }
  .map-header .el-input { width: 100% !important; }
}
.photo-item {
  cursor: pointer;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f7fa;
  transition: transform 0.2s;
}
.photo-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}
.photo-thumb-wrap {
  position: relative;
}
.photo-thumb {
  width: 100%;
  height: 120px;
  object-fit: cover;
  display: block;
}
.video-play-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(0,0,0,0.5);
  border-radius: 50%;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}
.thumb-fallback {
  width: 100%;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ccc;
}
.photo-meta {
  padding: 6px 8px;
  font-size: 12px;
  color: #666;
}
.photo-city {
  display: flex;
  align-items: center;
  gap: 2px;
  color: #409eff;
}
.photo-time {
  display: block;
  margin-top: 2px;
  color: #999;
}
.detail-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.detail-image {
  max-height: 60vh;
  width: 100%;
}
.detail-video {
  width: 100%;
  max-height: 60vh;
}
.detail-fallback {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
}
.detail-info {
  width: 100%;
  margin-top: 16px;
  font-size: 14px;
  line-height: 2;
}
.detail-name-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.detail-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #eee;
}
.detail-tags {
  margin-top: 8px;
}
.pagination-bar {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
.photo-tooltip {
  position: fixed;
  z-index: 10000;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(0,0,0,0.15);
  padding: 12px;
  width: 280px;
  pointer-events: none;
}
.tooltip-img {
  width: 100%;
  height: 160px;
  object-fit: cover;
  border-radius: 8px;
}
.tooltip-info {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.8;
  color: #666;
}
.tooltip-info p { margin: 0; }
</style>
