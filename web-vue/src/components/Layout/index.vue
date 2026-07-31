<template>
  <!-- 手机端布局 -->
  <div v-if="isMobile" class="mobile-layout" :key="lang">
    <header class="mobile-header">
      <span class="mobile-title">{{ t('app.title') }}</span>
      <el-dropdown @command="handleCommand">
        <el-avatar v-if="userStore.userInfo?.avatar" :size="32" :src="userStore.userInfo.avatar" />
        <el-avatar v-else :size="32" icon="UserFilled" />
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="logout">{{ t('common.logout') }}</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </header>
    <main class="mobile-content">
      <router-view />
    </main>
    <MobileTabBar />
  </div>

  <!-- 桌面端布局 -->
  <div v-else class="desktop-layout" :key="lang">
    <aside class="sidebar" :class="{ collapsed: isCollapsed }">
      <div class="logo">
        <span v-if="!isCollapsed">🐾 {{ t('app.title') }}</span>
        <span v-else>🐕</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapsed"
        router
        background-color="#1d1e2c"
        text-color="#a0a3bd"
        active-text-color="#409eff"
      >
        <template v-for="menu in menuList" :key="menu.id">
          <el-sub-menu v-if="menu.children && menu.children.length > 0" :index="menu.path">
            <template #title>
              <el-icon><component :is="menu.icon" /></el-icon>
              <span>{{ menu.title }}</span>
            </template>
            <el-menu-item
              v-for="child in menu.children"
              :key="child.path"
              :index="child.path"
            >
              <el-icon><component :is="child.icon" /></el-icon>
              <span>{{ child.title }}</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="menu.path">
            <el-icon><component :is="menu.icon" /></el-icon>
            <span>{{ menu.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </aside>
    <div class="main-area">
      <header class="header">
        <el-icon class="collapse-btn" @click="isCollapsed = !isCollapsed">
          <Fold v-if="!isCollapsed" />
          <Expand v-else />
        </el-icon>
        <div class="header-right">
          <span class="nickname">{{ userStore.userInfo?.nickname || 'User' }}</span>
          <el-dropdown @command="handleCommand">
            <el-avatar v-if="userStore.userInfo?.avatar" :size="32" :src="userStore.userInfo.avatar" />
            <el-avatar v-else :size="32" icon="UserFilled" />
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">{{ t('common.logout') }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { useI18n } from '../../utils/i18n'
import MobileTabBar from '../MobileTabBar.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { t, lang } = useI18n()
const isCollapsed = ref(false)
const isMobile = ref(false)

const activeMenu = computed(() => route.path)
const isAdmin = computed(() => {
  const userInfo = userStore.userInfo
  if (!userInfo) return false
  // 检查用户名是否为admin，或角色中是否有admin
  if (userInfo.username === 'admin') return true
  const roles = userInfo.roles || []
  return roles.some(r => r.roleKey === 'admin')
})

const menuList = computed(() => {
  return [
    { id: 2, title: t('menu.dashboard'), icon: 'DataAnalysis', path: '/dashboard', children: [] },
    {
      id: 6, title: t('menu.photo'), icon: 'Picture', path: '/photo',
      children: [
        { id: 7, title: t('menu.upload'), icon: 'Upload', path: '/photo/upload' },
        { id: 8, title: t('menu.memory'), icon: 'Location', path: '/photo/memory' },
        { id: 23, title: t('menu.photoOverview'), icon: 'Grid', path: '/photo/overview' },
        { id: 24, title: t('menu.photoReview'), icon: 'Stamp', path: '/photo/review' }
      ]
    },
    { id: 10, title: t('menu.video'), icon: 'VideoCamera', path: '/video', children: [] },
    {
      id: 11, title: t('menu.family'), icon: 'UserFilled', path: '/family',
      children: [
        { id: 12, title: t('menu.familyHome'), icon: 'HomeFilled', path: '/family' },
        { id: 13, title: t('menu.familyManage'), icon: 'Setting', path: '/family/manage' },
        { id: 25, title: t('menu.friend'), icon: 'User', path: '/friend' }
      ]
    },
    { id: 20, title: t('menu.fileStorage'), icon: 'Folder', path: '/file', children: [] },
    { id: 21, title: t('menu.book'), icon: 'Reading', path: '/book', children: [] },
    { id: 22, title: t('menu.aiChat'), icon: 'ChatDotRound', path: '/ai-chat', children: [] },
    { id: 30, title: t('menu.face'), icon: 'UserFilled', path: '/face', children: [] },
    { id: 14, title: t('menu.recycle'), icon: 'Delete', path: '/recycle', children: [] },
    {
      id: 1, title: t('menu.system'), icon: 'Monitor', path: '/system',
      children: [
        { id: 29, title: t('menu.profile'), icon: 'User', path: '/profile' },
        ...(isAdmin.value ? [
          { id: 3, title: t('menu.users'), icon: 'UserFilled', path: '/system/users' },
          { id: 4, title: t('menu.roles'), icon: 'UserFilled', path: '/system/roles' },
          { id: 5, title: t('menu.permissions'), icon: 'Lock', path: '/system/permissions' },
          { id: 16, title: t('menu.logs'), icon: 'Document', path: '/system/logs' }
        ] : []),
        { id: 15, title: t('menu.settings'), icon: 'Setting', path: '/system/settings' },
        { id: 26, title: t('menu.tagManage'), icon: 'PriceTag', path: '/tag' },
        { id: 27, title: t('menu.cityManage'), icon: 'MapLocation', path: '/city' }
      ]
    }
  ]
})

function checkMobile() {
  isMobile.value = window.innerWidth < 768
}

onMounted(async () => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
  if (!userStore.userInfo) {
    try { await userStore.getUserInfo() } catch (e) { /* ignore */ }
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})

function handleCommand(cmd) {
  if (cmd === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
/* 桌面端 */
.desktop-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}
.sidebar {
  width: 220px;
  background: #1d1e2c;
  color: #fff;
  transition: width 0.3s;
  overflow-y: auto;
  flex-shrink: 0;
}
.sidebar.collapsed {
  width: 64px;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
  border-bottom: 1px solid #2d2e3e;
}
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  border-bottom: 1px solid #e8e8e8;
  background: #fff;
}
.collapse-btn {
  font-size: 20px;
  cursor: pointer;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.nickname {
  font-size: 14px;
  color: #666;
}
.content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background: #f5f7fa;
}

/* 手机端 */
.mobile-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}
.mobile-header {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background: #1d1e2c;
  color: #fff;
}
.mobile-title {
  font-size: 18px;
  font-weight: bold;
  color: #409eff;
}
.mobile-content {
  flex: 1;
  padding: 12px;
  overflow-y: auto;
  background: #f5f7fa;
  padding-bottom: 68px;
}
</style>
