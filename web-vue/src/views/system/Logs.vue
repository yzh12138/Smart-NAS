<template>
  <div class="logs-page" :key="lang">
    <el-card>
      <template #header>{{ t('menu.logs') }}</template>
      <el-form :inline="true" style="margin-bottom:16px">
        <el-form-item :label="t('logs.user')">
          <el-input v-model="filters.userId" :placeholder="t('logs.userIdPlaceholder')" style="width:120px" />
        </el-form-item>
        <el-form-item :label="t('logs.action')">
          <el-input v-model="filters.action" :placeholder="t('logs.actionPlaceholder')" style="width:160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">{{ t('common.search') }}</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="logs" stripe>
        <el-table-column :label="t('logs.time')" width="180">
          <template #default="{ row }">{{ row.createTime || '-' }}</template>
        </el-table-column>
        <el-table-column :label="t('logs.user')" width="120">
          <template #default="{ row }">{{ row.username || '-' }}</template>
        </el-table-column>
        <el-table-column :label="t('logs.action')" width="150">
          <template #default="{ row }">{{ row.action || '-' }}</template>
        </el-table-column>
        <el-table-column :label="t('logs.target')" width="100">
          <template #default="{ row }">{{ row.targetType || '-' }}</template>
        </el-table-column>
        <el-table-column :label="t('logs.detail')">
          <template #default="{ row }">{{ row.detail || '-' }}</template>
        </el-table-column>
        <el-table-column :label="t('logs.ip')" width="130">
          <template #default="{ row }">{{ row.ipAddress || '-' }}</template>
        </el-table-column>
      </el-table>
      <el-pagination v-if="total > 0" v-model:current-page="page" :page-size="20" :total="total" layout="total, prev, pager, next" style="margin-top:16px;justify-content:flex-end" @current-change="loadData" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getLogList } from '../../api'
import { useI18n } from '../../utils/i18n'

const { t, lang } = useI18n()
const logs = ref([])
const page = ref(1)
const total = ref(0)
const filters = reactive({ userId: '', action: '' })

onMounted(() => loadData())

async function loadData() {
  const params = { page: page.value, size: 20 }
  if (filters.userId) params.userId = filters.userId
  if (filters.action) params.action = filters.action
  const res = await getLogList(params)
  if (res.code === 200) { logs.value = res.data.records || []; total.value = res.data.total || 0 }
}
</script>
