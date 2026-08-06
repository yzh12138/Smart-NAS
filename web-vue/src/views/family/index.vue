<template>
  <div class="family-page" :key="lang">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card>
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>{{ t('family.myFamilies') }}</span>
              <el-button size="small" @click="joinDialogVisible = true">{{ t('family.joinFamily') }}</el-button>
            </div>
          </template>
          <div v-for="f in families" :key="f.id" class="family-item" :class="{ active: selectedFamily?.id === f.id }" @click="selectFamily(f)">
            <div>
              <span>{{ f.familyName }}</span>
            </div>
            <el-tag size="small" v-if="f.ownerId === currentUserId">{{ t('family.owner') }}</el-tag>
          </div>
          <el-empty v-if="families.length === 0" :description="t('family.noFamily')" />
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card>
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>{{ selectedFamily ? selectedFamily.familyName : t('family.selectFamily') }}</span>
              <div v-if="selectedFamily" style="display:flex;gap:8px">
                <el-tag size="small" type="info">{{ t('family.inviteCode') }}: {{ selectedFamily.familyCode }}</el-tag>
                <el-button size="small" @click="loadMyMedia(); shareDialogVisible = true">{{ t('family.shareMedia') }}</el-button>
              </div>
            </div>
          </template>
          <div v-if="mediaList.length > 0">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px" v-if="hasOwnPhotos">
              <el-checkbox v-model="selectAll" :indeterminate="isIndeterminate" @change="handleSelectAll">{{ t('family.selectAll') || '全选我的照片' }}</el-checkbox>
              <el-button size="small" type="danger" :disabled="selectedPhotoIds.length === 0" @click="handleBatchUnshare">
                {{ t('family.batchUnshare') || '批量取消共享' }} ({{ selectedPhotoIds.length }})
              </el-button>
            </div>
            <div class="media-grid">
              <div v-for="photo in mediaList" :key="photo.id" class="media-item" :class="{ 'is-selected': selectedPhotoIds.includes(photo.id) }">
                <div class="media-thumb-wrap">
                  <img :src="`/api/photo/${photo.id}/thumb`" class="media-thumb" />
                  <div v-if="photo.mediaType === 'video'" class="media-play-icon">
                    <el-icon :size="24"><VideoPlay /></el-icon>
                  </div>
                  <div class="media-unshare" v-if="photo.userId === currentUserId" @click.stop="handleUnshare(photo.id)">
                    <el-icon><Close /></el-icon>
                  </div>
                  <div class="media-select" v-if="photo.userId === currentUserId" @click.stop="toggleSelect(photo.id)">
                    <el-icon v-if="selectedPhotoIds.includes(photo.id)" color="#409eff" :size="20"><CircleCheck /></el-icon>
                    <el-icon v-else :size="20" color="#c0c4cc"><CircleCheck /></el-icon>
                  </div>
                </div>
                <div class="media-name">{{ photo.originalName }}</div>
                <div class="media-type">{{ photo.mediaType === 'video' ? t('mediaType.video') : t('mediaType.photo') }}</div>
              </div>
            </div>
          </div>
          <el-empty v-else :description="t('family.noMedia')" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 加入家庭弹窗 -->
    <el-dialog v-model="joinDialogVisible" :title="t('family.joinFamily')" width="400px">
      <el-input v-model="joinCode" :placeholder="t('family.enterInviteCode')" clearable style="margin-bottom:16px" />
      <div v-if="joinLoading" style="text-align:center;padding:12px;color:#999">{{ t('common.loading') }}</div>
      <div v-else-if="joinResult" style="padding:12px;background:#f0f9ff;border-radius:8px;border:1px solid #b3d8ff">
        <p style="margin:0 0 4px"><strong>{{ joinResult.familyName }}</strong></p>
        <p style="margin:0;color:#666;font-size:12px">{{ joinResult.description || t('family.noDescription') }}</p>
      </div>
      <div v-else-if="joinCode && joinCode.length >= 4 && !joinResult" style="padding:12px;color:#999;text-align:center">
        {{ t('family.familyNotFound') }}
      </div>
      <template #footer>
        <el-button @click="joinDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleJoinFamily" :disabled="!joinCode || !joinResult || joinLoading">{{ t('family.join') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="shareDialogVisible" :title="t('family.shareToFamily')" width="600px">
      <el-table :data="myMedia" stripe max-height="400">
        <el-table-column label="" width="60">
          <template #default="{ row }">
            <div style="position:relative">
              <img :src="`/api/photo/${row.id}/thumb`" style="width:40px;height:40px;object-fit:cover;border-radius:4px" />
              <el-icon v-if="row.mediaType === 'video'" style="position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);color:white;background:rgba(0,0,0,0.5);border-radius:50%;padding:2px"><VideoPlay /></el-icon>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="originalName" :label="t('recycle.fileName')" />
        <el-table-column :label="t('mediaType.type')" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.mediaType === 'video' ? 'warning' : 'info'">{{ row.mediaType === 'video' ? t('mediaType.video') : t('mediaType.photo') }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('family.action')" width="100">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="handleShare(row.id)">{{ t('family.share') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { getMyFamilies, getFamilyMedia, shareToFamily, getPhotoList, searchFamilyByCode, joinFamily, batchUnshareFromFamily } from '../../api'
import { useI18n } from '../../utils/i18n'
import { ElMessage } from 'element-plus'

const { t, lang } = useI18n()
const families = ref([])
const selectedFamily = ref(null)
const mediaList = ref([])
const myMedia = ref([])
const shareDialogVisible = ref(false)
const currentUserId = ref(null)
const selectedPhotoIds = ref([])

const ownPhotoIds = computed(() => mediaList.value.filter(p => p.userId === currentUserId.value).map(p => p.id))
const hasOwnPhotos = computed(() => ownPhotoIds.value.length > 0)
const selectAll = computed({
  get: () => ownPhotoIds.value.length > 0 && selectedPhotoIds.value.length === ownPhotoIds.value.length,
  set: () => {}
})
const isIndeterminate = computed(() => selectedPhotoIds.value.length > 0 && selectedPhotoIds.value.length < ownPhotoIds.value.length)

// 加入家庭
const joinDialogVisible = ref(false)
const joinCode = ref('')
const joinResult = ref(null)
const joinLoading = ref(false)

onMounted(async () => {
  const res = await getMyFamilies()
  if (res.code === 200) {
    families.value = res.data
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    currentUserId.value = userInfo.userId || null
  }
})

watch(joinCode, async (val) => {
  if (!val || val.length < 4) { joinResult.value = null; return }
  joinLoading.value = true
  try {
    const res = await searchFamilyByCode(val)
    if (res.code === 200 && res.data) {
      joinResult.value = res.data
    } else {
      joinResult.value = null
    }
  } catch { joinResult.value = null }
  joinLoading.value = false
})


async function selectFamily(f) {
  selectedFamily.value = f
  selectedPhotoIds.value = []
  const res = await getFamilyMedia(f.id)
  if (res.code === 200) mediaList.value = res.data
}

async function handleShare(photoId) {
  await shareToFamily(selectedFamily.value.id, photoId)
  ElMessage.success(t('common.success'))
  shareDialogVisible.value = false
  selectFamily(selectedFamily.value)
}

async function loadMyMedia() {
  const res = await getPhotoList({ page: 1, size: 1000 })
  if (res.code === 200) myMedia.value = res.data.records || []
}

async function handleJoinFamily() {
  if (!joinResult.value) return
  await joinFamily(joinResult.value.id)
  ElMessage.success(t('family.joinSuccess'))
  joinDialogVisible.value = false
  joinCode.value = ''
  joinResult.value = null
  const res = await getMyFamilies()
  if (res.code === 200) families.value = res.data
}

async function handleUnshare(photoId) {
  await batchUnshareFromFamily([photoId])
  ElMessage.success(t('common.success'))
  selectedPhotoIds.value = selectedPhotoIds.value.filter(id => id !== photoId)
  selectFamily(selectedFamily.value)
}

function toggleSelect(photoId) {
  const idx = selectedPhotoIds.value.indexOf(photoId)
  if (idx >= 0) {
    selectedPhotoIds.value.splice(idx, 1)
  } else {
    selectedPhotoIds.value.push(photoId)
  }
}

function handleSelectAll(val) {
  if (val) {
    selectedPhotoIds.value = [...ownPhotoIds.value]
  } else {
    selectedPhotoIds.value = []
  }
}

async function handleBatchUnshare() {
  if (selectedPhotoIds.value.length === 0) return
  try {
    await batchUnshareFromFamily(selectedPhotoIds.value)
    ElMessage.success(t('common.success'))
    selectedPhotoIds.value = []
    selectFamily(selectedFamily.value)
  } catch {
    ElMessage.error(t('common.error') || '操作失败')
  }
}
</script>

<style scoped>
.family-item { padding: 12px; cursor: pointer; border-radius: 8px; display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.family-item:hover, .family-item.active { background: #ecf5ff; }
.media-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.media-item { border-radius: 8px; overflow: hidden; background: #f5f7fa; }
.media-thumb-wrap { position: relative; }
.media-thumb { width: 100%; height: 120px; object-fit: cover; display: block; }
.media-play-icon { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); background: rgba(0,0,0,0.5); border-radius: 50%; width: 40px; height: 40px; display: flex; align-items: center; justify-content: center; color: white; }
.media-name { padding: 6px 8px 2px; font-size: 12px; color: #666; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.media-type { padding: 0 8px 6px; font-size: 11px; color: #999; }
.media-unshare { position: absolute; top: 4px; right: 4px; width: 24px; height: 24px; border-radius: 50%; background: rgba(245,108,108,0.8); color: white; display: flex; align-items: center; justify-content: center; cursor: pointer; opacity: 0; transition: opacity 0.2s; font-size: 12px; }
.media-item:hover .media-unshare { opacity: 1; }
.media-select { position: absolute; top: 4px; left: 4px; cursor: pointer; opacity: 0; transition: opacity 0.2s; }
.media-item:hover .media-select, .media-item.is-selected .media-select { opacity: 1; }
.media-item.is-selected { outline: 2px solid #409eff; border-radius: 8px; }
</style>
