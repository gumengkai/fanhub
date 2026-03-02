import React, { useEffect, useState } from 'react'
import { Row, Col, Card, Statistic, Typography, Badge, Space, List, Tag as AntTag } from 'antd'
import {
  VideoCameraOutlined,
  PictureOutlined,
  HeartOutlined,
  EyeOutlined,
  FireOutlined,
  TagOutlined,
} from '@ant-design/icons'
import { Link, useNavigate } from 'react-router-dom'
import { videosApi, imagesApi, tagsApi } from '@services/api'
import MediaCard from '@components/MediaCard'
import './index.css'

const { Title, Text } = Typography

function Home() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)

  // 统计数据
  const [stats, setStats] = useState({
    totalVideos: 0,
    favoriteVideos: 0,
    totalImages: 0,
    favoriteImages: 0,
  })

  // 热门视频Top10
  const [hotVideos, setHotVideos] = useState([])
  // 收藏视频Top10
  const [favoriteVideos, setFavoriteVideos] = useState([])
  // 热门标签Top10
  const [hotTags, setHotTags] = useState([])

  useEffect(() => {
    fetchDashboardData()
  }, [])

  const fetchDashboardData = async () => {
    setLoading(true)
    try {
      // 1. 获取统计数据
      const [videosRes, imagesRes, favVideosRes, favImagesRes] = await Promise.all([
        videosApi.getList({ per_page: 1 }),
        imagesApi.getList({ per_page: 1 }),
        videosApi.getList({ per_page: 1, favorite: true }),
        imagesApi.getList({ per_page: 1, favorite: true }),
      ])

      setStats({
        totalVideos: videosRes.total || 0,
        favoriteVideos: favVideosRes.total || 0,
        totalImages: imagesRes.total || 0,
        favoriteImages: favImagesRes.total || 0,
      })

      // 2. 获取热门视频Top10（按播放次数排序）
      const hotVideosRes = await videosApi.getList({
        per_page: 10,
        sort_by: 'view_count',
        order: 'desc',
      })
      setHotVideos(hotVideosRes.items || [])

      // 3. 获取收藏视频Top10（按更新时间排序）
      const favoriteVideosRes = await videosApi.getList({
        per_page: 10,
        favorite: true,
        sort_by: 'updated_at',
        order: 'desc',
      })
      setFavoriteVideos(favoriteVideosRes.items || [])

      // 4. 获取热门标签Top10
      const tagsRes = await tagsApi.getList()
      const sortedTags = (tagsRes || [])
        .sort((a, b) => (b.video_count || 0) - (a.video_count || 0))
        .slice(0, 10)
      setHotTags(sortedTags)
    } catch (error) {
      console.error('Failed to fetch dashboard data:', error)
    } finally {
      setLoading(false)
    }
  }

  const statCards = [
    {
      title: '总视频数',
      value: stats.totalVideos,
      icon: <VideoCameraOutlined style={{ color: '#1890ff' }} />,
      link: '/videos',
    },
    {
      title: '收藏视频',
      value: stats.favoriteVideos,
      icon: <HeartOutlined style={{ color: '#ff4d4f' }} />,
      link: '/videos?favorite=true',
    },
    {
      title: '总图片数',
      value: stats.totalImages,
      icon: <PictureOutlined style={{ color: '#52c41a' }} />,
      link: '/images',
    },
    {
      title: '收藏图片',
      value: stats.favoriteImages,
      icon: <HeartOutlined style={{ color: '#eb2f96' }} />,
      link: '/images?favorite=true',
    },
  ]

  const handleVideoClick = (video) => {
    navigate(`/videos/${video.id}`)
  }

  const handleTagClick = (tagId) => {
    navigate(`/videos?tag=${tagId}`)
  }

  return (
    <div className="home-page">
      {/* 顶部统计卡片 */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        {statCards.map((card, index) => (
          <Col xs={24} sm={12} md={6} key={index}>
            <Link to={card.link}>
              <Card hoverable loading={loading} className="stat-card">
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

      {/* 主内容区域 */}
      <Row gutter={[16, 16]}>
        {/* 左侧：热门视频和收藏视频 */}
        <Col xs={24} lg={18}>
          {/* 热门视频Top10 */}
          <Card
            className="section-card"
            title={
              <Space>
                <FireOutlined style={{ color: '#ff4d4f' }} />
                <span>热门视频 Top10</span>
              </Space>
            }
            extra={<Link to="/videos?sort=view_count">查看全部</Link>}
            loading={loading}
            style={{ marginBottom: 24 }}
          >
            {hotVideos.length > 0 ? (
              <Row gutter={[16, 16]}>
                {hotVideos.map((video, index) => (
                  <Col xs={24} sm={12} md={8} lg={6} xl={4} key={video.id}>
                    <div className="top-item">
                      <div className={`top-rank ${index < 3 ? 'top-three' : ''}`}>
                        {index + 1}
                      </div>
                      <MediaCard
                        item={video}
                        type="video"
                        onClick={() => handleVideoClick(video)}
                      />
                      <div className="view-count">
                        <EyeOutlined /> {video.view_count || 0} 次播放
                      </div>
                    </div>
                  </Col>
                ))}
              </Row>
            ) : (
              <Text type="secondary">暂无视频</Text>
            )}
          </Card>

          {/* 收藏视频Top10 */}
          <Card
            className="section-card"
            title={
              <Space>
                <HeartOutlined style={{ color: '#eb2f96' }} />
                <span>收藏视频 Top10</span>
              </Space>
            }
            extra={<Link to="/videos?favorite=true">查看全部</Link>}
            loading={loading}
          >
            {favoriteVideos.length > 0 ? (
              <Row gutter={[16, 16]}>
                {favoriteVideos.map((video) => (
                  <Col xs={24} sm={12} md={8} lg={6} xl={4} key={video.id}>
                    <MediaCard
                      item={video}
                      type="video"
                      onClick={() => handleVideoClick(video)}
                    />
                  </Col>
                ))}
              </Row>
            ) : (
              <Text type="secondary">暂无收藏视频</Text>
            )}
          </Card>
        </Col>

        {/* 右侧：热门标签 */}
        <Col xs={24} lg={6}>
          <Card
            className="section-card tags-card"
            title={
              <Space>
                <TagOutlined style={{ color: '#1890ff' }} />
                <span>热门标签 Top10</span>
              </Space>
            }
            loading={loading}
          >
            {hotTags.length > 0 ? (
              <List
                dataSource={hotTags}
                renderItem={(tag, index) => (
                  <List.Item
                    className="tag-list-item"
                    onClick={() => handleTagClick(tag.id)}
                  >
                    <div className="tag-item">
                      <span className={`tag-rank ${index < 3 ? 'top-three' : ''}`}>
                        {index + 1}
                      </span>
                      <AntTag
                        color={tag.color || '#1890ff'}
                        className="tag-name"
                      >
                        {tag.name}
                      </AntTag>
                      <span className="tag-count">
                        {tag.video_count || 0} 个视频
                      </span>
                    </div>
                  </List.Item>
                )}
              />
            ) : (
              <Text type="secondary">暂无标签</Text>
            )}
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default Home
