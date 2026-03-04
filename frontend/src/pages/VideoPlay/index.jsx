import React, { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate, useLocation } from 'react-router-dom'
import {
  Typography,
  Button,
  Space,
  Tag,
  Divider,
  Row,
  Col,
  Card,
  Descriptions,
  message,
  Popconfirm,
  Empty,
  Spin,
  Progress,
  Input,
  Tooltip,
} from 'antd'
import {
  HeartOutlined,
  HeartFilled,
  LeftOutlined,
  DeleteOutlined,
  EditOutlined,
  TagsOutlined,
  ClockCircleOutlined,
  FileOutlined,
  VideoCameraOutlined,
  EyeOutlined,
  HistoryOutlined,
  StepBackwardOutlined,
  StepForwardOutlined,
  PlayCircleOutlined,
} from '@ant-design/icons'
import { videosApi } from '@services/api'
import VideoPlayer from '@components/VideoPlayer'
import VideoTags from './VideoTags'
import RelatedVideos from './RelatedVideos'
import './index.css'

const { Title, Text, Paragraph } = Typography
const { TextArea } = Input

function VideoPlay() {
  const { id } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const [video, setVideo] = useState(null)
  const [loading, setLoading] = useState(true)
  const [isEditing, setIsEditing] = useState(false)
  const [editedTitle, setEditedTitle] = useState('')
  const [editedDescription, setEditedDescription] = useState('')
  const [watchProgress, setWatchProgress] = useState({ position: 0, completed: false })

  // 上一个/下一个视频导航状态
  const [listContext, setListContext] = useState(null)
  const [adjacentVideos, setAdjacentVideos] = useState({ prev: null, next: null })
  const [navLoading, setNavLoading] = useState(false)

  const fetchVideo = useCallback(async () => {
    setLoading(true)
    try {
      const response = await videosApi.getById(id)
      setVideo(response)
      setEditedTitle(response.title)
      setEditedDescription(response.description || '')

      // Fetch watch history
      try {
        const historyRes = await fetch(`/api/history/video/${id}`)
        if (historyRes.ok) {
          const history = await historyRes.json()
          setWatchProgress({
            position: history.playback_position || 0,
            completed: history.is_completed || false
          })
        }
      } catch (e) {
        // No history, that's ok
      }
    } catch (error) {
      message.error('获取视频信息失败')
    } finally {
      setLoading(false)
    }
  }, [id])

  // 获取相邻视频（上一个/下一个）
  const fetchAdjacentVideos = useCallback(async () => {
    if (!listContext) return

    setNavLoading(true)
    try {
      const params = {
        page: 1,
        per_page: 1000, // 获取足够多的视频来找到当前视频的位置
        sort_by: listContext.sortBy,
        order: listContext.sortOrder,
        search: listContext.searchQuery || '',
      }
      if (listContext.favoriteOnly) {
        params.favorite = true
      }
      if (listContext.selectedTag) {
        params.tag_id = listContext.selectedTag
      }

      const response = await videosApi.getList(params)
      const videos = response.items || []

      // 找到当前视频在列表中的索引
      const currentIndex = videos.findIndex(v => v.id === parseInt(id))

      if (currentIndex !== -1) {
        setAdjacentVideos({
          prev: currentIndex > 0 ? videos[currentIndex - 1] : null,
          next: currentIndex < videos.length - 1 ? videos[currentIndex + 1] : null
        })
      }
    } catch (error) {
      console.error('Failed to fetch adjacent videos:', error)
    } finally {
      setNavLoading(false)
    }
  }, [listContext, id])

  // 从 location state 获取列表上下文
  useEffect(() => {
    if (location.state?.listContext) {
      setListContext(location.state.listContext)
    }
  }, [location.state])

  // 当列表上下文或视频ID变化时，获取相邻视频
  useEffect(() => {
    fetchAdjacentVideos()
  }, [fetchAdjacentVideos])

  // 获取视频信息
  useEffect(() => {
    fetchVideo()
  }, [fetchVideo])

  // 导航到上一个/下一个视频
  const handleNavigate = (direction) => {
    const targetVideo = direction === 'prev' ? adjacentVideos.prev : adjacentVideos.next
    if (targetVideo && listContext) {
      navigate(`/videos/${targetVideo.id}`, {
        state: { listContext }
      })
    }
  }

  // 键盘快捷键支持
  useEffect(() => {
    const handleKeyDown = (e) => {
      // 只有当没有在输入框中输入时才处理
      if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return

      switch (e.key) {
        case 'ArrowLeft':
          if (e.altKey && adjacentVideos.prev) {
            e.preventDefault()
            handleNavigate('prev')
          }
          break
        case 'ArrowRight':
          if (e.altKey && adjacentVideos.next) {
            e.preventDefault()
            handleNavigate('next')
          }
          break
        case 'p':
        case 'P':
          if (adjacentVideos.prev) {
            handleNavigate('prev')
          }
          break
        case 'n':
        case 'N':
          if (adjacentVideos.next) {
            handleNavigate('next')
          }
          break
        default:
          break
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [adjacentVideos, listContext])

  const handleFavorite = async () => {
    try {
      const response = await videosApi.toggleFavorite(video.id)
      setVideo({ ...video, is_favorite: response.is_favorite })
      message.success(response.is_favorite ? '已添加到收藏' : '已取消收藏')
    } catch (error) {
      message.error('操作失败')
    }
  }

  const handleDelete = async () => {
    try {
      await videosApi.delete(video.id)
      message.success('视频已删除')
      navigate('/videos')
    } catch (error) {
      message.error('删除失败')
    }
  }

  const handleSaveEdit = async () => {
    try {
      const response = await videosApi.update(video.id, {
        title: editedTitle,
        description: editedDescription,
      })
      setVideo(response)
      setIsEditing(false)
      message.success('保存成功')
    } catch (error) {
      message.error('保存失败')
    }
  }

  // 只更新视频标签，不重新获取整个视频
  const handleTagsUpdate = (updatedTags) => {
    setVideo({ ...video, tags: updatedTags })
  }

  const handleProgressUpdate = async (videoId, position, duration, completed) => {
    try {
      await fetch(`/api/videos/${videoId}/history`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          playback_position: position,
          duration: duration,
          is_completed: completed
        })
      })
      setWatchProgress({ position, completed })
    } catch (error) {
      console.error('Failed to save progress:', error)
    }
  }

  const formatDuration = (seconds) => {
    if (!seconds) return '--:--'
    const hours = Math.floor(seconds / 3600)
    const minutes = Math.floor((seconds % 3600) / 60)
    const secs = seconds % 60

    if (hours > 0) {
      return `${hours}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
    }
    return `${minutes}:${secs.toString().padStart(2, '0')}`
  }

  const formatFileSize = (bytes) => {
    if (!bytes) return 'Unknown'
    const units = ['B', 'KB', 'MB', 'GB', 'TB']
    let size = bytes
    let unitIndex = 0
    while (size >= 1024 && unitIndex < units.length - 1) {
      size /= 1024
      unitIndex++
    }
    return `${size.toFixed(2)} ${units[unitIndex]}`
  }

  const progressPercent = video?.duration > 0 
    ? Math.round((watchProgress.position / video.duration) * 100) 
    : 0

  if (loading) {
    return (
      <div className="video-play-loading">
        <Spin size="large" tip="加载中..." />
      </div>
    )
  }

  if (!video) {
    return (
      <Empty
        description="视频不存在"
        extra={<Button onClick={() => navigate('/videos')}>返回视频库</Button>}
      />
    )
  }

  return (
    <div className="video-play-page">
      <div className="video-nav-header">
        <Button
          icon={<LeftOutlined />}
          onClick={() => navigate('/videos')}
          className="back-button"
        >
          返回视频库
        </Button>

        <Space className="nav-controls">
          <Tooltip title="上一个视频 (Alt + ← 或 P)">
            <Button
              icon={<StepBackwardOutlined />}
              onClick={() => handleNavigate('prev')}
              disabled={!adjacentVideos.prev}
              loading={navLoading}
            >
              上一个
            </Button>
          </Tooltip>
          <Tooltip title="下一个视频 (Alt + → 或 N)">
            <Button
              icon={<StepForwardOutlined />}
              onClick={() => handleNavigate('next')}
              disabled={!adjacentVideos.next}
              loading={navLoading}
            >
              下一个
            </Button>
          </Tooltip>
        </Space>
      </div>

      <Row gutter={[24, 24]}>
        <Col xs={24} lg={16}>
          <VideoPlayer 
            video={video} 
            onProgressUpdate={handleProgressUpdate}
          />

          {/* Watch progress indicator */}
          {watchProgress.position > 0 && (
            <Card className="progress-card" size="small">
              <Space>
                <HistoryOutlined />
                <Text>观看进度</Text>
                <Progress 
                  percent={progressPercent} 
                  size="small" 
                  strokeColor="#fb7299"
                  format={() => `${formatDuration(watchProgress.position)} / ${formatDuration(video.duration)}`}
                />
                {watchProgress.completed && (
                  <Tag color="green">已完成</Tag>
                )}
              </Space>
            </Card>
          )}

          <Card className="video-info-card">
            {isEditing ? (
              <div className="video-edit-form">
                <Input
                  className="video-title-input"
                  value={editedTitle}
                  onChange={(e) => setEditedTitle(e.target.value)}
                  placeholder="视频标题"
                  size="large"
                />
                <TextArea
                  className="video-desc-input"
                  value={editedDescription}
                  onChange={(e) => setEditedDescription(e.target.value)}
                  placeholder="视频描述（可选）"
                  rows={3}
                  autoSize={{ minRows: 2, maxRows: 6 }}
                />
                <Space>
                  <Button type="primary" onClick={handleSaveEdit}>
                    保存
                  </Button>
                  <Button onClick={() => setIsEditing(false)}>取消</Button>
                </Space>
              </div>
            ) : (
              <>
                <div className="video-header">
                  <Title level={3} className="video-title">
                    {video.title}
                  </Title>
                  <Space>
                    <Button
                      type={video.is_favorite ? 'primary' : 'default'}
                      icon={video.is_favorite ? <HeartFilled /> : <HeartOutlined />}
                      onClick={handleFavorite}
                      style={{ borderColor: video.is_favorite ? '#fb7299' : undefined, background: video.is_favorite ? '#fb7299' : undefined }}
                    >
                      {video.is_favorite ? '已收藏' : '收藏'}
                    </Button>
                    <Button
                      icon={<EditOutlined />}
                      onClick={() => setIsEditing(true)}
                    >
                      编辑
                    </Button>
                    <Popconfirm
                      title="确认删除"
                      description="删除后将无法恢复，是否继续？"
                      onConfirm={handleDelete}
                      okText="删除"
                      cancelText="取消"
                    >
                      <Button danger icon={<DeleteOutlined />}>
                        删除
                      </Button>
                    </Popconfirm>
                  </Space>
                </div>

                {video.description && (
                  <Paragraph className="video-description">
                    {video.description}
                  </Paragraph>
                )}

                <VideoTags videoId={video.id} tags={video.tags} onUpdate={handleTagsUpdate} />

                <Divider />

                <Descriptions size="small" column={{ xs: 1, sm: 2, md: 3 }}>
                  <Descriptions.Item label={<><ClockCircleOutlined /> 时长</>}>
                    {formatDuration(video.duration)}
                  </Descriptions.Item>
                  <Descriptions.Item label={<><VideoCameraOutlined /> 分辨率</>}>
                    {video.width && video.height ? `${video.width}x${video.height}` : 'Unknown'}
                  </Descriptions.Item>
                  <Descriptions.Item label={<><FileOutlined /> 文件大小</>}>
                    {formatFileSize(video.file_size)}
                  </Descriptions.Item>
                  <Descriptions.Item label={<><EyeOutlined /> 播放次数</>}>
                    {video.view_count || 0}
                  </Descriptions.Item>
                  <Descriptions.Item label="来源">
                    {video.source?.name || 'Unknown'}
                  </Descriptions.Item>
                  <Descriptions.Item label="添加时间">
                    {new Date(video.created_at).toLocaleString()}
                  </Descriptions.Item>
                </Descriptions>
              </>
            )}
          </Card>
        </Col>

        <Col xs={24} lg={8}>
          <RelatedVideos videoId={video.id} currentVideo={video} />
        </Col>
      </Row>
    </div>
  )
}

export default VideoPlay
