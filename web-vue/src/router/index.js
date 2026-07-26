import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/login/index.vue') },
  {
    path: '/',
    component: () => import('../components/Layout/index.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/dashboard/index.vue') },
      { path: 'system/users', name: 'Users', component: () => import('../views/system/RoleManage.vue') },
      { path: 'system/roles', name: 'Roles', component: () => import('../views/system/RoleManage.vue') },
      { path: 'system/permissions', name: 'Permissions', component: () => import('../views/system/Permissions.vue') },
      { path: 'system/settings', name: 'Settings', component: () => import('../views/settings/index.vue') },
      { path: 'system/logs', name: 'Logs', component: () => import('../views/system/Logs.vue') },
      { path: 'photo/upload', name: 'PhotoUpload', component: () => import('../views/photo/Upload.vue') },
      { path: 'photo/memory', name: 'PhotoMemory', component: () => import('../views/photo/Memory.vue') },
      { path: 'photo/overview', name: 'PhotoOverview', component: () => import('../views/photo/Overview.vue') },
      { path: 'photo/review', name: 'PhotoReview', component: () => import('../views/photo/Review.vue') },
      { path: 'video', name: 'Video', component: () => import('../views/video/index.vue') },
      { path: 'family', name: 'Family', component: () => import('../views/family/index.vue') },
      { path: 'family/manage', name: 'FamilyManage', component: () => import('../views/family/Manage.vue') },
      { path: 'friend', name: 'Friend', component: () => import('../views/friend/index.vue') },
      { path: 'tag', name: 'TagManage', component: () => import('../views/tag/index.vue') },
      { path: 'city', name: 'CityManage', component: () => import('../views/city/index.vue') },
      { path: 'task', name: 'Task', component: () => import('../views/task/index.vue') },
      { path: 'recycle', name: 'Recycle', component: () => import('../views/recycle/index.vue') },
      { path: 'file', name: 'FileStorage', component: () => import('../views/file/index.vue') },
      { path: 'book', name: 'Book', component: () => import('../views/book/index.vue') },
      { path: 'book/reader/:id', name: 'BookReader', component: () => import('../views/book/Reader.vue') },
      { path: 'ai-chat', name: 'AiChat', component: () => import('../views/ai-chat/index.vue') },
      { path: 'ai-chat/mobile', name: 'AiChatMobile', component: () => import('../views/ai-chat/mobile.vue') },
      { path: 'face', name: 'Face', component: () => import('../views/face/index.vue') },
      { path: 'profile', name: 'Profile', component: () => import('../views/profile/index.vue') }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) next('/login')
  else next()
})
export default router
