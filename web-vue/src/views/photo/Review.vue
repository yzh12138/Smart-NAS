<template>
  <div class="review-page" :key="lang">
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <div style="display:flex;align-items:center;gap:12px">
            <span>{{ t('review.title') }}</span>
            <el-tag v-if="selectedIds.length > 0" type="primary">{{ selectedIds.length }} {{ t('overview.selected') }}</el-tag>
          </div>
          <div style="display:flex;gap:12px;align-items:center">
            <el-button v-if="!selectMode" type="primary" size="small" @click="handleBatchScanAll">
              {{ t('review.batchScanAll') }}
            </el-button>
            <el-button v-if="selectMode && selectedIds.length > 0" type="primary" size="small" @click="handleBatchScanSelected">
              {{ t('review.batchScanSelected') }} ({{ selectedIds.length }})
            </el-button>
            <el-button v-if="selectedIds.length > 0" size="small" @click="toggleSelectMode">{{ t('overview.cancelSelect') }}</el-button>
            <el-button v-else size="small" @click="toggleSelectMode">{{ t('overview.selectMode') }}</el-button>
          </div>
        </div>
      </template>
      <el-table :data="reviewList" stripe v-if="reviewList.length > 0" @selection-change="handleSelectionChange">
        <el-table-column v-if="selectMode" type="selection" width="55" />
        <el-table-column label="" width="80">
          <template #default="{ row }">
            <img :src="`/api/photo/${row.id}/thumb`" style="width:50px;height:50px;object-fit:cover;border-radius:4px" />
          </template>
        </el-table-column>
        <el-table-column prop="original_name" :label="t('recycle.fileName')" />
        <el-table-column :label="t('review.aiTags')">
          <template #default="{ row }">
            <el-tag v-for="tag in parseTags(row.ai_tag_names)" :key="tag" size="small" effect="plain" style="margin-right:4px;margin-bottom:4px">{{ tag }}</el-tag>
            <span v-if="!row.ai_tag_names" style="color:#999">{{ t('review.noTags') }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('review.city')">
          <template #default="{ row }">{{ row.city || '-' }}</template>
        </el-table-column>
        <el-table-column :label="t('review.location')" width="120">
          <template #default="{ row }">
            <span v-if="row.gps_lat && row.gps_lng" style="font-size:12px;color:#999">{{ row.gps_lat?.toFixed(4) }}, {{ row.gps_lng?.toFixed(4) }}</span>
            <span v-else style="color:#ccc">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('family.actions')" width="200">
          <template #default="{ row }">
            <el-button size="small" link @click="handleEdit(row)">{{ t('common.edit') }}</el-button>
            <el-popconfirm :title="t('review.confirmApprove')" @confirm="handleQuickApprove(row)">
              <template #reference>
                <el-button size="small" type="success" link>{{ t('review.approve') }}</el-button>
              </template>
            </el-popconfirm>
            <el-popconfirm :title="t('review.confirmReject')" @confirm="handleQuickReject(row)">
              <template #reference>
                <el-button size="small" type="danger" link>{{ t('review.reject') }}</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else :description="t('review.empty')" />
    </el-card>

    <!-- 扫描进度弹窗 -->
    <el-dialog v-model="scanProgressVisible" :title="t('review.scanProgress')" width="500px" :close-on-click-modal="false" :show-close="!scanRunning">
      <div v-if="scanProgress">
        <el-progress :percentage="scanProgressPercent" :status="scanProgressPercent === 100 ? 'success' : ''" style="margin-bottom:16px" />
        <p>{{ t('review.scanning') }} {{ scanProgress.current }} / {{ scanProgress.total }}</p>
        <p v-if="scanProgress.currentPhoto" style="color:#999;font-size:12px">{{ scanProgress.currentPhoto }}</p>
        <p style="margin-top:8px">
          <el-tag type="success" size="small">{{ t('review.success') }}: {{ scanProgress.success }}</el-tag>
          <el-tag type="danger" size="small" style="margin-left:8px">{{ t('review.failed') }}: {{ scanProgress.failed }}</el-tag>
          <el-tag v-if="scanProgress.skipped > 0" type="info" size="small" style="margin-left:8px">{{ t('review.skipped') }}: {{ scanProgress.skipped }}</el-tag>
        </p>
      </div>
      <template #footer>
        <el-button v-if="scanRunning && !scanPaused" @click="pauseScan">{{ t('review.pause') }}</el-button>
        <el-button v-if="scanRunning && scanPaused" type="primary" @click="resumeScan">{{ t('review.resume') }}</el-button>
        <el-button v-if="scanRunning" type="danger" @click="cancelScan">{{ t('review.cancelScan') }}</el-button>
        <el-button v-if="!scanRunning" @click="scanProgressVisible = false">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reviewDialogVisible" :title="t('review.reviewDetail')" width="600px">
      <div v-if="currentReview" style="text-align:center">
        <img :src="`/api/photo/${currentReview.id}/original`" style="max-width:100%;max-height:400px;object-fit:contain;border-radius:8px" />
        <div style="margin-top:16px">
          <p><strong>{{ currentReview.original_name || '' }}</strong></p>
          <p v-if="currentReview.city || currentReview.province">{{ currentReview.province || '' }} {{ currentReview.city || '' }}</p>
          <p v-if="currentReview.gps_lat && currentReview.gps_lng" style="font-size:12px;color:#999">
            {{ t('review.location') }}: {{ currentReview.gps_lat?.toFixed(6) }}, {{ currentReview.gps_lng?.toFixed(6) }}
          </p>
        </div>
        <div style="margin-top:16px">
          <p style="text-align:left;margin-bottom:8px">{{ t('review.selectTags') }}</p>
          <el-checkbox-group v-model="selectedTags">
            <el-checkbox v-for="tag in availableTags" :key="tag" :label="tag" style="margin-right:12px;margin-bottom:8px" />
          </el-checkbox-group>
        </div>
        <div style="margin-top:12px">
          <el-input v-model="customTag" :placeholder="t('review.customTagPlaceholder')" style="width:200px;margin-right:8px" />
          <el-button size="small" @click="addCustomTag">{{ t('review.addTag') }}</el-button>
        </div>
      </div>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="submitReview(true)">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getAiReviewQueue, reviewAiTags, scanSinglePhoto, getPhotoList } from '../../api'
import { useI18n } from '../../utils/i18n'
import { ElMessage } from 'element-plus'

const { t, lang } = useI18n()
const reviewList = ref([])
const reviewDialogVisible = ref(false)
const currentReview = ref(null)
const selectedTags = ref([])
const availableTags = ref([])
const customTag = ref('')
const selectMode = ref(false)
const selectedIds = ref([])

// 扫描进度相关
const scanProgressVisible = ref(false)
const scanRunning = ref(false)
const scanPaused = ref(false)
const scanProgress = ref(null)
const scanQueue = ref([])
const scanIndex = ref(0)
const scanCancelled = ref(false)

const scanProgressPercent = computed(() => {
  if (!scanProgress.value || scanProgress.value.total === 0) return 0
  return Math.round((scanProgress.value.current / scanProgress.value.total) * 100)
})

onMounted(() => loadReviewQueue())

async function loadReviewQueue() {
  const res = await getAiReviewQueue()
  if (res.code === 200) reviewList.value = res.data || []
}

function parseTags(tagNames) {
  if (!tagNames) return []
  return tagNames.split(/[,，]/).map(t => t.trim()).filter(t => t)
}

function toggleSelectMode() {
  selectMode.value = !selectMode.value
  if (!selectMode.value) selectedIds.value = []
}

function handleSelectionChange(rows) {
  selectedIds.value = rows.map(r => r.id)
}

async function handleBatchScanAll() {
  const res = await getPhotoList({ page: 1, size: 10000 })
  if (res.code !== 200) return
  const allPhotos = (res.data.records || []).filter(p => p.mediaType === 'image' && !p.aiAnalyzed)
  if (allPhotos.length === 0) {
    ElMessage.info(t('review.noPhotosToScan'))
    return
  }
  startScan(allPhotos)
}

function handleBatchScanSelected() {
  const photos = reviewList.value.filter(r => selectedIds.value.includes(r.id))
  if (photos.length === 0) return
  startScan(photos)
}

function startScan(photos) {
  scanQueue.value = photos
  scanIndex.value = 0
  scanCancelled.value = false
  scanPaused.value = false
  scanProgress.value = { total: photos.length, current: 0, success: 0, failed: 0, skipped: 0, currentPhoto: '' }
  scanProgressVisible.value = true
  scanRunning.value = true
  runNextScan()
}

async function runNextScan() {
  while (scanIndex.value < scanQueue.value.length && !scanCancelled.value) {
    if (scanPaused.value) {
      await new Promise(r => setTimeout(r, 200))
      continue
    }
    const photo = scanQueue.value[scanIndex.value]
    scanProgress.value.currentPhoto = photo.originalName || ''
    scanProgress.value.current = scanIndex.value + 1
    try {
      const res = await scanSinglePhoto(photo.id)
      if (res.code === 200 && res.data.success) {
        scanProgress.value.success++
      } else {
        scanProgress.value.failed++
      }
    } catch {
      scanProgress.value.failed++
    }
    scanIndex.value++
  }
  scanRunning.value = false
  scanProgress.value.current = scanProgress.value.total
  loadReviewQueue()
  selectedIds.value = []
  selectMode.value = false
}

function pauseScan() { scanPaused.value = true }
function resumeScan() { scanPaused.value = false }
function cancelScan() { scanCancelled.value = true; scanRunning.value = false }

function handleEdit(row) {
  currentReview.value = row
  availableTags.value = parseTags(row.ai_tag_names)
  selectedTags.value = [...availableTags.value]
  reviewDialogVisible.value = true
}

async function handleQuickApprove(row) {
  const tags = parseTags(row.ai_tag_names)
  await reviewAiTags(row.id, {
    tags: tags,
    city: row.city,
    province: row.province,
    approved: true
  })
  ElMessage.success(t('review.approved'))
  loadReviewQueue()
}

async function handleQuickReject(row) {
  await reviewAiTags(row.id, {
    tags: [],
    city: row.city,
    province: row.province,
    approved: false
  })
  ElMessage.success(t('review.rejected'))
  loadReviewQueue()
}

function addCustomTag() {
  if (customTag.value.trim() && !selectedTags.value.includes(customTag.value.trim())) {
    selectedTags.value.push(customTag.value.trim())
    if (!availableTags.value.includes(customTag.value.trim())) {
      availableTags.value.push(customTag.value.trim())
    }
    customTag.value = ''
  }
}

async function submitReview(approved) {
  await reviewAiTags(currentReview.value.id, {
    tags: approved ? selectedTags.value : [],
    city: currentReview.value.city,
    province: currentReview.value.province,
    approved
  })
  ElMessage.success(approved ? t('review.approved') : t('review.rejected'))
  reviewDialogVisible.value = false
  loadReviewQueue()
}
</script>

<style scoped>
.review-page { max-width: 1200px; margin: 0 auto; }
</style>
