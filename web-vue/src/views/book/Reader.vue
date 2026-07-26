<template>
  <div class="reader-page" :key="lang">
    <el-page-header @back="router.back()" :title="t('book.back')" :content="book?.title" />
    <div class="reader-container" v-if="book">
      <iframe v-if="book.fileFormat === 'pdf'" :src="`/api/book/${book.id}/read`" class="reader-frame" />
      <div v-else class="reader-unsupported">
        <el-empty :description="t('book.unsupportedFormat')" />
        <el-button type="primary" @click="download">{{ t('file.download') }} ({{ book.fileFormat }})</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getBookDetail } from '../../api'
import { useI18n } from '../../utils/i18n'

const route = useRoute()
const router = useRouter()
const { t, lang } = useI18n()
const book = ref(null)

onMounted(async () => {
  const res = await getBookDetail(route.params.id)
  if (res.code === 200) book.value = res.data
})

function download() { window.open(`/api/book/${book.value.id}/read`) }
</script>

<style scoped>
.reader-container { margin-top: 16px; }
.reader-frame { width: 100%; height: calc(100vh - 120px); border: none; }
.reader-unsupported { text-align: center; padding: 60px 0; }
</style>
