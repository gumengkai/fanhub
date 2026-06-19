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
  toggleLike: (id) => api.post(`/videos/${id}/like`),
  regenerateThumbnail: (id) => api.post(`/videos/${id}/thumbnail`),
  fixThumbnails: () => api.post('/videos/thumbnails/fix'),
  getStreamUrl: (id) => `/api/videos/${id}/stream`,
  getRelated: (id, params = {}) => api.get(`/videos/${id}/related`, { params }),
}

export const imagesApi = {
  getList: (params = {}) => api.get('/images', { params }),
  getById: (id) => api.get(`/images/${id}`),
  delete: (id) => api.delete(`/images/${id}`),
  batchDelete: (imageIds) => api.delete('/images/batch', { data: { image_ids: imageIds } }),
  toggleFavorite: (id) => api.post(`/images/${id}/favorite`),
  toggleLike: (id) => api.post(`/images/${id}/like`),
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
  getScanLogs: (params = {}) => api.get('/sources/scan-logs', { params }),
  getScanLog: (id) => api.get(`/sources/scan-logs/${id}`),
  deleteScanLog: (id) => api.delete(`/sources/scan-logs/${id}`),
  clearScanLogs: () => api.post('/sources/scan-logs/clear'),
}

export const favoritesApi = {
  getList: (params = {}) => api.get('/favorites', { params }),
  getStats: () => api.get('/favorites/stats'),
}

export const likesApi = {
  getList: (params = {}) => api.get('/likes', { params }),
  getStats: () => api.get('/likes/stats'),
}

export const historyApi = {
  getList: (params = {}) => api.get('/history', { params }),
  getVideoHistory: (videoId) => api.get(`/history/video/${videoId}`),
  updateVideoHistory: (videoId, data) => api.post(`/history/video/${videoId}`, data),
  clearHistory: () => api.post('/history/clear'),
  getStats: () => api.get('/history/stats'),
}

export const douyinApi = {
  getList: (params = {}) => api.get('/douyin', { params }),
  getById: (id) => api.get(`/douyin/${id}`),
  toggleLike: (id) => api.post(`/douyin/${id}/like`),
  toggleFavorite: (id) => api.post(`/douyin/${id}/favorite`),
  delete: (id) => api.delete(`/douyin/${id}`),
  getStats: () => api.get('/douyin/stats'),
  getStreamUrl: (id) => `/api/douyin/${id}/stream`,
  getThumbnailUrl: (id) => `/api/douyin/${id}/thumbnail`,
  updateHistory: (id, data) => api.post(`/douyin/${id}/history`, data),
}

export const peakApi = {
  getList: (params = {}) => api.get('/peak', { params }),
  getById: (id) => api.get(`/peak/${id}`),
  toggleLike: (id) => api.post(`/peak/${id}/like`),
  toggleFavorite: (id) => api.post(`/peak/${id}/favorite`),
  delete: (id) => api.delete(`/peak/${id}`),
  getStats: () => api.get('/peak/stats'),
  getStreamUrl: (id) => `/api/peak/${id}/stream`,
  getThumbnailUrl: (id) => `/api/peak/${id}/thumbnail`,
  updateHistory: (id, data) => api.post(`/peak/${id}/history`, data),
}

export const wordcloudApi = {
  getList: (params = {}) => api.get('/wordcloud', { params }),
}

export default api
