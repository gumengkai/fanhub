import React, { useEffect, useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Typography,
  message,
  Tabs,
  Badge,
} from 'antd'
import {
  HeartOutlined,
  VideoCameraOutlined,
  PictureOutlined,
} from '@ant-design/icons'
import { favoritesApi, videosApi, imagesApi } from '@services/api'
import MediaGrid from '@components/MediaGrid'
import Slideshow from '@components/Slideshow'

const { Title } = Typography

function Favorites() {
  const navigate = useNavigate()
  const [favorites, setFavorites] = useState([])
  const [loading, setLoading] = useState(false)
  const [stats, setStats] = useState({ video: 0, image: 0 })
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 24,
    total: 0,
  })
  const [mediaType, setMediaType] = useState('video')
  const [slideshowVisible, setSlideshowVisible] = useState(false)
  const [slideshowImages, setSlideshowImages] = useState([])
  const [slideshowStartIndex, setSlideshowStartIndex] = useState(0)

  const fetchFavorites = useCallback(async () => {
    setLoading(true)
    try {
      const response = await favoritesApi.getList({
        page: pagination.current,
        per_page: pagination.pageSize,
        type: mediaType,
      })

      setFavorites(response.items || [])
      setPagination({
        ...pagination,
        total: response.total || 0,
      })
    } catch (error) {
      message.error('获取收藏列表失败')
      console.error(error)
    } finally {
      setLoading(false)
    }
  }, [pagination.current, pagination.pageSize, mediaType])

  // 获取视频和图片各自的收藏数量
  const fetchStats = useCallback(async () => {
    try {
      const [videoRes, imageRes] = await Promise.all([
        favoritesApi.getList({ type: 'video', per_page: 1 }),
        favoritesApi.getList({ type: 'image', per_page: 1 }),
      ])
      setStats({
        video: videoRes.total || 0,
        image: imageRes.total || 0,
      })
    } catch (error) {
      console.error('Failed to fetch stats:', error)
    }
  }, [])

  useEffect(() => {
    fetchFavorites()
  }, [fetchFavorites])

  useEffect(() => {
    fetchStats()
  }, [fetchStats])

  const handlePageChange = (page, pageSize) => {
    setPagination({
      ...pagination,
      current: page,
      pageSize: pageSize,
    })
  }

  const handleTabChange = (key) => {
    setMediaType(key)
    setPagination({ ...pagination, current: 1 })
  }

  const handleUnfavorite = async (item) => {
    try {
      const api = item.media_type === 'video' ? videosApi : imagesApi
      await api.toggleFavorite(item.id)
      message.success('已取消收藏')
      // 直接从列表中移除该条目，不刷新整个列表
      setFavorites(favorites.filter(f => f.id !== item.id))
      setPagination({ ...pagination, total: pagination.total - 1 })
      // 更新统计
      setStats(prev => ({
        ...prev,
        [item.media_type]: Math.max(0, prev[item.media_type] - 1)
      }))
    } catch (error) {
      message.error('操作失败')
    }
  }

  // 视频点击播放
  const handleVideoClick = (video) => {
    navigate(`/videos/${video.id}`)
  }

  // 图片点击预览
  const handleImageClick = (image) => {
    setSlideshowImages(favorites)
    setSlideshowStartIndex(favorites.findIndex(img => img.id === image.id))
    setSlideshowVisible(true)
  }

  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <Title level={2} style={{ marginBottom: 16 }}>
          <HeartOutlined style={{ color: '#ff4d4f', marginRight: 12 }} />
          我的收藏
        </Title>
      </div>

      <Tabs
        defaultActiveKey="video"
        activeKey={mediaType}
        onChange={handleTabChange}
        items={[
          {
            key: 'video',
            label: (
              <span>
                <VideoCameraOutlined />
                视频
                <Badge count={stats.video} style={{ marginLeft: 8 }} />
              </span>
            ),
            children: (
              <MediaGrid
                items={favorites}
                type="video"
                loading={loading}
                pagination={{
                  current: pagination.current,
                  pageSize: pagination.pageSize,
                  total: pagination.total,
                  onChange: handlePageChange,
                }}
                onFavorite={handleUnfavorite}
                onItemClick={handleVideoClick}
              />
            ),
          },
          {
            key: 'image',
            label: (
              <span>
                <PictureOutlined />
                图片
                <Badge count={stats.image} style={{ marginLeft: 8 }} />
              </span>
            ),
            children: (
              <MediaGrid
                items={favorites}
                type="image"
                loading={loading}
                pagination={{
                  current: pagination.current,
                  pageSize: pagination.pageSize,
                  total: pagination.total,
                  onChange: handlePageChange,
                }}
                onFavorite={handleUnfavorite}
                onItemClick={handleImageClick}
              />
            ),
          },
        ]}
      />

      <Slideshow
        images={slideshowImages}
        visible={slideshowVisible}
        onClose={() => setSlideshowVisible(false)}
        initialIndex={slideshowStartIndex}
      />
    </div>
  )
}

export default Favorites
