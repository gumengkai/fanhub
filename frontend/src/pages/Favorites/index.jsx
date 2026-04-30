import React, { useEffect, useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Typography,
  message,
  Tabs,
  Badge,
  Space,
} from 'antd'
import {
  HeartFilled,
  StarFilled,
  VideoCameraOutlined,
  PictureOutlined,
  PlayCircleOutlined,
} from '@ant-design/icons'
import { favoritesApi, likesApi, videosApi, imagesApi, douyinApi } from '@services/api'
import MediaGrid from '@components/MediaGrid'
import Slideshow from '@components/Slideshow'

const { Title, Text } = Typography

const DouyinRed = '#FE2C55'
const DouyinGold = '#FFD700'

function Favorites() {
  const navigate = useNavigate()
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(false)
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 24,
    total: 0,
  })

  // 统计数据
  const [stats, setStats] = useState({
    videoLikes: 0,
    videoFavorites: 0,
    imageLikes: 0,
    imageFavorites: 0,
    douyinLikes: 0,
    douyinFavorites: 0,
  })

  // 当前Tab: likes | favorites
  const [mainTab, setMainTab] = useState('likes')
  // 当前媒体类型: video | image | douyin
  const [mediaType, setMediaType] = useState('video')

  const [slideshowVisible, setSlideshowVisible] = useState(false)
  const [slideshowImages, setSlideshowImages] = useState([])
  const [slideshowStartIndex, setSlideshowStartIndex] = useState(0)

  const fetchItems = useCallback(async () => {
    setLoading(true)
    try {
      let response
      if (mediaType === 'douyin') {
        // 抖音库
        const params = {
          page: pagination.current,
          per_page: pagination.pageSize,
        }
        if (mainTab === 'likes') params.liked = true
        else params.favorite = true
        response = await douyinApi.getList(params)
      } else if (mainTab === 'likes') {
        // 喜欢
        response = await likesApi.getList({
          page: pagination.current,
          per_page: pagination.pageSize,
          type: mediaType,
        })
      } else {
        // 收藏
        response = await favoritesApi.getList({
          page: pagination.current,
          per_page: pagination.pageSize,
          type: mediaType,
        })
      }

      setItems(response.items || [])
      setPagination(prev => ({
        ...prev,
        total: response.total || 0,
      }))
    } catch (error) {
      message.error('获取列表失败')
      console.error(error)
    } finally {
      setLoading(false)
    }
  }, [pagination.current, pagination.pageSize, mediaType, mainTab])

  // 获取统计数据
  const fetchStats = useCallback(async () => {
    try {
      // 视频库统计
      const [videoLikesRes, videoFavsRes, imageLikesRes, imageFavsRes] = await Promise.all([
        likesApi.getList({ type: 'video', per_page: 1 }),
        favoritesApi.getList({ type: 'video', per_page: 1 }),
        likesApi.getList({ type: 'image', per_page: 1 }),
        favoritesApi.getList({ type: 'image', per_page: 1 }),
      ])

      // 抖音库统计
      const douyinStatsRes = await douyinApi.getStats()

      setStats({
        videoLikes: videoLikesRes.total || 0,
        videoFavorites: videoFavsRes.total || 0,
        imageLikes: imageLikesRes.total || 0,
        imageFavorites: imageFavsRes.total || 0,
        douyinLikes: douyinStatsRes.liked || 0,
        douyinFavorites: douyinStatsRes.favorite || 0,
      })
    } catch (error) {
      console.error('Failed to fetch stats:', error)
    }
  }, [])

  useEffect(() => {
    fetchItems()
  }, [fetchItems])

  useEffect(() => {
    fetchStats()
  }, [])

  const handlePageChange = (page, pageSize) => {
    setPagination(prev => ({
      ...prev,
      current: page,
      pageSize: pageSize,
    }))
  }

  const handleMainTabChange = (key) => {
    setMainTab(key)
    setPagination(prev => ({ ...prev, current: 1 }))
  }

  const handleMediaTypeChange = (key) => {
    setMediaType(key)
    setPagination(prev => ({ ...prev, current: 1 }))
  }

  // 取消喜欢/收藏
  const handleToggle = async (item) => {
    try {
      if (mediaType === 'douyin') {
        if (mainTab === 'likes') {
          await douyinApi.toggleLike(item.id)
          message.success('已取消喜欢')
        } else {
          await douyinApi.toggleFavorite(item.id)
          message.success('已取消收藏')
        }
      } else {
        const api = mediaType === 'video' ? videosApi : imagesApi
        if (mainTab === 'likes') {
          await api.toggleLike(item.id)
          message.success('已取消喜欢')
        } else {
          await api.toggleFavorite(item.id)
          message.success('已取消收藏')
        }
      }
      setItems(items.filter(f => f.id !== item.id))
      setPagination(prev => ({ ...prev, total: prev.total - 1 }))
      fetchStats()
    } catch (error) {
      message.error('操作失败')
    }
  }

  // 视频点击播放
  const handleVideoClick = (video) => {
    if (mediaType === 'douyin') {
      navigate('/douyin')
    } else {
      navigate(`/videos/${video.id}`)
    }
  }

  // 图片点击预览
  const handleImageClick = (image) => {
    setSlideshowImages(items)
    setSlideshowStartIndex(items.findIndex(img => img.id === image.id))
    setSlideshowVisible(true)
  }

  // 计算总数
  const totalLikes = stats.videoLikes + stats.imageLikes + stats.douyinLikes
  const totalFavorites = stats.videoFavorites + stats.imageFavorites + stats.douyinFavorites

  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <Title level={2} style={{ marginBottom: 16 }}>个人中心</Title>
      </div>

      {/* 主 Tab：喜欢 / 收藏 */}
      <Tabs
        activeKey={mainTab}
        onChange={handleMainTabChange}
        items={[
          {
            key: 'likes',
            label: (
              <span>
                <HeartFilled style={{ color: mainTab === 'likes' ? '#ff4d4f' : undefined }} />
                喜欢
                <Badge count={totalLikes} style={{ marginLeft: 8, backgroundColor: '#ff4d4f' }} />
              </span>
            ),
          },
          {
            key: 'favorites',
            label: (
              <span>
                <StarFilled style={{ color: mainTab === 'favorites' ? '#faad14' : undefined }} />
                收藏
                <Badge count={totalFavorites} style={{ marginLeft: 8, backgroundColor: '#faad14' }} />
              </span>
            ),
          },
        ]}
      />

      {/* 媒体类型 Tab：视频 / 图片 / 抖音 */}
      <Tabs
        activeKey={mediaType}
        onChange={handleMediaTypeChange}
        items={[
          {
            key: 'video',
            label: (
              <span>
                <VideoCameraOutlined style={{ color: mediaType === 'video' ? '#1890ff' : undefined }} />
                视频库
                <Badge
                  count={mainTab === 'likes' ? stats.videoLikes : stats.videoFavorites}
                  style={{ marginLeft: 8, backgroundColor: '#1890ff' }}
                />
              </span>
            ),
            children: (
              <MediaGrid
                items={items}
                type="video"
                loading={loading}
                pagination={{
                  current: pagination.current,
                  pageSize: pagination.pageSize,
                  total: pagination.total,
                  onChange: handlePageChange,
                }}
                onFavorite={mainTab === 'favorites' ? handleToggle : undefined}
                onLike={mainTab === 'likes' ? handleToggle : undefined}
                onItemClick={handleVideoClick}
              />
            ),
          },
          {
            key: 'image',
            label: (
              <span>
                <PictureOutlined style={{ color: mediaType === 'image' ? '#52c41a' : undefined }} />
                图片库
                <Badge
                  count={mainTab === 'likes' ? stats.imageLikes : stats.imageFavorites}
                  style={{ marginLeft: 8, backgroundColor: '#52c41a' }}
                />
              </span>
            ),
            children: (
              <MediaGrid
                items={items}
                type="image"
                loading={loading}
                pagination={{
                  current: pagination.current,
                  pageSize: pagination.pageSize,
                  total: pagination.total,
                  onChange: handlePageChange,
                }}
                onFavorite={mainTab === 'favorites' ? handleToggle : undefined}
                onLike={mainTab === 'likes' ? handleToggle : undefined}
                onItemClick={handleImageClick}
              />
            ),
          },
          {
            key: 'douyin',
            label: (
              <span>
                <PlayCircleOutlined style={{ color: mediaType === 'douyin' ? DouyinRed : undefined }} />
                抖音库
                <Badge
                  count={mainTab === 'likes' ? stats.douyinLikes : stats.douyinFavorites}
                  style={{ marginLeft: 8, backgroundColor: DouyinRed }}
                />
              </span>
            ),
            children: (
              <MediaGrid
                items={items}
                type="video"
                loading={loading}
                pagination={{
                  current: pagination.current,
                  pageSize: pagination.pageSize,
                  total: pagination.total,
                  onChange: handlePageChange,
                }}
                onFavorite={mainTab === 'favorites' ? handleToggle : undefined}
                onLike={mainTab === 'likes' ? handleToggle : undefined}
                onItemClick={() => navigate('/douyin')}
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