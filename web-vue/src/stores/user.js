import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '../utils/request'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(null)
  const permissions = ref([])
  const menuList = ref([])

  async function login(username, password) {
    let lastError
    for (let i = 0; i < 3; i++) {
      try {
        const res = await request.post('/api/auth/login', { username, password })
        if (res.code === 200) {
          token.value = res.data.token
          localStorage.setItem('token', res.data.token)
          return true
        }
        throw new Error(res.message)
      } catch (e) {
        lastError = e
        if (i < 2) await new Promise(r => setTimeout(r, 2000))
      }
    }
    throw lastError
  }

  async function getUserInfo() {
    const res = await request.get('/api/auth/info')
    if (res.code === 200) {
      userInfo.value = res.data
      permissions.value = res.data.permissions || []
      buildMenu(res.data.permissions || [])
    }
  }

  function buildMenu(permTree) {
    const menus = []
    for (const perm of permTree) {
      if (perm.permType === 1 || perm.permType === 2) {
        const menu = {
          id: perm.id,
          title: perm.permName,
          icon: perm.icon,
          path: perm.path,
          children: []
        }
        if (perm.children && perm.children.length > 0) {
          menu.children = perm.children
            .filter(c => c.permType === 2)
            .map(c => ({
              id: c.id,
              title: c.permName,
              icon: c.icon,
              path: c.path
            }))
        }
        menus.push(menu)
      }
    }
    menuList.value = menus
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    permissions.value = []
    menuList.value = []
    localStorage.removeItem('token')
  }

  return { token, userInfo, permissions, menuList, login, getUserInfo, logout }
})
