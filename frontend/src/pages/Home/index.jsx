import React, { useEffect, useState } from 'react'
import { Row, Col, Card, Statistic, Typography, Badge, Space } from 'antd'
import {
  VideoCameraOutlined,
  PictureOutlined,
  DatabaseOutlined,
  HeartOutlined,
} from '@ant-design/icons'
import { Link } from 'react-router-dom'
import { videosApi, imagesApi, sourcesApi, favoritesApi } from '@services/api'
import MediaCard from '@components/MediaCard'

const { Title, Text } = Typography

function Home() {
  const [stats, setStats] = useState({
    videos: 0,
    images: 0,
    sources: 0,
    favorites: 0,
  })
  const [recentVideos, setRecentVideos] = useState([])
  const [recentImages, setRecentImages] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetchDashboardData()
  }, [])

  const fetchDashboardData = async () => {
    setLoading(true)
    try {
      // Fetch counts in parallel
      const [videosRes, imagesRes, sourcesRes, favoritesRes] = await Promise.all([
        videosApi.getList({ per_page: 1 }),
        imagesApi.getList({ per_page: 1 }),
        sourcesApi.getList(),
        favoritesApi.getStats(),
      ])

      setStats({
        videos: videosRes.total || 0,
        images: imagesRes.total || 0,
        sources: sourcesRes.length || 0,
        favorites: favoritesRes.total_count || 0,
      })

      // Fetch recent items
      const [recentVideosRes, recentImagesRes] = await Promise.all([
        videosApi.getList({ per_page: 4, sort_by: 'created_at', order: 'desc' }),
        imagesApi.getList({ per_page: 4, sort_by: 'created_at', order: 'desc' }),
      ])

      setRecentVideos(recentVideosRes.items || [])
      setRecentImages(recentImagesRes.items || [])
    } catch (error) {
      console.error('Failed to fetch dashboard data:', error)
    } finally {
      setLoading(false)
    }
  }

  const formatFileSize = (bytes) => {
    if (!bytes) return '0 B'
    const units = ['B', 'KB', 'MB', 'GB', 'TB']
    let size = bytes
    let unitIndex = 0
    while (size >= 1024 && unitIndex < units.length - 1) {
      size /= 1024
      unitIndex++
    }
    return `${size.toFixed(1)} ${units[unitIndex]}`
  }

  const statCards = [
    {
      title: '视频总数',
      value: stats.videos,
      icon: <VideoCameraOutlined style={{ color: '#1890ff' }} />,
      link: '/videos',
    },
    {
      title: '图片总数',
      value: stats.images,
      icon: <PictureOutlined style={{ color: '#52c41a' }} />,
      link: '/images',
    },
    {
      title: '来源数量',
      value: stats.sources,
      icon: <DatabaseOutlined style={{ color: '#faad14' }} />,
      link: '/sources',
    },
    {
      title: '我的收藏',
      value: stats.favorites,
      icon: <HeartOutlined style={{ color: '#ff4d4f' }} />,
      link: '/favorites',
    },
  ]

  return (
    <div>
      <Title level={2}>仪表盘</Title>

      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        {statCards.map((card, index) => (
          <Col xs={24} sm={12} md={6} key={index}>
            <Link to={card.link}>
              <Card hoverable loading={loading}>
                <Statistic
                  title={card.title}
                  value={card.value}
                  prefix={card.icon}
                />
              </Card>
            </Link>
          </Col>
        ))}
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card
            title={
              <Space>
                <VideoCameraOutlined />
                <span>最近添加的视频</span>
              </Space>
            }
            extra={<Link to="/videos">查看全部</Link>}
            loading={loading}
          >
            {recentVideos.length > 0 ? (
              <Row gutter={[16, 16]}>
                {recentVideos.map((video) => (
                  <Col xs={24} sm={12} key={video.id}>
                    <MediaCard
                      item={video}
                      type="video"
                      onClick={() => {}}
                    />
                  </Col>
                ))}
              </Row>
            ) : (
              <Text type="secondary">暂无视频</Text>
            )}
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card
            title={
              <Space>
                <PictureOutlined />
                <span>最近添加的图片</span>
              </Space>
            }
            extra={<Link to="/images">查看全部</Link>}
            loading={loading}
          >
            {recentImages.length > 0 ? (
              <Row gutter={[16, 16]}>
                {recentImages.map((image) => (
                  <Col xs={24} sm={12} key={image.id}>
                    <MediaCard
                      item={image}
                      type="image"
                      onClick={() => {}}
                    />
                  </Col>
                ))}
              </Row>
            ) : (
              <Text type="secondary">暂无图片</Text>
            )}
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default Home
