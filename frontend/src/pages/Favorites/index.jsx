import React, { useEffect, useState, useCallback } from 'react'
import {
  Typography,
  Space,
  Button,
  message,
  Tabs,
  Badge,
} from 'antd'
import {
  HeartOutlined,
  VideoCameraOutlined,
  PictureOutlined,
  ReloadOutlined,
} from '@ant-design/icons'
import { favoritesApi, videosApi, imagesApi } from '@services/api'
import MediaGrid from '@components/MediaGrid'

const { Title } = Typography

function Favorites() {
  const [favorites, setFavorites] = useState([])
  const [loading, setLoading] = useState(false)
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 24,
    total: 0,
  })
  const [mediaType, setMediaType] = useState(undefined)

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

  useEffect(() => {
    fetchFavorites()
  }, [fetchFavorites])

  const handlePageChange = (page, pageSize) => {
    setPagination({
      ...pagination,
      current: page,
      pageSize: pageSize,
    })
  }

  const handleTabChange = (key) => {
    setMediaType(key === 'all' ? undefined : key)
    setPagination({ ...pagination, current: 1 })
  }

  const handleUnfavorite = async (item) => {
    try {
      const api = item.media_type === 'video' ? videosApi : imagesApi
      await api.toggleFavorite(item.id)
      message.success('已取消收藏')
      fetchFavorites()
    } catch (error) {
      message.error('操作失败')
    }
  }

  const filteredVideos = favorites.filter((item) => item.media_type === 'video')
  const filteredImages = favorites.filter((item) => item.media_type === 'image')

  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <Title level={2} style={{ marginBottom: 16 }}>
          <HeartOutlined style={{ color: '#ff4d4f', marginRight: 12 }} />
          我的收藏
        </Title>

        <Space wrap>
          <Button
            icon={<ReloadOutlined />}
            onClick={fetchFavorites}
            loading={loading}
          >
            刷新
          </Button>
        </Space>
      </div>

      <Tabs
        defaultActiveKey="all"
        onChange={handleTabChange}
        items={[
          {
            key: 'all',
            label: (
              <span>
                <HeartOutlined />
                全部
                <Badge count={pagination.total} style={{ marginLeft: 8 }} />
              </span>
            ),
            children: (
              <MediaGrid
                items={favorites}
                type="mixed"
                loading={loading}
                pagination={{
                  current: pagination.current,
                  pageSize: pagination.pageSize,
                  total: pagination.total,
                  onChange: handlePageChange,
                }}
                onFavorite={handleUnfavorite}
              />
            ),
          },
          {
            key: 'video',
            label: (
              <span>
                <VideoCameraOutlined />
                视频
              </span>
            ),
            children: (
              <MediaGrid
                items={filteredVideos}
                type="video"
                loading={loading}
                pagination={{
                  current: pagination.current,
                  pageSize: pagination.pageSize,
                  total: pagination.total,
                  onChange: handlePageChange,
                }}
                onFavorite={handleUnfavorite}
              />
            ),
          },
          {
            key: 'image',
            label: (
              <span>
                <PictureOutlined />
                图片
              </span>
            ),
            children: (
              <MediaGrid
                items={filteredImages}
                type="image"
                loading={loading}
                pagination={{
                  current: pagination.current,
                  pageSize: pagination.pageSize,
                  total: pagination.total,
                  onChange: handlePageChange,
                }}
                onFavorite={handleUnfavorite}
              />
            ),
          },
        ]}
      />
    </div>
  )
}

export default Favorites
