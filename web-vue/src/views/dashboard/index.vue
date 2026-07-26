<template>
  <div class="dashboard" :key="lang">
    <el-row :gutter="12" class="stat-cards">
      <el-col :xs="12" :sm="12" :md="6" v-for="(card, idx) in statCards" :key="idx">
        <el-card shadow="hover" class="stat-card" style="cursor:pointer" @click="card.action && card.action()">
          <div class="stat-icon" :style="{ background: card.bg, color: card.color }"><el-icon :size="28"><component :is="card.icon" /></el-icon></div>
          <div class="stat-info"><div class="stat-value">{{ card.value }}</div><div class="stat-label">{{ card.label }}</div></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 管理员：用户数据概览 -->
    <el-card v-if="userStats.length > 0" style="margin-top:20px">
      <template #header>
        <span>👥 {{ t('dashboard.userStats') }}</span>
      </template>
      <el-table :data="userStats" stripe>
        <el-table-column :label="t('dashboard.username')">
          <template #default="{ row }">{{ row.nickname || '-' }}</template>
        </el-table-column>
        <el-table-column prop="photo_count" :label="t('dashboard.photos')" width="100" />
        <el-table-column prop="video_count" :label="t('dashboard.videos')" width="100" />
        <el-table-column prop="total_size" :label="t('dashboard.storage')" width="120">
          <template #default="{ row }">{{ formatSize(row.total_size) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-row :gutter="16" style="margin-top:16px">
      <el-col :xs="24" :sm="24" :md="16">
        <el-card>
          <template #header>📸 {{ t('dashboard.recentUploads') }}</template>
          <div class="recent-photos" v-if="recentPhotos.length > 0">
            <div v-for="photo in recentPhotos" :key="photo.id" class="photo-thumb" @mouseenter="showTooltip(photo, $event)" @mouseleave="hideTooltip">
              <img :src="`/api/photo/${photo.id}/thumb`" class="thumb-img" />
              <div class="photo-name">{{ photo.originalName }}</div>
            </div>
          </div>
          <el-empty v-else :description="t('dashboard.noPhotos')" />
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :md="8">
        <el-card>
          <template #header>🌍 {{ t('dashboard.cityDistribution') }}</template>
          <div ref="chartRef" style="height:300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 任务中心 -->
    <el-row :gutter="16" style="margin-top:16px" v-if="hasPendingTasks">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>📋 {{ t('task.title') }}</span>
              <el-tag type="warning" size="small">{{ pendingCount }} {{ t('task.pending') }}</el-tag>
            </div>
          </template>
          <el-table :data="pendingTasks" stripe max-height="300">
            <el-table-column prop="type" :label="t('task.type')" width="120">
              <template #default="{ row }">
                <el-tag :type="row.type === 'family' ? 'primary' : 'success'" size="small">
                  {{ row.type === 'family' ? t('task.familyJoin') : t('task.friendRequest') }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="username" :label="t('task.applicant')" />
            <el-table-column prop="targetName" :label="t('task.target')" />
            <el-table-column :label="t('family.actions')" width="200">
              <template #default="{ row }">
                <el-button size="small" type="success" @click="handleApproveTask(row)">{{ t('family.approve') }}</el-button>
                <el-button size="small" type="danger" @click="handleRejectTask(row)">{{ t('family.reject') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图片悬浮弹窗 -->
    <div v-if="tooltipPhoto" class="photo-tooltip" :style="{ left: tooltipX + 'px', top: tooltipY + 'px' }">
      <img :src="`/api/photo/${tooltipPhoto.id}/thumb`" class="tooltip-img" />
      <div class="tooltip-info">
        <p><strong>{{ tooltipPhoto.originalName || '' }}</strong></p>
        <p v-if="tooltipPhoto.city">📍 {{ tooltipPhoto.province }} {{ tooltipPhoto.city }}</p>
        <p v-if="tooltipPhoto.shootTime">🕐 {{ tooltipPhoto.shootTime }}</p>
        <p v-if="tooltipPhoto.width">{{ tooltipPhoto.width }} x {{ tooltipPhoto.height }}</p>
        <p v-if="tooltipPhoto.mimeType">📷 {{ tooltipPhoto.mimeType }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { getPhotoList, getCityPhotoStats, getTagList, getMyFamilies, getFamilyPending, approveMember, rejectMember, getPendingFriendRequests, acceptFriendRequest, rejectFriendRequest } from '../../api'
import { useI18n } from '../../utils/i18n'
import { ElMessage } from 'element-plus'

const { t, lang } = useI18n()
const router = useRouter()
const chartRef = ref()
const stats = ref({ totalPhotos: 0, totalTags: 0, totalCities: 0, todayUpload: 0 })
const recentPhotos = ref([])
const userStats = ref([])

// 任务中心
const pendingTasks = ref([])
const pendingCount = ref(0)
const hasPendingTasks = computed(() => pendingTasks.value.length > 0)

const statCards = computed(() => [
  { value: stats.value.totalPhotos, label: t('dashboard.totalPhotos'), icon: 'Picture', bg: '#409eff20', color: '#409eff', action: () => router.push('/photo/overview') },
  { value: stats.value.totalTags, label: t('dashboard.totalTags'), icon: 'PriceTag', bg: '#67c23a20', color: '#67c23a', action: () => router.push('/tag') },
  { value: stats.value.totalCities, label: t('dashboard.totalCities'), icon: 'FolderOpened', bg: '#e6a23c20', color: '#e6a23c', action: () => router.push('/city') },
  { value: stats.value.todayUpload, label: t('dashboard.todayUpload'), icon: 'Calendar', bg: '#f56c6c20', color: '#f56c6c', action: null }
])
const tooltipPhoto = ref(null)
const tooltipX = ref(0)
const tooltipY = ref(0)

onMounted(async () => {
  try {
    const [photoRes, cityRes, tagRes] = await Promise.all([
      getPhotoList({ page: 1, size: 8 }),
      getCityPhotoStats(),
      getTagList()
    ])
    if (photoRes.code === 200) { recentPhotos.value = photoRes.data.records || []; stats.value.totalPhotos = photoRes.data.total || 0 }
    if (cityRes.code === 200) { stats.value.totalCities = cityRes.data.length; initChart(cityRes.data) }
    if (tagRes.code === 200) stats.value.totalTags = tagRes.data.length

    // 加载任务中心
    loadPendingTasks()

    // 加载用户统计数据
    try {
      const userRes = await fetch('/api/photo/user-stats', { headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` } })
      const userData = await userRes.json()
      if (userData.code === 200) userStats.value = userData.data || []
    } catch (e) { /* 非管理员可能没权限 */ }
  } catch (e) { /* ignore */ }
})

function initChart(cityData) {
  if (!chartRef.value) return
  const chart = echarts.init(chartRef.value)
  chart.setOption({
    tooltip: { trigger: 'item' },
    series: [{ type: 'pie', radius: ['40%', '70%'], data: cityData.map(i => ({ name: i.city, value: i.count })), emphasis: { itemStyle: { shadowBlur: 10 } } }]
  })
  chart.on('click', (params) => {
    if (params.name) {
      router.push({ path: '/photo/overview', query: { city: params.name } })
    }
  })
}

function showTooltip(photo, event) {
  tooltipPhoto.value = photo
  tooltipX.value = event.clientX + 16
  tooltipY.value = event.clientY - 100
}

function hideTooltip() { tooltipPhoto.value = null }

async function loadPendingTasks() {
  try {
    const allTasks = []
    // 加载家庭申请
    const familyRes = await getMyFamilies()
    if (familyRes.code === 200) {
      for (const f of (familyRes.data || [])) {
        try {
          const pendingRes = await getFamilyPending(f.id)
          if (pendingRes.code === 200 && pendingRes.data) {
            for (const m of pendingRes.data) {
              allTasks.push({ ...m, type: 'family', targetName: f.familyName, familyId: f.id })
            }
          }
        } catch {}
      }
    }
    // 加载好友请求
    const friendRes = await getPendingFriendRequests()
    if (friendRes.code === 200) {
      for (const r of (friendRes.data || [])) {
        allTasks.push({ ...r, type: 'friend', targetName: '' })
      }
    }
    pendingTasks.value = allTasks
    pendingCount.value = allTasks.length
  } catch {}
}

async function handleApproveTask(row) {
  if (row.type === 'family') {
    await approveMember(row.id)
  } else {
    await acceptFriendRequest(row.id)
  }
  ElMessage.success(t('common.success'))
  loadPendingTasks()
}

async function handleRejectTask(row) {
  if (row.type === 'family') {
    await rejectMember(row.id)
  } else {
    await rejectFriendRequest(row.id)
  }
  ElMessage.success(t('common.success'))
  loadPendingTasks()
}

function formatSize(bytes) {
  if (!bytes) return '0B'
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + 'KB'
  if (bytes < 1073741824) return (bytes / 1048576).toFixed(1) + 'MB'
  return (bytes / 1073741824).toFixed(1) + 'GB'
}
</script>

<style scoped>
.stat-cards .stat-card { display: flex; align-items: center; padding: 20px; }
.stat-card :deep(.el-card__body) { display: flex; align-items: center; gap: 16px; width: 100%; }
.stat-icon { width: 64px; height: 64px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.stat-value { font-size: 28px; font-weight: bold; color: #333; }
.stat-label { font-size: 14px; color: #999; margin-top: 4px; }
.recent-photos { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
@media (max-width: 768px) {
  .recent-photos { grid-template-columns: repeat(2, 1fr); }
  .stat-card :deep(.el-card__body) { padding: 12px; }
  .stat-icon { width: 48px; height: 48px; }
  .stat-value { font-size: 20px; }
  .photo-tooltip { display: none !important; }
}
.photo-thumb { text-align: center; cursor: pointer; position: relative; }
.thumb-img { width: 100%; height: 120px; border-radius: 8px; object-fit: cover; }
.photo-name { font-size: 12px; color: #666; margin-top: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.photo-tooltip { position: fixed; z-index: 10000; background: white; border-radius: 12px; box-shadow: 0 8px 30px rgba(0,0,0,0.15); padding: 12px; width: 280px; pointer-events: none; }
.tooltip-img { width: 100%; height: 160px; object-fit: cover; border-radius: 8px; }
.tooltip-info { margin-top: 8px; font-size: 12px; line-height: 1.8; color: #666; }
.tooltip-info p { margin: 0; }
</style>
