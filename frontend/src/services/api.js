import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use(
  (config) => config,
  (error) => Promise.reject(error)
)

api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

export const videosApi = {
  getList: (params = {}) => api.get('/videos', { params }),
  getById: (id) => api.get(`/videos/${id}`),
  update: (id, data) => api.put(`/videos/${id}`, data),
  delete: (id) => api.delete(`/videos/${id}`),
  toggleFavorite: (id) => api.post(`/videos/${id}/favorite`),
  regenerateThumbnail: (id) => api.post(`/videos/${id}/thumbnail`),
  fixThumbnails: () => api.post('/videos/thumbnails/fix'),
  getStreamUrl: (id) => `/api/videos/${id}/stream`,
  getTags: (id) => api.get(`/videos/${id}/tags`),
  addTag: (id, tagId) => api.post(`/videos/${id}/tags`, { tag_id: tagId }),
  removeTag: (id, tagId) => api.delete(`/videos/${id}/tags/${tagId}`),
  getRelated: (id, params = {}) => api.get(`/videos/${id}/related`, { params }),
}

export const imagesApi = {
  getList: (params = {}) => api.get('/images', { params }),
  getById: (id) => api.get(`/images/${id}`),
  delete: (id) => api.delete(`/images/${id}`),
  batchDelete: (imageIds) => api.delete('/images/batch', { data: { image_ids: imageIds } }),
  toggleFavorite: (id) => api.post(`/images/${id}/favorite`),
  getFileUrl: (id) => `/api/images/${id}/file`,
  getThumbnailUrl: (id) => `/api/images/${id}/thumbnail`,
  getAll: (params = {}) => api.get('/images/all', { params }),
}

export const sourcesApi = {
  getList: () => api.get('/sources'),
  create: (data) => api.post('/sources', data),
  update: (id, data) => api.put(`/sources/${id}`, data),
  delete: (id) => api.delete(`/sources/${id}`),
  scan: (id) => api.post(`/sources/${id}/scan`),
  checkStatus: (id) => api.get(`/sources/${id}/status`),
  getStats: (id) => api.get(`/sources/${id}/stats`),
}

export const favoritesApi = {
  getList: (params = {}) => api.get('/favorites', { params }),
  getStats: () => api.get('/favorites/stats'),
}

export const tagsApi = {
  getList: () => api.get('/tags'),
  create: (data) => api.post('/tags', data),
  update: (id, data) => api.put(`/tags/${id}`, data),
  delete: (id) => api.delete(`/tags/${id}`),
}

export const historyApi = {
  getList: (params = {}) => api.get('/history', { params }),
  getVideoHistory: (videoId) => api.get(`/history/video/${videoId}`),
  updateVideoHistory: (videoId, data) => api.post(`/history/video/${videoId}`, data),
  clearHistory: () => api.post('/history/clear'),
  getStats: () => api.get('/history/stats'),
}

export default api
