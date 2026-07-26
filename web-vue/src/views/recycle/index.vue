<template>
  <div class="recycle-page" :key="lang">
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>{{ t('recycle.title') }}</span>
          <el-popconfirm :title="t('recycle.confirmEmpty')" @confirm="handleEmpty">
            <template #reference><el-button type="danger" size="small">{{ t('recycle.empty') }}</el-button></template>
          </el-popconfirm>
        </div>
      </template>
      <el-table :data="photos" stripe v-if="photos.length > 0">
        <el-table-column label="" width="80">
          <template #default="{ row }">
            <img :src="`/api/photo/${row.id}/thumb`" style="width:50px;height:50px;object-fit:cover;border-radius:4px" />
          </template>
        </el-table-column>
        <el-table-column :label="t('recycle.fileName')">
          <template #default="{ row }">{{ row.originalName || '-' }}</template>
        </el-table-column>
        <el-table-column :label="t('recycle.deleteTime')" width="180">
          <template #default="{ row }">{{ row.deletedTime || '-' }}</template>
        </el-table-column>
        <el-table-column :label="t('recycle.actions')" width="200">
          <template #default="{ row }">
            <el-button size="small" type="success" @click="handleRestore(row.id)">{{ t('recycle.restore') }}</el-button>
            <el-popconfirm :title="t('recycle.confirmPermanent')" @confirm="handlePermanent(row.id)">
              <template #reference><el-button size="small" type="danger">{{ t('recycle.permanentDelete') }}</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else :description="t('recycle.emptyList')" />
      <el-pagination v-if="total > 0" v-model:current-page="page" :page-size="20" :total="total" layout="total, prev, pager, next" style="margin-top:16px;justify-content:flex-end" @current-change="loadData" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getRecycleList, restorePhoto, permanentDelete, emptyRecycle } from '../../api'
import { useI18n } from '../../utils/i18n'
import { ElMessage } from 'element-plus'

const { t, lang } = useI18n()
const photos = ref([])
const page = ref(1)
const total = ref(0)

onMounted(() => loadData())

async function loadData() {
  const res = await getRecycleList({ page: page.value, size: 20 })
  if (res.code === 200) { photos.value = res.data.records || []; total.value = res.data.total || 0 }
}

async function handleRestore(id) { await restorePhoto(id); ElMessage.success(t('recycle.restored')); loadData() }
async function handlePermanent(id) { await permanentDelete(id); ElMessage.success(t('recycle.permanentlyDeleted')); loadData() }
async function handleEmpty() { await emptyRecycle(); ElMessage.success(t('recycle.emptied')); loadData() }
</script>
