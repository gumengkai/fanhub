import React, { useEffect, useState } from 'react'
import { Row, Col, Card, Statistic, Typography, Badge, Space, List, Tag } from 'antd'
import {
  VideoCameraOutlined,
  PictureOutlined,
  HeartOutlined,
  EyeOutlined,
  FireOutlined,
  PlayCircleOutlined,
  CloudOutlined,
} from '@ant-design/icons'
import { Link, useNavigate } from 'react-router-dom'
import { videosApi, imagesApi, douyinApi, peakApi, wordcloudApi } from '@services/api'
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
    totalDouyin: 0,
    likedDouyin: 0,
    totalPeak: 0,
    likedPeak: 0,
  })

  // 热门视频Top10
  const [hotVideos, setHotVideos] = useState([])
  // 收藏视频Top10
  const [favoriteVideos, setFavoriteVideos] = useState([])
  // 抖音库热门视频
  const [hotDouyin, setHotDouyin] = useState([])
  // 巅峰库热门视频
  const [hotPeak, setHotPeak] = useState([])
  // 词云数据
  const [wordcloud, setWordcloud] = useState([])

  useEffect(() => {
    fetchDashboardData()
  }, [])

  const fetchDashboardData = async () => {
    setLoading(true)
    try {
      // 1. 获取统计数据
      const [videosRes, imagesRes, favVideosRes, favImagesRes, douyinStatsRes, peakStatsRes] = await Promise.all([
        videosApi.getList({ per_page: 1 }),
        imagesApi.getList({ per_page: 1 }),
        videosApi.getList({ per_page: 1, favorite: true }),
        imagesApi.getList({ per_page: 1, favorite: true }),
        douyinApi.getStats(),
        peakApi.getStats(),
      ])

      setStats({
        totalVideos: videosRes.total || 0,
        favoriteVideos: favVideosRes.total || 0,
        totalImages: imagesRes.total || 0,
        favoriteImages: favImagesRes.total || 0,
        totalDouyin: douyinStatsRes.total || 0,
        likedDouyin: douyinStatsRes.liked || 0,
        totalPeak: peakStatsRes.total || 0,
        likedPeak: peakStatsRes.liked || 0,
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

      // 4. 获取抖音库热门视频
      const hotDouyinRes = await douyinApi.getList({
        per_page: 6,
        sort_by: 'view_count',
        order: 'desc',
      })
      setHotDouyin(hotDouyinRes.items || [])

      // 5. 获取巅峰库热门视频
      const hotPeakRes = await peakApi.getList({
        per_page: 6,
        sort_by: 'view_count',
        order: 'desc',
      })
      setHotPeak(hotPeakRes.items || [])

      // 6. 获取词云数据
      const wordcloudRes = await wordcloudApi.getList({ limit: 30, min_count: 2 })
      setWordcloud(wordcloudRes || [])
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
      title: '抖音库',
      value: stats.totalDouyin,
      icon: <PlayCircleOutlined style={{ color: '#eb2f96' }} />,
      link: '/douyin',
    },
    {
      title: '抖音喜欢',
      value: stats.likedDouyin,
      icon: <HeartOutlined style={{ color: '#FE2C55' }} />,
      link: '/douyin',
    },
    {
      title: '巅峰库',
      value: stats.totalPeak,
      icon: <FireOutlined style={{ color: '#FF6B00' }} />,
      link: '/peak',
    },
    {
      title: '巅峰喜欢',
      value: stats.likedPeak,
      icon: <HeartOutlined style={{ color: '#FF6B00' }} />,
      link: '/peak',
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
      icon: <HeartOutlined style={{ color: '#722ed1' }} />,
      link: '/images?favorite=true',
    },
  ]

  const handleVideoClick = (video) => {
    navigate(`/videos/${video.id}`)
  }

  const handleDouyinClick = () => {
    navigate('/douyin')
  }

  const handlePeakClick = () => {
    navigate('/peak')
  }

  const handleWordClick = (word) => {
    navigate(`/videos?search=${encodeURIComponent(word)}`)
  }

  return (
    <div className="home-page">
      {/* 顶部统计卡片 */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        {statCards.map((card, index) => (
          <Col xs={24} sm={12} md={8} lg={4} key={index}>
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
        <Col xs={24} lg={12}>
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
                  <Col xs={24} sm={12} md={8} lg={6} key={video.id}>
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
                  <Col xs={24} sm={12} md={8} lg={6} key={video.id}>
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

        {/* 中间：抖音库 */}
        <Col xs={24} lg={6}>
          <Card
            className="section-card"
            title={
              <Space>
                <PlayCircleOutlined style={{ color: '#eb2f96' }} />
                <span>抖音库热门</span>
              </Space>
            }
            extra={<Link to="/douyin">进入抖音库</Link>}
            loading={loading}
          >
            {hotDouyin.length > 0 ? (
              <Row gutter={[8, 8]}>
                {hotDouyin.map((video, index) => (
                  <Col span={12} key={video.id}>
                    <div className="douyin-item" onClick={handleDouyinClick}>
                      <div className={`douyin-rank ${index < 3 ? 'top-three' : ''}`}>
                        {index + 1}
                      </div>
                      <MediaCard
                        item={video}
                        type="video"
                        onClick={handleDouyinClick}
                      />
                      <div className="douyin-like">
                        {video.is_liked && <HeartOutlined style={{ color: '#FE2C55' }} />}
                      </div>
                    </div>
                  </Col>
                ))}
              </Row>
            ) : (
              <Text type="secondary">暂无抖音视频</Text>
            )}
          </Card>

          {/* 巅峰库热门 */}
          <Card
            className="section-card"
            title={
              <Space>
                <FireOutlined style={{ color: '#FF6B00' }} />
                <span>巅峰库热门</span>
              </Space>
            }
            extra={<Link to="/peak">进入巅峰库</Link>}
            loading={loading}
            style={{ marginTop: 24 }}
          >
            {hotPeak.length > 0 ? (
              <Row gutter={[8, 8]}>
                {hotPeak.map((video, index) => (
                  <Col span={12} key={video.id}>
                    <div className="peak-item" onClick={handlePeakClick}>
                      <div className={`peak-rank ${index < 3 ? 'top-three' : ''}`}>
                        {index + 1}
                      </div>
                      <MediaCard
                        item={video}
                        type="video"
                        onClick={handlePeakClick}
                      />
                      <div className="peak-like">
                        {video.is_liked && <HeartOutlined style={{ color: '#FF6B00' }} />}
                      </div>
                    </div>
                  </Col>
                ))}
              </Row>
            ) : (
              <Text type="secondary">暂无巅峰视频</Text>
            )}
          </Card>
        </Col>

        {/* 右侧：词云 */}
        <Col xs={24} lg={6}>
          <Card
            className="section-card wordcloud-card"
            title={
              <Space>
                <CloudOutlined style={{ color: '#1890ff' }} />
                <span>高频词汇</span>
              </Space>
            }
            loading={loading}
          >
            {wordcloud.length > 0 ? (
              <div className="wordcloud-container">
                {wordcloud.slice(0, 30).map((item, index) => (
                  <Tag
                    key={index}
                    className="wordcloud-tag"
                    onClick={() => handleWordClick(item.word)}
                    style={{
                      fontSize: Math.max(12, Math.min(18, 12 + item.count / 10)),
                      cursor: 'pointer',
                    }}
                  >
                    {item.word} ({item.count})
                  </Tag>
                ))}
              </div>
            ) : (
              <Text type="secondary">暂无词云数据</Text>
            )}
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default Home
