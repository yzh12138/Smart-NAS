import request from '../utils/request'

// Auth
export const login = (data) => request.post('/api/auth/login', data)
export const getUserInfo = () => request.get('/api/auth/info')
export const logout = () => request.post('/api/auth/logout')

// User
export const getUserList = (params) => request.get('/api/system/user/list', { params })
export const createUser = (data) => request.post('/api/system/user', data)
export const updateUser = (id, data) => request.put(`/api/system/user/${id}`, data)
export const deleteUser = (id) => request.delete(`/api/system/user/${id}`)
export const getUserAiPrompt = () => request.get('/api/system/user/ai-prompt')
export const updateUserAiPrompt = (data) => request.put('/api/system/user/ai-prompt', data)

// Role
export const getRoleList = () => request.get('/api/system/role/list')
export const createRole = (data) => request.post('/api/system/role', data)
export const updateRole = (id, data) => request.put(`/api/system/role/${id}`, data)
export const deleteRole = (id) => request.delete(`/api/system/role/${id}`)

// Permission
export const getPermissionTree = () => request.get('/api/system/permission/tree')
export const getPermissionList = () => request.get('/api/system/permission/list')
export const createPermission = (data) => request.post('/api/system/permission', data)
export const updatePermission = (id, data) => request.put(`/api/system/permission/${id}`, data)
export const deletePermission = (id) => request.delete(`/api/system/permission/${id}`)

// Photo
export const uploadPhotos = (formData) => request.post('/api/photo/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' }, timeout: 300000 })
export const getPhotoList = (params) => request.get('/api/photo/list', { params })
export const getPhotoDetail = (id) => request.get(`/api/photo/${id}`)
export const deletePhoto = (id) => request.delete(`/api/photo/${id}`)
export const updatePhotoName = (id, name) => request.put(`/api/photo/${id}/name`, { name })
export const getCityPhotoStats = () => request.get('/api/photo/map/cities')
export const getPhotosByCity = (city) => request.get(`/api/photo/map/city/${encodeURIComponent(city)}`)
export const searchPhotos = (keyword) => request.get('/api/photo/search', { params: { keyword } })
export const getSharedPhotos = () => request.get('/api/photo/shared')
export const batchAiScan = (photoIds) => request.post('/api/photo/batch-ai-scan', photoIds ? { photoIds } : {}, { timeout: 600000 })
export const scanSinglePhoto = (id) => request.post(`/api/photo/scan-single/${id}`, null, { timeout: 300000 })
export const reverseGeocode = (lat, lng) => request.get('/api/photo/reverse-geocode', { params: { lat, lng } })
export const getAiReviewQueue = () => request.get('/api/photo/ai-review')
export const reviewAiTags = (id, data) => request.post(`/api/photo/${id}/ai-review`, data)
export const getAiSuggestedTags = (id) => request.post(`/api/photo/${id}/ai-tags`)
export const confirmAiTags = (id, data) => request.post(`/api/photo/${id}/confirm-tags`, data)
export const trackPhotoClick = (id) => request.post(`/api/photo/${id}/click`)
export const getRecommendedPhotos = () => request.get('/api/photo/recommended')

// Photo Comments
export const getPhotoComments = (photoId) => request.get(`/api/photo/${photoId}/comment`)
export const addPhotoComment = (photoId, data) => request.post(`/api/photo/${photoId}/comment`, data)
export const deletePhotoComment = (photoId, commentId) => request.delete(`/api/photo/${photoId}/comment/${commentId}`)

// Tag
export const getTagList = () => request.get('/api/tag/list')
export const createTag = (data) => request.post('/api/tag', data)
export const updateTag = (id, data) => request.put(`/api/tag/${id}`, data)
export const deleteTag = (id) => request.delete(`/api/tag/${id}`)

// Recycle
export const getRecycleList = (params) => request.get('/api/recycle/list', { params })
export const restorePhoto = (id) => request.post(`/api/recycle/restore/${id}`)
export const permanentDelete = (id) => request.delete(`/api/recycle/permanent/${id}`)
export const emptyRecycle = () => request.delete('/api/recycle/empty')

// Family
export const createFamily = (data) => request.post('/api/family', data)
export const updateFamily = (id, data) => request.put(`/api/family/${id}`, data)
export const dissolveFamily = (id) => request.delete(`/api/family/${id}`)
export const getMyFamilies = () => request.get('/api/family/my')
export const getOwnedFamilies = () => request.get('/api/family/owned')
export const joinFamily = (id) => request.post(`/api/family/${id}/join`)
export const getFamilyMembers = (id) => request.get(`/api/family/${id}/members`)
export const getFamilyPending = (id) => request.get(`/api/family/${id}/pending`)
export const approveMember = (memberId) => request.post(`/api/family/member/${memberId}/approve`)
export const rejectMember = (memberId) => request.post(`/api/family/member/${memberId}/reject`)
export const removeMember = (familyId, userId) => request.delete(`/api/family/${familyId}/member/${userId}`)
export const shareToFamily = (familyId, photoId) => request.post(`/api/family/${familyId}/share/${photoId}`)
export const batchShareToFamily = (familyId, photoIds) => request.post(`/api/family/${familyId}/batch-share`, { photoIds })
export const unshareFromFamily = (familyId, photoId) => request.delete(`/api/family/${familyId}/unshare/${photoId}`)
export const batchUnshareFromFamily = (photoIds) => request.post('/api/family/batch-unshare', { photoIds })
export const getFamilyMedia = (id) => request.get(`/api/family/${id}/media`)
export const searchFamilyByCode = (code) => request.get(`/api/family/search/${code}`)
export const inviteFamilyMember = (familyId, userId) => request.post(`/api/family/${familyId}/invite`, { userId })

// AI Model
export const getAiModelList = () => request.get('/api/ai-model/list')
export const getDefaultAiModel = () => request.get('/api/ai-model/default')
export const createAiModel = (data) => request.post('/api/ai-model', data)
export const updateAiModel = (id, data) => request.put(`/api/ai-model/${id}`, data)
export const deleteAiModel = (id) => request.delete(`/api/ai-model/${id}`)
export const setDefaultAiModel = (id) => request.post(`/api/ai-model/${id}/default`)
export const getGlobalPrompt = () => request.get('/api/ai-model/global-prompt')
export const updateGlobalPrompt = (data) => request.put('/api/ai-model/global-prompt', data)

// Log
export const getLogList = (params) => request.get('/api/log/list', { params })

// Duplicate
export const getDuplicateList = () => request.get('/api/duplicate/list')
export const cleanDuplicates = (data) => request.post('/api/duplicate/clean', data)

// File Storage
export const getFileList = (params) => request.get('/api/file/list', { params })
export const uploadFile = (formData) => request.post('/api/file/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' }, timeout: 120000 })
export const deleteFile = (id) => request.delete(`/api/file/${id}`)

// Book
export const getBookList = (params) => request.get('/api/book/list', { params })
export const getBookDetail = (id) => request.get(`/api/book/${id}`)
export const uploadBook = (formData) => request.post('/api/book/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' }, timeout: 120000 })
export const updateBook = (id, data) => request.put(`/api/book/${id}`, data)
export const deleteBook = (id) => request.delete(`/api/book/${id}`)

// AI Chat
export const getConversations = () => request.get('/api/ai-chat/conversations')
export const createConversation = (data) => request.post('/api/ai-chat/conversation', data)
export const getConversationMessages = (id) => request.get(`/api/ai-chat/conversation/${id}/messages`)
export const sendChatMessage = (id, data) => request.post(`/api/ai-chat/conversation/${id}/send`, data, { timeout: 180000 })
export const deleteConversation = (id) => request.delete(`/api/ai-chat/conversation/${id}`)
export const uploadChatImage = (formData) => request.post('/api/ai-chat/upload-image', formData, { headers: { 'Content-Type': 'multipart/form-data' }, timeout: 30000 })

// AI User Prompts
export const getPromptList = () => request.get('/api/ai-prompt/list')
export const createPrompt = (data) => request.post('/api/ai-prompt', data)
export const updatePrompt = (id, data) => request.put(`/api/ai-prompt/${id}`, data)
export const deletePrompt = (id) => request.delete(`/api/ai-prompt/${id}`)
export const setDefaultPrompt = (id) => request.post(`/api/ai-prompt/${id}/default`)
export const getDefaultPrompt = () => request.get('/api/ai-prompt/default')

// Recycle
export const updateRecycleDays = (days) => request.put('/api/recycle/days', { days })

// Friends
export const sendFriendRequest = (friendId) => request.post(`/api/friend/send/${friendId}`)
export const acceptFriendRequest = (id) => request.post(`/api/friend/accept/${id}`)
export const rejectFriendRequest = (id) => request.post(`/api/friend/reject/${id}`)
export const removeFriend = (friendId) => request.delete(`/api/friend/${friendId}`)
export const getFriendList = () => request.get('/api/friend/list')
export const getPendingFriendRequests = () => request.get('/api/friend/pending')
export const searchUsers = (keyword) => request.get('/api/friend/search', { params: { keyword } })

// City
export const getCityList = () => request.get('/api/city/list')
export const getCityDetail = (id) => request.get(`/api/city/${id}`)
export const createCity = (data) => request.post('/api/city', data)
export const updateCity = (id, data) => request.put(`/api/city/${id}`, data)
export const deleteCity = (id) => request.delete(`/api/city/${id}`)

// Face Recognition
export const getFaceClusters = () => request.get('/api/face/clusters')
export const getFaceClusterPhotos = (clusterId) => request.get(`/api/face/cluster/${clusterId}/photos`)
export const createFaceCluster = (data) => request.post('/api/face/cluster', data)
export const renameFaceCluster = (id, data) => request.put(`/api/face/cluster/${id}`, data)
export const deleteFaceCluster = (id) => request.delete(`/api/face/cluster/${id}`)
export const removePhotoFromCluster = (clusterId, photoId) => request.delete(`/api/face/cluster/${clusterId}/photo/${photoId}`)
export const movePhotoToCluster = (toClusterId, photoId, fromClusterId) => {
  const params = fromClusterId ? { fromClusterId } : {}
  return request.post(`/api/face/cluster/${toClusterId}/photo/${photoId}`, null, { params })
}
