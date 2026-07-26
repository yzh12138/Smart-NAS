<template>
  <div class="profile-page" :key="lang">
    <el-card>
      <template #header>{{ t('profile.title') }}</template>
      <div style="display:flex;gap:40px">
        <!-- 头像 -->
        <div style="text-align:center">
          <el-avatar :size="120" :src="avatarUrl" icon="UserFilled" />
          <div style="margin-top:12px">
            <el-upload :auto-upload="false" :show-file-list="false" accept="image/*" :on-change="handleAvatarChange">
              <el-button size="small">{{ t('profile.changeAvatar') }}</el-button>
            </el-upload>
          </div>
        </div>
        <!-- 信息 -->
        <div style="flex:1">
          <el-form label-width="100px">
            <el-form-item :label="t('login.username')">
              <el-input v-model="form.username" disabled />
            </el-form-item>
            <el-form-item :label="t('login.nickname')">
              <el-input v-model="form.nickname" />
            </el-form-item>
            <el-form-item :label="t('profile.password')">
              <el-input v-model="form.password" type="password" show-password :placeholder="t('profile.passwordHint')" />
            </el-form-item>
            <el-form-item :label="t('profile.role')">
              <el-tag v-for="role in roles" :key="role.id" style="margin-right:8px">{{ role.roleName }}</el-tag>
              <span v-if="roles.length === 0" style="color:#999">-</span>
            </el-form-item>
            <el-form-item :label="t('profile.familyRole')">
              <el-select v-model="form.familyRole" :placeholder="t('profile.familyRolePlaceholder')" clearable>
                <el-option :label="t('profile.familyRoleDad')" value="爸爸" />
                <el-option :label="t('profile.familyRoleMom')" value="妈妈" />
                <el-option :label="t('profile.familyRoleBrother')" value="哥哥" />
                <el-option :label="t('profile.familyRoleSister')" value="姐姐" />
                <el-option :label="t('profile.familyRoleYoungerBrother')" value="弟弟" />
                <el-option :label="t('profile.familyRoleYoungerSister')" value="妹妹" />
                <el-option :label="t('profile.familyRoleGrandpa')" value="爷爷" />
                <el-option :label="t('profile.familyRoleGrandma')" value="奶奶" />
                <el-option :label="t('profile.familyRoleMaternalGrandpa')" value="外公" />
                <el-option :label="t('profile.familyRoleMaternalGrandma')" value="外婆" />
                <el-option :label="t('profile.familyRoleOther')" value="其他" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSave">{{ t('common.confirm') }}</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getUserInfo, updateUser } from '../../api'
import { useI18n } from '../../utils/i18n'
import { useUserStore } from '../../stores/user'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'

const { t, lang } = useI18n()
const userStore = useUserStore()
const form = ref({ username: '', nickname: '', password: '', familyRole: '' })
const roles = ref([])
const avatarFile = ref(null)
const avatarPreview = ref('')

const avatarUrl = computed(() => {
  if (avatarPreview.value) return avatarPreview.value
  const user = userStore.userInfo
  if (user && user.avatar) return user.avatar
  return ''
})

onMounted(async () => {
  // 先刷新store中的用户信息
  await userStore.getUserInfo()
  const res = await getUserInfo()
  if (res.code === 200) {
    form.value.username = res.data.username || ''
    form.value.nickname = res.data.nickname || ''
    form.value.familyRole = res.data.familyRole || ''
    roles.value = res.data.roles || []
  }
})

function handleAvatarChange(file) {
  avatarFile.value = file.raw
  avatarPreview.value = URL.createObjectURL(file.raw)
}

async function handleSave() {
  const data = { nickname: form.value.nickname, familyRole: form.value.familyRole || null }
  if (form.value.password) data.password = form.value.password

  // 上传头像
  if (avatarFile.value) {
    const formData = new FormData()
    formData.append('file', avatarFile.value)
    try {
      const uploadRes = await request.post('/api/file/upload-avatar', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
      if (uploadRes.code === 200) data.avatar = uploadRes.data
    } catch { ElMessage.error(t('profile.avatarUploadFailed')); return }
  }

  const userId = userStore.userInfo?.userId
  if (userId) {
    await updateUser(userId, data)
    ElMessage.success(t('common.success'))
    // 清除预览并刷新用户信息
    avatarFile.value = null
    avatarPreview.value = ''
    await userStore.getUserInfo()
  }
}
</script>

<style scoped>
.profile-page { max-width: 700px; margin: 0 auto; }
</style>
