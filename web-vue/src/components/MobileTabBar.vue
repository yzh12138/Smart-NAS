<template>
  <div class="mobile-tabbar">
    <div v-for="tab in tabs" :key="tab.path" class="tabbar-item" :class="{ active: currentPath === tab.path }" @click="navigate(tab.path)">
      <el-icon :size="22"><component :is="tab.icon" /></el-icon>
      <span>{{ tab.title }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from '../utils/i18n'

const route = useRoute()
const router = useRouter()
const { t, lang } = useI18n()
const currentPath = computed(() => route.path)

const tabs = computed(() => [
  { path: '/dashboard', title: t('menu.dashboard'), icon: 'HomeFilled' },
  { path: '/photo/upload', title: t('menu.upload'), icon: 'Upload' },
  { path: '/photo/memory', title: t('menu.memory'), icon: 'Location' },
  { path: '/system/settings', title: t('menu.settings'), icon: 'Setting' }
])

function navigate(path) { router.push(path) }
</script>

<style scoped>
.mobile-tabbar { position: fixed; bottom: 0; left: 0; right: 0; height: 56px; background: #fff; border-top: 1px solid #e8e8e8; display: flex; align-items: center; justify-content: space-around; z-index: 1000; padding-bottom: env(safe-area-inset-bottom); }
.tabbar-item { display: flex; flex-direction: column; align-items: center; gap: 2px; font-size: 11px; color: #999; cursor: pointer; padding: 4px 12px; transition: color 0.2s; }
.tabbar-item.active { color: #409eff; }
.tabbar-item span { line-height: 1; }
</style>
