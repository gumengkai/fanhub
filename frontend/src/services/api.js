import axios from 'axios'

// Create axios instance
const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor
api.interceptors.request.use(
  (config) => {
    // Add any auth headers here if needed
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor
api.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

// Videos API
export const videosApi = {
  getList: (params = {}) => api.get('/videos', { params }),
  getById: (id) => api.get(`/videos/${id}`),
  update: (id, data) => api.put(`/videos/${id}`, data),
  delete: (id) => api.delete(`/videos/${id}`),
  toggleFavorite: (id) => api.post(`/videos/${id}/favorite`),
  regenerateThumbnail: (id) => api.post(`/videos/${id}/thumbnail`),
  getStreamUrl: (id) => `/api/videos/${id}/stream`,
  // Tags
  getTags: (id) => api.get(`/videos/${id}/tags`),
  addTag: (id, tagId) => api.post(`/videos/${id}/tags`, { tag_id: tagId }),
  removeTag: (id, tagId) => api.delete(`/videos/${id}/tags/${tagId}`),
  // Related videos
  getRelated: (id, params = {}) => api.get(`/videos/${id}/related`, { params }),
}

// Images API
export const imagesApi = {
  getList: (params = {}) => api.get('/images', { params }),
  getById: (id) => api.get(`/images/${id}`),
  delete: (id) => api.delete(`/images/${id}`),
  toggleFavorite: (id) => api.post(`/images/${id}/favorite`),
  getFileUrl: (id) => `/api/images/${id}/file`,
  getThumbnailUrl: (id) => `/api/images/${id}/thumbnail`,
}

// Sources API
export const sourcesApi = {
  getList: () => api.get('/sources'),
  create: (data) => api.post('/sources', data),
  update: (id, data) => api.put(`/sources/${id}`, data),
  delete: (id) => api.delete(`/sources/${id}`),
  scan: (id) => api.post(`/sources/${id}/scan`),
  checkStatus: (id) => api.get(`/sources/${id}/status`),
  getStats: (id) => api.get(`/sources/${id}/stats`),
}

// Favorites API
export const favoritesApi = {
  getList: (params = {}) => api.get('/favorites', { params }),
  getStats: () => api.get('/favorites/stats'),
}

// Tags API
export const tagsApi = {
  getList: () => api.get('/tags'),
  create: (data) => api.post('/tags', data),
  update: (id, data) => api.put(`/tags/${id}`, data),
  delete: (id) => api.delete(`/tags/${id}`),
}

export default api
